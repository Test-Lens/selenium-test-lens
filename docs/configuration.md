# Configuration

UI Test Lens configuration is intentionally split by responsibility: visual overlay settings live in the overlay module, Selenium behavior lives in Selenium options, and trace/evidence settings live in the trace/evidence APIs.

## OverlayConfig

`OverlayConfig` configures the runtime visual overlay.

Common options:

| Option | Purpose |
|---|---|
| `enabled(...)` | Enables or disables overlay injection and visual decorations. |
| `showHudPanel(...)` | Controls whether the HUD panel is visible. |
| `hudPosition(...)` | Places the HUD, for example `TOP_RIGHT` or `BOTTOM_RIGHT`. |
| `hudOffset(...)` | Offsets the HUD from the viewport edge. |
| `hudMaxWidthPx(...)` | Limits HUD width. |
| `hudTheme(...)` | Applies a built-in HUD theme preset or a custom `HudTheme`. |
| `highlightColor(...)` | Sets the element highlight color. |
| `decorationDurationMs(...)` | Controls how long visual decorations stay visible. |

Example:

```java
OverlayConfig config = OverlayConfig.builder()
        .enabled(true)
        .showHudPanel(true)
        .hudPosition(HudPosition.TOP_RIGHT)
        .hudOffset(16, 16)
        .hudMaxWidthPx(320)
        .hudTheme(HudThemePreset.GLASS)
        .highlightColor("#38bdf8")
        .decorationDurationMs(1500)
        .build();
```

## HUD theme presets

`HudThemePreset` provides:

- `DEFAULT`
- `DARK`
- `LIGHT`
- `GLASS`
- `COMPACT`
- `HIGH_CONTRAST`
- `MINIMAL`

```java
OverlayConfig config = OverlayConfig.builder()
        .hudTheme(HudThemePreset.HIGH_CONTRAST)
        .build();
```

The default theme preserves the current UI Test Lens HUD appearance.

## Custom HudTheme

`HudTheme` is immutable and can be created through its builder. Values are passed to the runtime HUD as CSS variables with JavaScript fallbacks.

```java
HudTheme customTheme = HudTheme.builder()
        .background("rgba(15, 23, 42, 0.92)")
        .foreground("#f8fafc")
        .mutedForeground("#cbd5e1")
        .accent("#38bdf8")
        .success("#22c55e")
        .warning("#f59e0b")
        .danger("#ef4444")
        .borderColor("rgba(148, 163, 184, 0.35)")
        .borderRadiusPx(16)
        .fontFamily("Inter, system-ui, sans-serif")
        .fontSizePx(13)
        .boxShadow("0 18px 45px rgba(15, 23, 42, 0.35)")
        .opacity(0.96)
        .zIndex(2147483000)
        .backdropFilter("blur(12px)")
        .paddingPx(12)
        .gapPx(8)
        .build();

OverlayConfig config = OverlayConfig.builder()
        .hudTheme(customTheme)
        .build();
```

Validation is intentionally light. CSS values are not parsed aggressively; numeric pixel values must be non-negative and opacity must be between `0` and `1` when supplied.

## Overlay policy

Blocking overlays and popups can be modeled with an `OverlayPolicy`.

```java
OverlayPolicy policy = OverlayPolicy.builder()
        .handler(OverlayHandler.builder("Cookie consent")
                .detect(By.cssSelector("[data-testid='cookie-banner']"))
                .action(OverlayAction.click(By.cssSelector("[data-testid='accept-cookies']")))
                .optional(true)
                .build())
        .build();

overlay.setOverlayPolicy(policy);
```

Optional overlays are handled when present. Fatal overlays can fail an action when the policy decides the page is blocked.

## Selenium-side options

The Selenium module exposes focused options classes:

| Options class | Purpose |
|---|---|
| `UiLocatorOptions` | Retry timeout, polling and actionability behavior for locators. |
| `UiAssertionOptions` | Retry timeout and polling for web assertions. |
| `BusinessAssertionOptions` | Collect failures or fail fast in business assertion groups. |
| `UiStepOptions` | Step fail-fast behavior, HUD logging and optional screenshot-on-failure. |
| `ScreenshotCaptureOptions` | Screenshot output folder, file naming and session attachment. |
| `VideoEvidenceOptions` | Video source metadata, local-file validation and session attachment. |
| `AuthStateOptions` | Cookie/storage capture scope and metadata. |
| `AuthRestoreOptions` | Navigation, clearing and restore behavior. |
| `NetworkDiagnosticsOptions` | Capture mode, failed status threshold, ignored URLs and header masking. |
| `NetworkWaitCondition` | URL/method/status conditions and wait timeout for network assertions. |

Defaults prefer safety: headers are omitted or masked, screenshot-on-failure is opt-in, auth state files are written only when explicitly saved, and video evidence is attachment-based rather than recording-based.

## Current theme scope

The shared HUD theme system primarily covers the HUD panel. Wait HUD and assertion badges may use some shared visual variables, but they are not yet fully covered by one common theme contract.
