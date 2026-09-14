package io.github.testlens.selenium.auth;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthStateJsonParserTest {

    @Test
    void parsesExporterRoundTrip() {
        AuthState original = new AuthState(
                AuthStateMetadata.builder()
                        .label("standard-customer")
                        .role("customer")
                        .origin("https://app.example.com")
                        .domain("app.example.com")
                        .createdAt(Instant.parse("2026-06-14T10:00:00Z"))
                        .expiresAt(Instant.parse("2026-06-14T18:00:00Z"))
                        .labelEntry("suite", "checkout")
                        .build(),
                List.of(new AuthCookie("session", "abc", "app.example.com", "/", null, true, true, "Lax")),
                List.of(new AuthStorageEntry("https://app.example.com", "theme", "dark", AuthStorageType.LOCAL_STORAGE)),
                List.of(new AuthStorageEntry("https://app.example.com", "tab", "checkout", AuthStorageType.SESSION_STORAGE))
        );

        AuthState parsed = new AuthStateJsonParser().parse(original.exportJson());

        assertEquals("standard-customer", parsed.metadata().label());
        assertEquals("customer", parsed.metadata().role());
        assertEquals("checkout", parsed.metadata().labels().get("suite"));
        assertEquals("session", parsed.cookies().get(0).name());
        assertEquals("theme", parsed.localStorage().get(0).key());
        assertEquals("tab", parsed.sessionStorage().get(0).key());
    }

    @Test
    void parsesLegacyInsecureSameSiteNoneCookieForReplayWithoutMutatingTheState() {
        String legacyJson = """
                {
                  "metadata": {
                    "origin": "https://app.example.com"
                  },
                  "cookies": [
                    {
                      "name": "session",
                      "value": "legacy-value",
                      "domain": "app.example.com",
                      "path": "/",
                      "expiry": "",
                      "secure": false,
                      "httpOnly": false,
                      "sameSite": "None"
                    }
                  ],
                  "localStorage": [],
                  "sessionStorage": []
                }
                """;

        AuthState parsed = new AuthStateJsonParser().parse(legacyJson);
        AuthCookie cookie = parsed.cookies().get(0);

        assertEquals("None", cookie.sameSite());
        assertFalse(cookie.secure());
        assertNull(cookie.toSeleniumCookie().getSameSite());
        assertEquals("None", cookie.sameSite());
    }
}

