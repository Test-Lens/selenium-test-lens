package io.github.testlens.selenium.evidence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidencePathStrategyTest {
    @TempDir
    Path tempDir;

    @Test
    void sanitizesFileNameParts() {
        assertEquals("after-save-modal", EvidencePathStrategy.sanitize("After save: modal?"));
    }

    @Test
    void usesSafeNameWithoutTimestamp() {
        ScreenshotCaptureOptions options = ScreenshotCaptureOptions.builder()
                .outputDirectory(tempDir)
                .fileNamePrefix("failure:shot")
                .includeTimestamp(false)
                .build();

        Path path = EvidencePathStrategy.screenshotPath("After save?", options);

        assertEquals(tempDir.resolve("failure-shot_after-save.png"), path);
    }

    @Test
    void avoidsCollisionWhenOverwriteDisabled() throws Exception {
        ScreenshotCaptureOptions options = ScreenshotCaptureOptions.builder()
                .outputDirectory(tempDir)
                .fileNamePrefix("shot")
                .includeTimestamp(false)
                .overwriteExisting(false)
                .build();
        Path first = tempDir.resolve("shot_after-save.png");
        Files.writeString(first, "existing");

        Path next = EvidencePathStrategy.screenshotPath("After save", options);

        assertNotEquals(first, next);
        assertTrue(next.getFileName().toString().startsWith("shot_after-save_"));
    }
}

