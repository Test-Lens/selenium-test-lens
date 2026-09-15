---
title: HUD Studio
---

# HUD Studio

<style>
.tl-hud-studio-toolbar{display:flex;flex-wrap:wrap;align-items:center;gap:.5rem;margin:.75rem 0}.tl-hud-studio-toolbar a,.tl-hud-studio-toolbar button{display:inline-flex;align-items:center;justify-content:center;min-height:2.35rem;padding:.45rem .8rem;border:1px solid var(--md-default-fg-color--lightest);border-radius:.35rem;color:var(--md-typeset-color);background:var(--md-default-bg-color);font:inherit;font-weight:600;cursor:pointer}.tl-hud-studio-toolbar>[hidden]{display:none}.tl-hud-studio-toolbar .tl-hud-studio-open{border-color:var(--md-primary-fg-color);color:var(--md-primary-bg-color);background:var(--md-primary-fg-color)}.tl-hud-studio-toolbar a:focus-visible,.tl-hud-studio-toolbar button:focus-visible{outline:.15rem solid var(--md-accent-fg-color);outline-offset:.15rem}.tl-hud-studio-note{margin:.5rem 0 1rem;color:var(--md-default-fg-color--light);font-size:.85rem}.tl-hud-studio-frame{display:block;width:100%;max-width:100%;height:950px;border:1px solid var(--md-default-fg-color--lightest);border-radius:.6rem;background:#e7edf4}.tl-studio-focus-mode{overflow:hidden}.tl-studio-focus-mode .md-header,.tl-studio-focus-mode .md-tabs,.tl-studio-focus-mode .md-sidebar,.tl-studio-focus-mode .md-footer{display:none}.tl-studio-focus-mode .tl-hud-studio-host,.tl-hud-studio-host:fullscreen{position:fixed;inset:0;z-index:10000;display:flex;flex-direction:column;width:100vw;max-width:none;height:100vh;margin:0;padding:.5rem;background:var(--md-default-bg-color)}.tl-studio-focus-mode .tl-hud-studio-toolbar,.tl-hud-studio-host:fullscreen .tl-hud-studio-toolbar{flex:0 0 auto;margin:0 0 .5rem}.tl-studio-focus-mode .tl-hud-studio-frame,.tl-hud-studio-host:fullscreen .tl-hud-studio-frame{flex:1 1 auto;min-height:0;height:auto;border-radius:.35rem}.tl-studio-focus-mode .tl-hud-studio-note,.tl-studio-focus-mode .tl-hud-studio-description,.tl-hud-studio-host:fullscreen .tl-hud-studio-note,.tl-hud-studio-host:fullscreen .tl-hud-studio-description{display:none}body.tl-hud-studio-page .md-main__inner{max-width:none}body.tl-hud-studio-page .md-sidebar--secondary{display:none}body.tl-hud-studio-page .md-content{min-width:0}
@media(max-width:760px){.tl-hud-studio-frame{height:1550px}.tl-hud-studio-toolbar a,.tl-hud-studio-toolbar button{flex:1 1 auto}}
</style>

<div class="tl-hud-studio-host" data-studio-host>
  <div class="tl-hud-studio-toolbar" role="toolbar" aria-label="HUD Studio view options">
    <a class="tl-hud-studio-open" data-studio-open href="../../demo/hud-studio/" target="_blank" rel="noopener noreferrer" aria-label="Open HUD Studio in a new full-width tab" title="Open HUD Studio in a new full-width tab">Open Studio ↗</a>
    <button type="button" data-studio-expand aria-pressed="false" aria-label="Expand HUD Studio in this page" title="Expand HUD Studio in this page">Expand</button>
    <button type="button" data-studio-fullscreen aria-pressed="false" aria-label="Open HUD Studio in browser fullscreen" title="Open HUD Studio in browser fullscreen">Fullscreen</button>
    <button type="button" data-studio-exit aria-label="Exit expanded HUD Studio view" title="Exit expanded HUD Studio view" hidden>Exit expanded view</button>
  </div>
  <p class="tl-hud-studio-note">For the best editing experience, open Studio in a full-width view.</p>
  <p class="tl-hud-studio-description">Drag and resize the HUD in the preview or use the bounded controls. Every mode below uses the same Studio application, <code>HudOptions</code> model, runtime renderer, and generated Java.</p>
  <iframe class="tl-hud-studio-frame" data-studio-frame src="../../demo/hud-studio/" title="Interactive Test Lens HUD Studio" sandbox="allow-scripts" allow="fullscreen" allowfullscreen>
    HUD Studio could not be loaded. <a href="../../demo/hud-studio/" target="_blank" rel="noopener noreferrer">Open the standalone configurator</a>.
  </iframe>
</div>

<script src="../../javascripts/hud-studio-host.js"></script>

!!! info "Development API"
    Configurable HUD presets and HUD Studio are part of `0.3.0`. The `0.2.0` renderer does not accept `HudOptions`.

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
