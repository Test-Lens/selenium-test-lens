$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$manifestPath = Join-Path $repositoryRoot "docs/reference/public-api-manifest.txt"
$docsRoot = Join-Path $repositoryRoot "docs"

if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
    throw "Public API manifest is missing: $manifestPath"
}

function Split-Parameters([string]$parameters) {
    if ([string]::IsNullOrWhiteSpace($parameters)) {
        return @()
    }
    $result = [System.Collections.Generic.List[string]]::new()
    $start = 0
    $depth = 0
    for ($index = 0; $index -lt $parameters.Length; $index++) {
        switch ($parameters[$index]) {
            '<' { $depth++ }
            '>' { if ($depth -gt 0) { $depth-- } }
            ',' {
                if ($depth -eq 0) {
                    $result.Add($parameters.Substring($start, $index - $start).Trim())
                    $start = $index + 1
                }
            }
        }
    }
    $result.Add($parameters.Substring($start).Trim())
    return $result.ToArray()
}

function Simplify-Type([string]$type) {
    $value = $type.Trim()
    $value = $value -replace '\s+[A-Za-z_$][A-Za-z0-9_$]*$', ''
    $value = $value -replace '(?:java|javax|org|io|com)(?:\.[a-z_][A-Za-z0-9_$]*)*\.([A-Z][A-Za-z0-9_$]*)', '$1'
    $value = $value.Replace('$', '.')
    return ($value -replace '\s+', '')
}

function Signature-Key([string]$signature, [string]$typeName) {
    $value = ($signature.Trim() -replace ';$', '')
    $value = $value -replace '^public\s+', ''
    $value = $value -replace '^(?:static|final|abstract|synchronized|native|strictfp)\s+', ''
    if ($value -notmatch '^(?<prefix>.*?)\((?<parameters>.*)\)$') {
        throw "Not a callable signature: $signature"
    }

    $before = $Matches.prefix.Trim()
    $parameters = $Matches.parameters
    $tokens = @($before -split '\s+' | Where-Object { $_ })
    if ($tokens.Count -eq 0) {
        throw "Missing callable name: $signature"
    }
    $callableName = $tokens[-1]
    $simpleTypeName = (($typeName -split '\.')[-1]).Replace('$', '.')
    $simpleCallableFullName = (($callableName -split '\.')[-1]).Replace('$', '.')
    $simpleCallableName = ($simpleCallableFullName -split '\.')[-1]
    $isConstructor = $simpleCallableName -eq (($simpleTypeName -split '\.')[-1])
    $returnType = if ($isConstructor) { '<constructor>' } else {
        if ($tokens.Count -lt 2) { throw "Missing return type: $signature" }
        Simplify-Type $tokens[-2]
    }
    $parameterTypes = @(Split-Parameters $parameters | ForEach-Object { Simplify-Type $_ })
    return "$returnType $simpleCallableName($($parameterTypes -join ','))"
}

$manifestByType = @{}
$currentType = $null
foreach ($line in Get-Content -LiteralPath $manifestPath) {
    if ($line -match '^TYPE\s+(.+)$') {
        $currentType = $Matches[1]
        $manifestByType[$currentType] = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
        continue
    }
    if ($currentType -and $line -match '^\s{2}(public .+\(.*\))$') {
        [void]$manifestByType[$currentType].Add((Signature-Key $Matches[1] $currentType))
    }
}

$markerPattern = '(?ms)<!--\s*API SIGNATURES:\s*(?<type>[^\s]+)\s*-->\s*```java\s*\r?\n(?<body>.*?)\r?\n```'
$checkedBlocks = 0
$checkedSignatures = 0
$failures = [System.Collections.Generic.List[string]]::new()
$docFiles = Get-ChildItem -LiteralPath $docsRoot -Recurse -File -Filter '*.md' |
    Where-Object { $_.FullName -notmatch '[\\/]reference[\\/]public-api[\\/]' }

foreach ($docFile in $docFiles) {
    $relativePath = $docFile.FullName.Substring($repositoryRoot.Length + 1).Replace('\', '/')
    $content = [System.IO.File]::ReadAllText($docFile.FullName)
    foreach ($match in [regex]::Matches($content, $markerPattern)) {
        $checkedBlocks++
        $typeName = $match.Groups['type'].Value
        if (-not $manifestByType.ContainsKey($typeName)) {
            $failures.Add("$relativePath`: marker names a type absent from the public manifest: $typeName")
            continue
        }
        $signatureLines = @($match.Groups['body'].Value -split '\r?\n' | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
        foreach ($signatureLine in $signatureLines) {
            $checkedSignatures++
            try {
                $key = Signature-Key $signatureLine $typeName
                if (-not $manifestByType[$typeName].Contains($key)) {
                    $failures.Add("$relativePath`: undocumented public signature for $typeName`: '$signatureLine' (normalized: '$key')")
                }
            } catch {
                $failures.Add("$relativePath`: cannot parse '$signatureLine': $($_.Exception.Message)")
            }
        }
    }
}

if ($checkedBlocks -eq 0) {
    throw "No <!-- API SIGNATURES: ... --> blocks were found in handwritten documentation."
}
if ($failures.Count -gt 0) {
    throw "Documentation signature check failed:`n$($failures -join [Environment]::NewLine)"
}

Write-Host "Documentation signatures OK: $checkedSignatures signatures in $checkedBlocks checked blocks."
