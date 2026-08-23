# Element waiting and retry

Waiting occurs at two levels: operation retry and explicit condition waits. Both use `UiLocatorOptions.timeout()` and `pollInterval()`; operation retries additionally honor `maxRetries()` and the three retry flags.

<!-- SCREENSHOT TODO: assets/screenshots/wait-feedback-active.png
Show an explicit UiLocator wait while its condition is still unsatisfied.
The HUD must display the wait description/retry state and the application must show why it is waiting.
Use a deterministic demo state and omit unrelated browser chrome.
Feature documented: active wait and retry feedback.
Suggested alt text: Test Lens HUD showing an active wait retry for an unavailable element.
-->

## waitUntilVisible()

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator waitUntilVisible()
```

Polls Selenium's visibility condition until success/timeout, emits wait feedback, and returns the same locator. Timeout/fatal WebDriver failures become `UiLocatorException`.

```java
lens.getByTestId("dialog").waitUntilVisible();
```

## waitUntilHidden()

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator waitUntilHidden()
```

Polls invisibility; an absent element satisfies the condition. Returns the same locator.

## waitUntilClickable()

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator waitUntilClickable()
```

Waits for Selenium's clickable condition (visible and enabled). This is not proof that no overlay will intercept the later click; actionability/overlay handling still runs in `click()`.

```java
lens.getByRole("button", "Save").waitUntilClickable().click();
```

## waitUntilText(String expectedText)

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator waitUntilText(String expectedText)
```

Waits until `WebElement.getText().contains(expectedText)` using the configured polling interval and timeout, then returns the same locator. Matching is case-sensitive and is a substring check. A null expectation causes a failure rather than matching empty text.

All four waits resolve against the current DOM on every poll, ignore missing/stale elements while polling, emit wait and retry events to the attached log/trace/HUD pipeline, and do not capture evidence automatically. They return the same locator. Timeout or a fatal condition error is wrapped in `UiLocatorException` with elapsed context.

Related: [Assertions](assertions.md), [`UiLocatorOptions`](../reference/configuration.md#uilocatoroptions), [`click()`](actions.md#click).

## Retryable failures

`StaleElementReferenceException`, `ElementClickInterceptedException`, and `ElementNotInteractableException` are retried only when their corresponding flags are enabled. Other WebDriver exceptions fail immediately. `maxRetries` counts attempts and must be at least 1. Poll/timeout durations must be positive.

Assertions have their own [`UiAssertionOptions`](../reference/configuration.md#uiassertionoptions) and retry loop; see [Assertions](assertions.md).
