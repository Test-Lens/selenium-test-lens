package io.github.mmaciekk111.uitestlens.core.logging;

@FunctionalInterface
public interface UiTestLensLogSink {
    void accept(UiTestLensLogEntry entry);
}
