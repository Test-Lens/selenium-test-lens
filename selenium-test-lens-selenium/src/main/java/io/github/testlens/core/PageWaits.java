package io.github.testlens.core;

import io.github.testlens.OverlayConfig;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Sleeper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Provides condition polling for document, JavaScript-observed XHR/fetch, and SPA/React state.
 * A timeout is the total operation budget, including every stage of a composite wait. Condition polling is
 * distinct from recovery retry and does not make a session flaky.
 */
public class PageWaits {
    private static final Duration LEGACY_DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(100);
    private static final Duration DEFAULT_NETWORK_IDLE = Duration.ofMillis(500);
    private static final Duration DEFAULT_DOM_IDLE = Duration.ofMillis(300);

    private final WebDriver driver;
    private final JavascriptExecutor js;
    private final Duration defaultTimeout;
    private final Duration pollInterval;
    private final OverlayLogger logger;
    private final Clock clock;
    private final Sleeper sleeper;

    public PageWaits(WebDriver driver, OverlayConfig config) {
        this(driver, config, LEGACY_DEFAULT_TIMEOUT);
    }

    public PageWaits(WebDriver driver, OverlayConfig config, Duration defaultTimeout) {
        this(driver, config, defaultTimeout, DEFAULT_POLL_INTERVAL, OverlayLogger.noop(),
                new MonotonicClock(), Sleeper.SYSTEM_SLEEPER);
    }

    protected PageWaits(WebDriver driver,
                        OverlayConfig config,
                        Duration defaultTimeout,
                        Duration pollInterval,
                        OverlayLogger logger) {
        this(driver, config, defaultTimeout, pollInterval, logger, new MonotonicClock(), Sleeper.SYSTEM_SLEEPER);
    }

    PageWaits(WebDriver driver,
              OverlayConfig config,
              Duration defaultTimeout,
              Duration pollInterval,
              OverlayLogger logger,
              Clock clock,
              Sleeper sleeper) {
        if (!(driver instanceof JavascriptExecutor executor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.driver = driver;
        this.js = executor;
        this.defaultTimeout = nonNegative(defaultTimeout == null ? LEGACY_DEFAULT_TIMEOUT : defaultTimeout, "defaultTimeout");
        this.pollInterval = positive(pollInterval, "pollInterval");
        this.logger = logger == null ? OverlayLogger.noop() : logger;
        this.clock = clock == null ? new MonotonicClock() : clock;
        this.sleeper = sleeper == null ? Sleeper.SYSTEM_SLEEPER : sleeper;
    }

    /** Waits until one {@code document.readyState} observation per poll returns {@code complete}. */
    public void waitForDocumentReady() {
        waitForDocumentReady(defaultTimeout);
    }

    /**
     * Waits until {@code document.readyState} is {@code complete} within the supplied total timeout.
     * Terminal WebDriver or JavaScript failures are propagated rather than converted to timeouts.
     */
    public void waitForDocumentReady(Duration timeout) {
        Duration effectiveTimeout = timeoutOrDefault(timeout);
        execute("DOCUMENT_READY", "DOCUMENT_READY_STATE", effectiveTimeout, null, context -> {
            poll(context, "document.readyState to become complete", () -> {
                context.attempts++;
                return "complete".equals(js.executeScript("return document.readyState"));
            });
            return null;
        });
    }

    /** Waits until {@code document.readyState} is {@code interactive} or {@code complete}. */
    public void waitForInteractiveOrComplete() {
        waitForInteractiveOrComplete(defaultTimeout);
    }

    /** Waits for an interactive or complete document within the supplied total timeout. */
    public void waitForInteractiveOrComplete(Duration timeout) {
        Duration effectiveTimeout = timeoutOrDefault(timeout);
        execute("DOCUMENT_INTERACTIVE", "DOCUMENT_READY_STATE", effectiveTimeout, null, context -> {
            poll(context, "document.readyState to become interactive or complete", () -> {
                context.attempts++;
                Object value = js.executeScript("return document.readyState");
                return "interactive".equals(value) || "complete".equals(value);
            });
            return null;
        });
    }

    /**
     * Waits until the injected tracker observes zero active XHR/fetch requests for the entire idle duration.
     * The tracker observes only XHR and fetch operations started after its installation in the current document;
     * it does not cover earlier requests, images, style sheets, scripts, WebSockets, EventSource, or beacons, and
     * therefore is not a complete browser-network-idle signal.
     */
    public void waitForNetworkIdle(Duration idleDuration, Duration timeout) {
        Duration effectiveIdle = nonNegative(idleDuration == null ? DEFAULT_NETWORK_IDLE : idleDuration, "idleDuration");
        Duration effectiveTimeout = timeoutOrDefault(timeout);
        execute("NETWORK_IDLE", "JS_XHR_FETCH_TRACKER", effectiveTimeout, effectiveIdle, context -> {
            js.executeScript(networkTrackerScript());
            final Instant[] idleSince = {null};
            poll(context, "observed XHR/fetch activity to remain idle", () -> {
                context.attempts++;
                Object value = js.executeScript(networkActiveRequestsScript());
                if (!(value instanceof Number number)) {
                    throw new WebDriverException("XHR/fetch tracker returned a non-numeric active request count");
                }
                long active = number.longValue();
                Instant now = clock.instant();
                if (active != 0L) {
                    idleSince[0] = null;
                    return false;
                }
                if (idleSince[0] == null) idleSince[0] = now;
                return !Duration.between(idleSince[0], now).minus(effectiveIdle).isNegative();
            });
            return null;
        });
    }

    /** Waits for the default XHR/fetch idle window using the configured default timeout. */
    public void waitForNetworkIdle() {
        waitForNetworkIdle(DEFAULT_NETWORK_IDLE, defaultTimeout);
    }

    public WebElement waitForReactRootMounted(By rootLocator) {
        return waitForReactRootMounted(rootLocator, defaultTimeout);
    }

    public WebElement waitForReactRootMounted(By rootLocator, Duration timeout) {
        Duration effectiveTimeout = timeoutOrDefault(timeout);
        return execute("REACT_ROOT_MOUNTED", "DOCUMENT_READY_STATE", effectiveTimeout, null,
                context -> awaitReactRoot(context, rootLocator));
    }

    public void waitForSpaDomStableUnder(By rootLocator, Duration idleDuration, Duration timeout) {
        Duration effectiveIdle = nonNegative(idleDuration == null ? DEFAULT_DOM_IDLE : idleDuration, "idleDuration");
        Duration effectiveTimeout = timeoutOrDefault(timeout);
        execute("SPA_DOM_STABLE", "DOCUMENT_READY_STATE", effectiveTimeout, effectiveIdle, context -> {
            awaitDomStable(context, rootLocator, effectiveIdle);
            return null;
        });
    }

    public void waitForSpaDomStableUnder(By rootLocator) {
        waitForSpaDomStableUnder(rootLocator, DEFAULT_DOM_IDLE, defaultTimeout);
    }

    public WebElement waitForReactComponentVisible(By rootLocator, By componentLocator) {
        return waitForReactComponentVisible(rootLocator, componentLocator, defaultTimeout);
    }

    /**
     * Waits for the React root, DOM stability, and component visibility under one shared timeout deadline.
     */
    public WebElement waitForReactComponentVisible(By rootLocator, By componentLocator, Duration timeout) {
        Duration effectiveTimeout = timeoutOrDefault(timeout);
        return execute("REACT_COMPONENT_VISIBLE", "DOCUMENT_READY_STATE", effectiveTimeout, null, context -> {
            awaitReactRoot(context, rootLocator);
            awaitDomStable(context, rootLocator, DEFAULT_DOM_IDLE);
            return poll(context, "React component to become visible", () -> {
                context.attempts++;
                try {
                    WebElement component = driver.findElement(componentLocator);
                    return component != null && component.isDisplayed() ? component : null;
                } catch (NoSuchElementException ignored) {
                    return null;
                }
            });
        });
    }

    public void waitForReactAndNetworkIdle(By rootLocator) {
        waitForReactAndNetworkIdle(rootLocator, defaultTimeout);
    }

    /**
     * Waits for document readiness, observed XHR/fetch idle, a mounted React root, and DOM stability under
     * one shared timeout deadline.
     */
    public void waitForReactAndNetworkIdle(By rootLocator, Duration timeout) {
        Duration effectiveTimeout = timeoutOrDefault(timeout);
        execute("REACT_AND_NETWORK_IDLE", "JS_XHR_FETCH_TRACKER", effectiveTimeout, DEFAULT_NETWORK_IDLE, context -> {
            poll(context, "document.readyState to become complete", () -> {
                context.attempts++;
                return "complete".equals(js.executeScript("return document.readyState"));
            });
            js.executeScript(networkTrackerScript());
            final Instant[] idleSince = {null};
            poll(context, "observed XHR/fetch activity to remain idle", () -> {
                context.attempts++;
                Object value = js.executeScript(networkActiveRequestsScript());
                if (!(value instanceof Number number)) {
                    throw new WebDriverException("XHR/fetch tracker returned a non-numeric active request count");
                }
                Instant now = clock.instant();
                if (number.longValue() != 0L) {
                    idleSince[0] = null;
                    return false;
                }
                if (idleSince[0] == null) idleSince[0] = now;
                return !Duration.between(idleSince[0], now).minus(DEFAULT_NETWORK_IDLE).isNegative();
            });
            awaitReactRoot(context, rootLocator);
            awaitDomStable(context, rootLocator, DEFAULT_DOM_IDLE);
            return null;
        });
    }

    private WebElement awaitReactRoot(WaitContext context, By rootLocator) {
        return poll(context, "React root to mount", () -> {
            context.attempts++;
            try {
                WebElement root = driver.findElement(rootLocator);
                Object hasChildren = js.executeScript(
                        "return (arguments[0] && arguments[0].children && arguments[0].children.length > 0);", root);
                return Boolean.TRUE.equals(hasChildren) ? root : null;
            } catch (NoSuchElementException ignored) {
                return null;
            }
        });
    }

    private void awaitDomStable(WaitContext context, By rootLocator, Duration idleDuration) {
        poll(context, "SPA DOM to remain stable", () -> {
            context.attempts++;
            try {
                WebElement root = driver.findElement(rootLocator);
                Object age = js.executeScript(domStableMutationScript(), root);
                return age instanceof Number number && number.longValue() >= idleDuration.toMillis();
            } catch (NoSuchElementException ignored) {
                return false;
            }
        });
    }

    private <T> T poll(WaitContext context, String condition, Supplier<T> observation) {
        while (true) {
            T value = observation.get();
            if (value instanceof Boolean bool ? bool : value != null) return value;
            Instant now = clock.instant();
            if (!now.isBefore(context.deadline)) {
                throw new TimeoutException("Timed out waiting for " + condition);
            }
            Duration remaining = Duration.between(now, context.deadline);
            Duration sleepFor = remaining.compareTo(pollInterval) < 0 ? remaining : pollInterval;
            try {
                sleeper.sleep(sleepFor);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new WebDriverException("Interrupted while polling page wait", interrupted);
            }
        }
    }

    private <T> T execute(String waitKind, String source, Duration timeout, Duration idleDuration,
                          WaitOperation<T> operation) {
        Instant started = clock.instant();
        WaitContext context = new WaitContext(started.plus(timeout));
        emit(waitKind, source, timeout, idleDuration, Duration.ZERO, context.attempts,
                UiTestLensStatus.STARTED, null, null);
        showIndicator(waitKind);
        try {
            T result = operation.run(context);
            Duration elapsed = elapsedSince(started);
            rememberLastWaitMessage("[WAIT] " + waitKind + " passed in " + elapsed.toMillis() + " ms");
            emit(waitKind, source, timeout, idleDuration, elapsed, context.attempts,
                    UiTestLensStatus.PASSED, null, null);
            return result;
        } catch (TimeoutException timeoutFailure) {
            Duration elapsed = elapsedSince(started);
            rememberLastWaitMessage("[WAIT] " + waitKind + " timed out after " + elapsed.toMillis() + " ms");
            emit(waitKind, source, timeout, idleDuration, elapsed, context.attempts,
                    UiTestLensStatus.FAILED, "TIMEOUT", timeoutFailure);
            throw timeoutFailure;
        } catch (RuntimeException terminalFailure) {
            Duration elapsed = elapsedSince(started);
            rememberLastWaitMessage("[WAIT] " + waitKind + " failed after " + elapsed.toMillis() + " ms");
            emit(waitKind, source, timeout, idleDuration, elapsed, context.attempts,
                    UiTestLensStatus.FAILED, "TERMINAL_ERROR", terminalFailure);
            throw terminalFailure;
        } finally {
            hideIndicator();
        }
    }

    private void emit(String waitKind, String source, Duration timeout, Duration idleDuration,
                      Duration elapsed, long attempts, UiTestLensStatus status, String reason, Throwable throwable) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("waitKind", waitKind);
        metadata.put("source", source);
        metadata.put("timeoutMs", String.valueOf(timeout.toMillis()));
        metadata.put("pollIntervalMs", String.valueOf(pollInterval.toMillis()));
        metadata.put("elapsedMs", String.valueOf(elapsed.toMillis()));
        metadata.put("attempts", String.valueOf(attempts));
        if (idleDuration != null) metadata.put("idleDurationMs", String.valueOf(idleDuration.toMillis()));
        if (reason != null) metadata.put("reason", reason);
        try {
            logger.emit(UiTestLensLogEntry.builder()
                    .level(status == UiTestLensStatus.FAILED ? UiTestLensLogLevel.ERROR : UiTestLensLogLevel.INFO)
                    .eventType(UiTestLensEventType.WAIT)
                    .status(status)
                    .message(status == UiTestLensStatus.STARTED
                            ? "Page wait started: " + waitKind
                            : "Page wait " + status.name().toLowerCase() + ": " + waitKind)
                    .action("page.wait")
                    .metadata(metadata)
                    .throwable(throwable)
                    .build());
        } catch (RuntimeException ignored) {
            // Diagnostics are decorative and must not change the wait outcome.
        }
    }

    private void showIndicator(String waitKind) {
        try {
            js.executeScript(WaitHudJs.INIT
                    + "if (waitHud && waitHud.showIndicator) { waitHud.showIndicator(arguments[0]); }", waitKind);
        } catch (RuntimeException ignored) {
            // Decorative HUD only.
        }
    }

    private void hideIndicator() {
        try {
            js.executeScript(WaitHudJs.bridgeScript()
                    + "if (waitHud && waitHud.hideIndicator) { waitHud.hideIndicator(); }");
        } catch (RuntimeException ignored) {
            // Decorative HUD only.
        }
    }

    private void rememberLastWaitMessage(String message) {
        try {
            js.executeScript(rememberLastWaitMessageScript(), message);
        } catch (RuntimeException ignored) {
            // Decorative HUD only.
        }
    }

    static String rememberLastWaitMessageScript() {
        return UiTestLensRuntimeNames.ensureNamespaceScript()
                + "var waitState = window.__uiTestLens.state.wait;"
                + "waitState.lastMessage = arguments[0];"
                + "window.__seleniumLastWaitMessage = waitState.lastMessage;";
    }

    static String networkActiveRequestsScript() {
        return UiTestLensRuntimeNames.ensureNamespaceScript()
                + "var networkState = window.__uiTestLens.state.network;"
                + "if (typeof networkState.activeRequests !== 'number') {"
                + "  networkState.activeRequests = Number(window.__seleniumActiveRequests) || 0;"
                + "}"
                + "networkState.activeRequests = Math.max(0, networkState.activeRequests);"
                + "window.__seleniumActiveRequests = networkState.activeRequests;"
                + "return networkState.activeRequests;";
    }

    static String networkTrackerScript() {
        return UiTestLensRuntimeNames.ensureNamespaceScript()
                + "var networkState = window.__uiTestLens.state.network;"
                + "function syncLegacyActiveRequests() {"
                + "  networkState.activeRequests = Math.max(0, Number(networkState.activeRequests) || 0);"
                + "  window.__seleniumActiveRequests = networkState.activeRequests;"
                + "}"
                + "function beginRequest() { networkState.activeRequests++; syncLegacyActiveRequests(); }"
                + "function finishRequest() { networkState.activeRequests = Math.max(0, networkState.activeRequests - 1); syncLegacyActiveRequests(); }"
                + "if (networkState.trackerInstalled || window.__seleniumNetworkTrackerInstalled) {"
                + "  networkState.trackerInstalled = true; window.__seleniumNetworkTrackerInstalled = true;"
                + "  syncLegacyActiveRequests(); return;"
                + "}"
                + "networkState.trackerInstalled = true; window.__seleniumNetworkTrackerInstalled = true;"
                + "networkState.activeRequests = 0; syncLegacyActiveRequests();"
                + "(function() {"
                + "  var origOpen = XMLHttpRequest.prototype.open; var origSend = XMLHttpRequest.prototype.send;"
                + "  XMLHttpRequest.prototype.open = function() { return origOpen.apply(this, arguments); };"
                + "  XMLHttpRequest.prototype.send = function() {"
                + "    var finished = false; function finishOnce() { if (!finished) { finished = true; finishRequest(); } }"
                + "    this.addEventListener('loadend', finishOnce, {once:true}); beginRequest();"
                + "    try { return origSend.apply(this, arguments); } catch (failure) { finishOnce(); throw failure; }"
                + "  };"
                + "})();"
                + "(function() {"
                + "  if (!window.fetch) return; var origFetch = window.fetch;"
                + "  window.fetch = function() {"
                + "    beginRequest(); var promise;"
                + "    try { promise = origFetch.apply(window, arguments); } catch (failure) { finishRequest(); throw failure; }"
                + "    return Promise.resolve(promise).then("
                + "      function(value) { finishRequest(); return value; },"
                + "      function(failure) { finishRequest(); throw failure; });"
                + "  };"
                + "})();";
    }

    static String domStableMutationScript() {
        return UiTestLensRuntimeNames.ensureNamespaceScript()
                + "var root = arguments[0]; if (!root) return -1;"
                + "if (!root.__seleniumDomStableInit) {"
                + "  root.__seleniumDomStableInit = true; root.__seleniumLastMutation = Date.now();"
                + "  var obs = new MutationObserver(function() { root.__seleniumLastMutation = Date.now(); });"
                + "  obs.observe(root, {childList:true,subtree:true,attributes:true,characterData:true});"
                + "}"
                + "return Math.max(0, Date.now() - root.__seleniumLastMutation);";
    }

    private Duration timeoutOrDefault(Duration timeout) {
        return nonNegative(timeout == null ? defaultTimeout : timeout, "timeout");
    }

    private Duration elapsedSince(Instant started) {
        Duration elapsed = Duration.between(started, clock.instant());
        return elapsed.isNegative() ? Duration.ZERO : elapsed;
    }

    private static Duration nonNegative(Duration duration, String name) {
        if (duration == null || duration.isNegative()) throw new IllegalArgumentException(name + " must not be negative");
        return duration;
    }

    private static Duration positive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return duration;
    }

    private static final class WaitContext {
        private final Instant deadline;
        private long attempts;

        private WaitContext(Instant deadline) {
            this.deadline = deadline;
        }
    }

    @FunctionalInterface
    private interface WaitOperation<T> {
        T run(WaitContext context);
    }

    private static final class MonotonicClock extends Clock {
        private final Instant origin = Instant.now();
        private final long startNanos = System.nanoTime();

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return origin.plusNanos(System.nanoTime() - startNanos); }
    }
}
