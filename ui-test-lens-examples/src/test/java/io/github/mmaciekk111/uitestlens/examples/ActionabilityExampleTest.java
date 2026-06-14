package io.github.mmaciekk111.uitestlens.examples;

import io.github.mmaciekk111.uitestlens.JsOverlayDebug;
import io.github.mmaciekk111.uitestlens.selenium.actionability.ActionabilityOptions;
import io.github.mmaciekk111.uitestlens.selenium.actionability.ActionabilityReport;
import io.github.mmaciekk111.uitestlens.OverlayConfig;
import io.github.mmaciekk111.uitestlens.api.ApiCallActions;
import io.github.mmaciekk111.uitestlens.api.ApiOverlayPanel;
import io.github.mmaciekk111.uitestlens.core.Guards;
import io.github.mmaciekk111.uitestlens.core.OverlayLogger;
import io.github.mmaciekk111.uitestlens.core.OverlayRootManager;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogger;
import io.github.mmaciekk111.uitestlens.selenium.SeleniumOverlayFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

@Disabled("Documentation-only example; requires a real WebDriver.")
class ActionabilityExampleTest {

    @Test
    void checkElementActionabilityBeforeClicking() {
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

        ActionabilityOptions options = ActionabilityOptions.builder()
                .timeout(Duration.ofSeconds(5))
                .checkStableBounds(true)
                .checkReceivesClickPoint(true)
                .build();

        ActionabilityReport report = overlay.checkActionability(
                By.cssSelector("[data-testid='save']"),
                options
        );

        if (!report.isReady()) {
            throw new AssertionError(report.summary());
        }
    }
}
