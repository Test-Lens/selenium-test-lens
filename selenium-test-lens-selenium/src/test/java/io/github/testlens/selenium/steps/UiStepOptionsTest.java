package io.github.testlens.selenium.steps;

import io.github.testlens.selenium.evidence.ScreenshotCaptureOptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UiStepOptionsTest {

    @Test
    void defaultsMatchStepDslPolicy() {
        UiStepOptions options = UiStepOptions.defaults();

        assertTrue(options.failFast());
        assertTrue(options.logToHud());
        assertTrue(options.captureNestedEvents());
        assertFalse(options.includeStackTrace());
        assertFalse(options.captureScreenshotOnFailure());
        assertEquals(Path.of("target/ui-test-lens/screenshots"), options.screenshotCaptureOptions().outputDirectory());
        assertEquals(500, options.messagePreviewLimit());
    }

    @Test
    void canEnableScreenshotOnFailure() {
        ScreenshotCaptureOptions screenshotOptions = ScreenshotCaptureOptions.builder()
                .outputDirectory(Path.of("target/custom-screens"))
                .build();

        UiStepOptions options = UiStepOptions.builder()
                .captureScreenshotOnFailure(true)
                .screenshotCaptureOptions(screenshotOptions)
                .build();

        assertTrue(options.captureScreenshotOnFailure());
        assertEquals(Path.of("target/custom-screens"), options.screenshotCaptureOptions().outputDirectory());
    }

    @Test
    void validatesPreviewLimit() {
        assertThrows(IllegalArgumentException.class, () -> UiStepOptions.builder()
                .messagePreviewLimit(-1)
                .build());
    }
}

