# What's new in 0.3.1

Selenium Test Lens 0.3.1 makes HUD timestamps complete and configurable and adds outcome-driven element feedback without changing canonical trace data or interaction policy.

```java
import io.github.testlens.hud.HudOptions;
import io.github.testlens.hud.HudTimestampFormat;
import java.time.ZoneId;

HudOptions hud = HudOptions.builder()
        .showTimestamps(true)
        .timestampFormat(HudTimestampFormat.DATE_TIME)
        .timestampZone(ZoneId.of("Europe/Warsaw"))
        .build();
```

Available presentations are:

- `ISO_UTC` — compatibility default, for example `2026-07-15T22:00:00.000Z`;
- `TIME_ONLY` — `HH:mm:ss` in the selected zone;
- `DATE_TIME` — `dd.MM.yy HH:mm:ss` in the selected zone.

Call `.systemTimestampZone()` to use the system zone of the JVM that runs the test. This remains the test process zone even when WebDriver controls a remote BrowserStack browser. An explicit `ZoneId`, such as `Europe/Warsaw`, follows its winter/summer offset rules.

Every visible event-log row now receives one timestamp from the event itself. Missing, invalid, or placeholder values are assigned one instant when the row is accepted. Alert-deferred rows retain their original instant, and hiding timestamps removes the complete prefix without changing trace/JSON timestamps, ordering, duration, or elapsed measurements.

[Open HUD Studio](observability/hud-studio.md) to preview the format and zone and copy matching Java configuration.

## Operation-state highlights

Actions, waits, retries, `UiExpect`, and legacy element assertions use the same five typed visual states. A polling miss is WAITING/RETRY, never a terminal failure. SUCCESS and FAILURE come from the actual operation result; a neutral manual highlight never creates `ASSERTION_PASSED`.

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
