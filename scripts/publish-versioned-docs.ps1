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

function Invoke-Mike {
    param(
        [Parameter(Mandatory=$true)]
        [string[]]$Arguments,
        [Parameter(Mandatory=$true)]
        [string]$FailureMessage
    )

    $displayArguments = $Arguments | ForEach-Object {
        if ($_ -match '\s') { '"' + ($_ -replace '"', '\"') + '"' } else { $_ }
    }
    Write-Host ("mike arguments: " + ($displayArguments -join " "))
    $output = @(& mike @Arguments)
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) { throw "$FailureMessage (exit code $exitCode)." }
    return $output
}

Push-Location $root
try {
    Invoke-Mike -Arguments @("--version") -FailureMessage "Unable to read the pinned mike version"
    [string[]]$listArguments = @("list", "--branch", $Branch)
    $listed = (Invoke-Mike -Arguments $listArguments -FailureMessage "Unable to read mike metadata from branch '$Branch'") -join "`n"
    if ($Operation -eq "dev") {
        if ([string]::IsNullOrWhiteSpace($Version) -or -not $Version.EndsWith("-SNAPSHOT")) {
            throw "dev publication requires the -SNAPSHOT version read from the root POM."
        }
        [string[]]$deployArguments = @(
            "deploy", "dev",
            "--branch", $Branch,
            "--update-aliases",
            "--title=$Version / coming soon"
        )
        if (-not $NoPush) { $deployArguments += "--push" }
        Invoke-Mike -Arguments $deployArguments -FailureMessage "mike failed to update dev; gh-pages was not force-pushed"
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
    [string[]]$deployArguments = @(
        "deploy", $Version, "latest",
        "--branch", $Branch,
        "--config-file", $config,
        "--update-aliases",
        "--title=$Version"
    )
    if (-not $NoPush) { $deployArguments += "--push" }
    Invoke-Mike -Arguments $deployArguments -FailureMessage "mike failed to publish immutable version '$Version'"

    [string[]]$defaultArguments = @("set-default", "latest", "--branch", $Branch)
    if (-not $NoPush) { $defaultArguments += "--push" }
    Invoke-Mike -Arguments $defaultArguments -FailureMessage "mike failed to set root default to latest"
} finally {
    Pop-Location
}
