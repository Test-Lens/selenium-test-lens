package io.github.testlens.actions;

import io.github.testlens.OverlayConfig;
import io.github.testlens.core.BlockingOverlayHelper;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.logging.TargetDescriptor;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;
import io.github.testlens.selenium.actionability.ActionabilityChecker;
import io.github.testlens.selenium.actionability.ActionabilityFailureReason;
import io.github.testlens.selenium.actionability.ActionabilityOptions;
import io.github.testlens.selenium.actionability.ActionabilityReport;
import io.github.testlens.selenium.overlay.OverlayHandlingResult;
import io.github.testlens.selenium.overlay.OverlayHandlingStatus;
import io.github.testlens.selenium.overlay.OverlayPolicy;
import io.github.testlens.selenium.overlay.OverlayPolicyExecutor;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Optional;

public class SmartClickActions {

    private final WebDriver driver;
    private final OverlayConfig config;
    private final HighlightActions highlightActions;
    private final BlockingOverlayHelper blockingHelper;
    private final OverlayLogger logger;
    private OverlayPolicy overlayPolicy = OverlayPolicy.none();

    public SmartClickActions(WebDriver driver,
                             OverlayConfig config,
                             OverlayRootManager rootManager,
                             HighlightActions highlightActions) {
        this(driver, config, rootManager, highlightActions, OverlayLogger.noop());
    }

    public SmartClickActions(WebDriver driver,
                             OverlayConfig config,
                             OverlayRootManager rootManager,
                             HighlightActions highlightActions,
                             OverlayLogger logger) {
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.driver = driver;
        this.config = config;
        this.highlightActions = highlightActions;
        this.blockingHelper = new BlockingOverlayHelper(driver, config, rootManager, highlightActions);
        this.logger = logger != null ? logger : OverlayLogger.noop();
    }

    public void setOverlayPolicy(OverlayPolicy overlayPolicy) {
        this.overlayPolicy = overlayPolicy != null ? overlayPolicy : OverlayPolicy.none();
    }

    /**
     * Main click implementation with configurable overlay policy and existing legacy overlay heuristics.
     */
    public void clickWithOverlayHandling(WebElement target, String label) {
        if (target == null) {
            return;
        }
        emitClick("clickWithOverlayHandling", label, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO, null, false, null, false);
        try {
            runActionabilityCheck(target);
            boolean policyHandledBeforeClick = handleConfiguredOverlayPolicy();

            try {
                blockingHelper.handleGlobalOverlayIfPresent("OVERLAY", "CLOSE");
            } catch (RuntimeException observabilityFailure) {
                emitClick("overlayProbe", label, UiTestLensStatus.WARN, UiTestLensLogLevel.WARN,
                        observabilityFailure, false, "bestEffortOverlayProbe", false);
            }

            decorateClickTarget(target, label);
            long clickStarted = System.nanoTime();
            try {
                target.click();
                emitClick("clickWithOverlayHandling", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, false, null, true);
                return;
            } catch (WebDriverException e) {
                if (!isClickInterceptError(e)) {
                    throw e;
                }
                emitRecoveryRetry(label, e, Math.max(0, System.nanoTime() - clickStarted));
                if (handleConfiguredOverlayPolicy()) {
                    clickTarget(target, label);
                    emitClick("clickWithOverlayHandling", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, true, "overlayPolicy", true);
                    return;
                }
            }

            boolean handled = blockingHelper.handleBlockingOverlayFor(
                    target,
                    "BLOCKING OVERLAY",
                    "CLOSE"
            );

            if (handled) {
                clickTarget(target, label);
                emitClick("clickWithOverlayHandling", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, true, "blockingOverlay", true);
            } else {
                target.click();
                emitClick("clickWithOverlayHandling", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, true, "directRetry", policyHandledBeforeClick);
            }
        } catch (RuntimeException e) {
            emitClick("clickWithOverlayHandling", label, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR, e, false, null, false);
            throw e;
        }
    }

    /**
     * Legacy name kept for compatibility.
     */
    @Deprecated
    public void smartClick(WebElement target, String label) {
        clickWithOverlayHandling(target, label);
    }

    private boolean isClickInterceptError(Throwable e) {
        Throwable current = e;
        java.util.Set<Throwable> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        while (current != null && seen.add(current)) {
            if (current instanceof ElementClickInterceptedException) return true;
            current = current.getCause();
        }
        return false;
    }

    private void clickTarget(WebElement target, String label) {
        decorateClickTarget(target, label);
        target.click();
    }

    private void decorateClickTarget(WebElement target, String label) {
        if (config.isEnabled()) {
            highlightActions.highlightClick(target, label);
        }
    }

    private boolean handleConfiguredOverlayPolicy() {
        if (overlayPolicy == null || overlayPolicy.isEmpty()) {
            return false;
        }
        List<OverlayHandlingResult> results = new OverlayPolicyExecutor(driver, overlayPolicy, logger)
                .handleKnownOverlays();
        for (OverlayHandlingResult result : results) {
            if (result.status() == OverlayHandlingStatus.FAILED) {
                throw new IllegalStateException("Overlay policy failed for handler " + result.handlerName()
                        + ": " + result.message(), result.exception());
            }
        }
        return results.stream().anyMatch(OverlayHandlingResult::detected);
    }

    private void runActionabilityCheck(WebElement target) {
        try {
            OverlayPolicyExecutor policyExecutor = overlayPolicy == null || overlayPolicy.isEmpty()
                    ? null
                    : new OverlayPolicyExecutor(driver, overlayPolicy, logger);
            ActionabilityReport report = new ActionabilityChecker(driver, policyExecutor, logger)
                    .check(target, ActionabilityOptions.defaults());
            if (report.isReady()) {
                return;
            }
            Optional<ActionabilityFailureReason> reason = report.firstFailure()
                    .map(result -> result.failureReason());
            if (reason.isPresent() && shouldRetryOverlayPolicy(reason.get())) {
                handleConfiguredOverlayPolicy();
            }
        } catch (RuntimeException ignored) {
            // Actionability is best-effort in the legacy smart click flow; existing fallbacks remain authoritative.
        }
    }

    private static boolean shouldRetryOverlayPolicy(ActionabilityFailureReason reason) {
        return reason == ActionabilityFailureReason.BLOCKING_OVERLAY_DETECTED
                || reason == ActionabilityFailureReason.ELEMENT_COVERED
                || reason == ActionabilityFailureReason.CLICK_POINT_NOT_RECEIVED;
    }

    private void emitClick(String method,
                           String label,
                           UiTestLensStatus status,
                           UiTestLensLogLevel level,
                           Throwable throwable,
                           boolean fallback,
                           String fallbackType,
                           boolean popupHandled) {
        try {
            UiTestLensLogEntry.Builder builder = UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(status == UiTestLensStatus.FAILED ? UiTestLensEventType.ERROR : UiTestLensEventType.ACTION)
                    .status(status)
                    .message("Click action " + method + " " + status)
                    .action(method)
                    .target(TargetDescriptor.label(label))
                    .metadata("method", method)
                    .metadata("label", label == null ? "" : label)
                    .metadata("fallback", String.valueOf(fallback))
                    .metadata("popupHandled", String.valueOf(popupHandled))
                    .throwable(throwable);
            if (fallbackType != null) {
                builder.metadata("fallbackType", fallbackType);
            }
            logger.emit(builder.build());
        } catch (Exception ignored) {}
    }

    private void emitRecoveryRetry(String label, Throwable failure, long durationNanos) {
        Throwable cause = failure;
        Throwable current = failure;
        java.util.Set<Throwable> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        while (current != null && seen.add(current)) {
            if (current instanceof ElementClickInterceptedException) cause = current;
            current = current.getCause();
        }
        try {
            logger.emit(UiTestLensLogEntry.builder()
                    .level(UiTestLensLogLevel.WARN)
                    .eventType(UiTestLensEventType.LOCATOR_RETRY)
                    .status(UiTestLensStatus.WARN)
                    .message("Retrying intercepted click")
                    .action("clickWithOverlayHandling")
                    .target(TargetDescriptor.label(label))
                    .metadata("retryKind", "recovery")
                    .metadata("retryAction", "click")
                    .metadata("retryLocator", label == null ? "" : label)
                    .metadata("attempt", "1")
                    .metadata("nextAttempt", "2")
                    .metadata("exceptionType", cause.getClass().getName())
                    .metadata("failedAttemptDurationNanos", String.valueOf(durationNanos))
                    .throwable(cause)
                    .build());
        } catch (RuntimeException ignored) {
            // Retry diagnostics must not alter click recovery.
        }
    }
}

