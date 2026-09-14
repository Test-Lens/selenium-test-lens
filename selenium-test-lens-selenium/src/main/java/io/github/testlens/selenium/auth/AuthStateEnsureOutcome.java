package io.github.testlens.selenium.auth;

/**
 * Successful managed auth-state lifecycle outcome.
 *
 * @since 0.3.0
 */
public enum AuthStateEnsureOutcome {
    /** Persisted state was restored and validated without login or writing. */
    RESTORED,
    /** Missing state was created after one login and successful validation. */
    CREATED,
    /** Existing state was atomically replaced after one login and successful validation. */
    REFRESHED
}
