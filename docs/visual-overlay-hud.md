# Visual overlay and HUD

Selenium Test Lens includes runtime JavaScript resources for visual debugging. These resources live in `ui-test-lens-overlay` and remain independent of Selenium.

## Scope

The visual layer helps during test authoring and failure analysis:

- current step label
- HUD log messages
- element highlights
- wait indicators
- assertion badges
- API overlay diagnostics
- type hints
- scroll arrows

The visual layer is optional and controlled by `OverlayConfig`.

## Runtime resources

The overlay module ships these browser-side resources:

- `hud-panel.js`
- `wait-hud.js`
- `highlight.js`
- `assertion-badges.js`
- `api-overlay.js`
- `type-hint.js`
- `scroll-arrow.js`

The primary runtime namespace is `window.__uiTestLens`. Legacy aliases are preserved for compatibility where the runtime already exposes them.

## HUD

The HUD panel shows the current test step and recent log messages.
Theme presets include a maximum panel height so long logs scroll inside the HUD. Custom themes can opt in with `maxHeightPx(...)`; leaving it unset preserves uncapped custom HUD behavior.

```java
overlay.setStep("Checkout");
overlay.hudLog("info", "Saving order", "local");
```

Configuration example:

```java
OverlayConfig config = OverlayConfig.builder()
        .hudPosition(HudPosition.TOP_RIGHT)
        .hudTheme(HudThemePreset.GLASS)
        .build();

JsOverlayDebug overlay = new JsOverlayDebug(driver, config);
```

## Overlay root

Runtime resources create Selenium Test Lens elements inside the page without changing the application source. Overlay elements use high z-index values and isolated styles as much as possible, but they are still DOM elements injected into the tested page.

## Element highlighting

Element highlighting is used by locator actions and explicit debugging APIs. `OverlayConfig.highlightColor(...)` controls the primary highlight color.

```java
overlay.getByTestId("save-order").click();
```

Locator actions can highlight targets as part of the diagnostic flow.

## Wait HUD

The Wait HUD shows wait/debug feedback. It has its own runtime resource and limited integration with the shared HUD theme variables.

## Assertion badges

Assertion badges make assertion pass/fail state visible during interactive debugging. They are useful for authoring and demos; test failures and trace reports remain the source of truth.

The shared theme system does not fully control badge styling.

## API overlay

The API overlay is a browser-side visual diagnostic surface. It is separate from Selenium APIs and is loaded through overlay runtime resources.

## Type hints and scroll arrows

Type hints and scroll arrows make user-like actions easier to inspect while a Selenium test runs. They are part of the runtime overlay bundle and are intentionally lightweight.

## HUD themes

`HudThemePreset` includes:

- `DEFAULT`
- `DARK`
- `LIGHT`
- `GLASS`
- `COMPACT`
- `HIGH_CONTRAST`
- `BLACK_AND_COLORS`
- `MINIMAL`

Custom themes use `HudTheme.builder()` and are passed through `OverlayConfig`.

```java
HudTheme theme = HudTheme.builder()
        .background("rgba(15, 23, 42, 0.92)")
        .foreground("#f8fafc")
        .accent("#38bdf8")
        .borderRadiusPx(16)
        .fontFamily("Inter, system-ui, sans-serif")
        .maxHeightPx(420)
        .build();

OverlayConfig config = OverlayConfig.builder()
        .hudTheme(theme)
        .build();
```

The HUD runtime applies theme values through CSS variables such as:

- `--ui-test-lens-hud-bg`
- `--ui-test-lens-hud-fg`
- `--ui-test-lens-hud-muted`
- `--ui-test-lens-hud-accent`
- `--ui-test-lens-hud-success`
- `--ui-test-lens-hud-warning`
- `--ui-test-lens-hud-danger`
- `--ui-test-lens-hud-border`
- `--ui-test-lens-hud-radius`
- `--ui-test-lens-hud-font-family`
- `--ui-test-lens-hud-font-size`
- `--ui-test-lens-hud-shadow`
- `--ui-test-lens-hud-opacity`
- `--ui-test-lens-hud-z-index`
- `--ui-test-lens-hud-backdrop-filter`
- `--ui-test-lens-hud-padding`
- `--ui-test-lens-hud-gap`
- `--ui-test-lens-hud-max-height`

## Current limitations

- The common theme system is centered on the HUD panel.
- Wait HUD and assertion badges are not fully covered by shared theme presets.
- The overlay is diagnostic UI, not a full visual test report.
- Styling is inline/runtime JS; there is no external CSS dependency.

## Recommended follow-up tasks

1. Extend the common theme contract to Wait HUD and assertion badges.
2. Add a small visual smoke page for manual HUD theme inspection.
3. Document exact visual API methods once the pre-1.0 public API is frozen.
