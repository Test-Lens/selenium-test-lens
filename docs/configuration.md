# Configuration

The default configuration is enough for normal use. When you need to customize Test Lens, start with `TestLensOptions` and `OverlayConfig`. More specialized features expose their own options close to the API that uses them.

## Test Lens options

`TestLensOptions` is the main configuration object supplied when attaching Test Lens to an existing driver.

| Option | Purpose |
|---|---|
| `overlayConfig(...)` | Configures the visual overlay and HUD. |
| `locatorOptions(...)` | Sets the default locator timeout, polling, retries, and actionability behavior. |
| `outputRoot(...)` | Changes the root directory for session reports and diagnostics. |
| `screenshotOnFailure(...)` | Controls automatic screenshot capture during failed finalization. |
| `cleanupHudOnFinish(...)` | Controls whether the HUD, borders, and tooltips are cleared during finalization. |

By default, session output is written beneath `target/ui-test-lens`, and visual debug artifacts are cleared during finalization.

```java
OverlayConfig overlayConfig = OverlayConfig.builder()
        .hudPosition(HudPosition.TOP_RIGHT)
        .build();

TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
        .overlayConfig(overlayConfig)
        .build());
```

## Visual overlay

`OverlayConfig` controls browser-side decorations and the diagnostic HUD.

| Option | Purpose |
|---|---|
| `enabled(...)` | Enables or disables overlay injection and visual decorations. |
| `showHudPanel(...)` | Shows or hides the HUD panel. |
| `hudPosition(...)` | Places the HUD in a viewport corner. |
| `hudOffset(...)` | Sets its horizontal and vertical offsets from that corner. |
| `hudMaxWidthPx(...)` | Sets the maximum HUD width. |
| `hudTheme(...)` | Applies a preset or custom `HudTheme`. |
| `highlightColor(...)` | Sets the element highlight color. |
| `decorationDurationMs(...)` | Sets how long visual decorations remain visible. |

```java
OverlayConfig config = OverlayConfig.builder()
        .showHudPanel(true)
        .hudPosition(HudPosition.TOP_RIGHT)
        .hudOffset(16, 16)
        .hudTheme(HudThemePreset.DARK)
        .highlightColor("#38bdf8")
        .build();
```

### HUD position and appearance

`HudPosition` supports `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, and `BOTTOM_RIGHT`. Use `hudOffset(...)` and `hudMaxWidthPx(...)` when the panel would otherwise overlap application controls.

### Theme presets

| Preset | Description |
|---|---|
| `DEFAULT` | Default dark slate theme |
| `DARK` | Dark neutral theme |
| `LIGHT` | Light theme |
| `GLASS` | Translucent dark theme |
| `COMPACT` | Smaller text and reduced spacing |
| `HIGH_CONTRAST` | Higher-contrast colors and border |
| `BLACK_AND_COLORS` | Dark theme with vivid accents |
| `MINIMAL` | Light theme with reduced visual emphasis |

```java
OverlayConfig config = OverlayConfig.builder()
        .hudTheme(HudThemePreset.HIGH_CONTRAST)
        .build();
```

The `GLASS` preset applies CSS backdrop blur and saturation in browsers that support those properties.

### Custom theme

Use `HudTheme.builder()` when a preset does not fit the application under test:

```java
HudTheme customTheme = HudTheme.builder()
        .background("rgba(15, 23, 42, 0.92)")
        .foreground("#f8fafc")
        .accent("#38bdf8")
        .borderColor("rgba(148, 163, 184, 0.35)")
        .borderRadiusPx(16)
        .fontSizePx(13)
        .maxHeightPx(420)
        .build();

OverlayConfig config = OverlayConfig.builder()
        .hudTheme(customTheme)
        .build();
```

CSS strings are accepted without strict parsing. Numeric pixel values must be non-negative, `maxHeightPx` must be positive, and opacity must be between `0` and `1` when supplied.

## Advanced overlay policy

Overlay policies can detect and handle blocking UI such as consent banners. This is optional and currently configured through the lower-level `JsOverlayDebug` facade; ordinary tests should start with `TestLens`.

The example below assumes an existing `JsOverlayDebug` instance named `overlay`.

```java
OverlayPolicy policy = OverlayPolicy.builder()
        .handler(OverlayHandler.builder("Cookie consent")
                .detect(By.cssSelector("[data-testid='cookie-banner']"))
                .action(OverlayAction.click(
                        By.cssSelector("[data-testid='accept-cookies']")))
                .optional(true)
                .build())
        .build();

overlay.setOverlayPolicy(policy);
```

## Feature-specific configuration

Some features expose dedicated configuration types close to the API that uses them. You usually do not need to configure all of these globally.

| Type | Used for |
|---|---|
| `UiLocatorOptions` | Locator timeouts, polling, retries, and actionability; its retained `highlightBeforeAction` option is currently not consulted by `UiLocator` |
| `UiAssertionOptions` | Assertion timeouts, polling, and text comparison |
| `BusinessAssertionOptions` | Failure collection and fail-fast behavior in business assertion groups |
| `UiStepOptions` | Step failure behavior, HUD logging, and failure screenshots |
| `ScreenshotCaptureOptions` | Screenshot destination, naming, and session attachment |
| `VideoEvidenceOptions` | Existing video file or URL metadata and session attachment |
| `AuthStateOptions` | Authentication-state capture scope and metadata |
| `AuthRestoreOptions` | Authentication-state navigation, clearing, validation, and restore behavior |
| `NetworkDiagnosticsOptions` | Manual capture mode, failure threshold, ignored URLs, and headers; its deprecated `attachToSession` option has no automatic effect |
| `NetworkWaitCondition` | URL, method, status, timeout, and polling conditions for network waits |

See [Configuration builders](reference/configuration.md) or Javadoc for individual builder methods.

## Notes and limits

- With the default options, `finishFailed(Throwable)` attempts a screenshot. Capture is best-effort and can be disabled with `TestLensOptions.screenshotOnFailure(...)`.
- Network diagnostics omit headers by default. When headers are included, sensitive headers are masked by default.
- Network diagnostics default to explicit `MANUAL` events. Session attachment requires an explicit `NetworkDiagnostics.attachToSession(...)` call.
- Captured authentication state is written only when `AuthState.save(...)` is called. Saved files can contain cookies and tokens, so do not commit them.
- Video evidence attaches an existing local file or URL; Test Lens does not record video.

!!! note "Theme scope"

    HUD themes configure the main HUD panel. Other visual elements may not use every HUD theme setting.

## Next steps

- [Get started](getting-started.md)
- [Browse examples](examples.md)
- [Read the complete API reference](reference/index.md)
- [Configure the visual overlay and HUD](observability/visual-diagnostics.md)
