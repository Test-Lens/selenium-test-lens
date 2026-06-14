package io.github.mmaciekk111.uitestlens.core.browser;

public interface BrowserScriptExecutor {
    Object execute(String script, Object... args);

    default Object executeAsync(String script, Object... args) {
        throw new UnsupportedOperationException("Async script execution is not supported by this executor.");
    }
}
