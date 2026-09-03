param(
    [string]$ExpectedVersion
)

$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$rootPomPath = Join-Path $repositoryRoot "pom.xml"

function Read-Pom([string]$Path) {
    try {
        return [xml][IO.File]::ReadAllText($Path)
    } catch {
        throw "Cannot read Maven project '$Path': $($_.Exception.Message)"
    }
}

function Select-PomText([xml]$Pom, [string]$XPath) {
    $namespace = [System.Xml.XmlNamespaceManager]::new($Pom.NameTable)
    $namespace.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
    $node = $Pom.SelectSingleNode($XPath, $namespace)
    if ($null -eq $node) { return "" }
    return $node.InnerText.Trim()
}

$rootPom = Read-Pom $rootPomPath
$rootVersion = Select-PomText $rootPom "/m:project/m:version"
if ([string]::IsNullOrWhiteSpace($rootVersion)) {
    throw "Root pom.xml does not declare a project version"
}
if ([string]::IsNullOrWhiteSpace($ExpectedVersion)) {
    $ExpectedVersion = $rootVersion
}
if ($rootVersion -ne $ExpectedVersion) {
    throw "Root project version is '$rootVersion', expected '$ExpectedVersion'"
}

$pomFiles = @(Get-ChildItem -LiteralPath $repositoryRoot -Recurse -Filter pom.xml |
    Where-Object { $_.FullName -notmatch '[\\/]target[\\/]' } |
    Sort-Object FullName)
if ($pomFiles.Count -eq 0) {
    throw "No Maven projects found"
}

foreach ($pomFile in $pomFiles) {
    $pom = Read-Pom $pomFile.FullName
    $declaredVersion = Select-PomText $pom "/m:project/m:version"
    $parentVersion = Select-PomText $pom "/m:project/m:parent/m:version"
    $effectiveProjectVersion = if ([string]::IsNullOrWhiteSpace($declaredVersion)) {
        $parentVersion
    } else {
        $declaredVersion
    }
    $relativePath = $pomFile.FullName.Substring($repositoryRoot.Length).TrimStart('\', '/')

    if ($effectiveProjectVersion -ne $ExpectedVersion) {
        throw "Maven project '$relativePath' reports '$effectiveProjectVersion', expected '$ExpectedVersion'"
    }
    if ($pomFile.FullName -ne $rootPomPath -and $parentVersion -ne $ExpectedVersion) {
        throw "Maven module '$relativePath' uses parent version '$parentVersion', expected '$ExpectedVersion'"
    }
    Write-Output "$relativePath`t$effectiveProjectVersion"
}

Write-Output "Reactor version OK: $($pomFiles.Count) projects use $ExpectedVersion"
