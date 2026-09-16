[CmdletBinding()]
param(
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$BaselineTag = "v0.3.0",
    [string]$ExpectedSince = "0.3.1"
)

$ErrorActionPreference = "Stop"
$manifestPath = Join-Path $RepositoryRoot "docs/reference/public-api-manifest.txt"
if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
    throw "Public API manifest does not exist: $manifestPath"
}

function Read-ManifestSummary([string[]]$Lines, [string]$Label) {
    $typesLine = $Lines | Where-Object { $_ -match '^# Types: ' } | Select-Object -First 1
    $callablesLine = $Lines | Where-Object { $_ -match '^# Public callable methods/constructors: ' } | Select-Object -First 1
    if (-not $typesLine -or -not $callablesLine) {
        throw "$Label manifest does not contain the generated inventory header"
    }
    [pscustomobject]@{
        Types = [int]($typesLine -replace '^# Types: ', '')
        Callables = [int]($callablesLine -replace '^# Public callable methods/constructors: ', '')
    }
}

function Read-PublicTypes([string[]]$Lines) {
    @($Lines | Where-Object { $_ -like 'TYPE *' } | ForEach-Object { $_.Substring(5) })
}

$currentLines = @(Get-Content -LiteralPath $manifestPath -Encoding utf8)
$baselineLines = @(& git -C $RepositoryRoot show "${BaselineTag}:docs/reference/public-api-manifest.txt")
if ($LASTEXITCODE -ne 0 -or $baselineLines.Count -eq 0) {
    throw "Unable to read the public API baseline from $BaselineTag"
}

$current = Read-ManifestSummary $currentLines "Current"
$baseline = Read-ManifestSummary $baselineLines $BaselineTag
$baselineTypes = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
Read-PublicTypes $baselineLines | ForEach-Object { [void]$baselineTypes.Add($_) }
$newTypes = @(Read-PublicTypes $currentLines | Where-Object { -not $baselineTypes.Contains($_) })

$sourceRoots = @(
    "selenium-test-lens-core",
    "selenium-test-lens-overlay",
    "selenium-test-lens-selenium",
    "selenium-test-lens-react",
    "selenium-test-lens-junit5",
    "selenium-test-lens-testng",
    "selenium-test-lens-allure"
) | ForEach-Object { Join-Path $RepositoryRoot "$_/src/main/java" }

$violations = [Collections.Generic.List[string]]::new()
foreach ($type in $newTypes) {
    $outerType = ($type -split '\$')[0]
    $outerSimpleName = $outerType.Substring($outerType.LastIndexOf('.') + 1)
    $declaredSimpleName = if ($type.Contains('$')) { ($type -split '\$')[-1] } else { $outerSimpleName }
    $source = $sourceRoots | ForEach-Object {
        $candidate = Join-Path $_ (($outerType -replace '\.', [IO.Path]::DirectorySeparatorChar) + '.java')
        if (Test-Path -LiteralPath $candidate -PathType Leaf) { $candidate }
    } | Select-Object -First 1
    if (-not $source) {
        $violations.Add("${type}: source file was not found")
        continue
    }

    $lines = @(Get-Content -LiteralPath $source -Encoding utf8)
    $escapedName = [regex]::Escape($declaredSimpleName)
    $declarationIndex = -1
    for ($index = 0; $index -lt $lines.Count; $index++) {
        if ($lines[$index] -match "^\s*public\s+(?:(?:static|final|abstract)\s+)*(?:class|interface|enum|record)\s+$escapedName(?:\s|\{|\(|<)") {
            $declarationIndex = $index
            break
        }
    }
    if ($declarationIndex -lt 0) {
        $violations.Add("${type}: public declaration was not found in $source")
        continue
    }

    $end = $declarationIndex - 1
    while ($end -ge 0 -and ($lines[$end].Trim().Length -eq 0 -or $lines[$end].Trim().StartsWith('@'))) { $end-- }
    if ($end -lt 0 -or -not $lines[$end].Contains('*/')) {
        $violations.Add("${type}: public declaration has no adjacent Javadoc")
        continue
    }
    $start = $end
    while ($start -ge 0 -and -not $lines[$start].Contains('/**')) { $start-- }
    if ($start -lt 0) {
        $violations.Add("${type}: public declaration has no adjacent Javadoc")
        continue
    }
    $javadoc = ($lines[$start..$end] -join "`n")
    if ($javadoc -notmatch "(?m)@since\s+$([regex]::Escape($ExpectedSince))(?:\s|\*/|$)") {
        $relative = [IO.Path]::GetRelativePath($RepositoryRoot, $source)
        $violations.Add("${type}: expected @since $ExpectedSince in $relative")
    }
}

Write-Host ("Public API inventory: {0} types, {1} callables" -f $current.Types, $current.Callables)
Write-Host ("Baseline {0}: {1} types, {2} callables" -f $BaselineTag, $baseline.Types, $baseline.Callables)
Write-Host ("Added since baseline: {0} types, {1} callables" -f ($current.Types - $baseline.Types), ($current.Callables - $baseline.Callables))

if ($violations.Count -gt 0) {
    $violations | ForEach-Object { Write-Error $_ -ErrorAction Continue }
    exit 1
}

Write-Host "@since $ExpectedSince is present on all $($newTypes.Count) public types added after $BaselineTag."
