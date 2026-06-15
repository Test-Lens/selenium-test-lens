package io.github.testlens.selenium.assertions;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.selenium.locator.UiLocator;
import io.github.testlens.selenium.locator.UiLocatorException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

public final class UiExpect {
    private final UiLocator locator;
    private final UiAssertionOptions options;
    private final UiAssertionReporter reporter;

    public UiExpect(UiLocator locator, UiAssertionOptions options, OverlayLogger logger) {
        this.locator = Objects.requireNonNull(locator, "locator must not be null");
        this.options = options != null ? options : UiAssertionOptions.defaults();
        this.reporter = new UiAssertionReporter(logger);
    }

    public UiAssertionResult toBeVisible() {
        return assertUntil("toBeVisible", "", false, () -> {
            boolean visible = locator.isVisible();
            return visible
                    ? Evaluation.passed("visible", "Element is visible")
                    : Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_VISIBLE, "visible", "Element is not visible");
        });
    }

    public UiAssertionResult toBeHidden() {
        return assertUntil("toBeHidden", "", false, () -> {
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
            boolean enabled = locator.isEnabled();
            return enabled
                    ? Evaluation.passed("enabled", "Element is enabled")
                    : Evaluation.notReady(UiAssertionFailureReason.ELEMENT_NOT_ENABLED, "disabled", "Element is not enabled");
        });
    }

    public UiAssertionResult toBeDisabled() {
        return assertUntil("toBeDisabled", "", false, () -> {
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
            String actual = UiAssertionText.normalize(locator.textContent(), options);
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
            String actual = UiAssertionText.normalize(locator.textContent(), options);
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
            String actualRaw = locator.resolve().getAttribute("value");
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
            String actualRaw = locator.resolve().getAttribute("value");
            String actual = UiAssertionText.normalize(actualRaw, options);
            String actualPreview = UiAssertionText.valuePreview(actualRaw);
            return actual.contains(expectedNormalized)
                    ? Evaluation.passed(actualPreview, "Element value contained expected substring")
                    : Evaluation.notReady(UiAssertionFailureReason.VALUE_MISMATCH, actualPreview, "Element value did not contain expected substring");
        });
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
                lastEvaluation = evaluation;
                if (evaluation.passed()) {
                    UiAssertionResult result = UiAssertionResult.passed(assertionName, locator.description(), expectedPreview,
                            evaluation.actualPreview(), attempts, Duration.between(started, Instant.now()), evaluation.message());
                    reporter.passed(result);
                    return result;
                }
            } catch (RuntimeException e) {
                lastException = e;
                lastEvaluation = Evaluation.notReady(reasonFor(e), "", messageFor(e));
                if (options.failFastOnMissingElement() && isMissingElement(e)) {
                    UiAssertionResult result = UiAssertionResult.failed(assertionName, lastEvaluation.failureReason(),
                            locator.description(), expectedPreview, lastEvaluation.actualPreview(), attempts,
                            Duration.between(started, Instant.now()), lastEvaluation.message());
                    reporter.failed(result);
                    throw new UiAssertionError(result);
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
        if (e instanceof StaleElementReferenceException || messageContains(e, "stale")) {
            return UiAssertionFailureReason.STALE_ELEMENT;
        }
        return UiAssertionFailureReason.UNKNOWN;
    }

    private static boolean isMissingElement(RuntimeException e) {
        return e instanceof NoSuchElementException || messageContains(e, "not found") || messageContains(e, "missing");
    }

    private static boolean messageContains(RuntimeException e, String needle) {
        String message = e.getMessage();
        return message != null && message.toLowerCase().contains(needle);
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
}

