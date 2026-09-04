# Capabilities

Selenium Test Lens attaches observability, retryable element operations, diagnostics, and reports to a `WebDriver` that your test already owns. Follow the functional links below for behavior and concrete calls; the [reference section](reference/index.md) contains configuration, result types, and optional compatibility material.

## Find your task

| I want to... | Go to |
| --- | --- |
| attach my existing driver and manage a session | [TestLens lifecycle](reference/test-lens.md) |
| find, click, fill, wait for, assert, or read an element | [Elements](elements/index.md) |
| work with matching collections or a native select | [Collections](elements/collections.md) / [Select controls](elements/select-controls.md) |
| switch frame, window/tab, or handle a native dialog | [Browser context](browser-context/index.md) |
| understand HUD, highlights, waits, and assertion feedback | [Visual runtime diagnostics](observability/visual-diagnostics.md) |
| find failure screenshots, trace, HTML/JSON, ZIP, or logs | [Observability](observability/index.md) |
| configure retry, timeouts, overlay, artifacts, auth, or network | [Configuration builders](reference/configuration.md) |
| integrate JUnit, TestNG, Page Objects, Allure, or parallel tests | [JUnit 5 integration](integrations/junit5.md) / [TestNG integration](integrations/testng.md) / [Framework integration](framework-integration.md) |
| add React-specific retry/select/readiness helpers | [Optional React API](integrations/react.md) |

## Session and interaction model

| Capability | Public entry point | Behavior and boundary |
| --- | --- | --- |
| Attach to an existing driver | [`TestLens.attach(...)`](reference/test-lens.md#creation-and-lifecycle) | Keeps the same driver; Lens neither creates nor closes it. |
| Session lifecycle | `startSession`, `session`, `finishPassed`, `finishFailed`, `finishSkipped` | Records passed, failed, or skipped trace status and performs best-effort final exports. Final `FAILED` creates the configured failure bundle; finish methods do not quit the driver. |
| Named steps | [`step(...)`](advanced/steps-business-assertions.md#named-steps) | Records start/pass/failure, optionally HUD output, nested events, stack trace, and a failure screenshot. |
| Selenium locators | [`locator(By...)`](elements/locators.md) | Accepts any Selenium `By`, with an optional diagnostic label. |
| User-facing locators | `getByTestId`, text, role, label, placeholder, alt text | Named roles and labels use browser-computed accessibility through typed WebDriver APIs; factories remain lazy. |
| Element actions | [`UiLocator`](elements/actions.md) | Click, fill, clear, checked-state controls, file upload, focus, scrolling, key input/Enter, hover, double-click, right-click, and HTML select controls. |
| Reads | [`UiLocator`](elements/information.md) | Text, value, attribute, DOM property, visibility, enabled state, count, and resolved elements. |
| Collections | `resolveAll`, `count`, `first`, `last`, `nth` | `nth` is zero-based; derived locators remain lazy. |
| Retry/wait | [`UiLocatorOptions`](reference/configuration.md#uilocatoroptions) | Retries configured transient element failures; explicit visibility/clickability/text waits use timeout and polling settings. |
| Flaky outcomes | [`RetrySummary`](observability/flakiness.md) | Aggregates physical failures that caused another operation attempt; polling and runner-level retries remain distinct. |
| Assertions | [`UiExpect`](elements/assertions.md) | Retryable visibility, enabled, text, substring, value, and value-substring assertions. |

Every instrumented locator operation emits structured start/pass/retry/failure events. Operations can update the HUD. The overlay-aware `click()` path highlights its resolved target when `OverlayConfig.enabled()` is true; other locator actions do not currently apply that click decoration. Checked-state operations use semantic native/ARIA controls and at most one activation before state-only confirmation polling. Upload records only the number of files, while focus and scrolling use one explicit JavaScript call without click or Actions fallback. Read methods also resolve through the locator retry policy. Sensitive field values are represented by safe previews or lengths in diagnostics where implemented; this is not a general secret-classification system.

## Browser contexts and native dialogs

`TestLens` switches frames by `By`, `UiLocator`, or index; returns to the parent or default content; lists and switches window handles; waits for exactly one new window; and wraps native alert/confirm/prompt operations with [`TestLensAlert`](browser-context/alerts.md). Context operations emit trace/log events. New-window polling uses locator timeout settings.

## Visual diagnostics and blocking overlays

[`OverlayConfig`](reference/configuration.md#overlayconfig) controls browser decoration, the HUD, position, offsets, width, highlight color, and theme. The standard overlay-aware click can highlight its resolved target, and advanced helpers provide explicit highlights. Overlay policies can detect cookie banners, modals, or other blockers and execute a click, press Escape, wait for an element to disappear, or fail with a supplied message. Policy APIs are advanced because selectors and side effects are application-specific.

## Trace, reports, and evidence

- `UiTestLensSession` stores metadata, timeline events, failures, and artifacts.
- `TraceJsonExporter` and `TraceHtmlExporter` produce JSON and standalone HTML for one session or a suite.
- `TraceReportBundleExporter` creates a ZIP bundle and can copy referenced artifacts.
- `TestLens.finish*` writes per-session `trace.json` and `report.html`; final `FAILED` additionally builds a versioned, best-effort failure bundle and ZIP by default.
- Reports always contain a `Flakiness` summary. Passed-session retry policy defaults to `REPORT_ONLY` and can warn or reject a passed outcome.
- `ScreenshotCapture` produces PNG evidence and can attach it to a session.
- `VideoEvidence` attaches an existing local video or URL; it does **not** record video.
- Structured logging supports console, consumer, composite, memory, JSON, HTML, and plain-text sinks/exporters.
- `BusinessAssertions` records domain-level pass/failure results independently of DOM assertions.

See [observability](observability/index.md) for artifact paths, nullable results, suite exports, and security considerations.

## Authentication/session state

[`AuthStateManager`](advanced/auth-state.md) captures selected cookies, local storage, and session storage plus metadata; exports/imports JSON; and restores state with configurable origin navigation, clearing, expiry validation, and component selection. Auth-state files can contain live credentials and must be treated as secrets.

## Network diagnostics

[`NetworkDiagnostics`](advanced/network.md) supports explicit `MANUAL` events and passive Selenium 4.39 WebDriver BiDi capture. `MANUAL` remains the default. `BIDI` requires a session created with `enableBiDi()`; `AUTO` selects only a working BiDi subscription, without fallback. `PERFORMANCE_LOGS` remains unsupported. Capture includes requests, completed responses, fetch errors, redirect correlation, bounded storage, waits, assertions, JSON, trace events, and failure-bundle summaries. It does not collect bodies or a separate cookie collection.

`NetworkHudFilter` controls only raw network rows shown in the browser HUD. Capture-level `ignoreUrlPattern(...)` removes data everywhere; HUD URL patterns preserve events and evidence. Selenium Java 4.39.0 does not expose reliable fetch/XHR/beacon classification through typed `RequestData`, so successful API traffic should be selected by explicit URL patterns rather than resource-type heuristics.

This is diagnostics and observation, not request interception, response mocking, or a general HTTP client.

## Optional React API

The separate `selenium-test-lens-react` artifact adds retry helpers for re-rendering DOM, React-oriented select traversal, and readiness/actionability checks for conventions such as `aria-busy`, `data-loading`, progress bars, spinners, skeletons, focus locks, dialogs, and caller-supplied blockers. It inspects the rendered DOM; it does not access React internals or guarantee knowledge of every component library. See [Optional React API](integrations/react.md).

## Framework coexistence

Lens is runner-agnostic. JUnit and TestNG retain test lifecycle and assertion ownership. The optional [`selenium-test-lens-junit5`](integrations/junit5.md) and [`selenium-test-lens-testng`](integrations/testng.md) modules create one driver/Lens/session per invocation through their native runner callbacks; manual integrations attach Lens after driver creation and finalize it in teardown. Existing Page Objects may accept/use `TestLens`, return `UiLocator`, or continue using raw Selenium beside Lens. Reports are independent artifacts and can be attached to Allure or another reporter. Do not share mutable drivers or sessions across threads.

## What Test Lens does NOT do

- The main facade does not create, configure, pool, or close `WebDriver` instances. Optional runner adapters create and close drivers supplied by their configured factories; they do not pool them.
- It does not replace Selenium, Page Objects, JUnit, TestNG, or their lifecycle rules.
- It does not record video; it only attaches a file/URL supplied by another recorder.
- It does not implement network request interception, mocking, or response rewriting.
- Test Lens does not implement the ARIA accessible-name algorithm. Named role and label matching relies on `WebElement.getAccessibleName()` and role confirmation on `getAriaRole()`; an unsupported driver command fails without fallback.
- It does not wrap all W3C Actions, CDP, or BiDi operations. Use raw Selenium for offsets, held-key sequences, complex multi-select, unsupported contexts, and low-level browser protocols.
- It cannot guarantee screenshots, overlays, storage access, or network capture when the driver/browser lacks the required Selenium/JavaScript capabilities.
- It does not automatically redact every secret from screenshots, pages, arbitrary labels, metadata, reports, or network bodies/headers. Control what you capture and protect generated artifacts.
