package io.github.testlens.selenium.evidence;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotCaptureOptionsTest {

    @Test
    void defaultsUseTargetEvidenceDirectory() {
        ScreenshotCaptureOptions options = ScreenshotCaptureOptions.defaults();

        assertEquals(Path.of("target/ui-test-lens/screenshots"), options.outputDirectory());
        assertEquals("screenshot", options.fileNamePrefix());
        assertTrue(options.includeTimestamp());
        assertFalse(options.overwriteExisting());
        assertTrue(options.attachToSession());
        assertEquals(ScreenshotCaptureMode.VIEWPORT, options.captureMode());
        assertEquals(ScreenshotCaptureOptions.DEFAULT_MAX_PIXEL_COUNT, options.maxPixelCount());
        assertEquals(ScreenshotCaptureOptions.DEFAULT_MAX_TILE_COUNT, options.maxTileCount());
    }

    @Test
    void builderOverridesValues() {
        ScreenshotCaptureOptions options = ScreenshotCaptureOptions.builder()
                .outputDirectory(Path.of("target/custom"))
                .fileNamePrefix("failure")
                .includeTimestamp(false)
                .overwriteExisting(true)
                .attachToSession(false)
                .captureMode(ScreenshotCaptureMode.FULL_PAGE)
                .maxPixelCount(1234)
                .maxTileCount(7)
                .build();

        assertEquals(Path.of("target/custom"), options.outputDirectory());
        assertEquals("failure", options.fileNamePrefix());
        assertFalse(options.includeTimestamp());
        assertTrue(options.overwriteExisting());
        assertFalse(options.attachToSession());
        assertEquals(ScreenshotCaptureMode.FULL_PAGE, options.captureMode());
        assertEquals(1234, options.maxPixelCount());
        assertEquals(7, options.maxTileCount());
    }

    @Test
    void invalidFullPageOptionsAreRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ScreenshotCaptureOptions.builder().captureMode(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ScreenshotCaptureOptions.builder().maxPixelCount(0));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ScreenshotCaptureOptions.builder().maxTileCount(0));
    }
}

