package io.github.testlens.selenium.network;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.BiDi;
import org.openqa.selenium.bidi.Event;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.bidi.network.BaseParameters;
import org.openqa.selenium.bidi.network.BeforeRequestSent;
import org.openqa.selenium.bidi.network.BytesValue;
import org.openqa.selenium.bidi.network.FetchError;
import org.openqa.selenium.bidi.network.FetchTimingInfo;
import org.openqa.selenium.bidi.network.Header;
import org.openqa.selenium.bidi.network.RequestData;
import org.openqa.selenium.bidi.network.ResponseData;
import org.openqa.selenium.bidi.network.ResponseDetails;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** WebDriver BiDi network adapter using Selenium's public event and listener APIs. */
final class SeleniumBiDiNetworkCaptureSource implements NetworkCaptureSource {
    private final Module network;
    private final Map<String, String> diagnostics;
    private boolean closed;
    private boolean callbackFailed;
    private final Set<RequestHop> observedRedirectRequests = new LinkedHashSet<>();

    private SeleniumBiDiNetworkCaptureSource(Module network, Map<String, String> diagnostics) {
        this.network = network;
        this.diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }

    static NetworkCaptureSource open(WebDriver driver, NetworkDiagnosticsOptions options, NetworkCaptureSink sink) {
        BiDiDriverResolver.ResolvedBiDiDriver resolved;
        try { resolved = BiDiDriverResolver.resolve(driver); }
        catch (LinkageError failure) {
            throw SeleniumBiDiCompatibility.incompatible("Selenium BiDi driver initialization is incompatible", failure);
        }
        Module network;
        try {
            network = new OfficialModule(((HasBiDi) resolved.driver()).getBiDi(), resolved.ownedConnection());
        } catch (LinkageError failure) {
            closeOwned(resolved.ownedConnection(), failure);
            throw SeleniumBiDiCompatibility.incompatible("Selenium BiDi module creation is incompatible", failure);
        } catch (RuntimeException failure) {
            closeOwned(resolved.ownedConnection(), failure);
            throw captureFailure(BiDiFailureCategory.CAPTURE_START_FAILED,
                    "Unable to create Selenium's BiDi Network module", resolved.diagnostics(),
                    "CREATE_NETWORK_MODULE", failure);
        }
        try {
            return subscribe(network, options, sink, resolved.diagnostics());
        } catch (BiDiCaptureException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw captureFailure(BiDiFailureCategory.PROTOCOL_ERROR,
                    "BiDi connection was created but network listener registration failed",
                    resolved.diagnostics(), "REGISTER_LISTENERS", failure);
        }
    }

    static NetworkCaptureSource subscribe(Module network, NetworkDiagnosticsOptions options, NetworkCaptureSink sink) {
        return subscribe(network, options, sink, Map.of());
    }

    private static NetworkCaptureSource subscribe(Module network, NetworkDiagnosticsOptions options,
                                                   NetworkCaptureSink sink, Map<String, String> diagnostics) {
        try {
            SeleniumBiDiNetworkCaptureSource source = new SeleniumBiDiNetworkCaptureSource(network, diagnostics);
            network.onFailure(failure -> source.callback(() -> { throw failure; }, sink));
            network.onBeforeRequestSent(event -> source.callback(() ->
                    source.observeRequest(event, event.getRequest(), "beforeRequestSent", options, sink, true), sink));
            network.onResponseStarted(event -> source.callback(() ->
                    source.observeRequest(event, event.getRequest(), "responseStarted", options, sink, false), sink));
            network.onResponseCompleted(event -> source.callback(() -> {
                source.observeRequest(event, event.getRequest(), "responseCompleted", options, sink, false);
                responseCompleted(event, options, sink);
            }, sink));
            network.onFetchError(event -> source.callback(() -> fetchError(event, options, sink), sink));
            return source;
        } catch (RuntimeException | LinkageError failure) {
            closeAfterFailure(network, failure);
            if (failure instanceof LinkageError) throw SeleniumBiDiCompatibility.incompatible("Selenium BiDi registration incompatible", failure);
            throw (RuntimeException) failure;
        }
    }

    private static BiDiCaptureException captureFailure(BiDiFailureCategory category,
                                                        String message,
                                                        Map<String, String> sourceDiagnostics,
                                                        String stage,
                                                        Throwable cause) {
        Map<String, String> diagnostics = new LinkedHashMap<>(sourceDiagnostics);
        diagnostics.put("failureCategory", category.name());
        diagnostics.put("initializationStage", stage);
        return new BiDiCaptureException(category, message, diagnostics, cause);
    }

    private static void closeOwned(BiDi connection, Throwable failure) {
        if (connection == null) return;
        try {
            connection.close();
        } catch (RuntimeException | LinkageError closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    private static void closeAfterFailure(Module network, Throwable failure) {
        if (network == null) return;
        try {
            network.close();
        } catch (RuntimeException | LinkageError closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    @Override
    public Map<String, String> diagnostics() {
        return diagnostics;
    }

    @Override public void verifyHealth() { network.verifyHealth(); }

    private void observeRequest(BaseParameters event, RequestData request, String observationSource,
                                NetworkDiagnosticsOptions options, NetworkCaptureSink sink, boolean before) {
        // Only redirect response events can supply a missing standalone request observation.
        // Their timestamp identifies when this metadata was observed, not a reconstructed request start.
        if (!before && (event.getRedirectCount() <= 0 || request == null || safe(request.getRequestId()).isBlank())) return;
        String url = request == null ? "" : safe(request.getUrl());
        if (options.isIgnored(url)) {
            if (before) sink.ignored();
            return;
        }
        if (event.getRedirectCount() > 0 && request != null && !safe(request.getRequestId()).isBlank()) {
            RequestHop key = new RequestHop(request.getRequestId(), event.getRedirectCount());
            if (observedRedirectRequests.contains(key)) return;
            if (observedRedirectRequests.size() < options.maxCapturedEvents()) observedRedirectRequests.add(key);
            else if (!before) return;
        }
        Map<String, String> attributes = baseAttributes(event);
        attributes.put("requestObservationSource", observationSource);
        attributes.put("resourceTypeAvailable", "false");
        if (request != null) sizeAttribute(attributes, "requestHeadersSize", request, "getHeadersSize");
        NetworkRequest mapped = new NetworkRequest(
                request == null ? "" : request.getRequestId(),
                request == null ? "" : request.getMethod(),
                url,
                "",
                timestamp(event.getTimestamp()),
                headers(request == null ? List.of() : request.getHeaders(), options));
        sink.recorded(NetworkEvent.request(mapped, timestamp(event.getTimestamp()), attributes));
    }

    private static void responseCompleted(ResponseDetails event, NetworkDiagnosticsOptions options, NetworkCaptureSink sink) {
        RequestData request = event.getRequest();
        ResponseData response = event.getResponseData();
        String url = responseUrl(request, response);
        if (options.isIgnored(url)) {
            sink.ignored();
            return;
        }
        DurationValue duration = duration(request == null ? null : request.getTimings());
        Map<String, String> attributes = baseAttributes(event);
        attributes.put("durationAvailable", String.valueOf(duration.available));
        if (response != null) {
            attributes.put("fromCache", String.valueOf(response.isFromCache()));
            attributes.put("protocol", safe(response.getProtocol()));
            sizeAttribute(attributes, "bytesReceived", response, "getBytesReceived");
            sizeAttribute(attributes, "headersSize", response, "getHeadersSize");
            sizeAttribute(attributes, "bodySize", response, "getBodySize");
        }
        NetworkResponse mapped = new NetworkResponse(
                request == null ? "" : request.getRequestId(),
                url,
                response == null ? 0 : response.getStatus(),
                response == null ? "" : response.getStatusText(),
                response == null ? "" : response.getMimeType(),
                duration.value,
                timestamp(event.getTimestamp()),
                headers(response == null ? List.of() : response.getHeaders(), options));
        NetworkRequest correlatedRequest = correlatedRequest(request, event.getTimestamp(), options);
        sink.recorded(NetworkEvent.response(mapped, correlatedRequest, timestamp(event.getTimestamp()), attributes));
    }

    private static void sizeAttribute(Map<String, String> attributes, String name, Object value, String getter) {
        Object size = SeleniumBiDiCompatibility.size(value, getter);
        if (size instanceof Number number && number.longValue() >= 0) attributes.put(name, String.valueOf(size));
    }

    private synchronized void callback(Runnable mapping, NetworkCaptureSink sink) {
        if (closed || callbackFailed) return;
        try { mapping.run(); }
        catch (RuntimeException | LinkageError failure) {
            callbackFailed = true;
            BiDiCaptureException reported = failure instanceof BiDiCaptureException bidi ? bidi
                    : captureFailure(failure instanceof LinkageError ? BiDiFailureCategory.BINARY_INCOMPATIBILITY
                            : BiDiFailureCategory.CAPTURE_CALLBACK_FAILED,
                            "WebDriver BiDi event processing failed", diagnostics, "PROCESS_EVENT", failure);
            sink.failed(reported);
        }
    }

    private static String responseUrl(RequestData request, ResponseData response) {
        String requestUrl = request == null ? "" : safe(request.getUrl());
        if (!requestUrl.isBlank()) {
            // RequestData is local to this responseCompleted event and identifies the redirect hop.
            // Chromium can expose the preceding hop in ResponseData.url while the event-local request
            // already contains the URL whose response completed.
            return requestUrl;
        }
        return response == null ? "" : safe(response.getUrl());
    }

    private static NetworkRequest correlatedRequest(RequestData request, long timestamp,
                                                    NetworkDiagnosticsOptions options) {
        if (request == null || safe(request.getRequestId()).isBlank()) return null;
        return new NetworkRequest(request.getRequestId(), safe(request.getMethod()), safe(request.getUrl()), "",
                timestamp(timestamp), headers(request.getHeaders(), options));
    }

    private static void fetchError(FetchError event, NetworkDiagnosticsOptions options, NetworkCaptureSink sink) {
        RequestData request = event.getRequest();
        String url = request == null ? "" : safe(request.getUrl());
        if (options.isIgnored(url)) {
            sink.ignored();
            return;
        }
        NetworkFailure mapped = new NetworkFailure(
                request == null ? "" : request.getRequestId(),
                url,
                normalizeErrorText(event.getErrorText()),
                "FETCH_ERROR",
                timestamp(event.getTimestamp()));
        sink.recorded(NetworkEvent.failed(mapped, timestamp(event.getTimestamp()), baseAttributes(event)));
    }

    private static Map<String, String> baseAttributes(BaseParameters event) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("browsingContext", safe(event.getBrowsingContextId()));
        attributes.put("navigationId", safe(event.getNavigationId()));
        attributes.put("redirectCount", String.valueOf(event.getRedirectCount()));
        attributes.put("blocked", String.valueOf(event.isBlocked()));
        attributes.put("timestampSource", event.getTimestamp() > 0 ? "BIDI" : "UNAVAILABLE");
        return attributes;
    }

    private static Map<String, String> headers(List<Header> input, NetworkDiagnosticsOptions options) {
        if (!options.includeHeaders() || input == null || input.isEmpty()) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        for (Header header : input) {
            if (header == null || header.getName() == null || header.getValue() == null) continue;
            String name = header.getName();
            String value = headerValue(header.getValue());
            String existingKey = result.keySet().stream()
                    .filter(key -> key.equalsIgnoreCase(name)).findFirst().orElse(name);
            if (isSensitive(existingKey) && options.maskSensitiveHeaders()) {
                result.put(existingKey, "***");
            } else {
                result.merge(existingKey, value, (left, right) -> left + "\n" + right);
            }
        }
        return result;
    }

    private static String headerValue(BytesValue value) {
        String raw = safe(value.getValue());
        return value.getType() == BytesValue.Type.BASE64 ? "base64:" + raw : raw;
    }

    static boolean isSensitive(String name) {
        String lower = safe(name).toLowerCase(Locale.ROOT);
        return lower.equals("authorization")
                || lower.equals("proxy-authorization")
                || lower.equals("cookie")
                || lower.equals("set-cookie")
                || lower.equals("x-api-key")
                || lower.equals("x-auth-token");
    }

    private static DurationValue duration(FetchTimingInfo timings) {
        if (timings == null) return new DurationValue(Duration.ZERO, false);
        double start = timings.getRequestTime();
        double end = timings.getResponseEnd();
        if (!Double.isFinite(start) || !Double.isFinite(end) || start < 0 || end < start) {
            return new DurationValue(Duration.ZERO, false);
        }
        double millis = end - start;
        if (millis == 0) return new DurationValue(Duration.ZERO, false);
        return new DurationValue(Duration.ofNanos(Math.max(0L, Math.round(millis * 1_000_000d))), true);
    }

    private static Instant timestamp(long epochMillis) {
        return epochMillis > 0 ? Instant.ofEpochMilli(epochMillis) : Instant.EPOCH;
    }

    private static String normalizeErrorText(String text) {
        String value = safe(text);
        return value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")
                ? value.substring(1, value.length() - 1) : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public void close() {
        synchronized (this) {
            if (closed) return;
            closed = true;
            observedRedirectRequests.clear();
        }
        network.close();
    }

    private record DurationValue(Duration value, boolean available) {}
    private record RequestHop(String requestId, long redirectCount) {}

    interface Module extends AutoCloseable {
        default void verifyHealth() {}
        default void onFailure(Consumer<RuntimeException> consumer) {}
        void onBeforeRequestSent(Consumer<BeforeRequestSent> consumer);
        default void onResponseStarted(Consumer<ResponseDetails> consumer) {}
        void onResponseCompleted(Consumer<ResponseDetails> consumer);
        void onFetchError(Consumer<FetchError> consumer);
        @Override void close();
    }

    static final class OfficialModule implements Module {
        private final BiDi connection;
        private final BiDi ownedConnection;
        private final List<Object> listeners = new ArrayList<>();
        private Consumer<RuntimeException> failureConsumer = failure -> {};
        private boolean closed;

        OfficialModule(BiDi connection, BiDi ownedConnection) {
            this.connection = connection;
            this.ownedConnection = ownedConnection;
        }
        @Override public synchronized void onFailure(Consumer<RuntimeException> consumer) {
            failureConsumer = consumer;
        }
        @Override public void verifyHealth() {
            try { connection.getBidiSessionStatus(); }
            catch (LinkageError failure) {
                throw SeleniumBiDiCompatibility.incompatible("Selenium BiDi health verification is incompatible", failure);
            } catch (RuntimeException failure) {
                BiDiFailureCategory category = failure instanceof org.openqa.selenium.TimeoutException
                        || failure instanceof org.openqa.selenium.NoSuchSessionException
                        || failure instanceof java.io.UncheckedIOException
                        ? BiDiFailureCategory.TRANSPORT_ERROR : BiDiFailureCategory.PROTOCOL_ERROR;
                throw captureFailure(category, "Unable to verify WebDriver BiDi capture health",
                        Map.of(), "VERIFY_CONNECTION", failure);
            }
        }
        private synchronized <T> void listen(String name, java.util.function.Function<Map<String, Object>, T> mapper,
                                             Consumer<T> consumer) {
            if (closed) throw new IllegalStateException("BiDi network capture is closed");
            Event<T> event = new Event<>(name, payload -> {
                try { return mapper.apply(SeleniumBiDiCompatibility.normalizeSizes(payload)); }
                catch (RuntimeException | LinkageError failure) {
                    RuntimeException reported = failure instanceof RuntimeException runtime ? runtime
                            : SeleniumBiDiCompatibility.incompatible("Selenium BiDi event decoding is incompatible", failure);
                    failureConsumer.accept(reported);
                    return null;
                }
            });
            listeners.add(SeleniumBiDiCompatibility.addListener(connection, event, value -> {
                if (value != null) consumer.accept(value);
            }));
        }
        @Override public void onBeforeRequestSent(Consumer<BeforeRequestSent> consumer) {
            listen("network.beforeRequestSent", BeforeRequestSent::fromJsonMap, consumer);
        }
        @Override public void onResponseStarted(Consumer<ResponseDetails> consumer) {
            listen("network.responseStarted", ResponseDetails::fromJsonMap, consumer);
        }
        @Override public void onResponseCompleted(Consumer<ResponseDetails> consumer) {
            listen("network.responseCompleted", ResponseDetails::fromJsonMap, consumer);
        }
        @Override public void onFetchError(Consumer<FetchError> consumer) {
            listen("network.fetchError", FetchError::fromJsonMap, consumer);
        }
        @Override public synchronized void close() {
            if (closed) return;
            closed = true;
            RuntimeException failure = null;
            for (Object listener : listeners) {
                try { SeleniumBiDiCompatibility.removeListener(connection, listener); }
                catch (RuntimeException closeFailure) {
                    if (failure == null) failure = closeFailure;
                    else failure.addSuppressed(closeFailure);
                }
            }
            listeners.clear();
            if (ownedConnection != null) {
                try { ownedConnection.close(); }
                catch (RuntimeException | LinkageError closeFailure) {
                    RuntimeException reported = closeFailure instanceof RuntimeException runtime ? runtime
                            : SeleniumBiDiCompatibility.incompatible("Selenium BiDi close incompatible", closeFailure);
                    if (failure == null) failure = reported;
                    else failure.addSuppressed(reported);
                }
            }
            if (failure != null) throw failure;
        }
    }
}
