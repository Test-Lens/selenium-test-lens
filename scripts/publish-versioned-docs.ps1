param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("dev", "release", "bootstrap-0.1.0")]
    [string]$Operation,
    [string]$Version,
    [string]$Confirmation,
    [string]$Branch = "gh-pages",
    [switch]$NoPush
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $push = if ($NoPush) { @() } else { @("--push") }
    $listed = (& mike list --branch $Branch 2>$null) -join "`n"
    if ($Operation -eq "dev") {
        if ([string]::IsNullOrWhiteSpace($Version) -or -not $Version.EndsWith("-SNAPSHOT")) {
            throw "dev publication requires the -SNAPSHOT version read from the root POM."
        }
        & mike deploy --branch $Branch @push --update-aliases --title "$Version / coming soon" dev
        if ($LASTEXITCODE -ne 0) { throw "mike failed to update dev; gh-pages was not force-pushed." }
        return
    }

    if ($Operation -eq "bootstrap-0.1.0") {
        if ($Confirmation -ne "publish-immutable-0.1.0") { throw "Exact bootstrap confirmation is required." }
        $Version = "0.1.0"
        $config = "mkdocs-0.1.0.yml"
    } else {
        if ($Version -notmatch '^\d+\.\d+\.\d+$') { throw "Release version must be MAJOR.MINOR.PATCH." }
        $config = "mkdocs-release.yml"
    }
    if ($listed -match "(?m)^\s*$([regex]::Escape($Version))(\s|$)") {
        throw "Immutable documentation version '$Version' already exists on $Branch."
    }
    if ($Operation -eq "bootstrap-0.1.0") {
        $latestLine = @($listed -split "`n" | Where-Object { $_ -match '\[latest\]|latest\s*->' })
        if ($latestLine.Count -gt 0 -and ($latestLine -join "`n") -notmatch '0\.1\.0') {
            throw "latest already points to a newer stable release; bootstrap is forbidden."
        }
    }
    & mike deploy --branch $Branch --config-file $config @push --update-aliases --title $Version $Version latest
    if ($LASTEXITCODE -ne 0) { throw "mike failed to publish immutable version '$Version'." }
    & mike set-default --branch $Branch @push latest
    if ($LASTEXITCODE -ne 0) { throw "mike failed to set root default to latest." }
} finally {
    Pop-Location
}
