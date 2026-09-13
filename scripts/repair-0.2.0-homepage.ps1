param(
    [Parameter(Mandatory=$true)]
    [string]$Confirmation,
    [string]$Branch = "gh-pages",
    [switch]$NoPush
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$releaseTag = "v0.2.0"
$worktreeRoot = Join-Path ([IO.Path]::GetTempPath()) ("test-lens-docs-v0.2.0-" + [guid]::NewGuid())
$previewRoot = Join-Path ([IO.Path]::GetTempPath()) ("test-lens-docs-v0.2.0-preview-" + [guid]::NewGuid())
$worktreeAdded = $false

function Invoke-GitChecked {
    param(
        [Parameter(Mandatory=$true)][string[]]$Arguments,
        [Parameter(Mandatory=$true)][string]$FailureMessage
    )
    & git @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$FailureMessage (exit code $LASTEXITCODE)." }
}

if ($Confirmation -ne "repair-stable-0.2.0-homepage") {
    throw "Exact confirmation repair-stable-0.2.0-homepage is required."
}

Push-Location $root
try {
    Invoke-GitChecked -Arguments @("rev-parse", "--verify", "refs/tags/$releaseTag") -FailureMessage "Required release tag $releaseTag does not exist"
    & git diff --quiet $releaseTag -- ":(glob)**/src/main/java/**"
    if ($LASTEXITCODE -ne 0) {
        throw "Production Java sources differ from $releaseTag; the one-time homepage repair is no longer safe."
    }
    & git diff --quiet $releaseTag -- "docs/reference/public-api-manifest.txt"
    if ($LASTEXITCODE -ne 0) {
        throw "The public API manifest differs from $releaseTag; the homepage repair is no longer safe."
    }

    Invoke-GitChecked -Arguments @("worktree", "add", "--detach", $worktreeRoot, $releaseTag) -FailureMessage "Unable to create the detached $releaseTag documentation worktree"
    $worktreeAdded = $true

    $homepage = Join-Path $worktreeRoot "docs/index.md"
    Copy-Item -LiteralPath (Join-Path $root "docs/index.md") -Destination $homepage -Force

    $demoSource = Join-Path $root "docs/demo/hud"
    $demoDestination = Join-Path $worktreeRoot "docs/demo/hud"
    New-Item -ItemType Directory -Path $demoDestination -Force | Out-Null
    foreach ($demoFile in @("index.html", "demo.css", "demo.js")) {
        Copy-Item -LiteralPath (Join-Path $demoSource $demoFile) -Destination (Join-Path $demoDestination $demoFile) -Force
    }
    # Stable 0.2.0 must retain the renderer shipped by its release tag even when
    # the development renderer evolves on main.
    $runtimeSource = Join-Path $worktreeRoot "selenium-test-lens-overlay/src/main/resources/uitestlens/runtime"
    $runtimeDestination = Join-Path $demoDestination "runtime"
    New-Item -ItemType Directory -Path $runtimeDestination -Force | Out-Null
    foreach ($runtimeFile in @("hud-panel.js", "highlight.js", "scroll-arrow.js")) {
        Copy-Item -LiteralPath (Join-Path $runtimeSource $runtimeFile) -Destination (Join-Path $runtimeDestination $runtimeFile) -Force
    }

    # The dev homepage maps to main and keeps its edit link. The repaired stable
    # homepage has no matching source file in v0.2.0, so hide only that edit link.
    $homepageText = [IO.File]::ReadAllText($homepage)
    $homepageText = [Text.RegularExpressions.Regex]::Replace(
        $homepageText,
        '(?s)\r?\n<!-- configurable-hud-dev:start -->.*?<!-- configurable-hud-dev:end -->\r?\n',
        "`n"
    )
    $stableHomepageText = [regex]::Replace(
        $homepageText,
        '(?m)^(  - toc)\r?$',
        "`$1`n  - edit",
        1
    )
    if ($stableHomepageText -eq $homepageText) {
        throw "Unable to apply stable-only edit-link metadata to the repaired homepage."
    }
    [IO.File]::WriteAllText($homepage, $stableHomepageText, [Text.UTF8Encoding]::new($false))

    $expectedWorktreeChanges = @(
        "M docs/index.md",
        "?? docs/demo/hud/demo.css",
        "?? docs/demo/hud/demo.js",
        "?? docs/demo/hud/index.html",
        "?? docs/demo/hud/runtime/highlight.js",
        "?? docs/demo/hud/runtime/hud-panel.js",
        "?? docs/demo/hud/runtime/scroll-arrow.js"
    ) | Sort-Object
    $worktreeChanges = @(& git -C $worktreeRoot status --short --untracked-files=all | ForEach-Object { $_.Trim() } | Sort-Object)
    if (($worktreeChanges -join "`n") -ne ($expectedWorktreeChanges -join "`n")) {
        throw "The repair worktree contains changes outside the homepage demo: $($worktreeChanges -join ', ')."
    }

    Push-Location $worktreeRoot
    try {
        $env:DOCS_RELEASE_VERSION = "0.2.0"
        $env:DOCS_RELEASE_EDIT_URI = "edit/v0.2.0/docs/"
        & mkdocs build --strict --config-file mkdocs-release.yml --site-dir $previewRoot
        if ($LASTEXITCODE -ne 0) { throw "The repaired 0.2.0 documentation did not build strictly." }
        & (Join-Path $root "scripts/check-hud-demo.ps1") `
            -SiteDirectory $previewRoot `
            -RuntimeSourceDirectory $runtimeSource `
            -ExpectedRuntimeRef $releaseTag
    } finally {
        Pop-Location
    }

    & (Join-Path $root "scripts/publish-versioned-docs.ps1") `
        -Operation repair-0.2.0-homepage `
        -Version 0.2.0 `
        -Confirmation $Confirmation `
        -Branch $Branch `
        -SourceRoot $worktreeRoot `
        -NoPush:$NoPush
} finally {
    $cleanupFailure = $null
    if ($worktreeAdded) {
        # This path is generated under the system temp directory above. Force is
        # intentionally scoped to this disposable worktree so cleanup also works
        # when the overlaid homepage or an interrupted mike build leaves it dirty.
        & git worktree remove --force $worktreeRoot
        if ($LASTEXITCODE -ne 0) {
            $cleanupFailure = "Unable to remove temporary repair worktree: $worktreeRoot"
        }
    }
    if (Test-Path -LiteralPath $previewRoot) {
        Remove-Item -LiteralPath $previewRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
    Pop-Location
    if ($null -ne $cleanupFailure) { throw $cleanupFailure }
}
