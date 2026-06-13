# UI Test Lens runtime JS audit

This audit captures the state after extracting the main browser runtime pieces into `src/main/resources/uitestlens/runtime/`.

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
| `PageWaits` | wait message writes | legacy alias / state bridge | Temporarily yes | Centralize wait state writes behind a small helper or wait runtime bridge. |
| `PageWaits` | `waitForReadyState*` | Selenium page query | Yes | Keep inline; these are simple browser state reads. |
| `PageWaits` | network tracker install | candidate for extraction | Not long term | Extract to `network-tracker.js` or a page-observability helper. |
| `PageWaits` | DOM stable mutation observer | candidate for extraction | Not long term | Extract to a small `dom-stability.js` helper if it grows or needs tests. |
| `PopupDetector` | popup scanning and close-button detection | popup/blocking overlay heuristic | Not long term | Move to a dedicated heuristics runtime or Selenium adapter helper. |
| `BlockingOverlayHelper` | overlay detection and configured close button | popup/blocking overlay heuristic | Not long term | Merge conceptually with `PopupDetector` or create a `blocking-overlay.js` helper. |
| `TargetResolverActions` | label/input and file-input resolution | target resolver | Not long term | Extract to a target resolver runtime after stabilizing selector/label semantics. |
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
| `window.__seleniumLastWaitMessage` | Alias for `window.__uiTestLens.state.wait.lastMessage`. | HUD/wait diagnostics still synchronize with it. | Yes, after `PageWaits` and `JsOverlayDebug` use only primary state. |
| `window.__seleniumLastWaitElapsedMs` | Alias for `window.__uiTestLens.state.wait.lastElapsedMs`. | Wait elapsed diagnostics still synchronize with it. | Yes, after wait state bridge cleanup. |
| `window.__seleniumActiveRequests` | Alias for `window.__uiTestLens.state.network.activeRequests`. | Network wait tracker still exposes legacy state. | Yes, after network tracker extraction. |
| `window.__seleniumNetworkTrackerInstalled` | Alias for `window.__uiTestLens.state.network.trackerInstalled`. | Prevents double-installing the network tracker across old/new code paths. | Yes, after network tracker extraction. |
| `window.__seleniumApiModal` | Alias for `window.__uiTestLens.modules.apiOverlay`. | `ApiOverlayPanel` and some `JsOverlayDebug` methods still call the legacy name. | Yes, after Java bridge calls move to `modules.apiOverlay`. |
| `selenium-hud-panel`, `selenium-hud-step`, `selenium-hud-logs` | Legacy HUD DOM IDs kept inside `hud-panel.js`. | CSS/DOM compatibility and existing direct lookups in a few snippets. | Maybe; rename only with migration aliases. |
| `selenium-assert-badge`, `selenium-overlay-assert` | Legacy assertion badge class names kept inside `assertion-badges.js`. | Visual compatibility and downstream selectors. | Maybe; rename only with migration aliases. |
| `target.__seleniumAssertContainer` | Per-element assertion badge container reference. | Preserves stacking behavior and cleanup semantics. | Maybe; replace with symbol/data map in a breaking runtime cleanup. |

## Resource fallback policy

Primary runtime resources live under:

```text
uitestlens/runtime/...
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

## Recommended next extraction/refactor steps

1. `PageWaits` state write cleanup: centralize wait/network state bridge and reduce direct legacy writes.
2. `PopupDetector` / `BlockingOverlayHelper` as a dedicated heuristics module, with a shared JS runtime helper if needed.
3. `TargetResolverActions` JS cleanup: extract label/input/file target resolution only after target semantics are documented.
4. API overlay bridge cleanup: move Java bridge calls from `window.__seleniumApiModal` to `window.__uiTestLens.modules.apiOverlay` while preserving alias fallback.
5. Optional `overlay-root.js` only if root bootstrap grows beyond the current tiny `OverlayRootManager` script.
6. Selenium `WebDriverListener` adapter for action-level observability without requiring direct helper calls.
7. Multi-module split after runtime/resource contracts and package-level boundaries are stable.
