package io.github.testlens;

/**
 * Signals invalid access to managed in-memory test state or scenario resources.
 * Messages intentionally omit state keys, resource names, and stored values.
 *
 * @since 0.3.0
 */
public final class TestStateException extends IllegalStateException {
    /** Creates an exception with a value-safe diagnostic message. @since 0.3.0 */
    public TestStateException(String message) {
        super(message);
    }

    /** Creates an exception with a value-safe diagnostic message and original cause. @since 0.3.0 */
    public TestStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
