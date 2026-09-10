param([switch]$KeepWorkDirectoryOnFailure)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$fixture = Join-Path $root "docs-versioning/stable-0.1.0-examples"
$work = Join-Path ([IO.Path]::GetTempPath()) ("test-lens-stable-doc-examples-" + [guid]::NewGuid())
$ok = $false
try {
    Copy-Item -LiteralPath $fixture -Destination $work -Recurse
    Move-Item -LiteralPath (Join-Path $work "pom.xml.fixture") -Destination (Join-Path $work "pom.xml")
    $localRepository = Join-Path $work "m2"
    & mvn -q -f (Join-Path $work "pom.xml") "-Dmaven.repo.local=$localRepository" compile
    if ($LASTEXITCODE -ne 0) {
        throw "Stable documentation examples failed to compile against Maven Central 0.1.0."
    }
    $compiled = Join-Path $work "target/classes/docs/StableDocumentationExamples.class"
    if (-not (Test-Path -LiteralPath $compiled -PathType Leaf)) {
        throw "Stable documentation example compilation produced no class file."
    }
    Write-Host "Stable 0.1.0 documentation examples compile in an isolated Maven repository."
    $ok = $true
} finally {
    if ($ok -or -not $KeepWorkDirectoryOnFailure) {
        Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue
    } else {
        Write-Warning "Stable documentation example work directory retained: $work"
    }
}
