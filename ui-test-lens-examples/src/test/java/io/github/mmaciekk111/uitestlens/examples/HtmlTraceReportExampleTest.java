package io.github.mmaciekk111.uitestlens.examples;

import io.github.mmaciekk111.uitestlens.JsOverlayDebug;
import io.github.mmaciekk111.uitestlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class HtmlTraceReportExampleTest {

    @Disabled("Documentation-only example; requires a real WebDriver.")
    @Test
    void htmlTraceReportUsage() {
        WebDriver driver = null; // replace with a real driver

        JsOverlayDebug overlay = new JsOverlayDebug(driver);
        UiTestLensSession session = overlay.startSession("Checkout flow");

        overlay.step("Save form", () -> {
            overlay.getByTestId("save-button").click();
            overlay.expect(overlay.getByTestId("toast")).toContainText("Saved");
        });

        overlay.attachScreenshot("Save form", Path.of("target/screenshots/save-form.png"));
        overlay.attachVideo("Checkout video", Path.of("target/videos/checkout-flow.mp4"));

        Path report = overlay.exportTraceHtml(Path.of("target/ui-test-lens/checkout-flow.html"));

        assertNotNull(session);
        assertNotNull(report);
    }
}
