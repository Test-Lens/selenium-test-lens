$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$unicodeSuffix = ([char]0x017B).ToString() + ([char]0x00F3) + ([char]0x0142) + ([char]0x0107)
$testRoot = Join-Path ([IO.Path]::GetTempPath()) ("test-lens release fixture $unicodeSuffix " + [guid]::NewGuid())
$fixtureRoot = Join-Path $testRoot "source repository"
$releaseSourceRoot = Join-Path $testRoot "release source without git"
$consumerRoot = Join-Path $testRoot "consumer without git"
Import-Module (Join-Path $PSScriptRoot "ReleaseReactor.psm1") -Force

$publicationPolicy = Read-TestLensPublicationPolicy (Join-Path $PSScriptRoot "config/publication-policy.json")
if ($publicationPolicy.LatestReleasedVersion -ne "0.4.0") {
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

    [IO.File]::WriteAllText($corePom, $originalCore.Replace('<version>0.5.0-SNAPSHOT</version>', '<version>9.9.9</version>'))
    Assert-Throws { & (Join-Path $PSScriptRoot "check-reactor-versions.ps1") -RepositoryRoot $fixtureRoot } "wrong module version"
    [IO.File]::WriteAllText($corePom, $originalCore)

    $trackedPoms = @(& git -C $fixtureRoot ls-files -- "pom.xml" ":(glob)**/pom.xml")
    foreach ($relative in $trackedPoms) {
        $path = Join-Path $fixtureRoot $relative
        [IO.File]::WriteAllText($path, ([IO.File]::ReadAllText($path)).Replace("0.5.0-SNAPSHOT", "0.4.0-SNAPSHOT"))
    }
    Assert-ThrowsLike {
        & (Join-Path $PSScriptRoot "check-reactor-versions.ps1") -RepositoryRoot $fixtureRoot
    } "*must be newer than latest local release tag 'v0.4.0'*" "development version reuses latest release"
    foreach ($relative in $trackedPoms) {
        $path = Join-Path $fixtureRoot $relative
        [IO.File]::WriteAllText($path, ([IO.File]::ReadAllText($path)).Replace("0.4.0-SNAPSHOT", "0.5.0-SNAPSHOT"))
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
        [IO.File]::WriteAllText($path, ([IO.File]::ReadAllText($path)).Replace("0.5.0-SNAPSHOT", "0.5.1-SNAPSHOT"))
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
