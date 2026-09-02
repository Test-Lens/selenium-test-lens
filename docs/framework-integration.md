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
        lens.finishPassed();
    } catch (Exception | Error failure) {
        lens.finishFailed(failure);
        throw failure;
    }
} finally {
    driver.quit();
}
```

Keep driver shutdown in your existing framework cleanup. If your project has specific handling for WebDriver cleanup failures, keep that policy when adding Test Lens.

You can introduce Lens gradually. Existing Page Objects and direct Selenium calls can continue to use the attached driver.

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

## Allure coexistence

Allure and Test Lens can be used side by side. Keep existing Allure listeners, annotations, attachments, and result directories unchanged.

```java
@Step("Submit order")
void submitOrder() {
    lens.locator(SUBMIT, "Submit order").click();
}
```

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
- [Migrate incrementally from raw Selenium](migration.md)
- [Browse practical examples](examples.md)
