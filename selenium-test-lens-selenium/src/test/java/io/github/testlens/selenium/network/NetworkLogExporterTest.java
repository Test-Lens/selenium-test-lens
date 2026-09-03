package io.github.testlens.selenium.network;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkLogExporterTest {

    @Test
    void exportsEscapedJson() {
        NetworkEvent event = NetworkEvent.request(new NetworkRequest(
                "1",
                "POST",
                "https://app.example.com/api/orders?note=\"x\"",
                "xhr",
                null,
                Map.of("X-Test", "line\nvalue")
        ));

        String json = new NetworkLogExporter().export(List.of(event));

        assertTrue(json.startsWith("["));
        assertTrue(json.contains("\\\"x\\\""));
        assertTrue(json.contains("line\\nvalue"));
    }

    @Test
    void diagnosticsExportIncludesRequestedActiveAndLossCounters() {
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) -> null);
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(driver)
                .start(NetworkDiagnosticsOptions.builder()
                        .captureMode(NetworkCaptureMode.MANUAL)
                        .maxCapturedEvents(1)
                        .build());
        diagnostics.addManualEvent(NetworkEvent.request(NetworkRequest.of("GET", "/one")));
        diagnostics.addManualEvent(NetworkEvent.request(NetworkRequest.of("GET", "/two")));

        String json = diagnostics.exportJson();

        assertTrue(json.contains("\"requestedCaptureMode\":\"MANUAL\""));
        assertTrue(json.contains("\"activeCaptureMode\":\"MANUAL\""));
        assertTrue(json.contains("\"droppedEvents\":1"));
    }
}

