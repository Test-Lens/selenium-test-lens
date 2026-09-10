# Element waiting and retry

Waiting occurs at two levels: operation retry and explicit condition waits. A recovery retry exists only after a physical operation attempt fails with a configured retryable exception and the library schedules another attempt. Expected-condition checks, resolver DOM reads, missing-element waits, and unsatisfied assertions are polling, not recovery retries.

Collection-size waits are available directly on a composed locator: `waitUntilCount`, `waitUntilCountAtLeast`, and `waitUntilCountAtMost`. Each poll evaluates exactly one fresh query snapshot using `UiLocatorOptions.timeout()` and `pollInterval()`. This is ordinary polling, not recovery retry, so it does not affect `RetrySummary` or flaky-candidate policy. See [Element collections](collections.md#count-waits).

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

Related: [Assertions](assertions.md), [`UiLocatorOptions`](../reference/configuration.md#uilocatoroptions), [`click()`](actions.md#click-contract-native-activation-visible-recovery).

## Retryable failures

`StaleElementReferenceException`, `ElementClickInterceptedException`, and `ElementNotInteractableException` are retried only when their corresponding flags are enabled. Wrapped failures are classified by their exception cause chain, never by message text. Other WebDriver exceptions fail immediately. The historical name `maxRetries` is retained for compatibility, but its value is the maximum number of physical **attempts**, not retries: three failed attempts schedule only two retries. Poll/timeout durations must be positive.

Runner-level retry is separate again: for example, every TestNG `IRetryAnalyzer` attempt owns a new Lens session and is not aggregated across sessions. See [Flakiness and retry outcomes](../observability/flakiness.md).

Assertions have their own [`UiAssertionOptions`](../reference/configuration.md#uiassertionoptions) and retry loop; see [Assertions](assertions.md).

State assertions such as `toHaveCount`, `toHaveAttribute`, `toBeChecked`, and `toBeAttached` use that assertion loop and take exactly one current observation per attempt. Their `ASSERTION_RETRY` events remain polling diagnostics and do not contribute to recovery-retry flakiness.

Page assertions created by `expectPage()` follow the same distinction: each attempt performs one `getCurrentUrl()` or `getTitle()` on the active window. A mismatch is assertion polling, not recovery retry; the assertion never wraps a second `WebDriverWait` or changes the active window.

## Page and JavaScript waits

!!! info "Coming in 0.2.0"
    The `TestLens` page-wait facade is part of the current development line and is not available in Maven Central `0.1.0`. The lower-level `PageWaits` and corresponding `JsOverlayDebug` methods existed in 0.1.0.

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
void waitForPageReady()
void waitForPageReady(Duration timeout)
void waitForInteractiveOrComplete()
void waitForInteractiveOrComplete(Duration timeout)
void waitForNetworkIdle()
void waitForNetworkIdle(Duration idleDuration, Duration timeout)
```

The no-argument facade methods use `TestLensOptions.locatorOptions().timeout()` and `pollInterval()`. An explicit timeout takes precedence. Timeout and idle duration must not be negative, and the configured polling interval must be positive. Every operation owns one monotonic total deadline; a timeout always throws Selenium `TimeoutException`. Terminal WebDriver or JavaScript failures stop immediately with the original failure preserved.

`waitForPageReady` polls one `return document.readyState` observation at a time and accepts only `complete`. `waitForInteractiveOrComplete` accepts only `interactive` or `complete`. Neither method promises that an SPA has rendered its data. With Selenium's usual `PageLoadStrategy.NORMAL`, `get()` normally already waits for classic document loading; explicit readiness waits are mainly useful with `EAGER`, `NONE`, asynchronous navigation, or when re-observing the active document.

`waitForNetworkIdle` installs an idempotent in-page tracker and requires zero observed active XHR/fetch operations for the complete idle window. The tracker balances successful, rejected, aborted, and synchronously failed calls, and is installed again in a new document after navigation. It sees **only** XHR/fetch started after installation. It does not guarantee visibility into earlier requests, images, CSS, scripts, WebSocket, EventSource, beacon, or full browser traffic. This is a bounded SPA heuristic, not browser network idle. Use [WebDriver BiDi network diagnostics](../advanced/network.md) when passive browser-level evidence is required.

The existing React/SPA combinations retain their public API but now share one deadline across document, network, root, component, and DOM-stability stages. Wait polling emits one `WAIT/STARTED` and one terminal `WAIT/PASSED` or `WAIT/FAILED`; it does not emit recovery retries, affect `RetrySummary`, or mark the session flaky. HUD state is removed in `finally`, and diagnostic DOM text is inserted with text nodes rather than unescaped HTML.
