param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("dev", "release", "redeploy-release", "bootstrap-0.1.0")]
    [string]$Operation,
    [string]$Version,
    [string]$Confirmation,
    [string]$Branch = "gh-pages",
    [string]$SourceRoot,
    [switch]$NoPush
)

$ErrorActionPreference = "Stop"
$root = if ([string]::IsNullOrWhiteSpace($SourceRoot)) {
    Split-Path -Parent $PSScriptRoot
} else {
    (Resolve-Path -LiteralPath $SourceRoot).Path
}

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

function Invoke-GitPush {
    param([Parameter(Mandatory=$true)][string]$RefSpec)
    & git push origin $RefSpec
    if ($LASTEXITCODE -ne 0) {
        throw "Git rejected the documentation push for '$RefSpec'; no force push was attempted."
    }
}

Push-Location $root
try {
    Invoke-Mike -Arguments @("--version") -FailureMessage "Unable to read the pinned mike version"
    [string[]]$listArguments = @("list", "--branch", $Branch)
    $listed = (Invoke-Mike -Arguments $listArguments -FailureMessage "Unable to read mike metadata from branch '$Branch'") -join "`n"
    if ($Operation -eq "dev") {
        if ([string]::IsNullOrWhiteSpace($Version) -or $Version -notmatch '^\d+\.\d+\.\d+(-SNAPSHOT)?$') {
            throw "dev publication requires the semantic release or snapshot version read from the root POM."
        }
        $title = if ($Version.EndsWith("-SNAPSHOT", [StringComparison]::Ordinal)) {
            "$Version / coming soon"
        } else {
            $Version
        }
        [string[]]$deployArguments = @(
            "deploy", "dev",
            "--branch", $Branch,
            "--update-aliases",
            "--title=$title"
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
    $versionExists = $listed -match "(?m)^\s*$([regex]::Escape($Version))(\s|$)"
    if ($Operation -eq "redeploy-release") {
        # Administrative repair exception: this may replace documentation for an existing release,
        # but must never be used to change the semantics of an already released API.
        if (-not $versionExists) {
            throw "Documentation version '$Version' does not exist on $Branch; redeploy-release only repairs an existing version."
        }
        if ($Confirmation -ne "redeploy-docs-$Version") {
            throw "Exact redeploy confirmation 'redeploy-docs-$Version' is required."
        }
    } elseif ($versionExists) {
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
    $deployFailure = if ($Operation -eq "redeploy-release") {
        "mike failed to repair existing documentation version '$Version'"
    } else {
        "mike failed to publish immutable version '$Version'"
    }
    Invoke-Mike -Arguments $deployArguments -FailureMessage $deployFailure

    [string[]]$defaultArguments = @("set-default", "latest", "--branch", $Branch)
    if (-not $NoPush) { $defaultArguments += "--push" }
    Invoke-Mike -Arguments $defaultArguments -FailureMessage "mike failed to set root default to latest"
} finally {
    Pop-Location
}
