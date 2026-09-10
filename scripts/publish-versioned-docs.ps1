param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("dev", "release", "bootstrap-0.1.0", "repair-0.1.0-docs")]
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

function Get-PublishedPathHash {
    param([Parameter(Mandatory=$true)][string]$Path)
    $localRef = "refs/heads/$Branch"
    & git rev-parse --verify $localRef *> $null
    $treeRef = if ($LASTEXITCODE -eq 0) { $localRef } else { "refs/remotes/origin/$Branch" }
    $treeish = "${treeRef}:$Path"
    $value = @(& git rev-parse $treeish 2>$null)
    if ($LASTEXITCODE -ne 0 -or $value.Count -ne 1) {
        throw "Published path '$Path' does not exist on branch '$Branch'."
    }
    return $value[0].Trim()
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

    if ($Operation -eq "repair-0.1.0-docs") {
        if (-not [string]::IsNullOrWhiteSpace($Version)) {
            throw "The one-time 0.1.0 repair does not accept a version argument."
        }
        if ($Confirmation -ne "repair-immutable-0.1.0-docs") {
            throw "Exact one-time repair confirmation is required."
        }
        if ($listed -notmatch '(?m)^\s*0\.1\.0(\s|$)') {
            throw "The one-time repair requires an existing 0.1.0 publication."
        }
        $latestLine = @($listed -split "`n" | Where-Object { $_ -match '\[latest\]|latest\s*->' })
        if ($latestLine.Count -eq 0 -or ($latestLine -join "`n") -notmatch '0\.1\.0') {
            throw "The one-time repair requires latest to point to 0.1.0."
        }

        $devHashBefore = Get-PublishedPathHash -Path "dev"
        Write-Host "dev tree hash before 0.1.0 repair: $devHashBefore"
        [string[]]$repairArguments = @(
            "deploy", "0.1.0", "latest",
            "--branch", $Branch,
            "--config-file", "mkdocs-0.1.0.yml",
            "--update-aliases",
            "--title=0.1.0"
        )
        Invoke-Mike -Arguments $repairArguments -FailureMessage "mike failed to repair the historical 0.1.0 documentation"

        [string[]]$defaultArguments = @("set-default", "latest", "--branch", $Branch)
        Invoke-Mike -Arguments $defaultArguments -FailureMessage "mike failed to preserve the latest root default"

        $devHashAfter = Get-PublishedPathHash -Path "dev"
        Write-Host "dev tree hash after 0.1.0 repair: $devHashAfter"
        if ($devHashAfter -ne $devHashBefore) {
            throw "Safety check failed: repairing 0.1.0 changed the published dev tree."
        }
        if (-not $NoPush) {
            & git push origin $Branch
            if ($LASTEXITCODE -ne 0) {
                throw "The verified 0.1.0 repair could not be pushed without force."
            }
        }
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
