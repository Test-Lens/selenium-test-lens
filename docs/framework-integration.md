# Integrating Test Lens with an existing Selenium project

Test Lens is designed to fit into an existing Selenium test stack. Keep your current driver factory, test runner, Page Objects and reporting tools.

Create one `TestLens` instance for each driver/test invocation:

```java
TestLens lens = TestLens.attach(driver);
lens.startSession(testName);
```

## Integration model

In a manual integration, your project remains responsible for creating and closing `WebDriver`. Test Lens attaches to that driver, records Lens operations, and writes session diagnostics when the test finishes. The optional JUnit 5 and TestNG integrations deliberately own drivers returned by their configured factories.

A typical integration has three lifecycle points:

1. Create the driver as usual.
2. Attach Test Lens and start a session for the test invocation.
3. Finalize the Lens session with the test outcome, then close the driver.

If `runTest(...)` can propagate checked exceptions, the enclosing integration method should declare `throws Exception`.

```java
WebDriver driver = ExistingDriverFactory.create();

try {
    TestLens lens = TestLens.attach(driver);
    lens.startSession(testName);

    try {
        runTest(driver, lens);
    } catch (Exception | Error failure) {
        lens.finishFailed(failure);
        throw failure;
    }
    // Keep this outside the catch above: a retry policy may finalize FAILED
    // and throw RetryPolicyViolationException after writing the reports.
    lens.finishPassed();
} finally {
    driver.quit();
}
```

Keep driver shutdown in your existing framework cleanup. If your project has specific handling for WebDriver cleanup failures, keep that policy when adding Test Lens.

Locator composition is runner-neutral. A locator can be created in setup or page-object code and later scoped, filtered, or count-waited in JUnit 5, TestNG, or a custom runner; it retains the owning driver's current frame/window and the parent locator's options and diagnostics.

For a final `FAILED` status, finalization also completes the best-effort failure bundle before it returns. This preserves the live browser for screenshots, context and optional DOM/console capture. The JUnit 5 extension and TestNG listener therefore keep the same ordering: finish Lens and all bundle/report work first, then call `driver.quit()` exactly once.

You can introduce Lens gradually. Existing Page Objects and direct Selenium calls can continue to use the attached driver.

Page Objects can keep form interaction behind `UiLocator`: `check()`, `uncheck()`, `isChecked()`, `upload(Path...)`, `focus()`, and `scrollIntoView()` avoid exposing a raw `WebElement`. The lifecycle ownership rules below are unchanged by these actions.

## JUnit 5

Use the published `selenium-test-lens-junit5` module instead of copying lifecycle callbacks into each project:

```java
@RegisterExtension
final TestLensExtension testLens =
        TestLensExtension.builder(ExistingDriverFactory::create).build();

@Test
void savesOrder(WebDriver driver, TestLens lens) {
    driver.get(applicationUrl);
    lens.getByTestId("save").click();
}
```

Register it once per test class. Parameterized, repeated, nested, and parallel invocations receive separate state keyed by JUnit's unique context ID. The extension maps normal completion to `finishPassed()`, `TestAbortedException` (including assumptions) to `finishSkipped(reason)`, and other failures to `finishFailed(originalFailure)`.

If `finishPassed()` detects a configured retry-policy violation, the extension propagates it as the invocation failure after Lens has finalized and exported the session. It does not call `finishFailed()` a second time.

The extension owns the driver returned by the factory. It finalizes reports before calling `driver.quit()`, so do not add another quit in `@AfterEach`. Disabled tests do not create drivers or sessions. See the dedicated [JUnit 5 integration guide](integrations/junit5.md) for dependencies, configuration, cleanup errors, and the complete concurrency contract.

## TestNG

Use the published `selenium-test-lens-testng` listener rather than copying `@BeforeMethod`/`@AfterMethod` lifecycle code:

```java
@Listeners(TestLensTestNgListener.class)
@TestLensTestNg(factory = ExistingDriverFactory.class)
class OrderTest {
    @Test
    void savesOrder() {
        TestLens lens = TestLensTestNgContext.current().lens();
        lens.getByTestId("save").click();
    }
}
```

Both annotations are required; there is no automatic ServiceLoader registration. The adapter maps success, failure, skip, and success-percentage failure, owns `quit()`, and isolates state on the physical `ITestResult`. See the dedicated [TestNG integration guide](integrations/testng.md) for factory construction, retry/DataProvider/parallel behavior, and cleanup policy. A hand-written `@BeforeMethod`/`@AfterMethod` integration remains a legacy/manual alternative when a project needs a different ownership model.

For a retry-policy violation on an otherwise successful invocation, the listener explicitly changes the `ITestResult` to `FAILURE` and installs the policy exception as its throwable; throwing only from a listener callback is not relied upon.

## Allure coexistence

Allure and Test Lens can be used side by side. Keep existing Allure listeners, annotations, attachments, and result directories unchanged.

```java
@Step("Submit order")
void submitOrder() {
    lens.locator(SUBMIT, "Submit order").click();
}
```

Framework adapters expose the same main facade, so tests may use `lens.getByLabel("Email").fill(...)` or `lens.getByRole("button", "Submit order")` without runner-specific locator behavior.

Allure continues to produce `allure-results`; Test Lens writes its own diagnostics under `target/ui-test-lens` by default. You do not need to wrap every Allure step in `lens.step()`.

## Parallel execution

Associate each `TestLens` instance with exactly one driver and test invocation. Do not store a driver or Lens instance in a shared static field.

- In JUnit 5, store invocation state in `ExtensionContext.Store` using the invocation's unique ID.
- In TestNG, prefer the adapter's namespaced `ITestResult` state; manual integrations must provide equivalent per-invocation isolation.
- Give retry attempts distinct session names if their reports are collected together.

## Optional React extension

React support is separate from the main runtime. Add it only when your tests need the React-specific helpers:

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens-react</artifactId>
    <version>0.1.0</version>
</dependency>
```

The main `selenium-test-lens` artifact has no React dependency. Standard DOM interactions can continue to use the `TestLens` facade; the React extension is optional.

## Next steps

- [Install Selenium Test Lens](getting-started.md#installation)
- [Use locators, waits, assertions, and browser contexts](elements/index.md)

Framework adapters do not change assertion semantics: state and collection assertions poll inside the current invocation and return/throw the same `UiAssertionResult`/`UiAssertionError` as manual lifecycle usage. Their polling is not a runner retry and does not mark a session flaky.

The same rule applies to `lens.expectPage()`. JUnit 5 and TestNG tests can assert the active window's URL/title directly; this page polling remains inside the current runner invocation and is not a runner-level or recovery retry.
- [Migrate incrementally from raw Selenium](migration.md)
- [Browse practical examples](examples.md)
