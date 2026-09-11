param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("dev", "release", "bootstrap-0.1.0", "repair-0.2.0-homepage")]
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

function Get-GitObjectId {
    param([Parameter(Mandatory=$true)][string]$Revision)
    $value = @(& git rev-parse $Revision)
    if ($LASTEXITCODE -ne 0 -or $value.Count -ne 1) {
        throw "Unable to resolve Git object '$Revision'."
    }
    return $value[0].Trim()
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

    if ($Operation -eq "repair-0.2.0-homepage") {
        if ($Confirmation -ne "repair-stable-0.2.0-homepage") {
            throw "Exact stable-homepage repair confirmation is required."
        }
        if (-not [string]::IsNullOrWhiteSpace($Version) -and $Version -ne "0.2.0") {
            throw "The stable-homepage repair is restricted to version 0.2.0."
        }
        $Version = "0.2.0"
        $stableLine = @($listed -split "`n" | Where-Object { $_ -match '^\s*0\.2\.0(?:\s|$)' })
        if ($stableLine.Count -ne 1) {
            throw "Stable documentation version 0.2.0 must already exist exactly once on $Branch."
        }
        if (($stableLine -join "`n") -notmatch '\[latest\]|latest\s*->') {
            throw "The latest alias must point to 0.2.0 before its homepage can be repaired."
        }

        $devBefore = Get-GitObjectId "$Branch`:dev"
        $historicalBefore = Get-GitObjectId "$Branch`:0.1.0"
        $rootBefore = Get-GitObjectId "$Branch`:index.html"
        $branchBefore = Get-GitObjectId $Branch

        [string[]]$repairArguments = @(
            "deploy", "0.2.0", "latest",
            "--branch", $Branch,
            "--config-file", "mkdocs-release.yml",
            "--update-aliases",
            "--title=0.2.0"
        )
        Invoke-Mike -Arguments $repairArguments -FailureMessage "mike failed to rebuild the 0.2.0 homepage"

        # Rebuilding a single source page legitimately updates that page and
        # aggregate indexes derived from all pages. No other stable page, theme
        # asset, download, or version metadata may change in this repair.
        $allowedRepairChanges = @(
            "0.2.0/index.html",
            "0.2.0/search/search_index.json",
            "0.2.0/sitemap.xml",
            "0.2.0/sitemap.xml.gz",
            "latest/index.html",
            "latest/search/search_index.json",
            "latest/sitemap.xml",
            "latest/sitemap.xml.gz"
        )
        $repairChanges = @(& git diff --name-only --no-renames $branchBefore $Branch)
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to inspect the generated 0.2.0 repair diff."
        }
        $unexpectedRepairChanges = @($repairChanges | Where-Object { $_ -notin $allowedRepairChanges })
        if ($unexpectedRepairChanges.Count -gt 0) {
            throw "Repairing 0.2.0 changed generated files outside the homepage-derived allowlist: $($unexpectedRepairChanges -join ', ')."
        }
        foreach ($repairPath in $repairChanges) {
            & git cat-file -e "$Branch`:$repairPath"
            if ($LASTEXITCODE -ne 0) {
                throw "Repairing 0.2.0 removed allowed generated file '$repairPath' instead of updating it."
            }
        }
        foreach ($requiredHomepage in @("0.2.0/index.html", "latest/index.html")) {
            if ($repairChanges -notcontains $requiredHomepage) {
                throw "Repairing 0.2.0 did not update required generated homepage '$requiredHomepage'."
            }
        }
        Write-Host ("Validated generated repair paths: " + ($repairChanges -join ", "))

        if ((Get-GitObjectId "$Branch`:dev") -ne $devBefore) {
            throw "Repairing 0.2.0 changed the dev documentation tree."
        }
        if ((Get-GitObjectId "$Branch`:0.1.0") -ne $historicalBefore) {
            throw "Repairing 0.2.0 changed the historical 0.1.0 documentation tree."
        }
        if ((Get-GitObjectId "$Branch`:index.html") -ne $rootBefore) {
            throw "Repairing 0.2.0 changed the root redirect."
        }
        if ((Get-GitObjectId "$Branch`:latest") -ne (Get-GitObjectId "$Branch`:0.2.0")) {
            throw "The latest alias does not contain the repaired 0.2.0 site."
        }
        $metadata = ((@(& git show "$Branch`:versions.json")) -join "`n") | ConvertFrom-Json
        $stable = @($metadata | Where-Object version -eq "0.2.0")
        if ($stable.Count -ne 1 -or $stable[0].aliases -notcontains "latest") {
            throw "mike metadata no longer maps latest to 0.2.0."
        }
        if (-not $NoPush) {
            Invoke-GitPush "$Branch`:$Branch"
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
