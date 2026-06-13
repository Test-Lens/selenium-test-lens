package utils.jsExecHelper.core.logging;

@FunctionalInterface
public interface UiTestLensLogSink {
    void accept(UiTestLensLogEntry entry);
}
