package io.github.testlens.selenium.auth;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthStateOptionsTest {

    @Test
    void defaultsIncludeCookiesAndStorage() {
        AuthStateOptions options = AuthStateOptions.defaults();

        assertTrue(options.includeCookies());
        assertTrue(options.includeLocalStorage());
        assertTrue(options.includeSessionStorage());
        assertEquals("", options.label());
    }

    @Test
    void builderOverridesValues() {
        Instant expiresAt = Instant.parse("2026-06-14T10:00:00Z");

        AuthStateOptions options = AuthStateOptions.builder()
                .label("standard-customer")
                .role("customer")
                .origin("https://app.example.com")
                .includeCookies(false)
                .includeLocalStorage(false)
                .expiresAt(expiresAt)
                .labelEntry("suite", "checkout")
                .note("warning", "do not commit")
                .build();

        assertEquals("standard-customer", options.label());
        assertEquals("customer", options.role());
        assertEquals("https://app.example.com", options.origin());
        assertFalse(options.includeCookies());
        assertFalse(options.includeLocalStorage());
        assertTrue(options.includeSessionStorage());
        assertEquals(expiresAt, options.expiresAt());
        assertEquals("checkout", options.labels().get("suite"));
    }
}
