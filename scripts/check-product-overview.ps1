param([string]$SiteDirectory)
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$overviewPath = Join-Path $root "docs/index.md"
$studioTutorialPath = Join-Path $root "docs/ai/generate-tests-with-ai.md"
$mapPath = Join-Path $root "docs/maintainers/product-capability-map.md"
$mkdocsPath = Join-Path $root "mkdocs.yml"

foreach ($path in @($overviewPath, $studioTutorialPath, $mapPath, $mkdocsPath)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Required overview source is missing: $path" }
}

$overview = [IO.File]::ReadAllText($overviewPath)
$studioTutorial = [IO.File]::ReadAllText($studioTutorialPath)
$capabilityMap = [IO.File]::ReadAllText($mapPath)
$mkdocs = [IO.File]::ReadAllText($mkdocsPath)

$targetMatch = [regex]::Match($mkdocs, '(?m)^\s{4}target_release:\s*(\d+\.\d+\.\d+)\s*$')
if (-not $targetMatch.Success) { throw "mkdocs.yml must define one concrete AI integration target_release." }
$targetRelease = $targetMatch.Groups[1].Value

foreach ($contract in @(
    'title: Selenium Test Lens',
    '# Selenium observability and AI-assisted test engineering',
    '[Use Test Lens in your Selenium tests](getting-started.md)',
    '[Generate and repair tests with AI](ai/test-engineering-studio-getting-started.md)',
    '[AI Integration Prompt Builder](ai-assisted-integration.md)',
    '[Explore capabilities](#product-capabilities)',
    '## AI Test Engineering Studio',
    ('Requirement ' + [char]0x2192 + ' AI Architect ' + [char]0x2192 + ' AI Implementer ' + [char]0x2192 + ' Validation ' + [char]0x2192 + ' Compile ' + [char]0x2192 + ' Selenium ' + [char]0x2192 + ' AI Reviewer'),
    ('Failure ' + [char]0x2192 + ' Diagnosis ' + [char]0x2192 + ' Selector Candidate ' + [char]0x2192 + ' Review ' + [char]0x2192 + ' Approval ' + [char]0x2192 + ' Repair ' + [char]0x2192 + ' Rerun'),
    'Approve and apply',
    '### Quick start: run Studio from your Maven project',
    'mvn io.github.test-lens:test-lens-test-engineering-maven-plugin:0.5.1:studio',
    '### Choose the AI workflow that fits',
    '| AI Integration Prompt Builder | Test Engineering Studio |',
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
$studioNav = $mkdocs.IndexOf('- Test Engineering Studio:')
$usingNav = $mkdocs.IndexOf('- Using Test Lens:')
$releasesNav = $mkdocs.IndexOf('- Releases:')
if ($overviewNav -lt 0 -or $gettingNav -le $overviewNav -or $studioNav -le $gettingNav -or $usingNav -le $studioNav -or $releasesNav -le $usingNav) {
    throw "Navigation must expose Test Engineering Studio directly between Getting Started and Using Test Lens, before Releases."
}
foreach ($navContract in @(
    'Overview: ai/test-engineering-studio.md',
    'Quick Start: ai/test-engineering-studio-getting-started.md',
    'Generate Tests with AI: ai/generate-tests-with-ai.md',
    'AI Integration Prompt Builder: ai-assisted-integration.md'
)) {
    if (-not $mkdocs.Contains($navContract)) { throw "Navigation is missing discoverability contract: $navContract" }
}
$studioNavMatch = [regex]::Match($mkdocs, '(?ms)^  - Test Engineering Studio:\r?\n(?<section>.*?)(?=^  - |\z)')
if (-not $studioNavMatch.Success) { throw "Navigation is missing the Test Engineering Studio section." }
$studioNav = $studioNavMatch.Groups['section'].Value
if ($studioNav -match '#') { throw "Test Engineering Studio navigation must use Markdown files, not URL fragments." }
$studioTargets = [regex]::Matches($studioNav, '(?m)^[ \t]+-[^\r\n]*?:[ \t]+([^\r\n]+\.md)[ \t]*$') | ForEach-Object { $_.Groups[1].Value }
if (@($studioTargets | Select-Object -Unique).Count -ne @($studioTargets).Count) { throw "Test Engineering Studio navigation must not list the same Markdown page more than once." }
$studioOverview = Get-Content -LiteralPath (Resolve-Path (Join-Path $PSScriptRoot '..\docs\ai\test-engineering-studio.md')) -Raw
$studioQuickStart = Get-Content -LiteralPath (Resolve-Path (Join-Path $PSScriptRoot '..\docs\ai\test-engineering-studio-getting-started.md')) -Raw
foreach ($sectionContract in @('## Run and diagnose', '## Review a repair', '## Configuration and autodiscovery', '## Troubleshooting')) {
    if (-not $studioOverview.Contains($sectionContract)) { throw "Studio overview is missing section: $sectionContract" }
}
foreach ($sectionContract in @('## 2. Split project and machine-local configuration', '## Troubleshooting')) {
    if (-not $studioQuickStart.Contains($sectionContract)) { throw "Studio Quick Start is missing section: $sectionContract" }
}
foreach ($tutorialContract in @(
    '# Generate your first Selenium test with AI',
    'Verify that logging in with an invalid password displays the expected error and does not authenticate the user.',
    'Scan project',
    'Map application',
    'Correlate Page Objects',
    'Generate plan',
    'Generate test',
    'Approve and apply',
    'Run verification',
    'JUnit 5'
)) {
    if (-not $studioTutorial.Contains($tutorialContract)) { throw "Studio tutorial is missing discoverability contract: $tutorialContract" }
}

if ($SiteDirectory) {
    $htmlPath = Join-Path $SiteDirectory "index.html"
    if (-not (Test-Path -LiteralPath $htmlPath -PathType Leaf)) { throw "Built overview is missing: $htmlPath" }
    $html = [IO.File]::ReadAllText($htmlPath)
    foreach ($renderedContract in @(
        '<title>Selenium Test Lens',
        'name="description" content="Selenium observability and AI-assisted test engineering',
        'href="getting-started/"',
        'href="ai/test-engineering-studio-getting-started/"',
        'href="ai/generate-tests-with-ai/"',
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
