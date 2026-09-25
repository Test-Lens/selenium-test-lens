package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.ObservabilityMode;
import io.github.testlens.TestLens;
import io.github.testlens.TestLensOptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

@Disabled("Documentation-only example; requires a real WebDriver.")
class SeleniumUsageExampleTest {

    @Test
    void fastObservabilityKeepsTestSemantics() {
        WebDriver driver = null; // Replace with the already-created project driver.
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
                .observabilityMode(ObservabilityMode.FAST)
                .build());
        lens.startSession("CI checkout");
        lens.observeDriver().findElement(By.id("save")).click();
        lens.finishPassed();
    }

    @Test
    void seleniumOverlayUsage() {
        WebDriver driver = null; // Replace with a real driver in a test project.

        OverlayConfig config = OverlayConfig.builder().build();
        JsOverlayDebug overlay = new JsOverlayDebug(driver, config);

        overlay.initHud("Checkout test", "local");
        overlay.setStep("Open checkout");
        overlay.hudLog("info", "Opening checkout page", "local");

        WebElement saveButton = driver.findElement(By.cssSelector("[data-testid='save']"));
        // Purely visual: does not click the application.
        overlay.highlightElement(saveButton, "SAVE");
        // Decoration plus one explicit Selenium click (also clicks when decoration is disabled).
        overlay.highlightThenClick(saveButton, "SAVE");
        overlay.clearDebugArtifacts();
    }
}

