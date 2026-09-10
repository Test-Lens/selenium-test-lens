# Capabilities

Selenium Test Lens is an observability and failure-evidence layer for a consumer-owned `WebDriver`. Its value is not the number of wrapped methods; it is the connection between native interaction, visible recovery, structured trace, policy, and post-failure evidence.

## Five capability stories

### 1. Native interactions with visible recovery

The recommended `UiLocator.click()` resolves the current element, runs best-effort actionability diagnostics, decorates it when configured, and activates it with native `WebElement.click()`. Highlighting is pointer-transparent visual feedback. An intercepted click may be retried after an explicit overlay policy handles a blocker, and the locator policy may begin a fresh action attempt. There is no JavaScript-click, Actions-click, ancestor-click, or hidden mutation fallback.

The HUD and session trace make that behavior observable. See [element actions](elements/actions.md#click-contract-native-activation-visible-recovery) and [visual diagnostics](observability/visual-diagnostics.md).

### 2. Semantic and scoped queries

Stable `0.1.0` supplies Selenium locators plus the original test-id, text, and role-oriented helpers.

!!! info "Coming in 0.2.0"
    Browser-computed accessibility matching, expanded semantic factories, and immutable locator composition are not available in Maven Central `0.1.0`.

The development pipeline can filter a collection, select by position, search true descendants, or retain parents containing a match. Order is significant and scoping cannot escape to a sibling or document-global match, including for an XPath beginning with `//`. See [locators](elements/locators.md) and [collections](elements/collections.md).

### 3. Measurable recovery

!!! info "Coming in 0.2.0"
    `RetrySummary` and `RetryOutcomePolicy` are not available in Maven Central `0.1.0`.

Recovery retry means a physical operation failed and Lens scheduled another attempt. Condition polling and runner retry are separate. The summary shows recovered failures, while outcome policy can report, warn, or reject an otherwise passed session after evidence is written. See [flakiness and retry outcomes](observability/flakiness.md).

### 4. Trace, reports, and failure evidence

`UiTestLensSession`, trace, and HTML/JSON reporting are available in `0.1.0`. They preserve operations, status, failures, and attachments as a coherent timeline.

!!! info "Coming in 0.2.0"
    Automatic failure bundles and hardened exactly-once finalization are not available in Maven Central `0.1.0`.

For a final failed session, the development line can assemble screenshots, trace, report, context, runtime/configuration allowlists, network summary, manifest, and ZIP. Collection is best-effort and finalization never closes the driver. Video is attached, not recorded. See [trace](observability/trace.md), [reports](observability/reports.md), and [failure bundles](observability/failure-bundles.md).

### 5. Central protection for diagnostic text

!!! info "Coming in 0.2.0"
    Enabled-by-default central redaction is not available in Maven Central `0.1.0`.

One policy creates safe diagnostic copies before fan-out to HUD, trace, sinks, reports, network/API diagnostics, and failure-bundle text files. It preserves the structural exception type while leaving the original throwable untouched in the execution path. Screenshots/video are not pixel-redacted, optional page source/console are best-effort, auth-state files remain outside the transformation, and `disabled()` is an explicit opt-out. See [redaction](security/redaction.md).

## Advanced capabilities

| Capability | Boundary | Guide |
|---|---|---|
| WebDriver BiDi network diagnostics | Passive observation/correlation, not interception, mocking, CDP, or body capture. Manual events, waits, and assertions exist in `0.1.0`; BiDi lifecycle, safe snapshots, and HUD filtering are `0.2.0`. | [Network](advanced/network.md) |
| Authentication state | Captures/restores cookies and storage for a validated origin; cross-origin SSO restore is not automatic. Treat exported state as a secret. | [Auth state](advanced/auth-state.md) |
| Page and SPA waits | Ready-state waits plus an XHR/fetch tracker that sees only calls begun after installation. It is not full browser network idle. | [Waiting](elements/waiting.md) |
| React module | DOM-convention helpers for rerender windows and React Select; no React component-tree access or universal design-system guarantee. | [React](integrations/react.md) |
| API overlay | Displays caller-supplied API previews; it does not capture browser traffic. | [Visual helpers](advanced/visual-helpers.md) |
| JUnit 5 / TestNG adapters | Development-line `0.2.0` artifacts for per-invocation lifecycle ownership; not available in Maven Central `0.1.0`. | [Framework integration](framework-integration.md) |

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
