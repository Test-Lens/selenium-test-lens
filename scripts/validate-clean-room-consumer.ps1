param(
    [string]$ReleaseVersion,
    [string]$TestLensRepository
)

$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
$mavenCommandName = if ($env:OS -eq "Windows_NT") { "mvn.cmd" } else { "mvn" }
$mavenCommand = (Get-Command $mavenCommandName -ErrorAction Stop).Source
Import-Module (Join-Path $PSScriptRoot "CleanRoomRelease.psm1") -Force
$SourceVersion = Get-TestLensSourceVersion -RepositoryRoot $repo
if (-not $SourceVersion.EndsWith("-SNAPSHOT", [StringComparison]::Ordinal)) {
    throw "Source version must end with -SNAPSHOT, found '$SourceVersion'."
}
if ([string]::IsNullOrWhiteSpace($ReleaseVersion)) {
    $ReleaseVersion = $SourceVersion.Substring(0, $SourceVersion.Length - "-SNAPSHOT".Length)
}
if ($ReleaseVersion.EndsWith("-SNAPSHOT", [StringComparison]::Ordinal)) {
    throw "Release version must not be a snapshot: '$ReleaseVersion'."
}
if ([string]::IsNullOrWhiteSpace($TestLensRepository)) {
    $prepared = New-TestLensCleanRoomRelease -RepositoryRoot $repo -ReleaseVersion $ReleaseVersion
    $work = $prepared.WorkDirectory
    $staging = $prepared.StagingDirectory
    $emptyM2 = $prepared.EmptyMavenRepository
} else {
    $staging = [IO.Path]::GetFullPath($TestLensRepository)
    if (-not (Test-Path -LiteralPath $staging -PathType Container)) {
        throw "Prepared Test Lens repository does not exist: $staging"
    }
    $work = Join-Path ([IO.Path]::GetTempPath()) ("selenium-test-lens-maven-consumer-" + [guid]::NewGuid())
    $emptyM2 = Join-Path $work "empty-m2"
    New-Item -ItemType Directory -Force -Path $emptyM2 | Out-Null
}
$consumer = Join-Path $work "consumer"
New-Item -ItemType Directory -Force -Path (Join-Path $consumer "src/test/java/cleanroom") | Out-Null

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
