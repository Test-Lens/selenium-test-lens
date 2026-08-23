$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$docsRoot = Join-Path $root "docs"
$files = @()
$files += Join-Path $root "README.md"
$files += Get-ChildItem -Path $docsRoot -Filter "*.md" -File -Recurse | ForEach-Object { $_.FullName }

$linkPattern = "\[[^\]]+\]\(([^)]+)\)"
$failures = New-Object System.Collections.Generic.List[string]

foreach ($file in $files) {
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
