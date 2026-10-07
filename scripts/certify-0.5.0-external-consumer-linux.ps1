[CmdletBinding()]
param(
    [string]$Version = "0.5.0-SNAPSHOT",
    [string]$MavenRepository,
    [switch]$SkipStage,
    [switch]$Firefox
)

$ErrorActionPreference = "Stop"
if ($IsWindows -or $env:OS -eq "Windows_NT") {
    throw "The Linux external-consumer adapter must run on a non-Windows host."
}

$canonical = Join-Path $PSScriptRoot "certify-0.5.0-external-consumer.ps1"
$adapted = Join-Path $PSScriptRoot ".certify-0.5.0-external-consumer-linux.generated.ps1"
$source = [System.IO.File]::ReadAllText($canonical)

$requiredWindowsTokens = @(
    '& mvn.cmd',
    '-FilePath "mvn.cmd"',
    '-FilePath "java.exe"',
    '-WindowStyle Hidden'
)
foreach ($token in $requiredWindowsTokens) {
    if (-not $source.Contains($token)) {
        throw "Canonical certification launcher changed; Linux adapter token is missing: $token"
    }
}

# Keep one certification implementation. Only executable names and the Windows-only
# Start-Process presentation option differ on a GitHub-hosted Ubuntu runner.
$source = $source.Replace('& mvn.cmd', '& mvn')
$source = $source.Replace('-FilePath "mvn.cmd"', '-FilePath "mvn"')
$source = $source.Replace('-FilePath "java.exe"', '-FilePath "java"')
$source = $source.Replace(' -WindowStyle Hidden', '')

try {
    [System.IO.File]::WriteAllText($adapted, $source, [System.Text.UTF8Encoding]::new($false))
    & $adapted @PSBoundParameters
} finally {
    Remove-Item -LiteralPath $adapted -Force -ErrorAction SilentlyContinue
}
