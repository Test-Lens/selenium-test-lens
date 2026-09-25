param(
    [ValidateSet("chrome", "firefox", "both")]
    [string]$Browser = "both",
    [int]$Warmups = 2,
    [int]$Repetitions = 5,
    [int]$Operations = 100,
    [switch]$Headed,
    [string]$OutputRoot = ""
)

$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
$sha = (& git -C $repo rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0) { throw "Could not resolve source SHA" }
$dirty = @(& git -C $repo status --porcelain)
if ($LASTEXITCODE -ne 0) { throw "Could not resolve source worktree state" }
$sourceRevision = if ($dirty.Count -gt 0) { "$sha+working-tree" } else { $sha }
$runId = (Get-Date -Format "yyyyMMdd-HHmmss") + "-" + $sha.Substring(0, 7)
if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
    $OutputRoot = Join-Path $repo "target/performance-audit/$runId"
}
$OutputRoot = [System.IO.Path]::GetFullPath($OutputRoot)
New-Item -ItemType Directory -Force -Path $OutputRoot | Out-Null

Push-Location $repo
try {
    & mvn -Pbrowser-it -pl selenium-test-lens-browser-tests -am test-compile "-DskipTests"
    if ($LASTEXITCODE -ne 0) { throw "Performance harness build failed" }

    $browsers = if ($Browser -eq "both") { @("chrome", "firefox") } else { @($Browser) }
    foreach ($name in $browsers) {
        $output = Join-Path $OutputRoot $name
        & mvn -Pbrowser-it -pl selenium-test-lens-browser-tests -am verify `
            "-Dtest=NoUnitTestSelected" `
            "-Dsurefire.failIfNoSpecifiedTests=false" `
            "-Dit.test=RuntimePerformanceAuditIT,RuntimeWorkloadPerformanceIT,RuntimeTestNgLifecyclePerformanceIT" `
            "-Dperf.audit=true" `
            "-Dperf.sourceSha=$sourceRevision" `
            "-Dperf.outputDir=$output" `
            "-Dperf.warmups=$Warmups" `
            "-Dperf.repetitions=$Repetitions" `
            "-Dperf.operations=$Operations" `
            "-Dperf.workloadWarmups=$Warmups" `
            "-Dperf.workloadRepetitions=$Repetitions" `
            "-Dbrowser=$name" `
            "-Dheaded=$($Headed.IsPresent.ToString().ToLowerInvariant())"
        if ($LASTEXITCODE -ne 0) { throw "Performance audit failed for $name" }
    }
} finally {
    Pop-Location
}

Write-Host "Runtime audit results: $OutputRoot"
