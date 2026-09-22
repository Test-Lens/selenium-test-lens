---
title: HUD Studio
---

# Test Lens HUD Studio

HUD Studio configures the same browser-side renderer that Test Lens injects at runtime. Drag and resize the HUD in the preview or use the bounded product controls; both paths update one `HudOptions` model and the generated Java. The preview supplies synthetic events and never contacts an application or external service.

The preview uses the production semantic renderer and includes ACTION and ASSERTION running/terminal rows, assertion retry/failure, a SYSTEM warning, a USER message, and technical LOCATOR/HIGHLIGHT diagnostics. STANDARD/COMPACT suppress successful technical diagnostics before DOM insertion; DEBUG reveals them as muted DEBUG rows. Category/status icons never replace the visible textual labels, and preset switching uses the same filtering rules as the runtime.

The Source navigation section generates the opt-in `SourceNavigationOptions` block, IDE provider, and activation shortcut. F8 is the default toggle; the legacy Ctrl+Alt hold option remains available. Its preview uses synthetic file labels only and does not open an IDE.

!!! info "0.3.1 timestamp API"
    HUD timestamp format and zone controls are available in `0.3.1`. The configurable HUD and Studio introduced in `0.3.0` remain compatible.

!!! info "0.4.0 highlight timing"
    The Highlights section configures independent action, waiting, retry, success, and failure colors plus inherited or state-specific durations, border width, labels, enablement, and automatic feedback. HUD preset changes do not reset these values, and generated Java includes only explicit duration overrides in `HighlightOptions`.

<style>
.tl-hud-studio-toolbar{display:flex;flex-wrap:wrap;gap:.45rem;align-items:center;margin:.75rem 0;padding:.6rem;border:1px solid var(--md-default-fg-color--lightest);border-radius:.5rem;background:var(--md-code-bg-color)}
.tl-hud-studio-toolbar>[hidden]{display:none}
.tl-hud-studio-toolbar a,.tl-hud-studio-toolbar button{display:inline-flex;align-items:center;min-height:2.1rem;padding:.35rem .65rem;border:1px solid var(--md-primary-fg-color);border-radius:.35rem;color:var(--md-primary-fg-color);background:transparent;font:inherit;font-weight:600;text-decoration:none;cursor:pointer}
.tl-hud-studio-toolbar a:hover,.tl-hud-studio-toolbar button:hover{color:var(--md-primary-bg-color);background:var(--md-primary-fg-color)}
.tl-hud-studio-toolbar span{color:var(--md-default-fg-color--light);font-size:.75rem}
.tl-hud-studio-host{display:flex;flex-direction:column;min-width:0;width:100%;height:900px;border:1px solid var(--md-default-fg-color--lightest);border-radius:.6rem;overflow:hidden;background:#e7edf4}
.tl-hud-studio-frame{display:block;flex:1;width:100%;min-height:0;border:0;background:#e7edf4}
body.tl-hud-studio-page .md-main__inner{max-width:none}
body.tl-hud-studio-page .md-sidebar--secondary{display:none}
body.tl-hud-studio-page .md-content{min-width:0}
body.tl-studio-focus-mode{overflow:hidden}
body.tl-studio-focus-mode .md-header,body.tl-studio-focus-mode .md-tabs,body.tl-studio-focus-mode .md-sidebar{display:none}
body.tl-studio-focus-mode .tl-hud-studio-host,body.tl-hud-studio-page .tl-hud-studio-host:fullscreen{position:fixed;inset:0;z-index:1000;width:100vw;height:100vh;border:0;border-radius:0}
body.tl-studio-focus-mode .tl-hud-studio-frame{flex:1}
@media(max-width:760px){.tl-hud-studio-host{height:1500px}.tl-hud-studio-toolbar span{flex-basis:100%}}
</style>

<div class="tl-hud-studio-toolbar">
  <a data-studio-open href="../../demo/hud-studio/" target="_blank" rel="noopener noreferrer">Open Studio</a>
  <button data-studio-expand type="button" aria-pressed="false">Expand in this page</button>
  <button data-studio-fullscreen type="button" aria-pressed="false" aria-label="Open HUD Studio in browser fullscreen">Fullscreen</button>
  <button data-studio-exit type="button" hidden>Exit expanded view</button>
  <span>For the best editing experience, open Studio in a full-width view.</span>
</div>

<div class="tl-hud-studio-host" data-studio-host>
  <iframe data-studio-frame class="tl-hud-studio-frame" src="../../demo/hud-studio/" title="Interactive Test Lens HUD Studio" sandbox="allow-scripts" allow="fullscreen" allowfullscreen>
    HUD Studio could not be loaded. <a href="../../demo/hud-studio/">Open the standalone configurator</a>.
  </iframe>
</div>

<script src="../../javascripts/hud-studio-host.js"></script>

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
                .timestampFontSizePx(10)
                .build())
        .scrollbarStyle(HudScrollbarStyle.SUBTLE)
        .scrollbarThumbColor("#526174")
        .backgroundOpacity(0.82)
        .showTimestamps(true)
        .timestampPattern("HH:mm:ss.SSS")
        .timestampZone(ZoneId.of("Europe/Warsaw"))
        .showNetwork(false)
        .build();

HighlightOptions highlights = HighlightOptions.builder()
        .enabled(true)
        .automaticFeedback(true)
        .actionColor("#ffeb3b")
        .waitingColor("#2196f3")
        .retryColor("#ff9800")
        .successColor("#4caf50")
        .failureColor("#f44336")
        .durationMs(1500)
        .successDurationMs(2500)
        .failureDurationMs(4000)
        .borderWidthPx(2)
        .showLabels(true)
        .build();

TestLensOptions options = TestLensOptions.builder()
        .hud(hud)
        .highlights(highlights)
        .visualRedaction(VisualRedactionOptions.defaults())
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

`ISO_UTC` is the compatibility default and renders in UTC unless an explicit timestamp zone overrides it. `TIME_ONLY` and `DATE_TIME`
remain shortcuts and show their effective pattern in a read-only field. Select `CUSTOM` to edit a
Java `DateTimeFormatter` pattern; one through nine `S` letters select fraction precision directly.
The zone modes are `SYSTEM`, `UTC`, and `CUSTOM`. `SYSTEM` emits no redundant builder call and means
the JVM/test-runner system zone, never the browser zone. `UTC` emits `ZoneOffset.UTC`; `CUSTOM`
accepts a validated IANA `ZoneId`, such as `Europe/Warsaw`, or a fixed offset. Studio shows fixed
winter and summer instants so DST differences are visible without a DST switch. Because a static
page cannot inspect the future test runner, its SYSTEM preview clearly labels the browser zone used
only as a visual stand-in; generated Java retains JVM system-zone behavior.

## Source navigation controls and preview

Source navigation is disabled by default. Enable it and choose `INTELLIJ`, `VSCODE`, or `CUSTOM`. F8 is the default toggle and Escape switches the mode off; the deprecated `CTRL_ALT` choice preserves the legacy hold behavior for existing explicit configurations. **Preview Source Navigation ON** shows the same `File.java:line` active state used by the runtime. The preview uses synthetic paths and never launches an IDE.

IntelliJ IDEA 2026.1+ bundles `jetbrainsd`, so JetBrains Toolbox is **not required**. IntelliJ IDEA older than 2026.1 requires JetBrains Toolbox App 3.3+ to provide `jetbrainsd` and `jetbrains://` handling. Project identity/root mapping remains programmatic through `intellijProject(...)`; without explicit mapping, the runtime requires a valid `.idea/.name`. `CUSTOM` also requires `customUriTemplate(...)`, and non-standard roots use `sourceRoots(...)`. Reset returns to disabled, IntelliJ, F8, and inactive preview. Studio cannot inspect the test runner's local IDE, so its preview remains synthetic; the runtime HUD performs the compatibility check and shows **Ready**, **Action required**, or **Availability unverified**. See [IntelliJ compatibility and preflight](visual-diagnostics.md#intellij-compatibility-and-preflight) for requirements, recovery steps, Windows diagnostics, project mapping, and the browser/foreground boundary.

## Highlight configurator and preview

The Highlights section maps one-to-one to `HighlightOptions`: enabled, automatic feedback, five state colors, a default duration, optional duration overrides per state, 1–16 px border width, and labels. **Use default** keeps an override absent and shows the effective inherited value; restoring it removes only that explicit override. Blank/inherited is not zero. Explicit state zero suppresses that state, while common duration zero retains legacy zero-delay presentation. **Replay** intentionally cycles through ACTION, WAITING, RETRY, SUCCESS, and FAILURE long enough to inspect their configured timing; it does not pretend that a successful click retried. Turning automatic feedback off preserves manual highlights in the runtime; turning highlights off suppresses every state. Reset restores defaults without changing the selected HUD preset.

The generated code uses the plural `.highlights(highlights)` API, not its deprecated singular preview alias. It emits the common duration only after that control was explicitly changed and emits only state overrides that are explicit; inherited states are not materialized. It produces one `TestLensOptions` builder containing the HUD (including timestamps and source navigation), highlights, and explicit password-safe `VisualRedactionOptions.defaults()`. Advanced visual mask rules remain programmatic. See [state-aware highlights](visual-diagnostics.md#state-aware-highlights) for the lifecycle and timing contract.

## Visual editing and responsive preview

Drag the panel to choose the nearest corner anchor and bounded X/Y offsets. The resize handle changes width and maximum panel height; in `AUTO`, the real runtime renderer immediately moves the complete STEP item between the first and second row as space changes. The event log keeps its own maximum height and scrolls internally. Desktop, laptop, and mobile buttons resize the isolated preview viewport without introducing viewport-specific configuration. After any manual change Studio shows **Custom**, while retaining the selected preset as the base for a minimal builder.

Clicking HUD context, branding, or log regions selects the related control group. Color pickers and `#RRGGBB` fields remain synchronized. Every direct manipulation has a corresponding `HudOptions` builder method; Studio has no private presentation setting that is omitted from generated Java.

## Layout, typography, and color boundary

The four `HudPosition` values anchor the panel to a viewport corner. Offsets are limited to 0–500 px, width to 240–960 px, panel maximum height to 120–1000 px, and log maximum height to 80–720 px. At render time the dimensions and anchored offsets are clamped to a 10 px viewport margin without changing the stored options. The effective log height is the minimum of its configured limit, the panel content area, and the available viewport area.

`HudFontPreset` selects a bundled local stack (`SYSTEM`, `MONOSPACE`, or `UI_SANS`), with bounded base and header sizes. The global preset is the baseline. Optional `HudTypography` overrides independently select the stack for the header, current step, event log, and metadata; any section without an override inherits the global preset. Timestamp size is independently configurable from 8–18 px and defaults to 9 px, so changing it never changes message text size. Timestamps belong to the event log, while labels, optional pipeline text, and branding belong to metadata. Explicit typography wins over the selected HUD preset regardless of builder call order. Studio exposes the same controls and emits only selected overrides. No font is downloaded.

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
