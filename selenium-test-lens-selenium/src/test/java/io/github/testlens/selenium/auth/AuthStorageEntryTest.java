package io.github.testlens.selenium.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthStorageEntryTest {

    @Test
    void storesOriginKeyValueAndType() {
        AuthStorageEntry entry = new AuthStorageEntry("https://app.example.com", "theme", "dark", AuthStorageType.LOCAL_STORAGE);

        assertEquals("https://app.example.com", entry.origin());
        assertEquals("theme", entry.key());
        assertEquals("dark", entry.value());
        assertEquals(AuthStorageType.LOCAL_STORAGE, entry.type());
    }

    @Test
    void rejectsBlankKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuthStorageEntry("", " ", "", AuthStorageType.SESSION_STORAGE));
    }
}
