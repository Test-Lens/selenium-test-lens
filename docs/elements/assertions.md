# Element assertions

`UiExpect` is obtained from `TestLens.expect(...)` or `UiLocator.expect()`. Each assertion polls until it passes or times out, reports attempts and elapsed time, and returns `UiAssertionResult` on success. Failure throws `UiAssertionError` carrying the result.

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
UiExpect expect(By by)
UiExpect expect(By by, String label)
```

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiExpect expect()
UiExpect expect(UiAssertionOptions options)
```

The two `TestLens.expect` overloads create a locator from `By`; the label overload supplies the diagnostic name. `UiLocator.expect()` uses default assertion options, while `expect(options)` uses the supplied comparison/retry settings. Assertion polling re-reads the current element rather than caching a `WebElement`. Reporter events feed the session trace/log/HUD; assertions do not take screenshots automatically.

<!-- SCREENSHOT TODO: assets/screenshots/assertion-passed.png
Show a successful real UiExpect assertion with the target state visible.
The HUD must show the assertion name, target label, and passed status.
Feature documented: successful assertion feedback.
Suggested alt text: Test Lens HUD reporting a successful visible-element assertion.
-->

<!-- SCREENSHOT TODO: assets/screenshots/assertion-failed.png
Show a failed or timed-out real UiExpect assertion using synthetic expected/actual text.
The HUD must show failure status and enough context to understand the mismatch without secrets. Assertion expected/actual previews pass through the effective central `RedactionPolicy`; typed length-only and URL sanitization rules remain stricter and diagnostic rendering never performs another observation.
Feature documented: failed assertion diagnostics.
Suggested alt text: Test Lens HUD reporting a timed-out text assertion with expected and actual context.
-->

## Visibility and enabled state

<!-- API SIGNATURES: io.github.testlens.selenium.assertions.UiExpect -->
```java
UiAssertionResult toBeVisible()
UiAssertionResult toBeHidden()
UiAssertionResult toBeEnabled()
UiAssertionResult toBeDisabled()
```

`toBeVisible` requires a present, displayed element. Enabled/disabled assertions also require a present element. By default, a missing element is retryable: it may appear during polling, while a permanent absence ends as `TIMED_OUT` with `ELEMENT_NOT_FOUND`. With `failFastOnMissingElement(true)`, the first missing-element observation ends immediately as `FAILED` with `ELEMENT_NOT_FOUND`.

`toBeHidden` is intentionally different: a missing element satisfies the assertion on its first attempt, regardless of `failFastOnMissingElement`; its result uses `actualPreview="missing"`. A present but non-displayed element also passes. A stale element is not treated as missing: it remains retryable even when missing-element fail-fast is enabled, and a persistent stale condition times out with `STALE_ELEMENT`. Unrelated WebDriver failures end immediately as `FAILED`.

## Text

<!-- API SIGNATURES: io.github.testlens.selenium.assertions.UiExpect -->
```java
UiAssertionResult toHaveText(String expected)
UiAssertionResult toContainText(String expectedSubstring)
```

Reads `WebElement.getText()`. Exact/sub-string comparison follows whitespace, trimming, and case options. Result previews are truncated to `actualTextPreviewLimit`; the assertion input can still be written to trace/report data, so do not assert secrets as literal text.

Both text assertions require a present element and follow the missing/stale retry policy described above.

```java
lens.getByTestId("status").expect().toContainText("Saved");
```

## Value

<!-- API SIGNATURES: io.github.testlens.selenium.assertions.UiExpect -->
```java
UiAssertionResult toHaveValue(String expected)
UiAssertionResult toContainValue(String expectedSubstring)
```

Reads the element's `value` attribute and applies the same normalization rules. Value previews are redacted/limited by the implementation, but screenshots or application DOM may still expose sensitive input.

Both value assertions require a present element and follow the same missing/stale retry policy.

## Collections and attachment

<!-- API SIGNATURES: io.github.testlens.selenium.assertions.UiExpect -->
```java
UiAssertionResult toHaveCount(int expected)
UiAssertionResult toBeAttached()
UiAssertionResult toBeDetached()
```

`toHaveCount` evaluates the complete lazy query pipeline once per poll, including scoping, filters, semantic matching, and collection selection. Zero is a valid count. `toBeAttached` means that a fresh query returns at least one element; visibility, enabled state, and viewport position do not matter. `toBeDetached` means that the fresh query returns none, so replacing an element with another matching instance is still attached.

With missing-element fail-fast enabled, count zero immediately fails `toHaveCount(expected > 0)` and `toBeAttached`; `toHaveCount(0)` and `toBeDetached` still pass. A nonzero but mismatched count continues polling.

```java
lens.locator(By.cssSelector(".product-card"))
        .filterByAttribute("data-status", "available")
        .expect()
        .toHaveCount(3);
lens.getByRole("status").expect().toBeAttached();
```

## DOM attributes, classes, and CSS

<!-- API SIGNATURES: io.github.testlens.selenium.assertions.UiExpect -->
```java
UiAssertionResult toHaveAttribute(String attributeName, String expectedValue)
UiAssertionResult toHaveClass(String className)
UiAssertionResult toHaveCss(String propertyName, String expectedValue)
```

`toHaveAttribute` uses `getDomAttribute`, compares exactly and case-sensitively, and distinguishes a missing attribute from a present empty attribute. Text comparison options do not apply. Diagnostic output records the attribute name, presence, and value lengths—not raw values.

`toHaveClass` checks one complete, case-sensitive HTML class token. It never treats `button` as matching `button-primary`. `toHaveCss` uses `getCssValue` and compares the trimmed browser-computed result exactly; browsers can serialize equivalent colors, units, and URLs differently. CSS diagnostic previews are bounded and redact the contents of `url(...)`.

```java
lens.getByTestId("save").expect().toHaveAttribute("aria-busy", "false");
lens.getByTestId("save").expect().toHaveClass("ready");
lens.getByTestId("panel").expect().toHaveCss("display", "block");
```

## Selected and checked state

<!-- API SIGNATURES: io.github.testlens.selenium.assertions.UiExpect -->
```java
UiAssertionResult toBeSelected()
UiAssertionResult toBeChecked()
UiAssertionResult toBeUnchecked()
```

Selected and checked are intentionally separate. `toBeSelected` supports native `option` elements and explicit ARIA selected-state roles (`option`, `tab`, `row`, `gridcell`, `rowheader`, `columnheader`, and `treeitem`) with a valid `aria-selected`. It does not infer state from CSS classes or `data-*` attributes.

Checked assertions reuse the same semantic-control resolver as `check()`, `uncheck()`, and `isChecked()`: native checkbox/radio inputs, associated labels and their descendants, plus ARIA checkbox/radio/switch controls. Native state comes from `isSelected`; ARIA state comes from `aria-checked`. `mixed` satisfies neither checked nor unchecked. Unsupported or malformed states fail immediately with `UNSUPPORTED_ELEMENT_STATE`.

Assertions never click, run overlay recovery, or mutate `checked`, `aria-checked`, or the DOM:

```java
lens.getByRole("checkbox", "Terms").expect().toBeChecked();
lens.locator(By.cssSelector("option:checked")).expect().toBeSelected();
```

## Polling, fail-fast, and failure reasons

Each attempt performs one current observation: one complete collection snapshot or one freshly resolved element read. Missing elements and stale references are retryable unless the assertion's success contract or `failFastOnMissingElement` says otherwise. Invalid selectors, lost sessions, unsupported states, and other nontransient WebDriver errors fail on their first attempt and preserve the cause.

Assertion polling may emit `ASSERTION_RETRY`, but never the recovery `RETRY` trace event, never increments `RetrySummary`, and never marks the session flaky. New timeout reasons are `COUNT_MISMATCH`, `ATTRIBUTE_MISMATCH`, `CLASS_MISMATCH`, `CSS_MISMATCH`, `ELEMENT_NOT_SELECTED`, `ELEMENT_NOT_CHECKED`, `ELEMENT_STILL_CHECKED`, `ELEMENT_NOT_ATTACHED`, and `ELEMENT_STILL_ATTACHED`; invalid state uses `UNSUPPORTED_ELEMENT_STATE`.

## Result and failure

Successful assertions return a `UiAssertionResult`. Its consumer accessors are:

<!-- API SIGNATURES: io.github.testlens.selenium.assertions.UiAssertionResult -->
```java
String assertionName()
UiAssertionStatus status()
UiAssertionFailureReason failureReason()
String locatorDescription()
String expectedPreview()
String actualPreview()
int attempts()
Duration elapsed()
String message()
boolean isPassed()
String summary()
```

The public `passed(...)`, `failed(...)`, and `timedOut(...)` factories are intended for reporter/integration code. `new UiAssertionError(UiAssertionResult)` creates the failure thrown by the fluent assertions; `result()` retrieves it.

## Advanced constructors

Normal consumers obtain `UiExpect` from a locator/facade. These public constructors expose reporter/probe seams and are internal-style integration hooks:

<!-- API SIGNATURES: io.github.testlens.selenium.assertions.UiExpect -->
```java
UiExpect(UiLocator locator, UiAssertionOptions options, OverlayLogger logger)
UiExpect(UiLocator locator, UiAssertionOptions options, OverlayLogger logger, UiExpect.VisibilityProbe visibilityProbe)
UiExpect(UiLocator locator, UiAssertionOptions options, OverlayLogger logger, UiExpect.VisibilityProbe visibilityProbe, UiExpect.ElementProbe elementProbe)
```

The probe types themselves are classified `INTERNAL_STYLE_PUBLIC`; do not use them as the normal assertion API.

## Page URL and title assertions

`TestLens.expectPage()` and `JsOverlayDebug.expectPage()` create a page assertion bound to the driver's active window. An overload accepts `UiAssertionOptions`; only timeout and poll interval affect URL assertions, while title assertions also honor whitespace normalization, trimming, case sensitivity, and the preview limit.

<!-- API SIGNATURES: io.github.testlens.selenium.assertions.UiPageExpect -->
```java
UiPageExpect(WebDriver driver, UiAssertionOptions options, OverlayLogger logger)
UiAssertionResult toHaveUrl(String expected)
UiAssertionResult toContainUrl(String expectedSubstring)
UiAssertionResult toHaveTitle(String expected)
UiAssertionResult toContainTitle(String expectedSubstring)
```

```java
lens.expectPage().toContainUrl("/checkout");
lens.expectPage().toHaveTitle("Checkout");
```

`toHaveUrl` uses case-sensitive equality on the complete raw value returned by `WebDriver.getCurrentUrl()`. `toContainUrl` uses case-sensitive `String.contains`. Neither assertion normalizes slashes, host, port, encoding, query, fragment, or parameter ordering. `toHaveTitle` and `toContainTitle` use one `WebDriver.getTitle()` observation per poll and the normal text options.

Each poll performs exactly one driver read and comparison. Mismatches use `URL_MISMATCH` or `TITLE_MISMATCH`; assertion polling emits `ASSERTION_RETRY` but never a recovery `RETRY` and never marks the session flaky. `failFastOnMissingElement` is irrelevant because a page assertion has no element. Driver failures such as a closed window, lost session, active alert, or unreachable browser fail immediately and remain the cause of `UiAssertionError`.

Matching uses the full URL, but diagnostics retain only a bounded scheme/host/port/path preview. Userinfo, query, and fragment are removed; an unparseable URL is represented only as `url[length=N]`. The expected substring is represented only by its length. Assertions never read page source, execute JavaScript, use network capture, or switch windows or frames.
