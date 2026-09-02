param(
    [switch]$Update
)

$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$modules = [ordered]@{
    "selenium-test-lens-core" = "selenium-test-lens-core"
    "selenium-test-lens-overlay" = "selenium-test-lens-overlay"
    "selenium-test-lens-selenium" = "selenium-test-lens-selenium"
    "selenium-test-lens-junit5" = "selenium-test-lens-junit5"
    "selenium-test-lens-react" = "selenium-test-lens-react"
}
$manifestPath = Join-Path $repositoryRoot "docs/reference/public-api-manifest.txt"
$catalogPath = Join-Path $repositoryRoot "docs/reference/public-api-catalog.md"
$catalogDirectory = Join-Path $repositoryRoot "docs/reference/public-api"
$classificationPath = Join-Path $repositoryRoot "docs/reference/public-api-classification.csv"
$allowedClassifications = @("USER_API", "ADVANCED_API", "LOW_LEVEL_API", "INTERNAL_STYLE_PUBLIC")

function Normalize-Signature([string]$line) {
    return ($line.Trim() -replace '\s+', ' ' -replace ';$', '')
}

$lines = [System.Collections.Generic.List[string]]::new()
$types = [System.Collections.Generic.List[object]]::new()
$typeCount = 0
$callableCount = 0

foreach ($entry in $modules.GetEnumerator()) {
    $classesRoot = Join-Path $repositoryRoot "$($entry.Value)/target/classes"
    if (-not (Test-Path $classesRoot)) {
        throw "Missing compiled classes for $($entry.Key). Run 'mvn -DskipTests compile' first."
    }

    $lines.Add("MODULE $($entry.Key)")
    [array]$classFiles = Get-ChildItem -Path $classesRoot -Filter *.class -File -Recurse |
        Where-Object { $_.Name -notmatch '\$\d+\.class$' }
    [Array]::Sort($classFiles, [System.Collections.Generic.Comparer[object]]::Create(
        { param($left, $right) [StringComparer]::Ordinal.Compare($left.FullName, $right.FullName) }
    ))

    foreach ($classFile in $classFiles) {
        $relative = $classFile.FullName.Substring($classesRoot.Length + 1)
        $className = ($relative -replace '\.class$', '' -replace '[\\/]', '.')
        $output = & javap -public -classpath $classesRoot $className 2>$null
        if ($LASTEXITCODE -ne 0) {
            throw "javap failed for $className"
        }
        $declaration = $output | Where-Object { $_ -match '^public .*(class|interface|enum|record) ' } | Select-Object -First 1
        if (-not $declaration) {
            continue
        }

        $typeCount++
        $lines.Add("TYPE $className")
        $signatures = [System.Collections.Generic.List[string]]::new()
        foreach ($line in $output) {
            $normalized = Normalize-Signature $line
            if ($normalized -match '^public ' -and $normalized -ne (Normalize-Signature $declaration)) {
                $lines.Add("  $normalized")
                $signatures.Add($normalized)
                if ($normalized -match '\(') {
                    $callableCount++
                }
            }
        }
        $kind = if ($declaration -match '\binterface\b') { "interface" }
            elseif ($declaration -match '\benum\b|extends java\.lang\.Enum') { "enum" }
            elseif ($declaration -match '\brecord\b|extends java\.lang\.Record') { "record" }
            else { "class" }
        $packageName = $className.Substring(0, $className.LastIndexOf('.'))
        $types.Add([pscustomobject]@{
            Module = $entry.Key
            Type = $className
            Package = $packageName
            Kind = $kind
            Signatures = $signatures.ToArray()
        })
    }
}

if (-not (Test-Path $classificationPath)) {
    throw "Public API classification is missing: $classificationPath"
}
$classificationRows = @(Import-Csv $classificationPath)
$classifications = @{}
foreach ($row in $classificationRows) {
    if ([string]::IsNullOrWhiteSpace($row.Type) -or $row.Classification -notin $allowedClassifications) {
        throw "Invalid public API classification row for '$($row.Type)': '$($row.Classification)'"
    }
    if ($classifications.ContainsKey($row.Type)) {
        throw "Duplicate public API classification for $($row.Type)"
    }
    $classifications[$row.Type] = $row
}
$compiledNames = @($types | ForEach-Object Type)
$missingClassifications = @($compiledNames | Where-Object { -not $classifications.ContainsKey($_) })
$staleClassifications = @($classifications.Keys | Where-Object { $_ -notin $compiledNames })
if ($missingClassifications.Count -gt 0 -or $staleClassifications.Count -gt 0) {
    $details = @(
        $missingClassifications | ForEach-Object { "unclassified: $_" }
        $staleClassifications | ForEach-Object { "not public anymore: $_" }
    ) -join [Environment]::NewLine
    throw "Public API classification is incomplete or stale. Review docs/reference/public-api-classification.csv:`n$details"
}
foreach ($type in $types) {
    $row = $classifications[$type.Type]
    if ($row.Module -ne $type.Module) {
        throw "Classification module mismatch for $($type.Type): expected '$($type.Module)', found '$($row.Module)'"
    }
    if ($row.Classification -eq "USER_API") {
        if ([string]::IsNullOrWhiteSpace($row.Documentation)) {
            throw "USER_API type has no functional documentation mapping: $($type.Type)"
        }
        $documentationPath = Join-Path $repositoryRoot ($row.Documentation -replace '/', [IO.Path]::DirectorySeparatorChar)
        if (-not (Test-Path $documentationPath -PathType Leaf)) {
            throw "Documentation mapping for $($type.Type) does not exist: $($row.Documentation)"
        }
    }
}
$classificationSummary = ($allowedClassifications | ForEach-Object {
    $classificationName = $_
    $classificationCount = @($classificationRows | Where-Object Classification -eq $classificationName).Count
    "$classificationName=$classificationCount"
}) -join ", "

function ConvertTo-LfText([System.Collections.IEnumerable] $Lines) {
    return (($Lines -join "`n").TrimEnd("`r", "`n")) + "`n"
}

$header = @(
    "# Generated public API manifest. Do not edit by hand."
    "# Types: $typeCount"
    "# Public callable methods/constructors: $callableCount"
    "# Regenerate: mvn -DskipTests compile; ./scripts/check-public-api.ps1 -Update"
    ""
)
$actual = ConvertTo-LfText ($header + $lines)

$catalog = [System.Collections.Generic.List[string]]::new()
$catalog.Add("---")
$catalog.Add("search:")
$catalog.Add("  exclude: true")
$catalog.Add("---")
$catalog.Add("")
$catalog.Add("# Exhaustive public API catalog")
$catalog.Add("")
$catalog.Add("This generated catalog is the optional binary-surface reference for every published public type, constructor, method, and field. Start with [Capabilities](../capabilities.md) and the functional navigation for behavior, workflows, and examples. Internal-style types appear only because Java consumers can currently access them.")
$catalog.Add("")
$catalog.Add("Inventory: **$typeCount public types** and **$callableCount public callable methods/constructors** (including public nested types and compiler-generated record/enum members).")
$catalog.Add("")
$catalog.Add("Classifications: `USER_API` is the normal consumer path; `ADVANCED_API` is supported specialized functionality; `LOW_LEVEL_API` exposes lower abstractions; `INTERNAL_STYLE_PUBLIC` is binary-public implementation surface and is not recommended for application code.")
$catalog.Add("")
$catalog.Add("Signature details are split by published artifact and Java package to keep individual pages usable. These generated pages are excluded from site search.")
$catalog.Add("")
$catalogParts = @{}
foreach ($entry in $modules.GetEnumerator()) {
    $catalog.Add("## $($entry.Key)")
    $catalog.Add("")
    $catalog.Add("| Type | Package | Classification | Kind | Surface |")
    $catalog.Add("| --- | --- | --- | --- | --- |")
    foreach ($type in $types | Where-Object Module -eq $entry.Key) {
        $classification = $classifications[$type.Type].Classification
        $partKey = "$($type.Module)|$($type.Package)"
        if (-not $catalogParts.ContainsKey($partKey)) {
            $partSlug = (($type.Module + "-" + $type.Package) -replace '[^A-Za-z0-9]+', '-').Trim('-').ToLowerInvariant()
            $catalogParts[$partKey] = [pscustomobject]@{
                FileName = "$partSlug.md"
                Module = $type.Module
                Package = $type.Package
                Types = [System.Collections.Generic.List[object]]::new()
            }
        }
        $catalogParts[$partKey].Types.Add($type)
        $anchor = ($type.Type -replace '[^A-Za-z0-9]+', '-').Trim('-').ToLowerInvariant()
        $surfaceLink = "public-api/$($catalogParts[$partKey].FileName)#$anchor"
        $catalog.Add(("| `{0}` | `{1}` | `{2}` | `{3}` | [signatures]({4}) |" -f $type.Type, $type.Package, $classification, $type.Kind, $surfaceLink))
    }
    $catalog.Add("")
}
$actualCatalog = ConvertTo-LfText $catalog

$actualCatalogParts = @{}
foreach ($part in $catalogParts.Values) {
    $partLines = [System.Collections.Generic.List[string]]::new()
    $partLines.Add("---")
    $partLines.Add("search:")
    $partLines.Add("  exclude: true")
    $partLines.Add("---")
    $partLines.Add("")
    $partLines.Add(('# {0}: `{1}`' -f $part.Module, $part.Package))
    $partLines.Add("")
    $partLines.Add("Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.")
    $partLines.Add("")
    foreach ($type in $part.Types) {
        $classification = $classifications[$type.Type].Classification
        $anchor = ($type.Type -replace '[^A-Za-z0-9]+', '-').Trim('-').ToLowerInvariant()
        $partLines.Add(('## `{0}` {{#{1}}}' -f $type.Type, $anchor))
        $partLines.Add("")
        $partLines.Add(('- Artifact/module: `{0}`' -f $type.Module))
        $partLines.Add(('- Package: `{0}`' -f $type.Package))
        $partLines.Add(('- Classification: `{0}`' -f $classification))
        $partLines.Add(('- Type kind: `{0}`' -f $type.Kind))
        if (-not [string]::IsNullOrWhiteSpace($classifications[$type.Type].Documentation)) {
            $relativeDocumentation = $classifications[$type.Type].Documentation -replace '^docs/', '../../'
            $partLines.Add(('- Functional documentation: [{0}]({1})' -f $classifications[$type.Type].Documentation, $relativeDocumentation))
        }
        $partLines.Add("")
        $partLines.Add('```java')
        foreach ($signature in $type.Signatures) { $partLines.Add($signature) }
        $partLines.Add('```')
        $partLines.Add("")
    }
    $actualCatalogParts[$part.FileName] = ConvertTo-LfText $partLines
}

if ($Update) {
    $directory = Split-Path -Parent $manifestPath
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    [System.IO.File]::WriteAllText($manifestPath, $actual, [System.Text.UTF8Encoding]::new($false))
    [System.IO.File]::WriteAllText($catalogPath, $actualCatalog, [System.Text.UTF8Encoding]::new($false))
    New-Item -ItemType Directory -Force -Path $catalogDirectory | Out-Null
    Get-ChildItem -LiteralPath $catalogDirectory -Filter *.md -File | ForEach-Object {
        if (-not $actualCatalogParts.ContainsKey($_.Name)) { Remove-Item -LiteralPath $_.FullName -Force }
    }
    foreach ($partName in $actualCatalogParts.Keys) {
        [System.IO.File]::WriteAllText((Join-Path $catalogDirectory $partName), $actualCatalogParts[$partName], [System.Text.UTF8Encoding]::new($false))
    }
    Write-Host "Updated public API manifest: $typeCount types, $callableCount callables; $classificationSummary"
    exit 0
}

if (-not (Test-Path $manifestPath)) {
    throw "Public API manifest is missing. Run this script with -Update and review the result."
}
$expectedCatalog = [System.IO.File]::ReadAllText($catalogPath).Replace("`r`n", "`n")
if ($expectedCatalog -ne $actualCatalog) {
    Write-Error "Generated API catalog is stale. Run './scripts/check-public-api.ps1 -Update' and review it."
    exit 1
}
foreach ($partName in $actualCatalogParts.Keys) {
    $partPath = Join-Path $catalogDirectory $partName
    if (-not (Test-Path $partPath)) {
        Write-Error "Generated API catalog part is missing: $partName"
        exit 1
    }
    $expectedPart = [System.IO.File]::ReadAllText($partPath).Replace("`r`n", "`n")
    if ($expectedPart -ne $actualCatalogParts[$partName]) {
        Write-Error "Generated API catalog part is stale: $partName"
        exit 1
    }
}
$extraCatalogParts = @(Get-ChildItem -LiteralPath $catalogDirectory -Filter *.md -File | Where-Object { -not $actualCatalogParts.ContainsKey($_.Name) })
if ($extraCatalogParts.Count -gt 0) {
    Write-Error "Unexpected generated API catalog parts: $($extraCatalogParts.Name -join ', ')"
    exit 1
}
$expected = [System.IO.File]::ReadAllText($manifestPath).Replace("`r`n", "`n")
if ($expected -ne $actual) {
    Write-Error "Public API changed. Run './scripts/check-public-api.ps1 -Update', document the change, and commit the reviewed manifest."
    exit 1
}

Write-Host "Public API manifest OK: $typeCount types, $callableCount callables; $classificationSummary"
