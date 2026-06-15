package io.github.testlens.core.trace;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Failure details attached to a trace event.
 *
 * <p>Stack traces are optional so callers can choose compact or detailed reports.
 */
public final class TraceFailure {
    private final String message;
    private final String exceptionType;
    private final String stackTrace;
    private final Map<String, String> details;

    public TraceFailure(String message, String exceptionType, String stackTrace, Map<String, String> details) {
        this.message = safe(message);
        this.exceptionType = safe(exceptionType);
        this.stackTrace = safe(stackTrace);
        this.details = immutableCopy(details);
    }

    public static TraceFailure from(Throwable throwable, boolean includeStackTrace) {
        if (throwable == null) {
            return new TraceFailure("", "", "", Map.of());
        }
        return new TraceFailure(
                messageFor(throwable),
                throwable.getClass().getName(),
                includeStackTrace ? stackTrace(throwable) : "",
                Map.of()
        );
    }

    public TraceFailure withDetail(String key, String value) {
        if (key == null || key.isBlank() || value == null) {
            return this;
        }
        Map<String, String> copy = new LinkedHashMap<>(details);
        copy.put(key, value);
        return new TraceFailure(message, exceptionType, stackTrace, copy);
    }

    public String message() {
        return message;
    }

    public String exceptionType() {
        return exceptionType;
    }

    public String stackTrace() {
        return stackTrace;
    }

    public Map<String, String> details() {
        return details;
    }

    private static String messageFor(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private static Map<String, String> immutableCopy(Map<String, String> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

