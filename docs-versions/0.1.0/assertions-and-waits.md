# Assertions and waits

**Selenium Test Lens 0.1.0 — stable**

## Element waits and assertions

```java
lens.getByTestId("toast")
    .waitUntilVisible()
    .expect()
    .toContainText("Saved");
```

`UiLocator` provides `waitUntilVisible()`, `waitUntilHidden()`, `waitUntilClickable()`, and `waitUntilText(...)`. `UiExpect` provides visibility, enabled/disabled, text, and value assertions. Assertions poll according to `UiAssertionOptions` and throw `UiAssertionError` when they fail or time out.

```java
UiAssertionOptions assertionOptions = UiAssertionOptions.builder()
    .timeout(Duration.ofSeconds(3))
    .pollInterval(Duration.ofMillis(100))
    .build();

lens.getByTestId("status").expect(assertionOptions).toHaveText("Ready");
```

## Page and SPA waits

In 0.1.0 these methods live on `JsOverlayDebug`, not on `TestLens`:

```java
JsOverlayDebug overlay = new JsOverlayDebug(driver);
overlay.waitForPageReady(Duration.ofSeconds(5));
overlay.waitForInteractiveOrComplete(Duration.ofSeconds(5));
```

`waitForPageReady` polls `document.readyState` for `complete`; `waitForInteractiveOrComplete` accepts either state. React-root and MutationObserver-based SPA helpers are also available on `JsOverlayDebug`.

`waitForNetworkIdle(...)` is a page-injected XHR/fetch heuristic, not full browser network observation. It sees only requests started after tracker installation and does not cover images, CSS, scripts, WebSocket, EventSource, or browser-level traffic. In 0.1.0 a network-idle timeout was recorded in the wait diagnostic but was not guaranteed to throw; do not use its normal return as a hard assertion. Use an explicit application condition or network event assertion when failure must stop the test.
