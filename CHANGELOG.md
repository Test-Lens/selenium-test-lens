# Changelog

All notable changes to Selenium Test Lens will be documented in this file.

## [Unreleased]

- Added lazy semantic accessibility locators on the main `TestLens` facade for labels, placeholders, and alt text, plus browser-computed `UiLocator.accessibleName()`. Named role matching now uses WebDriver `getAccessibleName()` and `getAriaRole()` without falling back to element text or a partial in-library accessible-name algorithm.

The next planned release line is 0.2.0. Changes below are under development and have not been released.

- Added semantic `UiLocator` form and element actions: idempotent `check()`/`uncheck()` and `isChecked()` for native and ARIA controls, single-operation safe file upload, and explicit focus/scroll operations. Styled native controls activate only through their standard associated label; asynchronous state confirmation never repeats a click, and upload diagnostics do not expose local paths or file names.

### Added
- Added immutable `NetworkHudFilter` presets and URL rules for reducing raw network traffic in the HUD without changing capture, waits, summaries, trace, JSON, external log sinks, or failure evidence. Raw HUD entries now carry safe structured metadata and compact query-free messages.
- Added passive browser-network capture through Selenium 4.39 WebDriver BiDi. `BIDI` subscribes to request, completed-response, and fetch-error events; `AUTO` selects BiDi only when an active connection can be subscribed, while `MANUAL` remains the default and `PERFORMANCE_LOGS` remains unsupported. Capture now exposes requested/active modes, bounded event storage, dropped-event counts, redirect correlation, asynchronous waits, trace events, and failure-bundle summaries.
- Added automatic best-effort failure bundles for final `FAILED` sessions, including diagnostic/clean screenshots, context, trace diagnostics, safe runtime/configuration snapshots, network summary, optional page source/console, a versioned manifest, and deterministic ZIP packaging.
- Added runner-neutral recovery-retry observability with immutable per-session summaries, deterministic action/locator/exception grouping, lost-time accounting, configurable passed-outcome policies, JSON/HTML Flakiness sections, and propagation through the JUnit 5 and TestNG adapters.
- Added the published optional `selenium-test-lens-testng` module with an explicit per-invocation listener, factory configuration, current-invocation context, TestNG status mapping, and isolated retry/DataProvider/parallel lifecycle ownership.
- Added the published optional `selenium-test-lens-junit5` module with `TestLensExtension`, per-invocation WebDriver/Lens injection, passed/failed/aborted mapping, parallel-safe JUnit store isolation, and cleanup-safe driver ownership.
- Added `TestLens.finishSkipped(String)` so runner integrations can finalize aborted, assumed, or skipped tests as `SKIPPED` while retaining the common JSON/HTML and HUD-cleanup pipeline.

### Changed
- Reduced the accidental pre-1.0 binary surface from 230 types/1719 callables to 214 types/1653 callables. Recommended user APIs are unchanged; local reporters, locator result plumbing, unused plans/helpers, and the HTML escaper are now implementation-private. `BrowserScriptExecutor` is now explicitly classified as a supported low-level SPI, and the two previously deprecated no-op network-options callables were removed for the 0.2.0 line.
- Made facade finalization outcome explicit: `finishFailed(null)` now remains `FAILED`, and only failed finalization can request an automatic failure screenshot.
- Fixed `UiAssertionOptions.failFastOnMissingElement(true)` for the normal `UiLocator.expect(options)` path: genuinely missing required elements now fail on the first observation, while the default remains retryable. Missing elements still satisfy `toBeHidden`, and stale elements remain a distinct retryable state.
- Kept `MANUAL` as the default network mode. `BIDI` and `AUTO` now use an explicitly enabled WebDriver BiDi session without fallback; `PERFORMANCE_LOGS` remains `UNSUPPORTED`.
- Removed the previously deprecated, no-op `NetworkDiagnosticsOptions.attachToSession` accessor and builder method for 0.2.0 development. Network diagnostics are attached only through explicit `NetworkDiagnostics.attachToSession(...)` calls; failure-bundle finalization still snapshots the active network summary automatically.

### Removed
- Removed the three implementation-injection constructors of `JsOverlayDebug`; migrate to `JsOverlayDebug(WebDriver)` or `JsOverlayDebug(WebDriver, OverlayConfig)`. Removed the implementation-only `ApiCallActions` type and hid local assertion, business, locator, step, plan, script, and exporter plumbing. This intentional pre-1.0 break is planned for the 0.2.x line.

## [0.1.0]

### Added
- Multi-module Maven structure.
- `selenium-test-lens-core` with the logging/event model, log sinks, log exporters, `BrowserScriptExecutor`, and `JsResources`.
- `selenium-test-lens-overlay` with browser runtime JavaScript resources and overlay bridge classes.
- `selenium-test-lens` runtime JAR with the public Selenium facade/actions, waits, guards, popup/blocking overlay helpers, target resolver, and Selenium factories/adapters.
- `selenium-test-lens-react` with React-safe helpers and the `ReactSupport` entrypoint.
- `selenium-test-lens-examples` with compile-checked examples.
- Runtime JavaScript resources for API overlay, Wait HUD, Highlight, Type hint, Scroll arrow, HUD panel, and Assertion badges.
- `TestLens.attach(existingDriver)` with native HUD/trace actions, waits, retryable assertions, collections, frames/windows, HTML Select, common pointer actions, and browser alerts.
- Failure-safe session finalization with reports, screenshots and evidence that coexist with JUnit, TestNG and Allure.

### Changed
- Migrated project naming from the historical helper codebase to Selenium Test Lens.
- Moved Java packages to `io.github.testlens`.
- Prepared the first public `0.1.0` API and Maven Central artifact set.
- Split core, overlay, main Selenium runtime, React extension, and examples responsibilities.
- Introduced `window.__uiTestLens` as the primary browser runtime namespace while preserving legacy aliases.

### Removed
- Removed direct dependencies on legacy utility classes.
- Removed RestAssured from the main artifact path.
- Removed Selenium dependency from the overlay module.
- Removed React dependency from the Selenium module.

### Notes
- Project is still pre-1.0.
- Public APIs may still change between 0.x releases.
- Central Publisher Portal publication remains a manual, reviewed operation.
- Legacy browser runtime aliases are still maintained for compatibility.

