Set-StrictMode -Version Latest

function Get-TestLensSourceVersion {
    param([Parameter(Mandatory)][string]$RepositoryRoot)
    $pomPath = Join-Path $RepositoryRoot "pom.xml"
    if (-not (Test-Path -LiteralPath $pomPath -PathType Leaf)) {
        throw "Cannot read source version: root pom.xml does not exist at $pomPath"
    }
    try {
        [xml]$pom = [IO.File]::ReadAllText($pomPath)
        $namespace = [System.Xml.XmlNamespaceManager]::new($pom.NameTable)
        $namespace.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
        $node = $pom.SelectSingleNode("/m:project/m:version", $namespace)
        $version = if ($null -eq $node) { "" } else { $node.InnerText.Trim() }
    } catch {
        throw "Cannot read source version from root pom.xml: $($_.Exception.Message)"
    }
    if ([string]::IsNullOrWhiteSpace($version)) {
        throw "Cannot read source version from root pom.xml"
    }
    return $version
}

function Copy-TestLensReleaseTree {
    param([Parameter(Mandatory)][string]$SourceDirectory,
          [Parameter(Mandatory)][string]$DestinationDirectory)
    New-Item -ItemType Directory -Force -Path $DestinationDirectory | Out-Null
    foreach ($file in Get-ChildItem -LiteralPath $SourceDirectory -File -Force) {
        [IO.File]::Copy($file.FullName, (Join-Path $DestinationDirectory $file.Name), $true)
    }
    foreach ($directory in Get-ChildItem -LiteralPath $SourceDirectory -Directory -Force) {
        if ($directory.Name -in @("target", "build", ".gradle") -or
                ($directory.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            continue
        }
        Copy-TestLensReleaseTree $directory.FullName (Join-Path $DestinationDirectory $directory.Name)
    }
}

function New-TestLensCleanRoomRelease {
    param(
        [Parameter(Mandatory)][string]$RepositoryRoot,
        [string]$ReleaseVersion,
        [string]$WorkDirectory
    )
    $repository = [IO.Path]::GetFullPath($RepositoryRoot)
    $sourceVersion = Get-TestLensSourceVersion -RepositoryRoot $repository
    $sourceIsSnapshot = $sourceVersion.EndsWith("-SNAPSHOT", [StringComparison]::Ordinal)
    if ([string]::IsNullOrWhiteSpace($ReleaseVersion)) {
        $ReleaseVersion = if ($sourceIsSnapshot) {
            $sourceVersion.Substring(0, $sourceVersion.Length - "-SNAPSHOT".Length)
        } else {
            $sourceVersion
        }
    }
    if ([string]::IsNullOrWhiteSpace($ReleaseVersion) -or $ReleaseVersion.EndsWith("-SNAPSHOT")) {
        throw "Release version must be non-blank and must not be a snapshot"
    }
    if (-not $sourceIsSnapshot -and $ReleaseVersion -ne $sourceVersion) {
        throw "Release source version '$sourceVersion' cannot be transformed to '$ReleaseVersion'"
    }
    if ([string]::IsNullOrWhiteSpace($WorkDirectory)) {
        $WorkDirectory = Join-Path ([IO.Path]::GetTempPath()) ("selenium-test-lens-clean-room-" + [guid]::NewGuid())
    }
    $work = [IO.Path]::GetFullPath($WorkDirectory)
    $repositoryWithSeparator = $repository.TrimEnd([IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    if ($work.Equals($repository, [StringComparison]::OrdinalIgnoreCase) -or
            $work.StartsWith($repositoryWithSeparator, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Clean-room work directory must be outside the source repository"
    }

    $source = Join-Path $work "release-source"
    $staging = Join-Path $work "staging"
    $emptyM2 = Join-Path $work "empty-m2"
    New-Item -ItemType Directory -Force -Path $source, $staging, $emptyM2 | Out-Null
    if (Get-ChildItem -LiteralPath $source -Force) { throw "Clean-room release source is not empty: $source" }
    if (Get-ChildItem -LiteralPath $staging -Force) { throw "Clean-room staging repository is not empty: $staging" }
    if (Get-ChildItem -LiteralPath $emptyM2 -Force) { throw "Clean-room Maven repository is not empty: $emptyM2" }

    Get-ChildItem -LiteralPath $repository -Force |
        Where-Object { $_.Name -notin @(".git", ".agents", ".codex", "target") } |
        Where-Object {
            $tracked = @(& git -C $repository ls-files -- $_.Name)
            if ($LASTEXITCODE -ne 0) { throw "Cannot inspect tracked release source entry '$($_.Name)'" }
            $tracked.Count -gt 0
        } |
        ForEach-Object {
            if ($_.PSIsContainer) {
                Copy-TestLensReleaseTree $_.FullName (Join-Path $source $_.Name)
            } else {
                [IO.File]::Copy($_.FullName, (Join-Path $source $_.Name), $true)
            }
        }

    $poms = @(Get-ChildItem -LiteralPath $source -Recurse -Filter pom.xml |
        Where-Object { $_.FullName -notmatch '[\\/]target[\\/]' })
    if ($poms.Count -eq 0) { throw "No reactor POMs found in temporary release source" }
    foreach ($pom in $poms) {
        $content = [IO.File]::ReadAllText($pom.FullName)
        if (-not $content.Contains($sourceVersion)) {
            throw "Reactor POM does not reference source version '$sourceVersion': $($pom.FullName)"
        }
        if ($sourceVersion -ne $ReleaseVersion) {
            [IO.File]::WriteAllText($pom.FullName, $content.Replace($sourceVersion, $ReleaseVersion))
        }
    }
    if ($sourceVersion -ne $ReleaseVersion) {
        $stale = @($poms | Where-Object { [IO.File]::ReadAllText($_.FullName).Contains($sourceVersion) })
        if ($stale.Count -gt 0) { throw "Source snapshot remains in transformed POMs: $($stale.FullName -join ', ')" }
    }
    Write-Host "Clean-room release version: $sourceVersion -> $ReleaseVersion ($($poms.Count) reactor POMs)"

    $mavenName = if ($env:OS -eq "Windows_NT") { "mvn.cmd" } else { "mvn" }
    $maven = (Get-Command $mavenName -ErrorAction Stop).Source
    $releaseBuildArguments = @(
        "-f", (Join-Path $source "pom.xml"),
        "clean", "package", "-Prelease-artifacts", "-Dmaven.test.skip=true"
    )
    & $maven @releaseBuildArguments | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "Temporary release build failed" }
    & (Join-Path $source "scripts/validate-release-packaging.ps1") -Version $ReleaseVersion -StagingDirectory $staging | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "Release staging validation failed" }

    [pscustomobject]@{
        SourceVersion = $sourceVersion
        ReleaseVersion = $ReleaseVersion
        WorkDirectory = $work
        SourceDirectory = $source
        StagingDirectory = [IO.Path]::GetFullPath($staging)
        EmptyMavenRepository = [IO.Path]::GetFullPath($emptyM2)
    }
}

function Remove-TestLensTemporaryDirectory {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$RequiredNamePrefix
    )
    $resolved = [IO.Path]::GetFullPath($Path)
    $tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd(
        [IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar) +
        [IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -or
            -not ([IO.Path]::GetFileName($resolved)).StartsWith($RequiredNamePrefix, [StringComparison]::Ordinal)) {
        throw "Refusing to remove an unexpected temporary directory: $resolved"
    }
    for ($attempt = 1; $attempt -le 10; $attempt++) {
        if (-not (Test-Path -LiteralPath $resolved)) { return }
        try {
            $deletePath = if ($env:OS -eq "Windows_NT") { "\\?\$resolved" } else { $resolved }
            [IO.Directory]::Delete($deletePath, $true)
        } catch {
            if ($attempt -eq 10) {
                throw "Failed to remove temporary directory '$resolved': $($_.Exception.Message)"
            }
        }
        if (Test-Path -LiteralPath $resolved) { Start-Sleep -Milliseconds 200 }
    }
    if (Test-Path -LiteralPath $resolved) {
        throw "Failed to remove temporary directory: $resolved"
    }
}

Export-ModuleMember -Function Get-TestLensSourceVersion, New-TestLensCleanRoomRelease, Remove-TestLensTemporaryDirectory
