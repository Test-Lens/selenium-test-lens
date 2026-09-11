param(
    [string]$ReleaseVersion,
    [string]$TestLensRepository,
    [switch]$KeepWorkDirectoryOnFailure
)

$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
Import-Module (Join-Path $PSScriptRoot "CleanRoomRelease.psm1") -Force
$sourceVersion = Get-TestLensSourceVersion -RepositoryRoot $repo
$sourceIsSnapshot = $sourceVersion.EndsWith("-SNAPSHOT", [StringComparison]::Ordinal)
if ([string]::IsNullOrWhiteSpace($ReleaseVersion)) {
    $ReleaseVersion = if ($sourceIsSnapshot) {
        $sourceVersion.Substring(0, $sourceVersion.Length - "-SNAPSHOT".Length)
    } else {
        $sourceVersion
    }
}
if ($ReleaseVersion.EndsWith("-SNAPSHOT", [StringComparison]::Ordinal)) {
    throw "Gradle consumer requires a non-snapshot release version"
}
if (-not $sourceIsSnapshot -and $ReleaseVersion -ne $sourceVersion) {
    throw "Release source version '$sourceVersion' cannot be validated as '$ReleaseVersion'"
}

$ownedWork = $false
$failed = $true
$work = Join-Path ([IO.Path]::GetTempPath()) ("selenium-test-lens-gradle-consumer-" + [guid]::NewGuid())
try {
    if ([string]::IsNullOrWhiteSpace($TestLensRepository)) {
        $prepared = New-TestLensCleanRoomRelease -RepositoryRoot $repo -ReleaseVersion $ReleaseVersion
        $TestLensRepository = $prepared.StagingDirectory
        $work = $prepared.WorkDirectory
        $ownedWork = $true
    } else {
        $TestLensRepository = [IO.Path]::GetFullPath($TestLensRepository)
        if (-not (Test-Path -LiteralPath $TestLensRepository -PathType Container)) {
            throw "Prepared Test Lens repository does not exist: $TestLensRepository"
        }
        New-Item -ItemType Directory -Force -Path $work | Out-Null
        $ownedWork = $true
    }

    $sourceRoot = [IO.Path]::GetFullPath($repo).TrimEnd([IO.Path]::DirectorySeparatorChar)
    if ($TestLensRepository.StartsWith($sourceRoot + [IO.Path]::DirectorySeparatorChar,
            [StringComparison]::OrdinalIgnoreCase) -or
            $TestLensRepository.Equals($sourceRoot, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Test Lens repository must be isolated from the source project"
    }

    $gradleHome = Join-Path $work "empty-gradle-home"
    New-Item -ItemType Directory -Force -Path $gradleHome | Out-Null
    if (Get-ChildItem -LiteralPath $gradleHome -Force) { throw "Gradle user home is not empty" }
    $consumer = Join-Path $repo "consumer-tests/gradle"
    $wrapper = if ($env:OS -eq "Windows_NT") { Join-Path $consumer "gradlew.bat" } else { Join-Path $consumer "gradlew" }
    if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf)) { throw "Gradle Wrapper is missing: $wrapper" }

    & (Join-Path $PSScriptRoot "validate-gradle-wrapper.ps1") | Out-Host
    $previousGradleHome = $env:GRADLE_USER_HOME
    $env:GRADLE_USER_HOME = $gradleHome
    try {
        Push-Location $consumer
        try {
            $arguments = @(
                "clean", "test", "verifyResolvedGraph", "--no-daemon", "--stacktrace",
                "-PtestLensVersion=$ReleaseVersion",
                "-PtestLensRepository=$TestLensRepository"
            )
            & $wrapper @arguments
            if ($LASTEXITCODE -ne 0) { throw "Gradle clean-room consumer failed" }
        } finally { Pop-Location }
    } finally { $env:GRADLE_USER_HOME = $previousGradleHome }

    $graph = Join-Path $consumer "build/reports/dependency-graph.txt"
    if (-not (Test-Path -LiteralPath $graph -PathType Leaf)) { throw "Gradle dependency graph report is missing" }
    $graphText = [IO.File]::ReadAllText($graph)
    if ($graphText.Contains("-SNAPSHOT")) { throw "Resolved Gradle graph contains a snapshot" }
    & (Join-Path $PSScriptRoot "validate-published-bytecode.ps1") `
        -TestLensRepository $TestLensRepository -ReleaseVersion $ReleaseVersion | Out-Host
    $javaCommand = Get-Command java -ErrorAction Stop
    $javaVersionProcess = [Diagnostics.Process]::new()
    $javaVersionProcess.StartInfo = [Diagnostics.ProcessStartInfo]::new()
    $javaVersionProcess.StartInfo.FileName = $javaCommand.Source
    $javaVersionProcess.StartInfo.Arguments = "-version"
    $javaVersionProcess.StartInfo.UseShellExecute = $false
    $javaVersionProcess.StartInfo.RedirectStandardError = $true
    $javaVersionProcess.StartInfo.RedirectStandardOutput = $true
    if (-not $javaVersionProcess.Start()) { throw "Unable to start java -version" }
    $javaVersionError = $javaVersionProcess.StandardError.ReadToEnd()
    $javaVersionOutput = $javaVersionProcess.StandardOutput.ReadToEnd()
    $javaVersionProcess.WaitForExit()
    if ($javaVersionProcess.ExitCode -ne 0) { throw "java -version failed with exit code $($javaVersionProcess.ExitCode)" }
    $javaVersionLine = (($javaVersionError + $javaVersionOutput) -split "`r?`n" | Select-Object -First 1).Trim()
    Write-Output "Gradle clean-room consumer PASS on $javaVersionLine"
    Write-Output "Dependency graph report: $graph"
    $failed = $false
} finally {
    if ($ownedWork -and (-not $failed -or -not $KeepWorkDirectoryOnFailure)) {
        Remove-TestLensTemporaryDirectory -Path $work -RequiredNamePrefix "selenium-test-lens-"
    } elseif ($failed -and $KeepWorkDirectoryOnFailure) {
        Write-Warning "Preserved Gradle consumer diagnostics at $work"
    }
}
