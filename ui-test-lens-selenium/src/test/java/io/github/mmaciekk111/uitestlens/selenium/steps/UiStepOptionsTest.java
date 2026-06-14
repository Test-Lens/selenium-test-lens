package io.github.mmaciekk111.uitestlens.selenium.steps;

import org.junit.jupiter.api.Test;

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
        assertEquals(500, options.messagePreviewLimit());
    }

    @Test
    void validatesPreviewLimit() {
        assertThrows(IllegalArgumentException.class, () -> UiStepOptions.builder()
                .messagePreviewLimit(-1)
                .build());
    }
}
