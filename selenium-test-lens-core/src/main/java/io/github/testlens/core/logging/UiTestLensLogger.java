package io.github.testlens.core.logging;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight fan-out logger used by Selenium Test Lens components.
 *
 * <p>Sink failures are isolated so diagnostics do not break browser automation flows.
 */
public final class UiTestLensLogger {
    private static final UiTestLensLogger NOOP = new UiTestLensLogger(List.of());

    private final List<UiTestLensLogSink> sinks;

    private UiTestLensLogger(List<UiTestLensLogSink> sinks) {
        this.sinks = List.copyOf(sinks);
    }

    public static UiTestLensLogger noop() {
        return NOOP;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void emit(UiTestLensLogEntry entry) {
        if (entry == null || sinks.isEmpty()) {
            return;
        }
        for (UiTestLensLogSink sink : sinks) {
            try {
                sink.accept(entry);
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
        return new UiTestLensLogger(copy);
    }

    public static final class Builder {
        private final List<UiTestLensLogSink> sinks = new ArrayList<>();

        private Builder() {
        }

        public Builder sink(UiTestLensLogSink sink) {
            if (sink != null) {
                sinks.add(sink);
            }
            return this;
        }

        public UiTestLensLogger build() {
            return sinks.isEmpty() ? UiTestLensLogger.noop() : new UiTestLensLogger(sinks);
        }
    }
}

