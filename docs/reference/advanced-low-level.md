# Advanced and low-level public API

These types are public in the current artifacts but are not the recommended consumer entry point. Their exposure means compatibility changes still matter; use them only for integrations that cannot be built with `TestLens`, `UiLocator`, configuration, or documented advanced services.

## Advanced supported surfaces

- [`JsOverlayDebug`](../advanced/js-overlay-debug.md), `OverlayWait`, actionability checkers/models.
- auth, network, evidence, overlay-policy services and exporters.
- trace/log model, sinks, and exporters.
- React module helpers.
- legacy/direct action helpers in `io.github.testlens.actions.*`.

## Internal-style / likely accidental exposure

- JavaScript wrappers and resource internals: `AssertionBadgesJs`, `HighlightJs`, `HudPanelJs`, `ScrollArrowJs`, `TypeHintJs`, `WaitHudJs`, `ApiOverlayJs`, `JsResources`.
- browser/overlay infrastructure: `BrowserScriptExecutor`, `SeleniumBrowserScriptExecutor`, `OverlayBrowserScriptExecutors`, `OverlayRootManager`, `UiTestLensRuntimeNames`, `ScriptExecutor`, `OverlayLogger`.
- resolver/reporter mechanics: `UiLocatorResolver`, `UiAssertionReporter`, `UiStepReporter`, `BusinessAssertionReporter`, policy executors and low-level contexts.
- HTML/JSON implementation helpers: `TraceHtmlEscaper`, `TraceJsonWriter`, `TraceReportSupport`.
- low-level API overlay planning/context classes: `ApiCallActions`, `ApiOverlayContext`, `ApiOverlayPlan`, `ApiOverlayRule`, `ApiOverlayPanel`.

No stability promise beyond the published Java surface is implied here. These APIs accept raw scripts/selectors/browser objects, can mutate page decoration, and may bypass high-level retry, trace, HUD, evidence, or redaction conventions. Prefer raw Selenium itself when the desired operation is fundamentally a Selenium/CDP/BiDi concern.

The [exhaustive catalog](public-api-catalog.md) is the authoritative list of their currently compiled public signatures.
