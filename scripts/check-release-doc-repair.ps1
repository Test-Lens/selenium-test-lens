param(
    [Parameter(Mandatory = $true)][string]$Version,
    [Parameter(Mandatory = $true)][string]$SourceRef,
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"
if ($Version -notmatch '^\d+\.\d+\.\d+$') { throw "Release documentation repair version must be MAJOR.MINOR.PATCH." }
$expectedSourceRef = "release/$Version"
if ($SourceRef -ne $expectedSourceRef) { throw "Release documentation repair source must be '$expectedSourceRef'." }

$root = [IO.Path]::GetFullPath($RepositoryRoot)
$releaseTag = "v$Version"
& git -C $root rev-parse --verify "$releaseTag^{commit}" | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Release documentation repair requires immutable tag '$releaseTag'." }

Import-Module (Join-Path $root "scripts/ReleaseReactor.psm1") -Force
$pom = Read-TestLensPom (Join-Path $root "pom.xml")
$sourceVersion = Get-TestLensPomText $pom "/m:project/m:version"
if ($sourceVersion -ne $Version) {
    throw "Release documentation repair source version '$sourceVersion' does not match '$Version'."
}

$allowedExact = @(
    ".github/workflows/docs.yml",
    "mkdocs.yml",
    "mkdocs-release.yml",
    "mkdocs-0.1.0.yml",
    "requirements-docs.txt",
    "scripts/check-ai-integration-builder.ps1",
    "scripts/check-doc-links.ps1",
    "scripts/check-doc-seo.ps1",
    "scripts/check-doc-signatures.ps1",
    "scripts/check-doc-version-boundary.ps1",
    "scripts/check-hud-demo.ps1",
    "scripts/check-product-overview.ps1",
    "scripts/check-release-doc-repair.ps1",
    "scripts/publish-versioned-docs.ps1",
    "scripts/test-release-doc-repair.ps1",
    "scripts/validate-stable-doc-examples.ps1",
    "scripts/validate-versioned-docs.ps1"
)
$allowedPrefixes = @("docs/", "docs-versions/", "docs-versioning/", "overrides/")
$changed = @(& git -C $root diff --name-only "$releaseTag..HEAD" --)
if ($LASTEXITCODE -ne 0) { throw "Cannot compare release documentation repair with '$releaseTag'." }
$blocked = @($changed | ForEach-Object { $_.Trim().Replace('\', '/') } | Where-Object {
    $path = $_
    $path -and $path -notin $allowedExact -and -not ($allowedPrefixes | Where-Object { $path.StartsWith($_, [StringComparison]::Ordinal) })
})
if ($blocked.Count -gt 0) {
    throw "Release documentation repair changes forbidden product/build paths: $($blocked -join ', ')"
}

Write-Host "Release documentation repair source OK: $SourceRef describes $Version and changes only approved documentation/release-support paths."
