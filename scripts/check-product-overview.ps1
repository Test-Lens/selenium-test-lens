param([string]$SiteDirectory)
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$overviewPath = Join-Path $root "docs/index.md"
$mapPath = Join-Path $root "docs/maintainers/product-capability-map.md"
$mkdocsPath = Join-Path $root "mkdocs.yml"

foreach ($path in @($overviewPath, $mapPath, $mkdocsPath)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Required overview source is missing: $path" }
}

$overview = [IO.File]::ReadAllText($overviewPath)
$capabilityMap = [IO.File]::ReadAllText($mapPath)
$mkdocs = [IO.File]::ReadAllText($mkdocsPath)

$targetMatch = [regex]::Match($mkdocs, '(?m)^\s{4}target_release:\s*(\d+\.\d+\.\d+)\s*$')
if (-not $targetMatch.Success) { throw "mkdocs.yml must define one concrete AI integration target_release." }
$targetRelease = $targetMatch.Groups[1].Value

foreach ($contract in @(
    'title: Selenium Test Lens',
    '# Understand what your Selenium test did',
    '[Get started](getting-started.md)',
    '[Integrate with AI](ai-assisted-integration.md)',
    '[Explore capabilities](#product-capabilities)',
    '## What Test Lens adds to Selenium',
    '## Why teams add Test Lens',
    '## Product capabilities',
    '### Existing Selenium observation and Lens-native semantics',
    '### Live HUD and element highlights',
    '### Recovery that remains visible',
    '### Trace, logs, reports, and failure evidence',
    '### WebDriver BiDi network diagnostics',
    '### Text redaction and screenshot masking',
    '### Authentication state, test state, and resources',
    '### React and dynamic SPA behavior',
    '## See the result',
    '## Quick start',
    '## Requirements and optional capabilities',
    "## What's new in $targetRelease",
    'does **not** instrument every existing `WebDriver`',
    'Lens does not record video',
    'This is observation, not interception'
)) {
    if (-not $overview.Contains($contract)) { throw "Product overview is missing contract: $contract" }
}

if ($overview.Contains("## $targetRelease at a glance") -or $overview -match '(?m)^## 0\.3\.0 foundations\s*$') {
    throw "The landing page must not use release history as its primary information architecture."
}
if (-not $overview.Contains("Current stable release: $targetRelease")) {
    throw "Overview stable-release badge does not match mkdocs target_release $targetRelease."
}

foreach ($mapContract in @(
    "# Product capability map for release $targetRelease",
    '## Audit boundary',
    '## Integration and daily browser work',
    '## Waiting, assertions, and recovery',
    '## Live diagnostics and persistent evidence',
    '## Network, data protection, state, and optional modules',
    '## Documentation tools versus runtime integrations',
    'User problem and released behavior',
    'Public entry point / artifact',
    'Activation, requirements, and limits',
    'Detailed docs',
    'Confirming example, test, and availability source',
    'Not published for consumers',
    'maven.deploy.skip'
)) {
    if (-not $capabilityMap.Contains($mapContract)) { throw "Capability map is missing contract: $mapContract" }
}

$overviewNav = $mkdocs.IndexOf('- Overview: index.md')
$gettingNav = $mkdocs.IndexOf('- Getting Started:')
$releasesNav = $mkdocs.IndexOf('- Releases:')
if ($overviewNav -lt 0 -or $gettingNav -le $overviewNav -or $releasesNav -le $gettingNav) {
    throw "Navigation must separate Overview, Getting Started, and Releases in that order."
}

if ($SiteDirectory) {
    $htmlPath = Join-Path $SiteDirectory "index.html"
    if (-not (Test-Path -LiteralPath $htmlPath -PathType Leaf)) { throw "Built overview is missing: $htmlPath" }
    $html = [IO.File]::ReadAllText($htmlPath)
    foreach ($renderedContract in @(
        '<title>Selenium Test Lens',
        'name="description" content="Add observable Selenium interactions',
        'href="getting-started/"',
        'href="ai-assisted-integration/"',
        'href="#product-capabilities"',
        'id="product-capabilities"',
        'alt="Selenium Test Lens"',
        'width="720"',
        'height="184"',
        'loading="lazy"'
    )) {
        if (-not $html.Contains($renderedContract)) { throw "Built overview is missing contract: $renderedContract" }
    }
}

Write-Host "Product overview validation OK: complete $targetRelease capability story, separated releases, maintained evidence map, and rendered metadata/CTA contracts."
