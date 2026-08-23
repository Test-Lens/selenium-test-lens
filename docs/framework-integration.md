# Integrating Test Lens with an existing Selenium project

Test Lens is designed to fit into an existing Selenium test stack. Keep your current driver factory, test runner, Page Objects and reporting tools.

Create one `TestLens` instance for each driver/test invocation:

```java
TestLens lens = TestLens.attach(driver);
lens.startSession(testName);
```

## Integration model

Your project remains responsible for creating and closing `WebDriver`. Test Lens attaches to that driver, records Lens operations, and writes session diagnostics when the test finishes.

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

A JUnit 5 extension can create one driver and Lens instance in `beforeEach`, then finalize and clean them up in `afterEach`.

```java
final class LensExtension implements BeforeEachCallback, AfterEachCallback {
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(LensExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) {
        WebDriver driver = ExistingDriverFactory.create();
        try {
            TestLens lens = TestLens.attach(driver);
            lens.startSession(context.getDisplayName());
            context.getStore(NAMESPACE).put(context.getUniqueId(),
                    new State(driver, lens));
        } catch (RuntimeException | Error failure) {
            driver.quit();
            throw failure;
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        State state = context.getStore(NAMESPACE)
                .remove(context.getUniqueId(), State.class);
        if (state == null) {
            return;
        }

        Throwable failure = context.getExecutionException().orElse(null);
        try {
            if (failure == null) {
                state.lens().finishPassed();
            } else {
                state.lens().finishFailed(failure);
            }
        } finally {
            state.driver().quit();
        }
    }

    private record State(WebDriver driver, TestLens lens) {}
}
```

Register the extension once per test class. Parameterized and repeated tests receive a separate JUnit invocation and therefore separate stored state.

Test Lens 0.1.0 exposes passed/failed session finalization. In this example, any JUnit execution exception is recorded as a failed Lens session.

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
            if (status == ITestResult.SUCCESS) {
                current.lens().finishPassed();
            } else {
                Throwable failure = result.getThrowable();
                if (failure == null) {
                    failure = status == ITestResult.SKIP
                            ? new SkipException("TestNG invocation skipped")
                            : new AssertionError("TestNG result status: " + status);
                }
                current.lens().finishFailed(failure);
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

In this example, skipped TestNG invocations are recorded as failed Lens sessions because Test Lens 0.1.0 has no skipped finalizer. `SkipException` supplies the reason when TestNG provides no throwable. If your suite uses a different reporting policy for skipped tests, map that status explicitly in your integration.

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
