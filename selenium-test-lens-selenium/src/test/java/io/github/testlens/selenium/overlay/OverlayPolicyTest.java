package io.github.testlens.selenium.overlay;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayPolicyTest {

    @Test
    void noneIsEmpty() {
        assertTrue(OverlayPolicy.none().isEmpty());
        assertSame(OverlayPolicy.none(), OverlayPolicy.builder().build());
    }

    @Test
    void handlersAreImmutable() {
        OverlayHandler handler = OverlayHandler.builder("Newsletter")
                .detect(By.cssSelector(".newsletter"))
                .action(OverlayAction.pressEscape())
                .build();

        OverlayPolicy policy = OverlayPolicy.builder().handler(handler).build();

        assertEquals(1, policy.handlers().size());
        assertThrows(UnsupportedOperationException.class, () -> policy.handlers().add(handler));
    }
}
