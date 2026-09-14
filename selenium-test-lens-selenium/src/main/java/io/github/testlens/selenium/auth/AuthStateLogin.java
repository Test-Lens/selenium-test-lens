package io.github.testlens.selenium.auth;

import org.openqa.selenium.WebDriver;

/**
 * Performs the application's real login using the WebDriver already owned by Test Lens.
 * Implementations must not close the driver. Test Lens neither knows nor persists credentials.
 *
 * @since 0.3.0
 */
@FunctionalInterface
public interface AuthStateLogin {
    /**
     * Performs one application login attempt.
     *
     * @param driver the Test Lens-owned driver; implementations must not close it
     * @since 0.3.0
     */
    void login(WebDriver driver);
}
