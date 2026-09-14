package io.github.testlens.selenium.auth;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthCookieTest {

    @Test
    void convertsFromAndToSeleniumCookie() {
        Instant expiry = Instant.parse("2026-06-14T10:00:00Z");
        Cookie cookie = new Cookie.Builder("session", "abc")
                .domain("app.example.com")
                .path("/")
                .expiresOn(Date.from(expiry))
                .isSecure(true)
                .isHttpOnly(true)
                .sameSite("Lax")
                .build();

        AuthCookie authCookie = AuthCookie.fromSeleniumCookie(cookie);
        Cookie restored = authCookie.toSeleniumCookie();

        assertEquals("session", authCookie.name());
        assertEquals("abc", authCookie.value());
        assertEquals("app.example.com", restored.getDomain());
        assertTrue(restored.isSecure());
        assertTrue(restored.isHttpOnly());
        assertEquals("Lax", restored.getSameSite());
    }

    @Test
    void omitsInsecureSameSiteNoneWhenCreatingSeleniumCookie() {
        Cookie restored = cookie(false, "None").toSeleniumCookie();

        assertFalse(restored.isSecure());
        assertNull(restored.getSameSite());
    }

    @Test
    void omitsInsecureSameSiteNoneCaseInsensitively() {
        assertNull(cookie(false, "none").toSeleniumCookie().getSameSite());
        assertNull(cookie(false, "NONE").toSeleniumCookie().getSameSite());
    }

    @Test
    void preservesSecureSameSiteNone() {
        Cookie restored = cookie(true, "None").toSeleniumCookie();

        assertTrue(restored.isSecure());
        assertEquals("None", restored.getSameSite());
    }

    @Test
    void preservesInsecureSameSiteLaxAndStrict() {
        assertEquals("Lax", cookie(false, "Lax").toSeleniumCookie().getSameSite());
        assertEquals("Strict", cookie(false, "Strict").toSeleniumCookie().getSameSite());
    }

    @Test
    void omitsBlankSameSite() {
        assertNull(cookie(false, "").toSeleniumCookie().getSameSite());
        assertNull(cookie(false, "   ").toSeleniumCookie().getSameSite());
    }

    private static AuthCookie cookie(boolean secure, String sameSite) {
        return new AuthCookie("session", "abc", "app.example.com", "/", null,
                secure, false, sameSite);
    }
}

