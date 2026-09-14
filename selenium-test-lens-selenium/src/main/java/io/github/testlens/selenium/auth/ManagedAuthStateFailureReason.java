package io.github.testlens.selenium.auth;

/**
 * Structured reason for a managed auth-state lifecycle failure.
 *
 * @since 0.3.0
 */
public enum ManagedAuthStateFailureReason {
    /** The application login callback threw. */
    LOGIN_FAILED,
    /** The validator explicitly could not determine authentication. */
    VALIDATION_INCONCLUSIVE,
    /** The validator threw or returned no result. */
    VALIDATION_FAILED,
    /** Validation unambiguously rejected the session after login. */
    LOGIN_DID_NOT_AUTHENTICATE,
    /** Browser-state capture failed after authenticated login. */
    CAPTURE_FAILED,
    /** Serialization, flushing, validation, or atomic replacement failed. */
    PERSIST_FAILED,
    /** Existing state could not be restored and was not safely recreatable. */
    RESTORE_FAILED,
    /** A configured lock deadline elapsed. Reserved for lock implementations with a timeout. */
    LOCK_TIMEOUT,
    /** Canonicalization or JVM/filesystem lock acquisition failed. */
    LOCK_FAILED,
    /** Refresh or invalidate referenced a key not registered by ensure. */
    UNKNOWN_KEY,
    /** One manager received the same key with a different canonical path. */
    REGISTRATION_CONFLICT,
    /** Cookies or managed web storage could not be cleared before login. */
    BROWSER_STATE_CLEAR_FAILED,
    /** Persisted state could not be invalidated. */
    INVALIDATE_FAILED
}
