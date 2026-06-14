package io.github.mmaciekk111.uitestlens.core.browser;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

public final class OverlayBrowserScriptExecutors {
    private OverlayBrowserScriptExecutors() {}

    public static BrowserScriptExecutor from(WebDriver driver) {
        if (!(driver instanceof JavascriptExecutor js)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor.");
        }
        return new BrowserScriptExecutor() {
            @Override
            public Object execute(String script, Object... args) {
                return js.executeScript(script, args);
            }

            @Override
            public Object executeAsync(String script, Object... args) {
                return js.executeAsyncScript(script, args);
            }
        };
    }
}
