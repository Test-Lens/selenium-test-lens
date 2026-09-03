# Migrating from Selenium

You do not need to rewrite an existing Selenium suite to adopt Test Lens. Attach it to the `WebDriver` you already use, then migrate interactions and assertions where the additional waits and diagnostics are useful.

Raw Selenium can remain in the same Page Objects and tests.

## Migrating pre-1.0 overlay construction for 0.2.x

The first 0.2.x API-boundary cleanup removes only implementation constructors and types; the recommended `TestLens` API is unchanged. If older code assembled `JsOverlayDebug` with `ApiOverlayPanel`, `ApiCallActions`, `Guards`, or logger bridge arguments, replace that construction with one of the two supported forms:

```java
JsOverlayDebug overlay = new JsOverlayDebug(driver);
JsOverlayDebug configured = new JsOverlayDebug(driver, overlayConfig);
```

Component injection through those longer constructors was never a supported extension point. The facade still provides the same documented overlay behavior. Public locator-result plumbing and local reporter/helper types that were not returned by recommended APIs are now implementation-private. Because this is a deliberate pre-1.0 binary break, consumers of those internals must remove those imports before upgrading to the 0.2.x line.

## Add Test Lens

Add the stable runtime to your Maven project:

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens</artifactId>
    <version>0.1.0</version>
</dependency>
```

Java imports remain under `io.github.testlens.*`. Keep Selenium as an explicit dependency, and keep your existing driver creation and shutdown code.

## Attach it to your existing driver

Create one `TestLens` instance for each driver and test invocation:

```java
TestLens lens = TestLens.attach(driver);
lens.startSession(testName);
```

This attaches Lens to the driver your project already created; it does not create another browser. Put Lens finalization into the test lifecycle you already use. See [Framework integration](framework-integration.md) for JUnit and TestNG examples.

## Migrate incrementally

A practical migration order is:

1. Add Test Lens and attach it to the existing driver.
2. Start and finalize the Lens session from the existing test lifecycle.
3. Move simple clicks, fills, key presses, and waits to Lens.
4. Move assertions that benefit from polling until a timeout.
5. Migrate collections, HTML selects, frames, windows, and dialogs where Lens already provides an operation.
6. Leave unsupported and low-level Selenium calls unchanged.

Named steps can group related operations and their outcome in Lens diagnostics:

```java
lens.step("Save order", () -> {
    lens.getByTestId("save-order").click();
});
```

They do not replace test-runner or reporter steps. See [Framework integration](framework-integration.md) for reporter coexistence.

Once the Lens lifecycle is integrated, an active session can also collect explicit evidence:

```java
lens.captureScreenshot("After save");
```

## Common migrations

These are typical replacements, not strict one-to-one rewrites. Depending on the operation, Lens can add retry-aware resolution, waits, actionability checks, or diagnostics around the underlying Selenium call.

### Interactions

Replace a direct click when Lens diagnostics are useful:

```java
driver.findElement(By.cssSelector("[data-testid='save']")).click();
```

```java
lens.locator(By.cssSelector("[data-testid='save']"), "Save").click();
```

`fill(String)` resolves the field, clears its current value, and sends the new text using Lens locator behavior and diagnostics:

```java
driver.findElement(by).clear();
driver.findElement(by).sendKeys(value);
```

```java
lens.locator(by, label).fill(value);
```

Common keyboard and pointer interactions remain concise:

```java
lens.locator(search, "Search").press(Keys.ENTER);
lens.locator(menu, "Account menu").hover();
lens.locator(row, "Order row").doubleClick();
lens.locator(item, "Context item").rightClick();
```

### Assertions and waits

A raw assertion reads once:

```java
assertEquals("Saved",
        driver.findElement(By.cssSelector("[data-testid='toast']")).getText());
```

The Lens assertion polls until its configured timeout:

```java
lens.locator(By.cssSelector("[data-testid='toast']"), "Toast")
        .expect()
        .toHaveText("Saved");
```

Common explicit waits can move to locator operations:

```java
lens.locator(panel, "Results").waitUntilVisible();
lens.locator(spinner, "Loading spinner").waitUntilHidden();
lens.locator(submit, "Submit").waitUntilClickable().click();
```

### Collections and select controls

Lens provides equivalent collection operations, while indexed locators remain lazy until used:

```java
int count = lens.locator(rows, "Order rows").count();
lens.locator(rows, "Order rows").nth(index).click();
lens.locator(rows, "Order rows").first().click();
lens.locator(rows, "Order rows").last().click();
```

For a real HTML `<select>`, Selenium's `Select`:

```java
new Select(driver.findElement(country)).selectByVisibleText("Poland");
```

can become:

```java
lens.locator(country, "Country").selectByVisibleText("Poland");
```

For custom dropdown widgets, keep using their normal DOM interactions or raw Selenium.

### Frames, windows and dialogs

Lens wraps common browser-context operations:

```java
lens.switchToFrame(By.id("payment-frame"), "Payment frame");
lens.switchToDefaultContent();
lens.switchToWindow(handle, "Main window");
```

For a newly opened window or tab, take the handle snapshot before the opening action:

```java
Set<String> before = lens.windowHandles();
lens.getByTestId("open-receipt").click();
lens.switchToNewWindow(before, "Receipt");
```

Native alert, confirmation, and prompt dialogs use `TestLensAlert`:

```java
TestLensAlert dialog = lens.alert().waitUntilPresent();
String text = dialog.text();
dialog.accept();
```

Use `dismiss()` for a confirmation or `fill(String)` before accepting a prompt.

## Keep raw Selenium where it fits

Lens and raw Selenium can operate against the same driver. Keep Selenium for complex W3C action sequences, offset pointer movement, explicit `keyDown`/`keyUp` sequences, advanced multi-select flows, browser lifecycle management, CDP/BiDi features not wrapped by Lens, and application-specific operations.

Existing Page Objects do not need to move all at once. A Page Object can use Lens for supported interactions and direct WebDriver calls for everything else.

## Advanced facilities

Auth/session-state and passive network-diagnostics APIs are available through the lower-level `JsOverlayDebug` facade in 0.1.0. They are optional facilities, not required when migrating ordinary Selenium interactions.

See the [examples](examples.md) and [advanced/low-level API](reference/advanced-low-level.md) for the boundary between the normal `TestLens` facade and lower-level APIs.

## Next steps

- [Browse the complete API reference](reference/index.md)
- [Integrate with JUnit, TestNG, and existing reporters](framework-integration.md)
- [Review practical examples](examples.md)
