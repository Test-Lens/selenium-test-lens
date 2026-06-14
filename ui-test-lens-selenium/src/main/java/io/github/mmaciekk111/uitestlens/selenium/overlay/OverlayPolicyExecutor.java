package io.github.mmaciekk111.uitestlens.selenium.overlay;

import io.github.mmaciekk111.uitestlens.core.OverlayLogger;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensEventType;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogLevel;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensStatus;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class OverlayPolicyExecutor {
    private final WebDriver driver;
    private final OverlayPolicy policy;
    private final OverlayLogger logger;

    public OverlayPolicyExecutor(WebDriver driver, OverlayPolicy policy, OverlayLogger logger) {
        this.driver = Objects.requireNonNull(driver, "driver must not be null");
        this.policy = policy != null ? policy : OverlayPolicy.none();
        this.logger = logger != null ? logger : OverlayLogger.noop();
    }

    public List<OverlayHandlingResult> handleKnownOverlays() {
        if (policy.isEmpty()) {
            return List.of();
        }

        emit(UiTestLensEventType.OVERLAY_POLICY_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Overlay policy execution started", "", null, null);

        List<OverlayHandlingResult> results = new ArrayList<>();
        for (OverlayHandler handler : policy.handlers()) {
            OverlayHandlingResult result = handle(handler);
            results.add(result);
        }
        return List.copyOf(results);
    }

    public boolean handleKnownOverlaysIfAny() {
        return handleKnownOverlays().stream()
                .anyMatch(result -> result.status() == OverlayHandlingStatus.HANDLED
                        || result.status() == OverlayHandlingStatus.STILL_VISIBLE
                        || result.status() == OverlayHandlingStatus.FAILED);
    }

    private OverlayHandlingResult handle(OverlayHandler handler) {
        Instant started = Instant.now();
        List<String> attempted = new ArrayList<>();

        WebElement detected = visibleElement(handler.detect());
        if (detected == null) {
            OverlayHandlingResult result = OverlayHandlingResult.notDetected(handler.name(), elapsedSince(started));
            emitResult(result, handler);
            return result;
        }

        emit(UiTestLensEventType.OVERLAY_DETECTED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                "Overlay detected by policy handler", handler.name(), null, null);

        for (OverlayAction action : handler.actions()) {
            String actionDescription = action.describe();
            attempted.add(actionDescription);
            emit(UiTestLensEventType.OVERLAY_ACTION_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                    "Overlay action started", handler.name(), actionDescription, null);
            try {
                executeAction(handler, action);
                emit(UiTestLensEventType.OVERLAY_ACTION_PASSED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                        "Overlay action passed", handler.name(), actionDescription, null);
            } catch (OverlayPolicyFailure e) {
                OverlayHandlingResult result = OverlayHandlingResult.failed(handler.name(), attempted,
                        e.getMessage(), e, elapsedSince(started));
                emitResult(result, handler);
                return result;
            } catch (RuntimeException e) {
                OverlayHandlingResult result = OverlayHandlingResult.failed(handler.name(), attempted,
                        "Overlay action failed", e, elapsedSince(started));
                emitResult(result, handler);
                return result;
            }
        }

        if (visibleElement(handler.detect()) != null) {
            OverlayHandlingResult result = handler.failIfStillVisible() || !handler.optional()
                    ? OverlayHandlingResult.failed(handler.name(), attempted, "Overlay is still visible", null, elapsedSince(started))
                    : OverlayHandlingResult.stillVisible(handler.name(), attempted, elapsedSince(started));
            emitResult(result, handler);
            return result;
        }

        OverlayHandlingResult result = OverlayHandlingResult.handled(handler.name(), attempted, elapsedSince(started));
        emitResult(result, handler);
        return result;
    }

    private void executeAction(OverlayHandler handler, OverlayAction action) {
        switch (action.type()) {
            case CLICK -> visibleElementOrThrow(action.target()).click();
            case PRESS_ESCAPE -> new Actions(driver).sendKeys(Keys.ESCAPE).perform();
            case WAIT_UNTIL_GONE -> waitUntilGone(handler, action.target());
            case FAIL -> throw new OverlayPolicyFailure(action.reason());
        }
    }

    private WebElement visibleElementOrThrow(By locator) {
        WebElement element = visibleElement(locator);
        if (element == null) {
            throw new NoSuchElementException("Visible overlay action target was not found: " + locator);
        }
        return element;
    }

    private WebElement visibleElement(By locator) {
        List<WebElement> elements = driver.findElements(locator);
        for (WebElement element : elements) {
            try {
                if (element != null && element.isDisplayed()) {
                    return element;
                }
            } catch (StaleElementReferenceException ignored) {
                // Treat stale nodes as not visible for this polling attempt.
            }
        }
        return null;
    }

    private void waitUntilGone(OverlayHandler handler, By locator) {
        try {
            new WebDriverWait(driver, handler.timeout())
                    .until(ExpectedConditions.invisibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            throw new OverlayPolicyFailure("Overlay did not disappear before timeout", e);
        }
    }

    private void emitResult(OverlayHandlingResult result, OverlayHandler handler) {
        UiTestLensEventType eventType = switch (result.status()) {
            case HANDLED -> UiTestLensEventType.OVERLAY_HANDLED;
            case STILL_VISIBLE -> UiTestLensEventType.OVERLAY_STILL_VISIBLE;
            case FAILED -> UiTestLensEventType.OVERLAY_ACTION_FAILED;
            default -> UiTestLensEventType.OVERLAY;
        };
        UiTestLensStatus status = switch (result.status()) {
            case HANDLED, NOT_DETECTED -> UiTestLensStatus.PASSED;
            case STILL_VISIBLE -> UiTestLensStatus.WARN;
            case FAILED -> UiTestLensStatus.FAILED;
            case SKIPPED -> UiTestLensStatus.SKIPPED;
        };
        UiTestLensLogLevel level = switch (result.status()) {
            case FAILED -> UiTestLensLogLevel.ERROR;
            case STILL_VISIBLE -> UiTestLensLogLevel.WARN;
            default -> UiTestLensLogLevel.INFO;
        };
        emit(eventType, status, level, result.message(), handler.name(), null, result.exception());
    }

    private void emit(UiTestLensEventType eventType,
                      UiTestLensStatus status,
                      UiTestLensLogLevel level,
                      String message,
                      String handlerName,
                      String action,
                      Throwable throwable) {
        try {
            UiTestLensLogEntry.Builder builder = UiTestLensLogEntry.builder()
                    .eventType(eventType)
                    .status(status)
                    .level(level)
                    .action("overlayPolicy")
                    .message(message)
                    .throwable(throwable);
            if (handlerName != null && !handlerName.isBlank()) {
                builder.metadata("handler", handlerName);
            }
            if (action != null && !action.isBlank()) {
                builder.metadata("overlayAction", action);
            }
            logger.emit(builder.build());
        } catch (RuntimeException ignored) {
            // Logging must never change overlay handling behavior.
        }
    }

    private static Duration elapsedSince(Instant started) {
        return Duration.between(started, Instant.now());
    }

    private static final class OverlayPolicyFailure extends RuntimeException {
        private OverlayPolicyFailure(String message) {
            super(message);
        }

        private OverlayPolicyFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
