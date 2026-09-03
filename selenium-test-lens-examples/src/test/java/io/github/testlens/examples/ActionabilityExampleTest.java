package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.selenium.actionability.ActionabilityOptions;
import io.github.testlens.selenium.actionability.ActionabilityReport;
import io.github.testlens.OverlayConfig;
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
        JsOverlayDebug overlay = new JsOverlayDebug(driver, config);

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

