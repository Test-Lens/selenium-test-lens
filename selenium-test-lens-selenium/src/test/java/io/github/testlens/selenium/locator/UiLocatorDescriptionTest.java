package io.github.testlens.selenium.locator;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiLocatorDescriptionTest {

    @Test
    void usesByWhenLabelIsBlank() {
        UiLocatorDescription description = UiLocatorDescription.of(By.cssSelector(".save"), "");

        assertEquals("By.cssSelector: .save", description.displayName());
    }

    @Test
    void includesLabelWhenPresent() {
        UiLocatorDescription description = UiLocatorDescription.of(By.id("save"), "Save button");

        assertTrue(description.displayName().startsWith("Save button"));
        assertTrue(description.displayName().contains("By.id: save"));
    }
}
