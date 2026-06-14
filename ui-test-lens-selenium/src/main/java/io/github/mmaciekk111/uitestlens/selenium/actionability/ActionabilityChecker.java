package io.github.mmaciekk111.uitestlens.selenium.actionability;

import io.github.mmaciekk111.uitestlens.core.OverlayLogger;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensEventType;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogLevel;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensStatus;
import io.github.mmaciekk111.uitestlens.selenium.overlay.OverlayHandlingResult;
import io.github.mmaciekk111.uitestlens.selenium.overlay.OverlayHandlingStatus;
import io.github.mmaciekk111.uitestlens.selenium.overlay.OverlayPolicyExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.LockSupport;

public final class ActionabilityChecker {
    private static final double BOUNDS_THRESHOLD_PX = 1.0;

    private final WebDriver driver;
    private final JavascriptExecutor javascriptExecutor;
    private final OverlayPolicyExecutor overlayPolicyExecutor;
    private final OverlayLogger logger;

    public ActionabilityChecker(WebDriver driver, OverlayPolicyExecutor overlayPolicyExecutor, OverlayLogger logger) {
        this.driver = Objects.requireNonNull(driver, "driver must not be null");
        if (!(driver instanceof JavascriptExecutor executor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.javascriptExecutor = executor;
        this.overlayPolicyExecutor = overlayPolicyExecutor;
        this.logger = logger != null ? logger : OverlayLogger.noop();
    }

    public ActionabilityReport check(By locator, ActionabilityOptions options) {
        ActionabilityOptions effectiveOptions = options != null ? options : ActionabilityOptions.defaults();
        Instant started = Instant.now();
        String selectorDescription = locator == null ? "" : locator.toString();
        emit(UiTestLensEventType.ACTIONABILITY_CHECK_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Actionability check started", selectorDescription, null);
        if (locator == null) {
            ActionabilityReport report = ActionabilityReport.of(List.of(ActionabilityResult.failed(
                    ActionabilityCheckType.ATTACHED,
                    ActionabilityFailureReason.ELEMENT_NOT_ATTACHED,
                    "Locator must not be null",
                    elapsed(started))));
            emitFinal(report, selectorDescription);
            return report;
        }
        List<WebElement> elements;
        try {
            elements = driver.findElements(locator);
        } catch (WebDriverException e) {
            ActionabilityReport report = ActionabilityReport.of(List.of(ActionabilityResult.failed(
                    ActionabilityCheckType.ATTACHED,
                    ActionabilityFailureReason.UNKNOWN,
                    "Could not resolve locator",
                    elapsed(started))));
            emitFailure(report.firstFailure(), selectorDescription);
            emitFinal(report, selectorDescription);
            return report;
        }
        if (elements == null || elements.isEmpty()) {
            ActionabilityReport report = ActionabilityReport.of(List.of(ActionabilityResult.notReady(
                    ActionabilityCheckType.ATTACHED,
                    ActionabilityFailureReason.ELEMENT_NOT_ATTACHED,
                    "Element was not found",
                    elapsed(started))));
            emitFailure(report.firstFailure(), selectorDescription);
            emitFinal(report, selectorDescription);
            return report;
        }
        return check(elements.get(0), effectiveOptions, selectorDescription);
    }

    public ActionabilityReport check(WebElement element, ActionabilityOptions options) {
        return check(element, options, "");
    }

    private ActionabilityReport check(WebElement element, ActionabilityOptions options, String selectorDescription) {
        ActionabilityOptions effectiveOptions = options != null ? options : ActionabilityOptions.defaults();
        Instant started = Instant.now();
        String elementDescription = describeElement(element);
        emit(UiTestLensEventType.ACTIONABILITY_CHECK_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Actionability check started", selectorDescription, elementDescription);

        List<ActionabilityResult> results = new ArrayList<>();
        if (element == null) {
            results.add(result(ActionabilityCheckType.ATTACHED, ActionabilityStatus.NOT_READY,
                    ActionabilityFailureReason.ELEMENT_NOT_ATTACHED, "Element must not be null", started,
                    selectorDescription, elementDescription, Map.of()));
            return finish(results, selectorDescription);
        }

        if (effectiveOptions.checkAttached()) {
            ActionabilityResult attached = checkAttached(element, started, selectorDescription, elementDescription);
            results.add(attached);
            if (!attached.ready()) {
                return finish(results, selectorDescription);
            }
        }
        if (effectiveOptions.checkVisible()) {
            ActionabilityResult visible = checkVisible(element, started, selectorDescription, elementDescription);
            results.add(visible);
            if (!visible.ready()) {
                return finish(results, selectorDescription);
            }
        }
        if (effectiveOptions.checkEnabled()) {
            ActionabilityResult enabled = checkEnabled(element, started, selectorDescription, elementDescription);
            results.add(enabled);
            if (!enabled.ready()) {
                return finish(results, selectorDescription);
            }
        }
        if (effectiveOptions.scrollIntoView()) {
            ActionabilityResult scrolled = scrollIntoView(element, started, selectorDescription, elementDescription);
            results.add(scrolled);
            if (!scrolled.ready()) {
                return finish(results, selectorDescription);
            }
        }
        if (effectiveOptions.checkStableBounds()) {
            ActionabilityResult stable = checkStableBounds(element, effectiveOptions, started, selectorDescription, elementDescription);
            results.add(stable);
            if (!stable.ready()) {
                return finish(results, selectorDescription);
            }
        }
        if (effectiveOptions.checkReceivesClickPoint()) {
            ActionabilityResult clickPoint = checkClickPoint(element, started, selectorDescription, elementDescription);
            results.add(clickPoint);
            if (!clickPoint.ready()) {
                return finish(results, selectorDescription);
            }
        }
        if (effectiveOptions.checkOverlayPolicy()) {
            results.add(checkOverlayPolicy(started, selectorDescription, elementDescription));
        }
        return finish(results, selectorDescription);
    }

    private ActionabilityResult checkAttached(WebElement element,
                                              Instant started,
                                              String selectorDescription,
                                              String elementDescription) {
        try {
            Object attached = javascriptExecutor.executeScript(ActionabilityScripts.IS_ATTACHED, element);
            if (Boolean.TRUE.equals(attached)) {
                return result(ActionabilityCheckType.ATTACHED, ActionabilityStatus.READY, null,
                        "Element is attached", started, selectorDescription, elementDescription, Map.of());
            }
            return result(ActionabilityCheckType.ATTACHED, ActionabilityStatus.NOT_READY,
                    ActionabilityFailureReason.ELEMENT_NOT_ATTACHED, "Element is detached", started,
                    selectorDescription, elementDescription, Map.of());
        } catch (StaleElementReferenceException e) {
            return result(ActionabilityCheckType.ATTACHED, ActionabilityStatus.NOT_READY,
                    ActionabilityFailureReason.STALE_ELEMENT, "Element reference is stale", started,
                    selectorDescription, elementDescription, Map.of());
        } catch (WebDriverException e) {
            return result(ActionabilityCheckType.ATTACHED, ActionabilityStatus.FAILED,
                    ActionabilityFailureReason.JAVASCRIPT_ERROR, "Could not check element attachment", started,
                    selectorDescription, elementDescription, Map.of("error", safeMessage(e)));
        }
    }

    private ActionabilityResult checkVisible(WebElement element,
                                             Instant started,
                                             String selectorDescription,
                                             String elementDescription) {
        try {
            if (element.isDisplayed()) {
                return result(ActionabilityCheckType.VISIBLE, ActionabilityStatus.READY, null,
                        "Element is visible", started, selectorDescription, elementDescription, Map.of());
            }
            return result(ActionabilityCheckType.VISIBLE, ActionabilityStatus.NOT_READY,
                    ActionabilityFailureReason.ELEMENT_NOT_VISIBLE, "Element is not visible", started,
                    selectorDescription, elementDescription, Map.of());
        } catch (StaleElementReferenceException e) {
            return result(ActionabilityCheckType.VISIBLE, ActionabilityStatus.NOT_READY,
                    ActionabilityFailureReason.STALE_ELEMENT, "Element reference is stale", started,
                    selectorDescription, elementDescription, Map.of());
        }
    }

    private ActionabilityResult checkEnabled(WebElement element,
                                             Instant started,
                                             String selectorDescription,
                                             String elementDescription) {
        try {
            if (element.isEnabled()) {
                return result(ActionabilityCheckType.ENABLED, ActionabilityStatus.READY, null,
                        "Element is enabled", started, selectorDescription, elementDescription, Map.of());
            }
            return result(ActionabilityCheckType.ENABLED, ActionabilityStatus.NOT_READY,
                    ActionabilityFailureReason.ELEMENT_NOT_ENABLED, "Element is disabled", started,
                    selectorDescription, elementDescription, Map.of());
        } catch (StaleElementReferenceException e) {
            return result(ActionabilityCheckType.ENABLED, ActionabilityStatus.NOT_READY,
                    ActionabilityFailureReason.STALE_ELEMENT, "Element reference is stale", started,
                    selectorDescription, elementDescription, Map.of());
        }
    }

    private ActionabilityResult scrollIntoView(WebElement element,
                                               Instant started,
                                               String selectorDescription,
                                               String elementDescription) {
        try {
            javascriptExecutor.executeScript(ActionabilityScripts.SCROLL_INTO_VIEW, element);
            Map<String, Object> rect = rect(element);
            if (Boolean.TRUE.equals(rect.get("inViewport"))) {
                return result(ActionabilityCheckType.SCROLL_INTO_VIEW, ActionabilityStatus.READY, null,
                        "Element was scrolled into viewport", started, selectorDescription, elementDescription, rect);
            }
            return result(ActionabilityCheckType.SCROLL_INTO_VIEW, ActionabilityStatus.NOT_READY,
                    ActionabilityFailureReason.ELEMENT_OUTSIDE_VIEWPORT, "Element is outside viewport", started,
                    selectorDescription, elementDescription, rect);
        } catch (WebDriverException e) {
            return result(ActionabilityCheckType.SCROLL_INTO_VIEW, ActionabilityStatus.FAILED,
                    ActionabilityFailureReason.JAVASCRIPT_ERROR, "Could not scroll element into viewport", started,
                    selectorDescription, elementDescription, Map.of("error", safeMessage(e)));
        }
    }

    private ActionabilityResult checkStableBounds(WebElement element,
                                                  ActionabilityOptions options,
                                                  Instant started,
                                                  String selectorDescription,
                                                  String elementDescription) {
        try {
            Map<String, Object> previous = rect(element);
            for (int i = 1; i < options.stableBoundsSamples(); i++) {
                pause(options.stableBoundsSampleDelay());
                Map<String, Object> current = rect(element);
                if (!sameRect(previous, current)) {
                    return result(ActionabilityCheckType.STABLE_BOUNDS, ActionabilityStatus.NOT_READY,
                            ActionabilityFailureReason.ELEMENT_NOT_STABLE, "Element bounding box changed", started,
                            selectorDescription, elementDescription, Map.of("previous", shortRect(previous), "current", shortRect(current)));
                }
                previous = current;
            }
            return result(ActionabilityCheckType.STABLE_BOUNDS, ActionabilityStatus.READY, null,
                    "Element bounding box is stable", started, selectorDescription, elementDescription, shortRect(previous));
        } catch (WebDriverException e) {
            return result(ActionabilityCheckType.STABLE_BOUNDS, ActionabilityStatus.FAILED,
                    ActionabilityFailureReason.JAVASCRIPT_ERROR, "Could not sample element bounding box", started,
                    selectorDescription, elementDescription, Map.of("error", safeMessage(e)));
        }
    }

    private ActionabilityResult checkClickPoint(WebElement element,
                                                Instant started,
                                                String selectorDescription,
                                                String elementDescription) {
        try {
            Object response = javascriptExecutor.executeScript(ActionabilityScripts.CLICK_POINT, element);
            if (!(response instanceof Map<?, ?> raw)) {
                return result(ActionabilityCheckType.RECEIVES_CLICK_POINT, ActionabilityStatus.FAILED,
                        ActionabilityFailureReason.JAVASCRIPT_ERROR, "Click point script returned an unexpected value", started,
                        selectorDescription, elementDescription, Map.of());
            }
            Map<String, Object> details = toStringObjectMap(raw);
            if (Boolean.TRUE.equals(details.get("receives"))) {
                return result(ActionabilityCheckType.RECEIVES_CLICK_POINT, ActionabilityStatus.READY, null,
                        "Element receives click point", started, selectorDescription, elementDescription, details);
            }
            String topElement = String.valueOf(details.getOrDefault("topElement", ""));
            return result(ActionabilityCheckType.RECEIVES_CLICK_POINT, ActionabilityStatus.NOT_READY,
                    ActionabilityFailureReason.ELEMENT_COVERED,
                    topElement.isBlank() ? "Click point is not received by the target element"
                            : "Click point is covered by " + topElement,
                    started, selectorDescription, elementDescription, details);
        } catch (WebDriverException e) {
            return result(ActionabilityCheckType.RECEIVES_CLICK_POINT, ActionabilityStatus.FAILED,
                    ActionabilityFailureReason.JAVASCRIPT_ERROR, "Could not check click point", started,
                    selectorDescription, elementDescription, Map.of("error", safeMessage(e)));
        }
    }

    private ActionabilityResult checkOverlayPolicy(Instant started,
                                                   String selectorDescription,
                                                   String elementDescription) {
        if (overlayPolicyExecutor == null) {
            return result(ActionabilityCheckType.OVERLAY_POLICY, ActionabilityStatus.SKIPPED, null,
                    "No overlay policy executor configured", started, selectorDescription, elementDescription, Map.of());
        }
        try {
            List<OverlayHandlingResult> results = overlayPolicyExecutor.handleKnownOverlays();
            Optional<OverlayHandlingResult> failed = results.stream()
                    .filter(result -> result.status() == OverlayHandlingStatus.FAILED)
                    .findFirst();
            if (failed.isPresent()) {
                return result(ActionabilityCheckType.OVERLAY_POLICY, ActionabilityStatus.FAILED,
                        ActionabilityFailureReason.BLOCKING_OVERLAY_DETECTED,
                        "Overlay policy failed for " + failed.get().handlerName(), started,
                        selectorDescription, elementDescription, Map.of("handler", failed.get().handlerName()));
            }
            Optional<OverlayHandlingResult> stillVisible = results.stream()
                    .filter(result -> result.status() == OverlayHandlingStatus.STILL_VISIBLE)
                    .findFirst();
            if (stillVisible.isPresent()) {
                return result(ActionabilityCheckType.OVERLAY_POLICY, ActionabilityStatus.NOT_READY,
                        ActionabilityFailureReason.BLOCKING_OVERLAY_DETECTED,
                        "Overlay is still visible for " + stillVisible.get().handlerName(), started,
                        selectorDescription, elementDescription, Map.of("handler", stillVisible.get().handlerName()));
            }
            boolean handled = results.stream().anyMatch(OverlayHandlingResult::detected);
            return result(ActionabilityCheckType.OVERLAY_POLICY, ActionabilityStatus.READY, null,
                    handled ? "Overlay policy handled known overlays" : "No known blocking overlay detected",
                    started, selectorDescription, elementDescription, Map.of("handled", handled));
        } catch (RuntimeException e) {
            return result(ActionabilityCheckType.OVERLAY_POLICY, ActionabilityStatus.FAILED,
                    ActionabilityFailureReason.BLOCKING_OVERLAY_DETECTED, "Overlay policy execution failed", started,
                    selectorDescription, elementDescription, Map.of("error", safeMessage(e)));
        }
    }

    private ActionabilityReport finish(List<ActionabilityResult> results, String selectorDescription) {
        ActionabilityReport report = ActionabilityReport.of(results);
        emitFailure(report.firstFailure(), selectorDescription);
        emitFinal(report, selectorDescription);
        return report;
    }

    private ActionabilityResult result(ActionabilityCheckType type,
                                       ActionabilityStatus status,
                                       ActionabilityFailureReason failureReason,
                                       String message,
                                       Instant started,
                                       String selectorDescription,
                                       String elementDescription,
                                       Map<String, Object> details) {
        return ActionabilityResult.builder(type, status)
                .failureReason(failureReason)
                .message(message)
                .elapsed(elapsed(started))
                .selectorDescription(selectorDescription)
                .elementDescription(elementDescription)
                .details(details)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> rect(WebElement element) {
        Object response = javascriptExecutor.executeScript(ActionabilityScripts.BOUNDING_RECT, element);
        if (!(response instanceof Map<?, ?> raw)) {
            throw new WebDriverException("Bounding rect script returned an unexpected value");
        }
        return toStringObjectMap(raw);
    }

    private static Map<String, Object> toStringObjectMap(Map<?, ?> raw) {
        java.util.LinkedHashMap<String, Object> copy = new java.util.LinkedHashMap<>();
        raw.forEach((key, value) -> copy.put(String.valueOf(key), value));
        return copy;
    }

    private static boolean sameRect(Map<String, Object> previous, Map<String, Object> current) {
        return close(previous, current, "x")
                && close(previous, current, "y")
                && close(previous, current, "width")
                && close(previous, current, "height");
    }

    private static boolean close(Map<String, Object> previous, Map<String, Object> current, String key) {
        return Math.abs(number(previous.get(key)) - number(current.get(key))) <= BOUNDS_THRESHOLD_PX;
    }

    private static Map<String, Object> shortRect(Map<String, Object> rect) {
        return Map.of(
                "x", rect.get("x"),
                "y", rect.get("y"),
                "width", rect.get("width"),
                "height", rect.get("height"),
                "inViewport", rect.get("inViewport")
        );
    }

    private static double number(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0.0d;
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private static void pause(Duration delay) {
        if (delay != null && !delay.isZero() && !delay.isNegative()) {
            LockSupport.parkNanos(delay.toNanos());
        }
    }

    private static Duration elapsed(Instant started) {
        return Duration.between(started, Instant.now());
    }

    private String describeElement(WebElement element) {
        if (element == null) {
            return "";
        }
        String description = String.valueOf(element);
        return description.length() > 160 ? description.substring(0, 160) : description;
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null) {
            return throwable.getClass().getSimpleName();
        }
        return message.length() > 160 ? message.substring(0, 160) : message;
    }

    private void emitFailure(Optional<ActionabilityResult> failure, String selectorDescription) {
        failure.ifPresent(result -> emit(UiTestLensEventType.ACTIONABILITY_CHECK_FAILED,
                result.status() == ActionabilityStatus.FAILED ? UiTestLensStatus.FAILED : UiTestLensStatus.WARN,
                result.status() == ActionabilityStatus.FAILED ? UiTestLensLogLevel.ERROR : UiTestLensLogLevel.WARN,
                result.message(), selectorDescription, result.elementDescription()));
    }

    private void emitFinal(ActionabilityReport report, String selectorDescription) {
        UiTestLensEventType eventType = report.isReady()
                ? UiTestLensEventType.ACTIONABILITY_READY
                : UiTestLensEventType.ACTIONABILITY_NOT_READY;
        UiTestLensStatus status = report.isReady() ? UiTestLensStatus.PASSED : UiTestLensStatus.WARN;
        UiTestLensLogLevel level = report.isReady() ? UiTestLensLogLevel.INFO : UiTestLensLogLevel.WARN;
        emit(eventType, status, level, report.summary(), selectorDescription, null);
    }

    private void emit(UiTestLensEventType eventType,
                      UiTestLensStatus status,
                      UiTestLensLogLevel level,
                      String message,
                      String selectorDescription,
                      String elementDescription) {
        try {
            UiTestLensLogEntry.Builder builder = UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(eventType)
                    .status(status)
                    .message(message)
                    .action("actionability.check");
            if (selectorDescription != null && !selectorDescription.isBlank()) {
                builder.metadata("selector", selectorDescription);
            }
            if (elementDescription != null && !elementDescription.isBlank()) {
                builder.metadata("element", elementDescription);
            }
            logger.emit(builder.build());
        } catch (Exception ignored) {
        }
    }
}
