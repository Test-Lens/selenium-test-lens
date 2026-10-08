[CmdletBinding()]
param(
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$BaselineTag,
    [string]$ExpectedSince
)

$ErrorActionPreference = "Stop"
Import-Module (Join-Path $PSScriptRoot "ReleaseReactor.psm1") -Force
Import-Module (Join-Path $PSScriptRoot "PublicApiReleaseLine.psm1") -Force
$rootPom = Read-TestLensPom (Join-Path $RepositoryRoot "pom.xml")
$reactorVersion = Get-TestLensPomText $rootPom "/m:project/m:version"
if ([string]::IsNullOrWhiteSpace($BaselineTag) -or [string]::IsNullOrWhiteSpace($ExpectedSince)) {
    $tags = @(& git -C $RepositoryRoot tag --list "v*.*.*")
    if ($LASTEXITCODE -ne 0) { throw "Unable to read release tags" }
    $releaseLine = Resolve-TestLensApiReleaseLine -CurrentVersion $reactorVersion -ReleaseTags $tags
    if ([string]::IsNullOrWhiteSpace($BaselineTag)) { $BaselineTag = $releaseLine.BaselineTag }
    if ([string]::IsNullOrWhiteSpace($ExpectedSince)) { $ExpectedSince = $releaseLine.ExpectedSince }
}
Write-Host "API release line: reactor=$reactorVersion baseline=$BaselineTag expectedSince=$ExpectedSince"
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

function Read-CompatibilityProjection([string[]]$ManifestLines, [object[]]$ClassificationRows, [string]$Label) {
    $compatibility = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    @($ClassificationRows | Where-Object { $_.Classification -in @("USER_API", "ADVANCED_API") }) |
        ForEach-Object { [void]$compatibility.Add($_.Type) }
    if ($compatibility.Count -eq 0) { throw "$Label classification has no USER_API or ADVANCED_API types" }

    $types = [Collections.Generic.List[string]]::new()
    $callables = 0
    $included = $false
    foreach ($line in $ManifestLines) {
        if ($line -like 'TYPE *') {
            $type = $line.Substring(5)
            $included = $compatibility.Contains($type)
            if ($included) { $types.Add($type) }
        } elseif ($included -and $line -match '^\s+public .*\(') {
            $callables++
        }
    }
    return [pscustomobject]@{ Types = $types.ToArray(); Callables = $callables }
}

function Get-RepositoryRelativePath([string]$Root, [string]$Path) {
    $separator = [IO.Path]::DirectorySeparatorChar
    $alternate = [IO.Path]::AltDirectorySeparatorChar
    $rootPath = [IO.Path]::GetFullPath($Root).Replace($alternate, $separator).TrimEnd($separator)
    $sourcePath = [IO.Path]::GetFullPath($Path).Replace($alternate, $separator)
    $prefix = $rootPath + $separator
    $comparison = if ([Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT) {
        [StringComparison]::OrdinalIgnoreCase
    } else {
        [StringComparison]::Ordinal
    }
    if ($sourcePath.StartsWith($prefix, $comparison)) {
        return $sourcePath.Substring($prefix.Length)
    }
    return $sourcePath
}

$currentLines = @(Get-Content -LiteralPath $manifestPath -Encoding utf8)
$baselineLines = @(& git -C $RepositoryRoot show "${BaselineTag}:docs/reference/public-api-manifest.txt")
if ($LASTEXITCODE -ne 0 -or $baselineLines.Count -eq 0) {
    throw "Unable to read the public API baseline from $BaselineTag"
}

$current = Read-ManifestSummary $currentLines "Current"
$baselineClassificationLines = @(& git -C $RepositoryRoot show "${BaselineTag}:docs/reference/public-api-classification.csv")
if ($LASTEXITCODE -ne 0 -or $baselineClassificationLines.Count -eq 0) {
    throw "Unable to read the public API classification from $BaselineTag"
}
$baselineClassification = @($baselineClassificationLines | ConvertFrom-Csv)
$baselineProjection = Read-CompatibilityProjection $baselineLines $baselineClassification $BaselineTag
$baselineTypes = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$baselineProjection.Types | ForEach-Object { [void]$baselineTypes.Add($_) }
$newTypes = @(Read-PublicTypes $currentLines | Where-Object { -not $baselineTypes.Contains($_) })

$sourceRoots = @(
    "selenium-test-lens-core",
    "selenium-test-lens-overlay",
    "selenium-test-lens-selenium",
    "selenium-test-lens-react",
    "selenium-test-lens-junit5",
    "selenium-test-lens-testng",
    "selenium-test-lens-allure",
    "selenium-test-lens-application-model",
    "selenium-test-lens-selector-engine",
    "selenium-test-lens-selector-live",
    "selenium-test-lens-application-mapper",
    "selenium-test-lens-application-tooling",
    "selenium-test-lens-selector-tooling",
    "selenium-test-lens-test-engineering-studio",
    "selenium-test-lens-test-engineering-maven-plugin"
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
    # Member types declared in an interface are implicitly public even when the
    # source omits the redundant modifier. Top-level API declarations must keep
    # the explicit public modifier.
    $publicModifier = if ($type.Contains('$')) { '(?:public\s+)?' } else { 'public\s+' }
    $declarationIndex = -1
    for ($index = 0; $index -lt $lines.Count; $index++) {
        if ($lines[$index] -match "^\s*$publicModifier(?:(?:static|final|abstract)\s+)*(?:class|interface|enum|record)\s+$escapedName(?:\s|\{|\(|<)") {
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
        $relative = Get-RepositoryRelativePath $RepositoryRoot $source
        $violations.Add("${type}: expected @since $ExpectedSince in $relative")
    }
}

Write-Host ("Public API inventory: {0} types, {1} callables" -f $current.Types, $current.Callables)
Write-Host ("Baseline compatibility projection {0}: {1} types, {2} callables" -f $BaselineTag, $baselineProjection.Types.Count, $baselineProjection.Callables)
Write-Host ("Compatibility delta since baseline: {0} types, {1} callables" -f ($current.Types - $baselineProjection.Types.Count), ($current.Callables - $baselineProjection.Callables))

if ($violations.Count -gt 0) {
    $violations | ForEach-Object { Write-Error $_ -ErrorAction Continue }
    exit 1
}

Write-Host "@since $ExpectedSince is present on all $($newTypes.Count) public types added after $BaselineTag."
