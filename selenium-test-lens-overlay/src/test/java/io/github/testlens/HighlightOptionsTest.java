package io.github.testlens;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HighlightOptionsTest {
    @Test
    void defaultsAndCopyPreserveAllFiveStates() {
        HighlightOptions options = HighlightOptions.defaults();
        assertTrue(options.enabled());
        assertTrue(options.automaticFeedback());
        assertEquals("#ffeb3b", options.actionColor());
        assertEquals("#2196f3", options.waitingColor());
        assertEquals("#ff9800", options.retryColor());
        assertEquals("#4caf50", options.successColor());
        assertEquals("#f44336", options.failureColor());
        assertEquals(1500, options.durationMs());
        assertEquals(2, options.borderWidthPx());
        assertTrue(options.showLabels());

        HighlightOptions copy = options.toBuilder().automaticFeedback(false).build();
        assertFalse(copy.automaticFeedback());
        assertEquals(options.failureColor(), copy.color(HighlightState.FAILURE));
        assertEquals("failure", copy.toRuntimeMap(HighlightState.FAILURE).get("state"));
        assertEquals(options.actionColor(), copy.actionColor());
        assertEquals(options.durationMs(), copy.durationMs());
    }

    @Test
    void validatesDimensionsAndColors() {
        assertThrows(IllegalArgumentException.class, () -> HighlightOptions.builder().durationMs(-1));
        assertThrows(IllegalArgumentException.class, () -> HighlightOptions.builder().borderWidthPx(0));
        assertThrows(IllegalArgumentException.class, () -> HighlightOptions.builder().successColor(" "));
        assertDoesNotThrow(() -> HighlightOptions.builder().durationMs(0).build());
    }
}
