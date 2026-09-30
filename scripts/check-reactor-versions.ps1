param(
    [string]$ExpectedVersion,
    [switch]$IncludeBrowserIt,
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$ReleaseSourceRoot
)

$ErrorActionPreference = "Stop"
Import-Module (Join-Path $PSScriptRoot "ReleaseReactor.psm1") -Force
$model = Get-TestLensReactorModel `
    -RepositoryRoot $RepositoryRoot `
    -ReleaseSourceRoot $ReleaseSourceRoot `
    -IncludeBrowserIt:$IncludeBrowserIt
if ([string]::IsNullOrWhiteSpace($ExpectedVersion)) { $ExpectedVersion = $model.RootVersion }
if ($model.RootVersion -ne $ExpectedVersion) { throw "Root project version is '$($model.RootVersion)', expected '$ExpectedVersion'" }
foreach ($project in $model.Projects) {
    if ($project.Version -ne $ExpectedVersion) { throw "Maven project '$($project.RelativePom)' reports '$($project.Version)', expected '$ExpectedVersion'" }
    if ($project.RelativePom -ne "pom.xml" -and $project.ParentVersion -ne $ExpectedVersion) { throw "Maven module '$($project.RelativePom)' uses parent version '$($project.ParentVersion)', expected '$ExpectedVersion'" }
    Write-Output "$($project.RelativePom)`t$($project.Version)"
}
$mode = if ($IncludeBrowserIt) { "normal + browser-it" } else { "normal" }
Write-Output "Reactor version OK: $($model.Projects.Count) projects ($mode) use $ExpectedVersion"
