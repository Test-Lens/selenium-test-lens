package io.github.mmaciekk111.uitestlens.selenium.overlay;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayHandlingResultTest {

    @Test
    void exposesStatusAndAttemptedActions() {
        OverlayHandlingResult result = OverlayHandlingResult.handled(
                "Cookie consent",
                List.of("click(By.cssSelector: button)"),
                Duration.ofMillis(25)
        );

        assertEquals("Cookie consent", result.handlerName());
        assertEquals(OverlayHandlingStatus.HANDLED, result.status());
        assertEquals(1, result.attemptedActions().size());
        assertTrue(result.detected());
    }

    @Test
    void notDetectedIsNotDetected() {
        OverlayHandlingResult result = OverlayHandlingResult.notDetected("Cookie consent", Duration.ZERO);

        assertEquals(OverlayHandlingStatus.NOT_DETECTED, result.status());
        assertFalse(result.detected());
    }
}
