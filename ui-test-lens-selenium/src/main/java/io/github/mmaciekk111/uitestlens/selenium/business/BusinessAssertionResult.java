package io.github.mmaciekk111.uitestlens.selenium.business;

import java.time.Duration;

public final class BusinessAssertionResult {
    private final String subject;
    private final String description;
    private final BusinessAssertionStatus status;
    private final String message;
    private final Duration elapsed;
    private final BusinessAssertionFailure failure;

    private BusinessAssertionResult(String subject,
                                    String description,
                                    BusinessAssertionStatus status,
                                    String message,
                                    Duration elapsed,
                                    BusinessAssertionFailure failure) {
        this.subject = safe(subject);
        this.description = safe(description);
        this.status = status == null ? BusinessAssertionStatus.FAILED : status;
        this.message = safe(message);
        this.elapsed = elapsed == null ? Duration.ZERO : elapsed;
        this.failure = failure;
    }

    public static BusinessAssertionResult passed(String subject, String description, Duration elapsed) {
        return new BusinessAssertionResult(subject, description, BusinessAssertionStatus.PASSED,
                "Business assertion passed", elapsed, null);
    }

    public static BusinessAssertionResult failed(String subject,
                                                 String description,
                                                 BusinessAssertionFailure failure,
                                                 Duration elapsed) {
        String message = failure == null ? "Business assertion failed" : failure.message();
        return new BusinessAssertionResult(subject, description, BusinessAssertionStatus.FAILED,
                message, elapsed, failure);
    }

    public static BusinessAssertionResult skipped(String subject, String description, String message) {
        return new BusinessAssertionResult(subject, description, BusinessAssertionStatus.SKIPPED,
                message, Duration.ZERO, null);
    }

    public String subject() {
        return subject;
    }

    public String description() {
        return description;
    }

    public BusinessAssertionStatus status() {
        return status;
    }

    public String message() {
        return message;
    }

    public Duration elapsed() {
        return elapsed;
    }

    public BusinessAssertionFailure failure() {
        return failure;
    }

    public boolean isPassed() {
        return status == BusinessAssertionStatus.PASSED;
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append(description).append(" ").append(status);
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
