# Public API boundary review

This review separates supported user APIs from implementation-shaped classes that are nevertheless binary-public today. It proposes work for a dedicated pre-1.0 compatibility review; **no visibility or behavior changes are made here**.

| Classification | Types | Meaning |
| --- | ---: | --- |
| `USER_API` | 28 | Normal facade, element/assertion/lifecycle/configuration types. |
| `ADVANCED_API` | 133 | Supported specialized diagnostics, models, exporters, auth/network/evidence, and React. |
| `LOW_LEVEL_API` | 20 | Lower-abstraction helpers that consumers may deliberately compose. |
| `INTERNAL_STYLE_PUBLIC` | 38 | Implementation-shaped binary surface; not recommended for consumer code. |

| Type | Module | Current classification | Why suspicious | Suggested future action |
| --- | --- | --- | --- | --- |
| `BrowserScriptExecutor` | core | INTERNAL_STYLE_PUBLIC | Browser adapter SPI used by overlay integration. | Keep public only if documented as a supported SPI; otherwise hide. |
| `TraceHtmlEscaper` | core | INTERNAL_STYLE_PUBLIC | Output-encoding implementation helper. | Package-private. |
| `TraceJsonWriter` | core | INTERNAL_STYLE_PUBLIC | JSON serialization implementation helper. | Package-private or refactor behind exporters. |
| `TraceReportSupport` | core | INTERNAL_STYLE_PUBLIC | Shared exporter constants/path mechanics. | Refactor behind exporter facade. |
| `JsResources` | core | INTERNAL_STYLE_PUBLIC | Classpath JavaScript resource loader. | Package-private. |
| `ApiOverlayJs` | overlay | INTERNAL_STYLE_PUBLIC | Raw JavaScript wrapper. | Package-private. |
| `ApiOverlayPanel` | overlay | INTERNAL_STYLE_PUBLIC | Concrete injected panel implementation. | Refactor behind facade. |
| `AssertionBadgesJs` | overlay | INTERNAL_STYLE_PUBLIC | Raw JavaScript resource wrapper. | Package-private. |
| `HighlightJs` | overlay | INTERNAL_STYLE_PUBLIC | Raw JavaScript resource wrapper. | Package-private. |
| `HudPanelJs` | overlay | INTERNAL_STYLE_PUBLIC | Raw JavaScript resource wrapper. | Package-private. |
| `OverlayRootManager` | overlay | INTERNAL_STYLE_PUBLIC | DOM lifecycle implementation detail. | Refactor behind overlay facade. |
| `ScrollArrowJs` | overlay | INTERNAL_STYLE_PUBLIC | Raw JavaScript resource wrapper. | Package-private. |
| `TypeHintJs` | overlay | INTERNAL_STYLE_PUBLIC | Raw JavaScript resource wrapper. | Package-private. |
| `UiTestLensRuntimeNames` | overlay | INTERNAL_STYLE_PUBLIC | Internal DOM/runtime identifier constants. | Package-private. |
| `WaitHudJs` | overlay | INTERNAL_STYLE_PUBLIC | Raw JavaScript resource wrapper. | Package-private. |
| `ApiCallActions` | selenium | INTERNAL_STYLE_PUBLIC | Facade implementation used by `apiCallWithModal`. | Refactor behind `TestLens`. |
| `ApiOverlayContext` | selenium | INTERNAL_STYLE_PUBLIC | Data carrier for overlay planning internals. | Package-private. |
| `ApiOverlayPlan` | selenium | INTERNAL_STYLE_PUBLIC | Internal execution plan. | Package-private. |
| `ApiOverlayRule` | selenium | INTERNAL_STYLE_PUBLIC | Internal rule representation. | Package-private. |
| `OverlayBrowserScriptExecutors` | selenium | INTERNAL_STYLE_PUBLIC | Adapter selection/factory mechanics. | Package-private. |
| `SeleniumBrowserScriptExecutor` | selenium | INTERNAL_STYLE_PUBLIC | Concrete browser adapter implementation. | Hide behind supported SPI/factory. |
| `OverlayLogger` | selenium | INTERNAL_STYLE_PUBLIC | Internal bridge between legacy overlay and structured logging. | Refactor behind facade or supported SPI. |
| `ScriptExecutor` | selenium | INTERNAL_STYLE_PUBLIC | Raw JavaScript execution helper. | Package-private; raw Selenium remains available. |
| `UiAssertionReporter` | selenium | INTERNAL_STYLE_PUBLIC | Assertion event plumbing. | Expose a deliberate reporter SPI or hide. |
| `UiExpect.ElementProbe` | selenium | INTERNAL_STYLE_PUBLIC | Test/integration seam embedded in assertion implementation. | Package-private or move to test support. |
| `UiExpect.ElementProbeResult` | selenium | INTERNAL_STYLE_PUBLIC | Companion test seam data. | Package-private or move to test support. |
| `UiExpect.VisibilityProbe` | selenium | INTERNAL_STYLE_PUBLIC | Test/integration seam embedded in assertion implementation. | Package-private or supported SPI. |
| `UiExpect.VisibilityProbeResult` | selenium | INTERNAL_STYLE_PUBLIC | Companion test seam data. | Package-private or move to test support. |
| `BusinessAssertionReporter` | selenium | INTERNAL_STYLE_PUBLIC | Event plumbing rather than task API. | Expose supported reporter SPI or hide. |
| `UiLocatorResolver` | selenium | INTERNAL_STYLE_PUBLIC | Resolution implementation behind `UiLocator`. | Package-private. |
| `UiLocatorResult` | selenium | INTERNAL_STYLE_PUBLIC | Result model is not returned by the recommended locator facade. | Package-private or expose deliberately through a supported diagnostic result API. |
| `UiLocatorResult.Builder` | selenium | INTERNAL_STYLE_PUBLIC | Builder for an otherwise internal-style result. | Package-private with its result. |
| `UiLocatorFailureReason` | selenium | INTERNAL_STYLE_PUBLIC | Failure enum belongs to the unused/internal result path. | Package-private unless surfaced by the facade. |
| `UiLocatorStatus` | selenium | INTERNAL_STYLE_PUBLIC | Status enum belongs to the unused/internal result path. | Package-private unless surfaced by the facade. |
| `OverlayPolicyExecutor` | selenium | INTERNAL_STYLE_PUBLIC | Executor behind public policy configuration. | Refactor behind policy/facade. |
| `UiStepContext` | selenium | INTERNAL_STYLE_PUBLIC | Mutable/current-step plumbing. | Package-private. |
| `UiStepReporter` | selenium | INTERNAL_STYLE_PUBLIC | Step event plumbing. | Expose supported reporter SPI or hide. |
| `UiStepScope` | selenium | INTERNAL_STYLE_PUBLIC | Scope implementation used by step execution. | Package-private or keep only as deliberate advanced API. |

See the versioned [`public-api-classification.csv`](public-api-classification.csv) for the classification of every binary-public type. New public types fail the API check until explicitly classified.

## Supported facade with legacy construction seams

`JsOverlayDebug` remains `ADVANCED_API` because its documented locator, wait, visual, popup, assertion, evidence, trace, and API-overlay families are supported for deliberate advanced use. Its three constructors that accept `ApiOverlayPanel`, `ApiCallActions`, `Guards`, and logger bridge implementations are legacy implementation-shaped surface. A future cleanup should retain the `WebDriver` and `WebDriver`/`OverlayConfig` constructors and move component injection behind an internal factory or a deliberately documented extension SPI.
