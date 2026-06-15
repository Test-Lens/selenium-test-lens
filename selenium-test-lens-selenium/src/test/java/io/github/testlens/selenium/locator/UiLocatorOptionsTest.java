package io.github.testlens.selenium.locator;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiLocatorOptionsTest {

    @Test
    void defaultsMatchRetryableLocatorPolicy() {
        UiLocatorOptions options = UiLocatorOptions.defaults();

        assertEquals(Duration.ofSeconds(3), options.timeout());
        assertEquals(Duration.ofMillis(100), options.pollInterval());
        assertEquals(3, options.maxRetries());
        assertTrue(options.retryOnStaleElement());
        assertTrue(options.retryOnClickIntercepted());
        assertTrue(options.retryOnNotInteractable());
        assertTrue(options.highlightBeforeAction());
    }

    @Test
    void validatesRetryAndTiming() {
        assertThrows(IllegalArgumentException.class, () -> UiLocatorOptions.builder()
                .timeout(Duration.ZERO)
                .build());
        assertThrows(IllegalArgumentException.class, () -> UiLocatorOptions.builder()
                .pollInterval(Duration.ZERO)
                .build());
        assertThrows(IllegalArgumentException.class, () -> UiLocatorOptions.builder()
                .maxRetries(0)
                .build());
    }
}

