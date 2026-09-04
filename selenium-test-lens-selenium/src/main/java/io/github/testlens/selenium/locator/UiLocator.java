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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.interactions.Actions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.LongSupplier;

public final class UiLocator {
    private final WebDriver driver;
    private final JsOverlayDebug overlay;
    private final UiLocatorDescription description;
    private final UiLocatorOptions options;
    private final UiLocatorResolver resolver;
    private final OverlayLogger logger;
    private final LongSupplier nanoTicker;

    public UiLocator(WebDriver driver,
                     By by,
                     String label,
                     JsOverlayDebug overlay,
                     UiLocatorOptions options,
                     OverlayLogger logger) {
        this(driver, by, label, overlay, options, logger, System::nanoTime);
    }

    UiLocator(WebDriver driver, By by, String label, JsOverlayDebug overlay,
              UiLocatorOptions options, OverlayLogger logger, LongSupplier nanoTicker) {
        this.driver = Objects.requireNonNull(driver, "driver must not be null");
        this.overlay = Objects.requireNonNull(overlay, "overlay must not be null");
        this.description = UiLocatorDescription.of(by, label);
        this.options = options != null ? options : UiLocatorOptions.defaults();
        this.resolver = new UiLocatorResolver(driver);
        this.logger = logger != null ? logger : OverlayLogger.noop();
        this.nanoTicker = nanoTicker == null ? System::nanoTime : nanoTicker;
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

    /** Returns the accessible name computed by the browser/WebDriver. */
    public String accessibleName() {
        return read("accessibleName", WebElement::getAccessibleName);
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
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= options.maxRetries(); attempt++) {
            long started = nanoTicker.getAsLong();
            try {
                List<WebElement> elements = List.copyOf(CompositeBy.find(driver, by()));
                emit(UiTestLensEventType.LOCATOR_RESOLVE_PASSED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                        "Locator collection resolved", "resolveAll", elements.size(), null, null);
                return elements;
            } catch (RuntimeException failure) {
                lastFailure = failure;
                if (!(effectiveRetryCause(failure) instanceof StaleElementReferenceException)
                        || !options.retryOnStaleElement() || attempt >= options.maxRetries()) {
                    emit(UiTestLensEventType.LOCATOR_RESOLVE_FAILED, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR,
                            "Locator collection resolve failed", "resolveAll", attempt, null, failure);
                    throw locatorException("resolveAll", failure, "");
                }
                emitRecoveryRetry("collection", "resolveAll", attempt, attempt + 1,
                        elapsedNanos(started), effectiveRetryCause(failure), null);
            }
        }
        throw locatorException("resolveAll", lastFailure, "");
    }

    public int count() {
        int count = resolveAll().size();
        emit(UiTestLensEventType.LOCATOR_ACTION_PASSED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                "Locator count read: " + count, "count", 1, null, null);
        return count;
    }

    public UiLocator nth(int index) {
        return derived(CompositeBy.index(by(), index), collectionLabel("[" + index + "]"));
    }

    public UiLocator first() {
        return derived(CompositeBy.index(by(), 0), collectionLabel("[first]"));
    }

    public UiLocator last() {
        return derived(CompositeBy.last(by()), collectionLabel("[last]"));
    }

    public UiLocator locator(By descendant) {
        return locator(descendant, description() + " >> " + safePreview(String.valueOf(descendant)));
    }

    public UiLocator locator(By descendant, String label) {
        Objects.requireNonNull(descendant, "descendant locator must not be null");
        return derived(CompositeBy.descendants(by(), descendant), label);
    }

    public UiLocator locator(UiLocator descendant) {
        requireSameDriver(descendant);
        return locator(descendant.by(), description() + " >> " + descendant.description());
    }

    public UiLocator filterByText(String expectedText) {
        Objects.requireNonNull(expectedText, "expected text must not be null");
        return derived(CompositeBy.text(by(), expectedText, false), description() + " | text equals");
    }

    public UiLocator filterByTextContaining(String expectedText) {
        Objects.requireNonNull(expectedText, "expected text must not be null");
        return derived(CompositeBy.text(by(), expectedText, true), description() + " | text contains");
    }

    public UiLocator filterByAttribute(String attributeName, String expectedValue) {
        if (attributeName == null || attributeName.isBlank()) {
            throw new IllegalArgumentException("attribute name must not be blank");
        }
        Objects.requireNonNull(expectedValue, "expected attribute value must not be null");
        return derived(CompositeBy.attribute(by(), attributeName, expectedValue),
                description() + " | attribute " + safePreview(attributeName));
    }

    public UiLocator filterHas(By descendant) {
        Objects.requireNonNull(descendant, "descendant locator must not be null");
        return derived(CompositeBy.has(by(), descendant),
                description() + " | has(" + safePreview(String.valueOf(descendant)) + ")");
    }

    public UiLocator filterHas(UiLocator descendant) {
        requireSameDriver(descendant);
        return derived(CompositeBy.has(by(), descendant.by()),
                description() + " | has(" + descendant.description() + ")");
    }

    public UiLocator waitUntilCount(int expected) {
        return waitUntilCount("count ==", expected, count -> count == expected);
    }

    public UiLocator waitUntilCountAtLeast(int minimum) {
        return waitUntilCount("count >=", minimum, count -> count >= minimum);
    }

    public UiLocator waitUntilCountAtMost(int maximum) {
        return waitUntilCount("count <=", maximum, count -> count <= maximum);
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
            WebElement element = resolver.resolve(by(), options);
            emit(UiTestLensEventType.LOCATOR_RESOLVE_PASSED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                    "Locator resolved", "resolve", 1, null, null);
            return element;
        } catch (RuntimeException e) {
            emit(UiTestLensEventType.LOCATOR_RESOLVE_FAILED, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR,
                    "Locator resolve failed", "resolve", 1, null, e);
            CollectionSelectionException selection = selectionFailure(e);
            if (selection != null) {
                RuntimeException original = e.getCause() instanceof RuntimeException cause ? cause : e;
                throw new UiLocatorException("resolve", description(),
                        "Collection locator failed: " + description() + " | "
                                + selection.getMessage().split("\n", 2)[0], original, "");
            }
            throw e;
        }
    }

    public ActionabilityReport checkActionability() {
        return checkActionability(options.actionabilityOptions());
    }

    public ActionabilityReport checkActionability(io.github.testlens.selenium.actionability.ActionabilityOptions actionabilityOptions) {
        if (by() instanceof By.Remotable) {
            return overlay.checkActionability(by(), actionabilityOptions);
        }
        return overlay.checkActionability(resolve(), actionabilityOptions);
    }

    public By by() {
        return description.by();
    }

    public String description() {
        return description.displayName();
    }

    private WebElement currentElement(WebDriver webDriver) {
        if (by() instanceof By.Remotable) return webDriver.findElement(by());
        List<WebElement> elements = by().findElements(webDriver);
        if (elements.isEmpty()) throw new NoSuchElementException("Composite locator matched no elements");
        return elements.get(0);
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

    private UiLocator derived(By query, String label) {
        return new UiLocator(driver, query, label, overlay, options, logger, nanoTicker);
    }

    private void requireSameDriver(UiLocator descendant) {
        Objects.requireNonNull(descendant, "descendant locator must not be null");
        if (driver != descendant.driver) {
            throw new IllegalArgumentException("Composed locators must belong to the same WebDriver");
        }
    }

    private static CollectionSelectionException selectionFailure(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof CollectionSelectionException selection) return selection;
        }
        return null;
    }

    private UiLocator waitUntilCount(String operator, int expected, java.util.function.IntPredicate condition) {
        if (expected < 0) throw new IllegalArgumentException("expected count must be >= 0");
        Instant started = Instant.now();
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger lastCount = new AtomicInteger();
        String conditionDescription = operator + " " + expected;
        emit(UiTestLensEventType.WAIT, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Waiting until " + conditionDescription, "waitCount", 0, null, null);
        try {
            new WebDriverWait(driver, options.timeout())
                    .pollingEvery(options.pollInterval())
                    .ignoring(StaleElementReferenceException.class)
                    .until(webDriver -> {
                        attempts.incrementAndGet();
                        int current = CompositeBy.find(webDriver, by()).size();
                        lastCount.set(current);
                        return condition.test(current);
                    });
            emit(UiTestLensEventType.WAIT, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                    "Count wait passed: " + conditionDescription + ", lastCount=" + lastCount.get(),
                    "waitCount", attempts.get(), null, null);
            return this;
        } catch (RuntimeException failure) {
            long elapsed = Duration.between(started, Instant.now()).toMillis();
            emit(UiTestLensEventType.WAIT, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR,
                    "Count wait failed: " + conditionDescription + ", lastCount=" + lastCount.get(),
                    "waitCount", attempts.get(), null, failure);
            throw locatorException("waitUntilCount(" + conditionDescription + ")", failure,
                    "lastCount=" + lastCount.get() + ", attempts=" + attempts.get() + ", elapsed=" + elapsed + "ms");
        }
    }

    private UiLocator changeCheckedState(String action, CheckedState target) {
        CheckedActionMetadata metadata = new CheckedActionMetadata();
        metadata.targetState = target;
        emitControl(UiTestLensEventType.LOCATOR_ACTION_STARTED, UiTestLensStatus.STARTED,
                UiTestLensLogLevel.INFO, "Locator action started", action, 0, metadata, null);
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= options.maxRetries(); attempt++) {
            long attemptStarted = 0;
            boolean operationStarted = false;
            try {
                WebElement element = resolve();
                attemptStarted = nanoTicker.getAsLong();
                operationStarted = true;
                SemanticControl control = resolveSemanticControl(element);
                CheckedState initial = readCheckedState(control);
                metadata.observe(control, initial);
                requireTargetSupported(control, target);
                if (initial == target) {
                    metadata.finalState = initial;
                    emitControl(UiTestLensEventType.LOCATOR_ACTION_PASSED, UiTestLensStatus.PASSED,
                            UiTestLensLogLevel.INFO, "Locator action passed", action, attempt, metadata, null);
                    return this;
                }
                requireEnabled(control);
                requireActivation(control);
                metadata.clickPerformed = true;
                overlay.smartClickWithOverlayHandler(control.activationElement(), description.displayName());
                Confirmation confirmation = confirmCheckedState(target);
                metadata.confirmationAttempts += confirmation.attempts();
                metadata.finalState = confirmation.state();
                emitControl(UiTestLensEventType.LOCATOR_ACTION_PASSED, UiTestLensStatus.PASSED,
                        UiTestLensLogLevel.INFO, "Locator action passed", action, attempt, metadata, null);
                return this;
            } catch (RuntimeException failure) {
                lastFailure = failure;
                if (!shouldRetry(failure) || attempt >= options.maxRetries()) {
                    emitControl(UiTestLensEventType.LOCATOR_ACTION_FAILED, UiTestLensStatus.FAILED,
                            UiTestLensLogLevel.ERROR, "Locator action failed", action, attempt, metadata, failure);
                    throw locatorException(action, failure, "");
                }
                if (operationStarted) {
                    emitRecoveryRetry("action", action, attempt, attempt + 1,
                            elapsedNanos(attemptStarted), effectiveRetryCause(failure), null);
                }
            }
        }
        throw locatorException(action, lastFailure, "");
    }

    private Confirmation confirmCheckedState(CheckedState target) {
        AtomicInteger attempts = new AtomicInteger();
        CheckedState[] observed = new CheckedState[]{null};
        try {
            CheckedState state = new WebDriverWait(driver, options.timeout())
                    .pollingEvery(options.pollInterval())
                    .ignoring(NoSuchElementException.class)
                    .ignoring(StaleElementReferenceException.class)
                    .until(webDriver -> {
                        attempts.incrementAndGet();
                        SemanticControl current = resolveSemanticControl(currentElement(webDriver));
                        CheckedState value = readCheckedState(current);
                        observed[0] = value;
                        return value == target ? value : null;
                    });
            return new Confirmation(state, attempts.get());
        } catch (org.openqa.selenium.TimeoutException timeout) {
            String last = observed[0] == null ? "UNAVAILABLE" : observed[0].name();
            throw new IllegalStateException("Control state confirmation timed out; expected="
                    + target.name() + ", lastObserved=" + last, timeout);
        }
    }

    private SemanticControl resolveSemanticControl(WebElement origin) {
        return resolveSemanticControl(origin, true);
    }

    private SemanticControl resolveSemanticControl(WebElement origin, boolean validateAriaState) {
        String tag = normalized(origin.getTagName());
        String type = normalized(origin.getDomAttribute("type"));
        String role = normalized(origin.getDomAttribute("role"));
        if ("input".equals(tag) && ("checkbox".equals(type) || "radio".equals(type))) {
            ControlKind kind = "radio".equals(type) ? ControlKind.NATIVE_RADIO : ControlKind.NATIVE_CHECKBOX;
            var rect = origin.isDisplayed() ? origin.getRect() : null;
            if (rect != null && rect.getWidth() > 0 && rect.getHeight() > 0) {
                return new SemanticControl(origin, origin, kind, ActivationKind.CONTROL, tag, type, role);
            }
            WebElement label = visibleLabel(origin);
            return new SemanticControl(origin, label, kind, ActivationKind.LABEL, tag, type, role);
        }
        if ("checkbox".equals(role) || "switch".equals(role) || "radio".equals(role)) {
            ControlKind kind = switch (role) {
                case "checkbox" -> ControlKind.ARIA_CHECKBOX;
                case "switch" -> ControlKind.ARIA_SWITCH;
                default -> ControlKind.ARIA_RADIO;
            };
            if (validateAriaState) validateAriaState(origin, tag, type, role);
            return new SemanticControl(origin, origin, kind, ActivationKind.CONTROL, tag, type, role);
        }
        SemanticControl labeled = controlFromClosestLabel(origin);
        if (labeled != null) return labeled;
        throw unsupportedControl(tag, type, role, "element is not a supported checked control");
    }

    private void validateAriaState(WebElement element, String tag, String type, String role) {
        String state = normalized(element.getDomAttribute("aria-checked"));
        if (!("true".equals(state) || "false".equals(state) || "mixed".equals(state))) {
            throw unsupportedControl(tag, type, role, "ARIA control requires aria-checked=true, false, or mixed");
        }
    }

    private WebElement visibleLabel(WebElement input) {
        JavascriptExecutor executor = requireJavascript("resolve a label for a hidden native control");
        Object result = executor.executeScript("return Array.from(arguments[0].labels || []);", input);
        if (!(result instanceof List<?> labels)) return null;
        List<WebElement> visible = new ArrayList<>();
        for (Object candidate : labels) {
            if (candidate instanceof WebElement label && label.isDisplayed()) visible.add(label);
        }
        return visible.size() == 1 ? visible.get(0) : null;
    }

    private SemanticControl controlFromClosestLabel(WebElement origin) {
        if (!(driver instanceof JavascriptExecutor executor)) return null;
        Object result = executor.executeScript("""
                const origin = arguments[0];
                const label = origin && origin.closest ? origin.closest('label') : null;
                if (!label || !label.control) return null;
                return [label.control, label];
                """, origin);
        if (!(result instanceof List<?> pair) || pair.size() != 2
                || !(pair.get(0) instanceof WebElement state)
                || !(pair.get(1) instanceof WebElement label)) return null;
        String tag = normalized(state.getTagName());
        String type = normalized(state.getDomAttribute("type"));
        String role = normalized(state.getDomAttribute("role"));
        if (!"input".equals(tag) || !("checkbox".equals(type) || "radio".equals(type))) {
            throw unsupportedControl(tag, type, role, "label is not associated with a checkbox or radio");
        }
        if (!label.isDisplayed()) {
            throw unsupportedControl(tag, type, role, "associated label is not visible");
        }
        ControlKind kind = "radio".equals(type) ? ControlKind.NATIVE_RADIO : ControlKind.NATIVE_CHECKBOX;
        return new SemanticControl(state, label, kind, ActivationKind.LABEL, tag, type, role);
    }

    private CheckedState readCheckedState(SemanticControl control) {
        if (control.kind() == ControlKind.NATIVE_CHECKBOX) {
            if (Boolean.parseBoolean(control.stateElement().getDomProperty("indeterminate"))) {
                return CheckedState.MIXED;
            }
            return control.stateElement().isSelected() ? CheckedState.CHECKED : CheckedState.UNCHECKED;
        }
        if (control.kind() == ControlKind.NATIVE_RADIO) {
            return control.stateElement().isSelected() ? CheckedState.CHECKED : CheckedState.UNCHECKED;
        }
        String value = normalized(control.stateElement().getDomAttribute("aria-checked"));
        return switch (value) {
            case "true" -> CheckedState.CHECKED;
            case "false" -> CheckedState.UNCHECKED;
            case "mixed" -> CheckedState.MIXED;
            default -> throw unsupportedControl(control.tag(), control.type(), control.role(),
                    "ARIA control requires aria-checked=true, false, or mixed");
        };
    }

    private void requireTargetSupported(SemanticControl control, CheckedState target) {
        if (target == CheckedState.UNCHECKED
                && (control.kind() == ControlKind.NATIVE_RADIO || control.kind() == ControlKind.ARIA_RADIO)) {
            throw unsupportedControl(control.tag(), control.type(), control.role(), "radio controls cannot be unchecked");
        }
    }

    private void requireEnabled(SemanticControl control) {
        if ((control.kind() == ControlKind.NATIVE_CHECKBOX || control.kind() == ControlKind.NATIVE_RADIO)
                && !control.stateElement().isEnabled()) {
            throw unsupportedControl(control.tag(), control.type(), control.role(), "native control is disabled");
        }
        if (!(control.kind() == ControlKind.NATIVE_CHECKBOX || control.kind() == ControlKind.NATIVE_RADIO)
                && "true".equalsIgnoreCase(control.stateElement().getDomAttribute("aria-disabled"))) {
            throw unsupportedControl(control.tag(), control.type(), control.role(), "ARIA control is disabled");
        }
    }

    private void requireActivation(SemanticControl control) {
        if (control.activationElement() == null) {
            throw unsupportedControl(control.tag(), control.type(), control.role(),
                    "hidden control requires exactly one visible associated label");
        }
    }

    private IllegalStateException unsupportedControl(String tag, String type, String role, String reason) {
        return new IllegalStateException(reason + " [tag=" + safeControlValue(tag)
                + ", type=" + safeControlValue(type) + ", role=" + safeControlValue(role) + "]");
    }

    private static String safeControlValue(String value) {
        String normalized = normalized(value);
        return normalized.length() <= 32 ? normalized : normalized.substring(0, 32);
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private UploadPayload prepareUpload(Path[] files) {
        if (files == null) throw new IllegalArgumentException("Upload files array must not be null");
        if (files.length == 0) throw new IllegalArgumentException("Upload requires at least one file");
        List<String> paths = new ArrayList<>(files.length);
        for (int index = 0; index < files.length; index++) {
            Path file = files[index];
            if (file == null) throw new IllegalArgumentException("Upload file at index " + index + " must not be null");
            String supplied = file.toString();
            if (supplied.indexOf('\r') >= 0 || supplied.indexOf('\n') >= 0) {
                throw new IllegalArgumentException("Upload file path at index " + index + " contains a line break");
            }
            Path normalized = file.toAbsolutePath().normalize();
            if (!Files.exists(normalized) || !Files.isRegularFile(normalized)) {
                throw new IllegalArgumentException("Upload file at index " + index + " must be an existing regular file");
            }
            paths.add(normalized.toString());
        }
        return new UploadPayload(String.join("\n", paths), paths.size());
    }

    private void requireFileInput(WebElement element, int fileCount) {
        String tag = normalized(element.getTagName());
        String type = normalized(element.getDomAttribute("type"));
        if (!"input".equals(tag) || !"file".equals(type)) {
            throw unsupportedControl(tag, type, normalized(element.getDomAttribute("role")),
                    "upload requires input type=file");
        }
        if (fileCount > 1 && element.getDomAttribute("multiple") == null) {
            throw new IllegalStateException("Multiple-file upload requires a file input with the multiple attribute");
        }
    }

    private UiLocator executeJavaScript(String action, String script) {
        return execute(action, element -> {
            requireJavascript(action).executeScript(script, element);
            return null;
        });
    }

    private JavascriptExecutor requireJavascript(String operation) {
        if (driver instanceof JavascriptExecutor executor) return executor;
        throw new IllegalStateException("WebDriver must implement JavascriptExecutor to " + operation);
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
            long attemptStarted = 0;
            boolean operationStarted = false;
            try {
                WebElement element = resolve();
                attemptStarted = nanoTicker.getAsLong();
                operationStarted = true;
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
                if (operationStarted) {
                    emitRecoveryRetry("action", action, attempt, attempt + 1,
                            elapsedNanos(attemptStarted), effectiveRetryCause(e), valueLength);
                }
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
            long attemptStarted = 0;
            boolean operationStarted = false;
            try {
                WebElement element = resolve();
                attemptStarted = nanoTicker.getAsLong();
                operationStarted = true;
                T value = operation.apply(element);
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
                if (operationStarted) {
                    emitRecoveryRetry("read", action, attempt, attempt + 1,
                            elapsedNanos(attemptStarted), effectiveRetryCause(e), null);
                }
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
            String observation = String.valueOf(operation);
            if ("test-lens-assertion:count".equals(observation)) {
                try {
                    return UiExpect.ElementProbeResult.present(String.valueOf(CompositeBy.find(driver, by()).size()));
                } catch (CollectionSelectionException missingSelection) {
                    return UiExpect.ElementProbeResult.present("0");
                }
            }
            WebElement element = currentElement(driver);
            try {
                if ("test-lens-assertion:checkedState".equals(observation)) {
                    return UiExpect.ElementProbeResult.present(
                            readCheckedState(resolveSemanticControl(element, false)).name());
                }
                return UiExpect.ElementProbeResult.present(operation.apply(element));
            } catch (NoSuchElementException e) {
                return UiExpect.ElementProbeResult.missingElement();
            }
        } catch (NoSuchElementException e) {
            return UiExpect.ElementProbeResult.missingElement();
        }
    }

    private boolean shouldRetry(RuntimeException e) {
        Throwable effective = effectiveRetryCause(e);
        if (effective instanceof StaleElementReferenceException) {
            return options.retryOnStaleElement();
        }
        if (effective instanceof ElementClickInterceptedException) {
            return options.retryOnClickIntercepted();
        }
        if (effective instanceof ElementNotInteractableException) {
            return options.retryOnNotInteractable();
        }
        return false;
    }

    /** Ensures that a native or ARIA checkbox, switch, or radio is checked. */
    public UiLocator check() {
        return changeCheckedState("check", CheckedState.CHECKED);
    }

    /** Ensures that a native/ARIA checkbox or ARIA switch is unchecked. */
    public UiLocator uncheck() {
        return changeCheckedState("uncheck", CheckedState.UNCHECKED);
    }

    /** Reads the current checked state of a supported native or ARIA control. */
    public boolean isChecked() {
        CheckedActionMetadata metadata = new CheckedActionMetadata();
        emitControl(UiTestLensEventType.LOCATOR_ACTION_STARTED, UiTestLensStatus.STARTED,
                UiTestLensLogLevel.INFO, "Locator read started", "isChecked", 0, metadata, null);
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= options.maxRetries(); attempt++) {
            long attemptStarted = 0;
            boolean observationStarted = false;
            try {
                WebElement element = resolve();
                attemptStarted = nanoTicker.getAsLong();
                observationStarted = true;
                SemanticControl control = resolveSemanticControl(element);
                CheckedState state = readCheckedState(control);
                metadata.observe(control, state);
                metadata.finalState = state;
                emitControl(UiTestLensEventType.LOCATOR_ACTION_PASSED, UiTestLensStatus.PASSED,
                        UiTestLensLogLevel.INFO, "Locator read passed", "isChecked", attempt, metadata, null);
                return state == CheckedState.CHECKED;
            } catch (RuntimeException failure) {
                lastFailure = failure;
                if (!shouldRetry(failure) || attempt >= options.maxRetries()) {
                    emitControl(UiTestLensEventType.LOCATOR_ACTION_FAILED, UiTestLensStatus.FAILED,
                            UiTestLensLogLevel.ERROR, "Locator read failed", "isChecked", attempt, metadata, failure);
                    throw locatorException("isChecked", failure, "");
                }
                if (observationStarted) {
                    emitRecoveryRetry("read", "isChecked", attempt, attempt + 1,
                            elapsedNanos(attemptStarted), effectiveRetryCause(failure), null);
                }
            }
        }
        throw locatorException("isChecked", lastFailure, "");
    }

    /** Sends one or more local files to a file input without clicking it. */
    public UiLocator upload(Path... files) {
        int fileCount = files == null ? 0 : files.length;
        emit(UiTestLensEventType.LOCATOR_ACTION_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Locator action started", "upload", 0, null, null, Map.of("fileCount", String.valueOf(fileCount)));
        UploadPayload upload;
        try {
            upload = prepareUpload(files);
        } catch (RuntimeException failure) {
            emit(UiTestLensEventType.LOCATOR_ACTION_FAILED, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR,
                    "Locator action failed", "upload", 0, null, null,
                    Map.of("fileCount", String.valueOf(fileCount)));
            throw locatorException("upload", failure, "");
        }

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= options.maxRetries(); attempt++) {
            long attemptStarted = 0;
            boolean preflightStarted = false;
            boolean sendKeysStarted = false;
            try {
                WebElement element = resolve();
                attemptStarted = nanoTicker.getAsLong();
                preflightStarted = true;
                requireFileInput(element, upload.fileCount());
                sendKeysStarted = true;
                element.sendKeys(upload.payload());
                emit(UiTestLensEventType.LOCATOR_ACTION_PASSED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                        "Locator action passed", "upload", attempt, null, null,
                        Map.of("fileCount", String.valueOf(upload.fileCount())));
                return this;
            } catch (RuntimeException failure) {
                lastFailure = failure;
                if (sendKeysStarted || !shouldRetry(failure) || attempt >= options.maxRetries()) {
                    emit(UiTestLensEventType.LOCATOR_ACTION_FAILED, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR,
                            "Locator action failed", "upload", attempt, null, null,
                            Map.of("fileCount", String.valueOf(upload.fileCount())));
                    throw locatorException("upload", failure, "");
                }
                if (preflightStarted) {
                    emitRecoveryRetry("action", "upload", attempt, attempt + 1,
                            elapsedNanos(attemptStarted), effectiveRetryCause(failure), null);
                }
            }
        }
        throw locatorException("upload", lastFailure, "");
    }

    /** Focuses the current element without scrolling or clicking it. */
    public UiLocator focus() {
        return executeJavaScript("focus", """
                const element = arguments[0];
                try { element.focus({preventScroll: true}); }
                catch (ignored) { element.focus(); }
                """);
    }

    /** Scrolls the current element to the center/nearest viewport position. */
    public UiLocator scrollIntoView() {
        return executeJavaScript("scrollIntoView", """
                arguments[0].scrollIntoView({
                  block: "center",
                  inline: "nearest",
                  behavior: "instant"
                });
                """);
    }

    private static Throwable effectiveRetryCause(Throwable failure) {
        Throwable current = failure;
        Throwable effective = failure;
        java.util.Set<Throwable> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        while (current != null && seen.add(current)) {
            if (current instanceof StaleElementReferenceException
                    || current instanceof ElementClickInterceptedException
                    || current instanceof ElementNotInteractableException) {
                effective = current;
            }
            current = current.getCause();
        }
        return effective;
    }

    private long elapsedNanos(long started) {
        return Math.max(0, nanoTicker.getAsLong() - started);
    }

    private void emitRecoveryRetry(String kind, String action, int attempt, int nextAttempt,
                                   long failedAttemptDurationNanos, Throwable cause, Integer valueLength) {
        try {
            UiTestLensLogEntry.Builder builder = UiTestLensLogEntry.builder()
                    .level(UiTestLensLogLevel.WARN)
                    .eventType(UiTestLensEventType.LOCATOR_RETRY)
                    .status(UiTestLensStatus.WARN)
                    .message("Retrying locator " + kind + ": " + description.displayName())
                    .action("locator." + action)
                    .metadata("locator", by().toString())
                    .metadata("description", description.displayName())
                    .metadata("retryKind", "recovery")
                    .metadata("retryAction", action)
                    .metadata("retryLocator", by().toString())
                    .metadata("attempt", String.valueOf(attempt))
                    .metadata("nextAttempt", String.valueOf(nextAttempt))
                    .metadata("exceptionType", cause == null ? "" : cause.getClass().getName())
                    .metadata("failedAttemptDurationNanos", String.valueOf(failedAttemptDurationNanos))
                    .throwable(cause);
            if (valueLength != null) builder.metadata("valueLength", String.valueOf(valueLength));
            logger.emit(builder.build());
        } catch (RuntimeException ignored) {
            // Retry diagnostics must not alter the operation.
        }
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
        emit(eventType, status, level, message, action, attempt, valueLength, throwable, Map.of());
    }

    private void emit(UiTestLensEventType eventType,
                      UiTestLensStatus status,
                      UiTestLensLogLevel level,
                      String message,
                      String action,
                      int attempt,
                      Integer valueLength,
                      Throwable throwable,
                      Map<String, String> extraMetadata) {
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
            extraMetadata.forEach(builder::metadata);
            logger.emit(builder.build());
        } catch (Exception ignored) {
        }
    }

    private void emitControl(UiTestLensEventType eventType,
                             UiTestLensStatus status,
                             UiTestLensLogLevel level,
                             String message,
                             String action,
                             int attempt,
                             CheckedActionMetadata metadata,
                             Throwable throwable) {
        emit(eventType, status, level, message, action, attempt, null, throwable, metadata.asMap());
    }

    private enum ControlKind {
        NATIVE_CHECKBOX,
        NATIVE_RADIO,
        ARIA_CHECKBOX,
        ARIA_RADIO,
        ARIA_SWITCH
    }

    private enum ActivationKind { CONTROL, LABEL }

    private enum CheckedState { CHECKED, UNCHECKED, MIXED }

    private record SemanticControl(WebElement stateElement,
                                   WebElement activationElement,
                                   ControlKind kind,
                                   ActivationKind activationKind,
                                   String tag,
                                   String type,
                                   String role) {}

    private record Confirmation(CheckedState state, int attempts) {}

    private record UploadPayload(String payload, int fileCount) {}

    private static final class CheckedActionMetadata {
        private ControlKind controlKind;
        private ActivationKind activationKind;
        private CheckedState initialState;
        private CheckedState targetState;
        private CheckedState finalState;
        private boolean clickPerformed;
        private int confirmationAttempts;

        private void observe(SemanticControl control, CheckedState state) {
            controlKind = control.kind();
            activationKind = control.activationKind();
            if (initialState == null) initialState = state;
        }

        private Map<String, String> asMap() {
            Map<String, String> values = new LinkedHashMap<>();
            values.put("controlKind", name(controlKind));
            values.put("activationKind", name(activationKind));
            values.put("initialState", name(initialState));
            values.put("targetState", name(targetState));
            values.put("finalState", name(finalState));
            values.put("clickPerformed", String.valueOf(clickPerformed));
            values.put("confirmationAttempts", String.valueOf(confirmationAttempts));
            return values;
        }

        private static String name(Enum<?> value) {
            return value == null ? "" : value.name();
        }
    }
}

