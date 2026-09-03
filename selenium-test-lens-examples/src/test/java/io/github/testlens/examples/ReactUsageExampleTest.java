package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.react.ReactSafeExecutor;
import io.github.testlens.react.ReactSupport;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

@Disabled("Documentation-only example; requires a real WebDriver.")
class ReactUsageExampleTest {

    @Test
    void reactSafeUsage() {
        WebDriver driver = null; // Replace with a real driver in a test project.

        OverlayConfig config = OverlayConfig.builder().build();
        JsOverlayDebug overlay = new JsOverlayDebug(driver, config);

        ReactSafeExecutor react = ReactSupport.reactSafe(overlay);
        react.click(By.cssSelector("[data-testid='save']"), "Save");
        ReactSupport.smartClick(overlay, By.cssSelector("[data-testid='save']"), "Save with overlay handling");
    }
}

