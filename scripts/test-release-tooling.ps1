$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$unicodeSuffix = ([char]0x017B).ToString() + ([char]0x00F3) + ([char]0x0142) + ([char]0x0107)
$fixtureRoot = Join-Path ([IO.Path]::GetTempPath()) ("test-lens release fixture $unicodeSuffix " + [guid]::NewGuid())

function Assert-Throws([scriptblock]$Action, [string]$Name) {
    try { & $Action | Out-Null } catch { return }
    throw "Expected failure: $Name"
}

try {
    New-Item -ItemType Directory -Path $fixtureRoot | Out-Null
    foreach ($relative in @(git -C $repositoryRoot ls-files -- "pom.xml" ":(glob)**/pom.xml")) {
        $destination = Join-Path $fixtureRoot $relative
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
        Copy-Item -LiteralPath (Join-Path $repositoryRoot $relative) -Destination $destination
    }
    & git -C $fixtureRoot init -q
    & git -C $fixtureRoot add -- pom.xml */pom.xml
    if ($LASTEXITCODE -ne 0) { throw "Cannot initialize release-tooling fixture" }
    & git -C $fixtureRoot -c user.name=release-fixture -c user.email=release-fixture.invalid commit -q -m initial
    if ($LASTEXITCODE -ne 0) { throw "Cannot commit release-tooling fixture" }

    New-Item -ItemType Directory -Force -Path (Join-Path $fixtureRoot "target/generated") | Out-Null
    [IO.File]::WriteAllText((Join-Path $fixtureRoot "target/generated/pom.xml"), "<project/>")
    New-Item -ItemType Directory -Force -Path (Join-Path $fixtureRoot "nested disposable fixture") | Out-Null
    [IO.File]::WriteAllText((Join-Path $fixtureRoot "nested disposable fixture/pom.xml"), "<project/>")
    $normal = @(& (Join-Path $PSScriptRoot "check-reactor-versions.ps1") -RepositoryRoot $fixtureRoot)
    $browser = @(& (Join-Path $PSScriptRoot "check-reactor-versions.ps1") -RepositoryRoot $fixtureRoot -IncludeBrowserIt)
    if ($normal[-1] -notmatch '16 projects') { throw "Normal reactor count regression" }
    if ($browser[-1] -notmatch '17 projects') { throw "Browser reactor count regression" }
    & (Join-Path $PSScriptRoot "validate-release-packaging.ps1") -RepositoryRoot $fixtureRoot -MatrixOnly | Out-Null

    $corePom = Join-Path $fixtureRoot "selenium-test-lens-core/pom.xml"
    $originalCore = [IO.File]::ReadAllText($corePom)
    [IO.File]::WriteAllText($corePom, $originalCore.Replace('<artifactId>selenium-test-lens-core</artifactId>', '<artifactId>selenium-test-lens-core</artifactId><properties><maven.deploy.skip>true</maven.deploy.skip></properties>'))
    Assert-Throws { & (Join-Path $PSScriptRoot "validate-release-packaging.ps1") -RepositoryRoot $fixtureRoot -MatrixOnly } "published module deploy.skip"
    [IO.File]::WriteAllText($corePom, $originalCore)

    $internalPom = Join-Path $fixtureRoot "selenium-test-lens-selector-engine/pom.xml"
    $originalInternal = [IO.File]::ReadAllText($internalPom)
    [IO.File]::WriteAllText($internalPom, $originalInternal.Replace('<maven.deploy.skip>true</maven.deploy.skip>', '<maven.deploy.skip>false</maven.deploy.skip>'))
    Assert-Throws { & (Join-Path $PSScriptRoot "validate-release-packaging.ps1") -RepositoryRoot $fixtureRoot -MatrixOnly } "internal module deploy enabled"
    [IO.File]::WriteAllText($internalPom, $originalInternal)

    [IO.File]::WriteAllText($corePom, $originalCore.Replace('0.4.0-SNAPSHOT', '9.9.9-SNAPSHOT'))
    Assert-Throws { & (Join-Path $PSScriptRoot "check-reactor-versions.ps1") -RepositoryRoot $fixtureRoot } "wrong module version"
    [IO.File]::WriteAllText($corePom, $originalCore)

    $browserPom = Join-Path $fixtureRoot "selenium-test-lens-browser-tests/pom.xml"
    Move-Item -LiteralPath $browserPom -Destination "$browserPom.missing"
    Assert-Throws { & (Join-Path $PSScriptRoot "check-reactor-versions.ps1") -RepositoryRoot $fixtureRoot -IncludeBrowserIt } "missing profile module"
    Move-Item -LiteralPath "$browserPom.missing" -Destination $browserPom

    Remove-Item -LiteralPath (Join-Path $fixtureRoot "target") -Recurse -Force
    Remove-Item -LiteralPath (Join-Path $fixtureRoot "nested disposable fixture") -Recurse -Force
    if (@(& git -C $fixtureRoot status --porcelain).Count -ne 0) { throw "Release-tooling fixture was not restored to a clean state" }

    Assert-Throws { & (Join-Path $PSScriptRoot "prepare-patch-release.ps1") -RepositoryRoot $fixtureRoot -TargetVersion "invalid" -WhatIf } "invalid patch target"
    Assert-Throws { & (Join-Path $PSScriptRoot "prepare-patch-release.ps1") -RepositoryRoot $fixtureRoot -TargetVersion "0.4.0" -WhatIf } "non-patch target"
    foreach ($pomPath in @(git -C $fixtureRoot ls-files -- "pom.xml" ":(glob)**/pom.xml")) {
        $path = Join-Path $fixtureRoot $pomPath
        [IO.File]::WriteAllText($path, ([IO.File]::ReadAllText($path)).Replace("0.4.0-SNAPSHOT", "0.4.1-SNAPSHOT"))
    }
    & git -C $fixtureRoot add -- pom.xml */pom.xml
    & git -C $fixtureRoot -c user.name=release-fixture -c user.email=release-fixture.invalid commit -q -m patch-snapshot
    if ($LASTEXITCODE -ne 0) { throw "Cannot commit patch snapshot fixture" }
    & (Join-Path $PSScriptRoot "prepare-patch-release.ps1") -RepositoryRoot $fixtureRoot -TargetVersion "0.4.1" -WhatIf | Out-Null
    if (@(& git -C $fixtureRoot status --porcelain).Count -ne 0) { throw "Patch preparation -WhatIf mutated the fixture" }
    $cleanCore = [IO.File]::ReadAllText($corePom)
    Add-Content -LiteralPath $corePom -Value "dirty"
    Assert-Throws { & (Join-Path $PSScriptRoot "prepare-patch-release.ps1") -RepositoryRoot $fixtureRoot -TargetVersion "0.4.1" -WhatIf } "dirty worktree"
    [IO.File]::WriteAllText($corePom, $cleanCore)
    $rootPom = Join-Path $fixtureRoot "pom.xml"
    $rootText = [IO.File]::ReadAllText($rootPom)
    [IO.File]::WriteAllText($rootPom, $rootText.Replace("0.4.1-SNAPSHOT", "0.4.1"))
    Assert-Throws { & (Join-Path $PSScriptRoot "prepare-patch-release.ps1") -RepositoryRoot $fixtureRoot -TargetVersion "0.4.1" -WhatIf } "wrong source snapshot"
    [IO.File]::WriteAllText($rootPom, $rootText)

    foreach ($script in @("prepare-patch-release.ps1","check-patch-release.ps1")) {
        $text = [IO.File]::ReadAllText((Join-Path $PSScriptRoot $script))
        foreach ($forbidden in @("git push","git tag","git commit","mvn deploy","publish-versioned-docs")) {
            if ($text.Contains($forbidden)) { throw "$script contains forbidden release action '$forbidden'" }
        }
    }
    Write-Output "Release tooling fixture PASS: deterministic tracked reactor, profile count, version failures, publication policy, and no automated release actions"
} finally {
    if (Test-Path -LiteralPath $fixtureRoot) { Remove-Item -LiteralPath $fixtureRoot -Recurse -Force }
}
