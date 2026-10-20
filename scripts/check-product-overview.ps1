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
    'title: Selenium observability and diagnostic evidence',
    'description: Test Lens provides observable Selenium interactions, retryable actions, polling assertions, browser HUD, and diagnostic evidence for Java WebDriver projects.',
    ('# Test Lens ' + [char]0x2014 + ' Selenium observability and diagnostic evidence'),
    '[Use Test Lens in your Selenium tests](getting-started.md)',
    '[AI Integration Prompt Builder](ai-assisted-integration.md)',
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

$nav = ($mkdocs -split '(?m)^nav:\s*$')[1] -split '(?m)^markdown_extensions:' | Select-Object -First 1
if ($overview -match 'Test Engineering Studio|test-engineering-studio|generate-tests-with-ai' -or $nav -match 'test-engineering-studio|generate-tests-with-ai|ai/workflow-orchestration|ai/test-engineering\.md|ai/0.5.0-publication-candidates') {
    throw "Experimental Studio must not be promoted on the homepage or in navigation."
}
foreach ($kept in @('HUD Studio: observability/hud-studio.md', 'AI Integration Prompt Builder: ai-assisted-integration.md')) {
    if (-not $nav.Contains($kept)) { throw "Required independent tool is missing: $kept" }
}
foreach ($page in @('test-engineering-studio', 'test-engineering-studio-getting-started', 'generate-tests-with-ai', 'workflow-orchestration', 'test-engineering', '0.5.0-publication-candidates')) {
    $source = [IO.File]::ReadAllText((Join-Path $root "docs/ai/$page.md"))
    $notice = 'Experimental feature ' + [char]0x2014 + ' temporarily unavailable'
    if (-not $source.Contains($notice)) { throw "Missing availability notice: $page" }
    if ($SiteDirectory) {
        $built = [IO.File]::ReadAllText((Join-Path $SiteDirectory "ai/$page/index.html"))
        if (-not $built.Contains($notice)) { throw "Missing rendered availability notice: $page" }
    }
}
$readme = [IO.File]::ReadAllText((Join-Path $root 'README.md'))
if ($readme -match 'test-engineering-studio|:studio') { throw "README must not link to or launch Studio." }
$gateway = [IO.File]::ReadAllText((Join-Path $root 'overrides/mike-root.html'))
if ($gateway -match 'Test Engineering Studio|test-engineering-studio') { throw "Root gateway must not promote Studio." }

if ($SiteDirectory) {
    $htmlPath = Join-Path $SiteDirectory "index.html"
    if (-not (Test-Path -LiteralPath $htmlPath -PathType Leaf)) { throw "Built overview is missing: $htmlPath" }
    $html = [IO.File]::ReadAllText($htmlPath)
    foreach ($renderedContract in @(
        '<title>Selenium observability and diagnostic evidence - Test Lens</title>',
        'name="description" content="Test Lens provides observable Selenium interactions, retryable actions, polling assertions, browser HUD, and diagnostic evidence for Java WebDriver projects.',
        'href="getting-started/"',
        'href="ai-assisted-integration/"',
        'href="#product-capabilities"',
        'id="product-capabilities"',
        'alt="Test Lens"',
        'width="720"',
        'height="184"',
        'loading="lazy"'
    )) {
        if (-not $html.Contains($renderedContract)) { throw "Built overview is missing contract: $renderedContract" }
    }
}

Write-Host "Product overview validation OK: complete $targetRelease capability story, separated releases, maintained evidence map, and rendered metadata/CTA contracts."
