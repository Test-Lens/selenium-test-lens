# Capabilities

Test Lens for Selenium is an observability and failure-evidence layer for a consumer-owned `WebDriver`. Its value is not the number of wrapped methods; it is the connection between native interaction, visible recovery, structured trace, policy, and post-failure evidence.

## 0.3.0 capability additions

| Capability | Public entry point | Core contract |
| --- | --- | --- |
| Configurable HUD | `TestLensOptions.hud(HudOptions)` | Preset base plus order-independent explicit overrides; same renderer in runtime, homepage demo, and HUD Studio. |
| Visual Redaction | `TestLensOptions.visualRedaction(...)` | Screenshot-pixel masking, automatic password SOLID masks, and STRICT fail-closed publication by default. |
| Managed Auth State | `lens.authState().ensure(...)` | Restore and tri-state validation before bounded login/recreation and atomic persistence. |
| Managed Test State & Resources | `scenarioState()`, `suiteState()`, `resources()` | Per-invocation isolation, intentional suite sharing, and exactly-once LIFO cleanup. |
| Allure | `AllureTestLens.attach(...)` | Optional publication of finalized Test Lens evidence into the active Allure executable. |

These APIs are additive. See [What's new in 0.3.0](whats-new-0.3.0.md) and the [0.2.x migration guide](migrating-0.2-to-0.3.md).

## Five capability stories

### 1. Native interactions with visible recovery

The recommended `UiLocator.click()` resolves the current element, runs best-effort actionability diagnostics, and uses the bounded `NATIVE → ACTIONS → POINT → JS` cascade. Physical fallback stages dispatch only through a hit-tested point owned by the target or its descendant; the default final `HTMLElement.click()` fallback handles logically enabled targets whose physical geometry remains blocked. Set `UiLocatorOptions.javascriptClickFallback(false)` for physical-only `NATIVE → ACTIONS → POINT` behavior. Highlighting remains pointer-transparent visual feedback, and no strategy substitutes an ancestor or mutates application DOM to expose the target.

The HUD and session trace make that behavior observable. See [element actions](elements/actions.md#click-contract-bounded-fallback-cascade) and [visual diagnostics](observability/visual-diagnostics.md).

### 2. Semantic and scoped queries

Stable `0.1.0` supplies Selenium locators plus the original test-id, text, and role-oriented helpers.

The development pipeline can filter a collection, select by position, search true descendants, or retain parents containing a match. Order is significant and scoping cannot escape to a sibling or document-global match, including for an XPath beginning with `//`. See [locators](elements/locators.md) and [collections](elements/collections.md).

### 3. Measurable recovery

Recovery retry means a physical operation failed and Lens scheduled another attempt. Condition polling and runner retry are separate. The summary shows recovered failures, while outcome policy can report, warn, or reject an otherwise passed session after evidence is written. See [flakiness and retry outcomes](observability/flakiness.md).

### 4. Trace, reports, and failure evidence

`UiTestLensSession`, trace, and HTML/JSON reporting are available in `0.1.0`. They preserve operations, status, failures, and attachments as a coherent timeline.

For a final failed session, Test Lens can assemble screenshots, trace, report, context, runtime/configuration allowlists, network summary, manifest, and ZIP. Collection is best-effort and finalization never closes the driver. Video is attached, not recorded. See [trace](observability/trace.md), [reports](observability/reports.md), and [failure bundles](observability/failure-bundles.md).

Screenshot capture remains viewport-based by default. Release 0.2.0 adds bounded, opt-in full-page scroll-and-stitch capture through the same evidence pipeline, including diagnostic and clean failure images. It preserves the current viewport layout and uses no CDP. See [screenshots and evidence](observability/screenshots-evidence.md#portable-full-page-capture).

### 5. Central protection for diagnostic text

One text policy creates safe diagnostic copies before fan-out to HUD, trace, sinks, reports, network/API diagnostics, and failure-bundle text files. It preserves the structural exception type while leaving the original throwable untouched in the execution path. Separate browser-side [visual redaction](security/visual-redaction.md) masks configured screenshot regions; video is not modified. Optional page source/console are best-effort, auth-state files remain outside the transformation, and `disabled()` is an explicit text-redaction opt-out. See [redaction](security/redaction.md).

## Advanced capabilities

| Capability | Boundary | Guide |
|---|---|---|
| WebDriver BiDi network diagnostics | Passive observation/correlation, not interception, mocking, CDP, or body capture. Manual events, waits, and assertions exist in `0.1.0`; BiDi lifecycle, safe snapshots, and HUD filtering are `0.2.0`. | [Network](advanced/network.md) |
| Managed and low-level authentication state | Managed restore/validate/recreate has tri-state validation, one-login maximum, locking, and atomic replacement. Low-level cookies/storage capture and restore remain available. Cross-origin SSO restore is not automatic; persisted state is sensitive. | [Auth state](advanced/auth-state.md) |
| Managed Test State & Resources | In-memory typed state is isolated per physical invocation; suite state is explicitly shared inside one run; temporary resources use exactly-once LIFO cleanup. There is no disk or cross-JVM state. | [Managed test state](features/managed-test-state.md) |
| Page and SPA waits | Ready-state waits plus an XHR/fetch tracker that sees only calls begun after installation. It is not full browser network idle. | [Waiting](elements/waiting.md) |
| React & SPA resilience | DOM-convention helpers for rerender recovery, busy/loading readiness, actionability, and React Select; no React component-tree access or universal design-system guarantee. | [React/SPA helpers](features/react-spa.md) |
| API overlay | Displays caller-supplied API previews; it does not capture browser traffic. | [Visual helpers](advanced/visual-helpers.md) |
| JUnit 5 / TestNG adapters | Optional `0.2.0` artifacts for per-invocation lifecycle ownership. | [Framework integration](framework-integration.md) |
| Allure attachments | Optional `0.3.0` module streams finalized screenshots, report, trace, and ZIP into the active Allure executable. | [Allure integration](integrations/allure.md) |
| Explicit report upload | Streams one completed report ZIP after finalization; it is not automatic and does not use or close WebDriver. | [Report upload](observability/report-upload.md) |

## Find a routine task

These functions complete the working API but are not the project's primary differentiators.

| I want to… | Go to |
|---|---|
| attach an existing driver and finalize a session | [TestLens lifecycle](reference/test-lens.md) |
| click, fill, clear, press, hover, or inspect an element | [Elements](elements/index.md) |
| use checkboxes, radio buttons, uploads, or select controls | [Actions](elements/actions.md) / [Select controls](elements/select-controls.md) |
| poll an element, collection, URL, or title | [Assertions](elements/assertions.md) / [Waiting](elements/waiting.md) |
| switch frame/window or handle an alert | [Browser context](browser-context/index.md) |
| configure timeouts, overlays, evidence, auth, or network | [Configuration](reference/configuration.md) |
| migrate Page Objects or coexist with Allure | [Framework integration](framework-integration.md) |

## Ownership and limits

- The main facade attaches to, but does not create, pool, or close, WebDriver. Optional runner adapters close only drivers created by their configured factories.
- Test Lens does not replace Selenium, Page Objects, JUnit, TestNG, or their lifecycle rules.
- It does not implement every W3C Actions, CDP, or BiDi operation. Use raw Selenium for unsupported low-level work.
- Browser/WebDriver computes accessible role and name in the strengthened semantic contract; Lens does not implement the ARIA name algorithm.
- Overlay actionability is best-effort diagnostics, not proof that an interaction will succeed.
- Evidence depends on browser capabilities and collector availability. Failure of one best-effort collector does not rewrite the test outcome.
- Central redaction recognizes structured and configured secrets, not arbitrary personal data; visual pixels remain application-controlled.

See the [public API catalog](reference/public-api-catalog.md) for exact signatures rather than treating this capability map as a method inventory.
