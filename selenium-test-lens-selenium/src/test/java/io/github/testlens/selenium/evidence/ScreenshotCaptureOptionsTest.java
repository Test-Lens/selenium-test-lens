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
    }

    @Test
    void builderOverridesValues() {
        ScreenshotCaptureOptions options = ScreenshotCaptureOptions.builder()
                .outputDirectory(Path.of("target/custom"))
                .fileNamePrefix("failure")
                .includeTimestamp(false)
                .overwriteExisting(true)
                .attachToSession(false)
                .build();

        assertEquals(Path.of("target/custom"), options.outputDirectory());
        assertEquals("failure", options.fileNamePrefix());
        assertFalse(options.includeTimestamp());
        assertTrue(options.overwriteExisting());
        assertFalse(options.attachToSession());
    }
}
