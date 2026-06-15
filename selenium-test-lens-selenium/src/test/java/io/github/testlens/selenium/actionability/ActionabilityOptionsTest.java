package io.github.testlens.selenium.actionability;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionabilityOptionsTest {

    @Test
    void defaultsMatchInitialActionabilityPolicy() {
        ActionabilityOptions options = ActionabilityOptions.defaults();

        assertEquals(Duration.ofSeconds(3), options.timeout());
        assertEquals(Duration.ofMillis(100), options.pollInterval());
        assertTrue(options.checkAttached());
        assertTrue(options.checkVisible());
        assertTrue(options.checkEnabled());
        assertTrue(options.checkStableBounds());
        assertTrue(options.scrollIntoView());
        assertTrue(options.checkReceivesClickPoint());
        assertTrue(options.checkOverlayPolicy());
        assertEquals(2, options.stableBoundsSamples());
        assertEquals(Duration.ofMillis(100), options.stableBoundsSampleDelay());
    }

    @Test
    void validatesTimingAndSamples() {
        assertThrows(IllegalArgumentException.class, () -> ActionabilityOptions.builder()
                .timeout(Duration.ZERO)
                .build());
        assertThrows(IllegalArgumentException.class, () -> ActionabilityOptions.builder()
                .pollInterval(Duration.ZERO)
                .build());
        assertThrows(IllegalArgumentException.class, () -> ActionabilityOptions.builder()
                .stableBoundsSamples(1)
                .build());
        assertThrows(IllegalArgumentException.class, () -> ActionabilityOptions.builder()
                .stableBoundsSampleDelay(Duration.ofMillis(-1))
                .build());
    }
}
