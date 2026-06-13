package utils.jsExecHelper.core;

final class NoopOverlayLogger implements OverlayLogger {
    static final NoopOverlayLogger INSTANCE = new NoopOverlayLogger();

    private NoopOverlayLogger() {
    }

    @Override
    public void debug(String message) {
    }

    @Override
    public void info(String message) {
    }

    @Override
    public void warn(String message) {
    }

    @Override
    public void error(String message) {
    }

    @Override
    public void error(String message, Throwable throwable) {
    }
}
