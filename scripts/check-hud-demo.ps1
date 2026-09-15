param(
    [string]$SiteDirectory,
    [string]$RuntimeSourceDirectory,
    [string]$ExpectedRuntimeRef
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$demoSource = Join-Path $root "docs/demo/hud"
$runtimeSource = if ([string]::IsNullOrWhiteSpace($RuntimeSourceDirectory)) {
    Join-Path $root "selenium-test-lens-overlay/src/main/resources/uitestlens/runtime"
} else {
    (Resolve-Path -LiteralPath $RuntimeSourceDirectory).Path
}
$runtimeManifest = Join-Path $root "docs-hooks/hud-demo-runtime-assets.txt"
if (-not (Test-Path -LiteralPath $runtimeManifest -PathType Leaf)) {
    throw "HUD demo runtime asset manifest is missing: $runtimeManifest"
}
$runtimeFiles = @(Get-Content -LiteralPath $runtimeManifest | ForEach-Object { $_.Trim() } | Where-Object { $_ -and -not $_.StartsWith("#") })
if ($runtimeFiles.Count -eq 0 -or @($runtimeFiles | Sort-Object -Unique).Count -ne $runtimeFiles.Count) {
    throw "HUD demo runtime asset manifest must be non-empty and contain unique paths."
}

function Require-File {
    param([Parameter(Mandatory=$true)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Required HUD demo file is missing: $Path"
    }
}

foreach ($name in @("index.html", "demo.css", "demo.js")) {
    Require-File (Join-Path $demoSource $name)
}
foreach ($name in $runtimeFiles) {
    Require-File (Join-Path $runtimeSource $name)
}

$mkdocs = [IO.File]::ReadAllText((Join-Path $root "mkdocs.yml"))
if (-not $mkdocs.Contains("docs-hooks/copy-hud-demo-runtime.py")) {
    throw "mkdocs.yml does not register the canonical HUD demo runtime copy hook."
}

$homepage = [IO.File]::ReadAllText((Join-Path $root "docs/index.md"))
if (-not $homepage.Contains('src="demo/hud/"')) {
    throw "The documentation homepage does not use a version-relative HUD demo iframe URL."
}
if (-not $homepage.Contains('sandbox="allow-scripts"') -or $homepage -match 'sandbox="[^"]*(?:allow-same-origin|allow-forms|allow-popups|allow-top-navigation)') {
    throw "The HUD demo iframe must use only the minimal allow-scripts sandbox capability."
}
foreach ($hostContract in @("IntersectionObserver", "test-lens-demo-visibility", "test-lens-demo-ready", "frame.contentWindow.postMessage")) {
    if (-not $homepage.Contains($hostContract)) {
        throw "The homepage is missing HUD demo viewport lifecycle behavior '$hostContract'."
    }
}
if (-not $homepage.Contains('title="Interactive Test Lens HUD, highlight, and scroll-cue demonstration"')) {
    throw "The HUD demo iframe is missing its accessible title."
}
if (-not $homepage.Contains("Open the standalone HUD demo")) {
    throw "The HUD demo iframe is missing fallback content."
}

$demoHtml = [IO.File]::ReadAllText((Join-Path $demoSource "index.html"))
foreach ($asset in @("demo.css", "demo.js", "runtime/visual-typography.js", "runtime/hud-panel.js", "runtime/highlight.js", "runtime/scroll-arrow.js")) {
    if (-not $demoHtml.Contains($asset)) {
        throw "HUD demo HTML does not reference relative asset '$asset'."
    }
}

$typographyJs = [IO.File]::ReadAllText((Join-Path $runtimeSource "visual-typography.js"))
if (-not $typographyJs.Contains("fonts/Sora-wght.woff2") -or $typographyJs -match '(?i)https?://') {
    throw "The shared visual typography runtime must load only the bundled relative Sora asset."
}
if ($demoHtml -match '(?i)(?:src|href)\s*=\s*["''](?:https?:)?//') {
    throw "HUD demo HTML must not load external assets."
}

$demoJsPath = Join-Path $demoSource "demo.js"
$demoJs = [IO.File]::ReadAllText($demoJsPath)
foreach ($contract in @(
    "hud.init", "hud.setStep", "hud.log", "hud.preset",
    "highlight.element", "scrollArrow.scrollToElementWithArrow",
    "prefers-reduced-motion", "document.hidden", "visibilitychange",
    "event.source !== window.parent", "pendingDelays", "cancelScheduledWork",
    "cancelAnimationFrame", "dataset.demoState = 'passed'"
)) {
    if (-not $demoJs.Contains($contract)) {
        throw "HUD demo JavaScript is missing required behavior '$contract'."
    }
}
if ($demoJs -match '(?i)\bfetch\s*\(' -or $demoJs -match '\bXMLHttpRequest\b') {
    throw "The static HUD demo must not perform a real network request."
}

$node = Get-Command node -ErrorAction SilentlyContinue
if ($null -eq $node) {
    throw "Node.js is required to syntax-check the HUD demo JavaScript."
}
& $node.Source --check $demoJsPath
if ($LASTEXITCODE -ne 0) { throw "HUD demo JavaScript syntax validation failed." }
& $node.Source (Join-Path $root "scripts/test-hud-demo-lifecycle.mjs")
if ($LASTEXITCODE -ne 0) { throw "HUD demo lifecycle validation failed." }

if (-not [string]::IsNullOrWhiteSpace($ExpectedRuntimeRef)) {
    foreach ($name in $runtimeFiles) {
        $repositoryPath = "selenium-test-lens-overlay/src/main/resources/uitestlens/runtime/$name"
        $expectedBlob = (& git -C $root rev-parse "$ExpectedRuntimeRef`:$repositoryPath").Trim()
        if ($LASTEXITCODE -ne 0) { throw "Unable to resolve $repositoryPath at $ExpectedRuntimeRef." }
        $actualBlob = (& git -C $root hash-object (Join-Path $runtimeSource $name)).Trim()
        if ($LASTEXITCODE -ne 0 -or $actualBlob -ne $expectedBlob) {
            throw "HUD demo renderer '$name' differs from $ExpectedRuntimeRef."
        }
    }
}

if (-not [string]::IsNullOrWhiteSpace($SiteDirectory)) {
    $site = (Resolve-Path -LiteralPath $SiteDirectory).Path
    $builtDemo = Join-Path $site "demo/hud"
    foreach ($name in @("index.html", "demo.css", "demo.js")) {
        Require-File (Join-Path $builtDemo $name)
    }
    foreach ($name in $runtimeFiles) {
        $published = Join-Path $builtDemo "runtime/$name"
        Require-File $published
        $sourceHash = (Get-FileHash (Join-Path $runtimeSource $name) -Algorithm SHA256).Hash
        $publishedHash = (Get-FileHash $published -Algorithm SHA256).Hash
        if ($sourceHash -ne $publishedHash) {
            throw "Built HUD demo renderer '$name' differs from the canonical runtime resource."
        }
    }
    $builtHomepage = [IO.File]::ReadAllText((Join-Path $site "index.html"))
    if (-not $builtHomepage.Contains('src="demo/hud/"')) {
        throw "Built homepage does not preserve the version-relative HUD demo iframe URL."
    }
}

$runtimeContract = if ([string]::IsNullOrWhiteSpace($ExpectedRuntimeRef)) {
    "current runtime renderer"
} else {
    "renderer from $ExpectedRuntimeRef"
}
Write-Host "HUD demo validation OK: $runtimeContract, relative static assets, no network request, valid JavaScript."
