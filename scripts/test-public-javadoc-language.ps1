$ErrorActionPreference = "Stop"

$validator = Join-Path $PSScriptRoot "check-public-javadoc-language.ps1"
$workRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("test-lens-javadoc-language-" + [guid]::NewGuid())

function Invoke-Fixture([string]$Name, [string]$MainSource, [string]$TestSource = $null) {
    $fixtureRoot = Join-Path $workRoot $Name
    $mainPath = Join-Path $fixtureRoot "selenium-test-lens-core/src/main/java/example/Fixture.java"
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $mainPath) | Out-Null
    Set-Content -LiteralPath $mainPath -Encoding utf8 -Value $MainSource
    if ($null -ne $TestSource) {
        $testPath = Join-Path $fixtureRoot "selenium-test-lens-core/src/test/java/example/FixtureTest.java"
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $testPath) | Out-Null
        Set-Content -LiteralPath $testPath -Encoding utf8 -Value $TestSource
    }

    $powerShell = (Get-Process -Id $PID).Path
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $output = & $powerShell -NoProfile -File $validator -RepositoryRoot $fixtureRoot 2>&1 | Out-String
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    return [pscustomobject]@{ ExitCode = $exitCode; Output = $output }
}

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

try {
    New-Item -ItemType Directory -Force -Path $workRoot | Out-Null

    $polishS = [char]0x015b
    $polishC = [char]0x0107
    $diacritics = Invoke-Fixture "diacritics" "/** Zwraca warto${polishS}${polishC} elementu. */ public final class Fixture {}"
    Assert-True ($diacritics.ExitCode -ne 0) "Polish text with diacritics was accepted."
    Assert-True ($diacritics.Output -match 'Fixture\.java:1:') "Diagnostic did not contain the file and line."

    $ascii = Invoke-Fixture "ascii" "/** Domyslnie zwraca wartosc. */ public final class Fixture {}"
    Assert-True ($ascii.ExitCode -ne 0) "Polish text without diacritics was accepted."

    $english = Invoke-Fixture "english" "/** Returns the current element value. */ public final class Fixture {}"
    Assert-True ($english.ExitCode -eq 0) "English Javadoc was rejected: $($english.Output)"

    $ordinaryComment = Invoke-Fixture "ordinary-comment" "// Zwraca warto${polishS}${polishC}.`npublic final class Fixture {}"
    Assert-True ($ordinaryComment.ExitCode -eq 0) "A non-Javadoc implementation comment was rejected."

    $testOnly = Invoke-Fixture "test-source" "/** Returns a fixture. */ public final class Fixture {}" "/** Zwraca warto${polishS}${polishC}. */ class FixtureTest {}"
    Assert-True ($testOnly.ExitCode -eq 0) "Javadoc under src/test/java was scanned."

    Write-Host "Public Javadoc language validator tests passed."
} finally {
    if (Test-Path -LiteralPath $workRoot) {
        $resolved = [System.IO.Path]::GetFullPath($workRoot)
        $temp = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
        if (-not $resolved.StartsWith($temp, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove a test directory outside the system temporary directory: $resolved"
        }
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
