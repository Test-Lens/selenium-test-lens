# Integrating Test Lens with an existing Selenium project

Test Lens attaches to the `WebDriver` your framework already owns. It does not replace the driver factory, runner, Page Objects, or an existing reporter.

```java
TestLens lens = TestLens.attach(existingDriver);
lens.startSession("login");

lens.locator(By.id("username"), "Username").fill("john");
lens.locator(By.id("login"), "Login").click();
lens.locator(By.cssSelector(".result"), "Result").waitUntilVisible();
```

Native actions, waits, reads, assertions, collection operations, screenshots, and steps use one event pipeline for the trace and best-effort HUD. A document navigation does not require another `initHud()` call.

Frames, windows and tabs keep the same driver and Lens session. HUD reinjection follows the current browsing context:

```java
lens.switchToFrame(PAYMENT_FRAME, "Payment frame");
lens.locator(PAY, "Pay").click();
lens.switchToDefaultContent();

Set<String> before = lens.windowHandles();
lens.locator(OPEN_RECEIPT, "Open receipt").click();
lens.switchToNewWindow(before, "Receipt");
```

Create one `TestLens` per driver/test invocation. No frame, window, alert, select, or `Actions` instance is stored globally.

## JUnit 5

Use one driver and one `TestLens` per test invocation. A class-level extension implementing `BeforeEachCallback`, `AfterEachCallback`, and `TestWatcher` can store them in `ExtensionContext.Store`. Start the session in `beforeEach`; call `finishPassed()` from `testSuccessful` and `finishFailed(cause)` from `testFailed`. Both methods are diagnostic and do not throw, so the runner retains ownership of the original outcome.

```java
final class LensExtension implements BeforeEachCallback, TestWatcher {
    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(LensExtension.class);

    public void beforeEach(ExtensionContext context) {
        WebDriver driver = ExistingDriverFactory.create();
        TestLens lens = TestLens.attach(driver);
        lens.startSession(context.getDisplayName());
        context.getStore(NS).put(context.getUniqueId(), new State(driver, lens));
    }

    public void testSuccessful(ExtensionContext context) { finish(context, null); }
    public void testFailed(ExtensionContext context, Throwable cause) { finish(context, cause); }

    private void finish(ExtensionContext context, Throwable cause) {
        State state = context.getStore(NS).remove(context.getUniqueId(), State.class);
        if (state == null) return;
        try {
            if (cause == null) state.lens().finishPassed(); else state.lens().finishFailed(cause);
        } finally {
            state.driver().quit();
        }
    }
    record State(WebDriver driver, TestLens lens) {}
}
```

Register the extension at class level so parameterized, repeated, and nested invocations receive separate state. Use the invocation unique ID rather than a static field when parallel execution is enabled.

## TestNG

`@BeforeMethod`/`@AfterMethod(alwaysRun = true)` is sufficient. For `parallel="methods"`, parallel DataProviders, and retry analyzers, store invocation state in a `ThreadLocal` or an invocation-ID keyed concurrent map; never share a driver or Lens instance.

```java
private final ThreadLocal<State> state = new ThreadLocal<>();

@BeforeMethod
public void before(Method method) {
    WebDriver driver = ExistingDriverFactory.create();
    TestLens lens = TestLens.attach(driver);
    lens.startSession(method.getName());
    state.set(new State(driver, lens));
}

@AfterMethod(alwaysRun = true)
public void after(ITestResult result) {
    State current = state.get();
    try {
        if (current != null) {
            if (result.isSuccess()) current.lens().finishPassed();
            else current.lens().finishFailed(result.getThrowable());
        }
    } finally {
        if (current != null) current.driver().quit();
        state.remove();
    }
}
```

Give retry attempts distinct test/session names when aggregating results.

## Allure coexistence

Allure and Test Lens are independent observers of the same test:

```java
@Step("Submit order") // existing Allure step stays
void submitOrder() {
    lens.locator(SUBMIT, "Submit order").click();
}
```

- Keep Allure listeners, `@Step`, `@Attachment`, and `allure-results` unchanged.
- Lens writes its own session-scoped HTML, JSON, and evidence under `target/ui-test-lens` by default.
- Do not mechanically wrap every Allure step in `lens.step()`; use Lens steps only where the additional business grouping is useful.
- The runner still controls PASS/FAIL. `finishPassed/finishFailed` return diagnostic results and do not replace the original exception.
