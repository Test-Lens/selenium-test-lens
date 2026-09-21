param([string]$SiteDirectory)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$source = Join-Path $root "docs/demo/hud-studio"
$page = Join-Path $root "docs/observability/hud-studio.md"
$hostScript = Join-Path $root "docs/javascripts/hud-studio-host.js"
$favicon = Join-Path $root "docs/assets/images/favicon.png"
$mkdocs = Join-Path $root "mkdocs.yml"
$runtime = Join-Path $root "selenium-test-lens-overlay/src/main/resources/uitestlens/runtime"
$runtimeManifest = Join-Path $root "docs-hooks/hud-demo-runtime-assets.txt"
if (-not (Test-Path -LiteralPath $runtimeManifest -PathType Leaf)) { throw "HUD Studio runtime asset manifest is missing." }
$runtimeFiles = @(Get-Content -LiteralPath $runtimeManifest | ForEach-Object { $_.Trim() } | Where-Object { $_ -and -not $_.StartsWith("#") })
if ($runtimeFiles.Count -eq 0 -or @($runtimeFiles | Sort-Object -Unique).Count -ne $runtimeFiles.Count) {
    throw "HUD Studio runtime asset manifest must be non-empty and contain unique paths."
}

foreach ($name in @("index.html", "studio.css", "studio.js", "preview.html", "preview.css", "preview.js")) {
    if (-not (Test-Path -LiteralPath (Join-Path $source $name) -PathType Leaf)) { throw "HUD Studio asset is missing: $name" }
}
if (-not (Test-Path -LiteralPath $hostScript -PathType Leaf)) { throw "HUD Studio host script is missing." }
if (-not (Test-Path -LiteralPath $favicon -PathType Leaf)) { throw "Documentation favicon is missing." }
if (-not ([IO.File]::ReadAllText($mkdocs)).Contains('favicon: assets/images/favicon.png')) {
    throw "MkDocs does not reference the version-relative Test Lens favicon."
}
$pageText = [IO.File]::ReadAllText($page)
foreach ($contract in @('data-studio-host', 'data-studio-open', 'data-studio-expand', 'data-studio-fullscreen', 'data-studio-exit', '.tl-hud-studio-toolbar>[hidden]{display:none}', 'src="../../demo/hud-studio/"', 'href="../../demo/hud-studio/"', 'target="_blank"', 'rel="noopener noreferrer"', 'sandbox="allow-scripts"', 'allow="fullscreen"', 'allowfullscreen', 'body.tl-hud-studio-page .md-main__inner{max-width:none}', 'body.tl-hud-studio-page .md-sidebar--secondary{display:none}', 'For the best editing experience, open Studio in a full-width view.')) {
    if (-not $pageText.Contains($contract)) { throw "HUD Studio documentation host is missing contract: $contract" }
}
if ($pageText -match '(?s)(?<!tl-hud-studio-page )\.md-grid\s*\{[^}]*max-width\s*:\s*none' -or
    $pageText -match '(?s)(?<!tl-hud-studio-page )\.md-main__inner\s*\{[^}]*max-width\s*:\s*none') {
    throw "HUD Studio full-width styling must remain scoped to the Studio page."
}
if ($pageText -notmatch '(?s)\.tl-studio-focus-mode\s+\.md-header.*?\.md-sidebar.*?display:none' -or
    $pageText -notmatch '(?s)\.tl-studio-focus-mode\s+\.tl-hud-studio-host.*?position:fixed.*?width:100vw.*?height:100vh' -or
    $pageText -notmatch '(?s)\.tl-studio-focus-mode\s+\.tl-hud-studio-frame.*?flex:1') {
    throw "HUD Studio expanded mode must cover the viewport and hide documentation chrome."
}
$hostText = [IO.File]::ReadAllText($hostScript)
foreach ($contract in @("requestFullscreen()", "fullscreenchange", "tl-hud-studio-page", "tl-studio-focus-mode", "studio-host-resize", "new Event('resize')", "event.key === 'Escape'", "expandTrigger.focus()")) {
    if (-not $hostText.Contains($contract)) { throw "HUD Studio host behavior is missing contract: $contract" }
}
$html = [IO.File]::ReadAllText((Join-Path $source "index.html"))
foreach ($asset in @("preview.html", "studio.js", "studio.css")) {
    if (-not $html.Contains($asset)) { throw "HUD Studio does not use required relative asset: $asset" }
}
foreach ($control in @('id="timestamp-format"', '<option>CUSTOM</option>', 'id="timestamp-pattern"', 'readonly',
        'id="timestamp-zone-mode"', '<option>SYSTEM</option>', '<option>UTC</option>',
        'id="timestamp-zone-custom"', 'id="timestamp-zone"', 'id="timestamp-winter-preview"',
        'id="timestamp-summer-preview"', 'id="timestamp-font"')) {
    if (-not $html.Contains($control)) { throw "HUD Studio timestamp UI is missing contract: $control" }
}
if ($html -match '(?i)(?:src|href)\s*=\s*["''](?:https?:)?//') { throw "HUD Studio must not load external assets." }
if ($html -notmatch 'sandbox=["'']allow-scripts["'']') { throw "HUD Studio preview must use the minimal allow-scripts sandbox." }
foreach ($compatibilityHelp in @("IDEA 2026.1+", "Toolbox is not required", "Older IDEA versions require JetBrains Toolbox App 3.3+", "intellijProject(name, root)")) {
    if (-not $html.Contains($compatibilityHelp)) { throw "HUD Studio is missing Source Navigation compatibility help: $compatibilityHelp" }
}

$previewHtml = [IO.File]::ReadAllText((Join-Path $source "preview.html"))
foreach ($asset in @("runtime/visual-typography.js", "runtime/hud-panel.js", "runtime/highlight.js", "preview.js", "preview.css")) {
    if (-not $previewHtml.Contains($asset)) { throw "HUD Studio preview does not use required relative asset: $asset" }
}

$script = [IO.File]::ReadAllText((Join-Path $source "studio.js"))
$styles = [IO.File]::ReadAllText((Join-Path $source "studio.css"))
foreach ($contract in @("HudOptions.builder()", ".preset(HudPreset.", ".hud(hud)", "offsetXPx", "maxHeightPx", "headerLayout", "HudHeaderLayout", "data-header-layout", "fontPreset", "HudTypography.builder()", "data-typography", "scrollbarStyle", "scrollbarThumbColor", "scrollbar-style", "customLogo", "runtime-logo-path", "showNetwork", "showRetries", "timestampFormat", "timestampZone", "timestampFormatMode", "timestampZoneMode", "HudTimestampFormat", "ZoneId.of", "ZoneOffset.UTC", "HighlightOptions.builder()", ".highlights(highlights)", "actionColor", "waitingColor", "retryColor", "successColor", "failureColor", "automaticFeedback", "borderWidthPx", "sourceNavigation(SourceNavigationOptions.builder()", "SourceNavigationModifier", "VisualRedactionOptions.defaults()", "import io.github.testlens.TestLens")) {
    if (-not $script.Contains($contract)) { throw "HUD Studio is missing contract: $contract" }
}
$allScripts = $script + [IO.File]::ReadAllText((Join-Path $source "preview.js"))
if ($allScripts -match '(?i)\bfetch\s*\(' -or $allScripts -match '\bXMLHttpRequest\b') { throw "HUD Studio must not perform a network request." }
$previewScript = [IO.File]::ReadAllText((Join-Path $source "preview.js"))
$previewStyles = [IO.File]::ReadAllText((Join-Path $source "preview.css"))
foreach ($contract in @("function beginDrag", "function beginResize", "stl-studio-drag-handle", "position:vertical+'_'+horizontal", "Math.min(500", "offsetX", "maxHeight", "type:'hud-change'", "type:'hud-select'")) {
    if (-not $previewScript.Contains($contract)) { throw "HUD Studio preview is missing WYSIWYG contract: $contract" }
}
if ($previewScript -notmatch 'sourceNavigationPreviewActive' -or $previewScript -notmatch 'new KeyboardEvent') {
    throw "HUD Studio preview must expose inactive and active source-navigation states through the runtime keyboard contract."
}
if ($script -match 'var presets\s*=\s*\{') { throw "HUD Studio must consume preset definitions from the runtime renderer." }
if ($html -notmatch 'data-preset="STANDARD"' -or $html -match 'data-preset="DEFAULT"') { throw "HUD Studio preset names are stale." }
if ($html -notmatch 'aria-pressed="true"' -or $html -notmatch 'aria-pressed="false"') { throw "HUD Studio controls must expose pressed state." }
$mobileBreakpoint = [regex]::Match($styles, '@media\s*\(max-width:\s*(\d+)px\)')
if ($styles -notmatch '(?s)\.code-actions\s*\{[^}]*position:\s*sticky' -or
    -not $mobileBreakpoint.Success -or
    [int]$mobileBreakpoint.Groups[1].Value -lt 480 -or
    [int]$mobileBreakpoint.Groups[1].Value -gt 1024) {
    throw "HUD Studio must keep copy actions reachable and provide a real narrow-screen layout."
}
if ([int]$mobileBreakpoint.Groups[1].Value -lt 768 -or
    $styles -notmatch '(?s)html, body\s*\{[^}]*overflow-x:\s*hidden' -or
    $styles -notmatch '(?s)\.studio\s*\{[^}]*max-width:\s*100vw' -or
    $styles -notmatch '@media\s*\(max-width:\s*480px\)') {
    throw "HUD Studio must switch to one column before tablet width and prevent horizontal overflow."
}
if (-not $previewScript.Contains("type:'hud-rendered'") -or -not $script.Contains("dataset.hudReady='true'")) {
    throw "HUD Studio must expose readiness only after the runtime panel is rendered."
}
if ($previewStyles -match '\.stl-studio-(?:drag-handle|resize)') {
    throw "HUD Studio controls must not rely on document CSS that cannot cross the runtime Shadow DOM boundary."
}
if ($previewScript -notmatch "style\[data-studio-controls\]" -or
    $previewScript -notmatch '(?s)\.stl-studio-resize\s*\{[^}]*right:\s*[0-9]+px[^}]*bottom:\s*[0-9]+px[^}]*width:\s*18px[^}]*height:\s*18px') {
    throw "HUD Studio must inject its resize handle styles inside the runtime Shadow DOM with an adequate hit target."
}

$node = Get-Command node -ErrorAction Stop
& $node.Source --check (Join-Path $source "studio.js")
if ($LASTEXITCODE -ne 0) { throw "HUD Studio JavaScript syntax validation failed." }
& $node.Source --check (Join-Path $source "preview.js")
if ($LASTEXITCODE -ne 0) { throw "HUD Studio preview JavaScript syntax validation failed." }
& $node.Source (Join-Path $root "scripts/test-hud-studio.mjs")
if ($LASTEXITCODE -ne 0) { throw "HUD Studio behavior validation failed." }
& $node.Source (Join-Path $root "scripts/test-hud-studio-host.mjs")
if ($LASTEXITCODE -ne 0) { throw "HUD Studio host mode validation failed." }

if (-not [string]::IsNullOrWhiteSpace($SiteDirectory)) {
    $site = (Resolve-Path -LiteralPath $SiteDirectory).Path
    foreach ($builtPath in @(
        "observability/hud-studio/index.html",
        "demo/hud-studio/index.html",
        "javascripts/hud-studio-host.js",
        "assets/images/favicon.png",
        "assets/brand/test-lens-icon.png",
        "assets/brand/test-lens-logo-horizontal.png"
    )) {
        if (-not (Test-Path -LiteralPath (Join-Path $site $builtPath) -PathType Leaf)) {
            throw "Built HUD Studio host asset is missing: $builtPath"
        }
    }
    $builtPage = [IO.File]::ReadAllText((Join-Path $site "observability/hud-studio/index.html"))
    if (-not $builtPage.Contains('../../demo/hud-studio/') -or
        -not $builtPage.Contains('../../javascripts/hud-studio-host.js') -or
        -not $builtPage.Contains('<link rel="icon" href="../../assets/images/favicon.png">') -or
        -not $builtPage.Contains('body.tl-hud-studio-page .md-main__inner{max-width:none}')) {
        throw "Built HUD Studio page does not retain the canonical URLs and page-scoped full-width contract."
    }
    $builtHome = [IO.File]::ReadAllText((Join-Path $site "index.html"))
    if (-not $builtHome.Contains('<link rel="icon" href="assets/images/favicon.png">')) {
        throw "Built versioned landing page does not use a version-relative favicon URL."
    }
    foreach ($metadata in @(
        '<meta property="og:title" content="Test Lens">',
        '<meta property="og:description" content="Test Lens for Selenium provides observable, retryable interactions and diagnostic evidence for an existing WebDriver.">',
        '<meta property="og:url" content="https://test-lens.github.io/selenium-test-lens/">',
        '<meta property="og:image" content="https://test-lens.github.io/selenium-test-lens/latest/assets/brand/test-lens-logo-horizontal.png">',
        '<meta name="twitter:card" content="summary_large_image">'
    )) {
        if (-not $builtHome.Contains($metadata)) { throw "Built documentation branding metadata is missing: $metadata" }
    }
    foreach ($branding in @(
        '<img src="assets/brand/test-lens-icon.png" alt="logo">',
        '<img class="lens-home-logo" src="assets/brand/test-lens-logo-horizontal.png" alt="Test Lens">'
    )) {
        if (-not $builtHome.Contains($branding)) { throw "Built documentation branding image is missing: $branding" }
    }
    if ((Get-FileHash (Join-Path $site "assets/images/favicon.png") -Algorithm SHA256).Hash -ne
        (Get-FileHash $favicon -Algorithm SHA256).Hash) {
        throw "Built versioned favicon differs from the canonical Test Lens asset."
    }
    foreach ($name in @("index.html", "studio.css", "studio.js", "preview.html", "preview.css", "preview.js")) {
        $builtAsset = Join-Path $site "demo/hud-studio/$name"
        if ((Get-FileHash $builtAsset -Algorithm SHA256).Hash -ne
            (Get-FileHash (Join-Path $source $name) -Algorithm SHA256).Hash) {
            throw "Built versioned HUD Studio asset is stale or transformed: $name"
        }
    }
    foreach ($name in $runtimeFiles) {
        $built = Join-Path $site "demo/hud-studio/runtime/$name"
        if (-not (Test-Path -LiteralPath $built -PathType Leaf)) { throw "Built HUD Studio renderer is missing: $name" }
        if ((Get-FileHash $built -Algorithm SHA256).Hash -ne (Get-FileHash (Join-Path $runtime $name) -Algorithm SHA256).Hash) {
            throw "HUD Studio renderer differs from runtime: $name"
        }
    }
}
Write-Host "HUD Studio validation OK: shared runtime renderer, safe local assets, valid generator JavaScript."
