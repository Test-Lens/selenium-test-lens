# Element actions

**Selenium Test Lens 0.1.0 — stable**

`UiLocator` combines resolution, trace/HUD reporting, actionability diagnostics, and selected retries.

```java
lens.getByTestId("email").fill("person@example.test");
lens.getByTestId("search").pressEnter();
lens.locator(By.id("country")).selectByVisibleText("Poland");
lens.getByTestId("save").click();
```

Available 0.1.0 actions include `click`, `fill`, `clear`, `press`, HTML `select` operations, `hover`, `doubleClick`, and `rightClick`. Reads include text, visibility, enabled state, attributes, DOM properties, value, and selected option values.

## Click contract in 0.1.0

`UiLocator.click()` performs best-effort actionability diagnostics and delegates to the 0.1.0 overlay-aware click helper. That helper can highlight, invoke native Selenium element clicking, and apply the configured overlay policy when an intercepted click is detected. Locator recovery can then retry selected intercepted, stale, or not-interactable failures.

Do not infer the stricter interaction guarantees documented for later development versions. In particular, actionability is diagnostic, retries may repeat an action attempt, and 0.1.0 includes separate low-level helpers whose behavior is not identical to the current facade contract.

Highlighting is visual decoration. Disable it through `UiLocatorOptions.highlightBeforeAction(false)` when needed.
