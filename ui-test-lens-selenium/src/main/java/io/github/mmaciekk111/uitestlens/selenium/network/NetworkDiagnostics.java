package io.github.mmaciekk111.uitestlens.selenium.network;

import io.github.mmaciekk111.uitestlens.core.OverlayLogger;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensEventType;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogLevel;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensStatus;
import io.github.mmaciekk111.uitestlens.core.trace.TraceArtifact;
import io.github.mmaciekk111.uitestlens.core.trace.UiTestLensSession;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NetworkDiagnostics {
    private final WebDriver driver;
    private final OverlayLogger logger;
    private final List<NetworkEvent> events = new ArrayList<>();
    private NetworkDiagnosticsOptions options = NetworkDiagnosticsOptions.defaults();
    private NetworkDiagnosticsStatus status = NetworkDiagnosticsStatus.STOPPED;
    private boolean started;
    private int ignoredEvents;

    public NetworkDiagnostics(WebDriver driver) {
        this(driver, OverlayLogger.noop());
    }

    public NetworkDiagnostics(WebDriver driver, OverlayLogger logger) {
        if (driver == null) {
            throw new IllegalArgumentException("driver must not be null");
        }
        this.driver = driver;
        this.logger = logger == null ? OverlayLogger.noop() : logger;
    }

    public synchronized NetworkDiagnostics start(NetworkDiagnosticsOptions options) {
        this.options = options == null ? NetworkDiagnosticsOptions.defaults() : options;
        this.started = this.options.captureMode() != NetworkCaptureMode.OFF;
        if (this.options.captureMode() == NetworkCaptureMode.OFF) {
            this.status = NetworkDiagnosticsStatus.STOPPED;
            addInternal(NetworkEvent.info("Network diagnostics disabled"));
        } else if (this.options.captureMode() == NetworkCaptureMode.PERFORMANCE_LOGS
                || this.options.captureMode() == NetworkCaptureMode.BIDI) {
            this.status = NetworkDiagnosticsStatus.UNSUPPORTED;
            this.started = true;
            addInternal(NetworkEvent.warning("Requested network capture mode is not implemented without optional browser-specific dependencies; using manual collector"));
        } else {
            this.status = NetworkDiagnosticsStatus.STARTED;
            addInternal(NetworkEvent.info("Network diagnostics started in " + effectiveMode().name() + " mode"));
        }
        emit(UiTestLensEventType.NETWORK_DIAGNOSTICS_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Network diagnostics started", null);
        return this;
    }

    public synchronized NetworkDiagnostics stop() {
        this.started = false;
        this.status = NetworkDiagnosticsStatus.STOPPED;
        emit(UiTestLensEventType.NETWORK_DIAGNOSTICS_STOPPED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                "Network diagnostics stopped", null);
        return this;
    }

    public synchronized boolean isStarted() {
        return started;
    }

    public synchronized List<NetworkEvent> events() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public synchronized NetworkSummary summary() {
        return NetworkSummary.from(events, ignoredEvents, options.failedStatusThreshold(), status);
    }

    public synchronized NetworkEvent addManualEvent(NetworkEvent event) {
        if (event == null) {
            return null;
        }
        if (options.isIgnored(event.url())) {
            ignoredEvents++;
            return event;
        }
        NetworkEvent sanitized = sanitize(event);
        events.add(sanitized);
        emitRecorded(sanitized);
        return sanitized;
    }

    public NetworkDiagnosticsResult assertNoFailedRequests() {
        Instant startedAt = Instant.now();
        NetworkSummary summary = summary();
        if (summary.hasFailures()) {
            NetworkDiagnosticsResult result = NetworkDiagnosticsResult.of(
                    NetworkDiagnosticsStatus.ASSERTION_FAILED,
                    summary.failureSummary(),
                    summary,
                    elapsedSince(startedAt)
            );
            emit(UiTestLensEventType.NETWORK_ASSERTION_FAILED, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR,
                    result.message(), null);
            throw new NetworkAssertionError(result.message(), summary);
        }
        NetworkDiagnosticsResult result = NetworkDiagnosticsResult.of(
                NetworkDiagnosticsStatus.ASSERTION_PASSED,
                "No failed network requests",
                summary,
                elapsedSince(startedAt)
        );
        emit(UiTestLensEventType.NETWORK_ASSERTION_PASSED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                result.message(), null);
        return result;
    }

    public NetworkDiagnosticsResult attachToSession(UiTestLensSession session) {
        if (session == null) {
            return NetworkDiagnosticsResult.failed("No UiTestLensSession attached", summary(), null, Duration.ZERO);
        }
        session.addEvent(io.github.mmaciekk111.uitestlens.core.trace.TraceEvent.builder(
                        io.github.mmaciekk111.uitestlens.core.trace.TraceEventType.CUSTOM,
                        io.github.mmaciekk111.uitestlens.core.trace.TraceStatus.INFO,
                        "Network diagnostics summary")
                .message(summary().failureSummary())
                .attribute("totalRequests", String.valueOf(summary().totalRequests()))
                .attribute("totalResponses", String.valueOf(summary().totalResponses()))
                .attribute("failedResponses", String.valueOf(summary().failedResponses()))
                .attribute("failedRequests", String.valueOf(summary().failedRequests()))
                .build());
        return NetworkDiagnosticsResult.of(NetworkDiagnosticsStatus.ATTACHED, "Network diagnostics summary attached", summary(), Duration.ZERO);
    }

    public NetworkDiagnosticsResult attachToSession(UiTestLensSession session, Path outputPath) {
        Instant startedAt = Instant.now();
        if (session == null) {
            return NetworkDiagnosticsResult.failed("No UiTestLensSession attached", summary(), null, elapsedSince(startedAt));
        }
        if (outputPath == null) {
            return NetworkDiagnosticsResult.failed("Network log output path must not be null", summary(), null, elapsedSince(startedAt));
        }
        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputPath, exportJson());
            TraceArtifact artifact = session.attachArtifact(TraceArtifact.networkLog("Network log", outputPath)
                    .withMetadata("totalRequests", String.valueOf(summary().totalRequests()))
                    .withMetadata("failedResponses", String.valueOf(summary().failedResponses()))
                    .withMetadata("failedRequests", String.valueOf(summary().failedRequests())));
            emit(UiTestLensEventType.NETWORK_LOG_ATTACHED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                    "Network log attached", artifact.path());
            return NetworkDiagnosticsResult.of(NetworkDiagnosticsStatus.ATTACHED, "Network log attached", summary(), elapsedSince(startedAt));
        } catch (IOException | RuntimeException e) {
            return NetworkDiagnosticsResult.failed("Failed to attach network log: " + messageFor(e), summary(), e, elapsedSince(startedAt));
        }
    }

    public synchronized String exportJson() {
        return new NetworkLogExporter().export(events);
    }

    private void addInternal(NetworkEvent event) {
        events.add(event);
    }

    private NetworkCaptureMode effectiveMode() {
        if (options.captureMode() == NetworkCaptureMode.AUTO) {
            return NetworkCaptureMode.MANUAL;
        }
        return options.captureMode();
    }

    private NetworkEvent sanitize(NetworkEvent event) {
        if (!options.includeHeaders()) {
            return removeHeaders(event);
        }
        if (options.maskSensitiveHeaders()) {
            return maskHeaders(event);
        }
        return event;
    }

    private NetworkEvent removeHeaders(NetworkEvent event) {
        if (event.request() != null) {
            NetworkRequest request = event.request();
            return NetworkEvent.request(new NetworkRequest(request.id(), request.method(), request.url(),
                    request.resourceType(), request.timestamp(), Map.of()));
        }
        if (event.response() != null) {
            NetworkResponse response = event.response();
            return NetworkEvent.response(new NetworkResponse(response.requestId(), response.url(), response.status(),
                    response.statusText(), response.mimeType(), response.duration(), response.timestamp(), Map.of()));
        }
        return event;
    }

    private NetworkEvent maskHeaders(NetworkEvent event) {
        if (event.request() != null) {
            NetworkRequest request = event.request();
            return NetworkEvent.request(new NetworkRequest(request.id(), request.method(), request.url(),
                    request.resourceType(), request.timestamp(), mask(request.headers())));
        }
        if (event.response() != null) {
            NetworkResponse response = event.response();
            return NetworkEvent.response(new NetworkResponse(response.requestId(), response.url(), response.status(),
                    response.statusText(), response.mimeType(), response.duration(), response.timestamp(), mask(response.headers())));
        }
        return event;
    }

    private Map<String, String> mask(Map<String, String> headers) {
        Map<String, String> masked = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : (headers == null ? Map.<String, String>of() : headers).entrySet()) {
            if (isSensitiveHeader(entry.getKey())) {
                masked.put(entry.getKey(), "***");
            } else {
                masked.put(entry.getKey(), entry.getValue());
            }
        }
        return masked;
    }

    private boolean isSensitiveHeader(String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return lower.equals("authorization")
                || lower.equals("cookie")
                || lower.equals("set-cookie")
                || lower.equals("x-api-key")
                || lower.equals("x-auth-token");
    }

    private void emitRecorded(NetworkEvent event) {
        UiTestLensEventType eventType = switch (event.type()) {
            case REQUEST -> UiTestLensEventType.NETWORK_REQUEST_RECORDED;
            case RESPONSE -> UiTestLensEventType.NETWORK_RESPONSE_RECORDED;
            case FAILED -> UiTestLensEventType.NETWORK_FAILURE_RECORDED;
            default -> UiTestLensEventType.NETWORK_DIAGNOSTICS_STARTED;
        };
        emit(eventType, UiTestLensStatus.INFO, UiTestLensLogLevel.INFO, event.message(), event.url());
    }

    private void emit(UiTestLensEventType eventType, UiTestLensStatus status, UiTestLensLogLevel level, String message, String url) {
        try {
            logger.emit(UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(eventType)
                    .status(status)
                    .message(message)
                    .action("network.diagnostics")
                    .metadata("mode", options.captureMode().name())
                    .metadata("url", safeUrlPreview(url))
                    .metadata("totalRequests", String.valueOf(summary().totalRequests()))
                    .metadata("failedResponses", String.valueOf(summary().failedResponses()))
                    .metadata("failedRequests", String.valueOf(summary().failedRequests()))
                    .build());
        } catch (RuntimeException ignored) {}
    }

    private static String safeUrlPreview(String url) {
        String safe = url == null ? "" : url;
        int query = safe.indexOf('?');
        String withoutQuery = query >= 0 ? safe.substring(0, query) : safe;
        return withoutQuery.length() <= 180 ? withoutQuery : withoutQuery.substring(0, 177) + "...";
    }

    private static Duration elapsedSince(Instant started) {
        return Duration.between(started, Instant.now());
    }

    private static String messageFor(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
