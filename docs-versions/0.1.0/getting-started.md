# Getting started

**Selenium Test Lens 0.1.0 — stable**

Attach Test Lens to a driver created and owned by the consuming test framework:

```java
TestLens lens = TestLens.attach(driver);
lens.startSession("login");
try {
    driver.get("https://example.test/login");
    lens.locator(By.id("username"), "Username").fill("john");
    lens.locator(By.id("login"), "Login").click();
    lens.locator(By.id("welcome"), "Welcome").expect().toBeVisible();
    lens.finishPassed();
} catch (RuntimeException | Error failure) {
    lens.finishFailed(failure);
    throw failure;
} finally {
    driver.quit();
}
```

`TestLens` does not create or close the driver. Call one terminal method after the test body and close the driver in the framework-owned cleanup. In 0.1.0, treat finalization as a one-shot operation; the stronger idempotent finalization contract belongs to the development line.

Continue with [session lifecycle](session-lifecycle.md), [locators](locators.md), and [trace and reports](trace-and-reports.md).
