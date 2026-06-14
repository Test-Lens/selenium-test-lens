package io.github.mmaciekk111.uitestlens.selenium.business;

import io.github.mmaciekk111.uitestlens.selenium.assertions.UiAssertionResult;

import java.time.Duration;

public final class BusinessAssertionFailure {
    private final String subject;
    private final String description;
    private final String message;
    private final Throwable cause;
    private final String assertionSummary;
    private final Duration elapsed;

    public BusinessAssertionFailure(String subject,
                                    String description,
                                    String message,
                                    Throwable cause,
                                    String assertionSummary,
                                    Duration elapsed) {
        this.subject = safe(subject);
        this.description = safe(description);
        this.message = safe(message);
        this.cause = cause;
        this.assertionSummary = safe(assertionSummary);
        this.elapsed = elapsed == null ? Duration.ZERO : elapsed;
    }

    public static BusinessAssertionFailure fromAssertion(String subject,
                                                         String description,
                                                         UiAssertionResult result,
                                                         Throwable cause,
                                                         Duration elapsed) {
        String summary = result == null ? "" : result.summary();
        return new BusinessAssertionFailure(subject, description, summary, cause, summary, elapsed);
    }

    public static BusinessAssertionFailure unexpected(String subject,
                                                      String description,
                                                      Throwable cause,
                                                      Duration elapsed,
                                                      int previewLimit) {
        return new BusinessAssertionFailure(subject, description, preview(messageFor(cause), previewLimit),
                cause, "", elapsed);
    }

    public String subject() {
        return subject;
    }

    public String description() {
        return description;
    }

    public String message() {
        return message;
    }

    public Throwable cause() {
        return cause;
    }

    public String assertionSummary() {
        return assertionSummary;
    }

    public Duration elapsed() {
        return elapsed;
    }

    static String messageFor(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    static String preview(String value, int limit) {
        String safe = safe(value);
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative");
        }
        return safe.length() <= limit ? safe : safe.substring(0, limit) + "...";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
