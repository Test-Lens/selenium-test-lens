package io.github.testlens.selenium.assertions;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.selenium.locator.UiLocator;
import io.github.testlens.selenium.locator.UiLocatorException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

public final class UiExpect {
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
                    return Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_FOUND, "missing", "Element is not present");
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
                    return Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_FOUND, "missing", "Element is not present");
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
                    return Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_FOUND, "missing", "Element is not present");
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
                return Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_FOUND, "missing", "Element is not present");
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
                return Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_FOUND, "missing", "Element is not present");
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
                return Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_FOUND, "missing", "Element is not present");
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
                return Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_FOUND, "missing", "Element is not present");
            }
            String actualRaw = probeResult.value();
            String actual = UiAssertionText.normalize(actualRaw, options);
            String actualPreview = UiAssertionText.valuePreview(actualRaw);
            return actual.contains(expectedNormalized)
                    ? Evaluation.passed(actualPreview, "Element value contained expected substring")
                    : Evaluation.notReady(UiAssertionFailureReason.VALUE_MISMATCH, actualPreview, "Element value did not contain expected substring");
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
                if (options.failFastOnMissingElement()
                        && evaluation.failureReason() == UiAssertionFailureReason.ELEMENT_NOT_FOUND) {
                    throw failedAssertion(assertionName, expectedPreview, evaluation, attempts, started);
                }
            } catch (RuntimeException e) {
                lastException = e;
                lastEvaluation = Evaluation.notReady(reasonFor(e), "", messageFor(e));
                if (!isRetryableAssertionMiss(e)) {
                    throw failedAssertion(assertionName, expectedPreview, lastEvaluation, attempts, started);
                }
                if (options.failFastOnMissingElement() && isMissingElement(e)) {
                    throw failedAssertion(assertionName, expectedPreview, lastEvaluation, attempts, started);
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
                throw new UiAssertionError(result);
            }

            reporter.retry(assertionName, locator.description(), attempts, expectedPreview, lastEvaluation.actualPreview());
            LockSupport.parkNanos(options.pollInterval().toNanos());
        }
    }

    private UiAssertionError failedAssertion(String assertionName,
                                               String expectedPreview,
                                               Evaluation evaluation,
                                               int attempts,
                                               Instant started) {
        UiAssertionResult result = UiAssertionResult.failed(assertionName, evaluation.failureReason(),
                locator.description(), expectedPreview, evaluation.actualPreview(), attempts,
                Duration.between(started, Instant.now()), evaluation.message());
        reporter.failed(result);
        return new UiAssertionError(result);
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

    private static UiAssertionFailureReason reasonFor(RuntimeException e) {
        if (isMissingElement(e)) {
            return UiAssertionFailureReason.ELEMENT_NOT_FOUND;
        }
        if (isStaleElement(e)) {
            return UiAssertionFailureReason.STALE_ELEMENT;
        }
        return UiAssertionFailureReason.UNKNOWN;
    }

    private static boolean isMissingElement(RuntimeException e) {
        return e instanceof NoSuchElementException || e instanceof UiLocatorException locatorException
                && locatorException.getCause() instanceof NoSuchElementException;
    }

    private static boolean isRetryableAssertionMiss(RuntimeException e) {
        return isMissingElement(e) || isStaleElement(e);
    }

    private static boolean isStaleElement(RuntimeException e) {
        return e instanceof StaleElementReferenceException || e instanceof UiLocatorException locatorException
                && locatorException.getCause() instanceof StaleElementReferenceException;
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
                              String message) {
        private static Evaluation passed(String actualPreview, String message) {
            return new Evaluation(true, null, actualPreview, message);
        }

        private static Evaluation notReady(UiAssertionFailureReason failureReason, String actualPreview, String message) {
            return new Evaluation(false, failureReason, actualPreview, message);
        }
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

