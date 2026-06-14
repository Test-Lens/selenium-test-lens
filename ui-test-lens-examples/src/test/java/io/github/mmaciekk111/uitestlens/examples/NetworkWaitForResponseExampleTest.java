package io.github.mmaciekk111.uitestlens.examples;

import io.github.mmaciekk111.uitestlens.JsOverlayDebug;
import io.github.mmaciekk111.uitestlens.selenium.network.NetworkCaptureMode;
import io.github.mmaciekk111.uitestlens.selenium.network.NetworkDiagnosticsOptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.nio.file.Path;
import java.time.Duration;

class NetworkWaitForResponseExampleTest {

    @Disabled("Documentation-only example; requires a real browser/network-capable WebDriver.")
    @Test
    void waitForResponseUsage() {
        WebDriver driver = null; // replace with a real driver

        JsOverlayDebug overlay = new JsOverlayDebug(driver);
        overlay.startSession("Checkout flow");

        overlay.network().start(NetworkDiagnosticsOptions.builder()
                .captureMode(NetworkCaptureMode.AUTO)
                .failedStatusThreshold(400)
                .build());

        overlay.step("Save order", () -> {
            overlay.getByTestId("save-order").click();

            overlay.network().expectResponse()
                    .urlContains("/api/orders")
                    .method("POST")
                    .status(201)
                    .within(Duration.ofSeconds(10));
        });

        overlay.network().assertNoFailedRequests();
        overlay.exportTraceHtml(Path.of("target/ui-test-lens/checkout-flow.html"));
    }
}
