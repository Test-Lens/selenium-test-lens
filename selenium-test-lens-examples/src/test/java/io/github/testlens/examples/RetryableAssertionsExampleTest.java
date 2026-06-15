package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

@Disabled("Documentation-only example; requires a real WebDriver.")
class RetryableAssertionsExampleTest {

    @Test
    void retryableAssertionsUsage() {
        WebDriver driver = null; // replace with a real driver in a test project

        JsOverlayDebug overlay = new JsOverlayDebug(driver);

        overlay.getByTestId("save-button").click();

        overlay.expect(overlay.getByTestId("toast"))
                .toContainText("Saved");

        overlay.expect(By.cssSelector("[data-testid='save-button']"))
                .toBeEnabled();

        overlay.expect(overlay.getByTestId("modal"))
                .toBeVisible();
    }
}

