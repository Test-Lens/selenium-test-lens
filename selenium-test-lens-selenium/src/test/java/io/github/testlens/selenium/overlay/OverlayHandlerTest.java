package io.github.testlens.selenium.overlay;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayHandlerTest {

    @Test
    void buildsHandlerWithDefaults() {
        OverlayHandler handler = OverlayHandler.builder("Cookie consent")
                .detect(By.cssSelector("[data-testid='cookie-banner']"))
                .action(OverlayAction.pressEscape())
                .build();

        assertEquals("Cookie consent", handler.name());
        assertTrue(handler.optional());
        assertEquals(Duration.ofSeconds(2), handler.timeout());
        assertFalse(handler.failIfStillVisible());
        assertEquals(1, handler.actions().size());
    }

    @Test
    void validatesRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> OverlayHandler.builder(" ").build());
        assertThrows(NullPointerException.class, () -> OverlayHandler.builder("Missing detect")
                .action(OverlayAction.pressEscape())
                .build());
        assertThrows(IllegalArgumentException.class, () -> OverlayHandler.builder("No actions")
                .detect(By.cssSelector(".modal"))
                .build());
        assertThrows(IllegalArgumentException.class, () -> OverlayHandler.builder("Bad timeout")
                .detect(By.cssSelector(".modal"))
                .action(OverlayAction.pressEscape())
                .timeout(Duration.ZERO)
                .build());
    }
}
