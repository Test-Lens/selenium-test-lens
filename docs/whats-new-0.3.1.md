# What's new in 0.3.1

Test Lens for Selenium 0.3.1 unifies HUD, timestamp, source-navigation, highlight, and visual-redaction configuration under `TestLensOptions`, while improving reports and diagnostic evidence without changing canonical trace data or interaction policy. Each summary below links to the complete feature contract.

## HUD source navigation

```java
HudOptions.builder()
    .sourceNavigation(
        SourceNavigationOptions.builder()
            .enabled(true)
            .ide(SourceIde.INTELLIJ)
            .build())
    .build();
```

Hold Ctrl+Alt to reveal source locations in the HUD. Click a source location to navigate to the corresponding line in your IDE. The feature is off by default and intended for local debugging. The HUD remains passive outside the chord, and only the revealed link is interactive while it is held. Remote sessions may retain the logical label without an active link. Navigation is best-effort, cannot affect the test result, and absolute local paths are never added to exported event metadata.

[Read the source-navigation contract](observability/visual-diagnostics.md#local-source-navigation), including AltGr handling, source roots, providers, remote sessions, and privacy.

## Configurable HUD timestamps

```java
import io.github.testlens.hud.HudOptions;
import io.github.testlens.hud.HudTypography;
import java.time.ZoneId;

HudOptions hud = HudOptions.builder()
        .showTimestamps(true)
        .timestampPattern("yyyy-MM-dd HH:mm:ss.SSSSSS XXX")
        .timestampZone(ZoneId.of("Europe/Warsaw"))
        .typography(HudTypography.builder().timestampFontSizePx(10).build())
        .build();
```

Available preset presentations are:

- `ISO_UTC` — compatibility default, for example `2026-07-15T22:00:00.000Z`;
- `TIME_ONLY` — `HH:mm:ss` in the selected zone;
- `DATE_TIME` — `dd.MM.yy HH:mm:ss` in the selected zone.

Call `.systemTimestampZone()` to use the system zone of the JVM that runs the test. This remains the test process zone even when WebDriver controls a remote BrowserStack browser. An explicit `ZoneId`, such as `Europe/Warsaw`, follows its winter/summer offset rules.

For full control, `timestampPattern(...)` accepts an eagerly validated Java `DateTimeFormatter`
pattern and overrides the preset regardless of setter order. Fraction precision is selected with
1–9 `S` letters. Region `ZoneId` values apply DST automatically; fixed `ZoneOffset` values do not.
`HudTypography.timestampFontSizePx(...)` independently controls timestamp size (9 px by default).

Every visible event-log row now receives one timestamp from the event itself. Missing, invalid, or placeholder values are assigned one instant when the row is accepted. Alert-deferred rows retain their original instant, and hiding timestamps removes the complete prefix without changing trace/JSON timestamps, ordering, duration, or elapsed measurements.

[Open HUD Studio](observability/hud-studio.md) to preview the format and zone and copy matching Java configuration.

## Operation-state highlights

Actions, waits, recovery retries, `UiExpect`, and legacy element assertions use the same five typed visual states. Ordinary polling remains WAITING; RETRY means a genuine subsequent operation/recovery attempt, never every poll. SUCCESS and FAILURE come from the actual terminal operation result; a neutral manual highlight never creates `ASSERTION_PASSED`.

Automatic operation feedback is enabled by default when highlights and the master overlay are enabled. This is an intentional visual change from 0.3.0: successful and terminally failed element operations now receive the historical green/red assertion colors without changing their result, retry policy, or interaction count.

```java
HighlightOptions highlights = HighlightOptions.builder()
        .actionColor("#ffeb3b")
        .waitingColor("#2196f3")
        .retryColor("#ff9800")
        .successColor("#4caf50")
        .failureColor("#f44336")
        .durationMs(1500)
        .borderWidthPx(2)
        .showLabels(true)
        .automaticFeedback(true)
        .build();

TestLensOptions options = TestLensOptions.builder()
        .hud(hud)
        .highlights(highlights)
        .build();
```

Manual decoration uses the active facade, logger, session, and redaction policy:

```java
lens.locator(By.id("UserName"), "Pole logowania").highlight();
lens.highlight(element, "Pole logowania");
lens.highlight(element, "Zapis zakończony", HighlightState.SUCCESS);
```

These calls do not click, type, focus, or scroll. A manual `By` highlight resolves the locator once using its normal locator contract. Automatic feedback reuses the element already resolved by the operation, including scoped, `nth`, and Shadow DOM targets. Missing/detached targets simply omit decoration while HUD/trace keep the result.

Migration: `OverlayConfig.highlightColor(...)` supplies `HighlightOptions.actionColor(...)`, and `decorationDurationMs(...)` supplies its `durationMs(...)` only when the corresponding typed field was not explicitly set. Explicit typed fields win in either setter order; the legacy duration still controls arrows and other historical decorations. Replace consumer-created `new JsOverlayDebug(driver)` instances with the facade calls above; legacy use remains supported and renders through the same mechanism.

[Configure every highlight state](observability/visual-diagnostics.md#state-aware-highlights) or edit HUD and highlights together in [HUD Studio](observability/hud-studio.md).

## Readable standalone HTML reports

Session, suite, log-export, and bundle HTML now share a fluid layout that uses the available viewport instead of stopping at 1240 px. Failure details define a complete high-contrast palette in LIGHT, DARK, and reactive AUTO themes. AUTO follows a changed `prefers-color-scheme` value in the already-open document.

Timeline attributes open in a full-width row below their event. Keys are bounded and values receive the remaining width, with a stacked key/value layout on narrow screens. Long timelines keep sticky headers inside their vertical scroll region and expose a synchronized horizontal bar for the overflowing table currently in view. The standard table scrollbar, touchpad scrolling, selectable text, anchors, and native `<details>` fallback remain available; no network resource is required when the report is opened through `file://`.

Previously generated HTML embeds its old CSS and markup. Regenerate it with 0.3.1 to receive these fixes; replacing only the library JAR does not rewrite archived reports.

[Read the report layout and offline contract](observability/reports.md).

## Stabilized full-page diagnostic evidence

Opt-in full-page capture remains bounded and portable. Diagnostic evidence records fixed and sticky Test Lens artifacts, including the complete overlay, in the first tile where they appear and hides them for later tiles, so the composed image contains the overlay exactly once. Geometry and visual-redaction masks are refreshed per tile; restoration remains best-effort and capture failure does not replace the original test failure.

[Read the full-page and failure-evidence contract](observability/screenshots-evidence.md#portable-full-page-capture).
