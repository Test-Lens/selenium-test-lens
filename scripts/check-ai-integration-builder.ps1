param([string]$SiteDirectory)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$page = Join-Path $root "docs/ai-assisted-integration.md"
$script = Join-Path $root "docs/javascripts/ai-integration-builder.js"
$styles = Join-Path $root "docs/stylesheets/ai-integration-builder.css"
$mkdocs = Join-Path $root "mkdocs.yml"

foreach ($path in @($page, $script, $styles)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "AI Integration Builder source is missing: $path" }
}

$pageText = [IO.File]::ReadAllText($page)
$scriptText = [IO.File]::ReadAllText($script)
$styleText = [IO.File]::ReadAllText($styles)
$mkdocsText = [IO.File]::ReadAllText($mkdocs)

foreach ($contract in @(
    "AI-assisted integration: ai-assisted-integration.md",
    "stylesheets/ai-integration-builder.css"
)) {
    if (-not $mkdocsText.Contains($contract)) { throw "MkDocs AI integration contract missing: $contract" }
}

foreach ($contract in @(
    'data-ai-integration-builder',
    'data-generated-prompt',
    'data-copy-prompt',
    'data-selenium-version',
    'role="status"',
    'aria-live="polite"',
    'Core lifecycle is always included',
    'Why the prompt is detailed',
    'Compatibility checks',
    'Integration invariants',
    'What the agent should not do',
    '../javascripts/ai-integration-builder.js'
)) {
    if (-not $pageText.Contains($contract)) { throw "AI integration page contract missing: $contract" }
}

$expectedFeatures = @(
    "nativeObservation", "uiLocator", "hud", "reportsEvidence", "reportUpload", "allure",
    "bidiNetwork", "visualRedaction", "managedAuth", "managedTestState", "reactSpa", "applicationWaits"
)
$actualFeatures = @([regex]::Matches($pageText, 'data-feature="([A-Za-z0-9]+)"') | ForEach-Object { $_.Groups[1].Value })
if (@(Compare-Object ($expectedFeatures | Sort-Object) ($actualFeatures | Sort-Object)).Count -ne 0) {
    throw "AI Integration Builder feature controls differ from the reviewed public capability set."
}
if ($actualFeatures.Count -ne @($actualFeatures | Sort-Object -Unique).Count) { throw "AI Integration Builder has duplicate feature controls." }
if ([regex]::Matches($pageText, 'data-copy-prompt').Count -ne 1) { throw "AI Integration Builder must expose exactly one copy button." }
if ($pageText -match 'name="(?:detail|prompt-level|verbosity)"' -or $pageText -match '>\s*(?:Short prompt|Basic prompt|Advanced prompt)\s*<') {
    throw "AI Integration Builder must not expose a prompt-detail selector."
}

foreach ($module in @(
    "BASE", "PROJECT_DISCOVERY", "SELENIUM_COMPATIBILITY", "LIFECYCLE_INVARIANTS", "MANUAL_LIFECYCLE",
    "JUNIT5", "TESTNG", "OBSERVE_DRIVER", "UI_LOCATOR", "HUD", "REPORTS_AND_EVIDENCE", "REPORT_UPLOAD",
    "ALLURE", "BIDI_NETWORK", "VISUAL_REDACTION", "AUTH_STATE", "TEST_STATE_RESOURCES", "REACT_SPA",
    "APPLICATION_WAITS", "CI_PRESERVATION", "VALIDATION", "FINAL_REPORT"
)) {
    if ($scriptText -notmatch "(?m)^\s+${module}:\s*\[") { throw "AI Integration Builder module missing: $module" }
}

foreach ($forbidden in @(
    "selenium-test-lens-selector-engine", "selenium-test-lens-selector-tooling", "selenium-test-lens-selector-live",
    "selenium-test-lens-selector-lab", "selenium-test-lens-compatibility-engine", "selenium-test-lens-compatibility-tooling",
    "selenium-test-lens-migration-tooling"
)) {
    if ($scriptText.Contains($forbidden)) { throw "AI Integration Builder exposes an unpublished artifact: $forbidden" }
}

if ($styleText -notmatch '@media\s*\(max-width:\s*760px\)' -or
    $styleText -notmatch '(?s)\.tl-ai-builder\s*\{[^}]*min-width:\s*0' -or
    $styleText -notmatch '(?s)\.tl-ai-builder textarea\s*\{[^}]*max-width:\s*100%' -or
    $styleText -notmatch 'focus-visible') {
    throw "AI Integration Builder styles lack the reviewed responsive/focus/overflow contract."
}

$node = Get-Command node -ErrorAction Stop
& $node.Source --check $script
if ($LASTEXITCODE -ne 0) { throw "AI Integration Builder JavaScript syntax validation failed." }
& $node.Source (Join-Path $root "scripts/test-ai-integration-builder.mjs")
if ($LASTEXITCODE -ne 0) { throw "AI Integration Builder behavior validation failed." }

if (-not [string]::IsNullOrWhiteSpace($SiteDirectory)) {
    $site = (Resolve-Path -LiteralPath $SiteDirectory).Path
    foreach ($relative in @(
        "ai-assisted-integration/index.html",
        "javascripts/ai-integration-builder.js",
        "stylesheets/ai-integration-builder.css"
    )) {
        if (-not (Test-Path -LiteralPath (Join-Path $site $relative) -PathType Leaf)) {
            throw "Built AI Integration Builder asset is missing: $relative"
        }
    }
    $built = [IO.File]::ReadAllText((Join-Path $site "ai-assisted-integration/index.html"))
    foreach ($contract in @(
        '../javascripts/ai-integration-builder.js',
        'Copy integration prompt',
        'data-ai-integration-builder',
        'href="../getting-started/"',
        'href="../advanced/network/"'
    )) {
        if (-not $built.Contains($contract)) { throw "Built AI Integration Builder contract missing: $contract" }
    }
}

Write-Host "AI Integration Builder validation OK: public feature inventory, deterministic full prompt, compatibility boundary, accessibility and responsive assets."
