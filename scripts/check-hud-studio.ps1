param([string]$SiteDirectory)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$source = Join-Path $root "docs/demo/hud-studio"
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
$html = [IO.File]::ReadAllText((Join-Path $source "index.html"))
foreach ($asset in @("preview.html", "studio.js", "studio.css")) {
    if (-not $html.Contains($asset)) { throw "HUD Studio does not use required relative asset: $asset" }
}
if ($html -match '(?i)(?:src|href)\s*=\s*["''](?:https?:)?//') { throw "HUD Studio must not load external assets." }
if ($html -notmatch 'sandbox=["'']allow-scripts["'']') { throw "HUD Studio preview must use the minimal allow-scripts sandbox." }

$previewHtml = [IO.File]::ReadAllText((Join-Path $source "preview.html"))
foreach ($asset in @("runtime/visual-typography.js", "runtime/hud-panel.js", "runtime/highlight.js", "preview.js", "preview.css")) {
    if (-not $previewHtml.Contains($asset)) { throw "HUD Studio preview does not use required relative asset: $asset" }
}

$script = [IO.File]::ReadAllText((Join-Path $source "studio.js"))
$styles = [IO.File]::ReadAllText((Join-Path $source "studio.css"))
foreach ($contract in @("HudOptions.builder()", ".preset(HudPreset.", ".hud(hud)", "offsetXPx", "maxHeightPx", "headerLayout", "HudHeaderLayout", "data-header-layout", "fontPreset", "HudTypography.builder()", "data-typography", "scrollbarStyle", "scrollbarThumbColor", "scrollbar-style", "customLogo", "runtime-logo-path", "showNetwork", "showRetries", "timestampFormat", "timestampZone", "HudTimestampFormat", "ZoneId.of", "import io.github.testlens.TestLens")) {
    if (-not $script.Contains($contract)) { throw "HUD Studio is missing contract: $contract" }
}
$allScripts = $script + [IO.File]::ReadAllText((Join-Path $source "preview.js"))
if ($allScripts -match '(?i)\bfetch\s*\(' -or $allScripts -match '\bXMLHttpRequest\b') { throw "HUD Studio must not perform a network request." }
$previewScript = [IO.File]::ReadAllText((Join-Path $source "preview.js"))
$previewStyles = [IO.File]::ReadAllText((Join-Path $source "preview.css"))
foreach ($contract in @("function beginDrag", "function beginResize", "stl-studio-drag-handle", "position:vertical+'_'+horizontal", "Math.min(500", "offsetX", "maxHeight", "type:'hud-change'", "type:'hud-select'")) {
    if (-not $previewScript.Contains($contract)) { throw "HUD Studio preview is missing WYSIWYG contract: $contract" }
}
if ($script -match 'var presets\s*=\s*\{') { throw "HUD Studio must consume preset definitions from the runtime renderer." }
if ($html -notmatch 'data-preset="STANDARD"' -or $html -match 'data-preset="DEFAULT"') { throw "HUD Studio preset names are stale." }
if ($html -notmatch 'aria-pressed="true"' -or $html -notmatch 'aria-pressed="false"') { throw "HUD Studio controls must expose pressed state." }
$mobileBreakpoint = [regex]::Match($styles, '@media\s*\(max-width:\s*(\d+)px\)')
if ($styles -notmatch '(?s)\.code-actions\s*\{[^}]*position:\s*sticky' -or
    -not $mobileBreakpoint.Success -or
    [int]$mobileBreakpoint.Groups[1].Value -lt 480 -or
    [int]$mobileBreakpoint.Groups[1].Value -gt 860) {
    throw "HUD Studio must keep copy actions reachable and provide a real narrow-screen layout."
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

if (-not [string]::IsNullOrWhiteSpace($SiteDirectory)) {
    $site = (Resolve-Path -LiteralPath $SiteDirectory).Path
    foreach ($name in $runtimeFiles) {
        $built = Join-Path $site "demo/hud-studio/runtime/$name"
        if (-not (Test-Path -LiteralPath $built -PathType Leaf)) { throw "Built HUD Studio renderer is missing: $name" }
        if ((Get-FileHash $built -Algorithm SHA256).Hash -ne (Get-FileHash (Join-Path $runtime $name) -Algorithm SHA256).Hash) {
            throw "HUD Studio renderer differs from runtime: $name"
        }
    }
}
Write-Host "HUD Studio validation OK: shared runtime renderer, safe local assets, valid generator JavaScript."
