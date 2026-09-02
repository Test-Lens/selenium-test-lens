# Integrating Test Lens with an existing Selenium project

Test Lens is designed to fit into an existing Selenium test stack. Keep your current driver factory, test runner, Page Objects and reporting tools.

Create one `TestLens` instance for each driver/test invocation:

```java
TestLens lens = TestLens.attach(driver);
lens.startSession(testName);
```

## Integration model

In a manual integration, your project remains responsible for creating and closing `WebDriver`. Test Lens attaches to that driver, records Lens operations, and writes session diagnostics when the test finishes. The optional JUnit 5 extension is the deliberate exception: it owns drivers returned by the configured factory.

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

With TestNG, attach Lens in `@BeforeMethod` and finalize it in an `@AfterMethod(alwaysRun = true)` method. A `ThreadLocal` keeps invocation state separate when methods run in parallel.

```java
private final ThreadLocal<State> state = new ThreadLocal<>();

@BeforeMethod
public void beforeMethod(Method method) {
    WebDriver driver = ExistingDriverFactory.create();
    try {
        TestLens lens = TestLens.attach(driver);
        lens.startSession(method.getName());
        state.set(new State(driver, lens));
    } catch (RuntimeException | Error failure) {
        driver.quit();
        throw failure;
    }
}

@AfterMethod(alwaysRun = true)
public void afterMethod(ITestResult result) {
    State current = state.get();
    try {
        if (current != null) {
            int status = result.getStatus();
            switch (status) {
                case ITestResult.SUCCESS -> current.lens().finishPassed();
                case ITestResult.FAILURE -> current.lens().finishFailed(result.getThrowable());
                case ITestResult.SKIP -> {
                    Throwable skipped = result.getThrowable();
                    String reason = skipped == null
                            ? "TestNG invocation skipped"
                            : skipped.getMessage();
                    current.lens().finishSkipped(reason);
                }
                default -> current.lens().finishFailed(result.getThrowable());
            }
        }
    } finally {
        if (current != null) {
            current.driver().quit();
        }
        state.remove();
    }
}

private record State(WebDriver driver, TestLens lens) {}
```

The three TestNG terminal states map directly to the three Lens finalizers. Do not manufacture a `SkipException` merely to persist a reason: pass the runner-provided message, or a plain fallback string, to `finishSkipped(reason)`. This snippet belongs in the consuming TestNG project; the Test Lens runtime itself has no TestNG dependency.

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
- In TestNG, use a `ThreadLocal` or an invocation-ID keyed map when methods or data providers run in parallel.
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
