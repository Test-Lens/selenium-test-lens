package io.github.mmaciekk111.uitestlens.core.browser;

/**
 * Framework-neutral bridge for executing JavaScript in a browser context.
 *
 * <p>Selenium and other adapters implement this interface outside the core module.
 */
public interface BrowserScriptExecutor {
    Object execute(String script, Object... args);

    default Object executeAsync(String script, Object... args) {
        throw new UnsupportedOperationException("Async script execution is not supported by this executor.");
    }
}
