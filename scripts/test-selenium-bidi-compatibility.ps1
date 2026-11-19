param(
    [string[]]$Versions,
    [switch]$BrowserTests,
    [string]$GridUrl = '',
    [string]$ReuseFrozenRun = '',
    [string]$ChromeBinary = '',
    [string]$ChromeDriver = '',
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot)
)
$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath($RepositoryRoot)
$fixture = Join-Path $repo 'consumer-tests/selenium-bidi'
$matrix = Get-Content (Join-Path $fixture 'matrix.json') -Raw | ConvertFrom-Json
if (-not $Versions) { $Versions = $matrix.versions }
foreach ($version in $Versions) {
    if ($version -notmatch '^4\.\d+\.\d+$') { throw "Invalid Selenium stable version: $version" }
}
$run = Join-Path $repo ('target/selenium-bidi-compatibility/' + [Guid]::NewGuid().ToString('N'))
$frozen = Join-Path $run 'frozen'
New-Item -ItemType Directory -Path $run -Force | Out-Null
Push-Location $repo
try {
  if ($ReuseFrozenRun) {
    $previousRun = [IO.Path]::GetFullPath($ReuseFrozenRun)
    $frozen = Join-Path $previousRun 'frozen'
    $savedHashes = Get-Content -LiteralPath (Join-Path $previousRun 'artifact-sha256.json') -Raw | ConvertFrom-Json
    $artifacts = @{ lens = ''; core = ''; overlay = '' }
    $hashes = @{}
    foreach ($name in $artifacts.Keys) {
        $hashes[$name] = $savedHashes.$name
        if (-not $hashes[$name] -or (Get-FileHash -LiteralPath (Join-Path $frozen "$name.jar") -Algorithm SHA256).Hash -ne $hashes[$name]) {
            throw "Previously frozen artifact hash mismatch: $name"
        }
    }
    $legacy = Join-Path $frozen 'legacy.jar'
    if (-not (Test-Path -LiteralPath $legacy)) { throw 'Missing frozen legacy reproducer' }
  } else {
    New-Item -ItemType Directory -Path $frozen -Force | Out-Null
    # Build production ONCE against the declared baseline, never inside the version loop.
    & mvn -B -q -pl selenium-test-lens-selenium -am "-Dselenium.version=$($matrix.compileBaseline)" -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'Baseline Lens package failed' }
    [xml]$pom = Get-Content (Join-Path $repo 'pom.xml')
    $release = $pom.project.version
    $artifacts = @{
        lens = "selenium-test-lens-selenium/target/selenium-test-lens-$release.jar"
        core = "selenium-test-lens-core/target/selenium-test-lens-core-$release.jar"
        overlay = "selenium-test-lens-overlay/target/selenium-test-lens-overlay-$release.jar"
    }
    $hashes = @{}
    foreach ($name in $artifacts.Keys) {
        $destination = Join-Path $frozen "$name.jar"
        Copy-Item -LiteralPath (Join-Path $repo $artifacts[$name]) -Destination $destination
        $hashes[$name] = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash
    }
    $hashes | ConvertTo-Json | Set-Content (Join-Path $run 'artifact-sha256.json')
    & mvn -B -q -f (Join-Path $fixture 'legacy/pom.xml') package
    if ($LASTEXITCODE -ne 0) { throw 'Legacy binary reproducer compilation failed' }
    $legacy = Join-Path $frozen 'legacy.jar'
    Copy-Item -LiteralPath (Join-Path $fixture 'legacy/target/legacy-size-calls-1.jar') -Destination $legacy
  }
    $hashes | ConvertTo-Json | Set-Content (Join-Path $run 'artifact-sha256.json')
    $frozen | Set-Content (Join-Path $run 'frozen-artifacts-path.txt')
    $results = @()
    $requiredBinaryClasses = @('BinaryArtifactTest') + @(Get-ChildItem -LiteralPath (Join-Path $repo 'selenium-test-lens-selenium/src/test/java/io/github/testlens/selenium/network') -Filter '*Test.java' | ForEach-Object { $_.BaseName })
    foreach ($version in $Versions) {
        $output = Join-Path $run $version
        New-Item -ItemType Directory -Path $output -Force | Out-Null
        $arguments = @('-B', '-f', (Join-Path $fixture 'pom.xml'), 'test',
            "-Dselenium.version=$version", "-Dfrozen.dir=$frozen", "-Dlegacy.jar=$legacy",
            "-Drun.dir=$output", "-Dlens.tests=$(Join-Path $repo 'selenium-test-lens-selenium/src/test/java')",
            "-Dbidi.report.output=$(Join-Path $output 'report-artifacts')",
            "-Dlens.sha256=$($hashes.lens)", "-Dbidi.browser=$($BrowserTests.IsPresent.ToString().ToLowerInvariant())", "-Dbidi.grid=$GridUrl")
        if ($ChromeBinary) { $arguments += "-Dtest.chrome.binary=$ChromeBinary" }
        if ($ChromeDriver) { $arguments += "-Dwebdriver.chrome.driver=$ChromeDriver" }
        # Windows PowerShell turns native stderr (including Java INFO logs) into error records.
        # Capture it without terminating the matrix; Maven's exit code still controls the result.
        $savedErrorPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            & mvn @arguments *> (Join-Path $output 'maven.log')
            $exitCode = $LASTEXITCODE
        } finally { $ErrorActionPreference = $savedErrorPreference }
        foreach ($name in $artifacts.Keys) {
            if ((Get-FileHash -LiteralPath (Join-Path $frozen "$name.jar") -Algorithm SHA256).Hash -ne $hashes[$name]) {
                throw "Frozen artifact changed: $name"
            }
        }
        $status = if ($exitCode -eq 0) { 'PASS' } else { 'FAIL' }
        $chromeStatus = 'NOT RUN'
        $gridStatus = 'NOT RUN'
        $reportChromeStatus = 'NOT RUN'
        $reportGridStatus = 'NOT RUN'
        $browserReport = Join-Path $output 'surefire-reports/TEST-io.github.testlens.selenium.network.BrowserBinaryTest.xml'
        if (Test-Path -LiteralPath $browserReport) {
            [xml]$report = Get-Content -LiteralPath $browserReport
            foreach ($case in $report.testsuite.testcase) {
                $caseStatus = if ($null -ne $case.SelectSingleNode('skipped')) { 'NOT RUN' } elseif ($null -ne $case.SelectSingleNode('failure') -or $null -ne $case.SelectSingleNode('error')) { 'FAIL' } else { 'PASS' }
                if ($case.name -eq 'localChrome') { $chromeStatus = $caseStatus }
                if ($case.name -eq 'rawRemoteWebDriver') { $gridStatus = $caseStatus }
            }
        }
        $networkReport = Join-Path $output 'surefire-reports/TEST-io.github.testlens.selenium.network.NetworkReportCorrectnessTest.xml'
        if (Test-Path -LiteralPath $networkReport) {
            [xml]$reportData = Get-Content -LiteralPath $networkReport
            foreach ($case in $reportData.testsuite.testcase) {
                $caseStatus = if ($null -ne $case.SelectSingleNode('skipped')) { 'NOT RUN' } elseif ($null -ne $case.SelectSingleNode('failure') -or $null -ne $case.SelectSingleNode('error')) { 'FAIL' } else { 'PASS' }
                if ($case.name -eq 'localChromeReportDataMatchesServerJournal') { $reportChromeStatus = $caseStatus }
                if ($case.name -eq 'rawRemoteReportDataMatchesServerJournal') { $reportGridStatus = $caseStatus }
            }
        }
        # Keep binary and browser outcomes separate when only a browser scenario fails.
        $binaryReports = @(Get-ChildItem -LiteralPath (Join-Path $output 'surefire-reports') -Filter 'TEST-*.xml' -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notlike '*BrowserBinaryTest*' -and $_.Name -notlike '*NetworkReportCorrectnessTest*' })
        if ($binaryReports.Count -gt 0) {
            $binaryFailures = 0
            foreach ($file in $binaryReports) {
                [xml]$suite = Get-Content -LiteralPath $file.FullName
                $binaryFailures += [int]$suite.testsuite.failures + [int]$suite.testsuite.errors
            }
            $status = if ($binaryFailures -gt 0) { 'FAIL' } elseif ($exitCode -eq 0 -or @($chromeStatus, $gridStatus, $reportChromeStatus, $reportGridStatus) -contains 'FAIL') { 'PASS' } else { 'FAIL' }
        }
        # A successful build with skipped/missing tests is not a compatibility result.
        foreach ($requiredClass in $requiredBinaryClasses) {
            $requiredReport = Join-Path $output "surefire-reports/TEST-io.github.testlens.selenium.network.$requiredClass.xml"
            if (-not (Test-Path -LiteralPath $requiredReport)) { $status = 'FAIL'; continue }
            [xml]$requiredSuite = Get-Content -LiteralPath $requiredReport
            if ([int]$requiredSuite.testsuite.tests -lt 1 -or [int]$requiredSuite.testsuite.skipped -gt 0) { $status = 'FAIL' }
        }
        $gate = if ($exitCode -ne 0 -or $status -ne 'PASS' -or ($BrowserTests -and ($chromeStatus -ne 'PASS' -or $reportChromeStatus -ne 'PASS')) -or ($BrowserTests -and $GridUrl -and ($gridStatus -ne 'PASS' -or $reportGridStatus -ne 'PASS'))) { 'FAIL' } else { 'PASS' }
        $results += [PSCustomObject]@{ selenium = $version; gate = $gate; binary = $status; chrome = $chromeStatus; grid = $gridStatus; reportChrome = $reportChromeStatus; reportGrid = $reportGridStatus; lensSHA256 = $hashes.lens; log = (Join-Path $output 'maven.log') }
        Write-Host "Selenium $version binary: $status (same Lens SHA256 $($hashes.lens))"
        $results | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $run 'results.json')
    }
    Write-Host "Compatibility evidence: $run"
    if ($results.gate -contains 'FAIL') { throw 'Selenium BiDi compatibility matrix failed or required tests did not execute; inspect per-version maven.log' }
} finally { Pop-Location }
