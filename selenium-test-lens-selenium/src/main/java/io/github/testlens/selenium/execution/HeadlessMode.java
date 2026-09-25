package io.github.testlens.selenium.execution;

/**
 * Browser execution intent supplied before a WebDriver session is created.
 *
 * @since 0.4.0
 */
public enum HeadlessMode {
    /** Preserve the browser factory's existing execution-mode decision. */
    UNSET,
    /** Request headless browser execution. */
    TRUE,
    /** Request headed browser execution. */
    FALSE
}
