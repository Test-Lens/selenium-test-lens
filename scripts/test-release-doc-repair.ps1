$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$testRoot = Join-Path ([IO.Path]::GetTempPath()) ("test-lens-doc-repair-" + [guid]::NewGuid().ToString("N"))

function Assert-Throws([scriptblock]$Action, [string]$Name) {
    try { & $Action | Out-Null } catch { return }
    throw "Expected release documentation repair failure: $Name"
}

try {
    New-Item -ItemType Directory -Force -Path (Join-Path $testRoot "docs"), (Join-Path $testRoot "scripts") | Out-Null
    Copy-Item -LiteralPath (Join-Path $root "scripts/ReleaseReactor.psm1") -Destination (Join-Path $testRoot "scripts/ReleaseReactor.psm1")
    [IO.File]::WriteAllText((Join-Path $testRoot "pom.xml"), @'
<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>fixture</artifactId><version>0.5.0</version></project>
'@)
    [IO.File]::WriteAllText((Join-Path $testRoot "docs/index.md"), "release docs")
    & git -C $testRoot init -q
    & git -C $testRoot add -- .
    & git -C $testRoot -c user.name=docs-fixture -c user.email=docs-fixture.invalid commit -q -m release
    & git -C $testRoot tag v0.5.0

    Add-Content -LiteralPath (Join-Path $testRoot "docs/index.md") -Value "documentation repair"
    & git -C $testRoot add -- docs/index.md
    & git -C $testRoot -c user.name=docs-fixture -c user.email=docs-fixture.invalid commit -q -m docs-repair
    & (Join-Path $root "scripts/check-release-doc-repair.ps1") -Version 0.5.0 -SourceRef release/0.5.0 -RepositoryRoot $testRoot | Out-Null

    Assert-Throws {
        & (Join-Path $root "scripts/check-release-doc-repair.ps1") -Version 0.5.0 -SourceRef main -RepositoryRoot $testRoot
    } "wrong source ref"

    New-Item -ItemType Directory -Force -Path (Join-Path $testRoot "src/main/java") | Out-Null
    [IO.File]::WriteAllText((Join-Path $testRoot "src/main/java/Product.java"), "final class Product {}")
    & git -C $testRoot add -- src/main/java/Product.java
    & git -C $testRoot -c user.name=docs-fixture -c user.email=docs-fixture.invalid commit -q -m forbidden-product-change
    Assert-Throws {
        & (Join-Path $root "scripts/check-release-doc-repair.ps1") -Version 0.5.0 -SourceRef release/0.5.0 -RepositoryRoot $testRoot
    } "product change"

    Write-Host "Release documentation repair contract PASS: exact release identity, approved source ref, and docs-only diff enforced."
} finally {
    if (Test-Path -LiteralPath $testRoot) { Remove-Item -LiteralPath $testRoot -Recurse -Force -ErrorAction SilentlyContinue }
}
