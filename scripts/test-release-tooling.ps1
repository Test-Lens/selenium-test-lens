$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$unicodeSuffix = ([char]0x017B).ToString() + ([char]0x00F3) + ([char]0x0142) + ([char]0x0107)
$testRoot = Join-Path ([IO.Path]::GetTempPath()) ("test-lens release fixture $unicodeSuffix " + [guid]::NewGuid())
$fixtureRoot = Join-Path $testRoot "source repository"
$releaseSourceRoot = Join-Path $testRoot "release source without git"
$consumerRoot = Join-Path $testRoot "consumer without git"
Import-Module (Join-Path $PSScriptRoot "ReleaseReactor.psm1") -Force
Import-Module (Join-Path $PSScriptRoot "StudioCertificationSupport.psm1") -Force
Import-Module (Join-Path $PSScriptRoot "PublicApiReleaseLine.psm1") -Force

$release040 = Resolve-TestLensApiReleaseLine -CurrentVersion "0.4.0" -ReleaseTags @("v0.3.1")
if ($release040.BaselineTag -ne "v0.3.1" -or $release040.ExpectedSince -ne "0.4.0") {
    throw "0.4.0 API release-line contract regression"
}
$release050 = Resolve-TestLensApiReleaseLine -CurrentVersion "0.5.0" -ReleaseTags @("v0.3.1", "v0.4.0", "v0.5.0")
if ($release050.BaselineTag -ne "v0.4.0" -or $release050.ExpectedSince -ne "0.5.0") {
    throw "0.5.0 API release-line contract regression"
}

$publicationPolicy = Read-TestLensPublicationPolicy (Join-Path $PSScriptRoot "config/publication-policy.json")
if ($publicationPolicy.LatestReleasedVersion -ne "0.5.0") {
    throw "Latest released version guard regression"
}
foreach ($role in @("PUBLISHED_STABLE", "PUBLISHED_TOOLING", "INTERNAL", "TEST_ONLY", "DEMO", "FUTURE")) {
    if (-not $publicationPolicy.ArtifactsByRole.Contains($role)) {
        throw "Publication policy role is missing: $role"
    }
}
if ("selenium-test-lens-selector-tooling" -notin $publicationPolicy.PublishedTooling) {
    throw "Selector tooling must be a published tooling dependency"
}
if ($publicationPolicy.NonReactorPomPaths -notcontains "scripts/fixtures/s15-external-consumer/pom.xml") {
    throw "Clean-room consumer POM must be classified as a non-reactor test fixture"
}
$ciWorkflow = [IO.File]::ReadAllText((Join-Path $repositoryRoot ".github/workflows/ci.yml"))
if ($ciWorkflow.Contains("-Version 0.5.0-SNAPSHOT")) {
    throw "Linux certification must not hardcode a development version"
}
foreach ($contract in @(
    'Read-TestLensPom (Join-Path $repositoryRoot "pom.xml")',
    'Get-TestLensPomText $rootPom "/m:project/m:version"',
    '-Version $reactorVersion',
    'New-TestLensCertificationTempRoot -RunnerTemp "${{ runner.temp }}" -Linux $true',
    '$env:TMPDIR = $tempRoot',
    '$env:TEMP = $tempRoot',
    '$env:TMP = $tempRoot',
    'Remove-TestLensCertificationTempRoot -Path $tempRoot'
)) {
    if (-not $ciWorkflow.Contains($contract)) { throw "Linux certification workflow is missing contract: $contract" }
}
if ($ciWorkflow -match '(?m)^\s+TMPDIR:\s*') {
    throw "Linux certification must create its temp root before exporting TMPDIR"
}

$longRunnerTemp = "/home/runner/work/_temp/test-lens-s15-temp-436950dba8224e59aa046300ad98cb4a"
$shortLinuxTemp = Get-TestLensCertificationTempRootPath -RunnerTemp $longRunnerTemp -Linux $true -RandomId "436950dba8224e59aa046300ad98cb4a"
if ($shortLinuxTemp -ne "/tmp/tl-436950db" -or $shortLinuxTemp.StartsWith($longRunnerTemp, [StringComparison]::Ordinal)) {
    throw "Linux certification must choose a real short /tmp root instead of the long runner temp path"
}
$projectedSingletonSocket = $shortLinuxTemp + "/org.chromium.Chromium." + ("x" * 16) + "/SingletonSocket"
$projectedSocketBytes = [Text.Encoding]::UTF8.GetByteCount($projectedSingletonSocket)
$safeSocketBudget = 108 - 16 # Linux sockaddr_un.sun_path minus the certification safety margin.
if ([Text.Encoding]::UTF8.GetByteCount($shortLinuxTemp) -gt 48 -or $projectedSocketBytes -gt $safeSocketBudget) {
    throw "Linux Chrome SingletonSocket path budget regression: root=$shortLinuxTemp; projectedBytes=$projectedSocketBytes; safeBudget=$safeSocketBudget"
}
$linuxJob = [regex]::Match($ciWorkflow, '(?ms)^  linux-external-certification:.*?(?=^  [A-Za-z0-9_-]+:|\z)').Value
if ([string]::IsNullOrWhiteSpace($linuxJob)) {
    throw "Linux external-consumer job is missing"
}
if (-not $linuxJob.Contains('SE_BROWSER_PATH: ${{ steps.setup-chrome.outputs.chrome-path }}')) {
    throw "Linux certification must pass the setup-chrome binary to Selenium Manager"
}
if (-not $linuxJob.Contains('SE_BROWSER_NO_SANDBOX: "true"')) {
    throw "Linux certification must explicitly disable the unusable archive Chrome sandbox"
}
if ($linuxJob.Contains('MAVEN_OPTS:')) {
    throw "Linux certification must not pin a driver without the matching browser binary"
}
foreach ($diagnosticContract in @(
    'SE_BROWSER_PATH=${SE_BROWSER_PATH:-<unset>}',
    'SE_BROWSER_NO_SANDBOX=${SE_BROWSER_NO_SANDBOX:-<unset>}',
    'webdriver.chrome.driver=<not supplied by certification job>',
    'driverResolution=SELENIUM_MANAGER',
    'chrome_crashpad_handler=%A',
    'df -h /dev/shm',
    'PATH candidate $candidate=$resolved'
)) {
    if (-not $linuxJob.Contains($diagnosticContract)) {
        throw "Linux certification is missing bounded browser diagnostic: $diagnosticContract"
    }
}
foreach ($startupContract in @(
    '$env:SE_BROWSER_DIAGNOSTICS_DIR = Join-Path $tempRoot "browser-diagnostics"',
    '$env:SE_BROWSER_PROFILE_ROOT = Join-Path $tempRoot "browser-profiles"',
    'Invoke-TestLensChromeStartupSmoke',
    '-BrowserBinary $env:SE_BROWSER_PATH',
    '-NoSandbox'
)) {
    if (-not $linuxJob.Contains($startupContract)) {
        throw "Linux certification is missing Chrome startup evidence contract: $startupContract"
    }
}
if ($ciWorkflow.Contains('check-public-api-since.ps1 -BaselineTag v0.3.1 -ExpectedSince 0.4.0')) {
    throw "Current API since gate must derive its release line instead of using the 0.4.0 baseline"
}
$certificationSource = [IO.File]::ReadAllText((Join-Path $PSScriptRoot "certify-0.5.0-external-consumer.ps1"))
foreach ($diagnosticContract in @('api/status', 'api/project', 'api/config', 'readOnlyState=', '4096', 'Get-TestLensChromeDriverDiagnosticTail', 'browserDiagnostics=')) {
    if (-not $certificationSource.Contains($diagnosticContract)) {
        throw "External certification failure diagnostic is missing contract: $diagnosticContract"
    }
}

$browserDiagnosticRoot = Join-Path $testRoot "browser diagnostics"
[IO.Directory]::CreateDirectory($browserDiagnosticRoot) | Out-Null
$browserDiagnosticLog = Join-Path $browserDiagnosticRoot "chromedriver-contract.log"
[IO.File]::WriteAllText($browserDiagnosticLog, (("prefix`n" * 200) + "token=do-not-leak`nChrome startup failure"))
$browserDiagnosticTail = Get-TestLensChromeDriverDiagnosticTail -Directory $browserDiagnosticRoot -MaximumCharacters 512
if ($browserDiagnosticTail.Contains("do-not-leak") -or -not $browserDiagnosticTail.Contains("[REDACTED]") -or -not $browserDiagnosticTail.Contains("Chrome startup failure")) {
    throw "ChromeDriver diagnostic tail must be bounded, redacted, and preserve the failure tail"
}

$readSequence = [pscustomobject]@{ Count = 0 }
$ready = Wait-TestLensStudioActionAvailable -Action "MAP_APPLICATION" -Timeout ([TimeSpan]::FromSeconds(1)) -PollMilliseconds 1 -ReadState {
    $readSequence.Count++
    if ($readSequence.Count -eq 1) {
        return [pscustomobject]@{ OperationRunning = $true; AvailableActions = @() }
    }
    return [pscustomobject]@{ OperationRunning = $false; AvailableActions = @("MAP_APPLICATION") }
}
if ($readSequence.Count -ne 2 -or $ready.OperationRunning) {
    throw "Studio action availability wait did not observe backend readiness"
}

$timeoutWatch = [Diagnostics.Stopwatch]::StartNew()
try {
    Wait-TestLensStudioActionAvailable -Action "MAP_APPLICATION" -Timeout ([TimeSpan]::FromMilliseconds(25)) -PollMilliseconds 2 -ReadState {
        [pscustomobject]@{ OperationRunning = $true; AvailableActions = @("SCAN_PROJECT") }
    } | Out-Null
    throw "Expected Studio action availability timeout"
} catch {
    if ($_.Exception.Message -notlike "Timed out waiting for Studio action 'MAP_APPLICATION'; operationRunning=True; availableActions=SCAN_PROJECT; timeoutMs=25*") {
        throw "Studio action timeout diagnostic regression: $($_.Exception.Message)"
    }
} finally {
    $timeoutWatch.Stop()
}
if ($timeoutWatch.Elapsed -gt [TimeSpan]::FromSeconds(2)) {
    throw "Studio action availability timeout was not bounded"
}

function Assert-Throws([scriptblock]$Action, [string]$Name) {
    try { & $Action | Out-Null } catch { return }
    throw "Expected failure: $Name"
}

function Assert-ThrowsLike([scriptblock]$Action, [string]$Pattern, [string]$Name) {
    try { & $Action | Out-Null } catch {
        if ($_.Exception.Message -like $Pattern) { return }
        throw "Expected failure '$Name' matching '$Pattern', got: $($_.Exception.Message)"
    }
    throw "Expected failure: $Name"
}

try {
    New-Item -ItemType Directory -Force -Path $fixtureRoot, $releaseSourceRoot, $consumerRoot | Out-Null
    foreach ($relative in @(git -C $repositoryRoot ls-files -- "pom.xml" ":(glob)**/pom.xml")) {
        $destination = Join-Path $fixtureRoot $relative
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
        Copy-Item -LiteralPath (Join-Path $repositoryRoot $relative) -Destination $destination
    }
    & git -C $fixtureRoot init -q
    & git -C $fixtureRoot add -- .
    if ($LASTEXITCODE -ne 0) { throw "Cannot initialize release-tooling fixture" }
    & git -C $fixtureRoot -c user.name=release-fixture -c user.email=release-fixture.invalid commit -q -m initial
    if ($LASTEXITCODE -ne 0) { throw "Cannot commit release-tooling fixture" }
    & git -C $fixtureRoot tag v0.4.0
    if ($LASTEXITCODE -ne 0) { throw "Cannot create historical release tag in release-tooling fixture" }

    foreach ($relative in @(& git -C $fixtureRoot ls-files -- "pom.xml" ":(glob)**/pom.xml")) {
        $destination = Join-Path $releaseSourceRoot $relative
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
        Copy-Item -LiteralPath (Join-Path $fixtureRoot $relative) -Destination $destination
    }
    [IO.File]::WriteAllText((Join-Path $consumerRoot "pom.xml"), "<project/>")
    if (Test-Path -LiteralPath (Join-Path $releaseSourceRoot ".git")) {
        throw "Clean-room release source fixture must not contain .git"
    }
    if (Test-Path -LiteralPath (Join-Path $consumerRoot ".git")) {
        throw "Consumer fixture must not contain .git"
    }

    foreach ($root in @($fixtureRoot, $releaseSourceRoot)) {
        New-Item -ItemType Directory -Force -Path (Join-Path $root "target/generated") | Out-Null
        [IO.File]::WriteAllText((Join-Path $root "target/generated/pom.xml"), "<project/>")
        New-Item -ItemType Directory -Force -Path (Join-Path $root "nested disposable fixture") | Out-Null
        [IO.File]::WriteAllText((Join-Path $root "nested disposable fixture/pom.xml"), "<project/>")
    }

    Push-Location $consumerRoot
    try {
        $model = Get-TestLensReactorModel `
            -RepositoryRoot $fixtureRoot `
            -ReleaseSourceRoot $releaseSourceRoot `
            -IncludeBrowserIt
        $releasePom = Read-TestLensPom (Join-Path $fixtureRoot "pom.xml")
        if ((Get-TestLensPomText $releasePom "/m:project/m:version") -ne "0.5.0") {
            throw "Release certification version must be derived as 0.5.0 from the root POM"
        }
        if ($model.TrackedPomPaths -contains "target/generated/pom.xml") {
            throw "Generated target POM was included in tracked reactor metadata"
        }
        if ($model.TrackedPomPaths -contains "nested disposable fixture/pom.xml") {
            throw "Untracked fixture POM was included in tracked reactor metadata"
        }
        if ($model.TrackedPomPaths -notcontains "scripts/fixtures/s15-external-consumer/pom.xml") {
            throw "Tracked clean-room fixture POM was not observed by the release model"
        }
        $normal = @(& (Join-Path $PSScriptRoot "check-reactor-versions.ps1") `
            -RepositoryRoot $fixtureRoot -ReleaseSourceRoot $releaseSourceRoot)
        $browser = @(& (Join-Path $PSScriptRoot "check-reactor-versions.ps1") `
            -RepositoryRoot $fixtureRoot -ReleaseSourceRoot $releaseSourceRoot -IncludeBrowserIt)
        if ($normal[-1] -notmatch "$($model.NormalProjectCount) projects") { throw "Normal reactor count regression" }
        if ($browser[-1] -notmatch "$($model.BrowserProjectCount) projects") { throw "Browser reactor count regression" }
        & (Join-Path $PSScriptRoot "validate-release-packaging.ps1") `
            -RepositoryRoot $fixtureRoot `
            -ReleaseSourceRoot $releaseSourceRoot `
            -MatrixOnly | Out-Null
    } finally {
        Pop-Location
    }
    Assert-ThrowsLike {
        Get-TestLensReactorModel -RepositoryRoot $consumerRoot
    } "*RepositoryRoot is not a Git worktree*" "non-Git RepositoryRoot"

    $corePom = Join-Path $fixtureRoot "selenium-test-lens-core/pom.xml"
    $originalCore = [IO.File]::ReadAllText($corePom)
    [IO.File]::WriteAllText($corePom, $originalCore.Replace('<artifactId>selenium-test-lens-core</artifactId>', '<artifactId>selenium-test-lens-core</artifactId><properties><maven.deploy.skip>true</maven.deploy.skip></properties>'))
    Assert-Throws { & (Join-Path $PSScriptRoot "validate-release-packaging.ps1") -RepositoryRoot $fixtureRoot -MatrixOnly } "published module deploy.skip"
    [IO.File]::WriteAllText($corePom, $originalCore)

    $internalPom = Join-Path $fixtureRoot "selenium-test-lens-compatibility-engine/pom.xml"
    $originalInternal = [IO.File]::ReadAllText($internalPom)
    [IO.File]::WriteAllText($internalPom, $originalInternal.Replace('<maven.deploy.skip>true</maven.deploy.skip>', '<maven.deploy.skip>false</maven.deploy.skip>'))
    Assert-Throws { & (Join-Path $PSScriptRoot "validate-release-packaging.ps1") -RepositoryRoot $fixtureRoot -MatrixOnly } "internal module deploy enabled"
    [IO.File]::WriteAllText($internalPom, $originalInternal)

    [IO.File]::WriteAllText($corePom, $originalCore.Replace('<version>0.5.0</version>', '<version>9.9.9</version>'))
    Assert-Throws { & (Join-Path $PSScriptRoot "check-reactor-versions.ps1") -RepositoryRoot $fixtureRoot } "wrong module version"
    [IO.File]::WriteAllText($corePom, $originalCore)

    $trackedPoms = @(& git -C $fixtureRoot ls-files -- "pom.xml" ":(glob)**/pom.xml")
    foreach ($relative in $trackedPoms) {
        $path = Join-Path $fixtureRoot $relative
        [IO.File]::WriteAllText($path, ([IO.File]::ReadAllText($path)).Replace("0.5.0", "0.5.1"))
    }
    Assert-ThrowsLike {
        & (Join-Path $PSScriptRoot "check-reactor-versions.ps1") -RepositoryRoot $fixtureRoot
    } "*must equal publication policy latestReleasedVersion '0.5.0'*" "release version differs from policy"
    foreach ($relative in $trackedPoms) {
        $path = Join-Path $fixtureRoot $relative
        [IO.File]::WriteAllText($path, ([IO.File]::ReadAllText($path)).Replace("0.5.1", "0.5.0-SNAPSHOT"))
    }
    $snapshotPom = Read-TestLensPom (Join-Path $fixtureRoot "pom.xml")
    if ((Get-TestLensPomText $snapshotPom "/m:project/m:version") -ne "0.5.0-SNAPSHOT") {
        throw "Development certification version must be derived as 0.5.0-SNAPSHOT from the root POM"
    }
    Assert-ThrowsLike {
        & (Join-Path $PSScriptRoot "check-reactor-versions.ps1") -RepositoryRoot $fixtureRoot
    } "*must be newer than declared released version '0.5.0'*" "development version reuses declared release"
    foreach ($relative in $trackedPoms) {
        $path = Join-Path $fixtureRoot $relative
        [IO.File]::WriteAllText($path, ([IO.File]::ReadAllText($path)).Replace("0.5.0-SNAPSHOT", "0.5.0"))
    }

    $browserPom = Join-Path $fixtureRoot "selenium-test-lens-browser-tests/pom.xml"
    Move-Item -LiteralPath $browserPom -Destination "$browserPom.missing"
    Assert-Throws { & (Join-Path $PSScriptRoot "check-reactor-versions.ps1") -RepositoryRoot $fixtureRoot -IncludeBrowserIt } "missing profile module"
    Move-Item -LiteralPath "$browserPom.missing" -Destination $browserPom

    Remove-Item -LiteralPath (Join-Path $fixtureRoot "target") -Recurse -Force
    Remove-Item -LiteralPath (Join-Path $fixtureRoot "nested disposable fixture") -Recurse -Force
    if (@(& git -C $fixtureRoot status --porcelain).Count -ne 0) { throw "Release-tooling fixture was not restored to a clean state" }

    Assert-Throws { & (Join-Path $PSScriptRoot "prepare-patch-release.ps1") -RepositoryRoot $fixtureRoot -TargetVersion "invalid" -WhatIf } "invalid patch target"
    Assert-Throws { & (Join-Path $PSScriptRoot "prepare-patch-release.ps1") -RepositoryRoot $fixtureRoot -TargetVersion "0.5.0" -WhatIf } "non-patch target"
    foreach ($pomPath in @(git -C $fixtureRoot ls-files -- "pom.xml" ":(glob)**/pom.xml")) {
        $path = Join-Path $fixtureRoot $pomPath
        [IO.File]::WriteAllText($path, ([IO.File]::ReadAllText($path)).Replace("0.5.0", "0.5.1-SNAPSHOT"))
    }
    & git -C $fixtureRoot add -- pom.xml */pom.xml
    & git -C $fixtureRoot -c user.name=release-fixture -c user.email=release-fixture.invalid commit -q -m patch-snapshot
    if ($LASTEXITCODE -ne 0) { throw "Cannot commit patch snapshot fixture" }
    & (Join-Path $PSScriptRoot "prepare-patch-release.ps1") -RepositoryRoot $fixtureRoot -TargetVersion "0.5.1" -WhatIf | Out-Null
    if (@(& git -C $fixtureRoot status --porcelain).Count -ne 0) { throw "Patch preparation -WhatIf mutated the fixture" }
    $cleanCore = [IO.File]::ReadAllText($corePom)
    Add-Content -LiteralPath $corePom -Value "dirty"
    Assert-Throws { & (Join-Path $PSScriptRoot "prepare-patch-release.ps1") -RepositoryRoot $fixtureRoot -TargetVersion "0.5.1" -WhatIf } "dirty worktree"
    [IO.File]::WriteAllText($corePom, $cleanCore)
    $rootPom = Join-Path $fixtureRoot "pom.xml"
    $rootText = [IO.File]::ReadAllText($rootPom)
    [IO.File]::WriteAllText($rootPom, $rootText.Replace("0.5.1-SNAPSHOT", "0.5.1"))
    Assert-Throws { & (Join-Path $PSScriptRoot "prepare-patch-release.ps1") -RepositoryRoot $fixtureRoot -TargetVersion "0.5.1" -WhatIf } "wrong source snapshot"
    [IO.File]::WriteAllText($rootPom, $rootText)

    foreach ($script in @("prepare-patch-release.ps1","check-patch-release.ps1")) {
        $text = [IO.File]::ReadAllText((Join-Path $PSScriptRoot $script))
        foreach ($forbidden in @("git push","git tag","git commit","mvn deploy","publish-versioned-docs")) {
            if ($text.Contains($forbidden)) { throw "$script contains forbidden release action '$forbidden'" }
        }
    }
    Write-Output "Release tooling fixture PASS: explicit Git repository root, Git-free release/consumer roots, deterministic tracked reactor, profile count, version failures, publication policy, and no automated release actions"
} finally {
    if (Test-Path -LiteralPath $testRoot) { Remove-Item -LiteralPath $testRoot -Recurse -Force }
}
