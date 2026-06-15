package io.github.testlens.selenium.auth;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthStateJsonExporterTest {

    @Test
    void exportsEscapedJson() {
        AuthState state = new AuthState(
                AuthStateMetadata.builder()
                        .label("customer \"A\"")
                        .origin("https://app.example.com")
                        .createdAt(Instant.parse("2026-06-14T10:00:00Z"))
                        .build(),
                List.of(new AuthCookie("session", "line\nvalue", "app.example.com", "/", null, true, true, "Lax")),
                List.of(new AuthStorageEntry("https://app.example.com", "theme", "dark", AuthStorageType.LOCAL_STORAGE)),
                List.of()
        );

        String json = state.exportJson();

        assertTrue(json.contains("customer \\\"A\\\""));
        assertTrue(json.contains("line\\nvalue"));
        assertTrue(json.contains("\"cookies\""));
    }
}

