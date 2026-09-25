package io.github.testlens.examples;

import io.github.testlens.TestLens;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

@Disabled("Documentation-only example; requires a real WebDriver.")
class NativeSeleniumObservationExampleTest {

    @Test
    void existingPageObjectsCanKeepUsingNativeSelenium() {
        WebDriver rawDriver = null; // Replace with the driver owned by the test project.
        TestLens lens = TestLens.attach(rawDriver);
        WebDriver driver = lens.observeDriver();

        try {
            lens.startSession("native selenium");
            driver.findElement(By.id("email")).sendKeys("person@example.test");
            driver.findElement(By.id("save")).click();

            WebElement button = lens.observe(rawDriver.findElement(By.id("save")), "Save");
            button.click();
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
            lens.finishPassed();
        } catch (RuntimeException | Error failure) {
            lens.finishFailed(failure);
            throw failure;
        } finally {
            rawDriver.quit();
        }
    }
}
