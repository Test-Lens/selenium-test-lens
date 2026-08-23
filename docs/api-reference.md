# API guide

This guide covers the public Selenium Test Lens API most users need in 0.1.0. For complete signatures, see the [published Javadoc](https://javadoc.io/doc/io.github.test-lens/selenium-test-lens/0.1.0/).

Java packages use `io.github.testlens.*`; the Maven group is `io.github.test-lens`.

## TestLens facade

`TestLens.attach(existingDriver)` is the recommended, runner-agnostic entry point. Lens does not create, replace, or close the driver.

```java
TestLens lens = TestLens.attach(driver);
lens.startSession("checkout");

lens.locator(By.id("save"), "Save").click();
lens.finishPassed();
```

The facade provides locators, assertions, steps, screenshots, frame and window operations, alerts, and session finalization. `startSession(String)` creates and returns a `UiTestLensSession` and makes it the active session. `finishPassed()` and `finishFailed(Throwable)` return a `TestLensFinalizationResult`. Best-effort diagnostic and export failures are collected in `diagnosticFailures()` instead of being thrown by finalization.

## Locators and actions

Create a locator with `locator(By)`, `locator(By, String)`, `getByTestId(String)`, `getByText(String)`, `getByTextContaining(String)`, or `getByRole(String, String)`.

```java
lens.locator(By.id("email"), "Email").fill("test@example.com");
lens.getByTestId("save").click();
lens.locator(By.id("search"), "Search").press(Keys.ENTER);
```

`UiLocator` actions remain chainable:

| Operation | Return value |
| --- | --- |
| `click()`, `fill(String)`, `clear()` | the same `UiLocator` |
| `pressEnter()`, `press(CharSequence...)` | the same `UiLocator` |
| `hover()`, `doubleClick()`, `rightClick()` | the same `UiLocator` |
| `resolve()` | `WebElement` |

Reads include `textContent()` (`String`), `isVisible()` and `isEnabled()` (`boolean`), and `attribute(String)`, `property(String)`, and `value()` (`String`, potentially `null` when Selenium has no value to return).

!!! note "Role matching scope"

    `getByRole` matches explicit or supported implicit roles and compares the requested name with `aria-label` or normalized element text. It is not a complete implementation of the ARIA accessible-name algorithm.

## Waits and assertions

Locator waits poll until the configured timeout. Their timeout and polling interval come from `UiLocatorOptions`:

```java
lens.locator(By.id("spinner"), "Loading spinner").waitUntilHidden();
lens.locator(By.id("save"), "Save").waitUntilClickable().click();
lens.locator(By.id("status"), "Status").waitUntilText("Ready");
```

`waitUntilVisible()`, `waitUntilHidden()`, `waitUntilClickable()`, and `waitUntilText(String)` all return the same `UiLocator`.

Retryable assertions are available from `UiLocator.expect()` or `TestLens.expect(By, label)`:

```java
lens.locator(By.id("toast"), "Confirmation")
        .expect()
        .toContainText("Saved");
```

`UiExpect` supports `toBeVisible()`, `toBeHidden()`, `toBeEnabled()`, `toBeDisabled()`, `toHaveText(String)`, `toContainText(String)`, `toHaveValue(String)`, and `toContainValue(String)`. An element that is absent or present but not displayed satisfies `toBeHidden()`. Other WebDriver failures cause the assertion to fail rather than being treated as hidden.

## Collections and select controls

`resolveAll()` and `count()` inspect the current DOM. `nth(int)`, `first()`, and `last()` return lazy `UiLocator` values that resolve when used:

```java
UiLocator rows = lens.locator(By.cssSelector("table tbody tr"), "Order rows");
int count = rows.count();
rows.first().click();
rows.nth(2).click();
rows.last().click();
List<WebElement> elements = rows.resolveAll();
```

`nth(int)` is zero-based. HTML `<select>` controls support:

```java
UiLocator country = lens.locator(By.id("country"), "Country");
country.selectByVisibleText("Poland");
country.selectByValue("PL");
country.selectByIndex(1);

String text = country.selectedText();
String value = country.selectedValue();
```

These methods use Selenium's `Select` and therefore require a real HTML `<select>`. For custom dropdown widgets, use the component's normal DOM interactions or raw Selenium. React-specific helpers are available separately when they are relevant to the application.

## Frames and windows

```java
lens.switchToFrame(By.id("payment-frame"), "Payment frame");
lens.locator(By.id("pay"), "Pay").click();
lens.switchToParentFrame();
lens.switchToDefaultContent();
```

Frames can be selected with `switchToFrame(By, String)`, `switchToFrame(UiLocator)`, or `switchToFrame(int, String)`. For a new window or tab, snapshot the handles before the opening action:

```java
Set<String> before = lens.windowHandles();
lens.locator(By.id("open-receipt"), "Open receipt").click();
lens.switchToNewWindow(before, "Receipt");
```

`waitForNewWindow(Set<String>)` waits until at least one new handle is observed, then requires exactly one and returns it. No new handle results in a timeout; multiple new handles result in `NoSuchWindowException`. `currentWindowHandle()`, `windowHandles()`, and `switchToWindow(String, String)` are also available.

## Browser dialogs

Native alert, confirm, and prompt dialogs use `TestLensAlert`:

```java
TestLensAlert alert = lens.alert().waitUntilPresent();
String message = alert.text();
alert.accept();
```

Call `dismiss()` for a confirmation dialog or `fill(String)` before accepting a prompt. The prompt value is sent to Selenium but omitted from diagnostic messages; diagnostics record only its length.

## Trace, reports and evidence

High-level finalization attempts to write `trace.json` and `report.html` in a session-specific directory beneath `target/ui-test-lens` by default. Either export can fail independently, so the corresponding `jsonReport()` or `htmlReport()` value can be `null`. `outputDirectory()` and `failureScreenshot()` are also nullable `Path` values, and `diagnosticFailures()` contains failures encountered during best-effort finalization.

```java
UiTestLensSession session = lens.startSession("checkout");
lens.captureScreenshot("After save");
TestLensFinalizationResult result = lens.finishPassed();

if (result.htmlReport() != null) {
    System.out.println("HTML report: " + result.htmlReport());
}
if (!result.diagnosticFailures().isEmpty()) {
    result.diagnosticFailures().forEach(Throwable::printStackTrace);
}
```

For explicit or combined exports, the core API provides `TraceHtmlExporter`, `TraceJsonExporter`, and `TraceReportBundleExporter`:

```java
new TraceHtmlExporter().exportSuiteToDefault(List.of(session));
new TraceJsonExporter().exportSuiteToDefault(List.of(session));
new TraceReportBundleExporter().exportSuiteToDefault(List.of(session));
```

Suite defaults are `target/ui-test-lens-report/index.html`, `target/ui-test-lens-report/report.json`, and `target/ui-test-lens-report/ui-test-lens-report.zip`. Video evidence attaches a file or URL reference; Test Lens does not record video. See the [examples](examples.md) for reporting workflows.

## Logging

Core logging uses structured `UiTestLensLogEntry` values and `UiTestLensLogSink` implementations. `InMemoryLogSink` can export a log-only report:

```java
InMemoryLogSink logs = new InMemoryLogSink();
logs.accept(UiTestLensLogEntry.info("Opening checkout"));
logs.exportHtmlReport();
```

`UiTestLensLogEntry.info(String)` is a public factory in 0.1.0. Normal facade operations already feed the attached session, so ordinary tests do not need a separate logger.

## Low-level and raw Selenium APIs

`JsOverlayDebug` remains public for advanced facilities not exposed by `TestLens`, including the auth-state and passive network-diagnostics entry points. It is a lower-level facade, not the recommended entry point for normal tests.

Continue using raw Selenium for complex action sequences, offset pointer movement, explicit `keyDown`/`keyUp` sequences, advanced multi-select behavior, low-level W3C actions, or CDP/BiDi features not wrapped by Lens. Lens and raw Selenium can be mixed against the same driver.

**Next:** [framework integration](framework-integration.md) · [examples](examples.md) · [migration guide](migration.md)
