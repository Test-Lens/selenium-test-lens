package io.github.testlens.testng;

import io.github.testlens.TestLens;
import io.github.testlens.core.trace.UiTestLensSession;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.Reporter;

/** Read-only access to the resources owned by the current managed TestNG invocation. */
public final class TestLensTestNgContext {
    private final WebDriver driver;
    private final TestLens lens;
    private final UiTestLensSession session;

    TestLensTestNgContext(WebDriver driver, TestLens lens, UiTestLensSession session) {
        this.driver = driver;
        this.lens = lens;
        this.session = session;
    }

    /**
     * Returns the current physical invocation's context.
     *
     * @return managed invocation resources
     * @throws IllegalStateException outside a managed TestNG test invocation
     */
    public static TestLensTestNgContext current() {
        ITestResult result = Reporter.getCurrentTestResult();
        Object state = result == null ? null : result.getAttribute(TestLensTestNgListener.STATE_ATTRIBUTE);
        if (!(state instanceof TestLensTestNgListener.InvocationState invocation)) {
            throw new IllegalStateException(
                    "No active Selenium Test Lens TestNG invocation; register TestLensTestNgListener "
                            + "and annotate the test class with @TestLensTestNg");
        }
        return invocation.context();
    }

    /**
     * Returns the WebDriver owned by the listener.
     *
     * @return the WebDriver owned by the listener
     */
    public WebDriver driver() {
        return driver;
    }

    /**
     * Returns the Lens attached to {@link #driver()}.
     *
     * @return the Lens attached to {@link #driver()}
     */
    public TestLens lens() {
        return lens;
    }

    /**
     * Returns the trace session for this invocation.
     *
     * @return the trace session for this invocation
     */
    public UiTestLensSession session() {
        return session;
    }
}
