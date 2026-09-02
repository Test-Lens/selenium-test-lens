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
The HUD must show failure status and enough context to understand the mismatch without secrets.
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
