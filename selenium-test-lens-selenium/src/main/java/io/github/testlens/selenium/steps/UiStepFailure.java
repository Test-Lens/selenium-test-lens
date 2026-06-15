package io.github.testlens.selenium.steps;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class UiStepFailure {
    private final String message;
    private final Throwable cause;
    private final String causeType;
    private final String stackTrace;

    UiStepFailure(String message, Throwable cause, String causeType, String stackTrace) {
        this.message = safe(message);
        this.cause = cause;
        this.causeType = safe(causeType);
        this.stackTrace = safe(stackTrace);
    }

    public static UiStepFailure from(Throwable cause, UiStepOptions options) {
        UiStepOptions effectiveOptions = options == null ? UiStepOptions.defaults() : options;
        String message = preview(messageFor(cause), effectiveOptions.messagePreviewLimit());
        return new UiStepFailure(
                message,
                cause,
                cause == null ? "" : cause.getClass().getName(),
                effectiveOptions.includeStackTrace() && cause != null ? preview(stackTrace(cause), effectiveOptions.messagePreviewLimit()) : ""
        );
    }

    public String message() {
        return message;
    }

    public Throwable cause() {
        return cause;
    }

    public String causeType() {
        return causeType;
    }

    public String stackTrace() {
        return stackTrace;
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

    private static String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
