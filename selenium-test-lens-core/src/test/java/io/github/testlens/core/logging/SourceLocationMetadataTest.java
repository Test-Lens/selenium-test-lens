package io.github.testlens.core.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SourceLocationMetadataTest {
    @Test void retainsOnlyLogicalSourceMetadata() {
        UiTestLensLogEntry entry = UiTestLensLogEntry.builder()
                .sourceLocation(new SourceLocation("example.LoginPage", "login", "LoginPage.java", 53)).build();

        assertEquals(new SourceLocation("example.LoginPage", "login", "LoginPage.java", 53),
                entry.sourceLocation().orElseThrow());
        assertTrue(entry.metadata().values().stream().noneMatch(value -> value.contains(":\\") || value.startsWith("/")));
        assertFalse(entry.metadata().containsKey("source.path"));
        assertFalse(entry.metadata().containsKey("source.uri"));
    }
}
