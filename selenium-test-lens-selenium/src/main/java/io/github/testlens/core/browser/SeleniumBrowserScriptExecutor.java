package io.github.testlens.core.browser;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

public final class SeleniumBrowserScriptExecutor implements BrowserScriptExecutor {
    private final JavascriptExecutor executor;

    public SeleniumBrowserScriptExecutor(WebDriver driver) {
        if (!(driver instanceof JavascriptExecutor js)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor.");
        }
        this.executor = js;
    }

    public SeleniumBrowserScriptExecutor(JavascriptExecutor executor) {
        if (executor == null) {
            throw new IllegalArgumentException("JavascriptExecutor must not be null.");
        }
        this.executor = executor;
    }

    @Override
    public Object execute(String script, Object... args) {
        return executor.executeScript(script, args);
    }

    @Override
    public Object executeAsync(String script, Object... args) {
        return executor.executeAsyncScript(script, args);
    }
}
