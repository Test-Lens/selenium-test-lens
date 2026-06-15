package io.github.testlens.selenium.assertions;

import java.time.Duration;
import java.util.Objects;

public final class UiAssertionResult {
    private final String assertionName;
    private final UiAssertionStatus status;
    private final UiAssertionFailureReason failureReason;
    private final String locatorDescription;
    private final String expectedPreview;
    private final String actualPreview;
    private final int attempts;
    private final Duration elapsed;
    private final String message;

    private UiAssertionResult(String assertionName,
                              UiAssertionStatus status,
                              UiAssertionFailureReason failureReason,
                              String locatorDescription,
                              String expectedPreview,
                              String actualPreview,
                              int attempts,
                              Duration elapsed,
                              String message) {
        this.assertionName = safe(assertionName);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.failureReason = failureReason;
        this.locatorDescription = safe(locatorDescription);
        this.expectedPreview = safe(expectedPreview);
        this.actualPreview = safe(actualPreview);
        this.attempts = Math.max(0, attempts);
        this.elapsed = elapsed == null ? Duration.ZERO : elapsed;
        this.message = safe(message);
    }

    public static UiAssertionResult passed(String assertionName,
                                           String locatorDescription,
                                           String expectedPreview,
                                           String actualPreview,
                                           int attempts,
                                           Duration elapsed,
                                           String message) {
        return new UiAssertionResult(assertionName, UiAssertionStatus.PASSED, null, locatorDescription,
                expectedPreview, actualPreview, attempts, elapsed, message);
    }

    public static UiAssertionResult failed(String assertionName,
                                           UiAssertionFailureReason failureReason,
                                           String locatorDescription,
                                           String expectedPreview,
                                           String actualPreview,
                                           int attempts,
                                           Duration elapsed,
                                           String message) {
        return new UiAssertionResult(assertionName, UiAssertionStatus.FAILED,
                failureReason == null ? UiAssertionFailureReason.UNKNOWN : failureReason, locatorDescription,
                expectedPreview, actualPreview, attempts, elapsed, message);
    }

    public static UiAssertionResult timedOut(String assertionName,
                                             UiAssertionFailureReason failureReason,
                                             String locatorDescription,
                                             String expectedPreview,
                                             String actualPreview,
                                             int attempts,
                                             Duration elapsed,
                                             String message) {
        return new UiAssertionResult(assertionName, UiAssertionStatus.TIMED_OUT,
                failureReason == null ? UiAssertionFailureReason.TIMEOUT : failureReason, locatorDescription,
                expectedPreview, actualPreview, attempts, elapsed, message);
    }

    public String assertionName() {
        return assertionName;
    }

    public UiAssertionStatus status() {
        return status;
    }

    public UiAssertionFailureReason failureReason() {
        return failureReason;
    }

    public String locatorDescription() {
        return locatorDescription;
    }

    public String expectedPreview() {
        return expectedPreview;
    }

    public String actualPreview() {
        return actualPreview;
    }

    public int attempts() {
        return attempts;
    }

    public Duration elapsed() {
        return elapsed;
    }

    public String message() {
        return message;
    }

    public boolean isPassed() {
        return status == UiAssertionStatus.PASSED;
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append(assertionName).append(" ").append(status);
        if (!locatorDescription.isBlank()) {
            sb.append(" on ").append(locatorDescription);
        }
        if (failureReason != null) {
            sb.append(" reason=").append(failureReason);
        }
        if (!expectedPreview.isBlank()) {
            sb.append(" expected=[").append(expectedPreview).append("]");
        }
        if (!actualPreview.isBlank()) {
            sb.append(" actual=[").append(actualPreview).append("]");
        }
        sb.append(" attempts=").append(attempts);
        sb.append(" elapsedMs=").append(elapsed.toMillis());
        if (!message.isBlank()) {
            sb.append(" message=").append(message);
        }
        return sb.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
