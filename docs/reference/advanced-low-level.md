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
- browser/overlay infrastructure still crossing artifact boundaries: `SeleniumBrowserScriptExecutor`, `OverlayRootManager`, `UiTestLensRuntimeNames`, and `OverlayLogger`.
- deferred assertion probes, policy execution, and step scope construction.
- JSON/report implementation helpers shared across core packages: `TraceJsonWriter` and `TraceReportSupport`.
- `ApiOverlayPanel`, whose implementation is still shared by overlay and Selenium artifacts.

`BrowserScriptExecutor` is intentionally supported as a `LOW_LEVEL_API` SPI for framework-neutral execution of overlay scripts. Local reporters, locator result/resolver types, unused overlay plans, `ScriptExecutor`, `OverlayBrowserScriptExecutors`, `ApiCallActions`, `UiStepContext`, and `TraceHtmlEscaper` are no longer binary-public. See the [boundary review](public-api-boundary-review.md) for the complete decision table and deferred work.

No stability promise beyond the published Java surface is implied here. These APIs accept raw scripts/selectors/browser objects, can mutate page decoration, and may bypass high-level retry, trace, HUD, evidence, or redaction conventions. Prefer raw Selenium itself when the desired operation is fundamentally a Selenium/CDP/BiDi concern.

The [exhaustive catalog](public-api-catalog.md) is the authoritative list of their currently compiled public signatures.
