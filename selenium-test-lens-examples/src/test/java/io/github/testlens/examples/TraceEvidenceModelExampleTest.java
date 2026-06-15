package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceEvidenceModelExampleTest {

    @Disabled("Documentation-only example; requires a real WebDriver.")
    @Test
    void traceEvidenceModelUsage() {
        WebDriver driver = null; // replace with a real driver

        JsOverlayDebug overlay = new JsOverlayDebug(driver);
        UiTestLensSession session = overlay.startSession("Checkout flow");

        overlay.step("Save form", () -> {
            overlay.getByTestId("save-button").click();
            overlay.expect(overlay.getByTestId("toast")).toContainText("Saved");
        });

        overlay.attachScreenshot("Save form", Path.of("target/screenshots/save-form.png"));
        overlay.attachVideo("Checkout flow video", Path.of("target/videos/checkout-flow.mp4"));

        String json = session.exportJson();
        assertTrue(json.contains("Checkout flow"));
    }
}
