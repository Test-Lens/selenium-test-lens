package io.github.testlens.selenium.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRestoreOptionsTest {

    @Test
    void defaultsRestoreCookiesAndStorageAfterNavigation() {
        AuthRestoreOptions options = AuthRestoreOptions.defaults();

        assertTrue(options.navigateToOrigin());
        assertTrue(options.clearExistingCookies());
        assertTrue(options.clearExistingStorage());
        assertTrue(options.restoreCookies());
        assertTrue(options.restoreLocalStorage());
        assertTrue(options.restoreSessionStorage());
        assertTrue(options.validateOrigin());
        assertTrue(options.failIfExpired());
    }

    @Test
    void builderOverridesValues() {
        AuthRestoreOptions options = AuthRestoreOptions.builder()
                .navigateToOrigin(false)
                .clearExistingCookies(false)
                .restoreSessionStorage(false)
                .validateOrigin(false)
                .failIfExpired(false)
                .build();

        assertFalse(options.navigateToOrigin());
        assertFalse(options.clearExistingCookies());
        assertFalse(options.restoreSessionStorage());
        assertFalse(options.validateOrigin());
        assertFalse(options.failIfExpired());
    }
}
