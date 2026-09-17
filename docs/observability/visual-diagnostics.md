# Visual runtime diagnostics

Visual diagnostics appear inside the tested page while a test is running. They are most useful during headed local execution and debugging: the browser itself shows what Test Lens is doing, what it is waiting for, and which assertion passed or failed. These diagnostics are transient; persistent output is covered under [screenshots and evidence](screenshots-evidence.md), [trace](trace.md), and [reports](reports.md).

## HUD

The HUD is an in-browser diagnostic panel displayed over the tested page. It shows the active session, current step, element actions, waits, assertions, and diagnostic log rows so you can follow a test without switching to a console or report.

The standard overlay and HUD are enabled by default. Attach Test Lens and start a session to use them:

```java
TestLens lens = TestLens.attach(driver);
lens.startSession("Checkout");
```

The HUD is especially useful when debugging a headed test, demonstrating a flow, or investigating a wait/retry. It does not replace the persistent trace or test-runner output.

The panel observes and presents Lens operations; it does not alter Selenium's success criteria. HUD injection or cleanup failure is decorative and cannot turn an otherwise successful or failed WebDriver operation into the opposite outcome.

Raw network rows can be reduced independently with [`NetworkHudFilter`](../advanced/network.md#hud-only-filtering). Its default hides duplicate request rows and shows responses and failures. This affects only the HUD: capture, waits, counters, trace, JSON, reports, external sinks, and failure evidence remain complete.

### Configurable HUD

For 0.3.0, configure the panel through immutable `HudOptions`. The default is `COMPACT`; a preset establishes a coherent base and explicit builder overrides win independently of call order:

```java
HudOptions hud = HudOptions.builder()
        .preset(HudPreset.COMPACT)
        .position(HudPosition.TOP_RIGHT)
        .headerLayout(HudHeaderLayout.AUTO)
        .backgroundOpacity(0.88)
        .showNetwork(false)
        .build();

TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
        .hud(hud)
        .build());
lens.startSession("Checkout");
```

`MINIMAL`, `COMPACT`, `STANDARD`, and `DEBUG` configure content and density. `AUTO` keeps the atomic TEST and STEP items together on one row when they fit, then moves the complete STEP item to row two; individual values remain single-line and use ellipsis. Runtime clamping keeps configured dimensions and anchored offsets reachable in the current viewport without mutating the stored options.

The API also controls bounded panel/log dimensions, local global and section-specific font stacks, event categories, validated colors, opacity, branding, and native/subtle/standard event-log scrollbars. Presentation filters affect only the HUD; they never delete trace, report, retry, or network data.

[Open HUD Studio](hud-studio.md) to edit the same renderer visually and copy matching Java. The complete defaults and ranges are in [`HudOptions`](../reference/configuration.md#hudoptions).

### HUD timestamps

HUD timestamps are presentation-only. They can be enabled or disabled independently of the event timestamps retained by trace and JSON exporters:

```java
HudOptions hud = HudOptions.builder()
        .showTimestamps(true)
        .timestampPattern("yyyy-MM-dd HH:mm:ss.SSSSSS XXX")
        .timestampZone(ZoneId.of("Europe/Warsaw"))
        .typography(HudTypography.builder().timestampFontSizePx(10).build())
        .build();
```

| Format | Example | Zone behavior |
| --- | --- | --- |
| `ISO_UTC` | `2026-07-15T22:00:00.000Z` | UTC compatibility default when no explicit zone is configured. |
| `TIME_ONLY` | `00:00:00` | `HH:mm:ss` in the selected zone. |
| `DATE_TIME` | `16.07.26 00:00:00` | `dd.MM.yy HH:mm:ss` in the selected zone. |

The presets remain convenient shortcuts, but an explicit `timestampPattern(...)` wins over
`timestampFormat(...)` regardless of builder call order. Patterns use Java
`DateTimeFormatter` semantics. In particular, `S`, `SSS`, `SSSSSS`, and `SSSSSSSSS` display
1, 3, 6, and 9 fraction digits. HUD events retain an `Instant`, so up to nanosecond precision is
available; the pattern selects how much of that stored precision is visible.

`showTimestamps(false)` removes the complete timestamp node: no brackets, separator, padding, or
empty grid column remains. It is the `COMPACT` preset default; `STANDARD` and `DEBUG` show
timestamps. By default formatting resolves `ZoneId.systemDefault()` in the JVM running the test.
Call `.timestampZone(zone)` for an explicit IANA zone or `.systemTimestampZone()` to restore
JVM-system behavior. HUD timestamps use the test runner JVM timezone by default, not the browser
machine timezone.

An IANA region such as `ZoneId.of("Europe/Warsaw")` applies its historical winter/summer rules to
the event `Instant` automatically (`+01:00` in winter, `+02:00` in summer). A fixed
`ZoneOffset.ofHours(1)` always remains `+01:00`; there is deliberately no separate DST switch.
Timestamp font size is independent from the event message through
`HudTypography.timestampFontSizePx(...)` (default 9 px, valid range 8–18 px).

Formatting happens once in Java from the event `Instant`; browser runtime receives the canonical
instant separately from the ready presentation string. Repaint, source-navigation activation, and
HUD reinjection do not replace event time with render time. Changing HUD timestamp presentation
does not alter event instants, ordering, durations, trace JSON, HTML reports, `capturedAt`, network
timestamps, or evidence metadata. HUD Studio exposes visibility, preset, free-form pattern, zone,
font size, and deterministic winter/summer previews.

### Local source navigation

Source navigation is opt-in and intended for local debugging:

```java
HudOptions hud = HudOptions.builder()
        .sourceNavigation(
                SourceNavigationOptions.builder()
                        .enabled(true)
                        .ide(SourceIde.INTELLIJ)
                        .build())
        .build();
```

Hold Ctrl+Alt to reveal source locations in the HUD. Click a source location to navigate to the corresponding line in your IDE. Outside that chord the HUD remains fully passive; while it is held, only the source link accepts pointer events. Releasing either key or moving focus away from the browser immediately restores the passive state.

The mode is active only while the real Ctrl and Alt modifier state remains set. Pressing and releasing another key, such as Shift, does not deactivate it; releasing Ctrl or Alt does. `window.blur` always deactivates it. AltGr/AltGraph never activates source navigation, even on Windows keyboards that report it as `ctrlKey && altKey`.

The link displays only `File.java:line`. The source call site is captured when a user-facing action, wait, assertion, or manual highlight is emitted. Standard multi-module Maven/Gradle Java and Kotlin roots are detected lazily from `src/test/java`, `src/main/java`, `src/test/kotlin`, and `src/main/kotlin`. Add non-standard roots without publishing absolute paths in event metadata:

```java
SourceNavigationOptions sourceNavigation = SourceNavigationOptions.builder()
        .enabled(true)
        .activationModifier(SourceNavigationModifier.CTRL_ALT)
        .ide(SourceIde.VSCODE)
        .sourceRoots(
                Path.of("acceptance/src/test/java"),
                Path.of("shared/src/main/kotlin"))
        .build();
```

| Provider | Behavior |
| --- | --- |
| `INTELLIJ` | Uses the local `idea://open` protocol handler. The same provider can be used by compatible JetBrains IDE installations. |
| `VSCODE` | Uses the local `vscode://file` protocol handler. |
| `CUSTOM` | Uses `customUriTemplate(...)` with `{file}`, `{line}`, and `{column}` placeholders. The template is configured programmatically; Studio selects the provider but does not edit the template. |

Local file resolution and protocol navigation are best-effort and never change the test result. If the logical location cannot be resolved, the `File.java:line` text remains visible but is not a link. Remote WebDriver sessions may likewise show the logical label without an active local IDE target. A missing protocol handler or rejected navigation cannot fail the test.

The full local path and IDE URI stay in the Java/browser callback boundary and are not stored as `href`, `data-*`, hidden text, or serialized row data in the open Shadow DOM. Exported events retain only class, method, file name, and line number; absolute paths and IDE URIs are not written to reports, JSON traces, or evidence.

Use `OverlayConfig` for the master visual switch, HUD visibility, highlight behavior, and legacy compatibility. To hide only the HUD while retaining click decoration:

```java
OverlayConfig overlayConfig = OverlayConfig.builder()
        .showHudPanel(false)
        .build();
```

To disable all visual overlay behavior:

```java
OverlayConfig overlayConfig = OverlayConfig.builder()
        .enabled(false)
        .build();
```

See [Getting Started](../getting-started.md) and the complete [`OverlayConfig` table](../reference/configuration.md#overlayconfig).

`TestLens.startSession(...)` attempts the initial HUD injection. Events retry injection lazily when a browser document was not available earlier, such as around navigation. When [`TestLensOptions.cleanupHudOnFinish`](../reference/configuration.md#testlensoptions) is enabled, finalization removes HUD/debug artifacts on a best-effort basis. Injection and cleanup failures do not change the WebDriver operation's intended result.

## State-aware highlights

A highlight is a temporary border/label drawn around an element so you can see which DOM target Test Lens selected. It is useful when a selector matches an unexpected element or a click is intercepted by page UI.

The 0.3.1 `HighlightOptions` model controls manual decoration and automatic operation feedback:

```java
HighlightOptions highlights = HighlightOptions.builder()
        .enabled(true)
        .automaticFeedback(true)
        .actionColor("#ffeb3b")
        .waitingColor("#2196f3")
        .retryColor("#ff9800")
        .successColor("#4caf50")
        .failureColor("#f44336")
        .durationMs(1500)
        .borderWidthPx(2)
        .showLabels(true)
        .build();

TestLensOptions options = TestLensOptions.builder()
        .highlights(highlights)
        .build();
```

The defaults are exactly the values shown above. `durationMs(0)` is valid, and border width accepts 1 through 16 CSS pixels.

| State | Default | Shown when | It does **not** mean |
| --- | --- | --- | --- |
| `ACTION` | `#ffeb3b` | The concrete action is attempted, or a caller requests a neutral manual highlight. | A manual ACTION is not proof that an interaction occurred. |
| `WAITING` | `#2196f3` | An operation is waiting for its condition or next observation. | It is not a failure and does not mark the session flaky. |
| `RETRY` | `#ff9800` | A genuine subsequent operation/recovery attempt begins. | It is not every ordinary wait or assertion polling observation. |
| `SUCCESS` | `#4caf50` | The concrete action, wait, or assertion completes successfully. | A successful click proves only that the action call completed, not that the intended business result occurred. |
| `FAILURE` | `#f44336` | The operation reaches its final failure, timeout, or assertion failure. | It is not an intermediate miss that can still recover. |

`enabled(false)` disables both manual and automatic element decorations. `automaticFeedback(false)` leaves the manual API available but suppresses automatic ACTION/WAITING/RETRY/SUCCESS/FAILURE feedback. Hiding only the HUD with `OverlayConfig.showHudPanel(false)` does not disable highlights. The master `OverlayConfig.enabled(false)` disables all overlay visuals, including HUD and highlights.

Manual decoration is interaction-free:

```java
lens.locator(By.id("status"), "Order status").highlight();
lens.highlight(element, "Order status");
lens.highlight(element, "Saved", HighlightState.SUCCESS);
```

These calls do not click, type, focus, or scroll. Manual `ACTION` remains neutral. Manual `SUCCESS` chooses a visual state only: it emits a highlight diagnostic and never invents `ASSERTION_PASSED` or changes the test result.

The highlight uses a pointer-transparent overlay. It does not click, receive the click, change the target's state, or make the element actionable. The subsequent activation contract remains native `WebElement.click()` with only explicit overlay recovery and configured locator retries.

For compatibility, `OverlayConfig.highlightColor(...)` supplies `actionColor` and `decorationDurationMs(...)` supplies `durationMs` only when that typed field was not explicitly set. Explicit `HighlightOptions` fields win regardless of builder call order. The legacy duration continues to control arrows and other historical decorations. `UiLocatorOptions.highlightBeforeAction()` remains a retained option but is not consulted by the current locator implementation.

[HUD Studio](hud-studio.md) configures and previews all five states. The lower-level parent/closest helpers remain documented under [advanced explicit visual helpers](../advanced/js-overlay-debug.md#hud-and-explicit-visual-helpers).

## Wait feedback

Wait feedback shows that a condition is still being polled and reports its progress through the HUD/log/trace pipeline. It appears during explicit [element waits](../elements/waiting.md) and relevant retry loops, helping distinguish an active wait from a stalled test.

```java
lens.getByTestId("results").waitUntilVisible();
```

The visual feedback does not change the Selenium condition, polling interval, or deadline. Configure those through [`UiLocatorOptions.timeout(...)` and `pollInterval(...)`](../reference/configuration.md#uilocatoroptions).

## Assertion feedback

Assertion feedback makes fluent assertion progress visible. Reporter events distinguish assertion start, retries, pass, timeout, and failure, which is useful when an expected UI state arrives late or never appears.

```java
lens.getByTestId("status").expect().toHaveText("Saved");
```

Assertions use their own polling and comparison settings. See [Element assertions](../elements/assertions.md) and [`UiAssertionOptions`](../reference/configuration.md#uiassertionoptions). Diagnostic previews are bounded by the options, but page content and screenshots can still expose sensitive values.

## Legacy HUD theme and placement compatibility

`OverlayConfig` HUD position, offset, width, and `HudTheme` remain available for 0.2.x source compatibility:

```java
OverlayConfig overlayConfig = OverlayConfig.builder()
        .hudPosition(HudPosition.TOP_RIGHT)
        .hudTheme(HudThemePreset.HIGH_CONTRAST)
        .build();
```

When explicit `HudOptions` and legacy HUD setters are combined, `HudOptions` is authoritative for every overlapping value regardless of builder call order. Without explicit `HudOptions`, legacy settings retain their historical behavior. Custom `HudTheme` strings are trusted CSS; new code should prefer the bounded product model generated by HUD Studio.

## Low-level HUD API

`HudPanel`, `ApiOverlayPanel`, JS resource wrappers, `OverlayRootManager`, and related `*Js` types are formally public but intended for custom integrations and internal-style use. See [advanced/low-level API](../reference/advanced-low-level.md).
