# UI Test Lens visual overlay and HUD

## Scope

This document summarizes the current visual debugging layer in UI Test Lens 0.1.0-SNAPSHOT: runtime overlay resources, HUD APIs, element highlighting, wait diagnostics, assertion badges, API overlay panels, type hints, scroll arrows, and the configuration that is currently public.

The visual layer is split across modules:

- `ui-test-lens-overlay` owns Selenium-free runtime JavaScript resources and browser-script bridge classes.
- `ui-test-lens-selenium` owns the Selenium facade `JsOverlayDebug` and WebDriver-facing actions that call those resources.

## Runtime resources

The overlay module currently contains these runtime resources:

| Resource | Runtime module | Java loader or bridge |
|---|---|---|
| `api-overlay.js` | `window.__uiTestLens.modules.apiOverlay` | `ApiOverlayJs`, `ApiOverlayPanel` |
| `wait-hud.js` | `window.__uiTestLens.modules.waitHud` | `WaitHudJs` |
| `highlight.js` | `window.__uiTestLens.modules.highlight` | `HighlightJs` |
| `type-hint.js` | `window.__uiTestLens.modules.typeHint` | `TypeHintJs` |
| `scroll-arrow.js` | `window.__uiTestLens.modules.scrollArrow` | `ScrollArrowJs` |
| `hud-panel.js` | `window.__uiTestLens.modules.hud` | `HudPanelJs`, `HudPanel` |
| `assertion-badges.js` | `window.__uiTestLens.modules.assertionBadges` | `AssertionBadgesJs` |

The primary browser namespace is `window.__uiTestLens`. Legacy `window.__selenium...` aliases are still maintained for compatibility with older snippets and historical helper code.

## HUD

`HudPanel` renders a small browser-side status panel. The Selenium facade exposes the common HUD operations through:

```java
JsOverlayDebug overlay = new JsOverlayDebug(driver);

overlay.initHud("Checkout test", "local");
overlay.setStep("Open checkout");
overlay.hudLog("info", "Checkout opened", "local");
```

`setStep(...)` only updates the visible HUD label. `step(...)` is a higher-level DSL that executes code, measures duration, emits events, and can also update the HUD.

## Overlay root

`OverlayRootManager` creates and clears the shared overlay root. Runtime resources attach their visual DOM to that root. The current primary state key is:

```text
window.__uiTestLens.state.overlay.root
```

The legacy alias `window.__seleniumOverlayRoot` remains synchronized for compatibility.

Use `clearDebugArtifacts()` from `JsOverlayDebug` to remove current overlay artifacts:

```java
overlay.clearDebugArtifacts();
```

This clears the overlay root; it does not clear test logs, trace sessions, screenshots, videos, or exported files.

## Element highlighting

Element highlighting is driven by `highlight.js` through `HighlightActions`. Public Selenium facade methods include:

```java
overlay.highlightClick(element, "SAVE");
overlay.highlightElement(element, "Target");
overlay.highlightParent(element, "Parent");
overlay.highlightAncestor(element, 2, "Ancestor");
overlay.highlightClosest(element, ".card", "Card");
overlay.highlightThenClick(element, "Submit");
```

`UiLocator` can also highlight before action through `UiLocatorOptions.highlightBeforeAction(true)`, which is enabled by default.

## Wait HUD

The wait HUD is driven by `wait-hud.js`. Public entry points include:

```java
overlay.waitHudStart("Waiting for checkout");
overlay.waitHudStop("OK", 250);
overlay.forceHideWaitHud();
overlay.showWaitIndicator("Saving");
overlay.hideWaitIndicator();
overlay.showLastWaitInHud();
```

Wait helpers such as `OverlayWait` and page wait methods can update the HUD as part of their diagnostic flow.

## Assertion badges

Assertion badges are driven by `assertion-badges.js` and used by legacy/visual assertion actions. They are separate from retryable `UiExpect` assertions, although both can emit logger events.

Visual assertion methods include examples such as:

```java
overlay.assertTextEquals(element, "Saved", "toast text", true);
overlay.assertVisible(element, "modal visible", true);
```

The final `boolean badge` parameter controls whether a visual badge is drawn for that assertion call.

## API overlay

The API overlay is a browser-side visual panel for request/response diagnostics. It is not a network mocking or interception layer.

Common entry points include:

```java
String requestId = overlay.apiShowRequest("Create order", "POST", "/api/orders", "{...}");
overlay.apiSetPending(requestId, 5000);
overlay.apiSetResponse(requestId, 201, 320, "content-type: application/json", "{...}");
overlay.apiHighlightJsonPath("$.order.id");
overlay.hideApiModal();
```

Some Java bridges still call the legacy `window.__seleniumApiModal` alias internally while the runtime module is registered under `window.__uiTestLens.modules.apiOverlay`.

## Type hints and scroll arrows

Type hints are shown by typing helpers:

```java
overlay.typeWithHint(element, "value");
overlay.smartTypeWithHint(element, "value");
overlay.smartTypeWithHintHighlighted(element, "value", "EMAIL");
```

Scroll arrows are shown by scroll helpers:

```java
overlay.scrollToElementWithArrow(element);
overlay.scrollToElementWithArrow(element, 600);
```

These visuals are diagnostic aids. They do not replace Selenium interactions or locator/actionability logic.

## Configuration

`OverlayConfig` is the current public visual configuration object. It is Selenium-free and lives in the overlay module.

```java
OverlayConfig config = OverlayConfig.builder()
        .enabled(true)
        .showHudPanel(true)
        .hudPosition(HudPosition.TOP_RIGHT)
        .hudOffset(16, 16)
        .hudMaxWidthPx(320)
        .hudTheme(HudThemePreset.GLASS)
        .decorationDurationMs(1200)
        .highlightColor("#ffeb3b")
        .build();

JsOverlayDebug overlay = new JsOverlayDebug(driver, config);
```

Currently configurable:

- global visual overlay enablement through `enabled(...)`,
- HUD visibility through `showHudPanel(...)`,
- HUD position through `hudPosition(...)`,
- HUD offset through `hudOffset(...)`,
- HUD max width through `hudMaxWidthPx(...)`,
- HUD theme preset or custom values through `hudTheme(...)`,
- visual decoration duration through `decorationDurationMs(...)`,
- highlight frame/badge color through `highlightColor(...)`,
- legacy global popup close selector through `globalOverlayCloseButtonSelector(...)`.

Current defaults:

| Option | Default |
|---|---|
| `enabled` | `true` |
| `showHudPanel` | `true` |
| `hudPosition` | `BOTTOM_RIGHT` |
| `hudOffset` | `10, 10` |
| `hudMaxWidthPx` | `280` |
| `hudTheme` | `DEFAULT` |
| `decorationDurationMs` | `1500` |
| `highlightColor` | `#ffeb3b` |
| `globalOverlayCloseButtonSelector` | `null` |

## HUD themes

The HUD panel supports built-in presets:

- `DEFAULT`
- `DARK`
- `LIGHT`
- `GLASS`
- `COMPACT`
- `HIGH_CONTRAST`
- `MINIMAL`

Preset usage:

```java
OverlayConfig config = OverlayConfig.builder()
        .hudPosition(HudPosition.TOP_RIGHT)
        .hudTheme(HudThemePreset.GLASS)
        .build();
```

Custom theme usage:

```java
HudTheme customTheme = HudTheme.builder()
        .background("rgba(15, 23, 42, 0.92)")
        .foreground("#f8fafc")
        .mutedForeground("#cbd5e1")
        .accent("#38bdf8")
        .success("#22c55e")
        .warning("#facc15")
        .danger("#fb7185")
        .borderColor("rgba(148, 163, 184, 0.35)")
        .borderRadiusPx(16)
        .fontSizePx(13)
        .fontFamily("Inter, system-ui, sans-serif")
        .boxShadow("0 18px 45px rgba(15, 23, 42, 0.35)")
        .backdropFilter("blur(14px)")
        .paddingPx(12)
        .gapPx(8)
        .build();

OverlayConfig config = OverlayConfig.builder()
        .hudTheme(customTheme)
        .build();
```

The HUD runtime applies theme values with CSS variables such as:

- `--ui-test-lens-hud-bg`
- `--ui-test-lens-hud-fg`
- `--ui-test-lens-hud-muted-fg`
- `--ui-test-lens-hud-accent`
- `--ui-test-lens-hud-success`
- `--ui-test-lens-hud-warning`
- `--ui-test-lens-hud-danger`
- `--ui-test-lens-hud-border`

Fallback values preserve the previous default HUD appearance. This theme integration currently focuses on the HUD panel. Wait HUD and assertion badges still have mostly runtime-defined styling and should be handled in a separate visual theme pass if they need stable customization.

Not currently exposed as full public configuration:

- assertion badge theme,
- API overlay theme/layout,
- wait HUD theme,
- type hint masking or theme,
- scroll arrow styling.

## Current limitations

- The visual overlay is pragmatic diagnostics, not a full visual trace viewer.
- `OverlayConfig` covers key placement/visibility/duration/color settings and HUD panel themes, but not every runtime overlay element has a full theme API yet.
- Assertion badge styling is mostly runtime-defined; only duration and highlight color are shared through current config paths.
- Type hints can display the typed value in the browser overlay; sensitive value masking is a future hardening item.
- API overlay Java bridge still has compatibility calls through `window.__seleniumApiModal`.
- `clearDebugArtifacts()` clears overlay DOM artifacts only; it does not clear trace/evidence exports.
- The overlay module remains Selenium-free; WebDriver-specific construction stays in `ui-test-lens-selenium`.

## Recommended follow-up tasks

1. Add a typed visual theme/options model if HUD, badges, wait HUD, and API overlay need stable customization before 0.1.
2. Move remaining Java bridge calls from legacy `window.__seleniumApiModal` to `window.__uiTestLens.modules.apiOverlay` while keeping alias fallback.
3. Add optional value masking for type hints and HUD/log previews.
4. Add visual overlay examples that run only as documentation-only tests with a real WebDriver.
5. Decide before 1.0 whether loader/helper classes such as `*Js` should remain public or become internal.
