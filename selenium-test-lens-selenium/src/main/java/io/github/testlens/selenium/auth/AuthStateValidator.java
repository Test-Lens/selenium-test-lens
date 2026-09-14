package io.github.testlens.selenium.auth;

import org.openqa.selenium.WebDriver;

/**
 * Determines whether the application unambiguously considers the current browser session authenticated.
 * A returned {@link AuthStateValidation#INCONCLUSIVE} never triggers login. An exception is treated as a
 * validation execution failure and is propagated as the cause of {@link ManagedAuthStateException}.
 *
 * @since 0.3.0
 */
@FunctionalInterface
public interface AuthStateValidator {
    /**
     * Validates the current browser session.
     *
     * @param driver the Test Lens-owned driver
     * @return the authoritative tri-state authentication result; never {@code null}
     * @since 0.3.0
     */
    AuthStateValidation validate(WebDriver driver);
}
