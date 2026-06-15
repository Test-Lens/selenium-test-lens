package io.github.testlens.core.trace;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TraceArtifactTest {

    @Test
    void screenshotStoresPathAndType() {
        TraceArtifact artifact = TraceArtifact.screenshot("Save form", Path.of("target/screenshots/save.png"));

        assertEquals("Save form", artifact.name());
        assertEquals(TraceArtifactType.SCREENSHOT, artifact.type());
        assertEquals(Path.of("target/screenshots/save.png").toString(), artifact.path());
        assertEquals("image/png", artifact.mediaType());
    }

    @Test
    void videoStoresPathAndType() {
        TraceArtifact artifact = TraceArtifact.video("Checkout", Path.of("target/videos/checkout.mp4"));

        assertEquals(TraceArtifactType.VIDEO, artifact.type());
        assertEquals("video/mp4", artifact.mediaType());
    }

    @Test
    void urlRequiresNonBlankUrl() {
        assertThrows(IllegalArgumentException.class, () -> TraceArtifact.url("CI", TraceArtifactType.CUSTOM_URL, " "));
    }

    @Test
    void metadataReturnsNewArtifact() {
        TraceArtifact artifact = TraceArtifact.customFile("Log", Path.of("target/log.txt"), "text/plain")
                .withMetadata("source", "ci");

        assertEquals("ci", artifact.metadata().get("source"));
    }
}
