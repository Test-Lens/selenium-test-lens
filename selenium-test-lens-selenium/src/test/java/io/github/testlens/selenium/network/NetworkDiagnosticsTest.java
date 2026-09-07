package io.github.testlens.selenium.network;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.core.trace.TraceArtifactType;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkDiagnosticsTest {
    @TempDir
    Path tempDir;

    @Test
    void manualModeStartsAsStarted() {
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver())
                .start(NetworkDiagnosticsOptions.defaults());

        assertTrue(diagnostics.isStarted());
        assertEquals(NetworkDiagnosticsStatus.STARTED, diagnostics.summary().status());
    }

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
    void centralPolicyProtectsEventsExportAndWaitEvenWhenHeaderMaskingIsDisabled() {
        String secret = "network-canary-5b9e";
        RedactionPolicy policy = RedactionPolicy.builder().secret(secret).build();
        OverlayLogger logger = OverlayLogger.from(io.github.testlens.core.logging.UiTestLensLogger.builder()
                .redactionPolicy(policy).build());
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver(), logger)
                .start(NetworkDiagnosticsOptions.builder().includeHeaders(true)
                        .maskSensitiveHeaders(false).build());
        String url = "/api/orders?access_token=" + secret + "&page=1";
        diagnostics.addManualEvent(NetworkEvent.request(new NetworkRequest("req-1", "GET", url, "", null,
                Map.of("Authorization", "Bearer " + secret, "X-Test", secret))));
        diagnostics.addManualEvent(NetworkEvent.response(NetworkResponse.of("req-1", url, 200)));

        NetworkWaitResult wait = diagnostics.waitForResponse(NetworkWaitCondition.builder()
                .exactUrl(url).status(200).build());

        assertEquals(NetworkWaitStatus.MATCHED, wait.status());
        String exposed = diagnostics.events() + diagnostics.exportJson() + wait.message()
                + wait.conditionSummary() + wait.matchedEvent();
        assertFalse(exposed.contains(secret));
        assertTrue(diagnostics.exportJson().contains("[REDACTED]"));
        assertEquals("[REDACTED]", diagnostics.events().stream().filter(event -> event.request() != null)
                .findFirst().orElseThrow().request().headers().get("Authorization"));
        assertEquals(1, diagnostics.summary().totalRequests());
        assertEquals(1, diagnostics.summary().totalResponses());
    }

    @Test
    void centralPolicyStructurallyRedactsJsonInNetworkMetadata() {
        String canary = "o'NETWORK_JSON_CANARY";
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver())
                .start(NetworkDiagnosticsOptions.builder().includeHeaders(true)
                        .maskSensitiveHeaders(false).build());
        diagnostics.addManualEvent(NetworkEvent.request(new NetworkRequest("req-json", "POST", "/api", "", null,
                Map.of("X-Diagnostics", "{\"password\":\"" + canary + "\"}"))));

        String exposed = diagnostics.events() + diagnostics.exportJson();

        assertFalse(exposed.contains("NETWORK_JSON_CANARY"));
        assertFalse(exposed.contains("o'NETWORK"));
        assertTrue(exposed.contains("[REDACTED]"));
        assertEquals(1, diagnostics.summary().totalRequests());
    }

    @Test
    void centralPolicyRedactsSummaryAndNetworkAssertionFailure() {
        String canary = "TL_NETWORK_SECRET";
        String url = "https://user:pass@example.test/api?token=" + canary
                + "&safe=visible#TL_FRAGMENT_SECRET";
        List<io.github.testlens.core.logging.UiTestLensLogEntry> entries = new ArrayList<>();
        OverlayLogger logger = OverlayLogger.from(io.github.testlens.core.logging.UiTestLensLogger.builder()
                .sink(entries::add).build());
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver(), logger)
                .start(NetworkDiagnosticsOptions.builder().includeHeaders(true)
                        .maskSensitiveHeaders(false).build());
        diagnostics.addManualEvent(NetworkEvent.response(new NetworkResponse("req-secret", url, 503,
                "Service unavailable", "application/json", Duration.ZERO, null,
                Map.of("Set-Cookie", "session=" + canary))));

        NetworkWaitResult wait = diagnostics.waitForResponse(NetworkWaitCondition.builder()
                .urlContains("/api").status(503).includeFailedResponses(true).build());

        assertFalse(diagnostics.events().get(1).url().contains(canary));
        NetworkSummary summary = diagnostics.summary();
        NetworkAssertionError error = assertThrows(NetworkAssertionError.class,
                diagnostics::assertNoFailedRequests);
        NetworkEvent firstFailure = summary.firstFailure().orElseThrow();
        String exposed = firstFailure.url() + firstFailure.response().headers()
                + summary.failureSummary() + summary + firstFailure
                + error.getMessage() + error + error.summary().failureSummary()
                + error.summary().firstFailure().orElseThrow().url()
                + wait.message() + entries.stream().map(entry -> entry.message() + entry.metadata())
                .reduce("", String::concat);

        assertFalse(exposed.contains(canary));
        assertFalse(exposed.contains("user:pass"));
        assertFalse(exposed.contains("TL_FRAGMENT_SECRET"));
        assertTrue(firstFailure.url().contains("token=[REDACTED]"));
        assertTrue(firstFailure.url().contains("safe=visible"));
        assertEquals("[REDACTED]", firstFailure.response().headers().get("Set-Cookie"));
        assertEquals(1, summary.totalResponses());
    }

    @Test
    void fetchFailureAndCustomPolicyAreSafeAcrossSummaryErrorExportAndExternalSink() {
        String literal = "TL_LITERAL_NETWORK_SECRET";
        String query = "TL_CUSTOM_QUERY_SECRET";
        String fragment = "TL_FAILURE_FRAGMENT_SECRET";
        String replacement = "MASK\"\\\n";
        RedactionPolicy policy = RedactionPolicy.builder()
                .sensitiveKey("tenant-key")
                .secret(literal)
                .replacement(replacement)
                .build();
        List<io.github.testlens.core.logging.UiTestLensLogEntry> entries = new ArrayList<>();
        OverlayLogger logger = OverlayLogger.from(io.github.testlens.core.logging.UiTestLensLogger.builder()
                .redactionPolicy(policy).sink(entries::add).build());
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver(), logger)
                .start(NetworkDiagnosticsOptions.builder().includeHeaders(true)
                        .maskSensitiveHeaders(false).build());
        String url = "https://user:pass@example.test/api?tenant-key=" + query
                + "&safe=visible#" + fragment;
        diagnostics.addManualEvent(NetworkEvent.request(new NetworkRequest("request", "GET", url, "fetch",
                Instant.now(), Map.of("Authorization", "Bearer " + literal,
                "Cookie", "session=" + literal))));
        diagnostics.addManualEvent(NetworkEvent.failed(new NetworkFailure("request", url,
                "connection failed " + literal, "FETCH_ERROR", Instant.now())));

        NetworkSummary before = diagnostics.summary();
        NetworkAssertionError error = assertThrows(NetworkAssertionError.class,
                diagnostics::assertNoFailedRequests);
        diagnostics.addManualEvent(NetworkEvent.response(NetworkResponse.of("later", "/later", 200)));

        String sinkData = entries.stream().map(entry -> entry.message() + entry.metadata()
                + (entry.throwable() == null ? "" : entry.throwable().toString()))
                .reduce("", String::concat);
        String outward = diagnostics.events() + diagnostics.exportJson() + before.failureSummary()
                + before.firstFailure().orElseThrow().failure().message()
                + error.getMessage() + error + error.summary().failureSummary()
                + sinkData;
        for (String secret : List.of(literal, query, fragment, "user:pass")) {
            assertFalse(outward.contains(secret), secret);
        }
        assertTrue(outward.contains(replacement));
        assertTrue(before.firstFailure().orElseThrow().url().contains("safe=visible"));
        assertEquals(2, before.totalRequests() + before.failedRequests());
        assertEquals(0, before.totalResponses(), "summary must be an immutable point-in-time snapshot");
        assertEquals(1, diagnostics.summary().totalResponses());
    }

    @Test
    void redactionDoesNotChangeRawWaitMatchingOrCaptureCounts() {
        String secret = "TL_RAW_MATCH_SECRET";
        String url = "https://example.test/api?token=" + secret + "&safe=visible#private";
        RedactionPolicy policy = RedactionPolicy.builder().secret(secret).build();
        OverlayLogger logger = OverlayLogger.from(io.github.testlens.core.logging.UiTestLensLogger.builder()
                .redactionPolicy(policy).build());
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver(), logger)
                .start(NetworkDiagnosticsOptions.builder().includeHeaders(true)
                        .maskSensitiveHeaders(false).maxCapturedEvents(2).build());
        diagnostics.addManualEvent(NetworkEvent.request(new NetworkRequest("request", "POST", url, "", null,
                Map.of("Authorization", "Bearer " + secret))));
        diagnostics.addManualEvent(NetworkEvent.response(NetworkResponse.of("request", url, 201)));

        NetworkWaitResult result = diagnostics.waitForResponse(NetworkWaitCondition.builder()
                .exactUrl(url).method("POST").status(201).build());

        assertEquals(NetworkWaitStatus.MATCHED, result.status());
        assertEquals("POST", result.matchedRequest().method());
        assertFalse(result.matchedRequest().url().contains(secret));
        assertFalse(result.matchedResponse().url().contains(secret));
        assertFalse(result.conditionSummary().contains(secret));
        assertFalse(result.conditionSummary().contains("#private"));
        assertFalse(result.message().contains(secret));
        assertFalse(result.message().contains("#private"));
        assertEquals(1, diagnostics.summary().totalRequests());
        assertEquals(1, diagnostics.summary().totalResponses());
        assertEquals(0, diagnostics.summary().droppedEvents());
    }

    @Test
    void disabledCentralPolicyIsAnExplicitRawDataOptOut() {
        String secret = "TL_DISABLED_NETWORK_SECRET";
        String url = "https://user:pass@example.test/api?token=" + secret + "#fragment";
        OverlayLogger logger = OverlayLogger.from(io.github.testlens.core.logging.UiTestLensLogger.builder()
                .redactionPolicy(RedactionPolicy.disabled()).build());
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver(), logger)
                .start(NetworkDiagnosticsOptions.builder().includeHeaders(true)
                        .maskSensitiveHeaders(false).build());
        diagnostics.addManualEvent(NetworkEvent.response(NetworkResponse.of("request", url, 503)));

        NetworkAssertionError error = assertThrows(NetworkAssertionError.class,
                diagnostics::assertNoFailedRequests);

        assertTrue(diagnostics.summary().firstFailure().orElseThrow().url().contains(secret));
        assertTrue(error.getMessage().contains(secret));
    }

    @Test
    void manualEventReturnIsSafeEvenWhenCaptureDoesNotRetainIt() {
        String secret = "TL_UNRETAINED_EVENT_SECRET";
        String url = "https://user:pass@example.test/ignored?token=" + secret + "#fragment";
        OverlayLogger logger = OverlayLogger.from(io.github.testlens.core.logging.UiTestLensLogger.builder()
                .redactionPolicy(RedactionPolicy.builder().secret(secret).build()).build());
        NetworkDiagnostics stopped = new NetworkDiagnostics(fakeDriver(), logger);
        NetworkEvent inactive = stopped.addManualEvent(NetworkEvent.failed(
                NetworkFailure.of("inactive", url, "failure " + secret)));
        NetworkDiagnostics ignored = new NetworkDiagnostics(fakeDriver(), logger).start(NetworkDiagnosticsOptions.builder()
                .ignoreUrlPattern(".*ignored.*").build());
        NetworkEvent filtered = ignored.addManualEvent(NetworkEvent.failed(
                NetworkFailure.of("ignored", url, "failure " + secret)));

        String outward = inactive.url() + inactive.failure().message()
                + filtered.url() + filtered.failure().message();
        assertFalse(outward.contains(secret));
        assertFalse(outward.contains("user:pass"));
        assertFalse(outward.contains("#fragment"));
        assertEquals(1, ignored.summary().ignoredEvents());
        assertEquals(0, ignored.summary().failedRequests());
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
    void automaticCaptureModesAreUnsupportedWithoutManualFallback() {
        for (NetworkCaptureMode mode : new NetworkCaptureMode[]{
                NetworkCaptureMode.AUTO,
                NetworkCaptureMode.BIDI,
                NetworkCaptureMode.PERFORMANCE_LOGS}) {
            NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver())
                    .start(NetworkDiagnosticsOptions.builder().captureMode(mode).build());

            assertFalse(diagnostics.isStarted(), mode.name());
            assertEquals(NetworkDiagnosticsStatus.UNSUPPORTED, diagnostics.summary().status(), mode.name());
            assertTrue(diagnostics.events().get(0).message().contains(mode.name()), mode.name());
        }
    }

    @Test
    void waitForResponseSkipsUnsupportedModeImmediatelyWithoutPolling() {
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver())
                .start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.AUTO).build());
        NetworkWaitCondition condition = NetworkWaitCondition.builder()
                .urlContains("/api/orders")
                .timeout(Duration.ofSeconds(2))
                .pollInterval(Duration.ofMillis(100))
                .build();
        long startedNanos = System.nanoTime();

        NetworkWaitResult result = diagnostics.waitForResponse(condition);

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedNanos);
        assertEquals(NetworkWaitStatus.SKIPPED, result.status());
        assertEquals(NetworkWaitFailureReason.UNSUPPORTED_CAPTURE_MODE, result.failureReason());
        assertEquals(0, result.attempts());
        assertTrue(elapsed.compareTo(Duration.ofMillis(250)) < 0);
    }

    @Test
    void offModeDoesNotStartCaptureOrAllowWaiting() {
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver())
                .start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.OFF).build());

        NetworkWaitResult result = diagnostics.waitForResponse("/api/orders", 200);

        assertFalse(diagnostics.isStarted());
        assertEquals(NetworkDiagnosticsStatus.STOPPED, diagnostics.summary().status());
        assertEquals(NetworkWaitStatus.SKIPPED, result.status());
        assertEquals(NetworkWaitFailureReason.CAPTURE_NOT_STARTED, result.failureReason());
        assertEquals(0, result.attempts());
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
        NetworkWaitCondition condition = NetworkWaitCondition.builder()
                .urlContains("/api/orders")
                .method("POST")
                .status(201)
                .build();

        assertTrue(diagnostics.findMatchingEvent(condition).isPresent());
        NetworkWaitResult result = diagnostics.waitForResponse(condition);

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

