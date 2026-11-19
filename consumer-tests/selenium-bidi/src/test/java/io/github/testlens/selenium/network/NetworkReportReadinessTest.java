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
