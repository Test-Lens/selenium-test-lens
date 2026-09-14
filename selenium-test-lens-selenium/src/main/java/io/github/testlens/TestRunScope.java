package io.github.testlens;

import org.openqa.selenium.WebDriver;

import java.util.Objects;

/**
 * Explicit runner-neutral owner of one logical run's {@link SuiteStateManager}.
 * JUnit 5 and TestNG adapters create and close equivalent scopes automatically. Manual users should keep the scope
 * open for the run and attach each invocation's Lens through it.
 *
 * @since 0.3.0
 */
public final class TestRunScope implements AutoCloseable {
    private final SuiteStateManager suiteState = new SuiteStateManager();
    private boolean closed;

    private TestRunScope() {
        ManagedStateSupport.suiteOpened();
    }

    /** Opens an isolated manual logical run scope. @since 0.3.0 */
    public static TestRunScope open() { return new TestRunScope(); }

    /** Attaches a Lens using default options and this run's suite state. @since 0.3.0 */
    public synchronized TestLens attach(WebDriver driver) {
        return attach(driver, TestLensOptions.defaults());
    }

    /** Attaches a Lens using supplied options and this run's suite state. @since 0.3.0 */
    public synchronized TestLens attach(WebDriver driver, TestLensOptions options) {
        ensureOpen();
        return new TestLens(Objects.requireNonNull(driver, "driver"), options, ignored -> { }, suiteState);
    }

    /** Returns state shared by Lens instances attached to this open run. @since 0.3.0 */
    public synchronized SuiteStateManager suiteState() {
        ensureOpen();
        return suiteState;
    }

    /** Clears suite state. Closing an already closed run is harmless. @since 0.3.0 */
    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        suiteState.close();
        ManagedStateSupport.suiteClosed();
    }

    private void ensureOpen() {
        if (closed) throw new TestStateException("Test run scope is closed");
    }
}
