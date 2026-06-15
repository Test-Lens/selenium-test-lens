package io.github.testlens.core;

import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogSink;
import io.github.testlens.core.logging.UiTestLensLogger;

public final class OverlayLogger {
    private final UiTestLensLogger delegate;

    private OverlayLogger(UiTestLensLogger delegate) {
        this.delegate = delegate != null ? delegate : UiTestLensLogger.noop();
    }

    public static OverlayLogger noop() {
        return new OverlayLogger(UiTestLensLogger.noop());
    }

    public static OverlayLogger from(UiTestLensLogger logger) {
        return new OverlayLogger(logger);
    }

    public OverlayLogger withSink(UiTestLensLogSink sink) {
        return new OverlayLogger(delegate.withSink(sink));
    }

    public void debug(String message) {
        delegate.debug(message);
    }

    public void info(String message) {
        delegate.info(message);
    }

    public void warn(String message) {
        delegate.warn(message);
    }

    public void error(String message) {
        delegate.error(message);
    }

    public void error(String message, Throwable throwable) {
        delegate.error(message, throwable);
    }

    public void emit(UiTestLensLogEntry entry) {
        delegate.emit(entry);
    }
}
