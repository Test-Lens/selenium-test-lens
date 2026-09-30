Set-StrictMode -Version 2.0

function Read-TestLensPom([string]$Path) {
    try { return [xml][IO.File]::ReadAllText($Path) }
    catch { throw "Cannot read Maven project '$Path': $($_.Exception.Message)" }
}

function Get-TestLensPomText([xml]$Pom, [string]$XPath) {
    $namespace = New-Object System.Xml.XmlNamespaceManager($Pom.NameTable)
    $namespace.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
    $node = $Pom.SelectSingleNode($XPath, $namespace)
    if ($null -eq $node) { return "" }
    return $node.InnerText.Trim()
}

function Get-TestLensPomTexts([xml]$Pom, [string]$XPath) {
    $namespace = New-Object System.Xml.XmlNamespaceManager($Pom.NameTable)
    $namespace.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
    return @($Pom.SelectNodes($XPath, $namespace) | ForEach-Object { $_.InnerText.Trim() })
}

function Get-TestLensReactorModel {
    param(
        [Parameter(Mandatory = $true)][string]$RepositoryRoot,
        [string]$ReleaseSourceRoot,
        [switch]$IncludeBrowserIt
    )
    if ([string]::IsNullOrWhiteSpace($RepositoryRoot)) {
        throw "RepositoryRoot must identify the source Git worktree"
    }
    $repository = [IO.Path]::GetFullPath($RepositoryRoot)
    if (-not (Test-Path -LiteralPath $repository -PathType Container)) {
        throw "RepositoryRoot does not exist: $repository"
    }
    $repositoryPom = Join-Path $repository "pom.xml"
    if (-not (Test-Path -LiteralPath $repositoryPom -PathType Leaf)) {
        throw "RepositoryRoot does not contain root pom.xml: $repositoryPom"
    }
    $gitRootOutput = @(& git -C $repository rev-parse --show-toplevel 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "RepositoryRoot is not a Git worktree: $repository ($($gitRootOutput -join ' '))"
    }
    $gitRoot = [IO.Path]::GetFullPath(($gitRootOutput | Select-Object -Last 1).ToString()).TrimEnd(
        [IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar)
    $normalizedRepository = $repository.TrimEnd(
        [IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar)
    if (-not $gitRoot.Equals($normalizedRepository, [StringComparison]::OrdinalIgnoreCase)) {
        throw "RepositoryRoot must be the Git worktree root '$gitRoot', but was '$repository'"
    }

    $root = if ([string]::IsNullOrWhiteSpace($ReleaseSourceRoot)) {
        $repository
    } else {
        [IO.Path]::GetFullPath($ReleaseSourceRoot)
    }
    if (-not (Test-Path -LiteralPath $root -PathType Container)) {
        throw "ReleaseSourceRoot does not exist: $root"
    }
    $rootPomPath = Join-Path $root "pom.xml"
    if (-not (Test-Path -LiteralPath $rootPomPath -PathType Leaf)) {
        throw "ReleaseSourceRoot does not contain root pom.xml: $rootPomPath"
    }
    $rootPom = Read-TestLensPom $rootPomPath
    $normalModules = @(Get-TestLensPomTexts $rootPom "/m:project/m:modules/m:module")
    $profileModules = @(Get-TestLensPomTexts $rootPom "/m:project/m:profiles/m:profile/m:modules/m:module")
    $browserModules = @(Get-TestLensPomTexts $rootPom "/m:project/m:profiles/m:profile[m:id='browser-it']/m:modules/m:module")
    $knownModules = @($normalModules + $profileModules | Sort-Object -Unique)
    $selectedModules = @($normalModules)
    if ($IncludeBrowserIt) { $selectedModules = @($selectedModules + $browserModules | Sort-Object -Unique) }

    $trackedOutput = @(& git -C $repository ls-files -- "pom.xml" ":(glob)**/pom.xml" 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "git ls-files failed while enumerating reactor POMs from RepositoryRoot '$repository': $($trackedOutput -join ' ')"
    }
    $tracked = @($trackedOutput | ForEach-Object { $_.ToString().Replace('\','/') } | Sort-Object -Unique)
    $expectedKnown = @("pom.xml") + @($knownModules | ForEach-Object { $_.TrimEnd('/','\').Replace('\','/') + "/pom.xml" })
    $expectedKnown = @($expectedKnown | Sort-Object -Unique)
    $missing = @($expectedKnown | Where-Object { $_ -notin $tracked })
    $unexpected = @($tracked | Where-Object { $_ -notin $expectedKnown })
    if ($missing.Count -gt 0) { throw "Reactor POMs are missing or untracked: $($missing -join ', ')" }
    if ($unexpected.Count -gt 0) { throw "Tracked POMs are not classified by the root Maven module graph: $($unexpected -join ', ')" }

    $selected = @("pom.xml") + @($selectedModules | ForEach-Object { $_.TrimEnd('/','\').Replace('\','/') + "/pom.xml" })
    $rootVersion = Get-TestLensPomText $rootPom "/m:project/m:version"
    $rootDeploySkip = Get-TestLensPomText $rootPom "/m:project/m:properties/m:maven.deploy.skip"
    $projects = foreach ($relative in @($selected | Sort-Object -Unique)) {
        $full = Join-Path $root ($relative.Replace('/', [IO.Path]::DirectorySeparatorChar))
        if (-not [IO.File]::Exists($full)) { throw "Reactor module POM is missing: $relative" }
        $pom = Read-TestLensPom $full
        $declared = Get-TestLensPomText $pom "/m:project/m:version"
        $parent = Get-TestLensPomText $pom "/m:project/m:parent/m:version"
        $version = if ([string]::IsNullOrWhiteSpace($declared)) { $parent } else { $declared }
        $packaging = Get-TestLensPomText $pom "/m:project/m:packaging"
        if ([string]::IsNullOrWhiteSpace($packaging)) { $packaging = "jar" }
        $localSkip = Get-TestLensPomText $pom "/m:project/m:properties/m:maven.deploy.skip"
        $effectiveSkip = if (-not [string]::IsNullOrWhiteSpace($localSkip)) { $localSkip } else { $rootDeploySkip }
        $modulePath = if ($relative -eq "pom.xml") { "" } else { $relative.Substring(0, $relative.Length - 8).TrimEnd('/') }
        [pscustomobject]@{
            RelativePom = $relative; Directory = Split-Path -Parent $full
            ArtifactId = Get-TestLensPomText $pom "/m:project/m:artifactId"
            Packaging = $packaging; Version = $version; ParentVersion = $parent
            EffectiveDeploySkip = ($effectiveSkip -eq "true")
            ProfileOnly = ($relative -ne "pom.xml" -and $modulePath -in $profileModules)
        }
    }
    return [pscustomobject]@{
        RepositoryRoot = $repository; ReleaseSourceRoot = $root
        RootVersion = $rootVersion; Projects = @($projects)
        NormalProjectCount = 1 + $normalModules.Count
        BrowserProjectCount = 1 + (@($normalModules + $browserModules | Sort-Object -Unique)).Count
        TrackedPomPaths = $tracked
    }
}

Export-ModuleMember -Function Read-TestLensPom, Get-TestLensPomText, Get-TestLensPomTexts, Get-TestLensReactorModel
