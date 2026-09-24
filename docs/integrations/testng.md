# TestNG integration

The optional `selenium-test-lens-testng` module defaults to one `WebDriver`, `TestLens`, and session for every physical TestNG test-method invocation. The unreleased 0.4.0 line also offers explicit class-scoped driver reuse while retaining a separate Test Lens session and report for every invocation. Adding the dependency alone does not register a listener.

`TestLensTestNgContext.current().lens().expectPage()` polls URL or title in the active window of that physical invocation. Assertion polling stays within the session and does not become a TestNG retry or a recovery retry; popup selection remains an explicit Selenium/Lens context operation.

## Installation

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens-testng</artifactId>
    <version>0.3.0</version>
    <scope>test</scope>
</dependency>
```

Declare Selenium separately at the version selected by the test project. TestNG is a dependency of this adapter only; core, overlay, the main runtime, React, and the JUnit 5 adapter do not depend on it.

Gradle Kotlin DSL uses
`testImplementation("io.github.test-lens:selenium-test-lens-testng:0.3.0")`;
Groovy DSL uses
`testImplementation 'io.github.test-lens:selenium-test-lens-testng:0.3.0'`.
These coordinates are published with the 0.3.0 release.

## Factory and listener

Factories need an accessible public no-argument constructor. `PER_METHOD` constructs one for every physical invocation; `PER_CLASS` constructs one lazily for each concrete TestNG class instance and `XmlTest` context.

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
public abstract DriverScope driverScope()
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
public void onAfterClass(ITestClass testClass)
public void onFinish(ISuite suite)
```

## Driver scope

`PER_METHOD` is the default and preserves the established adapter contract exactly: the factory, driver, Lens, and session are created for the physical `@Test` invocation, `current()` is available in the test body, and finalization plus `quit()` happen after that test callback. It does not extend the managed context into user configuration hooks.

`PER_CLASS` is an explicit 0.4.0 opt-in for sequential tests that intentionally share browser state:

```java
@Listeners(TestLensTestNgListener.class)
@TestLensTestNg(
        factory = ChromeFactory.class,
        driverScope = DriverScope.PER_CLASS
)
public final class AccountPageTests {
    @BeforeMethod(alwaysRun = true)
    public void prepare() {
        TestLensTestNgContext.current().lens().waitForInteractiveOrComplete();
    }

    @Test
    public void editsProfile() {
        TestLensTestNgContext.current().lens().getByTestId("edit-profile").click();
    }

    @AfterMethod(alwaysRun = true)
    public void evidence() {
        TestLensTestNgContext.current().lens().captureScreenshot("after-method");
    }
}
```

The managed envelope is:

```text
adapter: acquire/reuse driver + start invocation session
user: all applicable BeforeMethod hooks
user: Test body or TestNG skip caused by setup
user: all applicable AfterMethod hooks
adapter: finish/export invocation session and clear callback binding
user: applicable AfterClass hooks
adapter: quit the class-instance owner exactly once
```

`current()` is intentionally unavailable in `BeforeClass`, `AfterClass`, group, test, and suite configuration hooks. TestNG 7.9 may call `IClassListener.onAfterClass` either before or after user `AfterClass` methods depending on `testng.listener.execution.symmetric`; Test Lens observes both boundaries and never treats the listener callback alone as permission to quit.

The owner key uses the owning `ISuite`, `ITestContext`/`XmlTest`, and concrete instance by identity. Two `@Factory` instances, the same class in two `<test>` blocks, and a later suite in the same JVM therefore receive different drivers. `parallel="instances"`, `parallel="classes"`, or independent `<test>` execution may run different owners concurrently. One owner remains sequential: `parallel="methods"`, a parallel DataProvider, and `invocationCount` with a multi-thread pool fail before the expensive driver is created:

```text
Shared WebDriver scope PER_CLASS conflicts with parallel method execution.
Use PER_METHOD, or execute this class sequentially.
```

Test Lens does not serialize such tests silently and does not disable TestNG parallelism globally.

`parallel=classes`, `parallel=tests`, and `parallel=instances` can run independent owners concurrently only when each owner's methods remain sequential. For factory instances, use TestNG's `group-by-instances="true"`; `parallel=instances` alone is not treated as proof that one instance cannot overlap, and the runtime guard rejects an actual overlap before the second user callback receives the driver.

TestNG may move sequential callbacks between worker threads. The adapter therefore identifies owners by suite/context/instance and guards actual overlapping invocations rather than treating `threadId` as identity. It does not unwrap or weaken a consumer-supplied Selenium `ThreadGuard`; a driver deliberately confined to one creation thread remains incompatible with any TestNG schedule that invokes it from another thread.

Every DataProvider row and every RetryAnalyzer attempt attaches a fresh `TestLens` and starts a fresh session with its own session ID, terminal status, scenario resources, evidence, network buffer, and report directory while a healthy class driver remains the same. Before the first `BeforeMethod`, TestNG has not yet published the final test `ITestResult`; the adapter gives `sessionName(...)` a read-only invocation descriptor containing the actual test method, instance, and context. It intentionally exposes no raw DataProvider values. At test entry the adapter binds the real result. Configuration failures are combined into the Lens outcome without rewriting unrelated TestNG results.

Class-scoped reuse does not navigate, refresh, clear cookies/storage, close windows, or reset authentication between methods. Test code remains responsible for application-state isolation and must declare TestNG dependencies/priorities if a demonstration intentionally relies on order. Do not call `quit()` in `AfterMethod` or `AfterClass`; the adapter owns the managed driver. A definitively lost session is not silently replaced with a different browser.

## Outcome and ownership contract

The listener maps `SUCCESS` to `finishPassed()`, `FAILURE` and `SUCCESS_PERCENTAGE_FAILURE` to `finishFailed(originalThrowable)`, and `SKIP`/`SkipException` to `finishSkipped(reason)`. It finalizes reports before calling `driver.quit()` and then removes the `ITestResult` state. If the test already finalized its Lens, the first terminal outcome and the exactly-once report pipeline are reused; the callback does not create a second terminal event or bundle. Do not call `quit()` again from `@AfterMethod`.

The listener never uploads reports. A test may explicitly finalize and synchronously upload before it returns; the listener then observes the existing terminal result and still owns its one driver cleanup. A suite-wide transport policy belongs in a project-owned listener ordered around the explicit [report uploader](../observability/report-upload.md), with credentials supplied from controlled configuration rather than test metadata.

If cleanup fails after an already failed or skipped test, the cleanup error is suppressed on the original throwable. Cleanup failure after a passed test changes the TestNG result to failure. A setup failure remains primary; a driver already created before attach/session failure is still closed once.

For `FAILURE` and `SUCCESS_PERCENTAGE_FAILURE`, the listener completes the failure bundle before its single `driver.quit()`. A partial bundle does not change TestNG status or replace the original throwable. Policy-induced failure follows the same ordering and is not finalized twice.

Disabled tests, configuration methods, dependency-skipped methods, and tests blocked by a failed `@BeforeMethod` do not create a driver or empty session. Calling `TestLensTestNgContext.current()` outside a managed test method throws `IllegalStateException`.

## DataProvider, retry, and parallel execution

In `PER_METHOD`, state is stored on the physical `ITestResult`; parallel methods and parallel DataProviders cannot see one another's resources. Every RetryAnalyzer attempt gets its own factory, driver, Lens, session ID, scenario scope, report directory, and status. In `PER_CLASS`, the driver owner is separate from the callback-local invocation binding: retries and sequential DataProvider rows reuse the healthy driver but never reuse terminal Lens state. One `SuiteStateManager` remains stored on the owning `ISuite` and is cleared at suite end. See [Managed Test State & Resources](../features/managed-test-state.md).

The default name contains the class, method, public TestNG invocation counter, and an opaque per-attempt token. It deliberately excludes DataProvider values. A custom `sessionName(ITestResult)` may return a different name, but should not include credentials or other parameter secrets.

## Recovery-retry policy

Return configured `TestLensOptions` from the factory. A policy violation from an otherwise successful method is written into the completed reports, then the listener explicitly changes its `ITestResult` to `FAILURE` and installs the `RetryPolicyViolationException` as throwable. Driver quit still happens exactly once; later cleanup errors are suppressed. `IRetryAnalyzer` attempts remain independent sessions rather than one aggregated summary.

Lens-owned network capture is stopped by every invocation finalizer. A later `PER_CLASS` invocation may start a new capture generation on the same driver without retaining the prior invocation's buffer or listeners; the driver is quit only after the class-instance lifecycle. Requests still in flight at the boundary cannot be reassigned perfectly when the browser protocol supplies no invocation correlation, so tests should await material requests before returning. A factory that selects `BIDI` or `AUTO` must create local or remote Chrome/Firefox options with BiDi enabled. One remote provider session can therefore contain several independent Test Lens reports; Test Lens does not overwrite provider status or take ownership from a provider SDK.

## Local 0.4.0 snapshot validation

Install the current reactor without publishing it, then point the consumer at the snapshot and verify resolution:

```powershell
mvn -DskipTests install
mvn dependency:tree "-Dincludes=io.github.test-lens"
```

Use `0.4.0-SNAPSHOT`, run a sequential `PER_CLASS` fixture, and confirm one Selenium session ID, multiple Test Lens session IDs/report directories, and one final `quit()`. For a BrowserStack smoke test, enable Selenium BiDi in the consumer's already-verified provider configuration; credentials must remain outside source and logs.

The factory's `TestLensOptions.redactionPolicy(...)` is used for that physical invocation through final report/bundle creation. Redaction errors remain diagnostic and never replace the original TestNG throwable or alter status mapping; the listener still quits its owned driver exactly once afterward. Screenshot pixels remain outside this protection.
