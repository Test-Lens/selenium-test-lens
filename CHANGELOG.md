# Changelog

All notable changes to Test Lens for Selenium will be documented in this file.

## [Unreleased]

### Added

- Added versioned structured runtime locator observations to existing native Selenium and `UiLocator` trace events.
  Standard Selenium `By` values use official remote parameters, custom and relative locators remain explicitly
  opaque/complex, and parent, shadow, frame, and window context is bounded without additional WebDriver commands.
  JSON exposes the additive `locatorObservation` object while retaining legacy locator fields and redaction.
- Added opt-in `ObservabilityMode.FAST`, independently configurable through Java, `testLens.observability`, or
  `TEST_LENS_OBSERVABILITY`. The successful-test path keeps Selenium/Smart Click/wait/retry semantics and bounded
  trace correlation while skipping default live HUD, automatic highlight, Source Navigation presentation, and
  retaining only the passed summary. Explicit presentation/retention overrides and full visually redacted failure
  evidence remain available.

### Fixed

- Added immutable pre-session headed/headless execution intent with Java API, `testLens.headless` system-property,
  and `TEST_LENS_HEADLESS` environment resolution. Adapted TestNG/JUnit factories receive it before driver creation;
  legacy factories remain unchanged for `UNSET`, and attaching an existing driver never recreates its session.
- Added opt-in observation of native Selenium operations through `TestLens.observeDriver()` and selective `observe(WebElement[, label])` views. Existing Page Objects can contribute correlated semantic HUD, trace/report, source-navigation, redacted metadata, durations, and automatic highlights while retaining exactly-once native Selenium execution and original exceptions; no Smart Click, wait, retry, re-resolution, or JavaScript fallback is introduced.

- Reduced semantic HUD transport overhead without dropping diagnostics: warm event updates use a self-validating browser dispatch, and ready operation diagnostics share the terminal flush while RUNNING, warnings, retries, failures, and user messages retain immediate delivery. Document navigation still triggers lazy reinjection, active alerts are detected and deferred in a bounded queue without changing prompt state, and the renderer preserves every semantic event while doing layout/auto-scroll once per batch. An opt-in Chrome/Firefox benchmark records reproducible CSV/JSON command counts and timings.

- Added opt-in TestNG `PER_CLASS` WebDriver ownership for sequential methods of one concrete class instance. Each DataProvider row and retry remains an independent Test Lens invocation/report, method hooks receive the correct callback-local context, class teardown is honored for both TestNG 7.9 listener orders, conflicting method parallelism fails before driver creation, and adapter-owned drivers are quit once.

- Hardened WebDriver BiDi startup for remote Selenium providers. Test Lens now initializes lazy `HasBiDi` connections with Selenium 4.39's `getBiDi()` contract, creates one temporary augmented view for capability-backed `RemoteWebDriver` sessions, bounds pre-listener initialization retry, and reports structured unsupported, endpoint, session, protocol, and capture-start diagnostics. This improves compatibility with remote providers, including BrowserStack configurations that expose Selenium BiDi.
- HUD rows now present structured semantic category and lifecycle phase independently from logging severity. Public actions/assertions update one operation-correlated row from RUNNING/RETRYING to PASSED/FAILED, STANDARD suppresses successful locator/actionability/highlight diagnostics, DEBUG exposes them as muted technical entries, and reports retain the semantic fields and Unicode through explicit UTF-8 output.
- Highlight feedback now follows one operation-owned visual lifecycle: internal resolve/actionability/Smart Click probes stay diagnostic-only, transitional states keep their full configured presentation time, terminal SUCCESS/FAILURE cannot be repainted by late callbacks, and rapid operations are ordered per target without blocking WebDriver execution. `HighlightOptions` adds explicit per-state duration overrides with inheritance and zero-state suppression, also supported by HUD Studio.
- Standard `UiLocator.click()` now uses one bounded `NATIVE → ACTIONS → POINT → JS` cascade. Physical fallbacks require a target-owned browser hit-test, covered but logically enabled targets reach one `HTMLElement.click()` dispatch, stale targets are re-resolved by the locator retry path, and `UiLocatorOptions.javascriptClickFallback(false)` keeps physical-only behavior.
- Source Navigation now defaults to an F8/Escape toggle while preserving explicitly configured `CTRL_ALT` as deprecated legacy hold behavior. The HUD remains normally scrollable and interactive, source targets use direct links, and IntelliJ navigation validates an exact project mapping before generating an encoded JetBrains Toolbox URI.
- Source Navigation now preflights IntelliJ version metadata, `jetbrains://` registration, `jetbrainsd`, and project/source mapping. Known incompatibilities disable misleading links and produce one actionable HUD/log diagnostic, while unverified environments remain usable with an explicit warning and an in-HUD retry.
- Full-page screenshot stitching now treats document height as a bounded dynamic capture extent, extending or cropping the tile plan as scrolling changes page height while retaining strict viewport, document-width, DPR, browsing-context, pixel, tile, and stabilization safeguards.

## [0.3.1] - 2026-09-16

### Added

- Added bounded, failure-aware trace retention with count, estimated-byte, and single-event limits; immutable final snapshots shared by JSON/HTML/failure bundles; explicit completeness telemetry; optional passed-session summary-only retention; pre-retention network redaction; and bounded failure context that reuses existing visually redacted screenshot evidence.

- Added opt-in Ctrl+Alt HUD source navigation for user-facing locator actions, waits, assertions, and manual highlights, with logical call-site metadata, lazy cached Maven/Gradle source resolution, IntelliJ/VS Code/custom protocol providers, remote-session safeguards, and HUD Studio generation. Absolute local paths remain local-only and are not exported.
- Added independent HUD timestamp presentation options: `HudTimestampFormat.ISO_UTC`, `TIME_ONLY`, and `DATE_TIME`, plus either the test JVM system zone or an explicit `ZoneId`. HUD Studio previews and generates the same configuration without freezing the author's local zone when SYSTEM is selected.
- Added immutable `HighlightOptions`, typed `HighlightState` values, and facade/locator entry points (`TestLens.highlight(...)`, `UiLocator.highlight(...)`) for interaction-free manual decoration. HUD Studio now previews and exports all five state colors, duration, border width, labels, enablement, and automatic feedback.

### Fixed

- HTML session, suite, log, and bundle reports now use a fluid viewport-width layout, readable failure diagnostics in every theme, full-row event attributes, sticky timeline headers, and a synchronized horizontal scrollbar for the currently visible overflowing timeline. AUTO now reacts correctly to system light/dark changes, and the standalone report remains usable offline with a no-JavaScript fallback.
- Every visible HUD event row now receives exactly one normalized timestamp through the shared runtime renderer, including steps, actions, highlights, waits, retries, assertions, network/control, auth-state, screenshot, warning, error, manual `hudLog`, and direct JavaScript paths.
- Invalid, missing, local/ambiguous, and placeholder timestamp values now receive one canonical instant when accepted instead of displaying empty brackets, `null`, `undefined`, `Invalid Date`, or labels such as `ui-test-lens`.
- Wait and structured-log paths now preserve the event instant; entries deferred while a browser alert is open retain their original time. Trace and JSON timestamps remain canonical UTC and duration/ordering behavior is unchanged.
- Element actions, waits, `UiExpect`, and legacy element assertions now share one state renderer driven by operation outcomes rather than log text. Ordinary polling uses WAITING, only a genuine subsequent operation/recovery attempt uses RETRY, and only terminal failures use FAILURE; negative assertions may succeed without inventing an element decoration.
- Highlight cleanup is operation-scoped: replacing a decoration cancels its old timer/listeners, stale timers cannot remove newer state, detached and Shadow DOM targets are handled safely, and decoration failures never replace the operation exception.
- Full-page diagnostic evidence keeps fixed and sticky Test Lens artifacts visible exactly once in the stitched image while refreshing geometry and visual-redaction masks for each tile.

## [0.3.0] - 2026-09-15

### Added

- Added Managed Test State & Resources with typed state isolated to one physical invocation, intentionally shared suite/run state with atomic initialization, retry and parallel isolation, and exactly-once LIFO scenario-resource cleanup integrated with JUnit 5, TestNG, and manual sessions. Cleanup failures are aggregated without replacing an existing primary test failure.
- Added Managed Auth State with restore/validate/recreate lifecycle, fail-safe tri-state validation, one-login maximum, process-local refresh/invalidate registration, canonical-path JVM and filesystem locking, and atomic old-file-preserving replacement.
- Added immutable `HudOptions` with Minimal, Compact, Standard, and Debug presets, semantic HUD row filtering, responsive atomic AUTO/INLINE/STACKED header layouts, validated semantic colors and opacity, anchored offsets, bounded sizing, global and section-specific local font presets, configurable native/subtle/standard event-log scrollbars, and bounded PNG branding. The shared runtime renderer now powers the runtime, homepage preview, and WYSIWYG HUD Studio.
- Added browser-side visual redaction for every Test Lens screenshot path, with explicit SOLID/BLUR locator rules, per-tile full-page refresh, and diagnostic/clean parity.
- Stabilized full-page screenshots with a temporary CSS guard, a frozen whole-overlay snapshot rendered exactly once at its initial document position, two-frame settling, one bounded whole-capture retry for changing document dimensions, and idempotent state restoration.

### Changed

- The runtime HUD now defaults to the compact product preset: pipeline and timestamps are hidden, TEST/STEP context is compact, and the event log receives the available panel space. Existing trace and session metadata are unchanged.
- React support is documented as React & SPA resilience rather than a runner integration; its DOM-convention behavior and optional artifact are unchanged.

### Security

- Screenshot capture now masks password inputs with SOLID by default and uses a STRICT fail-closed publication policy. BLUR is documented as obfuscation and falls back safely to SOLID when unavailable.
- Visual-mask installation verifies target geometry, recovers from React/SPA rerenders and replacements with a bounded whole-batch retry, and refreshes masks for every full-page tile.

### Integrations

- Added the optional `selenium-test-lens-allure` module for streaming finalized, redacted Test Lens screenshots, HTML, trace, and failure ZIP evidence into the active Allure test/step without replacing Allure reporting, capturing another screenshot, or using `ReportUploader`.

### Reliability

- Normalized insecure `SameSite=None` cookie representations during restore so persisted Firefox/WebDriver state remains replayable without forcing `Secure`, `Lax`, or `Strict`.
- Made browser-test Chrome process cleanup session-owned through exact profile arguments and process identity, preventing one concurrent harness from terminating another session.
- Updated failure-bundle browser contracts to honor best-effort page-source collection while retaining strong captured-source coverage and verifying that the driver remains active.
- Added viewport-safe runtime HUD clamping and deterministic panel/log height normalization across all four anchors.

### Documentation

- Added a user-facing 0.3.0 overview, a 0.2.x migration guide, consolidated configuration guidance, and prominent paths to HUD Studio and every new 0.3.0 capability.
- Added automated verification that public types introduced after the 0.2.0 API baseline declare `@since 0.3.0`.

### Compatibility

- Java 17 remains the minimum runtime and bytecode level. Clean-room Maven and Gradle consumers validate all seven library artifacts on JDK 17 and JDK 21, including the optional Allure module and its dependency isolation.
- Existing 0.2.x auth-state JSON remains readable; managed lifecycle and visual redaction are additive APIs.
- The previous development React documentation URL remains a noindex, version-local compatibility redirect.

## [0.2.0] - 2026-09-11

- Added explicit, synchronous upload of completed report ZIPs through the JDK HTTP client, with deterministic idempotency keys, bounded retries and response diagnostics, direct/system/explicit proxy selection, literal no-proxy rules, and no changes to session finalization or WebDriver ownership. Response preview consumption remains inside the request-timeout boundary, so a receiver cannot keep an upload blocked after sending only response headers.
- Added portable, opt-in full-page screenshots that preserve the current responsive viewport and stitch standard Selenium PNG tiles without CDP. Capture now reports mode, dimensions, and tile count, enforces pixel/tile limits, restores scroll and temporary styles, and can supply both diagnostic and clean failure-bundle images.
- Standardized published API Javadocs in English, clarified interaction, polling, lifecycle, network, evidence, and redaction contracts, and added a CI validator for Polish Javadoc text (including common words written without diacritics).
- Reorganized README and development documentation around Test Lens's observable interaction, scoped-query, recovery, trace/evidence, and diagnostic-redaction capabilities; clarified native click/recovery behavior and corrected local 0.1.0 versus 0.2.0 feature boundaries.
- Fixed central throwable redaction so trace, log exporters, retry summaries, network diagnostics, and failure reports retain the original exception class as structured `exceptionType` provenance while external sinks continue to receive only the redacted diagnostic copy.
- Fixed suite report aggregation so unfinished `STARTED` sessions can no longer be presented as a passed suite. JSON now records `summary.started`, HTML identifies incomplete sessions, and HTML/JSON/ZIP export remains a non-mutating diagnostic snapshot.
- Fixed session and facade finalization to be first-writer-wins and exactly-once per session. Repeated or concurrent finalizers now share one completed result (or the same retry-policy violation), preserve the first terminal outcome, and never repeat screenshots, network shutdown, exports, HUD cleanup, or failure-bundle creation.
- Fixed page/JavaScript waits so the main `TestLens` facade exposes document readiness and observed XHR/fetch idle waits using the configured locator timeout and polling interval. Network-idle timeout now throws, terminal JavaScript failures are preserved, React/SPA combinations share one deadline, and wait diagnostics emit one start plus one terminal event without creating recovery retries.
- Fixed Firefox compatibility in the XHR/fetch network-idle tracker by preserving the native `Window` receiver when delegating to `window.fetch`, preventing Gecko from raising `TypeError` before HTTP dispatch. The resulting contract is covered by the required Chrome and Firefox browser gates.
- Versioned published documentation with `mike`: immutable stable 0.1.0, an explicitly unreleased `/dev/`, a stable `latest` alias, and serialized full-branch GitHub Pages deployment.
- Fixed a capture lifecycle race where `stop()` during an in-progress BiDi initialization could be undone by the late completion of that initialization. Stopping now invalidates the generation before returning; stale sources are closed once and cannot publish state or events.
- Fixed semantic `getBy*` factories ignoring the `UiLocatorOptions` configured on their owning `TestLens`. Ordinary, semantic, and subsequently chained locators now share the same instance-scoped options; explicit locator options still take precedence.
- Fixed `NetworkDiagnostics.assertNoFailedRequests()` falsely passing when the current capture generation was never activated. Never-started, `OFF`, unsupported, and failed/in-progress starts now produce `NetworkAssertionError`; a successfully activated generation remains assertable after `stop()`.
- Fixed composed locator containment so descendant and `filterHas` queries cannot escape a parent subtree, including semantic locators and user XPath expressions beginning with `//`.

- Fixed a network confidentiality gap where the internal raw event buffer was correctly redacted by `events()` but `NetworkSummary.firstFailure()`, wait diagnostics, and `NetworkAssertionError` could still expose credentials, sensitive query values, fragments, headers, or throwable messages. Matching and correlation continue on raw session-local data; every public diagnostic result now receives an immutable snapshot protected by the effective central redaction policy.
- Fixed a structured-JSON redaction bypass where legal apostrophes or escaped quotes/backslashes inside a sensitive value could prevent the old quoted-pair matcher from recognizing the field. Valid JSON is now scanned structurally with bounded depth, whole sensitive values of every JSON type are replaced safely, and malformed fragments use a fail-closed tolerant fallback.
- Fixed auth-state origin isolation during restore. With origin validation enabled, Lens now preflights every storage entry, validates the browser origin after optional navigation and again before cookie mutation, and uses in-page atomic origin guards for storage clearing/writes. Redirects to SSO or another scheme/host/port are rejected before foreign-origin state is changed.
- Added an independent Gradle Wrapper consumer gate. It resolves release-transformed `0.2.0` artifacts only from an isolated Maven repository, exercises the main, React, JUnit 5 and TestNG public APIs, validates the resolved graph and Java 17 bytecode, and runs alongside the Maven clean-room consumer on JDK 17 and 21. Java 17 remains the minimum; Java 11 is not supported.
- Added an immutable, enabled-by-default `RedactionPolicy` shared by the logger, every sink, direct trace events, network diagnostics, the API overlay, reports, and failure-bundle text artifacts. Common structured credentials, sensitive URL parameters, JWT-shaped values, and caller-supplied literal secrets are masked before diagnostic fan-out; screenshots/video and replayable auth state remain outside the redaction boundary.
- Added runner-neutral polling page assertions through `expectPage()`: exact/contains URL and title checks use the assertion trace/HUD pipeline, observe the active window once per poll, keep URL matching raw and case-sensitive, and sanitize URL diagnostics.
- Added polling `UiExpect` assertions for collection count, DOM attributes, class tokens, computed CSS, selected/checked state, and DOM attachment. Every poll observes one fresh locator snapshot; assertion polling remains separate from recovery retry and does not mark a session flaky.
- Added lazy locator composition: scoped descendant lookup, visible-text/DOM-attribute/descendant filters, order-preserving collection stages, and count waits. Count polling observes fresh snapshots without being classified as recovery retry or flakiness.
- Added lazy semantic accessibility locators on the main `TestLens` facade for labels, placeholders, and alt text, plus browser-computed `UiLocator.accessibleName()`. Named role matching now uses WebDriver `getAccessibleName()` and `getAriaRole()` without falling back to element text or a partial in-library accessible-name algorithm.

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
- Removed the previously deprecated, no-op `NetworkDiagnosticsOptions.attachToSession` accessor and builder method in 0.2.0. Network diagnostics are attached only through explicit `NetworkDiagnostics.attachToSession(...)` calls; failure-bundle finalization still snapshots the active network summary automatically.

### Removed
- Removed the three implementation-injection constructors of `JsOverlayDebug`; migrate to `JsOverlayDebug(WebDriver)` or `JsOverlayDebug(WebDriver, OverlayConfig)`. Removed the implementation-only `ApiCallActions` type and hid local assertion, business, locator, step, plan, script, and exporter plumbing. This intentional pre-1.0 break is part of 0.2.0.

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

[Unreleased]: https://github.com/Test-Lens/selenium-test-lens/compare/v0.3.1...HEAD
[0.3.1]: https://github.com/Test-Lens/selenium-test-lens/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/Test-Lens/selenium-test-lens/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/Test-Lens/selenium-test-lens/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/Test-Lens/selenium-test-lens/releases/tag/v0.1.0

