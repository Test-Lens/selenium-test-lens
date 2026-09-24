package io.github.testlens.selenium.network;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.BiDi;
import org.openqa.selenium.bidi.module.Network;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Official Selenium 4.39 WebDriver BiDi network adapter. */
final class SeleniumBiDiNetworkCaptureSource implements NetworkCaptureSource {
    private final Module network;
    private final Map<String, String> diagnostics;
    private boolean closed;

    private SeleniumBiDiNetworkCaptureSource(Module network, Map<String, String> diagnostics) {
        this.network = network;
        this.diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }

    static NetworkCaptureSource open(WebDriver driver, NetworkDiagnosticsOptions options, NetworkCaptureSink sink) {
        BiDiDriverResolver.ResolvedBiDiDriver resolved = BiDiDriverResolver.resolve(driver);
        Module network;
        try {
            network = new OfficialModule(new Network(resolved.driver()), resolved.ownedConnection());
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
            NetworkCaptureSource source = new SeleniumBiDiNetworkCaptureSource(network, diagnostics);
            network.onBeforeRequestSent(event -> beforeRequest(event, options, sink));
            network.onResponseCompleted(event -> responseCompleted(event, options, sink));
            network.onFetchError(event -> fetchError(event, options, sink));
            return source;
        } catch (RuntimeException failure) {
            closeAfterFailure(network, failure);
            throw failure;
        }
    }

    private static BiDiCaptureException captureFailure(BiDiFailureCategory category,
                                                        String message,
                                                        Map<String, String> sourceDiagnostics,
                                                        String stage,
                                                        RuntimeException cause) {
        Map<String, String> diagnostics = new LinkedHashMap<>(sourceDiagnostics);
        diagnostics.put("failureCategory", category.name());
        diagnostics.put("initializationStage", stage);
        return new BiDiCaptureException(category, message, diagnostics, cause);
    }

    private static void closeOwned(BiDi connection, RuntimeException failure) {
        if (connection == null) return;
        try {
            connection.close();
        } catch (RuntimeException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    private static void closeAfterFailure(Module network, RuntimeException failure) {
        if (network == null) return;
        try {
            network.close();
        } catch (RuntimeException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    @Override
    public Map<String, String> diagnostics() {
        return diagnostics;
    }

    private static void beforeRequest(BeforeRequestSent event, NetworkDiagnosticsOptions options, NetworkCaptureSink sink) {
        RequestData request = event.getRequest();
        String url = request == null ? "" : safe(request.getUrl());
        if (options.isIgnored(url)) {
            sink.ignored();
            return;
        }
        Map<String, String> attributes = baseAttributes(event);
        attributes.put("resourceTypeAvailable", "false");
        attributes.put("requestHeadersSize", request == null ? "0" : String.valueOf(request.getHeadersSize()));
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
        String url = response == null ? (request == null ? "" : safe(request.getUrl())) : safe(response.getUrl());
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
            attributes.put("bytesReceived", String.valueOf(response.getBytesReceived()));
            attributes.put("headersSize", String.valueOf(response.getHeadersSize()));
            attributes.put("bodySize", String.valueOf(response.getBodySize()));
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
    public synchronized void close() {
        if (closed) return;
        closed = true;
        network.close();
    }

    private record DurationValue(Duration value, boolean available) {}

    interface Module extends AutoCloseable {
        void onBeforeRequestSent(Consumer<BeforeRequestSent> consumer);
        void onResponseCompleted(Consumer<ResponseDetails> consumer);
        void onFetchError(Consumer<FetchError> consumer);
        @Override void close();
    }

    private record OfficialModule(Network delegate, BiDi ownedConnection) implements Module {
        @Override public void onBeforeRequestSent(Consumer<BeforeRequestSent> consumer) {
            delegate.onBeforeRequestSent(consumer);
        }
        @Override public void onResponseCompleted(Consumer<ResponseDetails> consumer) {
            delegate.onResponseCompleted(consumer);
        }
        @Override public void onFetchError(Consumer<FetchError> consumer) {
            delegate.onFetchError(consumer);
        }
        @Override public void close() {
            RuntimeException failure = null;
            try {
                delegate.close();
            } catch (RuntimeException closeFailure) {
                failure = closeFailure;
            }
            if (ownedConnection != null) {
                try {
                    ownedConnection.close();
                } catch (RuntimeException closeFailure) {
                    if (failure == null) failure = closeFailure;
                    else failure.addSuppressed(closeFailure);
                }
            }
            if (failure != null) throw failure;
        }
    }
}
