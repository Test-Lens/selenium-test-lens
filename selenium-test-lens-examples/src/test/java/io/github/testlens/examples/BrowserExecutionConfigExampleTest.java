package io.github.testlens.examples;

import io.github.testlens.TestLens;
import io.github.testlens.selenium.execution.BrowserExecutionConfig;
import io.github.testlens.selenium.execution.HeadlessMode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

@Disabled("Documentation-only example; requires a real Chrome installation.")
class BrowserExecutionConfigExampleTest {

    @Test
    void manualFactoryResolvesExecutionIntentBeforeCreatingTheDriver() {
        BrowserExecutionConfig execution = BrowserExecutionConfig.resolve();
        WebDriver driver = createDriver(execution);
        TestLens lens = TestLens.attach(driver);
        try {
            lens.startSession("manual execution configuration");
            driver.get("https://example.test/");
            lens.finishPassed();
        } catch (RuntimeException | Error failure) {
            lens.finishFailed(failure);
            throw failure;
        } finally {
            driver.quit();
        }
    }

    private WebDriver createDriver(BrowserExecutionConfig execution) {
        ChromeOptions options = existingProjectOptions();
        if (execution.headless() == HeadlessMode.TRUE) {
            options.addArguments("--headless=new");
        }
        // FALSE is explicitly headed; this controlled factory has no headless default to remove.
        return new ChromeDriver(options);
    }

    private ChromeOptions existingProjectOptions() {
        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.addArguments("--lang=en-US");
        return options;
    }
}
