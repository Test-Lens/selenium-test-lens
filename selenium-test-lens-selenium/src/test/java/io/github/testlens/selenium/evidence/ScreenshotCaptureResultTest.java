package io.github.testlens.selenium.evidence;

import io.github.testlens.core.trace.TraceArtifact;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotCaptureResultTest {

    @Test
    void capturedResultStoresArtifactAndPath() {
        TraceArtifact artifact = TraceArtifact.screenshot("After save", Path.of("target/save.png"));

        ScreenshotCaptureResult result = ScreenshotCaptureResult.captured("After save", Path.of("target/save.png"), artifact, "ok");

        assertEquals(ScreenshotCaptureStatus.CAPTURED, result.status());
        assertTrue(result.isCaptured());
        assertSame(artifact, result.artifact());
        assertNotNull(result.capturedAt());
    }

    @Test
    void failedResultStoresException() {
        RuntimeException cause = new RuntimeException("boom");

        ScreenshotCaptureResult result = ScreenshotCaptureResult.failed("After save", null, "failed", cause);

        assertEquals(ScreenshotCaptureStatus.FAILED, result.status());
        assertFalse(result.isCaptured());
        assertSame(cause, result.exception());
    }
}

