# Overlay policies

Package: `io.github.testlens.selenium.overlay`<br>
API level: **Advanced**

`OverlayPolicy.builder().handler(...)` defines ordered application-specific blocker handlers. Each `OverlayHandler` has a non-blank name, detection `By`, one or more actions, optional/required semantics, positive timeout, and `failIfStillVisible` policy.

The supported action factories are:

<!-- API SIGNATURES: io.github.testlens.selenium.overlay.OverlayAction -->
```java
static OverlayAction click(By target)
static OverlayAction pressEscape()
static OverlayAction waitUntilGone(By target)
static OverlayAction fail(String message)
```

Their corresponding `OverlayActionType` values are `CLICK`, `PRESS_ESCAPE`, `WAIT_UNTIL_GONE`, and `FAIL`. There is no public JavaScript action. Use raw Selenium `JavascriptExecutor` outside the policy when an application-specific blocker genuinely requires script execution.

The policy executor runs around overlay-aware actions and returns `OverlayHandlingResult`/status. Required handlers and visibility validation can fail the consumer action. Selectors and configured actions are trusted test code and may mutate the page; keep the policy narrow and deterministic.

`OverlayHandlingStatus` distinguishes `NOT_DETECTED`, `HANDLED`, `STILL_VISIBLE`, `FAILED`, and `SKIPPED`. Inspect it together with the handler name, message, action results, and exception exposed by `OverlayHandlingResult`.

Use raw Selenium when a blocker needs an unsupported workflow. Exact factories/builders/results are in the [catalog](../reference/public-api-catalog.md).
