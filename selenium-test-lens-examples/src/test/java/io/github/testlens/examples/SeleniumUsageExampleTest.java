package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.api.ApiCallActions;
import io.github.testlens.api.ApiOverlayPanel;
import io.github.testlens.core.Guards;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.logging.InMemoryLogSink;
import io.github.testlens.core.logging.UiTestLensLogger;
import io.github.testlens.selenium.SeleniumOverlayFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

@Disabled("Documentation-only example; requires a real WebDriver.")
class SeleniumUsageExampleTest {

    @Test
    void seleniumOverlayUsage() {
        WebDriver driver = null; // Replace with a real driver in a test project.

        OverlayConfig config = OverlayConfig.builder().build();
        InMemoryLogSink sink = new InMemoryLogSink();
        UiTestLensLogger eventLogger = UiTestLensLogger.builder()
                .sink(sink)
                .build();
        OverlayLogger overlayLogger = OverlayLogger.from(eventLogger);

        OverlayRootManager rootManager = SeleniumOverlayFactory.overlayRoot(driver, config);
        ApiOverlayPanel apiPanel = SeleniumOverlayFactory.apiOverlayPanel(driver, rootManager, config);
        ApiCallActions apiCalls = new ApiCallActions(apiPanel);
        Guards guards = new Guards(driver, overlayLogger);

        JsOverlayDebug overlay = new JsOverlayDebug(driver, config, apiPanel, apiCalls, guards, overlayLogger);

        overlay.initHud("Checkout test", "local");
        overlay.setStep("Open checkout");
        overlay.hudLog("info", "Opening checkout page", "local");

        WebElement saveButton = driver.findElement(By.cssSelector("[data-testid='save']"));
        overlay.highlightElement(saveButton, "SAVE");
        overlay.clearDebugArtifacts();
    }
}
