package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.selenium.network.NetworkCaptureMode;
import io.github.testlens.selenium.network.NetworkDiagnosticsOptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.nio.file.Path;

class NetworkDiagnosticsExampleTest {

    @Disabled("Documentation-only example; requires a real browser/network-capable WebDriver.")
    @Test
    void networkDiagnosticsUsage() {
        WebDriver driver = null; // replace with a real driver

        JsOverlayDebug overlay = new JsOverlayDebug(driver);
        overlay.startSession("Checkout flow");

        overlay.network().start(NetworkDiagnosticsOptions.builder()
                .captureMode(NetworkCaptureMode.AUTO)
                .failedStatusThreshold(400)
                .ignoreUrlPattern(".*analytics.*")
                .build());

        overlay.step("Save order", () -> {
            overlay.getByTestId("save-order").click();
            overlay.expect(overlay.getByTestId("toast")).toContainText("Saved");
        });

        overlay.network().assertNoFailedRequests();
        overlay.network().attachToSession(
                overlay.session().orElseThrow(),
                Path.of("target/ui-test-lens/network/checkout-flow-network.json")
        );

        overlay.exportTraceHtml(Path.of("target/ui-test-lens/checkout-flow.html"));
    }
}
