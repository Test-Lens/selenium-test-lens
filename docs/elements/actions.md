# Element actions

Element actions use native Selenium operations and feed the same HUD, log, and trace pipeline as waits and assertions. The basic click, fill, clear, key, hover, and click-variant actions are available in `0.1.0`.

!!! info "Coming in 0.2.0"
    Form-control actions (`check`, `uncheck`, `upload`, `focus`, and `scrollIntoView`) and composed locator execution are part of the current development line and are not available in Maven Central `0.1.0`.

Actions resolve the current element and return the same `UiLocator` for chaining. Resolution/action retry is governed by [`UiLocatorOptions`](../reference/configuration.md#uilocatoroptions). A physical action/read failure that schedules another attempt emits a dedicated recovery `RETRY`; a terminal failure does not. Condition polling is separate and does not create recovery-retry evidence. A final failure is wrapped as `UiLocatorException`, with the underlying WebDriver failure retained as its cause.

## Click contract: native activation, visible recovery

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator click()
```

`UiLocator.click()` is the recommended public click API. There is no separate `smartClick()` that normal users need to select.

Each activation attempt uses native `WebElement.click()`. An intercepted click may be followed by another native click after explicit overlay recovery, and the locator retry policy may start a fresh action attempt.

The complete contract is:

- the locator resolves the target and runs best-effort actionability diagnostics;
- an enabled overlay may highlight the target, but the decoration is pointer-transparent and never activates it;
- physical activation is `WebElement.click()`;
- a configured overlay policy may identify and explicitly handle a blocker after `ElementClickInterceptedException`, then make another native click attempt;
- configured stale, intercepted, or not-interactable failures may cause the outer locator retry policy to begin another complete action attempt;
- start, retry, pass, and failure information is emitted to the HUD/log/trace pipeline.

The implementation deliberately does **not** fall back to JavaScript click, Selenium `Actions` click, clicking an ancestor, or mutating page state to simulate activation. Actionability is diagnostic and best-effort; it cannot guarantee that the browser will accept the next click.

### Returns

The same locator.

### Failure behavior

Throws `UiLocatorException` after the retry budget or on a non-retryable WebDriver failure. Overlay policy can also fail the click when a required blocker cannot be handled.

### Trace, HUD, highlight, and evidence

The locator and overlay-aware click layers emit structured start/pass/retry/failure events; an attached session records them and the HUD can display them. When overlays are enabled, the target is decorated using its diagnostic label. `click()` does not capture a screenshot by itself.

```java
lens.getByRole("button", "Save").waitUntilClickable().click();
```

Related: [`doubleClick()`](#doubleclick), [`rightClick()`](#rightclick), [waiting](waiting.md).

## fill(String value)

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator fill(String value)
```

Purpose: replace the current element value using Selenium keyboard input.

- Parameter `value`: text to send after clearing; `null` means clear without sending keys.
- Returns: the same `UiLocator` for chaining.
- Resolution/retry: resolves afresh on each configured attempt and retries only configured transient failures.
- Selenium operations: `WebElement.clear()`, then `sendKeys(value)` for non-null input.
- Diagnostics: runs best-effort actionability diagnostics and emits HUD/log/trace start/pass/retry/failure events. Metadata records input length rather than the literal value.
- Highlight/evidence: does not add a target highlight or capture a screenshot in the current implementation.
- Failure: throws `UiLocatorException` when a non-retryable failure occurs or attempts are exhausted.

```java
lens.getByTestId("email").fill("person@example.test");
```

Related: [`clear()`](#clear), [`press(...)`](#presscharsequence-keys), [value assertions](assertions.md#value).

## Checked controls

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator check()
UiLocator uncheck()
boolean isChecked()
```

`check()` and `uncheck()` are idempotent: they resolve and read the current state first, then perform at most one native `WebElement.click()` activation when a change is required. Native checkbox inputs, native radios, ARIA checkboxes, ARIA switches, and ARIA radios are recognized from their standard HTML/ARIA semantics. Radios support `check()` and `isChecked()` but deliberately reject `uncheck()`.

For a native input hidden behind standard form styling, the locator may point to the input, its associated `label`, or a descendant of that label. Test Lens resolves the state-bearing input through `label.control`/`input.labels` semantics and clicks the visible label; it does not guess from CSS classes, text, or `data-*` attributes. Custom controls must use `role="checkbox"`, `role="switch"`, or `role="radio"` with a valid `aria-checked` value. Disabled controls fail without activation.

After one activation, Test Lens polls only the freshly resolved control state until the locator timeout. This supports asynchronous rerenders without a second click. Confirmation polling is not a recovery retry and does not mark the session flaky. A mixed/indeterminate state makes `isChecked()` return `false`; `check()` or `uncheck()` still performs at most one activation and requires the requested final state.

```java
lens.getByLabel("Accept terms").check();
lens.locator(By.id("newsletter"), "Newsletter").uncheck();
boolean selected = lens.locator(By.id("plan"), "Plan").isChecked();
```

Use `expect().toBeChecked()` or `expect().toBeUnchecked()` when state may settle asynchronously. These assertions share the semantic control resolver but are read-only: they never activate the control, close an overlay, or increment click/change counters.

## upload(Path... files)

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator upload(Path... files)
```

Uploads one or more existing regular files to an `input[type=file]`, including a hidden input. Multiple paths require the HTML `multiple` attribute. All paths are validated and normalized before WebDriver is called; then Selenium receives exactly one newline-separated `sendKeys(...)` invocation. The input is never clicked and JavaScript never assigns its files. Once `sendKeys` starts, its failure is terminal because repeating an ambiguous upload could duplicate the operation.

Diagnostics record only `fileCount`. Local paths and file names are not included in Test Lens action messages or metadata. Page content and screenshots remain application-controlled evidence, so protect artifacts according to the normal evidence guidance.

```java
lens.getByLabel("Attachment").upload(Path.of("document.pdf"));
```

## focus() and scrollIntoView()

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator focus()
UiLocator scrollIntoView()
```

`focus()` makes one `JavascriptExecutor.executeScript(...)` call and focuses the current element with `preventScroll` when supported. `scrollIntoView()` makes one script call using centered block alignment, nearest inline alignment, and instant behavior. Neither method clicks, sends keys, invokes Selenium `Actions`, or provides a click fallback. A stale element may be resolved again under the locator retry policy; another JavaScript failure is terminal.

```java
lens.locator(By.id("search"), "Search").focus();
lens.locator(By.id("summary"), "Summary").scrollIntoView();
```

## clear()

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator clear()
```

Runs best-effort actionability diagnostics and calls `WebElement.clear()`. Returns the same locator and uses common retry/trace/HUD behavior. It does not add a target highlight.

## pressEnter()

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator pressEnter()
```

Equivalent to `press(Keys.ENTER)`. It calls `WebElement.sendKeys` after resolution and best-effort actionability diagnostics and returns the same locator.

## press(CharSequence... keys)

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator press(CharSequence... keys)
```

Sends the supplied Selenium key/text sequence to the element after best-effort actionability diagnostics. A null array becomes an empty sequence. For held keys, offsets, or composite W3C actions use Selenium `Actions` directly.

## hover()

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator hover()
```

Resolves the element and performs `new Actions(driver).moveToElement(element).perform()`. It returns the same locator and emits operation feedback. Unlike click-like actions, it does not retain an actionability report.

## doubleClick()

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator doubleClick()
```

Runs best-effort actionability diagnostics and performs Selenium `Actions.doubleClick(element)`. Returns the same locator and follows common failure/retry/observability behavior. It does not add a highlight.

## rightClick()

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator rightClick()
```

Runs best-effort actionability diagnostics and performs Selenium `Actions.contextClick(element)`. Returns the same locator and does not add a highlight. This opens the page/browser context menu only as allowed by the application and browser.

## checkActionability() overloads

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
ActionabilityReport checkActionability()
ActionabilityReport checkActionability(ActionabilityOptions options)
```

Runs diagnostics without performing an element action. The no-argument overload uses the locator's configured actionability options. Collection views resolve their current indexed element; ordinary locators use the underlying `By`. Returns a report rather than throwing merely because the report is not ready, but driver/script failures can propagate. See [Actionability configuration](../reference/configuration.md#actionabilityoptions).
