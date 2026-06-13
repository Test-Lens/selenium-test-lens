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
| `PageWaits` | wait message writes | wait state bridge | Yes | Cleanup completed: primary state is `window.__uiTestLens.state.wait.lastMessage`; legacy alias remains synchronized. |
| `PageWaits` | `waitForReadyState*` | Selenium page query | Yes | Keep inline; these are simple browser state reads. |
| `PageWaits` | network tracker install | network state bridge / candidate for extraction | Not long term | State cleanup completed: primary state is `window.__uiTestLens.state.network.activeRequests`; extract to `network-tracker.js` only if the script grows. |
| `PageWaits` | DOM stable mutation observer | DOM stability helper | Temporarily yes | `window.__uiTestLens.state.dom` is initialized, but observer state remains element-local to preserve semantics; extract to `dom-stability.js` only if it grows. |
| `PopupDetector` | popup scanning and close-button detection | popup/blocking overlay heuristic | Not long term | Move to a dedicated heuristics runtime or Selenium adapter helper. |
| `BlockingOverlayHelper` | overlay detection and configured close button | popup/blocking overlay heuristic | Not long term | Merge conceptually with `PopupDetector` or create a `blocking-overlay.js` helper. |
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

## Recommended next extraction/refactor steps

1. Optional `PageWaits` resource extraction: move network tracker and DOM stability scripts to resources only if they grow or need browser-level tests.
2. `PopupDetector` / `BlockingOverlayHelper` as a dedicated heuristics module, with a shared JS runtime helper if needed.
3. Optional `TargetResolverActions` resource extraction only after target semantics and selector escaping rules are documented.
4. API overlay bridge cleanup: move Java bridge calls from `window.__seleniumApiModal` to `window.__uiTestLens.modules.apiOverlay` while preserving alias fallback.
5. Optional `overlay-root.js` only if root bootstrap grows beyond the current tiny `OverlayRootManager` script.
6. Selenium `WebDriverListener` adapter for action-level observability without requiring direct helper calls.
7. Multi-module split after runtime/resource contracts and package-level boundaries are stable.
