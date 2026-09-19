package io.github.testlens.browser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class HudStudioDocumentationLayoutIT {
    private WebDriver driver;

    @AfterEach
    void closeDriver() {
        if (driver != null) driver.quit();
    }

    @Test
    void versionedStudioUsesAvailableWidthWithoutDocumentOverflow() {
        String root = System.getProperty("hudStudioDocsUrl");
        assumeTrue(root != null && !root.isBlank(), "-DhudStudioDocsUrl points to a built versioned docs root");
        driver = BrowserTestHarness.createDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        double desktopMain = 0;

        for (Dimension viewport : List.of(new Dimension(1920, 1000), new Dimension(1366, 900), new Dimension(500, 844))) {
            driver.manage().window().setSize(viewport);
            driver.get(root + "observability/hud-studio/");
            System.out.printf("HUD_STUDIO_PAGE url=%s title=%s body=%s%n", driver.getCurrentUrl(), driver.getTitle(),
                    js.executeScript("return document.body && document.body.className"));
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(ignored -> Boolean.TRUE.equals(js.executeScript("""
                    const frame=document.querySelector('[data-studio-frame]');
                    return document.body.classList.contains('tl-hud-studio-page') && Boolean(frame);
                    """)));
            WebElement frame = driver.findElement(By.cssSelector("[data-studio-frame]"));
            @SuppressWarnings("unchecked")
            Map<String, Number> metrics = (Map<String, Number>) js.executeScript("""
                    const frame=document.querySelector('[data-studio-frame]');
                    const viewport=document.documentElement.clientWidth;
                    const main=document.querySelector('.md-main__inner').getBoundingClientRect().width;
                    const article=document.querySelector('.md-content').getBoundingClientRect().width;
                    const host=document.querySelector('[data-studio-host]').getBoundingClientRect().width;
                    return {viewport,main,article,studio:host,utilization:host/article*100,
                      documentOverflow:document.documentElement.scrollWidth-viewport,
                      frameWidth:frame.getBoundingClientRect().width};
                    """);
            driver.switchTo().frame(frame);
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(ignored -> !driver.findElements(By.cssSelector(".studio")).isEmpty());
            @SuppressWarnings("unchecked")
            Map<String, Number> frameMetrics = (Map<String, Number>) js.executeScript("""
                    const studio=document.querySelector('.studio');
                    return {frameOverflow:document.documentElement.scrollWidth-document.documentElement.clientWidth,
                      innerStudio:studio.getBoundingClientRect().width, frameWidth:document.documentElement.clientWidth};
                    """);
            driver.switchTo().defaultContent();
            System.out.printf("HUD_STUDIO_LAYOUT viewport=%.0f main=%.1f article=%.1f studio=%.1f utilization=%.1f%% documentOverflow=%.1f frameOverflow=%.1f%n",
                    number(metrics, "viewport"), number(metrics, "main"), number(metrics, "article"), number(metrics, "studio"),
                    number(metrics, "utilization"), number(metrics, "documentOverflow"), number(frameMetrics, "frameOverflow"));
            assertTrue(number(metrics, "utilization") > (viewport.getWidth() >= 1366 ? 95 : 92),
                    "Studio must use the available article width");
            assertTrue(Math.abs(number(frameMetrics, "innerStudio") - number(frameMetrics, "frameWidth")) < 2,
                    "Configurator root must fill its iframe");
            assertTrue(number(metrics, "documentOverflow") <= 1, "Documentation must not overflow horizontally");
            assertTrue(number(frameMetrics, "frameOverflow") <= 1, "Studio iframe must not overflow horizontally");
            if (viewport.getWidth() >= 1366) {
                assertTrue(number(metrics, "main") > 1200, "Studio page must not retain the normal article max-width");
            }
            if (viewport.getWidth() == 1920) desktopMain = number(metrics, "main");
        }

        driver.manage().window().setSize(new Dimension(1920, 1000));
        driver.get(root + "observability/visual-diagnostics/");
        assertFalse(Boolean.TRUE.equals(js.executeScript("return document.body.classList.contains('tl-hud-studio-page')")));
        double ordinaryMain = ((Number) js.executeScript("return document.querySelector('.md-main__inner').getBoundingClientRect().width")).doubleValue();
        System.out.printf("HUD_STUDIO_LAYOUT ordinaryPageMain=%.1f%n", ordinaryMain);
        assertTrue(ordinaryMain < desktopMain * 0.8,
                "Ordinary documentation pages must keep the Material max-width");
    }

    private static double number(Map<String, Number> values, String key) {
        return values.get(key).doubleValue();
    }
}
