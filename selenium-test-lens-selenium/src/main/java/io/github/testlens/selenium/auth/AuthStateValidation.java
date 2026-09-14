package io.github.testlens.selenium.auth;

/**
 * Application-level authentication validation result.
 * {@code INCONCLUSIVE} means that authentication cannot be determined reliably and never authorizes login.
 *
 * @since 0.3.0
 */
public enum AuthStateValidation {
    /** The application unambiguously confirms an authenticated session. */
    AUTHENTICATED,
    /** The application unambiguously confirms that the session is not authenticated. */
    UNAUTHENTICATED,
    /** The application or environment cannot currently provide a reliable answer. */
    INCONCLUSIVE
}
