package io.github.testlens.selenium.evidence;

import io.github.testlens.core.trace.TraceArtifact;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoEvidenceResultTest {

    @Test
    void attachedResultStoresArtifactAndMetadata() {
        TraceArtifact artifact = TraceArtifact.video("Run video", Path.of("target/video.mp4"));

        VideoEvidenceResult result = VideoEvidenceResult.attached(
                "Run video",
                Path.of("target/video.mp4"),
                "",
                artifact,
                VideoEvidenceSource.SELENIUM_GRID,
                "attached",
                Map.of("provider", "Docker Selenium")
        );

        assertEquals(VideoEvidenceStatus.ATTACHED, result.status());
        assertTrue(result.isAttached());
        assertSame(artifact, result.artifact());
        assertEquals(VideoEvidenceSource.SELENIUM_GRID, result.source());
        assertEquals("Docker Selenium", result.metadata().get("provider"));
        assertNotNull(result.attachedAt());
    }

    @Test
    void failedResultStoresException() {
        RuntimeException cause = new RuntimeException("missing");

        VideoEvidenceResult result = VideoEvidenceResult.failed(
                "Run video",
                Path.of("target/missing.mp4"),
                "",
                VideoEvidenceSource.LOCAL_FILE,
                "failed",
                cause,
                Map.of()
        );

        assertEquals(VideoEvidenceStatus.FAILED, result.status());
        assertFalse(result.isAttached());
        assertSame(cause, result.exception());
    }
}
