package io.github.testlens.browser;

import io.github.testlens.selenium.execution.BrowserExecutionConfig;
import io.github.testlens.selenium.execution.HeadlessMode;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeadlessExecutionConfigIT {
    @Test
    void adaptedFactoryAppliesRequestedModeAndRecordsRuntimeGeometry() {
        HeadlessMode requested = HeadlessMode.valueOf(
                System.getProperty("test.execution.mode", "TRUE").trim().toUpperCase(Locale.ROOT));
        BrowserExecutionConfig execution = BrowserExecutionConfig.resolve(requested);
        WebDriver driver = BrowserTestHarness.createDriver(execution);
        try {
            driver.get("data:text/html,<title>execution-config</title><p>ready</p>");
            JavascriptExecutor js = (JavascriptExecutor) driver;
            @SuppressWarnings("unchecked")
            List<Number> geometry = (List<Number>) js.executeScript(
                    "return [outerWidth,outerHeight,innerWidth,innerHeight,devicePixelRatio]");
            String userAgent = String.valueOf(js.executeScript("return navigator.userAgent"));
            Capabilities capabilities = ((HasCapabilities) driver).getCapabilities();
            boolean effectiveHeadless = effectiveHeadless(capabilities, userAgent);
            System.out.printf(Locale.ROOT,
                    "execution-mode requested=%s browser=%s version=%s effectiveHeadless=%s "
                            + "outer=%sx%s viewport=%sx%s dpr=%s%n",
                    requested, capabilities.getBrowserName(), capabilities.getBrowserVersion(), effectiveHeadless,
                    geometry.get(0), geometry.get(1), geometry.get(2), geometry.get(3), geometry.get(4));
            assertEquals(requested == HeadlessMode.TRUE, effectiveHeadless);
            assertEquals("execution-config", driver.getTitle());
            assertTrue(geometry.get(2).intValue() > 0);
            assertTrue(geometry.get(3).intValue() > 0);
            assertTrue(geometry.get(4).doubleValue() > 0.0);
        } finally {
            driver.quit();
        }
    }

    private static boolean effectiveHeadless(Capabilities capabilities, String userAgent) {
        String browser = capabilities.getBrowserName().toLowerCase(Locale.ROOT);
        if (browser.contains("chrome")) return userAgent.contains("HeadlessChrome");
        if (browser.contains("firefox")) {
            Object marker = capabilities.getCapability("moz:headless");
            assertNotNull(marker, "GeckoDriver must expose moz:headless for this runtime contract");
            assertTrue(marker instanceof Boolean, "moz:headless must be boolean");
            return (Boolean) marker;
        }
        assertFalse(browser.isBlank());
        throw new IllegalArgumentException("Unsupported browser " + browser);
    }
}
