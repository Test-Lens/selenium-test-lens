package io.github.mmaciekk111.uitestlens.selenium.locator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiLocatorExceptionTest {

    @Test
    void exposesActionLocatorAndActionabilitySummary() {
        UiLocatorException exception = new UiLocatorException(
                "click",
                "Save button",
                "failed",
                new RuntimeException("cause"),
                "Actionability NOT_READY"
        );

        assertEquals("click", exception.action());
        assertEquals("Save button", exception.locatorDescription());
        assertTrue(exception.actionabilitySummary().contains("NOT_READY"));
    }
}
