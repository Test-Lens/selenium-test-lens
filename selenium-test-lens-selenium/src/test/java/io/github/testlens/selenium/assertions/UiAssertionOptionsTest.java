package io.github.testlens.selenium.assertions;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiAssertionOptionsTest {

    @Test
    void defaultsMatchRetryableAssertionPolicy() {
        UiAssertionOptions options = UiAssertionOptions.defaults();

        assertEquals(Duration.ofSeconds(3), options.timeout());
        assertEquals(Duration.ofMillis(100), options.pollInterval());
        assertTrue(options.normalizeWhitespace());
        assertTrue(options.caseSensitive());
        assertEquals(300, options.actualTextPreviewLimit());
        assertTrue(options.trimText());
        assertFalse(options.failFastOnMissingElement());
    }

    @Test
    void validatesTimingAndPreviewLimit() {
        assertThrows(IllegalArgumentException.class, () -> UiAssertionOptions.builder()
                .timeout(Duration.ZERO)
                .build());
        assertThrows(IllegalArgumentException.class, () -> UiAssertionOptions.builder()
                .pollInterval(Duration.ZERO)
                .build());
        assertThrows(IllegalArgumentException.class, () -> UiAssertionOptions.builder()
                .actualTextPreviewLimit(-1)
                .build());
    }

    @Test
    void normalizesTextAccordingToOptions() {
        UiAssertionOptions options = UiAssertionOptions.builder()
                .caseSensitive(false)
                .build();

        assertEquals("saved order", UiAssertionText.normalize("  Saved\n\tOrder  ", options));
    }

    @Test
    void valuePreviewDoesNotExposeValue() {
        assertEquals("length=12", UiAssertionText.valuePreview("masked-input"));
    }
}
