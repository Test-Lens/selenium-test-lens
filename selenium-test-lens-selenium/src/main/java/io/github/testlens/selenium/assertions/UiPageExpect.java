package io.github.testlens.selenium.assertions;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.redaction.RedactionPolicy;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

/** Polling assertions for the page in the WebDriver's current window. */
public final class UiPageExpect {
    private static final String PAGE_DESCRIPTION = "active page";

    private final WebDriver driver;
    private final UiAssertionOptions options;
    private final UiAssertionReporter reporter;
    private final RedactionPolicy redactionPolicy;

    public UiPageExpect(WebDriver driver, UiAssertionOptions options, OverlayLogger logger) {
        this.driver = Objects.requireNonNull(driver, "driver must not be null");
        this.options = options != null ? options : UiAssertionOptions.defaults();
        this.reporter = new UiAssertionReporter(logger);
        this.redactionPolicy = logger == null ? RedactionPolicy.defaults() : logger.redactionPolicy();
    }

    public UiAssertionResult toHaveUrl(String expected) {
        Objects.requireNonNull(expected, "expected URL must not be null");
        String expectedPreview = safeUrlPreview(expected, options.actualTextPreviewLimit());
        return assertUntil("toHaveUrl", UiAssertionFailureReason.URL_MISMATCH, expectedPreview, () -> {
            String actual = driver.getCurrentUrl();
            String actualPreview = safeUrlPreview(actual, options.actualTextPreviewLimit());
            return expected.equals(actual)
                    ? Evaluation.passed(actualPreview, "Page URL matched")
                    : Evaluation.notReady(actualPreview, "Page URL did not match");
        });
    }

    public UiAssertionResult toContainUrl(String expectedSubstring) {
        Objects.requireNonNull(expectedSubstring, "expected URL substring must not be null");
        String expectedPreview = "substring[length=" + expectedSubstring.length() + "]";
        return assertUntil("toContainUrl", UiAssertionFailureReason.URL_MISMATCH, expectedPreview, () -> {
            String actual = driver.getCurrentUrl();
            String actualPreview = safeUrlPreview(actual, options.actualTextPreviewLimit());
            return actual != null && actual.contains(expectedSubstring)
                    ? Evaluation.passed(actualPreview, "Page URL contained expected substring")
                    : Evaluation.notReady(actualPreview, "Page URL did not contain expected substring");
        });
    }

    public UiAssertionResult toHaveTitle(String expected) {
        Objects.requireNonNull(expected, "expected title must not be null");
        String normalizedExpected = UiAssertionText.normalize(expected, options);
        String expectedPreview = UiAssertionText.preview(normalizedExpected, options.actualTextPreviewLimit());
        return assertUntil("toHaveTitle", UiAssertionFailureReason.TITLE_MISMATCH, expectedPreview, () -> {
            String actual = UiAssertionText.normalize(driver.getTitle(), options);
            String actualPreview = UiAssertionText.preview(actual, options.actualTextPreviewLimit());
            return normalizedExpected.equals(actual)
                    ? Evaluation.passed(actualPreview, "Page title matched")
                    : Evaluation.notReady(actualPreview, "Page title did not match");
        });
    }

    public UiAssertionResult toContainTitle(String expectedSubstring) {
        Objects.requireNonNull(expectedSubstring, "expected title substring must not be null");
        String normalizedExpected = UiAssertionText.normalize(expectedSubstring, options);
        String expectedPreview = UiAssertionText.preview(normalizedExpected, options.actualTextPreviewLimit());
        return assertUntil("toContainTitle", UiAssertionFailureReason.TITLE_MISMATCH, expectedPreview, () -> {
            String actual = UiAssertionText.normalize(driver.getTitle(), options);
            String actualPreview = UiAssertionText.preview(actual, options.actualTextPreviewLimit());
            return actual.contains(normalizedExpected)
                    ? Evaluation.passed(actualPreview, "Page title contained expected substring")
                    : Evaluation.notReady(actualPreview, "Page title did not contain expected substring");
        });
    }

    private UiAssertionResult assertUntil(String assertionName,
                                          UiAssertionFailureReason mismatchReason,
                                          String expectedPreview,
                                          Supplier<Evaluation> observation) {
        Instant started = Instant.now();
        Instant deadline = started.plus(options.timeout());
        reporter.started(assertionName, PAGE_DESCRIPTION);
        int attempts = 0;
        Evaluation last = Evaluation.notReady("", "Page assertion has not run yet");

        while (true) {
            attempts++;
            try {
                last = observation.get();
            } catch (RuntimeException failure) {
                UiAssertionResult result = safe(UiAssertionResult.failed(assertionName,
                        UiAssertionFailureReason.UNKNOWN, PAGE_DESCRIPTION, expectedPreview, "", attempts,
                        Duration.between(started, Instant.now()), "Page state could not be read"));
                reporter.failed(result);
                UiAssertionError error = new UiAssertionError(result);
                error.initCause(failure);
                throw error;
            }

            if (last.passed()) {
                UiAssertionResult result = safe(UiAssertionResult.passed(assertionName, PAGE_DESCRIPTION,
                        expectedPreview, last.actualPreview(), attempts, Duration.between(started, Instant.now()),
                        last.message()));
                reporter.passed(result);
                return result;
            }

            if (!Instant.now().plus(options.pollInterval()).isBefore(deadline)) {
                UiAssertionResult result = safe(UiAssertionResult.timedOut(assertionName, mismatchReason,
                        PAGE_DESCRIPTION, expectedPreview, last.actualPreview(), attempts,
                        Duration.between(started, Instant.now()), last.message()));
                reporter.failed(result);
                UiAssertionError error = new UiAssertionError(result);
                error.initCause(new TimeoutException(last.message()));
                throw error;
            }

            reporter.retry(assertionName, PAGE_DESCRIPTION, attempts, expectedPreview, last.actualPreview());
            LockSupport.parkNanos(options.pollInterval().toNanos());
        }
    }

    private UiAssertionResult safe(UiAssertionResult result) {
        return result.redacted(redactionPolicy);
    }

    private static String safeUrlPreview(String value, int limit) {
        String raw = value == null ? "" : value;
        try {
            URI uri = new URI(raw);
            String scheme = uri.getScheme();
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();
            String preview;
            if (scheme == null) {
                if (uri.getRawAuthority() != null || uri.getUserInfo() != null) return lengthOnly(raw);
                preview = path;
            } else if (uri.getHost() != null) {
                preview = scheme + "://" + hostForPreview(uri.getHost())
                        + (uri.getPort() >= 0 ? ":" + uri.getPort() : "") + path;
            } else if ("file".equalsIgnoreCase(scheme) && uri.getRawAuthority() == null) {
                preview = scheme + ":" + path;
            } else {
                return lengthOnly(raw);
            }
            return UiAssertionText.preview(preview, limit);
        } catch (URISyntaxException | RuntimeException ignored) {
            return lengthOnly(raw);
        }
    }

    private static String hostForPreview(String host) {
        return host.indexOf(':') >= 0 ? "[" + host + "]" : host;
    }

    private static String lengthOnly(String value) {
        return "url[length=" + value.length() + "]";
    }

    private record Evaluation(boolean passed, String actualPreview, String message) {
        private static Evaluation passed(String actualPreview, String message) {
            return new Evaluation(true, actualPreview, message);
        }

        private static Evaluation notReady(String actualPreview, String message) {
            return new Evaluation(false, actualPreview, message);
        }
    }
}
