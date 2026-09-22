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
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SmartClickActions {

    private final WebDriver driver;
    private final OverlayConfig config;
    private final HighlightActions highlightActions;
    private final BlockingOverlayHelper blockingHelper;
    private final BlockingOverlayHelper silentBlockingHelper;
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
        this.silentBlockingHelper = new BlockingOverlayHelper(driver, config, rootManager, null);
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
     * Internal configured path used by {@code UiLocator.click()}. The boolean is deliberately per invocation:
     * the public two-argument legacy path above retains its historical physical-click behavior.
     */
    protected final void clickWithOverlayHandling(WebElement target,
                                                  String label,
                                                  boolean javascriptClickFallback) {
        if (target == null) {
            throw new IllegalArgumentException("Click target must not be null");
        }

        long startedNanos = System.nanoTime();
        List<RuntimeException> deterministicFailures = new ArrayList<>();
        requireLogicalClickTarget(target);
        prepareConfiguredOverlays();
        decorateClickTargetIfAbsent(target, label);

        try {
            target.click();
            debugStrategy("NATIVE", "dispatched", null);
            return;
        } catch (RuntimeException failure) {
            if (!isSafeBeforeDispatch(failure)) {
                debugStrategy("NATIVE", "ambiguous-failure", failure);
                throw failure;
            }
            deterministicFailures.add(failure);
            debugStrategy("NATIVE", strategyReason(failure), failure);
            recoverKnownOverlayAfterInterception(target, label);
        }

        requireLogicalClickTarget(target);
        try {
            new Actions(driver).moveToElement(target).perform();
            HitPoint center = centerHitPoint(target);
            if (center != null && center.owned()) {
                try {
                    new Actions(driver).moveToElement(target).click().perform();
                    debugStrategy("ACTIONS", "dispatched", null);
                    return;
                } catch (RuntimeException failure) {
                    if (!isSafeBeforeDispatch(failure)) {
                        debugStrategy("ACTIONS", "ambiguous-failure", failure);
                        throw failure;
                    }
                    deterministicFailures.add(failure);
                    debugStrategy("ACTIONS", strategyReason(failure), failure);
                }
            } else {
                debugStrategy("ACTIONS", center == null ? "no-center" : center.reason(), null);
            }
        } catch (MoveTargetOutOfBoundsException | ElementNotInteractableException safeMoveFailure) {
            deterministicFailures.add(safeMoveFailure);
            debugStrategy("ACTIONS", strategyReason(safeMoveFailure), safeMoveFailure);
        }

        requireLogicalClickTarget(target);
        List<HitPoint> points = validHitPoints(target);
        if (points.isEmpty()) {
            debugStrategy("POINT", "no-valid-point", null);
        } else {
            HitPoint point = points.get(0);
            try {
                new Actions(driver).moveToLocation(point.roundedX(), point.roundedY()).click().perform();
                debugStrategy("POINT", "dispatched", null);
                return;
            } catch (RuntimeException failure) {
                if (!isSafeBeforeDispatch(failure)) {
                    debugStrategy("POINT", "ambiguous-failure", failure);
                    throw failure;
                }
                deterministicFailures.add(failure);
                debugStrategy("POINT", strategyReason(failure), failure);
            }
        }

        if (javascriptClickFallback) {
            requireLogicalClickTarget(target);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", target);
            debugStrategy("JS", "dispatched", null);
            return;
        }

        ElementClickInterceptedException exhausted = new ElementClickInterceptedException(
                "Smart click exhausted physical strategies; JavaScript fallback is disabled"
                        + " | order=NATIVE,ACTIONS,POINT"
                        + " | elapsedMs=" + ((System.nanoTime() - startedNanos) / 1_000_000L));
        for (RuntimeException failure : deterministicFailures) {
            if (failure != exhausted) exhausted.addSuppressed(failure);
        }
        debugStrategy("NONE", "physical-strategies-exhausted", exhausted);
        throw exhausted;
    }

    private void prepareConfiguredOverlays() {
        handleConfiguredOverlayPolicy();
        try {
            silentBlockingHelper.handleGlobalOverlayIfPresent("OVERLAY", "CLOSE");
        } catch (RuntimeException observabilityFailure) {
            debugStrategy("PREPARE", "best-effort-overlay-probe-failed", observabilityFailure);
        }
    }

    private void recoverKnownOverlayAfterInterception(WebElement target, String label) {
        try {
            boolean handled = handleConfiguredOverlayPolicy();
            if (!handled) {
                silentBlockingHelper.handleBlockingOverlayFor(target, "BLOCKING OVERLAY", "CLOSE");
            }
        } catch (RuntimeException recoveryFailure) {
            debugStrategy("OVERLAY", "recovery-failed", recoveryFailure);
        }
    }

    private void requireLogicalClickTarget(WebElement target) {
        if (!target.isDisplayed()) {
            throw new ElementNotInteractableException("Smart click target is hidden");
        }
        String disabled = target.getDomAttribute("disabled");
        if (!target.isEnabled()
                || (disabled != null && !disabled.isBlank() && !"false".equalsIgnoreCase(disabled))
                || "true".equalsIgnoreCase(target.getDomAttribute("aria-disabled"))) {
            throw new ElementNotInteractableException("Smart click target is disabled");
        }
        try {
            Object connected = ((JavascriptExecutor) driver).executeScript(
                    "return arguments[0] == null ? false : arguments[0].isConnected !== false;", target);
            if (Boolean.FALSE.equals(connected)) {
                throw new StaleElementReferenceException("Smart click target is detached");
            }
        } catch (StaleElementReferenceException stale) {
            throw stale;
        } catch (WebDriverException unavailableScriptCheck) {
            debugStrategy("PREPARE", "connected-check-unavailable", unavailableScriptCheck);
        }
    }

    private HitPoint centerHitPoint(WebElement target) {
        Object value = ((JavascriptExecutor) driver).executeScript(HIT_TEST_SCRIPT, target, "CENTER");
        return toHitPoint(value);
    }

    private List<HitPoint> validHitPoints(WebElement target) {
        Object value = ((JavascriptExecutor) driver).executeScript(HIT_TEST_SCRIPT, target, "POINTS");
        if (!(value instanceof List<?> rawPoints)) return List.of();
        List<HitPoint> points = new ArrayList<>();
        for (Object rawPoint : rawPoints) {
            HitPoint point = toHitPoint(rawPoint);
            if (point != null && point.owned()) points.add(point);
        }
        return points;
    }

    private static HitPoint toHitPoint(Object value) {
        if (!(value instanceof Map<?, ?> map)) return null;
        Object x = map.get("x");
        Object y = map.get("y");
        if (!(x instanceof Number) || !(y instanceof Number)) return null;
        return new HitPoint(((Number) x).doubleValue(), ((Number) y).doubleValue(),
                Boolean.TRUE.equals(map.get("owned")), limited(String.valueOf(map.get("reason"))));
    }

    private static boolean isSafeBeforeDispatch(Throwable failure) {
        return failure instanceof ElementClickInterceptedException
                || failure instanceof ElementNotInteractableException
                || failure instanceof MoveTargetOutOfBoundsException
                || failure instanceof StaleElementReferenceException;
    }

    private static String strategyReason(Throwable failure) {
        if (failure instanceof ElementClickInterceptedException) return "intercepted";
        if (failure instanceof MoveTargetOutOfBoundsException) return "point-outside-viewport";
        if (failure instanceof StaleElementReferenceException) return "stale-before-dispatch";
        if (failure instanceof ElementNotInteractableException) return "not-interactable-before-dispatch";
        return failure == null ? "skipped" : "failure";
    }

    private void debugStrategy(String strategy, String result, Throwable failure) {
        String message = "Smart click strategy=" + strategy + " result=" + limited(result);
        if (failure == null) logger.debug(message);
        else logger.debug(message + " exceptionType=" + failure.getClass().getName());
    }

    private static String limited(String value) {
        if (value == null || "null".equals(value)) return "";
        String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
    }

    private record HitPoint(double x, double y, boolean owned, String reason) {
        int roundedX() { return (int) Math.round(x); }
        int roundedY() { return (int) Math.round(y); }
    }

    private static final String HIT_TEST_SCRIPT = """
            var target=arguments[0], mode=arguments[1];
            function topAt(x,y){
              var node=document.elementFromPoint(x,y), next=node;
              while(next && next.shadowRoot && next.shadowRoot.elementFromPoint){
                var inner=next.shadowRoot.elementFromPoint(x,y);
                if(!inner || inner===next) break;
                node=inner; next=inner;
              }
              return node;
            }
            function result(x,y){
              var hit=topAt(x,y);
              var owned=!!hit && (hit===target || target.contains(hit));
              var blocker=hit && !owned ? hit : null;
              var reason=owned ? 'target-owned' : (blocker ? 'hit-test-mismatch:'
                +(blocker.tagName||'').toLowerCase()
                +(blocker.id ? '#'+String(blocker.id).slice(0,48) : '') : 'no-hit-target');
              return {x:x,y:y,owned:owned,reason:reason};
            }
            if(!target || !target.isConnected || !target.getClientRects) return mode==='POINTS' ? [] : null;
            var viewportWidth=document.documentElement.clientWidth, viewportHeight=document.documentElement.clientHeight;
            var rects=Array.from(target.getClientRects()).map(function(rect){
              return {left:Math.max(0,rect.left),top:Math.max(0,rect.top),
                right:Math.min(viewportWidth,rect.right),bottom:Math.min(viewportHeight,rect.bottom)};
            }).filter(function(rect){return rect.right-rect.left>1 && rect.bottom-rect.top>1;});
            if(!rects.length) return mode==='POINTS' ? [] : null;
            var rect=rects[0], cx=(rect.left+rect.right)/2, cy=(rect.top+rect.bottom)/2;
            if(mode==='CENTER') return result(cx,cy);
            var raw=[];
            rects.forEach(function(r){
              var width=r.right-r.left,height=r.bottom-r.top;
              var ix=Math.min(Math.max(2,width*0.2),Math.max(0.5,width/2));
              var iy=Math.min(Math.max(2,height*0.2),Math.max(0.5,height/2));
              raw.push([(r.left+r.right)/2,(r.top+r.bottom)/2],
                [r.left+ix,(r.top+r.bottom)/2],[r.right-ix,(r.top+r.bottom)/2],
                [(r.left+r.right)/2,r.top+iy],[(r.left+r.right)/2,r.bottom-iy],
                [r.left+ix,r.top+iy],[r.right-ix,r.top+iy],
                [r.left+ix,r.bottom-iy],[r.right-ix,r.bottom-iy]);
            });
            var seen=new Set(), output=[];
            raw.forEach(function(point){
              var key=Math.round(point[0]*10)+':'+Math.round(point[1]*10);
              if(seen.has(key)) return; seen.add(key);
              var checked=result(point[0],point[1]);
              if(checked.owned) output.push(checked);
            });
            return output;
            """;

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
            highlightActions.automaticAction(target, label);
        }
    }

    private void decorateClickTargetIfAbsent(WebElement target, String label) {
        if (config.isEnabled()) {
            highlightActions.automaticActionIfAbsent(target, label);
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

