package io.github.testlens.selenium.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthStateTest {
    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadRoundTrip() throws Exception {
        AuthState state = new AuthState(
                AuthStateMetadata.builder().label("customer").origin("https://app.example.com").build(),
                List.of(new AuthCookie("session", "abc", "app.example.com", "/", null, true, true, "Lax")),
                List.of(),
                List.of()
        );
        Path path = tempDir.resolve("auth/customer.json");

        Path saved = state.save(path);
        AuthState loaded = AuthState.load(saved);

        assertTrue(Files.exists(path));
        assertEquals("customer", loaded.metadata().label());
        assertEquals("session", loaded.cookies().get(0).name());
    }
}

