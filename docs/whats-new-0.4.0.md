# Test Lens 0.4.0

Test Lens for Selenium 0.4.0 makes browser activity easier to observe without taking ownership away from Selenium or the test runner. The release connects ordinary Selenium calls to Lens evidence, separates browser execution from presentation, and gives actions, waits, retries, and assertions consistent semantic lifecycles.

![Semantic action and assertion lifecycle in the 0.4.0 HUD](assets/media/hud-action-assertion-lifecycle.webp)

## New

### Native Selenium observation

`TestLens.observeDriver(driver)` returns an explicit observed view of an existing driver. Calls such as `get`, `findElement`, `click`, `clear`, and `sendKeys` remain ordinary Selenium calls and execute exactly once; Lens adds correlated HUD, trace, report, source-navigation, duration, and safe target metadata. Selective `observe(element)` and `observe(element, label)` views are also available.

Native observation does not substitute Smart Click, wait, retry, re-resolution, or JavaScript fallback. Test Lens temporarily hides its overlay where a native click requires it, and sensitive `sendKeys` and script values are not copied into diagnostics. [Use native observation](getting-started.md#observe-ordinary-selenium-calls).

### TestNG `PER_CLASS` driver scope

The TestNG adapter can reuse one adapter-owned driver for sequential methods of a concrete class instance. Every method, DataProvider row, invocation, and retry still receives a fresh Lens session and report. Unsafe parallel-method, parallel-DataProvider, and `invocationCount`/`threadPoolSize` combinations fail before driver creation; there is no adaptive worker sizing. [Configure TestNG](integrations/testng.md#driver-scope).

### Headless execution configuration

`HeadlessMode` is `TRUE`, `FALSE`, or `UNSET`. For adapted factories, explicit Java configuration wins over `testLens.headless`, which wins over `TEST_LENS_HEADLESS`; `UNSET` preserves the factory's existing behavior. Test Lens maps enabled mode to Chrome `--headless=new` and Firefox `-headless` when it creates the adapted options. Attaching an arbitrary or already-created driver cannot change how that browser was launched.

### FAST observability

`ObservabilityMode.FAST` is independent of headless execution. It keeps Selenium, Smart Click, waits, retries, correlation, structured events, bounded recorder data, and failure evidence, while disabling the live HUD, automatic highlights, and live Source Navigation by default. Successful sessions retain their summary by default. Configure it through Java, `testLens.observability=fast`, or `TEST_LENS_OBSERVABILITY=fast`. [Compare DEFAULT and FAST](configuration.md#browser-execution-and-observability).

### Bounded recorder and locator observations

The trace recorder retains bounded event counts and estimated bytes, exposes completeness telemetry, and can keep only the summary of a passed session. Failures retain bounded context and reuse central redaction and screenshot evidence. Runtime locator observations add versioned, bounded selector/context data without extra WebDriver commands.

## Improved

- **Semantic HUD:** categories and phases are independent of logging severity. `ACTION`, `ASSERTION`, `LOCATOR`, `ACTIONABILITY`, `HIGHLIGHT`, `USER`, and `SYSTEM` rows carry `RUNNING`, `PASSED`, `RETRYING`, `FAILED`, `WARNING`, `INFO`, or `DEBUG` meaning. An operation ID updates one logical row instead of creating misleading start/result duplicates.
- **Highlights:** `ACTION`, `WAITING`, `RETRY`, `SUCCESS`, and `FAILURE` have one operation-owned lifecycle and configurable durations. Late technical callbacks cannot overwrite terminal success or failure.
- **Smart Click:** `NATIVE → ACTIONS → POINT → JS` shares one deadline and one logical lifecycle. Physical fallbacks require a target-owned hit test; JS is used only when enabled; ambiguous delivery is not followed by a blind second click.
- **BiDi and remote sessions:** Selenium 4.39 lazy `HasBiDi` connections and capability-backed `RemoteWebDriver` augmentation now fail gracefully with structured diagnostics. `AUTO` never falls back to performance logs, and providers still need their own BiDi capabilities.
- **Source Navigation:** F8 toggles actionable links and Escape turns them off. IntelliJ preflight checks project mapping and protocol availability before presenting a link.
- **Full-page evidence:** scroll-and-stitch follows bounded document-height changes while preserving viewport, width, DPR, context, pixel, tile, redaction, and restoration safeguards.

## Fixed

- Assertion start, progress, result, and failure no longer collapse into generic informational HUD rows.
- Warm semantic HUD updates are batched without dropping events; warnings, retries, failures, and user messages remain immediate.
- Terminal highlights cannot be repainted by stale resolve, actionability, or Smart Click callbacks.
- Covered, stale, and rerendered click targets follow the documented shared-deadline and locator-retry boundaries.
- TestNG retry and DataProvider invocations retain separate logical sessions and reports, including with healthy `PER_CLASS` driver reuse.
- Remote BiDi startup distinguishes unsupported, endpoint, session, protocol, and capture-start failures.

These are the user-visible parts of the complete [0.4.0 changelog](https://github.com/Test-Lens/selenium-test-lens/blob/main/CHANGELOG.md).

## Behavior changes

- `UiLocator.click()` now uses the bounded Smart Click cascade. Projects that previously expected only `WebElement.click()` should review `javascriptClickFallback` and the exactly-once ambiguity rules.
- Successful action/assertion rows update in place by operation ID. Row counts can therefore be lower while the semantic result is clearer.
- Source Navigation uses F8/Escape by default; explicitly configured `CTRL_ALT` remains a deprecated legacy hold mode.
- FAST removes live presentation by default but does not make WebDriver operations faster by changing their semantics.
- `PER_CLASS` is opt-in. The TestNG default remains `PER_METHOD`.

## Security / privacy

Native observation does not publish entered `sendKeys` text or JavaScript argument values. Central text redaction still protects diagnostic copies before HUD, trace, reports, network output, and failure-bundle text fan-out. Visual redaction remains a separate screenshot boundary; attached videos are caller-owned and are not redacted by Test Lens.

Source paths used for local navigation remain local-only. The offline selector, compatibility, and Migration tooling is nonpublished maintainer tooling with explicit bounds; it is not a runtime dependency or a normal Maven Central coordinate.

## Compatibility

- Java 17 or newer; clean-room consumers are certified on JDK 17 and 21.
- Selenium 4.39 remains an explicit consumer dependency.
- Published artifacts remain the parent plus seven runtime/library modules: core, overlay, main `selenium-test-lens`, JUnit 5, TestNG, Allure, and React.
- Local Chrome and Firefox release coverage includes headed/headless, DEFAULT/FAST, WebDriver BiDi, and representative runner lifecycles.

WebDriver BiDi flows do not require a matching `selenium-devtools` CDP module. A Chrome/CDP version warning is therefore not a Test Lens BiDi limitation. BrowserStack and other external Grid providers are not claimed as release-certified; provider-specific capability support is still required.

## Known limitations

- Native observation covers the documented operations and explicit observed views; it does not turn arbitrary third-party wrappers into Lens locators.
- FAST intentionally has no live HUD, automatic highlights, or live Source Navigation unless presentation is explicitly re-enabled.
- `PER_CLASS` is sequential and fails fast for unsafe parallel combinations.
- `FULL_PAGE` captures the top-level document's current responsive layout; it does not expand iframe documents or nested scroll containers.
- BiDi availability depends on the browser session and remote provider. `AUTO` degrades to unavailable diagnostics, not performance logs.
- Test Lens can attach an existing video but does not record or redact it.

## Upgrade from 0.3.x

1. Update the Test Lens artifacts to `0.4.0`, keep Selenium 4.39 explicit, and run on Java 17 or newer.
2. Review Smart Click's fallback policy, especially `javascriptClickFallback(false)` for physical-only behavior.
3. Choose browser execution (`HeadlessMode`) and observability (`DEFAULT` or `FAST`) independently.
4. Opt into `observeDriver(...)` only where ordinary Selenium calls should contribute evidence.
5. Keep TestNG `PER_METHOD` unless sequential class-owned reuse is intentional; review the documented parallel restrictions before choosing `PER_CLASS`.
6. Revisit HUD/highlight snapshots: operation-correlated rows and terminal-state protection deliberately change the visible lifecycle.

The complete user configuration map is in [Configuration](configuration.md); exact signatures are in the [generated API reference](reference/index.md).
