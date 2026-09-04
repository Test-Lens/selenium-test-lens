package io.github.testlens.core.logging;

import io.github.testlens.core.redaction.RedactionPolicy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight fan-out logger used by Selenium Test Lens components.
 *
 * <p>Sink failures are isolated so diagnostics do not break browser automation flows.
 */
public final class UiTestLensLogger {
    private static final UiTestLensLogger NOOP = new UiTestLensLogger(List.of(), RedactionPolicy.defaults());

    private final List<UiTestLensLogSink> sinks;
    private final RedactionPolicy redactionPolicy;

    private UiTestLensLogger(List<UiTestLensLogSink> sinks, RedactionPolicy redactionPolicy) {
        this.sinks = List.copyOf(sinks);
        this.redactionPolicy = redactionPolicy == null ? RedactionPolicy.defaults() : redactionPolicy;
    }

    public static UiTestLensLogger noop() {
        return NOOP;
    }

    public static Builder builder() {
        return new Builder();
    }

    public RedactionPolicy redactionPolicy() { return redactionPolicy; }

    public void emit(UiTestLensLogEntry entry) {
        if (entry == null || sinks.isEmpty()) {
            return;
        }
        UiTestLensLogEntry safe = safeEntry(entry);
        for (UiTestLensLogSink sink : sinks) {
            try {
                sink.accept(safe);
            } catch (Exception ignored) {
                // Logging must not break browser automation tests.
            }
        }
    }

    public void trace(String message) {
        emit(UiTestLensLogEntry.builder().level(UiTestLensLogLevel.TRACE).message(message).build());
    }

    public void debug(String message) {
        emit(UiTestLensLogEntry.builder().level(UiTestLensLogLevel.DEBUG).message(message).build());
    }

    public void info(String message) {
        emit(UiTestLensLogEntry.info(message));
    }

    public void warn(String message) {
        emit(UiTestLensLogEntry.warn(message));
    }

    public void error(String message) {
        error(message, null);
    }

    public void error(String message, Throwable throwable) {
        emit(UiTestLensLogEntry.error(message, throwable));
    }

    public UiTestLensLogger withSink(UiTestLensLogSink sink) {
        if (sink == null) {
            return this;
        }
        List<UiTestLensLogSink> copy = new ArrayList<>(sinks);
        copy.add(sink);
        return new UiTestLensLogger(copy, redactionPolicy);
    }

    public static final class Builder {
        private final List<UiTestLensLogSink> sinks = new ArrayList<>();
        private RedactionPolicy redactionPolicy = RedactionPolicy.defaults();

        private Builder() {
        }

        public Builder sink(UiTestLensLogSink sink) {
            if (sink != null) {
                sinks.add(sink);
            }
            return this;
        }

        public Builder redactionPolicy(RedactionPolicy policy) {
            redactionPolicy = policy == null ? RedactionPolicy.defaults() : policy;
            return this;
        }

        public UiTestLensLogger build() {
            return new UiTestLensLogger(sinks, redactionPolicy);
        }
    }

    private UiTestLensLogEntry safeEntry(UiTestLensLogEntry entry) {
        TargetDescriptor target = entry.target();
        Map<String, String> targetMetadata = redactMap(target == null ? Map.of() : target.metadata());
        TargetDescriptor safeTarget = target == null ? TargetDescriptor.none() : new TargetDescriptor(
                redact(target.selector()), redact(target.label()), redact(target.tagName()), redact(target.text()), targetMetadata);
        return new UiTestLensLogEntry(entry.timestamp(), entry.level(), entry.eventType(), entry.status(),
                redact(entry.message()), redact(entry.step()), redact(entry.action()), safeTarget,
                redactMap(entry.metadata()), redactThrowable(entry.throwable(), 0));
    }

    private Map<String, String> redactMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) return Map.of();
        Map<String, String> safe = new LinkedHashMap<>();
        values.forEach((key, value) -> safe.put(key, redact(key, value)));
        return safe;
    }

    private String redact(String value) {
        try { return redactionPolicy.redact(value); }
        catch (RuntimeException failure) { return "[REDACTION_FAILED]"; }
    }

    private String redact(String key, String value) {
        try { return redactionPolicy.redact(key, value); }
        catch (RuntimeException failure) { return "[REDACTION_FAILED]"; }
    }

    private Throwable redactThrowable(Throwable throwable, int depth) {
        if (throwable == null || depth >= 16) return null;
        Throwable cause = redactThrowable(throwable.getCause(), depth + 1);
        DiagnosticThrowable safe = new DiagnosticThrowable(throwable.getClass().getName(),
                redact(throwable.getMessage()), cause);
        safe.setStackTrace(throwable.getStackTrace());
        for (Throwable suppressed : throwable.getSuppressed()) {
            Throwable redacted = redactThrowable(suppressed, depth + 1);
            if (redacted != null) safe.addSuppressed(redacted);
        }
        return safe;
    }

    private static final class DiagnosticThrowable extends RuntimeException {
        private final String originalType;
        private DiagnosticThrowable(String originalType, String message, Throwable cause) {
            super(message, cause, true, true);
            this.originalType = originalType;
        }
        @Override public String toString() { return originalType + (getMessage() == null ? "" : ": " + getMessage()); }
    }
}

