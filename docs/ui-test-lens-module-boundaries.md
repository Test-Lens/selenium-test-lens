# UI Test Lens module boundaries

## Current package boundary matrix

| Package | Classes | Selenium dependency? | Runtime JS dependency? | Proposed module | Notes |
| ------- | ------: | -------------------: | ---------------------: | --------------- | ----- |
| `io.github.mmaciekk111.uitestlens` | 3 | Yes | Yes, through facade and waits | `ui-test-lens-selenium` | `JsOverlayDebug` and `OverlayWait` are Selenium-bound. `OverlayConfig` still needs placement decision. |
| `io.github.mmaciekk111.uitestlens.actions` | 7 | Yes | Yes | `ui-test-lens-selenium` | Selenium action helpers with overlay and event instrumentation. |
| `io.github.mmaciekk111.uitestlens.api` | 6 | Partial | Yes | Future `ui-test-lens-api-overlay` or overlay first | `ApiOverlayPanel` uses `BrowserScriptExecutor` internally but keeps a WebDriver constructor. Models and context are mostly neutral but API-overlay-specific. |
| `io.github.mmaciekk111.uitestlens.core` | 14 | Mixed | Yes | Split across core, overlay, and selenium | Runtime bridge loaders are overlay candidates. `PageWaits`, `PopupDetector`, `BlockingOverlayHelper`, and `Guards` are Selenium-bound. |
| `io.github.mmaciekk111.uitestlens.core.browser` | 2 | Mixed | No | Core contract plus Selenium adapter | `BrowserScriptExecutor` is neutral. `SeleniumBrowserScriptExecutor` belongs with the Selenium module after the split. |
| `io.github.mmaciekk111.uitestlens.core.logging` | 9 | No | No | `ui-test-lens-core` | Event model and sinks are JDK-only. |
| `io.github.mmaciekk111.uitestlens.core.logging.export` | 5 | No | No | `ui-test-lens-core` | Text, JSON, and HTML exporters are JDK-only. |
| `io.github.mmaciekk111.uitestlens.hud` | 2 | Compatibility only | Yes | `ui-test-lens-overlay` | `HudPanel` uses `BrowserScriptExecutor` internally and keeps WebDriver constructors for compatibility. |
| `io.github.mmaciekk111.uitestlens.react` | 2 | Yes | Indirect | `ui-test-lens-react` | React helpers still depend on Selenium and the current facade. |
| `io.github.mmaciekk111.uitestlens.scroll` | 2 | No | No | Core or overlay decision | Neutral scroll edge enums used by Selenium scroll actions and runtime bridge code. |
| `io.github.mmaciekk111.uitestlens.utils` | 1 | No | Resource loading only | Core or overlay decision | `JsResources` is JDK-only, but most current usage is runtime resource loading. |

## Overlay-ready packages

- Runtime JavaScript resources in `src/main/resources/uitestlens/runtime`.
- Runtime bridge loaders in `core/*Js`: `ApiOverlayJs`, `WaitHudJs`, `HighlightJs`, `TypeHintJs`, `ScrollArrowJs`, `HudPanelJs`, and `AssertionBadgesJs`.
- `OverlayRootManager`, because it now uses `BrowserScriptExecutor` internally while preserving WebDriver compatibility constructors.
- `HudPanel`, because it now uses `BrowserScriptExecutor` internally.
- `ApiOverlayPanel`, partially: it uses `BrowserScriptExecutor` internally, but API overlay ownership still needs a separate module decision.
- `BrowserScriptExecutor`, as the neutral contract needed by overlay bridge code.

## Selenium-bound packages

- Root facade and waits: `JsOverlayDebug`, `OverlayWait`.
- `actions`, including highlight, typing, smart click/input, scroll, target resolver, and visual assertions.
- Selenium wait and guard helpers: `PageWaits`, `Guards`.
- Popup and overlay heuristics: `PopupDetector`, `BlockingOverlayHelper`.
- `SeleniumBrowserScriptExecutor`.
- React helpers: `ReactSafeExecutor`, `ReactSelectHelper`.

## Core-ready packages

- `core.logging`.
- `core.logging.export`.
- Neutral runtime constants in `UiTestLensRuntimeNames`, subject to ownership decision.
- Neutral enums such as `ScrollElementEdge`, `ScrollViewportEdge`, and possibly `HudPosition`.

## Still mixed / requires decision

- `core`: contains both neutral runtime names/loaders and Selenium-bound waits, guards, popup heuristics, and adapters.
- `api`: mixes neutral API overlay models with browser panel bridge code.
- `OverlayConfig`: could live in core, but currently references HUD/overlay configuration concepts.
- `HudPosition`: simple enum, but it is HUD-specific.
- `JsResources`: JDK-only, but semantically tied to runtime resources.
- `UiTestLensRuntimeNames`: neutral strings, but mostly browser runtime concepts.
- `ScriptExecutor`: historical empty placeholder superseded by `BrowserScriptExecutor`; should be removed or reconciled in a separate cleanup.

## Next safe refactors before actual split

1. Keep pure logging/export classes inside the future core boundary.
2. Keep runtime resources and `BrowserScriptExecutor` bridge loaders inside the overlay boundary.
3. Keep Selenium actions, waits, popup heuristics, and the current facade inside the Selenium boundary.
4. Continue moving bridge classes away from direct Selenium execution where this does not change behavior.
5. Split React helpers only after direct `JsOverlayDebug` coupling is reduced.
6. Leave API overlay as either an overlay sub-area or a future optional module until ownership is explicit.

