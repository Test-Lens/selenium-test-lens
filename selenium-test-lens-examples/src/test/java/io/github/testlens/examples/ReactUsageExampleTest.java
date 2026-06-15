package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.api.ApiCallActions;
import io.github.testlens.api.ApiOverlayPanel;
import io.github.testlens.core.Guards;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.logging.UiTestLensLogger;
import io.github.testlens.react.ReactSafeExecutor;
import io.github.testlens.react.ReactSupport;
import io.github.testlens.selenium.SeleniumOverlayFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

@Disabled("Documentation-only example; requires a real WebDriver.")
class ReactUsageExampleTest {

    @Test
    void reactSafeUsage() {
        WebDriver driver = null; // Replace with a real driver in a test project.

        OverlayConfig config = OverlayConfig.builder().build();
        UiTestLensLogger eventLogger = UiTestLensLogger.builder().build();
        OverlayLogger overlayLogger = OverlayLogger.from(eventLogger);

        OverlayRootManager rootManager = SeleniumOverlayFactory.overlayRoot(driver, config);
        ApiOverlayPanel apiPanel = SeleniumOverlayFactory.apiOverlayPanel(driver, rootManager, config);
        JsOverlayDebug overlay = new JsOverlayDebug(
                driver,
                config,
                apiPanel,
                new ApiCallActions(apiPanel),
                new Guards(driver, overlayLogger),
                overlayLogger
        );

        ReactSafeExecutor react = ReactSupport.reactSafe(overlay);
        react.click(By.cssSelector("[data-testid='save']"), "Save");
        ReactSupport.smartClick(overlay, By.cssSelector("[data-testid='save']"), "Save with overlay handling");
    }
}
