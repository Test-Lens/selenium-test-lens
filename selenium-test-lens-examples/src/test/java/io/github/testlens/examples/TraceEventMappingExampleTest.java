package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.nio.file.Path;

@Disabled("Documentation-only example; requires a real WebDriver.")
class TraceEventMappingExampleTest {

    @Test
    void traceEventMappingUsage() {
        WebDriver driver = null; // replace with a real driver

        JsOverlayDebug overlay = new JsOverlayDebug(driver);
        overlay.startSession("Checkout flow");

        // Locator, action, assertion, step, and network events are forwarded
        // into the trace session while the session is attached.
        overlay.step("Save order", () -> {
            overlay.getByTestId("save-order").click();
            overlay.expect(overlay.getByTestId("toast")).toContainText("Saved");
        });

        overlay.network().assertNoFailedRequests();

        overlay.exportTraceHtml(Path.of("target/ui-test-lens/checkout-flow.html"));
    }
}

