# UI Test Lens module boundaries

## Current package boundary matrix

The first Maven split is now in place:

- `ui-test-lens-core` contains `core.logging`, `core.logging.export`, and `core.browser.BrowserScriptExecutor`.
- `ui-test-lens-overlay` contains runtime resources, runtime bridge/loaders, `OverlayRootManager`, `HudPanel`, `ApiOverlayPanel`, `OverlayConfig`, and `HudPosition`.
- `ui-test-lens-selenium` contains the Selenium facade/actions module and owns Selenium actions, waits, popup helpers, API call actions, and `SeleniumBrowserScriptExecutor`.
- `ui-test-lens-react` contains React-safe helpers.
- `ui-test-lens` is now an all-in-one compatibility artifact that depends on core, overlay, selenium, and react modules.
- `ui-test-lens-examples` contains compile-checked examples and is not a runtime dependency for users.

The latest post-split stabilization report is:

```text
docs/ui-test-lens-module-stabilization-report.md
```

Current artifact usage matrix:

| Use case | Artifact |
| -------- | -------- |
| Event logging/export model only | `ui-test-lens-core` |
| Browser overlay runtime bridge only | `ui-test-lens-overlay` |
| Selenium tests with overlay/actions/waits | `ui-test-lens-selenium` |
| React-safe Selenium helpers | `ui-test-lens-react` |
| Simple all-in-one local usage | `ui-test-lens` |
| Compile-checked examples | `ui-test-lens-examples` |

| Package | Classes | Selenium dependency? | Runtime JS dependency? | Proposed module | Notes |
| ------- | ------: | -------------------: | ---------------------: | --------------- | ----- |
| `io.github.mmaciekk111.uitestlens` | 3 | Mixed across modules | Yes | `ui-test-lens-overlay` plus `ui-test-lens-selenium` | `OverlayConfig` moved to overlay. `JsOverlayDebug` and `OverlayWait` moved to `ui-test-lens-selenium`. |
| `io.github.mmaciekk111.uitestlens.actions` | 7 | Yes | Yes | `ui-test-lens-selenium` | Selenium action helpers with overlay and event instrumentation. |
| `io.github.mmaciekk111.uitestlens.api` | 6 | Mixed across modules | Yes | Overlay, selenium, and future `ui-test-lens-api-overlay` | `ApiOverlayJs` and `ApiOverlayPanel` are in overlay. API action/context/plan/rule classes moved to `ui-test-lens-selenium`. |
| `io.github.mmaciekk111.uitestlens.core` | 14 | Mixed across modules | Yes | Overlay plus selenium | Runtime bridge loaders and `OverlayRootManager` are in overlay. `PageWaits`, `PopupDetector`, `BlockingOverlayHelper`, and `Guards` moved to `ui-test-lens-selenium`. |
| `io.github.mmaciekk111.uitestlens.core.browser` | 3 | Mixed across modules | No | `ui-test-lens-core` and selenium | `BrowserScriptExecutor` is in core. `SeleniumBrowserScriptExecutor` and `OverlayBrowserScriptExecutors` live in `ui-test-lens-selenium`; overlay has no Selenium helper. |
| `io.github.mmaciekk111.uitestlens.core.logging` | 9 | No | No | `ui-test-lens-core` | Moved to `ui-test-lens-core`; event model and sinks are JDK-only. |
| `io.github.mmaciekk111.uitestlens.core.logging.export` | 5 | No | No | `ui-test-lens-core` | Moved to `ui-test-lens-core`; text, JSON, and HTML exporters are JDK-only. |
| `io.github.mmaciekk111.uitestlens.hud` | 2 | No | Yes | `ui-test-lens-overlay` | Moved to overlay. `HudPanel` uses `BrowserScriptExecutor`; Selenium construction is provided by `SeleniumOverlayFactory`. |
| `io.github.mmaciekk111.uitestlens.react` | 4+ | Yes | Indirect | `ui-test-lens-react` | React helpers and React-aware actionability depend on Selenium and adapt `JsOverlayDebug` through `ReactSupport`; Selenium does not depend on this module. |
| `io.github.mmaciekk111.uitestlens.scroll` | 2 | No | No | Core or overlay decision | Neutral scroll edge enums used by Selenium scroll actions and runtime bridge code. |
| `io.github.mmaciekk111.uitestlens.utils` | 1 | No | Resource loading only | Core or overlay decision | `JsResources` is JDK-only, but most current usage is runtime resource loading. |

## Overlay-ready packages

- Runtime JavaScript resources in `ui-test-lens-overlay/src/main/resources/uitestlens/runtime`.
- Runtime bridge loaders in `core/*Js`: `ApiOverlayJs`, `WaitHudJs`, `HighlightJs`, `TypeHintJs`, `ScrollArrowJs`, `HudPanelJs`, and `AssertionBadgesJs`.
- `OverlayRootManager`, because it now uses only `BrowserScriptExecutor` in overlay.
- `HudPanel`, because it now uses `BrowserScriptExecutor` internally.
- `ApiOverlayPanel`, partially: it uses `BrowserScriptExecutor` internally and has no Selenium imports, but API overlay ownership still needs a separate module decision.
- `BrowserScriptExecutor`, as the neutral contract needed by overlay bridge code.

## Selenium-bound packages

- Root facade and waits: `JsOverlayDebug`, `OverlayWait`.
- `actions`, including highlight, typing, smart click/input, scroll, target resolver, and visual assertions.
- Selenium wait and guard helpers: `PageWaits`, `Guards`.
- Popup and overlay heuristics: `PopupDetector`, `BlockingOverlayHelper`.
- Configurable blocking overlay policy: `selenium.overlay.OverlayPolicy`, `OverlayHandler`, `OverlayAction`, and `OverlayPolicyExecutor`.
- Selenium actionability checks: `selenium.actionability.ActionabilityChecker`, options, reports, status/failure enums, and Selenium-only page query scripts.
- `SeleniumBrowserScriptExecutor`, `OverlayBrowserScriptExecutors`, and `SeleniumOverlayFactory` are in `ui-test-lens-selenium`.
- React-safe call sites moved to `ReactSupport` in `ui-test-lens-react`.
- React-aware actionability checks live in `ui-test-lens-react` and layer on top of Selenium actionability.

## Core-ready packages

- `core.logging`.
- `core.logging.export`.
- `core.browser.BrowserScriptExecutor`.
- Neutral runtime constants in `UiTestLensRuntimeNames`, subject to ownership decision.
- Neutral enums such as `ScrollElementEdge`, `ScrollViewportEdge`, and possibly `HudPosition`.

## Still mixed / requires decision

- `core`: contains both neutral runtime names/loaders and Selenium-bound waits, guards, popup heuristics, and adapters.
- `api`: mixes neutral API overlay models with browser panel bridge code.
- `OverlayConfig`: now lives in overlay; a later config split may separate core-safe values from overlay-specific values.
- `HudPosition`: now lives in overlay.
- `JsResources`: now lives in core as a neutral resource loader, while runtime resource tests live in overlay.
- `UiTestLensRuntimeNames`: neutral strings, but mostly browser runtime concepts.
- `ScriptExecutor`: historical empty placeholder superseded by `BrowserScriptExecutor`; should be removed or reconciled in a separate cleanup.

## Next safe refactors before actual split

1. Keep `ui-test-lens-core` Selenium-free as more neutral types are considered for migration.
2. Keep Selenium actions, waits, popup heuristics, and the current facade inside the Selenium boundary.
3. Continue moving bridge callers toward `BrowserScriptExecutor` where this does not change behavior.
4. Leave API overlay as either an overlay sub-area or a future optional module until ownership is explicit.
5. Use React-aware readiness from `ui-test-lens-react` in future locator/action APIs without moving React logic back into `ui-test-lens-selenium`.
