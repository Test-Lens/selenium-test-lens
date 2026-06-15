package io.github.testlens.selenium.network;

import io.github.testlens.core.trace.TraceArtifactType;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
                Map.of("Authorization", "Bearer sample-value", "X-Test", "ok"))));

        assertTrue(diagnostics.events().get(1).request().headers().isEmpty());

        NetworkDiagnostics withHeaders = new NetworkDiagnostics(fakeDriver()).start(NetworkDiagnosticsOptions.builder()
                .includeHeaders(true)
                .maskSensitiveHeaders(true)
                .build());
        withHeaders.addManualEvent(NetworkEvent.request(new NetworkRequest("1", "GET", "/api", "", null,
                Map.of("Authorization", "Bearer sample-value", "X-Test", "ok"))));

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

    @Test
    void waitForResponseReturnsMatchedWhenEventAlreadyExists() {
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver()).start(NetworkDiagnosticsOptions.defaults());
        diagnostics.addManualEvent(NetworkEvent.response(NetworkResponse.of("1", "/api/orders", 200)));

        NetworkWaitResult result = diagnostics.waitForResponse("/api/orders", 200);

        assertEquals(NetworkWaitStatus.MATCHED, result.status());
        assertEquals(200, result.matchedResponse().status());
    }

    @Test
    void waitForResponseReturnsTimedOutWhenNoEventAppears() {
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver()).start(NetworkDiagnosticsOptions.defaults());

        NetworkWaitResult result = diagnostics.waitForResponse(NetworkWaitCondition.builder()
                .urlContains("/api/orders")
                .status(200)
                .timeout(Duration.ofMillis(30))
                .pollInterval(Duration.ofMillis(5))
                .build());

        assertEquals(NetworkWaitStatus.TIMED_OUT, result.status());
        assertTrue(result.message().contains("/api/orders"));
    }

    @Test
    void waitForResponseMatchesEventAddedDuringPolling() throws Exception {
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver()).start(NetworkDiagnosticsOptions.defaults());
        CountDownLatch added = new CountDownLatch(1);

        Thread thread = new Thread(() -> {
            LockSupport.parkNanos(Duration.ofMillis(25).toNanos());
            diagnostics.addManualEvent(NetworkEvent.response(NetworkResponse.of("1", "/api/orders", 201)));
            added.countDown();
        });
        thread.start();

        NetworkWaitResult result = diagnostics.waitForResponse(NetworkWaitCondition.builder()
                .urlContains("/api/orders")
                .status(201)
                .timeout(Duration.ofMillis(300))
                .pollInterval(Duration.ofMillis(10))
                .build());

        assertTrue(added.await(1, TimeUnit.SECONDS));
        assertEquals(NetworkWaitStatus.MATCHED, result.status());
        assertNotNull(result.matchedEvent());
    }

    @Test
    void waitForResponseCanMatchMethodThroughRequestId() {
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver()).start(NetworkDiagnosticsOptions.defaults());
        diagnostics.addManualEvent(NetworkEvent.request(new NetworkRequest("req-1", "POST", "/api/orders", "", null, null)));
        diagnostics.addManualEvent(NetworkEvent.response(NetworkResponse.of("req-1", "/api/orders", 201)));

        NetworkWaitResult result = diagnostics.waitForResponse(NetworkWaitCondition.builder()
                .urlContains("/api/orders")
                .method("POST")
                .status(201)
                .build());

        assertEquals(NetworkWaitStatus.MATCHED, result.status());
        assertEquals("POST", result.matchedRequest().method());
    }

    @Test
    void waitForResponseReturnsFailedWhenFailedResponseIsExcluded() {
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver()).start(NetworkDiagnosticsOptions.defaults());
        diagnostics.addManualEvent(NetworkEvent.response(NetworkResponse.of("1", "/api/orders", 500)));

        NetworkWaitResult result = diagnostics.waitForResponse(NetworkWaitCondition.builder()
                .urlContains("/api/orders")
                .status(500)
                .includeFailedResponses(false)
                .timeout(Duration.ofMillis(30))
                .pollInterval(Duration.ofMillis(5))
                .build());

        assertEquals(NetworkWaitStatus.FAILED, result.status());
        assertEquals(NetworkWaitFailureReason.FAILED_RESPONSE_MATCHED, result.failureReason());
    }

    @Test
    void waitForResponseSkippedWhenCaptureNotStarted() {
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver());

        NetworkWaitResult result = diagnostics.waitForResponse("/api/orders", 200);

        assertEquals(NetworkWaitStatus.SKIPPED, result.status());
        assertEquals(NetworkWaitFailureReason.CAPTURE_NOT_STARTED, result.failureReason());
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

