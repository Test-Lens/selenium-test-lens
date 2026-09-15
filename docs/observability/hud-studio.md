---
title: HUD Studio
---

# Test Lens HUD Studio

HUD Studio configures the same browser-side renderer that Test Lens injects at runtime. Drag and resize the HUD in the preview or use the bounded product controls; both paths update one `HudOptions` model and the generated Java. The preview supplies synthetic events and never contacts an application or external service.

!!! info "Development API"
    Configurable HUD presets and HUD Studio are part of `0.3.0`. The `0.2.0` renderer does not accept `HudOptions`.

<style>
.tl-hud-studio-frame { display:block; width:100%; max-width:100%; height:900px; border:1px solid var(--md-default-fg-color--lightest); border-radius:.6rem; background:#e7edf4; }
@media(max-width:760px){.tl-hud-studio-frame{height:1500px}}
</style>

<iframe class="tl-hud-studio-frame" src="../../demo/hud-studio/" title="Interactive Test Lens HUD Studio" sandbox="allow-scripts">
  HUD Studio could not be loaded. [Open the standalone configurator](../demo/hud-studio/index.html).
</iframe>

## Public configuration

`HudOptions` is immutable and defaults to `HudPreset.COMPACT`. A preset establishes base content and layout values; explicit builder overrides win whether they appear before or after `preset(...)`.

```java
HudOptions hud = HudOptions.builder()
        .preset(HudPreset.COMPACT)
        .position(HudPosition.TOP_RIGHT)
        .headerLayout(HudHeaderLayout.AUTO)
        .offsetXPx(16)
        .offsetYPx(24)
        .maxHeightPx(360)
        .fontPreset(HudFontPreset.SYSTEM)
        .typography(HudTypography.builder()
                .header(HudFontPreset.MONOSPACE)
                .eventLog(HudFontPreset.UI_SANS)
                .build())
        .scrollbarStyle(HudScrollbarStyle.SUBTLE)
        .scrollbarThumbColor("#526174")
        .backgroundOpacity(0.82)
        .showNetwork(false)
        .build();

TestLensOptions options = TestLensOptions.builder()
        .hud(hud)
        .build();

TestLens lens = TestLens.attach(driver, options);
```

| Preset | Intended view |
|---|---|
| `MINIMAL` | Current step and branding; the event log is hidden. |
| `COMPACT` | Default: compact test/step context and categorized event log, without pipeline or timestamps. |
| `STANDARD` | More width and log height, with timestamps. |
| `DEBUG` | Largest diagnostic view, including pipeline and timestamps. |

All presets use `HudHeaderLayout.AUTO`: TEST and STEP share a row while their atomic label/value pairs fit, then the complete STEP item moves to a second row. `INLINE` keeps both items on one row and truncates their values; `STACKED` always uses separate rows. Individual values remain single-line, expose their full text as a tooltip, and use ellipsis when constrained. The optional DEBUG/custom PIPE value is a separate metadata row, so it does not change the responsive TEST/STEP decision.

Visibility switches affect only presentation. Suppressed network, recovery, wait, or assertion rows still flow to trace, reports, and other configured sinks. `showTimestamps(false)` removes timestamps from HUD rows; it does not change event timestamps in the model.

## Visual editing and responsive preview

Drag the panel to choose the nearest corner anchor and bounded X/Y offsets. The resize handle changes width and maximum panel height; in `AUTO`, the real runtime renderer immediately moves the complete STEP item between the first and second row as space changes. The event log keeps its own maximum height and scrolls internally. Desktop, laptop, and mobile buttons resize the isolated preview viewport without introducing viewport-specific configuration. After any manual change Studio shows **Custom**, while retaining the selected preset as the base for a minimal builder.

Clicking HUD context, branding, or log regions selects the related control group. Color pickers and `#RRGGBB` fields remain synchronized. Every direct manipulation has a corresponding `HudOptions` builder method; Studio has no private presentation setting that is omitted from generated Java.

## Layout, typography, and color boundary

The four `HudPosition` values anchor the panel to a viewport corner. Offsets are limited to 0–500 px, width to 240–960 px, panel maximum height to 120–1000 px, and log maximum height to 80–720 px. At render time the dimensions and anchored offsets are clamped to a 10 px viewport margin without changing the stored options. The effective log height is the minimum of its configured limit, the panel content area, and the available viewport area.

`HudFontPreset` selects a bundled local stack (`SYSTEM`, `MONOSPACE`, or `UI_SANS`), with bounded base and header sizes. The global preset is the baseline. Optional `HudTypography` overrides independently select the stack for the header, current step, event log, and metadata; any section without an override inherits the global preset. Timestamps belong to the event log, while labels, optional pipeline text, and branding belong to metadata. A section override is explicit builder state, so it wins over the selected HUD preset regardless of builder call order. Studio exposes the same four overrides with **Inherit** as the default and emits only the overrides that were selected. No font is downloaded.

Colors accept six-digit hexadecimal values, and background opacity must be between `0` and `1`. Success, warning, and failure colors drive matching event rows. The API deliberately does not accept arbitrary CSS, font URLs, or `@font-face` declarations.

The event log uses `HudScrollbarStyle.SUBTLE` by default: a 6 px rounded Chromium scrollbar with a dark track and a muted neutral thumb. Firefox uses its closest supported `thin` scrollbar with the same thumb and track colors. `STANDARD` is wider in Chromium and more visible; `DEBUG` selects it by default. `NATIVE` removes Test Lens scrollbar classes and color properties so the browser and operating system render the control. A builder can override the bounded 4–14 px Chromium width and the track, thumb, and hover colors. Firefox does not expose an exact pixel-width scrollbar API, so `SUBTLE` maps to `thin` and `STANDARD` maps to its native-width custom-color variant.

## Branding and custom logos

`HudBranding` selects `TEST_LENS`, `CUSTOM`, `BOTH`, or `NONE`. A custom logo is loaded explicitly from a local PNG:

```java
HudOptions hud = HudOptions.builder()
        .branding(HudBranding.BOTH)
        .logoPlacement(HudLogoPlacement.LEFT_RAIL)
        .railWidthPx(24)
        .customLogo(Path.of("branding", "company.png"))
        .build();
```

`HudLogoPlacement` supports the left rail and the left or right side of the compact header. Rail width is bounded to 16–80 px. The file must be a regular, non-symlink PNG no larger than 1 MiB; its IHDR dimensions are limited to 4096 by 4096 and 16,777,216 pixels. Test Lens embeds a data URL for the current browser document and never inserts caller-provided HTML. SVG is intentionally unsupported because an active SVG security contract would be substantially broader. Studio reads a selected or dropped PNG only for preview; it does not upload or copy it. **Runtime logo path** is a separate project path used by generated Java.

Legacy `OverlayConfig` HUD position, width, and `HudTheme` methods remain available for compatibility. When explicit `HudOptions` and legacy HUD setters are combined, `HudOptions` wins for every overlapping value regardless of builder call order. Without explicit `HudOptions`, legacy settings retain their historical behavior. Prefer `TestLensOptions.builder().hud(...)` for new code; `HudTheme` is the lower-level trusted-style API and is not the contract generated by Studio.
