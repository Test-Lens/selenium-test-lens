package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

class PlaywrightStyleLocatorsExampleTest {

    @Disabled("Documentation-only example; requires a real WebDriver.")
    @Test
    void playwrightStyleLocatorsUsage() {
        WebDriver driver = null; // replace with a real driver

        JsOverlayDebug overlay = new JsOverlayDebug(driver);

        overlay.getByLabel("Email").fill("test@example.com");
        overlay.getByPlaceholder("Search").fill("invoice");
        overlay.getByRole("button", "Save").click();

        overlay.expect(overlay.getByTextContaining("Saved"))
                .toBeVisible();
    }
}

