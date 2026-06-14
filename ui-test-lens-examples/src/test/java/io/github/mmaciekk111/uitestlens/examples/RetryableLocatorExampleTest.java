package io.github.mmaciekk111.uitestlens.examples;

import io.github.mmaciekk111.uitestlens.JsOverlayDebug;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Documentation-only example; requires a real WebDriver.")
class RetryableLocatorExampleTest {

    @Test
    void retryableLocatorUsage() {
        WebDriver driver = null; // replace with a real driver in a test project

        JsOverlayDebug overlay = new JsOverlayDebug(driver);

        overlay.getByTestId("email").fill("test@example.com");
        overlay.getByTestId("save-button").click();

        String toast = overlay.getByTestId("toast").textContent();
        assertTrue(toast.contains("Saved"));

        boolean modalVisible = overlay.getByTestId("modal").isVisible();
        assertTrue(modalVisible);
    }
}
