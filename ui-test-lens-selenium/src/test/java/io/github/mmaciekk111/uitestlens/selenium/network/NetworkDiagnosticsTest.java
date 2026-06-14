package io.github.mmaciekk111.uitestlens.selenium.network;

import io.github.mmaciekk111.uitestlens.core.trace.TraceArtifactType;
import io.github.mmaciekk111.uitestlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkDiagnosticsTest {
    @TempDir
    Path tempDir;

    @Test
    void manualModeRecordsEventsAndAssertPassesWithoutFailures() {
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver())
                .start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.MANUAL).build());

        diagnostics.addManualEvent(NetworkEvent.request(NetworkRequest.of("GET", "/api/orders")));
        diagnostics.addManualEvent(NetworkEvent.response(NetworkResponse.of("1", "/api/orders", 200)));

        assertEquals(1, diagnostics.summary().totalRequests());
        assertEquals(NetworkDiagnosticsStatus.ASSERTION_PASSED, diagnostics.assertNoFailedRequests().status());
    }

    @Test
    void assertNoFailedRequestsThrowsForFailedResponse() {
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver()).start(NetworkDiagnosticsOptions.defaults());
        diagnostics.addManualEvent(NetworkEvent.response(NetworkResponse.of("1", "/api/orders", 500)));

        assertThrows(NetworkAssertionError.class, diagnostics::assertNoFailedRequests);
    }

    @Test
    void ignoredUrlPatternSkipsEvents() {
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver()).start(NetworkDiagnosticsOptions.builder()
                .ignoreUrlPattern(".*analytics.*")
                .build());

        diagnostics.addManualEvent(NetworkEvent.response(NetworkResponse.of("1", "https://x/analytics/pixel", 500)));

        assertEquals(0, diagnostics.summary().failedResponses());
        assertEquals(1, diagnostics.summary().ignoredEvents());
    }

    @Test
    void headersAreOmittedByDefaultAndCanBeMasked() {
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver()).start(NetworkDiagnosticsOptions.defaults());
        diagnostics.addManualEvent(NetworkEvent.request(new NetworkRequest("1", "GET", "/api", "", null,
                Map.of("Authorization", "Bearer secret", "X-Test", "ok"))));

        assertTrue(diagnostics.events().get(1).request().headers().isEmpty());

        NetworkDiagnostics withHeaders = new NetworkDiagnostics(fakeDriver()).start(NetworkDiagnosticsOptions.builder()
                .includeHeaders(true)
                .maskSensitiveHeaders(true)
                .build());
        withHeaders.addManualEvent(NetworkEvent.request(new NetworkRequest("1", "GET", "/api", "", null,
                Map.of("Authorization", "Bearer secret", "X-Test", "ok"))));

        assertEquals("***", withHeaders.events().get(1).request().headers().get("Authorization"));
        assertEquals("ok", withHeaders.events().get(1).request().headers().get("X-Test"));
    }

    @Test
    void attachToSessionWritesNetworkLogArtifact() throws Exception {
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver()).start(NetworkDiagnosticsOptions.defaults());
        diagnostics.addManualEvent(NetworkEvent.response(NetworkResponse.of("1", "/api/orders", 200)));
        UiTestLensSession session = UiTestLensSession.start("Checkout");
        Path output = tempDir.resolve("network.json");

        NetworkDiagnosticsResult result = diagnostics.attachToSession(session, output);

        assertEquals(NetworkDiagnosticsStatus.ATTACHED, result.status());
        assertTrue(Files.readString(output).contains("/api/orders"));
        assertEquals(TraceArtifactType.NETWORK_LOG, session.artifacts().get(0).type());
    }

    @Test
    void unsupportedCaptureModeFallsBackWithoutCrash() {
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver())
                .start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.BIDI).build());

        assertTrue(diagnostics.isStarted());
        assertEquals(NetworkDiagnosticsStatus.UNSUPPORTED, diagnostics.summary().status());
    }

    private static WebDriver fakeDriver() {
        return (WebDriver) Proxy.newProxyInstance(
                NetworkDiagnosticsTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "toString" -> "network-driver";
                    default -> null;
                }
        );
    }
}
