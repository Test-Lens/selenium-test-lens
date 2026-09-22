package io.github.testlens;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

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
        for (HighlightState state : HighlightState.values()) {
            assertEquals(1500, copy.effectiveDurationMs(state));
            assertEquals(OptionalLong.empty(), copy.durationOverrideMs(state));
        }
    }

    @Test
    void validatesDimensionsAndColors() {
        assertThrows(IllegalArgumentException.class, () -> HighlightOptions.builder().durationMs(-1));
        assertThrows(IllegalArgumentException.class, () -> HighlightOptions.builder().borderWidthPx(0));
        assertThrows(IllegalArgumentException.class, () -> HighlightOptions.builder().successColor(" "));
        assertDoesNotThrow(() -> HighlightOptions.builder().durationMs(0).build());
        assertThrows(IllegalArgumentException.class, () -> HighlightOptions.builder().actionDurationMs(-1));
        assertThrows(IllegalArgumentException.class, () -> HighlightOptions.builder().clearDurationOverride(null));
        assertThrows(IllegalArgumentException.class, () -> HighlightOptions.defaults().effectiveDurationMs(null));
        assertThrows(IllegalArgumentException.class, () -> HighlightOptions.defaults().durationOverrideMs(null));
    }

    @Test
    void stateDurationsInheritOverrideClearAndCopyWithoutSetterOrderEffects() {
        HighlightOptions first = HighlightOptions.builder()
                .actionDurationMs(500)
                .waitingDurationMs(600)
                .retryDurationMs(800)
                .successDurationMs(2500)
                .durationMs(2000)
                .failureDurationMs(0)
                .build();
        HighlightOptions second = HighlightOptions.builder()
                .durationMs(2000)
                .retryDurationMs(800)
                .waitingDurationMs(600)
                .actionDurationMs(500)
                .successDurationMs(2500)
                .failureDurationMs(0)
                .build();

        for (HighlightState state : HighlightState.values()) {
            long expected = switch (state) {
                case ACTION -> 500;
                case WAITING -> 600;
                case RETRY -> 800;
                case SUCCESS -> 2500;
                case FAILURE -> 0;
            };
            assertEquals(expected, first.effectiveDurationMs(state));
            assertEquals(expected, second.effectiveDurationMs(state));
        }
        assertEquals(OptionalLong.of(2500), first.durationOverrideMs(HighlightState.SUCCESS));
        assertEquals(OptionalLong.of(0), first.durationOverrideMs(HighlightState.FAILURE));
        assertEquals(OptionalLong.of(500), first.durationOverrideMs(HighlightState.ACTION));
        assertEquals(Boolean.TRUE, first.toRuntimeMap(HighlightState.FAILURE).get("suppress"));

        HighlightOptions copy = first.toBuilder().durationMs(3000).build();
        assertEquals(2500, copy.effectiveDurationMs(HighlightState.SUCCESS));
        assertEquals(500, copy.effectiveDurationMs(HighlightState.ACTION));
        assertEquals(OptionalLong.of(500), copy.durationOverrideMs(HighlightState.ACTION));

        HighlightOptions cleared = copy.toBuilder().clearDurationOverride(HighlightState.SUCCESS).build();
        assertEquals(3000, cleared.effectiveDurationMs(HighlightState.SUCCESS));
        assertEquals(OptionalLong.empty(), cleared.durationOverrideMs(HighlightState.SUCCESS));
        assertEquals(500, cleared.effectiveDurationMs(HighlightState.ACTION),
                "clearing SUCCESS must leave other explicit overrides untouched");
    }

    @Test
    void commonZeroKeepsLegacyPresentationWhileExplicitStateZeroSuppresses() {
        HighlightOptions inheritedZero = HighlightOptions.builder().durationMs(0).build();
        HighlightOptions explicitZero = HighlightOptions.builder().durationMs(900).successDurationMs(0).build();

        assertEquals(0, inheritedZero.effectiveDurationMs(HighlightState.SUCCESS));
        assertEquals(Boolean.FALSE, inheritedZero.toRuntimeMap(HighlightState.SUCCESS).get("suppress"));
        assertEquals(0, explicitZero.effectiveDurationMs(HighlightState.SUCCESS));
        assertEquals(Boolean.TRUE, explicitZero.toRuntimeMap(HighlightState.SUCCESS).get("suppress"));
    }
}
