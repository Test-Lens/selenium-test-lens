package io.github.testlens.core.browser;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Selenium compatibility helper for creating neutral browser script executors.
 */
final class OverlayBrowserScriptExecutors {
    private OverlayBrowserScriptExecutors() {}

    public static BrowserScriptExecutor from(WebDriver driver) {
        return new SeleniumBrowserScriptExecutor(driver);
    }

    public static BrowserScriptExecutor from(JavascriptExecutor executor) {
        return new SeleniumBrowserScriptExecutor(executor);
    }
}

