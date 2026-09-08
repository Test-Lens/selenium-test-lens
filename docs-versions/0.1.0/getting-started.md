# Getting started

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

This example is taken from the API present in tag `v0.1.0`. Expanded browser-computed semantic locators, page assertions, form actions, central redaction, automatic failure bundles, and passive BiDi capture are documented only in the development version.
