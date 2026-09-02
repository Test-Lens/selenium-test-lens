package io.github.testlens.testng;

import io.github.testlens.TestLensOptions;
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
