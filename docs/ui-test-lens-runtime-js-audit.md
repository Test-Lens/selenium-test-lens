# UI Test Lens runtime JS audit

This audit captures the state after extracting the main browser runtime pieces into `ui-test-lens-overlay/src/main/resources/uitestlens/runtime/`.

`BrowserScriptExecutor` now provides a neutral Java contract for browser script execution from `ui-test-lens-core`. `SeleniumBrowserScriptExecutor`, `OverlayBrowserScriptExecutors`, and `SeleniumOverlayFactory` live in `ui-test-lens-selenium`. `ui-test-lens-overlay` no longer imports Selenium and uses `BrowserScriptExecutor` as its primary API. Runtime bridge loaders accept the neutral executor, and `HudPanel`, `ApiOverlayPanel`, and `OverlayRootManager` use the neutral executor internally. Selenium action/PageWaits/popup helpers now live in `ui-test-lens-selenium` and still use direct Selenium execution where appropriate.

## Current runtime resources

| Resource | Module namespace | Loader class | Legacy fallback | Tests |
| -------- | ---------------- | ------------ | --------------- | ----- |
| `api-overlay.js` | `window.__uiTestLens.modules.apiOverlay`, alias `apiModal` | `ApiOverlayJs` | `selenium/api-overlay.js` | `ApiOverlayJsTest`, `JsResourcesTest` |
| `wait-hud.js` | `window.__uiTestLens.modules.waitHud` | `WaitHudJs` | `selenium/wait/WaitHud.js` | `WaitHudJsTest`, `JsResourcesTest` |
| `highlight.js` | `window.__uiTestLens.modules.highlight` | `HighlightJs` | `selenium/highlight.js` | `HighlightJsTest`, `JsResourcesTest` |
| `type-hint.js` | `window.__uiTestLens.modules.typeHint` | `TypeHintJs` | `selenium/type-hint.js` | `TypeHintJsTest`, `JsResourcesTest` |
| `scroll-arrow.js` | `window.__uiTestLens.modules.scrollArrow` | `ScrollArrowJs` | `selenium/scroll-arrow.js` | `ScrollArrowJsTest`, `JsResourcesTest` |
| `hud-panel.js` | `window.__uiTestLens.modules.hud` | `HudPanelJs` | `selenium/hud-panel.js` | `HudPanelJsTest`, `JsResourcesTest` |
| `assertion-badges.js` | `window.__uiTestLens.modules.assertionBadges` | `AssertionBadgesJs` | `selenium/assertion-badges.js` | `AssertionBadgesJsTest`, `JsResourcesTest` |

All listed resources are non-empty, use the primary `window.__uiTestLens` namespace, and keep compatibility with legacy `window.__selenium...` globals where current Java or downstream code may still depend on them. No `TODO` or `FIXME` markers were found in the runtime resource files during this audit.

## Remaining inline JavaScript

| Class | Method/area | Inline JS type | Keep inline? | Recommended next action |
| ----- | ----------- | -------------- | ------------ | ----------------------- |
| `OverlayRootManager` | `ensureRootExists`, `clearRoot` | overlay runtime bootstrap, legacy alias sync | Yes | Cleanup completed: primary root state is `window.__uiTestLens.state.overlay.root`; consider `overlay-root.js` only if bootstrap grows. |
| `PageWaits` | wait message writes | wait state bridge | Yes | Cleanup completed: primary state is `window.__uiTestLens.state.wait.lastMessage`; legacy alias remains synchronized. |
| `PageWaits` | `waitForReadyState*` | Selenium page query | Yes | Keep inline; these are simple browser state reads. |
| `PageWaits` | network tracker install | network state bridge / candidate for extraction | Not long term | State cleanup completed: primary state is `window.__uiTestLens.state.network.activeRequests`; extract to `network-tracker.js` only if the script grows. |
| `PageWaits` | DOM stable mutation observer | DOM stability helper | Temporarily yes | `window.__uiTestLens.state.dom` is initialized, but observer state remains element-local to preserve semantics; extract to `dom-stability.js` only if it grows. |
| `PopupDetector` | popup scanning and close-button detection | popup/blocking overlay heuristic | Not long term | Cleanup completed: scripts are named helper methods with unit marker tests; move to a dedicated heuristics runtime only after behavior is documented. |
| `BlockingOverlayHelper` | overlay detection and configured close button | popup/blocking overlay heuristic | Not long term | Cleanup completed: scripts are named helper methods with unit marker tests; merge conceptually with `PopupDetector` or create a `blocking-overlay.js` helper later. |
| `TargetResolverActions` | click/file input resolution scripts | target resolver page query | Temporarily yes | Cleanup completed: scripts are named helper methods with unit marker tests; extract to a target resolver runtime only after selector/label semantics are documented. |
| `ReactSelectHelper` | react-select probing and option selection | React/SPA helper | Temporarily yes | Keep in React helper for now; later move to `ui-test-lens-react` adapter. |
| `HighlightActions` | module calls and click fallback snippets | bridge call / Selenium action | Yes | Bridge calls are fine; click fallback snippets can remain action-specific. |
| `TypingActions` | type hint call and small input focus/click snippets | bridge call / Selenium action | Yes | Bridge call is fine; keep small input actions inline unless they become shared. |
| `ScrollActions` | no-overlay animated scroll | Selenium action | Yes | This is scroll behavior, not visual runtime; keep inline unless shared. |
| `ScrollActions` | scroll arrow call | bridge call | Yes | Already delegates to `scroll-arrow.js`. |
| `HudPanel` | HUD init/log/step calls | bridge call | Yes | Already delegates to `hud-panel.js`. |
| `AssertActions` | assertion badge call | bridge call | Yes | Already delegates to `assertion-badges.js`; `OverlayAssertionResult` unchanged. |
| `ApiOverlayPanel` | API modal operations | bridge calls with legacy alias | Temporarily yes | Migrate Java bridge calls from `window.__seleniumApiModal` to `window.__uiTestLens.modules.apiOverlay` while keeping alias fallback. |
| `JsOverlayDebug` | wait HUD calls and wait state writes | bridge call / legacy alias | Temporarily yes | Replace remaining direct legacy writes with small helper methods after wait state cleanup. |
| `JsOverlayDebug` | API modal methods | bridge calls with legacy alias | Temporarily yes | Delegate to `ApiOverlayPanel` or primary API overlay module to remove duplication. |
| `Guards` | error page text extraction | Selenium page query | Yes | Keep inline; it is a small diagnostic query. |
| `OverlayWait` | covered-element check using `elementFromPoint` | Selenium page query | Yes | Keep inline; this is a small guard query. |

## Legacy namespace usage

| Legacy name | Current usage | Compatibility reason | Removal candidate? |
| ----------- | ------------- | -------------------- | ------------------ |
| `window.__seleniumOverlayRoot` | Legacy alias synchronized from `window.__uiTestLens.state.overlay.root`; used by runtime resources and older Java snippets. | Existing inline snippets and downstream code may still read it. | Yes, after all Java snippets use `state.overlay.root` or runtime APIs. |
| `window.__seleniumWaitHud` | Alias for `window.__uiTestLens.modules.waitHud`. | Current wait bridge and legacy consumers. | Yes, after wait HUD callers use `modules.waitHud`. |
| `window.__seleniumLastWaitMessage` | Alias synchronized from `window.__uiTestLens.state.wait.lastMessage`. | HUD/wait diagnostics still synchronize with it. | Yes, after all wait diagnostics use only primary state. |
| `window.__seleniumLastWaitElapsedMs` | Alias for `window.__uiTestLens.state.wait.lastElapsedMs`. | Wait elapsed diagnostics still synchronize with it. | Yes, after wait state bridge cleanup. |
| `window.__seleniumActiveRequests` | Alias synchronized from `window.__uiTestLens.state.network.activeRequests`. | Network wait tracker still exposes legacy state. | Yes, after network tracker extraction and a compatibility window. |
| `window.__seleniumNetworkTrackerInstalled` | Alias synchronized from `window.__uiTestLens.state.network.trackerInstalled`. | Prevents double-installing the network tracker across old/new code paths. | Yes, after network tracker extraction and a compatibility window. |
| `window.__seleniumApiModal` | Alias for `window.__uiTestLens.modules.apiOverlay`. | `ApiOverlayPanel` and some `JsOverlayDebug` methods still call the legacy name. | Yes, after Java bridge calls move to `modules.apiOverlay`. |
| `selenium-hud-panel`, `selenium-hud-step`, `selenium-hud-logs` | Legacy HUD DOM IDs kept inside `hud-panel.js`. | CSS/DOM compatibility and existing direct lookups in a few snippets. | Maybe; rename only with migration aliases. |
| `selenium-assert-badge`, `selenium-overlay-assert` | Legacy assertion badge class names kept inside `assertion-badges.js`. | Visual compatibility and downstream selectors. | Maybe; rename only with migration aliases. |
| `target.__seleniumAssertContainer` | Per-element assertion badge container reference. | Preserves stacking behavior and cleanup semantics. | Maybe; replace with symbol/data map in a breaking runtime cleanup. |

## Resource fallback policy

Primary runtime resources live under:

```text
ui-test-lens-overlay/src/main/resources/uitestlens/runtime/...
```

Legacy fallback resources use historical Selenium-oriented paths:

```text
selenium/...
```

Fallbacks stay for now because the project is still in a compatibility migration phase. They should be removed only after:

- all Java callers use `window.__uiTestLens.modules...` or `window.__uiTestLens.state...`,
- downstream consumers have a migration window,
- release notes identify the first version without legacy paths/globals,
- tests cover every runtime resource through the primary path.

## Target resolver cleanup

`TargetResolverActions` was reviewed after the runtime namespace migration.

Changes made:

- the click target resolver script was moved from the public method body into `clickTargetResolverScript()`,
- the file input resolver script was moved from the public method body into `fileInputResolverScript()`,
- unit tests now verify the script markers and fallback intent without Selenium or a browser.

What intentionally stayed the same:

- the resolver algorithm,
- fallback order,
- public methods,
- returned values,
- exception behavior,
- logger event semantics.

The resolver scripts do not currently need browser-global runtime state. They remain page-query snippets that inspect the supplied element, its descendants, label association, and ancestors. Because they do not store state, no `window.__uiTestLens.state...` key was introduced here, and no legacy `window.__selenium...` alias is needed.

The scripts were not moved to `src/main/resources/uitestlens/runtime/` in this stage because they are still tightly coupled to Selenium `WebElement` arguments and the current resolver semantics are not yet a stable runtime contract. A future extraction should first document target resolver semantics, selector escaping rules, privacy constraints for labels/text, and expected behavior for ambiguous matches.

## Popup and blocking overlay cleanup

`PopupDetector` and `BlockingOverlayHelper` were reviewed after the runtime namespace and event logger work.

Changes made:

- `PopupDetector` now exposes named script helpers for popup detection, global close button lookup, viewport-center overlay lookup, and close button lookup inside an overlay,
- `BlockingOverlayHelper` now exposes named script helpers for configured close button lookup, blocking overlay lookup over a target, and close button lookup inside the blocking overlay,
- unit tests verify key selectors, text keywords, visibility checks, and overlay heuristics without Selenium or a browser.

What intentionally stayed the same:

- popup and blocking overlay heuristics,
- selector lists,
- text keywords,
- fallback order,
- sleep duration after dismiss,
- public methods,
- coupling to `SmartClickActions`, `SmartInputActions`, and `HighlightActions`,
- exception handling behavior.

No new runtime globals were introduced. These scripts do not currently store browser state, so they do not need a `window.__uiTestLens.state...` key or a legacy `window.__selenium...` alias.

The scripts were not moved to `src/main/resources/uitestlens/runtime/` in this stage because they are still Selenium helper heuristics rather than a stable browser runtime contract. A future extraction should first consolidate `PopupDetector` and `BlockingOverlayHelper`, document selector and keyword policy, and decide whether popup/blocking overlay behavior belongs in Selenium adapter code or a shared heuristics runtime.

## Recommended next extraction/refactor steps

1. Optional `PageWaits` resource extraction: move network tracker and DOM stability scripts to resources only if they grow or need browser-level tests.
2. Optional `PopupDetector` / `BlockingOverlayHelper` consolidation as a dedicated heuristics module, with a shared JS runtime helper if needed.
3. Optional `TargetResolverActions` resource extraction only after target semantics and selector escaping rules are documented.
4. API overlay bridge cleanup: move Java bridge calls from `window.__seleniumApiModal` to `window.__uiTestLens.modules.apiOverlay` while preserving alias fallback.
5. Optional `overlay-root.js` only if root bootstrap grows beyond the current tiny `OverlayRootManager` script.
6. Selenium `WebDriverListener` adapter for action-level observability without requiring direct helper calls.
7. Multi-module split after runtime/resource contracts and package-level boundaries are stable.

## Browser script executor abstraction

The first split preparation step introduced:

- `io.github.mmaciekk111.uitestlens.core.browser.BrowserScriptExecutor`,
- `io.github.mmaciekk111.uitestlens.core.browser.SeleniumBrowserScriptExecutor` in `ui-test-lens-selenium`.

Runtime bridge loaders now use `inject(BrowserScriptExecutor)`. `OverlayRootManager`, `HudPanel`, and `ApiOverlayPanel` store the neutral executor internally and no longer keep `WebDriver` constructors in overlay. Selenium-facing construction is provided by `SeleniumOverlayFactory` in `ui-test-lens-selenium`.

Direct Selenium script execution intentionally remains in:

- `PageWaits`,
- `PopupDetector`,
- `BlockingOverlayHelper`,
- `TargetResolverActions`,
- Selenium actions except `HudPanel`,
- `JsOverlayDebug`,
- React helpers.

Those areas are behavior-heavy and should be migrated in smaller commits after the bridge layer is stable.
