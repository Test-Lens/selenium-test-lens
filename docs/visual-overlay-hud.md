# Visual overlay and HUD

Selenium Test Lens can display a lightweight diagnostic overlay while a test runs. The HUD shows current activity and recent diagnostic messages, while temporary highlights and indicators make browser interactions easier to follow.

The visual layer is optional and best-effort. It helps during interactive and headed runs, but it does not decide whether a test passes or fails. The test result, session trace, and generated reports remain authoritative.

## What the overlay shows

Depending on the operation, the visual layer can show:

- the current named step
- recent Lens diagnostic messages
- temporary element highlighting
- wait feedback
- assertion pass/fail feedback

Supported Lens operations feed these diagnostics while a session is active. Raw Selenium calls do not automatically produce equivalent Lens events.

```java
TestLens lens = TestLens.attach(driver);
lens.startSession("Checkout");

lens.locator(By.id("save-order"), "Save order").click();
```

The overlay is injected into the tested page at runtime and does not require changes to the application source.

## Configure the HUD

Use `OverlayConfig` through `TestLensOptions` when attaching Lens:

```java
OverlayConfig config = OverlayConfig.builder()
        .hudPosition(HudPosition.TOP_RIGHT)
        .hudTheme(HudThemePreset.GLASS)
        .build();

TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
        .overlayConfig(config)
        .build());
```

`HudPosition` supports `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, and `BOTTOM_RIGHT`.

For the complete set of overlay options, including how to disable the overlay or HUD panel, see [Configuration](configuration.md).

## HUD themes

The built-in `HudThemePreset` values in 0.1.0 are:

| Preset | Visual style |
| --- | --- |
| `DEFAULT` | Default dark, neutral styling |
| `DARK` | Dark styling |
| `LIGHT` | Light styling |
| `GLASS` | Translucent styling |
| `COMPACT` | Smaller text and reduced spacing |
| `HIGH_CONTRAST` | High-visibility colors and borders |
| `BLACK_AND_COLORS` | Black background with vivid accents |
| `MINIMAL` | Light styling with reduced visual emphasis |

`GLASS` uses a translucent background and browser backdrop blur and saturation where those CSS properties are supported.

## Custom themes

Use `HudTheme.builder()` when a preset does not fit the tested application:

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

See [Configuration](configuration.md) for the remaining theme properties and validation rules.

## Element, wait and assertion feedback

Supported Lens interactions can temporarily highlight their target when visual diagnostics are enabled. `OverlayConfig.highlightColor(...)` controls the primary highlight color.

Wait feedback may appear while locator waits are polling. Assertion feedback can show pass or failure state during interactive debugging. These indicators supplement the session trace and test result; they do not replace them.

## Advanced visual diagnostics

The 0.1.0 `TestLens` facade also provides specialized helpers: `apiCallWithModal(...)` for visual API-call diagnostics, `scrollToElementWithArrow(...)` for guided scrolling, and `smartUploadFile(...)` for visually guided file upload. These facilities are optional and are not required for ordinary interactions, waits, assertions, or lifecycle management. See the [API guide](api-reference.md) for the normal facade boundary.

## Current limitations

- HUD themes primarily configure the main HUD panel. Auxiliary wait and assertion indicators may not use every theme setting.
- The overlay is diagnostic UI inside the tested page, not the generated HTML report.
- Because the overlay is injected into the page, unusual application DOM or CSS behavior can interfere with its presentation.

## Next steps

- [Configure Test Lens](configuration.md)
- [Read about trace, reports, and evidence](api-reference.md#trace-reports-and-evidence)
