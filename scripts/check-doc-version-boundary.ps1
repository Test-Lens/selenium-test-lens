param(
    [string]$ReleaseTag = "v0.1.0"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$pom = [xml][IO.File]::ReadAllText((Join-Path $root "pom.xml"))
$ns = [Xml.XmlNamespaceManager]::new($pom.NameTable)
$ns.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
$version = $pom.SelectSingleNode("/m:project/m:version", $ns).InnerText.Trim()
if (-not $version.EndsWith("-SNAPSHOT", [StringComparison]::Ordinal)) {
    throw "Documentation development source must use a -SNAPSHOT Maven version; found '$version'."
}
$mkdocs = [IO.File]::ReadAllText((Join-Path $root "mkdocs.yml"))
if ($mkdocs -notmatch "(?m)^\s+current:\s+$([regex]::Escape($version))\s*$") {
    throw "mkdocs.yml development display version does not match the root POM version '$version'."
}
if (-not (& git -C $root tag --list $ReleaseTag)) {
    throw "Required historical tag '$ReleaseTag' does not exist."
}

$boundary = Join-Path $root "docs-versioning/0.2.0-boundary.txt"
$stableEntries = [Collections.Generic.List[string]]::new()
foreach ($line in [IO.File]::ReadAllLines($boundary)) {
    $entry = $line.Trim()
    if (-not $entry -or $entry.StartsWith("#")) { continue }
    $kind, $value = $entry -split ' ', 2
    if ($kind -eq "MODULE") {
        if (-not (Test-Path (Join-Path $root $value))) { throw "Boundary module missing on main: $value" }
        $tagModule = @(& git -C $root ls-tree -r --name-only $ReleaseTag | Where-Object { $_ -eq "$value/pom.xml" })
        if ($tagModule.Count -gt 0) { throw "Boundary module unexpectedly exists in $ReleaseTag`: $value" }
        $stableEntries.Add($value)
    } elseif ($kind -eq "TYPE") {
        $sourceType = ($value -split '\$')[0]
        $relative = ($sourceType -replace '\.', '/') + ".java"
        $mainMatches = @(& git -C $root ls-files "*/src/main/java/$relative")
        if ($mainMatches.Count -eq 0) { throw "Boundary type missing on main: $value" }
        $tagMatches = @(& git -C $root ls-tree -r --name-only $ReleaseTag | Select-String -SimpleMatch "/src/main/java/$relative")
        if ($tagMatches.Count -gt 0) { throw "Boundary type unexpectedly exists in $ReleaseTag`: $value" }
        if (-not $value.Contains('$')) {
            $stableEntries.Add(($value -split '\.')[-1])
        }
    } elseif ($kind -eq "SYMBOL") {
        $mainText = & git -C $root grep -F $value -- '*src/main/java/*.java' 2>$null
        if (-not $mainText) { throw "Boundary symbol missing on main: $value" }
        $tagText = & git -C $root grep -F $value $ReleaseTag -- '*src/main/java/*.java' 2>$null
        if ($tagText) { throw "Boundary symbol unexpectedly exists in $ReleaseTag`: $value" }
        $stableEntries.Add($value)
    } else {
        throw "Unknown boundary entry: $entry"
    }
}

$stableText = (Get-ChildItem (Join-Path $root "docs-versions/0.1.0") -File -Recurse | Get-Content) -join "`n"
foreach ($forbidden in $stableEntries) {
    if ($stableText -match [regex]::Escape($forbidden)) { throw "Stable snapshot contains development-only symbol/module: $forbidden" }
}
if ($stableText -match '0\.2\.0-SNAPSHOT</version>|:0\.2\.0-SNAPSHOT') {
    throw "Stable snapshot contains an installable 0.2.0-SNAPSHOT dependency."
}

$matrix = Join-Path $root "docs-versioning/0.1.0-feature-matrix.tsv"
foreach ($line in [IO.File]::ReadAllLines($matrix)) {
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) { continue }
    $columns = $line -split "`t"
    if ($columns.Count -ne 4) { throw "Invalid 0.1.0 feature matrix row: $line" }
    $feature, $symbol, $evidence, $page = $columns
    $tagSymbol = & git -C $root grep -F $symbol $ReleaseTag -- '*src/main/java/*.java' 2>$null
    if (-not $tagSymbol) { throw "Stable feature '$feature' lacks production symbol in $ReleaseTag`: $symbol" }
    $tagEvidence = @(& git -C $root ls-tree -r --name-only $ReleaseTag | Where-Object { $_ -eq $evidence })
    if ($tagEvidence.Count -ne 1) { throw "Stable feature '$feature' lacks tag evidence: $evidence" }
    if (-not (Test-Path -LiteralPath (Join-Path $root "docs-versions/0.1.0/$page") -PathType Leaf)) {
        throw "Stable feature '$feature' lacks documentation page: $page"
    }
}
$workflow = [IO.File]::ReadAllText((Join-Path $root ".github/workflows/docs.yml"))
foreach ($required in @("group: documentation-pages", "cancel-in-progress: false", "contents: read", "contents: write", "pages: write", "id-token: write")) {
    if (-not $workflow.Contains($required)) { throw "Documentation workflow contract missing: $required" }
}
foreach ($forbidden in @("pull_request_target", "continue-on-error", "mike delete", "--force", "mkdocs gh-deploy")) {
    if ($workflow.Contains($forbidden)) { throw "Unsafe documentation workflow construct: $forbidden" }
}
Write-Host "Documentation boundary OK: $version development source and audited $ReleaseTag feature matrix verified."
