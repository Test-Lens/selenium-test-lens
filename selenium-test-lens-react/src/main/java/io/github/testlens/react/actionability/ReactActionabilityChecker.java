package io.github.testlens.react.actionability;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.selenium.actionability.ActionabilityReport;
import io.github.testlens.selenium.actionability.ActionabilityStatus;
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

public final class ReactActionabilityChecker {
    private final JsOverlayDebug overlay;
    private final WebDriver driver;
    private final JavascriptExecutor javascriptExecutor;

    public ReactActionabilityChecker(JsOverlayDebug overlay) {
        this.overlay = Objects.requireNonNull(overlay, "overlay must not be null");
        this.driver = overlay.getDriver();
        if (!(driver instanceof JavascriptExecutor executor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.javascriptExecutor = executor;
    }

    public ReactActionabilityReport check(By locator, ReactActionabilityOptions options) {
        ReactActionabilityOptions effectiveOptions = options != null ? options : ReactActionabilityOptions.defaults();
        ActionabilityReport baseReport = overlay.checkActionability(locator, effectiveOptions.baseOptions());
        if (!baseReport.isReady()) {
            return ReactActionabilityReport.of(baseReport, List.of(ReactReadinessResult.notReady(
                    ReactReadinessCheckType.BASE_ACTIONABILITY,
                    ReactReadinessFailureReason.BASE_ACTIONABILITY_NOT_READY,
                    baseReport.summary(),
                    Duration.ZERO
            )));
        }
        try {
            List<WebElement> elements = driver.findElements(locator);
            if (elements == null || elements.isEmpty()) {
                return ReactActionabilityReport.of(baseReport, List.of(ReactReadinessResult.notReady(
                        ReactReadinessCheckType.STALE_AFTER_RESOLVE,
                        ReactReadinessFailureReason.STALE_NODE,
                        "Element disappeared after base actionability check",
                        Duration.ZERO
                )));
            }
            return checkReactReadiness(baseReport, elements.get(0), effectiveOptions);
        } catch (StaleElementReferenceException e) {
            return ReactActionabilityReport.of(baseReport, List.of(ReactReadinessResult.notReady(
                    ReactReadinessCheckType.STALE_AFTER_RESOLVE,
                    ReactReadinessFailureReason.STALE_NODE,
                    "Element became stale after base actionability check",
                    Duration.ZERO
            )));
        }
    }

    public ReactActionabilityReport check(WebElement element, ReactActionabilityOptions options) {
        ReactActionabilityOptions effectiveOptions = options != null ? options : ReactActionabilityOptions.defaults();
        ActionabilityReport baseReport = overlay.checkActionability(element, effectiveOptions.baseOptions());
        if (!baseReport.isReady()) {
            return ReactActionabilityReport.of(baseReport, List.of(ReactReadinessResult.notReady(
                    ReactReadinessCheckType.BASE_ACTIONABILITY,
                    ReactReadinessFailureReason.BASE_ACTIONABILITY_NOT_READY,
                    baseReport.summary(),
                    Duration.ZERO
            )));
        }
        return checkReactReadiness(baseReport, element, effectiveOptions);
    }

    private ReactActionabilityReport checkReactReadiness(ActionabilityReport baseReport,
                                                         WebElement element,
                                                         ReactActionabilityOptions options) {
        Instant started = Instant.now();
        List<ReactReadinessResult> results = new ArrayList<>();
        Map<String, Object> signals;
        try {
            Object response = javascriptExecutor.executeScript(ReactReadinessScripts.READINESS, element);
            if (!(response instanceof Map<?, ?> raw)) {
                return ReactActionabilityReport.of(baseReport, List.of(ReactReadinessResult.failed(
                        ReactReadinessCheckType.BASE_ACTIONABILITY,
                        ReactReadinessFailureReason.JAVASCRIPT_ERROR,
                        "React readiness script returned an unexpected value",
                        elapsed(started)
                )));
            }
            signals = toStringObjectMap(raw);
        } catch (StaleElementReferenceException e) {
            return ReactActionabilityReport.of(baseReport, List.of(ReactReadinessResult.notReady(
                    ReactReadinessCheckType.STALE_AFTER_RESOLVE,
                    ReactReadinessFailureReason.STALE_NODE,
                    "Element became stale while checking React readiness",
                    elapsed(started)
            )));
        } catch (WebDriverException e) {
            return ReactActionabilityReport.of(baseReport, List.of(ReactReadinessResult.failed(
                    ReactReadinessCheckType.BASE_ACTIONABILITY,
                    ReactReadinessFailureReason.JAVASCRIPT_ERROR,
                    "Could not execute React readiness script",
                    elapsed(started)
            )));
        }

        addAttributeResult(results, options.checkAriaDisabled(), signals, "ariaDisabled",
                ReactReadinessCheckType.ARIA_DISABLED, ReactReadinessFailureReason.ARIA_DISABLED_TRUE,
                "aria-disabled=true is active", started);
        addAttributeResult(results, options.checkAriaBusy(), signals, "ariaBusy",
                ReactReadinessCheckType.ARIA_BUSY, ReactReadinessFailureReason.ARIA_BUSY_TRUE,
                "aria-busy=true is active", started);
        addAttributeResult(results, options.checkDataLoading(), signals, "dataLoading",
                ReactReadinessCheckType.DATA_LOADING, ReactReadinessFailureReason.DATA_LOADING_ACTIVE,
                "React loading data attribute is active", started);
        addAttributeResult(results, options.checkDataPending(), signals, "dataPending",
                ReactReadinessCheckType.DATA_PENDING, ReactReadinessFailureReason.DATA_PENDING_ACTIVE,
                "React pending data attribute is active", started);
        addVisibleGlobalResult(results, options.checkProgressbar(), signals, "progressbar",
                ReactReadinessCheckType.PROGRESSBAR_PRESENT, ReactReadinessFailureReason.PROGRESSBAR_BLOCKING,
                "Visible progressbar is present", started);
        addVisibleGlobalResult(results, options.checkSpinner(), signals, "spinner",
                ReactReadinessCheckType.SPINNER_PRESENT, ReactReadinessFailureReason.SPINNER_BLOCKING,
                "Visible spinner/loading indicator is present", started);
        addVisibleGlobalResult(results, options.checkSkeleton(), signals, "skeleton",
                ReactReadinessCheckType.SKELETON_PRESENT, ReactReadinessFailureReason.SKELETON_BLOCKING,
                "Visible skeleton loader is present", started);
        addVisibleGlobalResult(results, options.checkFocusLock(), signals, "focusLock",
                ReactReadinessCheckType.FOCUS_LOCK_ACTIVE, ReactReadinessFailureReason.FOCUS_LOCK_BLOCKING,
                "Visible focus-lock overlay is active", started);
        addVisibleGlobalResult(results, options.checkDialogOrModal(), signals, "dialogOrModal",
                ReactReadinessCheckType.DIALOG_OR_MODAL_ACTIVE, ReactReadinessFailureReason.DIALOG_BLOCKING,
                "Visible dialog or modal is active", started);
        addCustomLocatorResults(results, options.customBusyIndicators(), ReactReadinessCheckType.SPINNER_PRESENT,
                ReactReadinessFailureReason.SPINNER_BLOCKING, "Custom busy indicator is visible", started);
        addCustomLocatorResults(results, options.customBlockingOverlays(), ReactReadinessCheckType.DIALOG_OR_MODAL_ACTIVE,
                ReactReadinessFailureReason.DIALOG_BLOCKING, "Custom blocking overlay is visible", started);

        return ReactActionabilityReport.of(baseReport, results);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> nestedMap(Map<String, Object> signals, String key) {
        Object value = signals.get(key);
        if (value instanceof Map<?, ?> raw) {
            return toStringObjectMap(raw);
        }
        return Map.of();
    }

    private void addAttributeResult(List<ReactReadinessResult> results,
                                    boolean enabled,
                                    Map<String, Object> signals,
                                    String key,
                                    ReactReadinessCheckType checkType,
                                    ReactReadinessFailureReason failureReason,
                                    String failureMessage,
                                    Instant started) {
        if (!enabled) {
            results.add(ReactReadinessResult.skipped(checkType, "Check disabled", elapsed(started)));
            return;
        }
        Map<String, Object> signal = nestedMap(signals, key);
        if (Boolean.TRUE.equals(signal.get("active"))) {
            results.add(ReactReadinessResult.builder(checkType, ActionabilityStatus.NOT_READY)
                    .failureReason(failureReason)
                    .message(failureMessage)
                    .elapsed(elapsed(started))
                    .details(signal)
                    .build());
            return;
        }
        results.add(ReactReadinessResult.ready(checkType, "React readiness signal is not active", elapsed(started)));
    }

    private void addVisibleGlobalResult(List<ReactReadinessResult> results,
                                        boolean enabled,
                                        Map<String, Object> signals,
                                        String key,
                                        ReactReadinessCheckType checkType,
                                        ReactReadinessFailureReason failureReason,
                                        String failureMessage,
                                        Instant started) {
        if (!enabled) {
            results.add(ReactReadinessResult.skipped(checkType, "Check disabled", elapsed(started)));
            return;
        }
        String blocker = stringValue(signals.get(key));
        if (!blocker.isBlank()) {
            results.add(ReactReadinessResult.builder(checkType, ActionabilityStatus.NOT_READY)
                    .failureReason(failureReason)
                    .message(failureMessage + ": " + blocker)
                    .elapsed(elapsed(started))
                    .detail("blocker", blocker)
                    .build());
            return;
        }
        results.add(ReactReadinessResult.ready(checkType, "No blocking React readiness signal detected", elapsed(started)));
    }

    private void addCustomLocatorResults(List<ReactReadinessResult> results,
                                         List<By> locators,
                                         ReactReadinessCheckType checkType,
                                         ReactReadinessFailureReason failureReason,
                                         String failureMessage,
                                         Instant started) {
        for (By locator : locators) {
            boolean visible = false;
            try {
                visible = driver.findElements(locator).stream().anyMatch(WebElement::isDisplayed);
            } catch (RuntimeException ignored) {
                // Keep custom diagnostics best-effort; failed custom locators should not hide other results.
            }
            if (visible) {
                results.add(ReactReadinessResult.builder(checkType, ActionabilityStatus.NOT_READY)
                        .failureReason(failureReason)
                        .message(failureMessage)
                        .elapsed(elapsed(started))
                        .detail("locator", locator.toString())
                        .build());
            } else {
                results.add(ReactReadinessResult.ready(checkType, "Custom locator is not visible", elapsed(started)));
            }
        }
    }

    private static Map<String, Object> toStringObjectMap(Map<?, ?> raw) {
        java.util.LinkedHashMap<String, Object> copy = new java.util.LinkedHashMap<>();
        raw.forEach((key, value) -> copy.put(String.valueOf(key), value));
        return copy;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Duration elapsed(Instant started) {
        return Duration.between(started, Instant.now());
    }
}

