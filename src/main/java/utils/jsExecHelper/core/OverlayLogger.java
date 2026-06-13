package utils.jsExecHelper.core;

public interface OverlayLogger {
    void debug(String message);
    void info(String message);
    void warn(String message);
    void error(String message);
    void error(String message, Throwable throwable);

    static OverlayLogger noop() {
        return NoopOverlayLogger.INSTANCE;
    }
}
