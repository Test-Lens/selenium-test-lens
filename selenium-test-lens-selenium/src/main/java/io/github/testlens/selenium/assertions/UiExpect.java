package io.github.testlens.selenium.assertions;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.selenium.locator.UiLocator;
import io.github.testlens.selenium.locator.UiLocatorException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

public final class UiExpect {
    private static final String INTERNAL_OBSERVATION_PREFIX = "test-lens-assertion:";
    private static final String DOM_ATTRIBUTE_MISSING = "\u0000test-lens:dom-attribute-missing";
    private static final String DOM_ATTRIBUTE_PRESENT = "\u0000test-lens:dom-attribute-present:";
    private static final Set<String> ARIA_SELECTED_ROLES = Set.of(
            "option", "tab", "row", "gridcell", "rowheader", "columnheader", "treeitem");
    private final UiLocator locator;
    private final UiAssertionOptions options;
    private final UiAssertionReporter reporter;
    private final VisibilityProbe visibilityProbe;
    private final ElementProbe elementProbe;

    public UiExpect(UiLocator locator, UiAssertionOptions options, OverlayLogger logger) {
        this(locator, options, logger, null, null);
    }

    public UiExpect(UiLocator locator, UiAssertionOptions options, OverlayLogger logger, VisibilityProbe visibilityProbe) {
        this(locator, options, logger, visibilityProbe, null);
    }

    public UiExpect(UiLocator locator, UiAssertionOptions options, OverlayLogger logger, VisibilityProbe visibilityProbe, ElementProbe elementProbe) {
        this.locator = Objects.requireNonNull(locator, "locator must not be null");
        this.options = options != null ? options : UiAssertionOptions.defaults();
        this.reporter = new UiAssertionReporter(logger);
        this.visibilityProbe = visibilityProbe;
        this.elementProbe = elementProbe;
    }

    public UiAssertionResult toBeVisible() {
        return assertUntil("toBeVisible", "", false, () -> {
            if (elementProbe != null) {
                ElementProbeResult probeResult = elementProbe.probe(element -> String.valueOf(element.isDisplayed()));
                if (!probeResult.present()) {
                    return missingElement();
                }
                boolean visible = Boolean.parseBoolean(probeResult.value());
                return visible
                        ? Evaluation.passed("visible", "Element is visible")
                        : Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_VISIBLE, "hidden", "Element is not visible");
            }
            boolean visible = locator.isVisible();
            return visible
                    ? Evaluation.passed("visible", "Element is visible")
                    : Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_VISIBLE, "visible", "Element is not visible");
        });
    }

    public UiAssertionResult toBeHidden() {
        return assertUntil("toBeHidden", "", false, () -> {
            if (visibilityProbe != null) {
                VisibilityProbeResult probeResult = visibilityProbe.probe();
                if (!probeResult.present()) {
                    return Evaluation.passed("missing", "Element is not present");
                }
                return !probeResult.visible()
                        ? Evaluation.passed("hidden", "Element is hidden")
                        : Evaluation.notReady(UiAssertionFailureReason.ELEMENT_STILL_VISIBLE, "visible", "Element is still visible");
            }
            try {
                boolean visible = locator.isVisible();
                return !visible
                        ? Evaluation.passed("hidden", "Element is hidden")
                        : Evaluation.notReady(UiAssertionFailureReason.ELEMENT_STILL_VISIBLE, "visible", "Element is still visible");
            } catch (UiLocatorException | NoSuchElementException e) {
                return Evaluation.passed("missing", "Element is not present");
            }
        });
    }

    public UiAssertionResult toBeEnabled() {
        return assertUntil("toBeEnabled", "", false, () -> {
            if (elementProbe != null) {
                ElementProbeResult probeResult = elementProbe.probe(element -> String.valueOf(element.isEnabled()));
                if (!probeResult.present()) {
                    return missingElement();
                }
                boolean enabled = Boolean.parseBoolean(probeResult.value());
                return enabled
                        ? Evaluation.passed("enabled", "Element is enabled")
                        : Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_ENABLED, "disabled", "Element is not enabled");
            }
            boolean enabled = locator.isEnabled();
            return enabled
                    ? Evaluation.passed("enabled", "Element is enabled")
                    : Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_ENABLED, "disabled", "Element is not enabled");
        });
    }

    public UiAssertionResult toBeDisabled() {
        return assertUntil("toBeDisabled", "", false, () -> {
            if (elementProbe != null) {
                ElementProbeResult probeResult = elementProbe.probe(element -> String.valueOf(element.isEnabled()));
                if (!probeResult.present()) {
                    return missingElement();
                }
                boolean enabled = Boolean.parseBoolean(probeResult.value());
                return !enabled
                        ? Evaluation.passed("disabled", "Element is disabled")
                        : Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_DISABLED, "enabled", "Element is still enabled");
            }
            boolean enabled = locator.isEnabled();
            return !enabled
                    ? Evaluation.passed("disabled", "Element is disabled")
                    : Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_DISABLED, "enabled", "Element is still enabled");
        });
    }

    public UiAssertionResult toHaveText(String expected) {
        String expectedNormalized = UiAssertionText.normalize(expected, options);
        String expectedPreview = UiAssertionText.preview(expectedNormalized, options.actualTextPreviewLimit());
        return assertUntil("toHaveText", expectedPreview, false, () -> {
            ElementProbeResult probeResult = probeTextContent();
            if (!probeResult.present()) {
                return missingElement();
            }
            String actual = UiAssertionText.normalize(probeResult.value(), options);
            String actualPreview = UiAssertionText.preview(actual, options.actualTextPreviewLimit());
            return actual.equals(expectedNormalized)
                    ? Evaluation.passed(actualPreview, "Element text matched")
                    : Evaluation.notReady(UiAssertionFailureReason.TEXT_MISMATCH, actualPreview, "Element text did not match");
        });
    }

    public UiAssertionResult toContainText(String expectedSubstring) {
        String expectedNormalized = UiAssertionText.normalize(expectedSubstring, options);
        String expectedPreview = UiAssertionText.preview(expectedNormalized, options.actualTextPreviewLimit());
        return assertUntil("toContainText", expectedPreview, false, () -> {
            ElementProbeResult probeResult = probeTextContent();
            if (!probeResult.present()) {
                return missingElement();
            }
            String actual = UiAssertionText.normalize(probeResult.value(), options);
            String actualPreview = UiAssertionText.preview(actual, options.actualTextPreviewLimit());
            return actual.contains(expectedNormalized)
                    ? Evaluation.passed(actualPreview, "Element text contained expected substring")
                    : Evaluation.notReady(UiAssertionFailureReason.TEXT_MISMATCH, actualPreview, "Element text did not contain expected substring");
        });
    }

    public UiAssertionResult toHaveValue(String expected) {
        String expectedNormalized = UiAssertionText.normalize(expected, options);
        String expectedPreview = UiAssertionText.valuePreview(expected);
        return assertUntil("toHaveValue", expectedPreview, true, () -> {
            ElementProbeResult probeResult = probeValue();
            if (!probeResult.present()) {
                return missingElement();
            }
            String actualRaw = probeResult.value();
            String actual = UiAssertionText.normalize(actualRaw, options);
            String actualPreview = UiAssertionText.valuePreview(actualRaw);
            return actual.equals(expectedNormalized)
                    ? Evaluation.passed(actualPreview, "Element value matched")
                    : Evaluation.notReady(UiAssertionFailureReason.VALUE_MISMATCH, actualPreview, "Element value did not match");
        });
    }

    public UiAssertionResult toContainValue(String expectedSubstring) {
        String expectedNormalized = UiAssertionText.normalize(expectedSubstring, options);
        String expectedPreview = UiAssertionText.valuePreview(expectedSubstring);
        return assertUntil("toContainValue", expectedPreview, true, () -> {
            ElementProbeResult probeResult = probeValue();
            if (!probeResult.present()) {
                return missingElement();
            }
            String actualRaw = probeResult.value();
            String actual = UiAssertionText.normalize(actualRaw, options);
            String actualPreview = UiAssertionText.valuePreview(actualRaw);
            return actual.contains(expectedNormalized)
                    ? Evaluation.passed(actualPreview, "Element value contained expected substring")
                    : Evaluation.notReady(UiAssertionFailureReason.VALUE_MISMATCH, actualPreview, "Element value did not contain expected substring");
        });
    }

    public UiAssertionResult toHaveCount(int expected) {
        if (expected < 0) throw new IllegalArgumentException("expected count must be >= 0");
        return assertUntil("toHaveCount", String.valueOf(expected), false, () -> {
            int actual = Integer.parseInt(observe("count"));
            if (actual == expected) return Evaluation.passed(String.valueOf(actual), "Element count matched");
            return Evaluation.notReady(UiAssertionFailureReason.COUNT_MISMATCH, String.valueOf(actual),
                    "Element count did not match", expected > 0 && actual == 0);
        });
    }

    public UiAssertionResult toHaveAttribute(String attributeName, String expectedValue) {
        if (attributeName == null || attributeName.isBlank()) {
            throw new IllegalArgumentException("attribute name must not be blank");
        }
        Objects.requireNonNull(expectedValue, "expected attribute value must not be null");
        String expectedPreview = attributeName + " present expectedLength=" + expectedValue.length();
        return assertUntil("toHaveAttribute", expectedPreview, false, () -> {
            ElementProbeResult probe = probeElement(element -> {
                String value = element.getDomAttribute(attributeName);
                return value == null ? DOM_ATTRIBUTE_MISSING : DOM_ATTRIBUTE_PRESENT + value;
            });
            if (!probe.present()) return missingElement();
            boolean attributePresent = probe.value().startsWith(DOM_ATTRIBUTE_PRESENT);
            String actual = attributePresent ? probe.value().substring(DOM_ATTRIBUTE_PRESENT.length()) : null;
            String actualPreview = !attributePresent
                    ? attributeName + " missing"
                    : attributeName + " present actualLength=" + actual.length();
            return expectedValue.equals(actual)
                    ? Evaluation.passed(actualPreview, "DOM attribute matched")
                    : Evaluation.notReady(UiAssertionFailureReason.ATTRIBUTE_MISMATCH, actualPreview,
                    "DOM attribute did not match");
        });
    }

    public UiAssertionResult toHaveClass(String className) {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("class name must not be blank");
        }
        if (containsHtmlAsciiWhitespace(className)) {
            throw new IllegalArgumentException("class name must be one token without HTML whitespace");
        }
        String expectedPreview = UiAssertionText.preview(className, options.actualTextPreviewLimit());
        return assertUntil("toHaveClass", expectedPreview, false, () -> {
            ElementProbeResult probe = probeElement(element -> element.getDomAttribute("class"));
            if (!probe.present()) return missingElement();
            String[] tokens = classTokens(probe.value());
            for (String token : tokens) {
                if (token.equals(className)) {
                    return Evaluation.passed("classTokenCount=" + tokens.length, "Class token was present");
                }
            }
            return Evaluation.notReady(UiAssertionFailureReason.CLASS_MISMATCH,
                    "classTokenCount=" + tokens.length, "Class token was not present");
        });
    }

    public UiAssertionResult toHaveCss(String propertyName, String expectedValue) {
        if (propertyName == null || propertyName.isBlank()) {
            throw new IllegalArgumentException("CSS property name must not be blank");
        }
        Objects.requireNonNull(expectedValue, "expected CSS value must not be null");
        String expected = expectedValue;
        String expectedPreview = cssPreview(expected);
        return assertUntil("toHaveCss", propertyName + "=" + expectedPreview, false, () -> {
            ElementProbeResult probe = probeElement(element -> element.getCssValue(propertyName));
            if (!probe.present()) return missingElement();
            String actual = probe.value() == null ? "" : probe.value().trim();
            String actualPreview = propertyName + "=" + cssPreview(actual);
            return actual.equals(expected)
                    ? Evaluation.passed(actualPreview, "Computed CSS matched")
                    : Evaluation.notReady(UiAssertionFailureReason.CSS_MISMATCH, actualPreview,
                    "Computed CSS did not match");
        });
    }

    public UiAssertionResult toBeSelected() {
        return assertUntil("toBeSelected", "selected", false, () -> {
            ElementProbeResult probe = probeElement(UiExpect::selectedState);
            if (!probe.present()) return missingElement();
            return "SELECTED".equals(probe.value())
                    ? Evaluation.passed("selected", "Element is selected")
                    : Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_SELECTED, "not selected",
                    "Element is not selected");
        });
    }

    public UiAssertionResult toBeChecked() {
        return checkedAssertion("toBeChecked", "CHECKED", UiAssertionFailureReason.ELEMENT_NOT_CHECKED);
    }

    public UiAssertionResult toBeUnchecked() {
        return checkedAssertion("toBeUnchecked", "UNCHECKED", UiAssertionFailureReason.ELEMENT_STILL_CHECKED);
    }

    public UiAssertionResult toBeAttached() {
        return assertUntil("toBeAttached", "attached", false, () -> {
            int count = Integer.parseInt(observe("count"));
            return count > 0
                    ? Evaluation.passed("attached count=" + count, "Element is attached")
                    : Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_ATTACHED, "detached count=0",
                    "Element is not attached", true);
        });
    }

    public UiAssertionResult toBeDetached() {
        return assertUntil("toBeDetached", "detached", false, () -> {
            int count = Integer.parseInt(observe("count"));
            return count == 0
                    ? Evaluation.passed("detached count=0", "Element is detached")
                    : Evaluation.notReady(UiAssertionFailureReason.ELEMENT_STILL_ATTACHED,
                    "attached count=" + count, "Element is still attached");
        });
    }

    private ElementProbeResult probeTextContent() {
        if (elementProbe != null) {
            return elementProbe.probe(WebElement::getText);
        }
        return ElementProbeResult.present(locator.textContent());
    }

    private ElementProbeResult probeValue() {
        if (elementProbe != null) {
            return elementProbe.probe(element -> element.getAttribute("value"));
        }
        return ElementProbeResult.present(locator.resolve().getAttribute("value"));
    }

    private ElementProbeResult probeElement(Function<WebElement, String> reader) {
        if (elementProbe == null) throw new IllegalStateException("Assertion observation is unavailable");
        return elementProbe.probe(reader);
    }

    private String observe(String observation) {
        if (elementProbe == null) throw new IllegalStateException("Assertion observation is unavailable");
        ElementProbeResult result = elementProbe.probe(new ObservationRequest(observation));
        return result.value();
    }

    private UiAssertionResult checkedAssertion(String assertionName,
                                               String expectedState,
                                               UiAssertionFailureReason mismatchReason) {
        return assertUntil(assertionName, expectedState.toLowerCase(Locale.ROOT), false, () -> {
            ElementProbeResult probe = probeElement(new ObservationRequest("checkedState"));
            if (!probe.present()) return missingElement();
            String state = probe.value();
            return expectedState.equals(state)
                    ? Evaluation.passed(state.toLowerCase(Locale.ROOT), "Checked state matched")
                    : Evaluation.notReady(mismatchReason,
                    state == null ? "unknown" : state.toLowerCase(Locale.ROOT), "Checked state did not match");
        });
    }

    private static Evaluation missingElement() {
        return Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_FOUND,
                "missing", "Element is not present", true);
    }

    private static String selectedState(WebElement element) {
        if ("option".equalsIgnoreCase(element.getTagName())) {
            return element.isSelected() ? "SELECTED" : "NOT_SELECTED";
        }
        String explicitRole = safeLower(element.getDomAttribute("role"));
        String role = explicitRole.isEmpty() ? safeLower(element.getAriaRole()) : explicitRole;
        if (!ARIA_SELECTED_ROLES.contains(role)) {
            throw new UnsupportedOperationException("Element does not expose a supported selected state");
        }
        String selected = safeLower(element.getDomAttribute("aria-selected"));
        return switch (selected) {
            case "true" -> "SELECTED";
            case "false" -> "NOT_SELECTED";
            default -> throw new UnsupportedOperationException(
                    "Element has no valid aria-selected state for role " + role);
        };
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean containsHtmlAsciiWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '\t' || current == '\n' || current == '\f' || current == '\r' || current == ' ') {
                return true;
            }
        }
        return false;
    }

    private static String[] classTokens(String value) {
        if (value == null || value.isBlank()) return new String[0];
        return value.trim().split("[\\t\\n\\f\\r ]+");
    }

    private String cssPreview(String value) {
        String redacted = value == null ? "" : value.replaceAll("(?i)url\\s*\\([^)]*\\)", "url(***)");
        return UiAssertionText.preview(redacted, options.actualTextPreviewLimit());
    }

    private UiAssertionResult assertUntil(String assertionName,
                                          String expectedPreview,
                                          boolean valueAssertion,
                                          Supplier<Evaluation> evaluationSupplier) {
        Instant started = Instant.now();
        Instant deadline = started.plus(options.timeout());
        reporter.started(assertionName, locator.description());
        int attempts = 0;
        Evaluation lastEvaluation = Evaluation.notReady(UiAssertionFailureReason.UNKNOWN, "", "Assertion has not run yet");
        RuntimeException lastException = null;

        while (true) {
            attempts++;
            try {
                Evaluation evaluation = evaluationSupplier.get();
                lastException = null;
                lastEvaluation = evaluation;
                if (evaluation.passed()) {
                    UiAssertionResult result = UiAssertionResult.passed(assertionName, locator.description(), expectedPreview,
                            evaluation.actualPreview(), attempts, Duration.between(started, Instant.now()), evaluation.message());
                    reporter.passed(result);
                    return result;
                }
                if (options.failFastOnMissingElement() && evaluation.missingElement()) {
                    throw failedAssertion(assertionName, expectedPreview, evaluation, attempts, started, null);
                }
            } catch (RuntimeException e) {
                lastException = e;
                lastEvaluation = Evaluation.notReady(reasonFor(e, assertionName), "", messageFor(e));
                if (!isRetryableAssertionMiss(e)) {
                    throw failedAssertion(assertionName, expectedPreview, lastEvaluation, attempts, started, e);
                }
                if (options.failFastOnMissingElement() && isMissingElement(e)) {
                    throw failedAssertion(assertionName, expectedPreview, lastEvaluation, attempts, started, e);
                }
            }

            if (!Instant.now().plus(options.pollInterval()).isBefore(deadline)) {
                UiAssertionResult result = UiAssertionResult.timedOut(assertionName,
                        lastEvaluation.failureReason() == UiAssertionFailureReason.UNKNOWN
                                ? UiAssertionFailureReason.TIMEOUT
                                : lastEvaluation.failureReason(),
                        locator.description(), expectedPreview, lastEvaluation.actualPreview(), attempts,
                        Duration.between(started, Instant.now()), timeoutMessage(lastEvaluation, lastException, valueAssertion));
                reporter.failed(result);
                UiAssertionError error = new UiAssertionError(result);
                error.initCause(new TimeoutException(
                        timeoutMessage(lastEvaluation, lastException, valueAssertion), lastException));
                throw error;
            }

            reporter.retry(assertionName, locator.description(), attempts, expectedPreview, lastEvaluation.actualPreview());
            LockSupport.parkNanos(options.pollInterval().toNanos());
        }
    }

    private UiAssertionError failedAssertion(String assertionName,
                                               String expectedPreview,
                                               Evaluation evaluation,
                                               int attempts,
                                               Instant started,
                                               RuntimeException cause) {
        UiAssertionResult result = UiAssertionResult.failed(assertionName, evaluation.failureReason(),
                locator.description(), expectedPreview, evaluation.actualPreview(), attempts,
                Duration.between(started, Instant.now()), evaluation.message());
        reporter.failed(result);
        UiAssertionError error = new UiAssertionError(result);
        if (cause != null) error.initCause(cause);
        return error;
    }

    private static String timeoutMessage(Evaluation evaluation, RuntimeException exception, boolean valueAssertion) {
        if (exception != null) {
            return messageFor(exception);
        }
        if (evaluation.message() == null || evaluation.message().isBlank()) {
            return valueAssertion ? "Value assertion timed out" : "Assertion timed out";
        }
        return evaluation.message();
    }

    private static UiAssertionFailureReason reasonFor(RuntimeException e, String assertionName) {
        boolean stateAssertion = "toBeSelected".equals(assertionName)
                || "toBeChecked".equals(assertionName)
                || "toBeUnchecked".equals(assertionName);
        if (stateAssertion && (e instanceof UnsupportedOperationException || e instanceof IllegalStateException)) {
            return UiAssertionFailureReason.UNSUPPORTED_ELEMENT_STATE;
        }
        if (isMissingElement(e)) {
            return UiAssertionFailureReason.ELEMENT_NOT_FOUND;
        }
        if (isStaleElement(e)) {
            return UiAssertionFailureReason.STALE_ELEMENT;
        }
        return UiAssertionFailureReason.UNKNOWN;
    }

    private static boolean isMissingElement(RuntimeException e) {
        return hasCause(e, NoSuchElementException.class);
    }

    private static boolean isRetryableAssertionMiss(RuntimeException e) {
        return isMissingElement(e) || isStaleElement(e);
    }

    private static boolean isStaleElement(RuntimeException e) {
        return hasCause(e, StaleElementReferenceException.class);
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (type.isInstance(current)) return true;
        }
        return false;
    }

    private static String messageFor(RuntimeException e) {
        if (e instanceof WebDriverException && e.getMessage() != null) {
            return e.getMessage();
        }
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    private record Evaluation(boolean passed,
                              UiAssertionFailureReason failureReason,
                              String actualPreview,
                              String message,
                              boolean missingElement) {
        private static Evaluation passed(String actualPreview, String message) {
            return new Evaluation(true, null, actualPreview, message, false);
        }

        private static Evaluation notReady(UiAssertionFailureReason failureReason, String actualPreview, String message) {
            return notReady(failureReason, actualPreview, message, false);
        }

        private static Evaluation notReady(UiAssertionFailureReason failureReason,
                                           String actualPreview,
                                           String message,
                                           boolean missingElement) {
            return new Evaluation(false, failureReason, actualPreview, message, missingElement);
        }
    }

    private record ObservationRequest(String observation) implements Function<WebElement, String> {
        @Override public String apply(WebElement ignored) { return INTERNAL_OBSERVATION_PREFIX + observation; }
        @Override public String toString() { return INTERNAL_OBSERVATION_PREFIX + observation; }
    }

    @FunctionalInterface
    public interface VisibilityProbe {
        VisibilityProbeResult probe();
    }

    public record VisibilityProbeResult(boolean present, boolean visible) {
        public static VisibilityProbeResult visibleElement() {
            return new VisibilityProbeResult(true, true);
        }

        public static VisibilityProbeResult hiddenElement() {
            return new VisibilityProbeResult(true, false);
        }

        public static VisibilityProbeResult missingElement() {
            return new VisibilityProbeResult(false, false);
        }
    }

    @FunctionalInterface
    public interface ElementProbe {
        ElementProbeResult probe(Function<WebElement, String> reader);
    }

    public record ElementProbeResult(boolean present, String value) {
        public static ElementProbeResult present(String value) {
            return new ElementProbeResult(true, value == null ? "" : value);
        }

        public static ElementProbeResult missingElement() {
            return new ElementProbeResult(false, "");
        }
    }
}

