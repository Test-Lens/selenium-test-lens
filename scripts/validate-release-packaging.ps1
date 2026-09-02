param(
    [string]$Version = ([xml](Get-Content -LiteralPath (Join-Path $PSScriptRoot "../pom.xml") -Raw)).project.version,
    [string]$StagingDirectory = (Join-Path ([System.IO.Path]::GetTempPath()) ("selenium-test-lens-release-staging-" + [guid]::NewGuid()))
)

$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
$pom = [xml](Get-Content -Raw (Join-Path $repo "pom.xml"))
$ns = New-Object System.Xml.XmlNamespaceManager($pom.NameTable)
$ns.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
$excluded = $pom.SelectSingleNode("//m:plugin[m:artifactId='central-publishing-maven-plugin']/m:configuration/m:excludeArtifacts", $ns)
if ($null -eq $excluded -or $excluded.InnerText.Trim() -ne "selenium-test-lens-examples,selenium-test-lens-browser-tests") {
    throw "Central configuration must exclude examples and browser integration tests"
}

$components = @(
    @{ Artifact = "selenium-test-lens-parent"; Directory = $repo; Packaging = "pom" },
    @{ Artifact = "selenium-test-lens-core"; Directory = (Join-Path $repo "selenium-test-lens-core"); Packaging = "jar" },
    @{ Artifact = "selenium-test-lens-overlay"; Directory = (Join-Path $repo "selenium-test-lens-overlay"); Packaging = "jar" },
    @{ Artifact = "selenium-test-lens"; Directory = (Join-Path $repo "selenium-test-lens-selenium"); Packaging = "jar" },
    @{ Artifact = "selenium-test-lens-junit5"; Directory = (Join-Path $repo "selenium-test-lens-junit5"); Packaging = "jar" },
    @{ Artifact = "selenium-test-lens-react"; Directory = (Join-Path $repo "selenium-test-lens-react"); Packaging = "jar" }
)

New-Item -ItemType Directory -Force -Path $StagingDirectory | Out-Null
foreach ($component in $components) {
    $artifact = $component.Artifact
    $destination = Join-Path $StagingDirectory "io/github/test-lens/$artifact/$Version"
    New-Item -ItemType Directory -Force -Path $destination | Out-Null
    Copy-Item -LiteralPath (Join-Path $component.Directory "pom.xml") -Destination (Join-Path $destination "$artifact-$Version.pom") -Force

    if ($component.Packaging -eq "jar") {
        foreach ($suffix in ".jar", "-sources.jar", "-javadoc.jar") {
            $source = Join-Path $component.Directory "target/$artifact-$Version$suffix"
            if (-not (Test-Path -LiteralPath $source)) { throw "Missing release artifact: $source" }
            Copy-Item -LiteralPath $source -Destination $destination -Force
        }
        $licenseCount = @(jar tf (Join-Path $component.Directory "target/$artifact-$Version.jar") | Where-Object { $_ -eq "META-INF/LICENSE" }).Count
        if ($licenseCount -ne 1) { throw "$artifact must contain exactly one META-INF/LICENSE; found $licenseCount" }
    }
}

$unexpected = Get-ChildItem -LiteralPath $StagingDirectory -Recurse -File | Where-Object { $_.Name -like "*selenium-test-lens-examples*" }
if ($unexpected) { throw "Examples artifact found in release staging" }
$unexpectedBrowserTests = Get-ChildItem -LiteralPath $StagingDirectory -Recurse -File | Where-Object { $_.Name -like "*selenium-test-lens-browser-tests*" }
if ($unexpectedBrowserTests) { throw "Browser integration test artifact found in release staging" }

Write-Output "Release staging validation PASS: $StagingDirectory"
Get-ChildItem -LiteralPath $StagingDirectory -Recurse -File | ForEach-Object { $_.FullName.Substring($StagingDirectory.Length + 1) }
