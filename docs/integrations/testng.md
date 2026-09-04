# TestNG integration

The optional `selenium-test-lens-testng` module owns one `WebDriver`, `TestLens`, and session for every physical TestNG test-method invocation. The coordinate below follows the unreleased `0.2.0-SNAPSHOT` source tree and therefore requires a local source build or a configured snapshot repository. It uses TestNG's invocation listener and `ITestResult` attributes; adding the dependency alone does not register a listener.

`TestLensTestNgContext.current().lens().expectPage()` polls URL or title in the active window of that physical invocation. Assertion polling stays within the session and does not become a TestNG retry or a recovery retry; popup selection remains an explicit Selenium/Lens context operation.

## Installation

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens-testng</artifactId>
    <version>0.2.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

Declare Selenium separately at the version selected by the test project. TestNG is a dependency of this adapter only; core, overlay, the main runtime, React, and the JUnit 5 adapter do not depend on it.

## Factory and listener

Factories need an accessible public no-argument constructor. A new factory is constructed for every physical invocation.

```java
public final class ChromeFactory implements TestLensTestNgFactory {
    public ChromeFactory() {}

    @Override
    public WebDriver createDriver() {
        return new ChromeDriver();
    }

    @Override
    public TestLensOptions lensOptions() {
        return TestLensOptions.builder()
                .outputRoot(Path.of("target", "ui-test-lens"))
                .build();
    }
}
```

Register the listener explicitly and configure its factory separately. `@Listeners` is not used as a meta-annotation, and there is no `META-INF/services` registration, so **both annotations are required**.

```java
@Listeners(TestLensTestNgListener.class)
@TestLensTestNg(factory = ChromeFactory.class)
class LoginTest {
    @Test
    void login() {
        TestLensTestNgContext invocation = TestLensTestNgContext.current();
        WebDriver driver = invocation.driver();
        TestLens lens = invocation.lens();
        UiTestLensSession session = invocation.session();

        driver.get(applicationUrl);
        lens.getByTestId("login").click();
    }
}
```

<!-- API SIGNATURES: io.github.testlens.testng.TestLensTestNg -->
```java
public abstract Class<? extends TestLensTestNgFactory> factory()
```

<!-- API SIGNATURES: io.github.testlens.testng.TestLensTestNgFactory -->
```java
public abstract WebDriver createDriver()
public TestLensOptions lensOptions()
public String sessionName(ITestResult result)
```

<!-- API SIGNATURES: io.github.testlens.testng.TestLensTestNgContext -->
```java
public static TestLensTestNgContext current()
public WebDriver driver()
public TestLens lens()
public UiTestLensSession session()
```

<!-- API SIGNATURES: io.github.testlens.testng.TestLensTestNgListener -->
```java
public TestLensTestNgListener()
public void beforeInvocation(IInvokedMethod method, ITestResult result)
public void afterInvocation(IInvokedMethod method, ITestResult result)
```

## Outcome and ownership contract

The listener maps `SUCCESS` to `finishPassed()`, `FAILURE` and `SUCCESS_PERCENTAGE_FAILURE` to `finishFailed(originalThrowable)`, and `SKIP`/`SkipException` to `finishSkipped(reason)`. It finalizes reports before calling `driver.quit()` and then removes the `ITestResult` state. Do not call `quit()` again from `@AfterMethod`.

If cleanup fails after an already failed or skipped test, the cleanup error is suppressed on the original throwable. Cleanup failure after a passed test changes the TestNG result to failure. A setup failure remains primary; a driver already created before attach/session failure is still closed once.

For `FAILURE` and `SUCCESS_PERCENTAGE_FAILURE`, the listener completes the failure bundle before its single `driver.quit()`. A partial bundle does not change TestNG status or replace the original throwable. Policy-induced failure follows the same ordering and is not finalized twice.

Disabled tests, configuration methods, dependency-skipped methods, and tests blocked by a failed `@BeforeMethod` do not create a driver or empty session. Calling `TestLensTestNgContext.current()` outside a managed test method throws `IllegalStateException`.

## DataProvider, retry, and parallel execution

State is stored as a namespaced attribute of the physical `ITestResult`, not on the test class or listener. Parallel methods and parallel DataProviders therefore cannot see one another's drivers or sessions, and reusing a test instance is safe. Every RetryAnalyzer attempt gets its own factory, driver, Lens, session ID, report directory, and final status: a failed attempt is recorded as `FAILED`, while a later successful attempt is a separate `PASSED` session.

The default name contains the class, method, public TestNG invocation counter, and an opaque per-attempt token. It deliberately excludes DataProvider values. A custom `sessionName(ITestResult)` may return a different name, but should not include credentials or other parameter secrets.

## Recovery-retry policy

Return configured `TestLensOptions` from the factory. A policy violation from an otherwise successful method is written into the completed reports, then the listener explicitly changes its `ITestResult` to `FAILURE` and installs the `RetryPolicyViolationException` as throwable. Driver quit still happens exactly once; later cleanup errors are suppressed. `IRetryAnalyzer` attempts remain independent sessions rather than one aggregated summary.

Lens-owned network capture is stopped by every finalizer before the listener's single `driver.quit()`. A factory that selects `BIDI` or `AUTO` must create Chrome/Firefox options with `enableBiDi()`; missing BiDi is reported as unsupported and never falls back to manual events.

The factory's `TestLensOptions.redactionPolicy(...)` is used for that physical invocation through final report/bundle creation. Redaction errors remain diagnostic and never replace the original TestNG throwable or alter status mapping; the listener still quits its owned driver exactly once afterward. Screenshot pixels remain outside this protection.
