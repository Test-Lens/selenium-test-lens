package io.github.testlens.selenium.network;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;
import io.github.testlens.core.trace.TraceArtifact;
import io.github.testlens.core.trace.UiTestLensSession;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/** Passive MANUAL or WebDriver BiDi network diagnostics for one WebDriver session. */
public final class NetworkDiagnostics {
    private final WebDriver driver;
    private final OverlayLogger logger;
    private final NetworkCaptureSourceFactory captureFactory;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition eventArrived = lock.newCondition();
    private final List<NetworkEvent> events = new ArrayList<>();
    private NetworkDiagnosticsOptions options = NetworkDiagnosticsOptions.defaults();
    private NetworkDiagnosticsStatus status = NetworkDiagnosticsStatus.STOPPED;
    private NetworkCaptureMode activeMode;
    private NetworkCaptureSource captureSource;
    private Throwable startFailure;
    private String statusMessage = "Network diagnostics are not started";
    private boolean started;
    private boolean limitWarningEmitted;
    private long generation;
    private int capturedEvents;
    private int ignoredEvents;
    private int droppedEvents;

    public NetworkDiagnostics(WebDriver driver) {
        this(driver, OverlayLogger.noop());
    }

    public NetworkDiagnostics(WebDriver driver, OverlayLogger logger) {
        this(driver, logger, SeleniumBiDiNetworkCaptureSource::open);
    }

    NetworkDiagnostics(WebDriver driver, OverlayLogger logger, NetworkCaptureSourceFactory captureFactory) {
        if (driver == null) throw new IllegalArgumentException("driver must not be null");
        this.driver = driver;
        this.logger = logger == null ? OverlayLogger.noop() : logger;
        this.captureFactory = Objects.requireNonNull(captureFactory, "captureFactory");
    }

    /** Starts the requested capture mode without falling back to another source. */
    public NetworkDiagnostics start(NetworkDiagnosticsOptions requestedOptions) {
        stop();
        NetworkDiagnosticsOptions effective = requestedOptions == null
                ? NetworkDiagnosticsOptions.defaults() : requestedOptions;
        NetworkCaptureMode requestedMode = effective.captureMode();
        long token;
        lock.lock();
        try {
            options = effective;
            generation++;
            token = generation;
            started = false;
            activeMode = null;
            status = NetworkDiagnosticsStatus.STOPPED;
            startFailure = null;
            limitWarningEmitted = false;
            statusMessage = "Network diagnostics are not started";
            if (requestedMode == NetworkCaptureMode.OFF) {
                statusMessage = "Network diagnostics are disabled (OFF)";
                addInternal(NetworkEvent.info(statusMessage));
            } else if (requestedMode == NetworkCaptureMode.MANUAL) {
                activate(token, NetworkCaptureMode.MANUAL, null);
                statusMessage = "Network diagnostics started in MANUAL mode";
                addInternal(NetworkEvent.info(statusMessage));
            } else if (requestedMode == NetworkCaptureMode.PERFORMANCE_LOGS) {
                markUnsupported("Network capture mode PERFORMANCE_LOGS is not implemented");
            }
            eventArrived.signalAll();
        } finally {
            lock.unlock();
        }
        if (requestedMode == NetworkCaptureMode.OFF) {
            emit(UiTestLensEventType.NETWORK_DIAGNOSTICS_STOPPED, UiTestLensStatus.SKIPPED,
                    UiTestLensLogLevel.INFO, statusMessage(), null, null);
            return this;
        }
        if (requestedMode == NetworkCaptureMode.MANUAL) {
            emitStarted();
            return this;
        }
        if (requestedMode == NetworkCaptureMode.PERFORMANCE_LOGS) {
            emitUnsupported();
            return this;
        }

        NetworkCaptureSource opened = null;
        try {
            opened = captureFactory.open(driver, effective, new NetworkCaptureSink() {
                @Override
                public void recorded(NetworkEvent event) {
                    recordCaptured(token, event);
                }

                @Override
                public void ignored() {
                    recordIgnored(token);
                }
            });
            boolean accepted;
            lock.lock();
            try {
                accepted = generation == token && !started && captureSource == null;
                if (accepted) {
                    activate(token, NetworkCaptureMode.BIDI, opened);
                    statusMessage = "Network diagnostics started with WebDriver BiDi"
                            + (requestedMode == NetworkCaptureMode.AUTO ? " (AUTO selected BIDI)" : "");
                    addInternal(NetworkEvent.info(statusMessage));
                    opened = null;
                }
                eventArrived.signalAll();
            } finally {
                lock.unlock();
            }
            if (!accepted) {
                closeQuietly(opened, null);
                opened = null;
            } else {
                emitStarted();
            }
        } catch (NetworkCaptureUnsupportedException unsupported) {
            markStartUnsupported(token, requestedMode, unsupported);
        } catch (RuntimeException failure) {
            markStartFailed(token, requestedMode, failure);
        } finally {
            closeQuietly(opened, null);
        }
        return this;
    }

    /** Stops active capture, preserves collected events, and never closes the WebDriver. */
    public NetworkDiagnostics stop() {
        NetworkCaptureSource source;
        lock.lock();
        try {
            if (!started && captureSource == null) return this;
            generation++;
            started = false;
            activeMode = null;
            status = NetworkDiagnosticsStatus.STOPPED;
            statusMessage = "Network diagnostics stopped";
            source = captureSource;
            captureSource = null;
            eventArrived.signalAll();
        } finally {
            lock.unlock();
        }
        RuntimeException closeFailure = closeQuietly(source, null);
        if (closeFailure != null) {
            lock.lock();
            try {
                addInternal(NetworkEvent.warning("Network capture stop failed: " + messageFor(closeFailure)));
            } finally {
                lock.unlock();
            }
            emit(UiTestLensEventType.NETWORK_DIAGNOSTICS_STOPPED, UiTestLensStatus.FAILED,
                    UiTestLensLogLevel.WARN, "Network diagnostics stopped with a cleanup failure", null, closeFailure);
        } else {
            emit(UiTestLensEventType.NETWORK_DIAGNOSTICS_STOPPED, UiTestLensStatus.PASSED,
                    UiTestLensLogLevel.INFO, "Network diagnostics stopped", null, null);
        }
        return this;
    }

    public boolean isStarted() {
        lock.lock();
        try { return started; } finally { lock.unlock(); }
    }

    public List<NetworkEvent> events() {
        lock.lock();
        try { return Collections.unmodifiableList(new ArrayList<>(events)); } finally { lock.unlock(); }
    }

    public NetworkSummary summary() {
        lock.lock();
        try {
            return NetworkSummary.from(events, ignoredEvents, droppedEvents,
                    options.failedStatusThreshold(), status);
        } finally { lock.unlock(); }
    }

    /** Returns the requested capture mode without starting or changing capture. */
    public NetworkCaptureMode captureMode() {
        lock.lock();
        try { return options.captureMode(); } finally { lock.unlock(); }
    }

    /** Returns the actual active source, which can differ from requested {@code AUTO}. */
    public Optional<NetworkCaptureMode> activeCaptureMode() {
        lock.lock();
        try { return started ? Optional.ofNullable(activeMode) : Optional.empty(); } finally { lock.unlock(); }
    }

    /** Adds a caller-supplied event while MANUAL capture is active. */
    public NetworkEvent addManualEvent(NetworkEvent event) {
        if (event == null) return null;
        NetworkEvent recorded = null;
        boolean warnLimit = false;
        lock.lock();
        try {
            if (!started || activeMode != NetworkCaptureMode.MANUAL) return event;
            if (options.isIgnored(event.url())) {
                ignoredEvents++;
                return event;
            }
            NetworkEvent sanitized = sanitize(event);
            AddResult result = addCaptured(sanitized);
            recorded = result == AddResult.ADDED ? sanitized : null;
            warnLimit = result == AddResult.DROPPED_WITH_WARNING;
        } finally { lock.unlock(); }
        if (recorded != null) emitRecorded(recorded);
        if (warnLimit) emitLimitWarning();
        return recorded == null ? event : recorded;
    }

    public NetworkDiagnosticsResult assertNoFailedRequests() {
        Instant startedAt = Instant.now();
        NetworkSummary current = summary();
        if (current.hasFailures()) {
            NetworkDiagnosticsResult result = NetworkDiagnosticsResult.of(
                    NetworkDiagnosticsStatus.ASSERTION_FAILED, current.failureSummary(), current, elapsedSince(startedAt));
            emit(UiTestLensEventType.NETWORK_ASSERTION_FAILED, UiTestLensStatus.FAILED,
                    UiTestLensLogLevel.ERROR, result.message(), null, null);
            throw new NetworkAssertionError(result.message(), current);
        }
        NetworkDiagnosticsResult result = NetworkDiagnosticsResult.of(
                NetworkDiagnosticsStatus.ASSERTION_PASSED, "No failed network requests", current, elapsedSince(startedAt));
        emit(UiTestLensEventType.NETWORK_ASSERTION_PASSED, UiTestLensStatus.PASSED,
                UiTestLensLogLevel.INFO, result.message(), null, null);
        return result;
    }

    public NetworkWaitResult waitForResponse(String urlContains, int status) {
        return waitForResponse(NetworkWaitCondition.builder().urlContains(urlContains).status(status).build());
    }

    /** Waits on a condition signalled directly by asynchronous capture callbacks. */
    public NetworkWaitResult waitForResponse(NetworkWaitCondition condition) {
        NetworkWaitCondition effective = condition == null ? NetworkWaitCondition.builder().build() : condition;
        Instant startedAt = Instant.now();
        emit(UiTestLensEventType.NETWORK_WAIT_STARTED, UiTestLensStatus.STARTED,
                UiTestLensLogLevel.INFO, "Network wait started: " + effective.summary(), null, null);
        NetworkWaitResult result;
        lock.lock();
        try {
            result = immediateWaitFailure(effective, startedAt);
            if (result == null) result = awaitMatch(effective, startedAt);
        } finally { lock.unlock(); }
        emitWaitResult(result);
        return result;
    }

    public NetworkResponseExpectation expectResponse() { return new NetworkResponseExpectation(this); }

    public Optional<NetworkEvent> findMatchingEvent(NetworkWaitCondition condition) {
        NetworkWaitCondition effective = condition == null ? NetworkWaitCondition.builder().build() : condition;
        List<NetworkEvent> snapshot = events();
        return snapshot.stream().filter(event -> effective.matches(event, snapshot)).findFirst();
    }

    /** Explicitly attaches the current summary to a session. Options never invoke this automatically. */
    public NetworkDiagnosticsResult attachToSession(UiTestLensSession session) {
        NetworkSummary current = summary();
        if (session == null) {
            return NetworkDiagnosticsResult.failed("No UiTestLensSession attached", current, null, Duration.ZERO);
        }
        session.addEvent(io.github.testlens.core.trace.TraceEvent.builder(
                        io.github.testlens.core.trace.TraceEventType.CUSTOM,
                        io.github.testlens.core.trace.TraceStatus.INFO, "Network diagnostics summary")
                .message(current.failureSummary())
                .attribute("requestedCaptureMode", captureMode().name())
                .attribute("activeCaptureMode", activeCaptureMode().map(Enum::name).orElse(""))
                .attribute("totalRequests", String.valueOf(current.totalRequests()))
                .attribute("totalResponses", String.valueOf(current.totalResponses()))
                .attribute("failedResponses", String.valueOf(current.failedResponses()))
                .attribute("failedRequests", String.valueOf(current.failedRequests()))
                .attribute("ignoredEvents", String.valueOf(current.ignoredEvents()))
                .attribute("droppedEvents", String.valueOf(current.droppedEvents())).build());
        return NetworkDiagnosticsResult.of(NetworkDiagnosticsStatus.ATTACHED,
                "Network diagnostics summary attached", current, Duration.ZERO);
    }

    /** Explicitly exports the current event log and attaches it to a session. */
    public NetworkDiagnosticsResult attachToSession(UiTestLensSession session, Path outputPath) {
        Instant startedAt = Instant.now();
        if (session == null) return NetworkDiagnosticsResult.failed(
                "No UiTestLensSession attached", summary(), null, elapsedSince(startedAt));
        if (outputPath == null) return NetworkDiagnosticsResult.failed(
                "Network log output path must not be null", summary(), null, elapsedSince(startedAt));
        try {
            Path parent = outputPath.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(outputPath, exportJson());
            NetworkSummary current = summary();
            TraceArtifact artifact = session.attachArtifact(TraceArtifact.networkLog("Network log", outputPath)
                    .withMetadata("totalRequests", String.valueOf(current.totalRequests()))
                    .withMetadata("failedResponses", String.valueOf(current.failedResponses()))
                    .withMetadata("failedRequests", String.valueOf(current.failedRequests()))
                    .withMetadata("droppedEvents", String.valueOf(current.droppedEvents())));
            emit(UiTestLensEventType.NETWORK_LOG_ATTACHED, UiTestLensStatus.PASSED,
                    UiTestLensLogLevel.INFO, "Network log attached", artifact.path(), null);
            return NetworkDiagnosticsResult.of(NetworkDiagnosticsStatus.ATTACHED,
                    "Network log attached", current, elapsedSince(startedAt));
        } catch (IOException | RuntimeException failure) {
            return NetworkDiagnosticsResult.failed("Failed to attach network log: " + messageFor(failure),
                    summary(), failure, elapsedSince(startedAt));
        }
    }

    public String exportJson() { return new NetworkLogExporter().export(this); }

    private void activate(long token, NetworkCaptureMode mode, NetworkCaptureSource source) {
        if (generation != token) return;
        captureSource = source;
        activeMode = mode;
        started = true;
        status = NetworkDiagnosticsStatus.STARTED;
        startFailure = null;
    }

    private void markStartUnsupported(long token, NetworkCaptureMode requestedMode, RuntimeException unsupported) {
        lock.lock();
        try {
            if (generation != token) return;
            markUnsupported("Network capture mode " + requestedMode.name() + " is unsupported: "
                    + messageFor(unsupported));
            startFailure = unsupported;
            eventArrived.signalAll();
        } finally { lock.unlock(); }
        emitUnsupported();
    }

    private void markStartFailed(long token, NetworkCaptureMode requestedMode, RuntimeException failure) {
        lock.lock();
        try {
            if (generation != token) return;
            started = false;
            activeMode = null;
            captureSource = null;
            status = NetworkDiagnosticsStatus.FAILED;
            startFailure = failure;
            statusMessage = "WebDriver BiDi network capture failed to start for " + requestedMode.name()
                    + ": " + messageFor(failure);
            addInternal(NetworkEvent.warning(statusMessage));
            eventArrived.signalAll();
        } finally { lock.unlock(); }
        emit(UiTestLensEventType.NETWORK_DIAGNOSTICS_STARTED, UiTestLensStatus.FAILED,
                UiTestLensLogLevel.ERROR, statusMessage(), null, failure);
    }

    private void markUnsupported(String message) {
        started = false;
        activeMode = null;
        captureSource = null;
        status = NetworkDiagnosticsStatus.UNSUPPORTED;
        statusMessage = message;
        addInternal(NetworkEvent.warning(message));
    }

    private void recordCaptured(long token, NetworkEvent event) {
        if (event == null) return;
        boolean recorded = false;
        boolean warnLimit = false;
        lock.lock();
        try {
            if (!started || activeMode != NetworkCaptureMode.BIDI || generation != token) return;
            AddResult result = addCaptured(event);
            recorded = result == AddResult.ADDED;
            warnLimit = result == AddResult.DROPPED_WITH_WARNING;
        } finally { lock.unlock(); }
        if (recorded) emitRecorded(event);
        if (warnLimit) emitLimitWarning();
    }

    private void recordIgnored(long token) {
        lock.lock();
        try {
            if (started && activeMode == NetworkCaptureMode.BIDI && generation == token) ignoredEvents++;
        } finally { lock.unlock(); }
    }

    private AddResult addCaptured(NetworkEvent event) {
        if (isCapturedEvent(event) && capturedEvents >= options.maxCapturedEvents()) {
            droppedEvents++;
            if (!limitWarningEmitted) {
                limitWarningEmitted = true;
                addInternal(NetworkEvent.warning("Network event limit reached; subsequent events are dropped"));
                return AddResult.DROPPED_WITH_WARNING;
            }
            return AddResult.DROPPED;
        }
        events.add(event);
        if (isCapturedEvent(event)) capturedEvents++;
        eventArrived.signalAll();
        return AddResult.ADDED;
    }

    private NetworkWaitResult immediateWaitFailure(NetworkWaitCondition condition, Instant startedAt) {
        if (status == NetworkDiagnosticsStatus.UNSUPPORTED) return NetworkWaitResult.skipped(
                condition, statusMessage, NetworkWaitFailureReason.UNSUPPORTED_CAPTURE_MODE, 0, elapsedSince(startedAt));
        if (status == NetworkDiagnosticsStatus.FAILED) return NetworkWaitResult.failed(
                condition, statusMessage, NetworkWaitFailureReason.CAPTURE_START_FAILED,
                startFailure, 0, elapsedSince(startedAt));
        if (!started) return NetworkWaitResult.skipped(condition, "Network diagnostics capture is not started",
                NetworkWaitFailureReason.CAPTURE_NOT_STARTED, 0, elapsedSince(startedAt));
        return null;
    }

    private NetworkWaitResult awaitMatch(NetworkWaitCondition condition, Instant startedAt) {
        long deadline = System.nanoTime() + condition.timeout().toNanos();
        int attempts = 0;
        while (true) {
            attempts++;
            List<NetworkEvent> snapshot = List.copyOf(events);
            Optional<NetworkEvent> failed = snapshot.stream()
                    .filter(event -> condition.matchesFailedResponse(event, snapshot)).findFirst();
            if (failed.isPresent()) return NetworkWaitResult.failed(condition,
                    "Failed response matched while waiting for: " + condition.summary(),
                    NetworkWaitFailureReason.FAILED_RESPONSE_MATCHED, null, attempts, elapsedSince(startedAt));
            Optional<NetworkEvent> matched = snapshot.stream()
                    .filter(event -> condition.matches(event, snapshot)).findFirst();
            if (matched.isPresent()) {
                NetworkRequest request = matched.get().request();
                if (request == null) request = findRequestFor(matched.get(), snapshot).orElse(null);
                return NetworkWaitResult.matched(condition, matched.get(), request, attempts, elapsedSince(startedAt));
            }
            if (!started) return NetworkWaitResult.skipped(condition,
                    "Network diagnostics capture stopped while waiting",
                    NetworkWaitFailureReason.CAPTURE_NOT_STARTED, attempts, elapsedSince(startedAt));
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) return NetworkWaitResult.timedOut(condition, attempts,
                    elapsedSince(startedAt), NetworkSummary.from(events, ignoredEvents, droppedEvents,
                            options.failedStatusThreshold(), status));
            try {
                eventArrived.awaitNanos(Math.min(remaining, condition.pollInterval().toNanos()));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return NetworkWaitResult.failed(condition, "Network wait was interrupted",
                        NetworkWaitFailureReason.UNKNOWN, interrupted, attempts, elapsedSince(startedAt));
            }
        }
    }

    private Optional<NetworkRequest> findRequestFor(NetworkEvent responseEvent, List<NetworkEvent> snapshot) {
        if (responseEvent == null || responseEvent.response() == null) return Optional.empty();
        String requestId = responseEvent.response().requestId();
        NetworkRequest embedded = responseEvent.correlatedRequest();
        if (embedded != null && requestId.equals(embedded.id())) return Optional.of(embedded);
        String redirect = responseEvent.attributes().get("redirectCount");
        Optional<NetworkRequest> correlated = snapshot.stream()
                .filter(event -> event.request() != null && requestId.equals(event.request().id()))
                .filter(event -> redirect == null || redirect.equals(event.attributes().get("redirectCount")))
                .map(NetworkEvent::request).findFirst();
        if (correlated.isPresent() || redirect != null) return correlated;
        return snapshot.stream().map(NetworkEvent::request).filter(Objects::nonNull)
                .filter(request -> requestId.equals(request.id())).findFirst();
    }

    private NetworkEvent sanitize(NetworkEvent event) {
        if (!options.includeHeaders()) return removeHeaders(event);
        if (options.maskSensitiveHeaders()) return maskHeaders(event);
        return event;
    }

    private NetworkEvent removeHeaders(NetworkEvent event) {
        if (event.request() != null) {
            NetworkRequest request = event.request();
            return NetworkEvent.request(new NetworkRequest(request.id(), request.method(), request.url(),
                    request.resourceType(), request.timestamp(), Map.of()), event.timestamp(), event.attributes());
        }
        if (event.response() != null) {
            NetworkResponse response = event.response();
            return NetworkEvent.response(new NetworkResponse(response.requestId(), response.url(), response.status(),
                    response.statusText(), response.mimeType(), response.duration(), response.timestamp(), Map.of()),
                    event.timestamp(), event.attributes());
        }
        return event;
    }

    private NetworkEvent maskHeaders(NetworkEvent event) {
        if (event.request() != null) {
            NetworkRequest request = event.request();
            return NetworkEvent.request(new NetworkRequest(request.id(), request.method(), request.url(),
                    request.resourceType(), request.timestamp(), mask(request.headers())), event.timestamp(), event.attributes());
        }
        if (event.response() != null) {
            NetworkResponse response = event.response();
            return NetworkEvent.response(new NetworkResponse(response.requestId(), response.url(), response.status(),
                    response.statusText(), response.mimeType(), response.duration(), response.timestamp(),
                    mask(response.headers())), event.timestamp(), event.attributes());
        }
        return event;
    }

    private Map<String, String> mask(Map<String, String> headers) {
        Map<String, String> masked = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : headers == null ? Map.<String, String>of().entrySet() : headers.entrySet()) {
            masked.put(entry.getKey(), SeleniumBiDiNetworkCaptureSource.isSensitive(entry.getKey())
                    ? "***" : entry.getValue());
        }
        return masked;
    }

    private void addInternal(NetworkEvent event) { events.add(event); }

    private void emitStarted() {
        emit(UiTestLensEventType.NETWORK_DIAGNOSTICS_STARTED, UiTestLensStatus.STARTED,
                UiTestLensLogLevel.INFO, statusMessage(), null, null);
    }

    private void emitUnsupported() {
        emit(UiTestLensEventType.NETWORK_DIAGNOSTICS_STARTED, UiTestLensStatus.SKIPPED,
                UiTestLensLogLevel.WARN, statusMessage(), null, startFailure());
    }

    private void emitRecorded(NetworkEvent event) {
        UiTestLensEventType eventType = switch (event.type()) {
            case REQUEST -> UiTestLensEventType.NETWORK_REQUEST_RECORDED;
            case RESPONSE -> UiTestLensEventType.NETWORK_RESPONSE_RECORDED;
            case FAILED -> UiTestLensEventType.NETWORK_FAILURE_RECORDED;
            default -> UiTestLensEventType.NETWORK_DIAGNOSTICS_STARTED;
        };
        emit(eventType, UiTestLensStatus.INFO, UiTestLensLogLevel.INFO, event.message(), event.url(), null);
    }

    private void emitLimitWarning() {
        emit(UiTestLensEventType.NETWORK_DIAGNOSTICS_STARTED, UiTestLensStatus.WARN,
                UiTestLensLogLevel.WARN, "Network event limit reached; subsequent events are dropped", null, null);
    }

    private void emitWaitResult(NetworkWaitResult result) {
        UiTestLensEventType eventType;
        UiTestLensStatus eventStatus;
        UiTestLensLogLevel level;
        if (result.status() == NetworkWaitStatus.MATCHED) {
            eventType = UiTestLensEventType.NETWORK_WAIT_PASSED;
            eventStatus = UiTestLensStatus.PASSED;
            level = UiTestLensLogLevel.INFO;
        } else if (result.status() == NetworkWaitStatus.TIMED_OUT) {
            eventType = UiTestLensEventType.NETWORK_WAIT_TIMED_OUT;
            eventStatus = UiTestLensStatus.FAILED;
            level = UiTestLensLogLevel.ERROR;
        } else {
            eventType = UiTestLensEventType.NETWORK_WAIT_FAILED;
            eventStatus = result.status() == NetworkWaitStatus.SKIPPED
                    ? UiTestLensStatus.SKIPPED : UiTestLensStatus.FAILED;
            level = result.status() == NetworkWaitStatus.SKIPPED
                    ? UiTestLensLogLevel.WARN : UiTestLensLogLevel.ERROR;
        }
        String url = result.matchedEvent() == null ? null : result.matchedEvent().url();
        emit(eventType, eventStatus, level, result.message(), url, result.exception());
    }

    private void emit(UiTestLensEventType eventType, UiTestLensStatus eventStatus, UiTestLensLogLevel level,
                      String message, String url, Throwable throwable) {
        try {
            NetworkSummary current = summary();
            logger.emit(UiTestLensLogEntry.builder().level(level).eventType(eventType).status(eventStatus)
                    .message(message).action("network.diagnostics")
                    .metadata("requestedMode", captureMode().name())
                    .metadata("activeMode", activeCaptureMode().map(Enum::name).orElse(""))
                    .metadata("url", safeUrlPreview(url))
                    .metadata("totalRequests", String.valueOf(current.totalRequests()))
                    .metadata("totalResponses", String.valueOf(current.totalResponses()))
                    .metadata("failedResponses", String.valueOf(current.failedResponses()))
                    .metadata("failedRequests", String.valueOf(current.failedRequests()))
                    .metadata("ignoredEvents", String.valueOf(current.ignoredEvents()))
                    .metadata("droppedEvents", String.valueOf(current.droppedEvents()))
                    .throwable(throwable).build());
        } catch (RuntimeException ignored) {
            // Diagnostics must not change application behavior.
        }
    }

    private String statusMessage() {
        lock.lock();
        try { return statusMessage; } finally { lock.unlock(); }
    }

    private Throwable startFailure() {
        lock.lock();
        try { return startFailure; } finally { lock.unlock(); }
    }

    private static RuntimeException closeQuietly(NetworkCaptureSource source, RuntimeException primary) {
        if (source == null) return primary;
        try { source.close(); }
        catch (RuntimeException failure) {
            if (primary != null) primary.addSuppressed(failure);
            else primary = failure;
        }
        return primary;
    }

    private static boolean isCapturedEvent(NetworkEvent event) {
        return event != null && (event.type() == NetworkEventType.REQUEST
                || event.type() == NetworkEventType.RESPONSE || event.type() == NetworkEventType.FAILED);
    }

    private static String safeUrlPreview(String url) {
        String safe = url == null ? "" : url;
        int query = safe.indexOf('?');
        String withoutQuery = query >= 0 ? safe.substring(0, query) : safe;
        return withoutQuery.length() <= 180 ? withoutQuery : withoutQuery.substring(0, 177) + "...";
    }

    private static Duration elapsedSince(Instant started) {
        Duration elapsed = Duration.between(started, Instant.now());
        return elapsed.isNegative() ? Duration.ZERO : elapsed;
    }

    private static String messageFor(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private enum AddResult { ADDED, DROPPED, DROPPED_WITH_WARNING }
}
