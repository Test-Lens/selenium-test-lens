[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = "High")]
param(
    [Parameter(Mandatory = $true)][string]$TargetVersion,
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"
Import-Module (Join-Path $PSScriptRoot "ReleaseReactor.psm1") -Force
$root = [IO.Path]::GetFullPath($RepositoryRoot)
$model = Get-TestLensReactorModel -RepositoryRoot $root -IncludeBrowserIt
if ($TargetVersion -notmatch '^\d+\.\d+\.\d+$') { throw "TargetVersion must be an exact non-SNAPSHOT semantic version" }
if ([int]($TargetVersion.Split('.')[2]) -lt 1) { throw "TargetVersion must identify a 0.4.x patch release (patch component >= 1)" }
if ($model.RootVersion -notmatch '^(\d+)\.(\d+)\.(\d+)-SNAPSHOT$') { throw "Current version '$($model.RootVersion)' is not a patch-development SNAPSHOT" }
$expectedTarget = "$($Matches[1]).$($Matches[2]).$($Matches[3])"
if ($TargetVersion -ne $expectedTarget) { throw "TargetVersion '$TargetVersion' must match current development version '$expectedTarget'" }
$status = @(& git -C $root status --porcelain 2>&1)
if ($LASTEXITCODE -ne 0) { throw "git status failed: $($status -join ' ')" }
if ($status.Count -gt 0) { throw "Patch release preparation requires a clean worktree" }
$tag = "v$TargetVersion"
& git -C $root show-ref --verify --quiet "refs/tags/$tag"
if ($LASTEXITCODE -eq 0) { throw "Local tag '$tag' already exists" }

foreach ($project in $model.Projects) {
    $path = Join-Path $root ($project.RelativePom.Replace('/', [IO.Path]::DirectorySeparatorChar))
    $text = [IO.File]::ReadAllText($path)
    $pattern = '<version>\s*' + [regex]::Escape($model.RootVersion) + '\s*</version>'
    $updated = [regex]::Replace($text, $pattern, "<version>$TargetVersion</version>")
    if ($updated -eq $text) { throw "No project/parent version '$($model.RootVersion)' found in $($project.RelativePom)" }
    if ($PSCmdlet.ShouldProcess($project.RelativePom, "set Maven project/parent version to $TargetVersion")) { [IO.File]::WriteAllText($path, $updated, [Text.UTF8Encoding]::new($false)) }
}

$mkdocs = Join-Path $root "mkdocs.yml"
if ([IO.File]::Exists($mkdocs)) {
    $text = [IO.File]::ReadAllText($mkdocs)
    $updated = $text -replace '(?m)^    current: .+$', "    current: $TargetVersion" -replace '(?m)^    is_dev: .+$', '    is_dev: false' -replace '(?m)^    is_snapshot: .+$', '    is_snapshot: false'
    if ($PSCmdlet.ShouldProcess("mkdocs.yml", "set release documentation version to $TargetVersion")) { [IO.File]::WriteAllText($mkdocs, $updated, [Text.UTF8Encoding]::new($false)) }
}
Write-Output "Prepared tracked reactor POMs and mkdocs.yml for $TargetVersion. Review changelog/docs, run check-patch-release.ps1, then commit/tag/publish manually with separate authorization."
