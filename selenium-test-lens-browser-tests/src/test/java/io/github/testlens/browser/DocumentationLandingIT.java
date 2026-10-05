package io.github.testlens.browser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DocumentationLandingIT {
    private WebDriver driver;

    @AfterEach
    void closeDriver() {
        if (driver != null) driver.quit();
    }

    @Test
    void productOverviewExplainsTheReleaseAndItsPrimaryRoutesOnDesktopAndMobile() {
        String root = System.getProperty("documentationLandingUrl");
        assumeTrue(root != null && !root.isBlank(),
                "-DdocumentationLandingUrl points to a built or deployed documentation version root");
        if (!root.endsWith("/")) root += "/";

        driver = BrowserTestHarness.createDriver();
        driver.manage().window().setSize(new Dimension(1440, 1000));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        driver.get(root);
        waitForOverview(wait);
        String resolvedRoot = driver.getCurrentUrl();
        assertTrue(driver.getTitle().startsWith("Selenium Test Lens — product overview"), driver.getTitle());
        assertTrue(driver.findElement(By.cssSelector("meta[name='description']"))
                .getAttribute("content").contains("observable Selenium interactions"));

        Map<String, String> requiredHeadings = Map.of(
                "what-test-lens-adds-to-selenium", "What Test Lens adds to Selenium",
                "why-teams-add-test-lens", "Why teams add Test Lens",
                "product-capabilities", "Product capabilities",
                "see-the-result", "See the result",
                "quick-start", "Quick start",
                "requirements-and-optional-capabilities", "Requirements and optional capabilities",
                "whats-new-in-040", "What's new in 0.4.0");
        for (Map.Entry<String, String> heading : requiredHeadings.entrySet()) {
            WebElement value = driver.findElement(By.id(heading.getKey()));
            assertTrue(value.getText().startsWith(heading.getValue()), "Missing landing heading: " + heading.getValue());
        }
        assertTrue(driver.findElements(By.xpath("//h2[contains(.,'at a glance') or contains(.,'foundations')]")).isEmpty(),
                "Release chronology must not be the landing information architecture");

        WebElement heroImage = driver.findElement(By.cssSelector(".lens-home-logo"));
        assertEquals("Selenium Test Lens", heroImage.getAttribute("alt"));
        wait.until(ignored -> ((Number) js.executeScript("return arguments[0].naturalWidth", heroImage)).intValue() > 0);

        followAndReturn(wait, resolvedRoot, "Get started", "getting-started/", "Getting started");
        followAndReturn(wait, resolvedRoot, "Integrate with AI", "ai-assisted-integration/", "AI-assisted integration");

        driver.findElement(By.linkText("Explore capabilities")).click();
        wait.until(ignored -> driver.getCurrentUrl().endsWith("#product-capabilities"));
        WebElement capabilities = driver.findElement(By.id("product-capabilities"));
        assertTrue(capabilities.isDisplayed());

        @SuppressWarnings("unchecked")
        Map<String, String> themeColors = (Map<String, String>) js.executeScript("""
                const before=getComputedStyle(document.body);
                const light={background:before.getPropertyValue('--md-default-bg-color').trim(),color:before.color};
                document.body.setAttribute('data-md-color-scheme','slate');
                const after=getComputedStyle(document.body);
                return {lightBackground:light.background,lightColor:light.color,
                  darkBackground:after.getPropertyValue('--md-default-bg-color').trim(),darkColor:after.color};
                """);
        assertFalse(themeColors.get("lightBackground").equals(themeColors.get("darkBackground")), themeColors.toString());
        assertFalse(themeColors.get("darkBackground").equals(themeColors.get("darkColor")), themeColors.toString());
        js.executeScript("scrollTo(0,0)");
        captureIfConfigured("documentationLandingDesktopScreenshot");

        driver.manage().window().setSize(new Dimension(500, 844));
        wait.until(ignored -> ((Number) js.executeScript("return document.documentElement.clientWidth")).intValue() <= 500);
        @SuppressWarnings("unchecked")
        Map<String, Number> mobile = (Map<String, Number>) js.executeScript("""
                const hero=document.querySelector('.lens-hero').getBoundingClientRect();
                const button=document.querySelector('.lens-hero .md-button').getBoundingClientRect();
                return {viewport:document.documentElement.clientWidth,
                  overflow:document.documentElement.scrollWidth-document.documentElement.clientWidth,
                  heroLeft:hero.left,heroRight:hero.right,buttonWidth:button.width,heroWidth:hero.width};
                """);
        assertTrue(mobile.get("overflow").doubleValue() <= 1, mobile.toString());
        assertTrue(mobile.get("heroLeft").doubleValue() >= 0, mobile.toString());
        assertTrue(mobile.get("heroRight").doubleValue() <= mobile.get("viewport").doubleValue() + 1, mobile.toString());
        assertTrue(mobile.get("buttonWidth").doubleValue() >= mobile.get("heroWidth").doubleValue() * 0.8, mobile.toString());
        js.executeScript("scrollTo(0,0)");
        captureIfConfigured("documentationLandingMobileScreenshot");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> failedResources = (List<Map<String, Object>>) js.executeAsyncScript("""
                const done=arguments[arguments.length-1];
                const urls=[...new Set(Array.from(document.querySelectorAll('img[src],script[src],link[rel="stylesheet"][href]'))
                  .map(node=>node.src||node.href).filter(url=>url&&url.startsWith(location.origin)))];
                Promise.all(urls.map(async url=>{try{const response=await fetch(url,{cache:'no-store'});
                  return {url,status:response.status,ok:response.ok};}catch(error){return {url,status:0,ok:false};}}))
                  .then(results=>done(results.filter(result=>!result.ok)));
                """);
        assertTrue(failedResources.isEmpty(), "Landing resources failed: " + failedResources);

        boolean localBuildWithoutMikeMetadata = root.contains("127.0.0.1") || root.contains("localhost");
        List<LogEntry> severe = driver.manage().logs().get(LogType.BROWSER).getAll().stream()
                .filter(entry -> entry.getLevel().intValue() >= Level.SEVERE.intValue())
                .filter(entry -> !(localBuildWithoutMikeMetadata && entry.getMessage().contains("/versions.json")))
                .toList();
        assertTrue(severe.isEmpty(), "Landing console errors: " + severe);

        System.out.printf("DOCUMENTATION_LANDING url=%s headings=%d mobile=%s theme=%s failedResources=%s consoleSevere=%s%n",
                driver.getCurrentUrl(), requiredHeadings.size(), mobile, themeColors, failedResources, severe);
    }

    private void followAndReturn(WebDriverWait wait, String root, String linkText, String suffix, String targetHeading) {
        driver.findElement(By.linkText(linkText)).click();
        wait.until(ignored -> driver.getCurrentUrl().contains(suffix));
        wait.until(ignored -> driver.findElement(By.cssSelector(".md-content h1")).getText().startsWith(targetHeading));
        driver.navigate().back();
        wait.until(ignored -> driver.getCurrentUrl().equals(root));
        waitForOverview(wait);
    }

    private void waitForOverview(WebDriverWait wait) {
        wait.until(ignored -> !driver.findElements(By.cssSelector(".lens-hero h1")).isEmpty());
    }

    private void captureIfConfigured(String property) {
        String configured = System.getProperty(property, "").trim();
        if (configured.isEmpty()) return;
        try {
            Path target = Path.of(configured).toAbsolutePath().normalize();
            if (target.getParent() != null) Files.createDirectories(target.getParent());
            Files.copy(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath(), target,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException failure) {
            throw new AssertionError("Unable to save documentation landing screenshot", failure);
        }
    }

}
