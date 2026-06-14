# UI Test Lens 0.1 feature audit

This document summarizes the current 0.1.0-SNAPSHOT feature set, module ownership, public entry points, limitations and recommended next steps.

For migration from historical JsTestTools-style names and one-module usage to the current UI Test Lens APIs, see [`ui-test-lens-api-migration-guide.md`](ui-test-lens-api-migration-guide.md).

For the visual overlay/HUD resource inventory and current configuration surface, see [`ui-test-lens-visual-overlay-hud.md`](ui-test-lens-visual-overlay-hud.md).

## Current product shape

UI Test Lens is now shaped around:

```text
Playwright-like reliability + business diagnostics + Selenium compatibility
```

It does not replace Selenium and does not depend on Playwright. The project builds a diagnostic and reliability layer above Selenium: retryable locators, actionability checks, business-readable assertions and steps, trace/evidence reports, auth state reuse, and passive network diagnostics.

The project is still pre-1.0. Public APIs are usable, but names, package boundaries, and stricter behavior may still change before a stable release.

## Module ownership summary

| Module | Responsibility | Selenium dependency | Notes |
|---|---|---|---|
| `ui-test-lens-core` | Logging/event model, sinks, exporters, trace/evidence model, HTML trace exporter | No | Neutral foundation shared by all modules. |
| `ui-test-lens-overlay` | Runtime JS overlay resources and browser script executor abstraction | No | Owns overlay runtime assets and stays Selenium-free. |
| `ui-test-lens-selenium` | Selenium facade, locators, assertions, actionability, overlay policy, evidence capture, auth/session state, network diagnostics | Yes | Main Selenium compatibility and reliability layer. |
| `ui-test-lens-react` | React support, React-aware actionability, React-side helpers | Yes, through Selenium module | Extension layer; Selenium does not depend on React. |
| `ui-test-lens` | All-in-one compatibility bundle | Transitive | Convenience artifact for users who want the full feature set. |
| `ui-test-lens-examples` | Documentation and compile-check examples | Test/example only | Not intended as a runtime dependency or deployable user artifact. |

## Implemented feature inventory

### 5.1 Logging and exporters

`ui-test-lens-core` provides `UiTestLensLogger`, structured log entries, log levels/statuses/event types, and sink-based routing. Current sinks include `ConsoleLogSink`, `InMemoryLogSink`, and `ConsumerLogSink`.

Text, JSON, and HTML log exporters are available for basic evidence generation. This remains the neutral event/logging layer used by Selenium, overlay, trace, and reporting integrations.

### 5.2 Runtime overlay resources

`ui-test-lens-overlay` owns runtime JavaScript resources and overlay panels. The primary runtime namespace is `window.__uiTestLens`, while legacy aliases are maintained for compatibility.

The overlay module uses `BrowserScriptExecutor` and stays Selenium-free. Selenium-specific construction lives in the Selenium module.

Visual runtime resources include API overlay, wait HUD, highlight, type hint, scroll arrow, HUD panel, and assertion badges. Current public visual configuration is intentionally limited to `OverlayConfig` settings such as enablement, HUD visibility/position/offset/max width, decoration duration, highlight color, and a legacy global popup close selector.

### 5.3 Selenium facade

`JsOverlayDebug` is the main Selenium-side facade. It coordinates HUD/log/step APIs, overlay initialization, smart Selenium actions, retryable locators, assertions, trace sessions, evidence helpers, auth/session state, and network diagnostics.

`OverlayWait`, smart actions, and compatibility helpers remain available for Selenium users while newer APIs layer reliability behavior on top.

### 5.4 Blocking overlay policy

`ui-test-lens-selenium` provides `OverlayPolicy`, `OverlayHandler`, `OverlayAction`, `OverlayPolicyExecutor`, and `JsOverlayDebug.setOverlayPolicy(...)`.

Policies model known popup/overlay handling, including optional versus fatal overlays. They can click targets, press escape, wait until gone, or fail with a clear reason.

Current limitation: legacy `PopupDetector` and `BlockingOverlayHelper` heuristics still exist as fallback behavior. The policy API is the preferred explicit model, but old recovery paths have not been fully retired.

### 5.5 Actionability checks

Selenium actionability is implemented through `ActionabilityChecker`, `ActionabilityOptions`, `ActionabilityReport`, and result/failure models.

The initial checks cover attached, visible, enabled, stable bounds, scroll into view, receives click point, covered element detection, and configured overlay policy.

Current limitation: this is best-effort diagnostic actionability, not yet a strict full Playwright actionability engine.

### 5.6 React-aware actionability

`ui-test-lens-react` provides `ReactActionabilityChecker`, `ReactSupport.actionability(...)`, and `ReactSupport.checkActionability(...)`.

React readiness checks extend the Selenium actionability base with `aria-disabled`, `aria-busy`, `data-loading`, `data-pending`, progressbars, spinners, skeletons, focus-lock, dialogs, and modals.

Current limitation: React heuristics are best-effort and intentionally stay in the React module. `ui-test-lens-selenium` still does not depend on React.

### 5.7 Retryable UI locators

`ui-test-lens-selenium` provides `UiLocator`, `UiLocatorOptions`, `UiLocatorResolver`, and supporting result/failure models.

Public entry points include `JsOverlayDebug.locator(...)`, `JsOverlayDebug.getByTestId(...)`, `getByText(...)`, `getByTextContaining(...)`, `getByLabel(...)`, `getByPlaceholder(...)`, and `getByRole(...)`. A locator stores `By` and a description, resolves a fresh `WebElement` before each action, retries stale/intercept/not-interactable cases, and can run actionability before click.

Current limitation: Playwright-style helpers are pragmatic XPath/CSS-based helpers, not a full Playwright locator engine. `getByRole(...)` does not implement the complete ARIA accessible-name algorithm, and `getByText(...)` can match parent containers in complex DOM trees. Prefer `getByTestId(...)` for critical flows when stable test IDs are available.

### 5.8 Retryable web assertions

`UiExpect`, `UiAssertionOptions`, `UiAssertionResult`, `UiAssertionError`, and `UiAssertionReporter` provide retryable web assertions over `UiLocator`.

Public entry points include `JsOverlayDebug.expect(...)` and `UiLocator.expect()`.

Supported assertions include visible/hidden, enabled/disabled, exact text, contains text, exact value, and contains value.

Current limitation: full soft assertion aggregation is not complete yet. Logger events are mapped into attached trace sessions, but future releases can still add richer parent/child relationships and evidence links.

### 5.9 Business assertions

`BusinessAssertions`, `BusinessAssertionOptions`, `BusinessAssertionResult`, `BusinessAssertionFailure`, and `BusinessAssertionError` provide a lightweight business-readable assertion group DSL.

The public entry point is `JsOverlayDebug.business(...)`. Checks can collect multiple failures or fail fast depending on options, and `BusinessAssertionError` produces a more readable grouped summary than raw Selenium/assertion exceptions.

Current limitation: domain-specific DSL methods belong in the user's test project or adapters, not in the core library.

### 5.10 Business steps

`UiStep`, `UiStepOptions`, `UiStepResult`, `UiStepStatus`, `UiStepFailure`, `UiStepError`, and related reporter/scope classes implement named test steps.

Public entry points include `JsOverlayDebug.step(...)`. Steps record status, start/end time, duration, and failure summary. They integrate with HUD labels through existing `setStep(...)` and `hudLog(...)`, and can optionally capture screenshots on failure.

Current limitation: there is not yet a full hierarchical trace viewer. Step data is present, but nested visualization remains basic.

### 5.11 Trace/evidence model

`ui-test-lens-core` provides `UiTestLensSession`, `TraceEvent`, `TraceArtifact`, `TraceFailure`, `TraceMetadata`, `TraceJsonExporter`, `CompositeLogSink`, and `TraceLogSink`.

Selenium integration includes `JsOverlayDebug.startSession(...)`, `attachSession(...)`, `session()`, `attachScreenshot(...)`, and `attachVideo(...)`. Once a session is attached, logger events are forwarded into the trace timeline through `TraceLogSink`.

Current limitation: trace event mapping is now automatic for logger events, but deep hierarchy, parent/child grouping, and richer evidence correlation remain future work.

### 5.12 HTML trace report exporter

`TraceHtmlExporter`, `TraceHtmlExportOptions`, and `TraceHtmlEscaper` generate a self-contained HTML report from `UiTestLensSession`.

Convenience APIs include `UiTestLensSession.exportHtml(...)` and `JsOverlayDebug.exportTraceHtml(...)`. The report shows session metadata, summary cards, event type summary, categorized timeline events, failure summary, artifact preview/list, and optional raw JSON.

Current limitation: the report is intentionally static. It is not a full interactive trace viewer.

### 5.13 Screenshot evidence capture

`ui-test-lens-selenium` provides `ScreenshotCapture`, `ScreenshotCaptureOptions`, `ScreenshotCaptureResult`, and related status/path strategy classes.

`JsOverlayDebug.captureScreenshot(...)` uses Selenium `TakesScreenshot`, writes PNG files under `target/ui-test-lens/screenshots` by default, and attaches screenshots to the active `UiTestLensSession` when present.

Current limitation: screenshot-on-failure is opt-in through step options and is not enabled by default.

### 5.14 Video evidence attachments

`VideoEvidence`, `VideoEvidenceOptions`, `VideoEvidenceResult`, and source/status models provide ordered video references.

Public APIs include `attachVideoFile(...)` and `attachVideoUrl(...)`. Modeled sources include `LOCAL_FILE`, `REMOTE_URL`, `SELENIUM_GRID`, `SELENOID`, `BROWSERSTACK`, `SAUCE_LABS`, `CI_ARTIFACT`, and `CUSTOM`.

Current limitation: this is attachment/reference support only. UI Test Lens does not record video, run ffmpeg, download provider artifacts, or call provider APIs.

### 5.15 Auth/session state

`AuthState`, `AuthStateManager`, `AuthStateOptions`, `AuthRestoreOptions`, cookie/storage DTOs, and manual JSON exporter/parser implement Selenium-side auth state capture and restore.

The public entry point is `JsOverlayDebug.auth()`. The manager captures cookies, localStorage, and sessionStorage, stores metadata such as label/role/origin/domain, saves JSON, loads JSON, and restores state through WebDriver and `JavascriptExecutor`.

Safety notes: no passwords are modeled as dedicated fields, but auth state files can contain cookies and tokens. `target/ui-test-lens/auth/` is ignored.

Current limitation: no encryption, no cross-origin storage support, and no built-in login flow.

### 5.16 Passive network diagnostics

`NetworkDiagnostics`, `NetworkDiagnosticsOptions`, `NetworkEvent`, `NetworkRequest`, `NetworkResponse`, `NetworkFailure`, `NetworkSummary`, `NetworkLogExporter`, and `NetworkAssertionError` provide passive network diagnostics.

The public entry point is `JsOverlayDebug.network()`. Diagnostics supports manual/fallback collection, request/response/failure events, ignored URL patterns, omitted/masked headers, `assertNoFailedRequests()`, JSON export, and `NETWORK_LOG` trace artifact attachment.

Capture modes:

- `MANUAL` is implemented.
- `AUTO` falls back to manual.
- `OFF` disables collection.
- `PERFORMANCE_LOGS` and `BIDI` are modeled but currently reported as unsupported/fallback without extra dependencies.

Current limitation: no real browser capture provider is implemented yet.

### 5.17 Wait-for-response assertions

`NetworkWaitCondition`, `NetworkWaitResult`, `NetworkResponseExpectation`, and wait status/failure models add retryable waits over collected network events.

Public APIs include `NetworkDiagnostics.waitForResponse(...)` and `NetworkDiagnostics.expectResponse().within(...)`.

Supported matching includes URL contains, exact URL, URL regex, method, exact status, status range, and request-only mode.

Current limitation: this depends on events already collected by the diagnostics layer. It does not intercept, mock, route, or fulfill requests.

## Public entry points

```java
JsOverlayDebug overlay = new JsOverlayDebug(driver);

overlay.setOverlayPolicy(policy);

overlay.locator(By.cssSelector("[data-testid='save']")).click();
overlay.getByTestId("save").click();

overlay.expect(overlay.getByTestId("toast")).toContainText("Saved");

overlay.business("Order summary")
        .check("shows total", () -> overlay.getByTestId("total").expect().toHaveText("123.00 PLN"))
        .verify();

overlay.step("Save order", () -> {
    overlay.getByTestId("save").click();
});

UiTestLensSession session = overlay.startSession("Checkout flow");
overlay.captureScreenshot("After save");
overlay.attachVideoUrl("CI video", "https://ci.example.com/artifacts/video.mp4");
overlay.exportTraceHtml(Path.of("target/ui-test-lens/checkout.html"));

AuthState state = overlay.auth().captureState(AuthStateOptions.builder().label("customer").build());
state.save(Path.of("target/ui-test-lens/auth/customer.json"));

overlay.network().start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.MANUAL).build());
overlay.network().expectResponse().urlContains("/api/orders").status(201).within(Duration.ofSeconds(10));
```

## Safety and privacy notes

- Auth state files can contain cookies and tokens.
- Do not commit `target/ui-test-lens/auth/`.
- Network headers are omitted by default and sensitive headers are masked when header capture is enabled.
- Screenshots and videos may contain sensitive UI data.
- HTML reports may contain user-facing text, failure messages, paths, URLs, and artifact metadata.
- Passwords are not modeled as dedicated fields.
- No encryption is provided in 0.1.

## Current limitations

- The project is pre-1.0 and public APIs may still change.
- Maven Central release metadata/publication is not configured yet.
- WebDriver BiDi capture is not implemented yet.
- Network interception and mocking are not implemented.
- Provider-specific video download is not implemented.
- Built-in video recording is not implemented.
- Playwright-style locator helpers are initial/pragmatic and do not implement the full Playwright locator or ARIA accessible-name algorithms.
- The HTML trace report is not a full interactive trace viewer.
- Auth state encryption is not implemented.
- Cross-origin auth storage is not implemented.
- Real browser network capture beyond manual/fallback mode is not implemented.

## Recommended next steps

1. Polish and stabilize public API names before a 0.1 release.
2. Harden Playwright-style locator helper semantics and document edge cases.
3. Improve HTML trace report UX.
4. Add richer parent/child trace grouping and evidence correlation.
5. Add optional real browser network capture providers:
   - performance logs,
   - BiDi,
   - guarded browser support.
6. Add wait/action trace markers to screenshots and videos.
7. Add Maven Wrapper.
8. Prepare publishing metadata, but do not release yet.
9. Add API migration guide from historical JsTestTools names.
