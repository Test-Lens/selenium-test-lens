package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.api.ApiCallActions;
import io.github.testlens.api.ApiOverlayPanel;
import io.github.testlens.core.Guards;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.logging.UiTestLensLogger;
import io.github.testlens.selenium.SeleniumOverlayFactory;
import io.github.testlens.selenium.overlay.OverlayAction;
import io.github.testlens.selenium.overlay.OverlayHandler;
import io.github.testlens.selenium.overlay.OverlayPolicy;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

@Disabled("Documentation-only example; requires a real WebDriver.")
class OverlayPolicyExampleTest {

    @Test
    void configureKnownBlockingOverlays() throws ReflectiveOperationException {
        WebDriver driver = null; // Replace with a real driver in a test project.

        OverlayPolicy policy = OverlayPolicy.builder()
                .handler(OverlayHandler.builder("Cookie consent")
                        .detect(By.cssSelector("[data-testid='cookie-banner']"))
                        .action(OverlayAction.click(By.cssSelector("[data-testid='accept-cookies']")))
                        .optional(true)
                        .timeout(Duration.ofSeconds(2))
                        .build())
                .handler(OverlayHandler.builder("Newsletter modal")
                        .detect(By.cssSelector("[data-testid='newsletter-modal']"))
                        .action(OverlayAction.pressEscape())
                        .action(OverlayAction.waitUntilGone(By.cssSelector("[data-testid='newsletter-modal']")))
                        .optional(true)
                        .failIfStillVisible(false)
                        .build())
                .handler(OverlayHandler.builder("Session expired")
                        .detect(By.cssSelector("[data-testid='session-expired']"))
                        .action(OverlayAction.fail("Session expired popup detected"))
                        .optional(false)
                        .failIfStillVisible(true)
                        .build())
                .build();

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
        // In application code, call:
        // overlay.setOverlayPolicy(policy);
        JsOverlayDebug.class.getMethod("setOverlayPolicy", OverlayPolicy.class)
                .invoke(overlay, policy);
    }
}

