param(
    [Parameter(Mandatory = $true)][string]$ExpectedVersion,
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot),
    [switch]$SkipApiChecks
)

$ErrorActionPreference = "Stop"
if ($ExpectedVersion -notmatch '^\d+\.\d+\.\d+$') { throw "ExpectedVersion must be an exact non-SNAPSHOT semantic version" }
if ([int]($ExpectedVersion.Split('.')[2]) -lt 1) { throw "ExpectedVersion must identify a 0.4.x patch release (patch component >= 1)" }
$root = [IO.Path]::GetFullPath($RepositoryRoot)
& (Join-Path $PSScriptRoot "check-reactor-versions.ps1") -RepositoryRoot $root -ExpectedVersion $ExpectedVersion
if ($LASTEXITCODE -ne 0) { throw "Normal reactor version validation failed" }
& (Join-Path $PSScriptRoot "check-reactor-versions.ps1") -RepositoryRoot $root -ExpectedVersion $ExpectedVersion -IncludeBrowserIt
if ($LASTEXITCODE -ne 0) { throw "Browser reactor version validation failed" }
& (Join-Path $PSScriptRoot "validate-release-packaging.ps1") -RepositoryRoot $root -Version $ExpectedVersion -MatrixOnly
if ($LASTEXITCODE -ne 0) { throw "Publication matrix validation failed" }
$mkdocs = [IO.File]::ReadAllText((Join-Path $root "mkdocs.yml"))
if ($mkdocs -notmatch "(?m)^    current: $([regex]::Escape($ExpectedVersion))$") { throw "mkdocs.yml current version is not $ExpectedVersion" }
$changelog = [IO.File]::ReadAllText((Join-Path $root "CHANGELOG.md"))
if ($changelog -notmatch "(?m)^## \[$([regex]::Escape($ExpectedVersion))\]") { throw "CHANGELOG.md lacks a $ExpectedVersion release section" }
& git -C $root show-ref --verify --quiet "refs/tags/v$ExpectedVersion"
if ($LASTEXITCODE -eq 0) { throw "Local tag v$ExpectedVersion already exists" }
if (-not $SkipApiChecks) {
    & (Join-Path $PSScriptRoot "check-public-api.ps1")
    if ($LASTEXITCODE -ne 0) { throw "Public API inventory failed" }
    & (Join-Path $PSScriptRoot "check-public-api-since.ps1") -BaselineTag v0.3.1 -ExpectedSince 0.4.0
    if ($LASTEXITCODE -ne 0) { throw "Public API @since validation failed" }
}
Write-Output "Patch release check PASS for $ExpectedVersion. This script did not commit, tag, push, publish, or deploy documentation."
