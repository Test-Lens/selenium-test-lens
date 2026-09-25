package io.github.testlens.testng;

import io.github.testlens.TestLensOptions;
import io.github.testlens.selenium.execution.BrowserExecutionConfig;
import io.github.testlens.selenium.execution.HeadlessMode;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;

import java.util.Objects;
import java.util.UUID;

/** Creates and configures one independently owned browser invocation. */
public interface TestLensTestNgFactory {
    /**
     * Creates the WebDriver owned by the listener for this invocation.
     *
     * @return a new non-null driver
     */
    WebDriver createDriver();

    /**
     * Creates a driver after resolving pre-session execution intent.
     *
     * @param executionConfig immutable resolved execution configuration
     * @return a new non-null driver
     * @since 0.4.0
     */
    default WebDriver createDriver(BrowserExecutionConfig executionConfig) {
        BrowserExecutionConfig resolved = Objects.requireNonNull(executionConfig, "executionConfig");
        if (resolved.headless() != HeadlessMode.UNSET) {
            throw new IllegalStateException("Headed/headless execution intent " + resolved.headless()
                    + " is configured, but this TestLensTestNgFactory does not support BrowserExecutionConfig; "
                    + "override createDriver(BrowserExecutionConfig)");
        }
        return createDriver();
    }

    /**
     * Supplies immutable Lens options for this invocation.
     *
     * @return non-null Lens options
     */
    default TestLensOptions lensOptions() {
        return TestLensOptions.defaults();
    }

    /**
     * Creates a report-safe session name. The default never includes DataProvider values.
     *
     * @param result the physical TestNG invocation
     * @return a non-null session name
     */
    default String sessionName(ITestResult result) {
        Objects.requireNonNull(result, "result");
        String method = result.getMethod().getRealClass().getSimpleName()
                + "." + result.getMethod().getMethodName();
        int invocation = Math.max(1, result.getMethod().getCurrentInvocationCount() + 1);
        return method + " [invocation " + invocation + ", attempt "
                + UUID.randomUUID().toString().substring(0, 8) + "]";
    }
}
