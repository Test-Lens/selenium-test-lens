$ErrorActionPreference = "Stop"

function Get-PathRelativeTo([string]$BasePath, [string]$TargetPath) {
    $baseFullPath = [System.IO.Path]::GetFullPath($BasePath).TrimEnd([System.IO.Path]::DirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
    $targetFullPath = [System.IO.Path]::GetFullPath($TargetPath)
    $baseUri = [Uri]::new($baseFullPath)
    $targetUri = [Uri]::new($targetFullPath)
    return [Uri]::UnescapeDataString($baseUri.MakeRelativeUri($targetUri).ToString()).Replace('\', '/')
}

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$docsRoot = Join-Path $root "docs"
$mkdocsConfig = Join-Path $root "mkdocs.yml"
$excludedDocs = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)

if (Test-Path $mkdocsConfig) {
    $inExcludeDocs = $false
    foreach ($line in Get-Content -Path $mkdocsConfig) {
        if ($line -match '^exclude_docs:\s*\|\s*$') {
            $inExcludeDocs = $true
            continue
        }
        if ($inExcludeDocs -and $line -match '^\S') {
            break
        }
        if ($inExcludeDocs -and $line -match '^\s+([^#].*?)\s*$') {
            [void]$excludedDocs.Add($Matches[1].Trim().Replace('\', '/'))
        }
    }
}

$files = @()
$files += Join-Path $root "README.md"
$files += Get-ChildItem -Path $docsRoot -Filter "*.md" -File -Recurse | ForEach-Object { $_.FullName }
$versionedDocsRoot = Join-Path $root "docs-versions"
if (Test-Path $versionedDocsRoot) {
    $files += Get-ChildItem -Path $versionedDocsRoot -Filter "*.md" -File -Recurse | ForEach-Object { $_.FullName }
}

$linkPattern = "\[[^\]]+\]\(([^)]+)\)"
$failures = New-Object System.Collections.Generic.List[string]

foreach ($file in $files) {
    $sourceRelativeToDocs = $null
    if ($file.StartsWith($docsRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        $sourceRelativeToDocs = Get-PathRelativeTo $docsRoot $file
        if ($excludedDocs.Contains($sourceRelativeToDocs)) {
            continue
        }
    }

    $content = Get-Content -Path $file
    for ($i = 0; $i -lt $content.Count; $i++) {
        $line = $content[$i]
        foreach ($match in [regex]::Matches($line, $linkPattern)) {
            $rawLink = $match.Groups[1].Value.Trim()
            if ($rawLink.Length -eq 0) {
                continue
            }
            if ($rawLink.StartsWith("#") -or $rawLink.StartsWith("http://") -or $rawLink.StartsWith("https://") -or $rawLink.StartsWith("mailto:")) {
                continue
            }
            if ($rawLink.Contains("://")) {
                continue
            }

            $target = $rawLink.Split("#")[0]
            if ($target.Length -eq 0) {
                continue
            }

            $target = [Uri]::UnescapeDataString($target)
            $baseDirectory = Split-Path -Path $file -Parent
            $targetPath = Join-Path -Path $baseDirectory -ChildPath $target

            if ($null -ne $sourceRelativeToDocs) {
                $targetFullPath = [System.IO.Path]::GetFullPath($targetPath)
                if ($targetFullPath.StartsWith($docsRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
                    $targetRelativeToDocs = Get-PathRelativeTo $docsRoot $targetFullPath
                    if ($excludedDocs.Contains($targetRelativeToDocs)) {
                        $relativeFile = Resolve-Path -Path $file -Relative
                        $failures.Add("${relativeFile}:$($i + 1) -> $rawLink (target is excluded from the published site)")
                        continue
                    }
                }
            }

            if (-not (Test-Path -Path $targetPath)) {
                $relativeFile = Resolve-Path -Path $file -Relative
                $failures.Add("${relativeFile}:$($i + 1) -> $rawLink")
            }
        }
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Host "All markdown links OK"
