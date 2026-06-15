package io.github.testlens.selenium.locator;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;
import io.github.testlens.selenium.actionability.ActionabilityReport;
import io.github.testlens.selenium.assertions.UiAssertionOptions;
import io.github.testlens.selenium.assertions.UiExpect;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;

public final class UiLocator {
    private final WebDriver driver;
    private final JsOverlayDebug overlay;
    private final UiLocatorDescription description;
    private final UiLocatorOptions options;
    private final UiLocatorResolver resolver;
    private final OverlayLogger logger;

    public UiLocator(WebDriver driver,
                     By by,
                     String label,
                     JsOverlayDebug overlay,
                     UiLocatorOptions options,
                     OverlayLogger logger) {
        this.driver = Objects.requireNonNull(driver, "driver must not be null");
        this.overlay = Objects.requireNonNull(overlay, "overlay must not be null");
        this.description = UiLocatorDescription.of(by, label);
        this.options = options != null ? options : UiLocatorOptions.defaults();
        this.resolver = new UiLocatorResolver(driver);
        this.logger = logger != null ? logger : OverlayLogger.noop();
    }

    public UiLocator click() {
        return execute("click", element -> {
            ActionabilityReport report = checkActionability();
            overlay.smartClickWithOverlayHandler(element, description.displayName());
            return report;
        });
    }

    public UiLocator fill(String value) {
        return execute("fill", element -> {
            ActionabilityReport report = checkActionability();
            element.clear();
            if (value != null) {
                element.sendKeys(value);
            }
            return report;
        }, value == null ? 0 : value.length());
    }

    public UiLocator clear() {
        return execute("clear", element -> {
            ActionabilityReport report = checkActionability();
            element.clear();
            return report;
        });
    }

    public UiLocator pressEnter() {
        return execute("pressEnter", element -> {
            ActionabilityReport report = checkActionability();
            element.sendKeys(Keys.ENTER);
            return report;
        });
    }

    public String textContent() {
        return read("textContent", WebElement::getText);
    }

    public boolean isVisible() {
        return read("isVisible", WebElement::isDisplayed);
    }

    public boolean isEnabled() {
        return read("isEnabled", WebElement::isEnabled);
    }

    public UiExpect expect() {
        return new UiExpect(this, UiAssertionOptions.defaults(), logger);
    }

    public WebElement resolve() {
        emit(UiTestLensEventType.LOCATOR_RESOLVE_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Resolving locator", "resolve", 0, null, null);
        try {
            WebElement element = resolver.resolve(by(), options);
            emit(UiTestLensEventType.LOCATOR_RESOLVE_PASSED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                    "Locator resolved", "resolve", 1, null, null);
            return element;
        } catch (RuntimeException e) {
            emit(UiTestLensEventType.LOCATOR_RESOLVE_FAILED, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR,
                    "Locator resolve failed", "resolve", 1, null, e);
            throw e;
        }
    }

    public ActionabilityReport checkActionability() {
        return checkActionability(options.actionabilityOptions());
    }

    public ActionabilityReport checkActionability(io.github.testlens.selenium.actionability.ActionabilityOptions actionabilityOptions) {
        return overlay.checkActionability(by(), actionabilityOptions);
    }

    public By by() {
        return description.by();
    }

    public String description() {
        return description.displayName();
    }

    private UiLocator execute(String action, Function<WebElement, ActionabilityReport> operation) {
        return execute(action, operation, null);
    }

    private UiLocator execute(String action, Function<WebElement, ActionabilityReport> operation, Integer valueLength) {
        Instant started = Instant.now();
        emit(UiTestLensEventType.LOCATOR_ACTION_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Locator action started", action, 0, valueLength, null);
        RuntimeException lastFailure = null;
        String lastActionabilitySummary = "";
        for (int attempt = 1; attempt <= options.maxRetries(); attempt++) {
            try {
                WebElement element = resolve();
                ActionabilityReport report = operation.apply(element);
                lastActionabilitySummary = report == null ? "" : report.summary();
                emit(UiTestLensEventType.LOCATOR_ACTION_PASSED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                        "Locator action passed", action, attempt, valueLength, null);
                return this;
            } catch (RuntimeException e) {
                lastFailure = e;
                if (!shouldRetry(e) || attempt >= options.maxRetries()) {
                    emit(UiTestLensEventType.LOCATOR_ACTION_FAILED, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR,
                            "Locator action failed", action, attempt, valueLength, e);
                    throw locatorException(action, e, lastActionabilitySummary);
                }
                emit(UiTestLensEventType.LOCATOR_RETRY, UiTestLensStatus.WARN, UiTestLensLogLevel.WARN,
                        "Retrying locator action", action, attempt, valueLength, e);
            }
        }
        throw locatorException(action, lastFailure, lastActionabilitySummary);
    }

    private <T> T read(String action, Function<WebElement, T> operation) {
        Instant started = Instant.now();
        emit(UiTestLensEventType.LOCATOR_ACTION_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Locator read started", action, 0, null, null);
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= options.maxRetries(); attempt++) {
            try {
                T value = operation.apply(resolve());
                emit(UiTestLensEventType.LOCATOR_ACTION_PASSED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                        "Locator read passed", action, attempt, null, null);
                return value;
            } catch (RuntimeException e) {
                lastFailure = e;
                if (!shouldRetry(e) || attempt >= options.maxRetries()) {
                    emit(UiTestLensEventType.LOCATOR_ACTION_FAILED, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR,
                            "Locator read failed", action, attempt, null, e);
                    throw locatorException(action, e, "");
                }
                emit(UiTestLensEventType.LOCATOR_RETRY, UiTestLensStatus.WARN, UiTestLensLogLevel.WARN,
                        "Retrying locator read", action, attempt, null, e);
            }
        }
        throw locatorException(action, lastFailure, "");
    }

    private boolean shouldRetry(RuntimeException e) {
        if (e instanceof StaleElementReferenceException) {
            return options.retryOnStaleElement();
        }
        if (e instanceof ElementClickInterceptedException) {
            return options.retryOnClickIntercepted();
        }
        if (e instanceof ElementNotInteractableException) {
            return options.retryOnNotInteractable();
        }
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return (options.retryOnClickIntercepted() && lower.contains("intercepted"))
                || (options.retryOnNotInteractable() && lower.contains("not interactable"))
                || (options.retryOnStaleElement() && lower.contains("stale"));
    }

    private UiLocatorException locatorException(String action, RuntimeException cause, String actionabilitySummary) {
        String message = "UiLocator action failed: " + action + " on " + description.displayName();
        if (actionabilitySummary != null && !actionabilitySummary.isBlank()) {
            message += " | actionability=" + actionabilitySummary;
        }
        return new UiLocatorException(action, description.displayName(), message, cause, actionabilitySummary);
    }

    private void emit(UiTestLensEventType eventType,
                      UiTestLensStatus status,
                      UiTestLensLogLevel level,
                      String message,
                      String action,
                      int attempt,
                      Integer valueLength,
                      Throwable throwable) {
        try {
            UiTestLensLogEntry.Builder builder = UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(eventType)
                    .status(status)
                    .message(message + ": " + description.displayName())
                    .action("locator." + action)
                    .metadata("locator", by().toString())
                    .metadata("description", description.displayName())
                    .metadata("attempt", String.valueOf(attempt))
                    .throwable(throwable);
            if (valueLength != null) {
                builder.metadata("valueLength", String.valueOf(valueLength));
            }
            logger.emit(builder.build());
        } catch (Exception ignored) {
        }
    }
}
