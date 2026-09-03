package io.github.testlens.selenium.network;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.bidi.network.BeforeRequestSent;
import org.openqa.selenium.bidi.network.FetchError;
import org.openqa.selenium.bidi.network.ResponseDetails;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class SeleniumBiDiNetworkCaptureSourceTest {
    @Test
    void registersExactlyThreeOfficialListenersAndClosesOnce() {
        FakeModule module = new FakeModule();
        NetworkCaptureSource source = SeleniumBiDiNetworkCaptureSource.subscribe(
                module, NetworkDiagnosticsOptions.defaults(), new RecordingSink());

        assertEquals(1, module.beforeRegistrations);
        assertEquals(1, module.responseRegistrations);
        assertEquals(1, module.errorRegistrations);

        source.close();
        source.close();
        assertEquals(1, module.closes.get());
    }

    @Test
    void partialRegistrationFailureClosesModuleAndPreservesCause() {
        FakeModule module = new FakeModule();
        RuntimeException expected = new RuntimeException("subscription failed");
        module.registrationFailure = expected;

        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> SeleniumBiDiNetworkCaptureSource.subscribe(
                        module, NetworkDiagnosticsOptions.defaults(), new RecordingSink()));

        assertSame(expected, actual);
        assertEquals(1, module.beforeRegistrations);
        assertEquals(1, module.responseRegistrations);
        assertEquals(0, module.errorRegistrations);
        assertEquals(1, module.closes.get());
    }

    @Test
    void mapsRequestResponseAndFetchErrorWithCorrelationMetadata() {
        FakeModule module = new FakeModule();
        RecordingSink sink = new RecordingSink();
        SeleniumBiDiNetworkCaptureSource.subscribe(module,
                NetworkDiagnosticsOptions.builder().includeHeaders(true).build(), sink);

        module.before.accept(BeforeRequestSent.fromJsonMap(base("req-7", "/api/orders", 2, 1_700_000_000_123L)));
        module.response.accept(ResponseDetails.fromJsonMap(response(
                "req-7", "/api/orders", 2, 201, 1_700_000_000_150L, 10, 37)));
        module.error.accept(FetchError.fromJsonMap(fetchError(
                "req-8", "/api/broken", 0, 1_700_000_000_200L)));

        NetworkEvent request = sink.events.get(0);
        NetworkEvent response = sink.events.get(1);
        NetworkEvent failure = sink.events.get(2);
        assertEquals("req-7", request.request().id());
        assertEquals("POST", request.request().method());
        assertEquals(Instant.ofEpochMilli(1_700_000_000_123L), request.timestamp());
        assertEquals("ctx-1", request.attributes().get("browsingContext"));
        assertEquals("nav-1", request.attributes().get("navigationId"));
        assertEquals("2", request.attributes().get("redirectCount"));
        assertEquals("false", request.attributes().get("resourceTypeAvailable"));
        assertEquals("", request.request().resourceType());
        assertEquals(201, response.response().status());
        assertEquals("req-7", response.response().requestId());
        assertNotNull(response.correlatedRequest());
        assertEquals("req-7", response.correlatedRequest().id());
        assertEquals("POST", response.correlatedRequest().method());
        assertEquals("/api/orders", response.correlatedRequest().url());
        assertEquals(Duration.ofMillis(27), response.response().duration());
        assertEquals("true", response.attributes().get("durationAvailable"));
        assertEquals("false", response.attributes().get("fromCache"));
        assertEquals("req-8", failure.failure().requestId());
        assertEquals("FETCH_ERROR", failure.failure().failureType());
        assertEquals("connection reset", failure.failure().message());
    }

    @Test
    void responseBeforeRequestRetainsRequestDataWithoutCreatingADuplicateRequestEvent() {
        FakeModule module = new FakeModule();
        RecordingSink sink = new RecordingSink();
        SeleniumBiDiNetworkCaptureSource.subscribe(module, NetworkDiagnosticsOptions.defaults(), sink);

        module.response.accept(ResponseDetails.fromJsonMap(response(
                "race", "/api/race", 0, 201, 1_700_000_000_150L, 10, 37)));
        module.before.accept(BeforeRequestSent.fromJsonMap(base(
                "race", "/api/race", 0, 1_700_000_000_123L)));

        assertEquals(2, sink.events.size());
        assertEquals(NetworkEventType.RESPONSE, sink.events.get(0).type());
        assertEquals(NetworkEventType.REQUEST, sink.events.get(1).type());
        assertEquals("race", sink.events.get(0).correlatedRequest().id());
        assertEquals(1, sink.events.stream().filter(event -> event.type() == NetworkEventType.REQUEST).count());
        assertEquals(1, sink.events.stream().filter(event -> event.type() == NetworkEventType.RESPONSE).count());
    }

    @Test
    void headersAreOptionalMaskedCaseInsensitivelyAndPreserveBase64AndDuplicates() {
        Map<String, Object> event = base("req-h", "/api/headers", 0, 1000);
        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) event.get("request");
        request.put("headers", List.of(
                header("Authorization", "string", "Bearer secret"),
                header("proxy-authorization", "string", "proxy secret"),
                header("COOKIE", "string", "sid=secret"),
                header("Set-Cookie", "string", "token=secret"),
                header("x-api-key", "string", "key"),
                header("X-Auth-Token", "string", "token"),
                header("X-Repeat", "string", "one"),
                header("x-repeat", "base64", "dHdv")));

        RecordingSink omitted = captureRequest(event, NetworkDiagnosticsOptions.defaults());
        assertTrue(omitted.events.get(0).request().headers().isEmpty());

        RecordingSink included = captureRequest(event, NetworkDiagnosticsOptions.builder()
                .includeHeaders(true).maskSensitiveHeaders(true).build());
        Map<String, String> headers = included.events.get(0).request().headers();
        assertEquals("***", headers.get("Authorization"));
        assertEquals("***", headers.get("proxy-authorization"));
        assertEquals("***", headers.get("COOKIE"));
        assertEquals("***", headers.get("Set-Cookie"));
        assertEquals("***", headers.get("x-api-key"));
        assertEquals("***", headers.get("X-Auth-Token"));
        assertEquals("one\nbase64:dHdv", headers.get("X-Repeat"));
        assertFalse(included.events.get(0).attributes().containsKey("cookies"));
        assertFalse(included.events.get(0).attributes().containsKey("body"));

        RecordingSink unmasked = captureRequest(event, NetworkDiagnosticsOptions.builder()
                .includeHeaders(true).maskSensitiveHeaders(false).build());
        assertEquals("Bearer secret", unmasked.events.get(0).request().headers().get("Authorization"));
    }

    @Test
    void ignoredUrlNeverProducesCapturedEvent() {
        FakeModule module = new FakeModule();
        RecordingSink sink = new RecordingSink();
        SeleniumBiDiNetworkCaptureSource.subscribe(module, NetworkDiagnosticsOptions.builder()
                .includeHeaders(true).ignoreUrlPattern(".*ignored.*").build(), sink);

        module.before.accept(BeforeRequestSent.fromJsonMap(base("req-i", "/ignored", 0, 1000)));

        assertEquals(0, sink.events.size());
        assertEquals(1, sink.ignored.get());
    }

    @Test
    void unavailableOrNegativeTimingProducesZeroDuration() {
        FakeModule module = new FakeModule();
        RecordingSink sink = new RecordingSink();
        SeleniumBiDiNetworkCaptureSource.subscribe(module, NetworkDiagnosticsOptions.defaults(), sink);

        module.response.accept(ResponseDetails.fromJsonMap(response("req", "/api", 0, 200, 1000, 20, 10)));

        assertEquals(Duration.ZERO, sink.events.get(0).response().duration());
        assertEquals("false", sink.events.get(0).attributes().get("durationAvailable"));
    }

    private RecordingSink captureRequest(Map<String, Object> event, NetworkDiagnosticsOptions options) {
        FakeModule module = new FakeModule();
        RecordingSink sink = new RecordingSink();
        SeleniumBiDiNetworkCaptureSource.subscribe(module, options, sink);
        module.before.accept(BeforeRequestSent.fromJsonMap(event));
        return sink;
    }

    private static Map<String, Object> base(String id, String url, long redirect, long timestamp) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("context", "ctx-1");
        map.put("isBlocked", false);
        map.put("navigation", "nav-1");
        map.put("redirectCount", redirect);
        map.put("request", request(id, url, 10, 37));
        map.put("timestamp", timestamp);
        map.put("intercepts", List.of());
        map.put("initiator", Map.of("type", "script"));
        return map;
    }

    private static Map<String, Object> response(String id, String url, long redirect, int status,
                                                 long timestamp, double requestTime, double responseEnd) {
        Map<String, Object> map = base(id, url, redirect, timestamp);
        map.put("request", request(id, url, requestTime, responseEnd));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("url", url);
        response.put("protocol", "http/1.1");
        response.put("status", status);
        response.put("statusText", "Created");
        response.put("fromCache", false);
        response.put("headers", List.of(header("Content-Type", "string", "application/json")));
        response.put("mimeType", "application/json");
        response.put("bytesReceived", 42L);
        response.put("headersSize", 12L);
        response.put("bodySize", 30L);
        response.put("content", Map.of("size", 30L));
        map.put("response", response);
        return map;
    }

    private static Map<String, Object> fetchError(String id, String url, long redirect, long timestamp) {
        Map<String, Object> map = base(id, url, redirect, timestamp);
        map.put("errorText", "connection reset");
        return map;
    }

    private static Map<String, Object> request(String id, String url, double requestTime, double responseEnd) {
        return new LinkedHashMap<>(Map.of(
                "request", id, "url", url, "method", "POST", "headers", new ArrayList<>(),
                "cookies", new ArrayList<>(), "headersSize", 0L,
                "timings", timings(requestTime, responseEnd)));
    }

    private static Map<String, Object> timings(double requestTime, double responseEnd) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String name : List.of("timeOrigin", "redirectStart", "redirectEnd", "fetchStart", "dnsStart",
                "dnsEnd", "connectStart", "connectEnd", "tlsStart", "requestStart", "responseStart")) {
            values.put(name, 0.0);
        }
        values.put("requestTime", requestTime);
        values.put("responseEnd", responseEnd);
        return values;
    }

    private static Map<String, Object> header(String name, String type, String value) {
        return Map.of("name", name, "value", Map.of("type", type, "value", value));
    }

    private static final class RecordingSink implements NetworkCaptureSink {
        private final List<NetworkEvent> events = new ArrayList<>();
        private final AtomicInteger ignored = new AtomicInteger();
        @Override public void recorded(NetworkEvent event) { events.add(event); }
        @Override public void ignored() { ignored.incrementAndGet(); }
    }

    private static final class FakeModule implements SeleniumBiDiNetworkCaptureSource.Module {
        private Consumer<BeforeRequestSent> before;
        private Consumer<ResponseDetails> response;
        private Consumer<FetchError> error;
        private int beforeRegistrations;
        private int responseRegistrations;
        private int errorRegistrations;
        private RuntimeException registrationFailure;
        private final AtomicInteger closes = new AtomicInteger();

        @Override public void onBeforeRequestSent(Consumer<BeforeRequestSent> consumer) {
            beforeRegistrations++;
            before = consumer;
        }
        @Override public void onResponseCompleted(Consumer<ResponseDetails> consumer) {
            responseRegistrations++;
            if (registrationFailure != null) throw registrationFailure;
            response = consumer;
        }
        @Override public void onFetchError(Consumer<FetchError> consumer) {
            errorRegistrations++;
            error = consumer;
        }
        @Override public void close() { closes.incrementAndGet(); }
    }
}
