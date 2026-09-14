package io.github.testlens.selenium.auth;

/**
 * Managed auth-state failure with a stable reason and a deliberately secret-safe message.
 * The original callback or WebDriver failure is retained as the cause for programmatic diagnosis;
 * Test Lens applies its central redaction policy before diagnostic fan-out.
 *
 * @since 0.3.0
 */
public final class ManagedAuthStateException extends RuntimeException {
    private final ManagedAuthStateFailureReason reason;

    ManagedAuthStateException(ManagedAuthStateFailureReason reason, String message) {
        this(reason, message, null);
    }

    ManagedAuthStateException(ManagedAuthStateFailureReason reason, String message, Throwable cause) {
        super(message == null ? "Managed auth state failed" : message, cause);
        this.reason = reason == null ? ManagedAuthStateFailureReason.RESTORE_FAILED : reason;
    }

    /**
     * Returns the stable machine-readable failure reason.
     *
     * @return the failure reason
     * @since 0.3.0
     */
    public ManagedAuthStateFailureReason reason() { return reason; }
}
