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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SeleniumBiDiNetworkCaptureSourceTest {
    @Test
    void unsubscribeCanWaitForAnAlreadyQueuedCallbackWithoutDeadlock() {
        FakeModule module = new FakeModule();
        RecordingSink sink = new RecordingSink();
        NetworkCaptureSource source = SeleniumBiDiNetworkCaptureSource.subscribe(
                module, NetworkDiagnosticsOptions.defaults(), sink);
        var executor = Executors.newSingleThreadExecutor();
        module.closeAction = () -> {
            try {
                executor.submit(() -> module.before.accept(BeforeRequestSent.fromJsonMap(
                        base("queued", "/queued", 0, 1000)))).get(1, TimeUnit.SECONDS);
            } catch (Exception failure) {
                throw new AssertionError("Unsubscribe could not drain the queued callback", failure);
            }
        };
        try {
            assertDoesNotThrow(source::close);
            assertTrue(sink.events.isEmpty());
        } finally {
            executor.shutdownNow();
        }
    }

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
    void partialRegistrationLinkageFailureClosesModuleAndPreservesCleanupFailure() {
        FakeModule module = new FakeModule();
        LinkageError registration = new NoSuchMethodError("incompatible subscription API");
        LinkageError cleanup = new NoClassDefFoundError("incompatible cleanup API");
        module.registrationFailure = registration;
        module.closeFailure = cleanup;

        BiDiCaptureException actual = assertThrows(BiDiCaptureException.class,
                () -> SeleniumBiDiNetworkCaptureSource.subscribe(
                        module, NetworkDiagnosticsOptions.defaults(), new RecordingSink()));

        assertSame(registration, actual.getCause());
        assertEquals(1, module.closes.get());
        assertArrayEquals(new Throwable[]{cleanup}, registration.getSuppressed());
    }

    @Test
    void closedSourceIgnoresQueuedRequestResponseAndErrorCallbacks() {
        FakeModule module = new FakeModule();
        RecordingSink sink = new RecordingSink();
        NetworkCaptureSource source = SeleniumBiDiNetworkCaptureSource.subscribe(
                module, NetworkDiagnosticsOptions.defaults(), sink);
        source.close();

        module.before.accept(BeforeRequestSent.fromJsonMap(base("late", "/late", 0, 1000)));
        module.response.accept(ResponseDetails.fromJsonMap(response("late", "/late", 0, 200, 1001, 1, 2)));
        module.error.accept(FetchError.fromJsonMap(fetchError("late", "/late", 0, 1002)));

        assertTrue(sink.events.isEmpty());
        assertEquals(0, sink.ignored.get());
    }

    @Test
    void callbackLinkageFailureIsReportedOnceAndDoesNotEscapeDispatcher() {
        FakeModule module = new FakeModule();
        LinkageError linkage = new NoSuchMethodError("changed accessor");
        List<RuntimeException> failures = new ArrayList<>();
        SeleniumBiDiNetworkCaptureSource.subscribe(module, NetworkDiagnosticsOptions.defaults(),
                new NetworkCaptureSink() {
                    @Override public void recorded(NetworkEvent event) { throw linkage; }
                    @Override public void ignored() { fail("Unexpected ignored event"); }
                    @Override public void failed(RuntimeException failure) { failures.add(failure); }
                });

        BeforeRequestSent request = BeforeRequestSent.fromJsonMap(base("req", "/api", 0, 1000));
        assertDoesNotThrow(() -> module.before.accept(request));
        assertDoesNotThrow(() -> module.before.accept(request));
        assertEquals(1, failures.size());
        assertInstanceOf(BiDiCaptureException.class, failures.get(0));
        assertSame(linkage, failures.get(0).getCause());
    }

    @Test
    void largeProtocolSizesRemainExact() {
        FakeModule module = new FakeModule();
        RecordingSink sink = new RecordingSink();
        SeleniumBiDiNetworkCaptureSource.subscribe(module, NetworkDiagnosticsOptions.defaults(), sink);
        Map<String, Object> large = response("large", "/large", 0, 200, 1000, 1, 2);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) large.get("response");
        long size = (long) Integer.MAX_VALUE + 9876;
        response.put("bytesReceived", size);
        response.put("bodySize", size);
        response.put("headersSize", size);
        module.response.accept(ResponseDetails.fromJsonMap(large));

        assertEquals(String.valueOf(size), sink.events.get(0).attributes().get("bytesReceived"));
        assertEquals(String.valueOf(size), sink.events.get(0).attributes().get("bodySize"));
        assertEquals(String.valueOf(size), sink.events.get(0).attributes().get("headersSize"));
    }

    @Test
    void nullableResponseSizesAreOmitted() {
        FakeModule module = new FakeModule();
        RecordingSink sink = new RecordingSink();
        SeleniumBiDiNetworkCaptureSource.subscribe(module, NetworkDiagnosticsOptions.defaults(), sink);
        Map<String, Object> event = response("unknown", "/unknown", 0, 200, 1000, 1, 2);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) event.get("response");
        response.put("headersSize", null);
        response.put("bodySize", null);
        module.response.accept(ResponseDetails.fromJsonMap(SeleniumBiDiCompatibility.normalizeSizes(event)));
        assertEquals(1, sink.events.size());
        assertFalse(sink.events.get(0).attributes().containsKey("headersSize"));
        assertFalse(sink.events.get(0).attributes().containsKey("bodySize"));
    }

    @Test
    void malformedNullableRequestSizeFailsClosed() {
        Map<String, Object> requestEvent = base("unavailable", "/unavailable", 0, 1001);
        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) requestEvent.get("request");
        request.put("headersSize", null);
        BiDiCaptureException failure = assertThrows(BiDiCaptureException.class,
                () -> SeleniumBiDiCompatibility.normalizeSizes(requestEvent));
        assertEquals(BiDiFailureCategory.PROTOCOL_ERROR, failure.category());
        assertNull(request.get("headersSize"));
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
                "race", "/api/final", 1, 201, 1_700_000_000_150L, 10, 37)));

        List<NetworkEvent> responseSnapshot = List.copyOf(sink.events);
        assertEquals(1, responseSnapshot.size());
        assertEquals(NetworkEventType.RESPONSE, responseSnapshot.get(0).type());
        assertEquals("race", responseSnapshot.get(0).correlatedRequest().id());
        assertEquals("1", responseSnapshot.get(0).attributes().get("redirectCount"));
        assertEquals(0, responseSnapshot.stream()
                .filter(event -> event.type() == NetworkEventType.REQUEST).count());

        module.before.accept(BeforeRequestSent.fromJsonMap(base(
                "race", "/api/final", 1, 1_700_000_000_123L)));

        List<NetworkEvent> completeSnapshot = List.copyOf(sink.events);
        assertEquals(2, completeSnapshot.size());
        assertEquals(NetworkEventType.RESPONSE, completeSnapshot.get(0).type());
        assertEquals(NetworkEventType.REQUEST, completeSnapshot.get(1).type());
        assertEquals("race", completeSnapshot.get(1).request().id());
        assertEquals("1", completeSnapshot.get(1).attributes().get("redirectCount"));
        assertEquals(completeSnapshot.get(0).response().requestId(), completeSnapshot.get(1).request().id());
        assertEquals(completeSnapshot.get(0).attributes().get("redirectCount"),
                completeSnapshot.get(1).attributes().get("redirectCount"));
        assertEquals(1, completeSnapshot.stream()
                .filter(event -> event.type() == NetworkEventType.REQUEST).count());
        assertEquals(1, completeSnapshot.stream()
                .filter(event -> event.type() == NetworkEventType.RESPONSE).count());
    }

    @Test
    void multiHopRedirectsRetainResponseLocalCorrelationAcrossCallbackInterleaving() {
        FakeModule module = new FakeModule();
        RecordingSink sink = new RecordingSink();
        SeleniumBiDiNetworkCaptureSource.subscribe(module, NetworkDiagnosticsOptions.defaults(), sink);

        // The next hop can be announced before the prior hop's responseCompleted callback arrives.
        module.before.accept(BeforeRequestSent.fromJsonMap(base("chain", "/redirect-middle", 1, 120)));
        module.response.accept(ResponseDetails.fromJsonMap(response("chain", "/redirect-start", 0, 302, 125, 1, 2)));
        module.before.accept(BeforeRequestSent.fromJsonMap(base("chain", "/redirect-final", 2, 130)));
        module.response.accept(ResponseDetails.fromJsonMap(response("chain", "/redirect-middle", 1, 302, 135, 2, 3)));
        module.response.accept(ResponseDetails.fromJsonMap(response("chain", "/redirect-final", 2, 200, 140, 3, 4)));

        List<NetworkEvent> responses = sink.events.stream().filter(event -> event.response() != null).toList();
        assertEquals(List.of("0", "1", "2"), responses.stream()
                .map(event -> event.attributes().get("redirectCount")).toList());
        assertTrue(responses.stream().allMatch(event -> event.correlatedRequest() != null));
        assertTrue(responses.stream().allMatch(event ->
                event.response().requestId().equals(event.correlatedRequest().id())));
        assertEquals(List.of("/redirect-start", "/redirect-middle", "/redirect-final"), responses.stream()
                .map(event -> event.correlatedRequest().url()).toList());
    }

    @Test
    void chromeRedirectResponsesUseEventLocalRequestDataAcrossSupportedCallbackOrderings() {
        assertChromeRedirectOrdering("request-0", "request-1", "response-0", "response-1");
        assertChromeRedirectOrdering("request-0", "response-0", "request-1", "response-1");
        assertChromeRedirectOrdering("request-0", "request-1", "response-1", "response-0");
    }

    @Test
    void responseCorrelationDoesNotDependOnStandaloneRequestIdentityMultiplicityOrArrival() {
        FakeModule module = new FakeModule();
        RecordingSink sink = new RecordingSink();
        SeleniumBiDiNetworkCaptureSource.subscribe(module, NetworkDiagnosticsOptions.defaults(), sink);

        // Repeated URL, changed IDs between hops, duplicate callback and unrelated concurrent traffic.
        module.before.accept(BeforeRequestSent.fromJsonMap(base("hop-0", "/same", 0, 100)));
        module.before.accept(BeforeRequestSent.fromJsonMap(base("noise", "/irrelevant", 0, 101)));
        module.before.accept(BeforeRequestSent.fromJsonMap(base("hop-1", "/same", 1, 102)));
        module.before.accept(BeforeRequestSent.fromJsonMap(base("hop-1", "/same", 1, 102)));
        module.response.accept(ResponseDetails.fromJsonMap(response("hop-0", "/same", 0, 302, 110, 1, 2)));
        module.response.accept(ResponseDetails.fromJsonMap(response("noise", "/irrelevant", 0, 204, 111, 1, 2)));
        // The final completion can arrive after navigation/client-side work has already continued.
        module.response.accept(ResponseDetails.fromJsonMap(response("hop-1", "/same", 1, 200, 500, 2, 3)));

        NetworkEvent finalResponse = sink.events.stream()
                .filter(event -> event.response() != null && event.response().status() == 200)
                .findFirst().orElseThrow();
        assertNotNull(finalResponse.correlatedRequest());
        assertEquals("hop-1", finalResponse.correlatedRequest().id());
        assertEquals("/same", finalResponse.correlatedRequest().url());
        assertEquals("1", finalResponse.attributes().get("redirectCount"));
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

    private static void assertChromeRedirectOrdering(String... callbacks) {
        FakeModule module = new FakeModule();
        RecordingSink sink = new RecordingSink();
        SeleniumBiDiNetworkCaptureSource.subscribe(module, NetworkDiagnosticsOptions.defaults(), sink);

        for (String callback : callbacks) {
            switch (callback) {
                case "request-0" -> module.before.accept(BeforeRequestSent.fromJsonMap(
                        base("shared", "/redirect", 0, 100)));
                case "request-1" -> module.before.accept(BeforeRequestSent.fromJsonMap(
                        base("shared", "/redirect-middle", 1, 105)));
                case "response-0" -> module.response.accept(ResponseDetails.fromJsonMap(response(
                        "shared", "/redirect", "/redirect", 0, 302, 110, 1, 2)));
                case "response-1" -> module.response.accept(ResponseDetails.fromJsonMap(response(
                        "shared", "/api/final", "/redirect-middle", 1, 200, 115, 2, 3)));
                default -> throw new IllegalArgumentException("Unknown callback: " + callback);
            }
        }

        List<NetworkEvent> responses = sink.events.stream()
                .filter(event -> event.response() != null)
                .sorted(java.util.Comparator.comparing(event -> event.attributes().get("redirectCount")))
                .toList();
        assertEquals(List.of("0", "1"), responses.stream()
                .map(event -> event.attributes().get("redirectCount")).toList());
        assertEquals(List.of("/redirect", "/api/final"), responses.stream()
                .map(event -> event.correlatedRequest().url()).toList());
        assertEquals(List.of("/redirect", "/api/final"), responses.stream()
                .map(event -> event.response().url()).toList());
        assertTrue(responses.stream().allMatch(event -> "shared".equals(event.response().requestId())));
        assertTrue(responses.stream().allMatch(event ->
                event.response().requestId().equals(event.correlatedRequest().id())));
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
        return response(id, url, url, redirect, status, timestamp, requestTime, responseEnd);
    }

    private static Map<String, Object> response(String id, String requestUrl, String responseUrl,
                                                 long redirect, int status, long timestamp,
                                                 double requestTime, double responseEnd) {
        Map<String, Object> map = base(id, requestUrl, redirect, timestamp);
        map.put("request", request(id, requestUrl, requestTime, responseEnd));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("url", responseUrl);
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
        Map<String, Object> request = new LinkedHashMap<>(Map.of(
                "request", id, "url", url, "method", "POST", "headers", new ArrayList<>(),
                "cookies", new ArrayList<>(), "headersSize", 0L,
                "timings", timings(requestTime, responseEnd)));
        request.put("bodySize", null);
        request.put("destination", "");
        request.put("initiatorType", "");
        return request;
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
        private Throwable registrationFailure;
        private Throwable closeFailure;
        private Runnable closeAction = () -> {};
        private final AtomicInteger closes = new AtomicInteger();

        @Override public void onBeforeRequestSent(Consumer<BeforeRequestSent> consumer) {
            beforeRegistrations++;
            before = consumer;
        }
        @Override public void onResponseCompleted(Consumer<ResponseDetails> consumer) {
            responseRegistrations++;
            rethrow(registrationFailure);
            response = consumer;
        }
        @Override public void onFetchError(Consumer<FetchError> consumer) {
            errorRegistrations++;
            error = consumer;
        }
        @Override public void close() {
            closes.incrementAndGet();
            closeAction.run();
            rethrow(closeFailure);
        }
        private static void rethrow(Throwable failure) {
            if (failure instanceof RuntimeException runtime) throw runtime;
            if (failure instanceof Error error) throw error;
        }
    }
}
