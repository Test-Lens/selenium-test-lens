param(
    [string]$Version,
    [string]$StagingDirectory = (Join-Path ([System.IO.Path]::GetTempPath()) ("selenium-test-lens-release-staging-" + [guid]::NewGuid())),
    [switch]$MatrixOnly,
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$ReleaseSourceRoot
)

$ErrorActionPreference = "Stop"
Import-Module (Join-Path $PSScriptRoot "ReleaseReactor.psm1") -Force
$repo = [IO.Path]::GetFullPath($RepositoryRoot)
$releaseSource = if ([string]::IsNullOrWhiteSpace($ReleaseSourceRoot)) {
    $repo
} else {
    [IO.Path]::GetFullPath($ReleaseSourceRoot)
}
$model = Get-TestLensReactorModel `
    -RepositoryRoot $repo `
    -ReleaseSourceRoot $releaseSource `
    -IncludeBrowserIt
if ([string]::IsNullOrWhiteSpace($Version)) { $Version = $model.RootVersion }
$pom = [xml](Get-Content -Raw (Join-Path $releaseSource "pom.xml"))
$ns = New-Object System.Xml.XmlNamespaceManager($pom.NameTable)
$ns.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
$excluded = $pom.SelectSingleNode("//m:plugin[m:artifactId='central-publishing-maven-plugin']/m:configuration/m:excludeArtifacts", $ns)
$published = @("selenium-test-lens-parent","selenium-test-lens-core","selenium-test-lens-overlay","selenium-test-lens","selenium-test-lens-junit5","selenium-test-lens-testng","selenium-test-lens-allure","selenium-test-lens-react")
$internal = @("selenium-test-lens-examples","selenium-test-lens-browser-tests","selenium-test-lens-selector-engine","selenium-test-lens-selector-live","selenium-test-lens-selector-lab","selenium-test-lens-selector-tooling","selenium-test-lens-compatibility-engine","selenium-test-lens-compatibility-tooling","selenium-test-lens-migration-tooling")
$classified = @($published + $internal | Sort-Object -Unique)
$actual = @($model.Projects.ArtifactId | Sort-Object -Unique)
$unknown = @($actual | Where-Object { $_ -notin $classified })
$missing = @($classified | Where-Object { $_ -notin $actual })
if ($unknown.Count -gt 0 -or $missing.Count -gt 0) { throw "Publication policy does not match reactor. Unknown: $($unknown -join ', '); missing: $($missing -join ', ')" }
$centralExcluded = if ($null -eq $excluded) { @() } else { @($excluded.InnerText.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ } | Sort-Object -Unique) }
if (@(Compare-Object $internal $centralExcluded).Count -ne 0) { throw "Central excludeArtifacts does not exactly match the nonpublished module policy" }

$rows = foreach ($project in $model.Projects) {
    $publicationExpected = $project.ArtifactId -in $published
    $ok = if ($publicationExpected) { -not $project.EffectiveDeploySkip } else { $project.EffectiveDeploySkip -and $project.ArtifactId -in $centralExcluded }
    [pscustomobject]@{ ArtifactId=$project.ArtifactId; Reactor=if($project.ProfileOnly){"browser-it"}else{"normal"}; Packaging=$project.Packaging; PublicationExpected=$publicationExpected; EffectiveDeploySkip=$project.EffectiveDeploySkip; Result=if($ok){"PASS"}else{"FAIL"}; Directory=$project.Directory }
}
$rows | Sort-Object ArtifactId | Format-Table ArtifactId,Reactor,Packaging,PublicationExpected,EffectiveDeploySkip,Result -AutoSize | Out-String | Write-Output
$failed = @($rows | Where-Object { $_.Result -ne "PASS" })
if ($failed.Count -gt 0) { throw "Release publication matrix contains $($failed.Count) invalid module(s)" }
if ($MatrixOnly) { Write-Output "Release publication matrix PASS: 8 published coordinates, 9 nonpublished modules"; return }

$components = @($rows | Where-Object { $_.PublicationExpected } | ForEach-Object { @{ Artifact=$_.ArtifactId; Directory=$_.Directory; Packaging=$_.Packaging } })

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
        if ($artifact -eq "selenium-test-lens-overlay") {
            $requiredTypographyAssets = @(
                "uitestlens/runtime/visual-typography.js",
                "uitestlens/runtime/fonts/Sora-wght.woff2",
                "META-INF/licenses/OFL-Sora.txt"
            )
            $binaryEntries = @(jar tf (Join-Path $component.Directory "target/$artifact-$Version.jar"))
            $sourceEntries = @(jar tf (Join-Path $component.Directory "target/$artifact-$Version-sources.jar"))
            foreach ($entry in $requiredTypographyAssets) {
                if ($entry -notin $binaryEntries) { throw "$artifact binary JAR is missing $entry" }
                if ($entry -notin $sourceEntries) { throw "$artifact source JAR is missing $entry" }
            }
        }
    }
}

$unexpected = @(Get-ChildItem -LiteralPath $StagingDirectory -Recurse -File | Where-Object { $name=$_.Name; @($internal | Where-Object { $name -like "*$_*" }).Count -gt 0 })
if ($unexpected.Count -gt 0) { throw "Nonpublished artifact found in release staging: $($unexpected.Name -join ', ')" }

Write-Output "Release staging validation PASS: $StagingDirectory"
Get-ChildItem -LiteralPath $StagingDirectory -Recurse -File | ForEach-Object { $_.FullName.Substring($StagingDirectory.Length + 1) }
