package io.github.testlens.selenium.network;

import io.github.testlens.core.OverlayLogger;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class NetworkReportReadinessTest {
    @Test
    void correlatedResponsesDoNotCompleteTheJournalBeforeTheDelayedRedirectRequestArrives() {
        String origin = "http://peer.invalid";
        AtomicReference<NetworkCaptureSink> callbacks = new AtomicReference<>();
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) -> null);
        NetworkDiagnostics network = new NetworkDiagnostics(driver, OverlayLogger.noop(), (ignored, options, sink) -> {
            callbacks.set(sink);
            return () -> {};
        }).start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.BIDI).build());
        Map<String, Long> requests = Map.of("GET " + origin + "/redirect", 1L, "GET " + origin + "/final", 1L);
        Map<String, Long> responses = Map.of("302 " + origin + "/redirect", 1L, "200 " + origin + "/final", 1L);
        try {
            callbacks.get().recorded(request(origin + "/redirect", 0));
            callbacks.get().recorded(response(origin + "/redirect", 302, 0));
            callbacks.get().recorded(response(origin + "/final", 200, 1));

            NetworkWaitResult matched = network.waitForResponse(NetworkWaitCondition.builder()
                    .exactUrl(origin + "/final").method("GET").status(200).build());
            assertEquals(NetworkWaitStatus.MATCHED, matched.status());
            assertEquals(origin + "/final", matched.matchedRequest().url());
            assertEquals(2, network.summary().totalResponses(), "The previous response-only barrier is already satisfied");
            assertEquals(1, network.summary().totalRequests());
            assertFalse(NetworkReportCorrectnessTest.trafficComplete(network.events(), requests, responses),
                    "Response completion and embedded correlation cannot prove request callback completion");

            callbacks.get().recorded(request(origin + "/final", 1));
            assertTrue(NetworkReportCorrectnessTest.trafficComplete(network.events(), requests, responses));
            assertEquals(2, network.summary().totalRequests());
            assertEquals(2, network.summary().totalResponses());
            assertEquals(1, network.events().stream().filter(event -> event.request() != null
                    && event.url().equals(origin + "/final") && "1".equals(event.attributes().get("redirectCount"))).count());
            assertTrue(network.exportJson().contains(origin + "/final"));
        } finally {
            network.stop();
        }
    }

    @Test
    void timeoutDiagnosticRetainsTrafficCountsWithoutHeadersCredentialsOrUrlSecrets() {
        String secret = "DIAGNOSTIC_SECRET_SENTINEL";
        AtomicReference<NetworkCaptureSink> callbacks = new AtomicReference<>();
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) -> null);
        NetworkDiagnostics network = new NetworkDiagnostics(driver, OverlayLogger.noop(), (ignored, options, sink) -> {
            callbacks.set(sink);
            return () -> {};
        }).start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.BIDI).build());
        try {
            String url = "http://user:password@peer.invalid/case/final?token=" + secret + "#" + secret;
            NetworkRequest request = new NetworkRequest("shared-redirect", "GET", url, "", Instant.now(),
                    Map.of("Authorization", secret, "Cookie", secret));
            callbacks.get().recorded(NetworkEvent.request(request));
            callbacks.get().recorded(response(url, 200, 1));
            callbacks.get().recorded(NetworkEvent.warning(secret));
            String diagnostic = NetworkReportCorrectnessTest.trafficDiagnostic(network,
                    Map.of("GET http://peer.invalid/case/final", 1L),
                    Map.of("200 http://peer.invalid/case/final", 1L));
            assertTrue(diagnostic.contains("actualRequests={GET /case/final=1}"));
            assertTrue(diagnostic.contains("actualResponses={200 /case/final=1}"));
            assertTrue(diagnostic.contains("status=STARTED"));
            assertFalse(diagnostic.contains(secret));
            assertFalse(diagnostic.contains("password"));
            assertFalse(diagnostic.contains("Authorization"));
            assertFalse(diagnostic.contains("Cookie"));
            assertFalse(diagnostic.contains("token="));
        } finally { network.stop(); }
    }

    @Test
    void timeoutDiagnosticIdentifiesCaptureFailureWithoutPrintingItsMessage() {
        String secret = "FAILURE_DIAGNOSTIC_SECRET";
        AtomicReference<NetworkCaptureSink> callbacks = new AtomicReference<>();
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) -> null);
        NetworkDiagnostics network = new NetworkDiagnostics(driver, OverlayLogger.noop(), (ignored, options, sink) -> {
            callbacks.set(sink);
            return () -> {};
        }).start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.BIDI).build());
        try {
            callbacks.get().failed(new BiDiCaptureException(BiDiFailureCategory.PROTOCOL_ERROR, secret,
                    Map.of(), new IllegalArgumentException(secret)));
            String diagnostic = NetworkReportCorrectnessTest.trafficDiagnostic(network, Map.of(), Map.of());
            assertTrue(diagnostic.contains("status=FAILED"));
            assertTrue(diagnostic.contains("PROTOCOL_ERROR"));
            assertTrue(diagnostic.contains("java.lang.IllegalArgumentException@"));
            assertFalse(diagnostic.contains(secret));
        } finally { network.stop(); }
    }

    @Test
    void rawTrafficObservationRetainsOnlySafeFixtureMetadata() {
        String secret = "RAW_WIRE_SECRET";
        Map<String, Object> payload = Map.of("redirectCount", 1L,
                "request", Map.of("method", "GET", "url", "http://user:password@peer.invalid/case/final?token=" + secret,
                        "headers", Map.of("Authorization", secret), "body", secret, "request", secret),
                "response", Map.of("status", 200L, "url", "http://peer.invalid/case/redirect#" + secret,
                        "headers", Map.of("Set-Cookie", secret), "body", secret));
        String row = NetworkReportCorrectnessTest.rawTrafficRow("network.responseCompleted", payload);
        assertEquals("network.responseCompleted method=GET requestUrl=/case/final hop=1 responseStatus=200 responseUrl=/case/redirect", row);
        String started = NetworkReportCorrectnessTest.rawTrafficRow("network.responseStarted", payload);
        assertEquals("network.responseStarted method=GET requestUrl=/case/final hop=1 responseStatus=200 responseUrl=/case/redirect", started);
        assertFalse(started.contains(secret));
        assertFalse(started.contains("password"));
        assertFalse(row.contains(secret));
        assertFalse(row.contains("password"));
        assertNull(NetworkReportCorrectnessTest.rawTrafficRow("network.beforeRequestSent",
                Map.of("request", Map.of("url", "https://outside.invalid/" + secret))));
        assertNull(NetworkReportCorrectnessTest.rawTrafficRow(secret, payload));
    }

    @Test
    void rawWireDiagnosticsAreDisabledByDefaultWithoutReadingTheDriver() {
        withWireDiagnostics(null, () -> {
            WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.class, org.openqa.selenium.bidi.HasBiDi.class},
                    (proxy, method, args) -> { throw new AssertionError("Disabled probe must not access the driver"); });
            var probe = NetworkReportCorrectnessTest.RawTrafficProbe.open(driver, null);
            assertTrue(probe.snapshot().startsWith("DISABLED (bidi.wire.diagnostics=false)"));
            probe.close();
            probe.close();
        });
    }

    @Test
    void unknownLensCaptureSourceDoesNotOpenAnotherConnection() {
        withWireDiagnostics("true", () -> {
            WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.class},
                    (proxy, method, args) -> { throw new AssertionError("Probe must not augment or access a raw driver"); });
            NetworkDiagnostics network = new NetworkDiagnostics(driver, OverlayLogger.noop(),
                    (ignored, options, sink) -> () -> {}).start(
                    NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.BIDI).build());
            try {
                var probe = NetworkReportCorrectnessTest.RawTrafficProbe.open(driver, network);
                assertTrue(probe.snapshot().startsWith("UNAVAILABLE (Lens capture source not recognized; no extra connection opened)"));
                probe.close();
                probe.close();
                assertTrue(network.isStarted(), "Probe does not own or stop the Lens capture source");
            } finally { network.stop(); }
        });
    }

    @Test
    void enabledRawWireProbeDoesNotHideCriticalDriverErrors() {
        withWireDiagnostics("true", () -> {
            LinkageError failure = new LinkageError("synthetic public BiDi accessor failure");
            WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.class, org.openqa.selenium.bidi.HasBiDi.class},
                    (proxy, method, args) -> { throw failure; });
            assertSame(failure, assertThrows(LinkageError.class,
                    () -> NetworkReportCorrectnessTest.RawTrafficProbe.open(driver, null)));
        });
    }

    @Test
    void rawWireRowsAreBoundedAndClosedProbeIgnoresFurtherEvents() {
        var probe = new NetworkReportCorrectnessTest.RawTrafficProbe(null, "TEST_FIXTURE");
        Map<String, Object> payload = Map.of("redirectCount", 1L,
                "request", Map.of("method", "GET", "url", "http://user:password@peer.invalid/case/final?token=SECRET"));
        for (int index = 0; index < 200; index++) probe.record("network.beforeRequestSent", payload);
        String snapshot = probe.snapshot();
        assertEquals(128, snapshot.split("network.beforeRequestSent", -1).length - 1);
        assertTrue(snapshot.startsWith("TEST_FIXTURE"));
        assertFalse(snapshot.contains("SECRET"));
        assertFalse(snapshot.contains("password"));
        probe.close();
        probe.close();
        probe.record("network.responseCompleted", payload);
        assertEquals(snapshot, probe.snapshot());
    }

    private static void withWireDiagnostics(String value, Runnable assertions) {
        String previous = System.getProperty("bidi.wire.diagnostics");
        try {
            if (value == null) System.clearProperty("bidi.wire.diagnostics");
            else System.setProperty("bidi.wire.diagnostics", value);
            assertions.run();
        } finally {
            if (previous == null) System.clearProperty("bidi.wire.diagnostics");
            else System.setProperty("bidi.wire.diagnostics", previous);
        }
    }
    private static NetworkEvent request(String url, int hop) {
        return NetworkEvent.request(requestData(url), Instant.now(), Map.of("redirectCount", String.valueOf(hop)));
    }

    private static NetworkEvent response(String url, int status, int hop) {
        return NetworkEvent.response(NetworkResponse.of("shared-redirect", url, status), requestData(url), Instant.now(),
                Map.of("redirectCount", String.valueOf(hop)));
    }

    private static NetworkRequest requestData(String url) {
        return new NetworkRequest("shared-redirect", "GET", url, "", Instant.now(), Map.of());
    }
}
