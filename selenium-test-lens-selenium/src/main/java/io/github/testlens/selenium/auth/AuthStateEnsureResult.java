package io.github.testlens.selenium.auth;

import java.time.Duration;

/**
 * Minimal, secret-safe result of a successful managed auth-state lifecycle operation.
 * It intentionally contains neither the logical key, path, callbacks nor captured browser state.
 *
 * @since 0.3.0
 */
public final class AuthStateEnsureResult {
    private final AuthStateEnsureOutcome outcome;
    private final Duration elapsed;

    AuthStateEnsureResult(AuthStateEnsureOutcome outcome, Duration elapsed) {
        this.outcome = outcome;
        this.elapsed = elapsed == null ? Duration.ZERO : elapsed;
    }

    /**
     * Returns how the authenticated state was obtained.
     *
     * @return the successful lifecycle outcome
     * @since 0.3.0
     */
    public AuthStateEnsureOutcome outcome() { return outcome; }

    /**
     * Returns time spent inside the managed lifecycle, including lock acquisition.
     *
     * @return non-null elapsed time
     * @since 0.3.0
     */
    public Duration elapsed() { return elapsed; }

    /**
     * Returns a representation containing only the outcome and elapsed time.
     *
     * @return a secret-safe representation
     * @since 0.3.0
     */
    @Override public String toString() {
        return "AuthStateEnsureResult{outcome=" + outcome + ", elapsed=" + elapsed + '}';
    }
}
