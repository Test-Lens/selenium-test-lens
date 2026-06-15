package io.github.testlens.selenium.overlay;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayActionTest {

    @Test
    void createsClickAction() {
        OverlayAction action = OverlayAction.click(By.cssSelector("[data-testid='accept']"));

        assertEquals(OverlayActionType.CLICK, action.type());
        assertTrue(action.describe().contains("click"));
    }

    @Test
    void requiresFailReason() {
        assertThrows(IllegalArgumentException.class, () -> OverlayAction.fail(" "));
    }
}

