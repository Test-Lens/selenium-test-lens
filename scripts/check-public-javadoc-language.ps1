[CmdletBinding()]
param(
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"

$publishedModules = @(
    "selenium-test-lens-core",
    "selenium-test-lens-overlay",
    "selenium-test-lens-selenium",
    "selenium-test-lens-react",
    "selenium-test-lens-junit5",
    "selenium-test-lens-testng"
)

$polishCharacters = @(0x0105, 0x0107, 0x0119, 0x0142, 0x0144, 0x00f3, 0x015b, 0x017a, 0x017c,
    0x0104, 0x0106, 0x0118, 0x0141, 0x0143, 0x00d3, 0x015a, 0x0179, 0x017b) |
    ForEach-Object { [string][char]$_ }
$polishDiacritics = '[' + (($polishCharacters | ForEach-Object { [regex]::Escape($_) }) -join '') + ']'
$polishWords = @(
    'akcje', 'atrybut', 'bledy', 'blad', 'chowa', 'domyslnie', 'elementow',
    'jezeli', 'jesli', 'klikniecie', 'okresla', 'pobiera', 'pokazuje', 'pozwala',
    'proba', 'proby', 'probuje', 'sluzy', 'szuka', 'tekst', 'tworzy', 'ustawia',
    'uzywa', 'wartosc', 'wartosci', 'widocznosc', 'wpisuje', 'wyjatek', 'wykonuje',
    'wykrywa', 'wylicza', 'wywoluje', 'zamyka', 'zwraca'
)
$polishWordPattern = '(?i)(?<![\p{L}\p{N}_])(?:' + (($polishWords | ForEach-Object {
    [regex]::Escape($_)
}) -join '|') + ')(?![\p{L}\p{N}_])'

$violations = [System.Collections.Generic.List[object]]::new()

foreach ($module in $publishedModules) {
    $sourceRoot = Join-Path $RepositoryRoot "$module/src/main/java"
    if (-not (Test-Path -LiteralPath $sourceRoot -PathType Container)) { continue }

    Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Filter '*.java' | ForEach-Object {
        $file = $_
        $lines = @(Get-Content -LiteralPath $file.FullName -Encoding utf8)
        $insideJavadoc = $false
        for ($index = 0; $index -lt $lines.Count; $index++) {
            $line = $lines[$index]
            if ($line -match '/\*\*') { $insideJavadoc = $true }
            if ($insideJavadoc) {
                $match = [regex]::Match($line, $polishDiacritics)
                if (-not $match.Success) { $match = [regex]::Match($line, $polishWordPattern) }
                if ($match.Success) {
                    $rootFull = [System.IO.Path]::GetFullPath($RepositoryRoot).TrimEnd(
                        [System.IO.Path]::DirectorySeparatorChar,
                        [System.IO.Path]::AltDirectorySeparatorChar)
                    $relative = if ($file.FullName.StartsWith($rootFull, [System.StringComparison]::OrdinalIgnoreCase)) {
                        $file.FullName.Substring($rootFull.Length).TrimStart(
                            [System.IO.Path]::DirectorySeparatorChar,
                            [System.IO.Path]::AltDirectorySeparatorChar)
                    } else {
                        $file.FullName
                    }
                    $violations.Add([pscustomobject]@{
                        Path = $relative
                        Line = $index + 1
                        Token = $match.Value
                    })
                }
            }
            if ($insideJavadoc -and $line -match '\*/') { $insideJavadoc = $false }
        }
    }
}

if ($violations.Count -gt 0) {
    foreach ($violation in $violations) {
        Write-Error ("{0}:{1}: Polish text detected in published Javadoc (token: {2})" -f
            $violation.Path, $violation.Line, $violation.Token) -ErrorAction Continue
    }
    exit 1
}

Write-Host "Published Javadoc language check passed for $($publishedModules.Count) modules."
