package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.react.ReactSupport;
import io.github.testlens.react.actionability.ReactActionabilityOptions;
import io.github.testlens.react.actionability.ReactActionabilityReport;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

@Disabled("Documentation-only example; requires a real WebDriver.")
class ReactActionabilityExampleTest {

    @Test
    void checkReactReadinessBeforeClicking() {
        WebDriver driver = null; // Replace with a real driver in a test project.

        OverlayConfig config = OverlayConfig.builder().build();
        JsOverlayDebug overlay = new JsOverlayDebug(driver, config);

        ReactActionabilityOptions options = ReactActionabilityOptions.builder()
                .checkAriaBusy(true)
                .checkDataLoading(true)
                .checkSpinner(true)
                .checkSkeleton(true)
                .build();

        ReactActionabilityReport report = ReactSupport.checkActionability(
                overlay,
                By.cssSelector("[data-testid='save']"),
                options
        );

        if (!report.isReady()) {
            throw new AssertionError(report.summary());
        }
    }
}

