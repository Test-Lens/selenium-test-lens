package io.github.testlens.selenium.auth;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}

