package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.selenium.steps.UiStepOptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.nio.file.Path;

class ScreenshotEvidenceExampleTest {

    @Disabled("Documentation-only example; requires a real WebDriver.")
    @Test
    void screenshotEvidenceUsage() {
        WebDriver driver = null; // replace with a real driver

        JsOverlayDebug overlay = new JsOverlayDebug(driver);
        overlay.startSession("Checkout flow");

        overlay.step("Save form", () -> {
            overlay.getByTestId("save-button").click();
        });

        overlay.captureScreenshot("After save");

        overlay.exportTraceHtml(Path.of("target/ui-test-lens/checkout-flow.html"));
    }

    @Disabled("Documentation-only example; requires a real WebDriver.")
    @Test
    void screenshotOnFailedStepUsage() {
        WebDriver driver = null; // replace with a real driver

        JsOverlayDebug overlay = new JsOverlayDebug(driver);
        overlay.startSession("Checkout flow");

        UiStepOptions options = UiStepOptions.builder()
                .captureScreenshotOnFailure(true)
                .build();

        overlay.step("Verify order summary", options, () -> {
            overlay.getByTestId("order-total").expect().toHaveText("123.00 PLN");
        });
    }
}
