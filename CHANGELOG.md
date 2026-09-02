# Changelog

All notable changes to Selenium Test Lens will be documented in this file.

## [Unreleased]

### Added
- Added the published optional `selenium-test-lens-testng` module with an explicit per-invocation listener, factory configuration, current-invocation context, TestNG status mapping, and isolated retry/DataProvider/parallel lifecycle ownership.
- Added the published optional `selenium-test-lens-junit5` module with `TestLensExtension`, per-invocation WebDriver/Lens injection, passed/failed/aborted mapping, parallel-safe JUnit store isolation, and cleanup-safe driver ownership.
- Added `TestLens.finishSkipped(String)` so runner integrations can finalize aborted, assumed, or skipped tests as `SKIPPED` while retaining the common JSON/HTML and HUD-cleanup pipeline.

### Changed
- Made facade finalization outcome explicit: `finishFailed(null)` now remains `FAILED`, and only failed finalization can request an automatic failure screenshot.
- Fixed `UiAssertionOptions.failFastOnMissingElement(true)` for the normal `UiLocator.expect(options)` path: genuinely missing required elements now fail on the first observation, while the default remains retryable. Missing elements still satisfy `toBeHidden`, and stale elements remain a distinct retryable state.
- Made `MANUAL` the default and only active `NetworkDiagnostics` capture mode. `AUTO`, `BIDI`, and `PERFORMANCE_LOGS` now report `UNSUPPORTED` without falling back or waiting for unavailable browser events.
- Deprecated `NetworkDiagnosticsOptions.attachToSession` for removal in 0.2.0. Network diagnostics are attached only through explicit `NetworkDiagnostics.attachToSession(...)` calls.

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

