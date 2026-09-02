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
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public final class UiLocator {
    private final WebDriver driver;
    private final JsOverlayDebug overlay;
    private final UiLocatorDescription description;
    private final UiLocatorOptions options;
    private final UiLocatorResolver resolver;
    private final OverlayLogger logger;
    private final Integer collectionIndex;
    private final boolean lastCollectionElement;

    public UiLocator(WebDriver driver,
                     By by,
                     String label,
                     JsOverlayDebug overlay,
                     UiLocatorOptions options,
                     OverlayLogger logger) {
        this(driver, by, label, overlay, options, logger, null, false);
    }

    private UiLocator(WebDriver driver,
                      By by,
                      String label,
                      JsOverlayDebug overlay,
                      UiLocatorOptions options,
                      OverlayLogger logger,
                      Integer collectionIndex,
                      boolean lastCollectionElement) {
        this.driver = Objects.requireNonNull(driver, "driver must not be null");
        this.overlay = Objects.requireNonNull(overlay, "overlay must not be null");
        this.description = UiLocatorDescription.of(by, label);
        this.options = options != null ? options : UiLocatorOptions.defaults();
        this.resolver = new UiLocatorResolver(driver);
        this.logger = logger != null ? logger : OverlayLogger.noop();
        this.collectionIndex = collectionIndex;
        this.lastCollectionElement = lastCollectionElement;
    }

    public UiLocator click() {
        return execute("click", element -> {
            ActionabilityReport report = safeActionability(element);
            overlay.smartClickWithOverlayHandler(element, description.displayName());
            return report;
        });
    }

    public UiLocator fill(String value) {
        return execute("fill", element -> {
            ActionabilityReport report = safeActionability(element);
            element.clear();
            if (value != null) {
                element.sendKeys(value);
            }
            return report;
        }, value == null ? 0 : value.length());
    }

    public UiLocator clear() {
        return execute("clear", element -> {
            ActionabilityReport report = safeActionability(element);
            element.clear();
            return report;
        });
    }

    public UiLocator pressEnter() {
        return press(Keys.ENTER);
    }

    public UiLocator press(CharSequence... keys) {
        CharSequence[] effectiveKeys = keys == null ? new CharSequence[0] : keys;
        return execute("press", element -> {
            ActionabilityReport report = safeActionability(element);
            element.sendKeys(effectiveKeys);
            return report;
        });
    }

    public UiLocator selectByVisibleText(String text) {
        Objects.requireNonNull(text, "visible text must not be null");
        return execute("select visible text '" + safePreview(text) + "'", element -> {
            ActionabilityReport report = safeActionability(element);
            new Select(element).selectByVisibleText(text);
            return report;
        });
    }

    public UiLocator selectByValue(String value) {
        Objects.requireNonNull(value, "select value must not be null");
        return execute("select value '" + safePreview(value) + "'", element -> {
            ActionabilityReport report = safeActionability(element);
            new Select(element).selectByValue(value);
            return report;
        });
    }

    public UiLocator selectByIndex(int index) {
        return execute("select index " + index, element -> {
            ActionabilityReport report = safeActionability(element);
            new Select(element).selectByIndex(index);
            return report;
        });
    }

    public String selectedText() {
        return read("selectedText", element -> new Select(element).getFirstSelectedOption().getText());
    }

    public String selectedValue() {
        return read("selectedValue", element -> new Select(element).getFirstSelectedOption().getAttribute("value"));
    }

    public UiLocator hover() {
        return execute("hover", element -> {
            new Actions(driver).moveToElement(element).perform();
            return null;
        });
    }

    public UiLocator doubleClick() {
        return execute("doubleClick", element -> {
            ActionabilityReport report = safeActionability(element);
            new Actions(driver).doubleClick(element).perform();
            return report;
        });
    }

    public UiLocator rightClick() {
        return execute("rightClick", element -> {
            ActionabilityReport report = safeActionability(element);
            new Actions(driver).contextClick(element).perform();
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

    public String attribute(String name) {
        Objects.requireNonNull(name, "attribute name must not be null");
        return read("attribute", element -> element.getAttribute(name));
    }

    public String property(String name) {
        Objects.requireNonNull(name, "property name must not be null");
        return read("property", element -> element.getDomProperty(name));
    }

    public String value() {
        return attribute("value");
    }

    public List<WebElement> resolveAll() {
        emit(UiTestLensEventType.LOCATOR_RESOLVE_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Resolving locator collection", "resolveAll", 0, null, null);
        try {
            List<WebElement> elements = List.copyOf(driver.findElements(by()));
            emit(UiTestLensEventType.LOCATOR_RESOLVE_PASSED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                    "Locator collection resolved", "resolveAll", elements.size(), null, null);
            return elements;
        } catch (RuntimeException failure) {
            emit(UiTestLensEventType.LOCATOR_RESOLVE_FAILED, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR,
                    "Locator collection resolve failed", "resolveAll", 1, null, failure);
            throw locatorException("resolveAll", failure, "");
        }
    }

    public int count() {
        int count = resolveAll().size();
        emit(UiTestLensEventType.LOCATOR_ACTION_PASSED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                "Locator count read: " + count, "count", 1, null, null);
        return count;
    }

    public UiLocator nth(int index) {
        return new UiLocator(driver, by(), collectionLabel("[" + index + "]"), overlay, options, logger, index, false);
    }

    public UiLocator first() {
        return new UiLocator(driver, by(), collectionLabel("[first]"), overlay, options, logger, 0, false);
    }

    public UiLocator last() {
        return new UiLocator(driver, by(), collectionLabel("[last]"), overlay, options, logger, null, true);
    }

    public UiLocator waitUntilVisible() {
        return waitUntil("visible", webDriver -> {
            WebElement element = currentElement(webDriver);
            return element.isDisplayed() ? element : null;
        });
    }

    public UiLocator waitUntilHidden() {
        return waitUntil("hidden", webDriver -> {
            try { return !currentElement(webDriver).isDisplayed(); }
            catch (NoSuchElementException | StaleElementReferenceException ignored) { return true; }
        });
    }

    public UiLocator waitUntilClickable() {
        return waitUntil("clickable", webDriver -> {
            WebElement element = currentElement(webDriver);
            return element.isDisplayed() && element.isEnabled() ? element : null;
        });
    }

    public UiLocator waitUntilText(String expectedText) {
        return waitUntil("text '" + safePreview(expectedText) + "'", webDriver ->
                currentElement(webDriver).getText().contains(expectedText));
    }

    public UiExpect expect() {
        return expect(UiAssertionOptions.defaults());
    }

    public UiExpect expect(UiAssertionOptions assertionOptions) {
        return new UiExpect(this, assertionOptions, logger, this::probeVisibilityForAssertion, this::probeElementForAssertion);
    }

    public WebElement resolve() {
        emit(UiTestLensEventType.LOCATOR_RESOLVE_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Resolving locator", "resolve", 0, null, null);
        try {
            WebElement element = isCollectionView() ? resolveCollectionElement() : resolver.resolve(by(), options);
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
        return isCollectionView()
                ? overlay.checkActionability(resolve(), actionabilityOptions)
                : overlay.checkActionability(by(), actionabilityOptions);
    }

    public By by() {
        return description.by();
    }

    public String description() {
        return description.displayName();
    }

    private boolean isCollectionView() {
        return collectionIndex != null || lastCollectionElement;
    }

    private WebElement resolveCollectionElement() {
        AtomicInteger actualCount = new AtomicInteger();
        try {
            return new WebDriverWait(driver, options.timeout())
                    .pollingEvery(options.pollInterval())
                    .ignoring(StaleElementReferenceException.class)
                    .until(webDriver -> {
                        List<WebElement> current = webDriver.findElements(by());
                        actualCount.set(current.size());
                        int requested = lastCollectionElement ? current.size() - 1 : collectionIndex;
                        if (requested < 0 || requested >= current.size()) return null;
                        return current.get(requested);
                    });
        } catch (RuntimeException failure) {
            String requested = lastCollectionElement ? "last" : String.valueOf(collectionIndex);
            throw new UiLocatorException("resolveCollectionElement", description(),
                    "Collection locator failed: " + description() +
                            " | requestedIndex=" + requested + " | actualCount=" + actualCount.get(),
                    failure, "");
        }
    }

    private WebElement currentElement(WebDriver webDriver) {
        if (!isCollectionView()) return webDriver.findElement(by());
        List<WebElement> current = webDriver.findElements(by());
        int requested = lastCollectionElement ? current.size() - 1 : collectionIndex;
        if (requested < 0 || requested >= current.size()) {
            throw new NoSuchElementException("Collection locator " + description() +
                    " requestedIndex=" + (lastCollectionElement ? "last" : requested) +
                    " actualCount=" + current.size());
        }
        return current.get(requested);
    }

    private String collectionLabel(String suffix) {
        String base = description();
        return base + " " + suffix;
    }

    private UiLocator waitUntil(String conditionName, ExpectedCondition<?> condition) {
        Instant started = Instant.now();
        emit(UiTestLensEventType.WAIT, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Waiting until " + conditionName, "wait", 0, null, null);
        AtomicInteger attempts = new AtomicInteger();
        try {
            new WebDriverWait(driver, options.timeout())
                    .pollingEvery(options.pollInterval())
                    .ignoring(NoSuchElementException.class)
                    .ignoring(StaleElementReferenceException.class)
                    .until(webDriver -> {
                        int attempt = attempts.incrementAndGet();
                        Object result = condition.apply(webDriver);
                        boolean satisfied = result instanceof Boolean value ? value : result != null;
                        if (!satisfied) {
                            emit(UiTestLensEventType.LOCATOR_RETRY, UiTestLensStatus.WARN, UiTestLensLogLevel.INFO,
                                    "Wait retry: " + conditionName, "wait", attempt, null, null);
                        }
                        return satisfied;
                    });
            emit(UiTestLensEventType.WAIT, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                    "Wait passed: " + conditionName, "wait", attempts.get(), null, null);
            return this;
        } catch (RuntimeException failure) {
            emit(UiTestLensEventType.WAIT, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR,
                    "Wait failed: " + conditionName, "wait", attempts.get(), null, failure);
            throw locatorException("waitUntil(" + conditionName + ")", failure,
                    "elapsed=" + Duration.between(started, Instant.now()).toMillis() + "ms");
        }
    }

    private static String safePreview(String value) {
        if (value == null) return "null";
        return value.length() <= 80 ? value : value.substring(0, 77) + "...";
    }

    private UiLocator execute(String action, Function<WebElement, ActionabilityReport> operation) {
        return execute(action, operation, null);
    }

    private ActionabilityReport safeActionability(WebElement element) {
        try {
            return overlay.checkActionability(element, options.actionabilityOptions());
        } catch (RuntimeException diagnosticFailure) {
            emit(UiTestLensEventType.ACTIONABILITY_CHECK_FAILED, UiTestLensStatus.WARN, UiTestLensLogLevel.WARN,
                    "Actionability diagnostics unavailable; continuing with Selenium action",
                    "actionability", 1, null, diagnosticFailure);
            return null;
        }
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

    private UiExpect.VisibilityProbeResult probeVisibilityForAssertion() {
        try {
            WebElement element = currentElement(driver);
            try {
                return element.isDisplayed()
                        ? UiExpect.VisibilityProbeResult.visibleElement()
                        : UiExpect.VisibilityProbeResult.hiddenElement();
            } catch (NoSuchElementException e) {
                return UiExpect.VisibilityProbeResult.missingElement();
            }
        } catch (NoSuchElementException e) {
            return UiExpect.VisibilityProbeResult.missingElement();
        }
    }

    private UiExpect.ElementProbeResult probeElementForAssertion(Function<WebElement, String> operation) {
        try {
            WebElement element = currentElement(driver);
            try {
                return UiExpect.ElementProbeResult.present(operation.apply(element));
            } catch (NoSuchElementException e) {
                return UiExpect.ElementProbeResult.missingElement();
            }
        } catch (NoSuchElementException e) {
            return UiExpect.ElementProbeResult.missingElement();
        }
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

