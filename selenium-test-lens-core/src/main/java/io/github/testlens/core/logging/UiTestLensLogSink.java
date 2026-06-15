package io.github.testlens.core.logging;

@FunctionalInterface
public interface UiTestLensLogSink {
    void accept(UiTestLensLogEntry entry);
}

