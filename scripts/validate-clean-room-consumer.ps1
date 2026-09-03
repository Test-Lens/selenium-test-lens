param(
    [string]$ReleaseVersion
)

$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
$mavenCommandName = if ($env:OS -eq "Windows_NT") { "mvn.cmd" } else { "mvn" }
$mavenCommand = (Get-Command $mavenCommandName -ErrorAction Stop).Source
$rootPomPath = Join-Path $repo "pom.xml"
if (-not (Test-Path -LiteralPath $rootPomPath -PathType Leaf)) {
    throw "Cannot read source version: root pom.xml does not exist at $rootPomPath"
}
try {
    [xml]$rootPom = [IO.File]::ReadAllText($rootPomPath)
    $rootNamespace = [System.Xml.XmlNamespaceManager]::new($rootPom.NameTable)
    $rootNamespace.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
    $sourceVersionNode = $rootPom.SelectSingleNode("/m:project/m:version", $rootNamespace)
    $SourceVersion = if ($null -eq $sourceVersionNode) { "" } else { $sourceVersionNode.InnerText.Trim() }
} catch {
    throw "Cannot read source version from root pom.xml: $($_.Exception.Message)"
}
if ([string]::IsNullOrWhiteSpace($SourceVersion)) {
    throw "Cannot read source version from root pom.xml"
}
if (-not $SourceVersion.EndsWith("-SNAPSHOT", [StringComparison]::Ordinal)) {
    throw "Clean-room source version must be a -SNAPSHOT version, found '$SourceVersion'"
}
if ([string]::IsNullOrWhiteSpace($ReleaseVersion)) {
    $ReleaseVersion = $SourceVersion.Substring(0, $SourceVersion.Length - "-SNAPSHOT".Length)
}
if ([string]::IsNullOrWhiteSpace($ReleaseVersion)) {
    throw "Release version must not be blank"
}
$work = Join-Path ([System.IO.Path]::GetTempPath()) ("selenium-test-lens-clean-room-" + [guid]::NewGuid())
$source = Join-Path $work "release-source"
$staging = Join-Path $work "staging"
$emptyM2 = Join-Path $work "empty-m2"
$consumer = Join-Path $work "consumer"

function Copy-ReleaseTree([string]$SourceDirectory, [string]$DestinationDirectory) {
    New-Item -ItemType Directory -Force -Path $DestinationDirectory | Out-Null
    foreach ($file in Get-ChildItem -LiteralPath $SourceDirectory -File -Force) {
        [IO.File]::Copy($file.FullName, (Join-Path $DestinationDirectory $file.Name), $true)
    }
    foreach ($directory in Get-ChildItem -LiteralPath $SourceDirectory -Directory -Force) {
        if ($directory.Name -eq "target" -or ($directory.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            continue
        }
        Copy-ReleaseTree $directory.FullName (Join-Path $DestinationDirectory $directory.Name)
    }
}

New-Item -ItemType Directory -Force -Path $source, $staging, $emptyM2, (Join-Path $consumer "src/test/java/cleanroom") | Out-Null
if (Get-ChildItem -LiteralPath $emptyM2 -Force) { throw "Clean-room Maven repository is not empty" }
Get-ChildItem -LiteralPath $repo -Force |
    Where-Object { $_.Name -notin @(".git", ".agents", ".codex", "target") } |
    Where-Object {
        $trackedEntries = @(& git -C $repo ls-files -- $_.Name)
        if ($LASTEXITCODE -ne 0) {
            throw "Cannot determine whether '$($_.Name)' belongs to the release source"
        }
        $trackedEntries.Count -gt 0
    } |
    ForEach-Object {
        if ($_.PSIsContainer) {
            Copy-ReleaseTree $_.FullName (Join-Path $source $_.Name)
        } else {
            [IO.File]::Copy($_.FullName, (Join-Path $source $_.Name), $true)
        }
    }

$reactorPoms = @(Get-ChildItem -LiteralPath $source -Recurse -Filter pom.xml |
    Where-Object { $_.FullName -notmatch '[\\/]target[\\/]' })
if ($reactorPoms.Count -eq 0) {
    throw "No reactor POMs found in temporary release source"
}
$transformedPoms = 0
$reactorPoms | ForEach-Object {
    $content = [IO.File]::ReadAllText($_.FullName)
    if (-not $content.Contains($SourceVersion)) {
        throw "Reactor POM does not reference source version '$SourceVersion': $($_.FullName)"
    }
    $updated = $content.Replace($SourceVersion, $ReleaseVersion)
    if ($updated -eq $content) {
        throw "Release-version transformation made no change in $($_.FullName)"
    }
    [IO.File]::WriteAllText($_.FullName, $updated)
    $transformedPoms++
}
if ($transformedPoms -ne $reactorPoms.Count) {
    throw "Release-version transformation covered $transformedPoms of $($reactorPoms.Count) reactor POMs"
}
$staleSnapshotPoms = @($reactorPoms | Where-Object {
    [IO.File]::ReadAllText($_.FullName).Contains($SourceVersion)
})
if ($staleSnapshotPoms.Count -gt 0) {
    throw "Source snapshot '$SourceVersion' remains in transformed POMs: $($staleSnapshotPoms.FullName -join ', ')"
}
Write-Output "Clean-room version transform: $SourceVersion -> $ReleaseVersion ($transformedPoms reactor POMs)"

& $mavenCommand -f (Join-Path $source "pom.xml") clean package -Prelease-artifacts -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Temporary release build failed" }
& (Join-Path $source "scripts/validate-release-packaging.ps1") -Version $ReleaseVersion -StagingDirectory $staging
if ($LASTEXITCODE -ne 0) { throw "Release staging validation failed" }

$absoluteStagingPath = [System.IO.Path]::GetFullPath($staging)
if (-not (Test-Path -LiteralPath $absoluteStagingPath -PathType Container)) {
    throw "Release staging directory does not exist: $absoluteStagingPath"
}
$stagingUriBuilder = [System.UriBuilder]::new()
$stagingUriBuilder.Scheme = [System.Uri]::UriSchemeFile
$stagingUriBuilder.Host = ""
$stagingUriBuilder.Path = $absoluteStagingPath.TrimEnd(
    [System.IO.Path]::DirectorySeparatorChar,
    [System.IO.Path]::AltDirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
$stagingRepositoryUri = $stagingUriBuilder.Uri
$stagingUri = $stagingRepositoryUri.AbsoluteUri
if ([string]::IsNullOrWhiteSpace($stagingUri) -or
        -not $stagingRepositoryUri.IsAbsoluteUri -or
        $stagingRepositoryUri.Scheme -ne [System.Uri]::UriSchemeFile) {
    throw "Invalid release staging repository URI for '$absoluteStagingPath': '$stagingUri'"
}
$consumerPom = @"
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>cleanroom</groupId><artifactId>consumer</artifactId><version>1</version>
  <properties><maven.compiler.release>17</maven.compiler.release></properties>
  <repositories><repository><id>lens-staging</id><url>$stagingUri</url></repository></repositories>
  <dependencies>
    <dependency><groupId>io.github.test-lens</groupId><artifactId>selenium-test-lens</artifactId><version>$ReleaseVersion</version></dependency>
    <dependency><groupId>org.seleniumhq.selenium</groupId><artifactId>selenium-java</artifactId><version>4.39.0</version></dependency>
    <dependency><groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter</artifactId><version>5.11.4</version><scope>test</scope></dependency>
  </dependencies>
  <build><plugins>
    <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.13.0</version></plugin>
    <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><version>3.2.5</version></plugin>
  </plugins></build>
</project>
"@
$consumerPomPath = Join-Path $consumer "pom.xml"
[IO.File]::WriteAllText($consumerPomPath, $consumerPom)

$generatedPom = [xml](Get-Content -Raw $consumerPomPath)
$pomNs = [System.Xml.XmlNamespaceManager]::new($generatedPom.NameTable)
$pomNs.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
$repositoryNode = $generatedPom.SelectSingleNode(
    "/m:project/m:repositories/m:repository[m:id='lens-staging']", $pomNs)
if ($null -eq $repositoryNode -or [string]::IsNullOrWhiteSpace($repositoryNode.url)) {
    throw "Generated consumer POM is missing the lens-staging repository URL"
}
[System.Uri]$generatedRepositoryUri = $null
if (-not [System.Uri]::TryCreate($repositoryNode.url.Trim(), [System.UriKind]::Absolute, [ref]$generatedRepositoryUri) -or
        $generatedRepositoryUri.Scheme -ne [System.Uri]::UriSchemeFile) {
    throw "Generated consumer POM has an invalid lens-staging repository URL: '$($repositoryNode.url)'"
}
$expectedPath = $absoluteStagingPath.TrimEnd(
    [System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
$generatedPath = [System.IO.Path]::GetFullPath($generatedRepositoryUri.LocalPath).TrimEnd(
    [System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
if ($generatedPath -ne $expectedPath) {
    throw "Generated lens-staging repository points to '$generatedPath', expected '$expectedPath'"
}

Write-Output "Staging directory: $absoluteStagingPath"
Write-Output "Staging repository URI: $stagingUri"
Write-Output "Generated consumer repository:"
Write-Output "<repository><id>lens-staging</id><url>$($repositoryNode.url)</url></repository>"

$smoke = @'
package cleanroom;

import io.github.testlens.TestLens;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseConsumerTest {
    @Test
    void attachesToExistingDriverAndRunsHudAction() {
        ChromeOptions options = new ChromeOptions().addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(options);
        TestLens lens = TestLens.attach(driver);
        try {
            lens.startSession("clean-room-release");
            driver.get("data:text/html,<button id='go' onclick=\"this.dataset.clicked='yes'\">Go</button>");
            lens.locator(By.id("go"), "Go").click();
            assertTrue("yes".equals(driver.findElement(By.id("go")).getAttribute("data-clicked")));
            Object hud = ((JavascriptExecutor) driver).executeScript(
                    "var host=document.getElementById('selenium-overlay-host');" +
                    "return host && host.shadowRoot && host.shadowRoot.querySelector('#selenium-hud-panel');");
            assertNotNull(hud, "Lens HUD must be present after the native click");
            assertNotNull(lens.finishPassed());
        } finally {
            driver.quit();
        }
    }
}
'@
[IO.File]::WriteAllText((Join-Path $consumer "src/test/java/cleanroom/ReleaseConsumerTest.java"), $smoke)

$repoArg = "-Dmaven.repo.local=$emptyM2"
Push-Location $consumer
try {
    & $mavenCommand $repoArg dependency:tree
    if ($LASTEXITCODE -ne 0) { throw "Clean-room dependency tree failed" }
    & $mavenCommand $repoArg test-compile
    if ($LASTEXITCODE -ne 0) { throw "Clean-room test compilation failed" }
    & $mavenCommand $repoArg test
    if ($LASTEXITCODE -ne 0) { throw "Clean-room browser smoke failed" }
} finally {
    Pop-Location
}

Write-Output "Clean-room release consumer PASS"
Write-Output "Staging repository: $staging"
Write-Output "Empty Maven repository: $emptyM2"
