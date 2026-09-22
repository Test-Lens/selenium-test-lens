# Configuration

Test Lens attaches to a `WebDriver` created by the consumer. `TestLensOptions` collects session-level behavior; specialized immutable builders configure individual capabilities. Defaults are usable without configuration, and no option transfers driver ownership to the main facade.

The tables below are a decision map. Exact builder methods, ranges, and defaults are in the [configuration reference](reference/configuration.md).

## Core

```java
TestLensOptions options = TestLensOptions.builder()
        .locatorOptions(UiLocatorOptions.builder()
                .timeout(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .javascriptClickFallback(true)
                .build())
        .outputRoot(Path.of("target", "ui-test-lens"))
        .build();

TestLens lens = TestLens.attach(driver, options);
```

| Configuration | Default | Contract |
| --- | --- | --- |
| `locatorOptions(...)` | `UiLocatorOptions.defaults()` | Instance-wide locator timeout, polling, operation retries, actionability, and Smart Click JS fallback. The same options reach ordinary and semantic locator factories. |
| `outputRoot(...)` | `target/ui-test-lens` | Root for reports and diagnostics. Keep it outside tracked or public content. |
| `cleanupHudOnFinish(...)` | `true` | Best-effort removal of Test Lens browser decorations during finalization. |
| `retryOutcomePolicy(...)` | `REPORT_ONLY` | Controls an otherwise passed session that used recovery retries; does not redefine explicit failed/skipped outcomes. |

## HUD

`HudOptions` is the preferred 0.3.0 product model:

```java
HudOptions hud = HudOptions.builder()
        .preset(HudPreset.COMPACT)
        .position(HudPosition.TOP_RIGHT)
        .headerLayout(HudHeaderLayout.AUTO)
        .backgroundOpacity(0.9)
        .showTimestamps(true)
        .timestampPattern("yyyy-MM-dd HH:mm:ss.SSSSSS XXX")
        .timestampZone(ZoneId.of("Europe/Warsaw"))
        .typography(HudTypography.builder().timestampFontSizePx(10).build())
        .showNetwork(false)
        .build();

TestLensOptions options = TestLensOptions.builder().hud(hud).build();
```

Precedence is deterministic:

```text
selected preset
-> explicit HudOptions builder overrides
-> validated immutable options
-> viewport-safe effective rendering
```

Explicit `HudOptions` is authoritative over overlapping legacy `OverlayConfig` position, offset, width, and `HudTheme` values. When no explicit `HudOptions` is supplied, those legacy setters retain their historical behavior. `OverlayConfig` still controls the master overlay switch, HUD visibility, highlight color, and decoration duration.

| Area | Default / range | Security or compatibility note |
| --- | --- | --- |
| Preset | `COMPACT`; also `MINIMAL`, `STANDARD`, `DEBUG` | Preset is only a base; explicit overrides win regardless of call order. |
| Header | `AUTO`; also `INLINE`, `STACKED` | TEST/STEP are atomic single-line items with ellipsis; PIPE is separate metadata. |
| Position | `BOTTOM_RIGHT`; four corners, offsets 0–500 px | Effective offsets and dimensions are clamped to the viewport; stored options are unchanged. |
| Size | width 240–960, panel 120–1000, log 80–720 px | Effective log height is limited by configured log height, panel content, and viewport. |
| Typography | local `UI_SANS` except Debug uses `MONOSPACE`; timestamp 9 px | `SYSTEM`, `MONOSPACE`, and `UI_SANS` are local stacks; section overrides inherit from the global preset. Timestamp size is independently bounded to 8–18 px. No font URLs. |
| Scrollbar | `SUBTLE`; Debug uses `STANDARD`; `NATIVE` available | Chromium honors bounded 4–14 px width; Firefox maps to engine-supported widths while preserving colors. |
| Colors | validated `#RRGGBB`; opacity 0–1 | No arbitrary CSS is accepted by `HudOptions`. |
| Timestamps | `ISO_UTC`; JVM system zone; no custom pattern | Presets remain shortcuts. `timestampPattern(...)` accepts Java `DateTimeFormatter` patterns and overrides the preset. `S` supports 1–9 fraction digits. |
| Branding | Test Lens mark in a 16 px rail | Custom logos are bounded, regular non-symlink PNG files; SVG, URLs, and HTML are rejected. |

[Customize the same runtime renderer in HUD Studio](observability/hud-studio.md).

Use `.systemTimestampZone()` to explicitly retain JVM-system behavior in a reusable builder. HUD
timestamps use the test runner JVM timezone by default, not the browser machine timezone, so a
remote BrowserStack browser cannot substitute its own system zone. Region `ZoneId` values apply
DST automatically; a fixed `ZoneOffset` does not. Formatting affects only HUD presentation and
never mutates the event `Instant`, trace/JSON/report data, ordering, durations, or evidence metadata.

## Element feedback

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

TestLensOptions options = TestLensOptions.builder().hud(hud).highlights(highlights).build();
```

`enabled(false)` disables both manual and automatic state borders. `automaticFeedback(false)` keeps manual `lens.highlight(...)` and `locator.highlight()` available. HUD visibility is independent. A duration of zero renders the state and schedules its removal without an artificial visibility delay; border width accepts 1-16 px. Legacy `OverlayConfig.highlightColor` and `decorationDurationMs` feed only action color and highlight duration when those individual fields were not explicitly set. Explicit typed fields win regardless of setter order, while `decorationDurationMs` retains its separate legacy meaning for arrows and other decorations.

For a consumer `LensTestBase`, create `TestLensOptions` once in its setup/option factory and attach the facade with those options. Use `lens.highlight(element, label)` instead of allocating an additional `JsOverlayDebug`; this keeps the active driver, session, redaction, logger, and cleanup ownership together.

## Evidence

| Configuration | Default | Contract |
| --- | --- | --- |
| `screenshotOnFailure(...)` | `true` | Requests the historical automatic screenshot only for a final failed outcome. Capture remains best effort. |
| `failureBundleOptions(...)` | bundle enabled, sensitive collectors off | Diagnostic/clean screenshots, context, trace diagnostics, network summary, allowlists, manifest, and ZIP are enabled; page source and console default off. |
| `ScreenshotCaptureOptions` | viewport capture | `FULL_PAGE` is opt-in and bounded by pixel/tile limits. |
| `VideoEvidenceOptions` | caller-supplied attachment | Test Lens attaches existing video; it does not record or visually redact it. |

See [Screenshots & evidence](observability/screenshots-evidence.md) and [Failure bundles](observability/failure-bundles.md).

## Visual Redaction

```java
VisualRedactionOptions visual = VisualRedactionOptions.builder()
        .mask(By.id("account-number"), VisualMaskMode.SOLID)
        .failurePolicy(VisualRedactionFailurePolicy.STRICT)
        .build();
```

`TestLensOptions.visualRedaction(...)` defaults to automatic SOLID masking of password inputs and the fail-closed `STRICT` policy. Explicit masks are required targets. `BEST_EFFORT` is an opt-in that can publish a partially masked screenshot with diagnostics. `solidColor` accepts `#RRGGBB`, blur radius is 2–32 px, and padding is 0–32 px.

This controls screenshot pixels only. Configure diagnostic text separately with `RedactionPolicy`. Review the [complete visual-redaction boundary](security/visual-redaction.md) before publishing evidence.

## Network and WebDriver BiDi

`NetworkDiagnosticsOptions` defaults to explicit `MANUAL` capture, headers off, sensitive-header masking on, failed status threshold 400, 10,000 captured events, and the default HUD filter. `BIDI` and `AUTO` require a WebDriver session created with BiDi enabled and never fall back to performance logs.

`NetworkWaitCondition` defines URL, method, status, timeout, and polling criteria. Passive capture is diagnostics, not interception, stubbing, or body virtualization. See [Network diagnostics](advanced/network.md).

## Managed Auth State

`AuthStateRequest` combines a process-local key, persisted path, login callback, and tri-state validator. The manager performs a bounded restore–validate–recreate lifecycle with at most one login. `INCONCLUSIVE` and validator exceptions do not trigger login or overwrite old bytes.

Low-level `AuthStateOptions` and `AuthRestoreOptions` still configure direct cookie and Web Storage capture/restore. Origin validation, navigation, clearing, component selection, and expiry checks default on. Persisted auth state can contain live authentication material and is outside report redaction. See [Managed Auth State](advanced/auth-state.md).

## Managed Test State

Scenario and suite managers have no builder because their lifetime is the configuration:

- `scenarioState()` and `resources()` bind to the active physical invocation and close at finalization;
- `suiteState()` binds to one JUnit root, TestNG `ISuite`, or explicit `TestRunScope`;
- a manual Lens not attached through a managed run scope has no suite state and fails fast.

State is in memory only and is not automatically evidence. See [Managed Test State & Resources](features/managed-test-state.md).

## Reports and upload

Report exporters use their own immutable options for output shape and theme. `ReportUploadOptions` is deliberately separate from `TestLensOptions`: configuration alone cannot perform network I/O. It requires an explicit HTTP(S) endpoint and controls bounded payloads, timeouts, retries, proxy routing, headers, and redaction. Upload remains an explicit post-finalization call. See [Report upload](observability/report-upload.md).

## Runner integrations

- JUnit 5 passes immutable options through `TestLensExtension.Builder.lensOptions(...)` and owns only drivers created by its configured factory.
- TestNG factories return `lensOptions()` per physical invocation; the listener and factory define the runner lifecycle.
- Allure uses separate `AllureTestLensOptions` at attachment time and consumes only finalized evidence.

See [JUnit 5](integrations/junit5.md), [TestNG](integrations/testng.md), and [Allure](integrations/allure.md).

## Text redaction

`TestLensOptions.redactionPolicy(...)` defaults to `RedactionPolicy.defaults()`. The immutable policy protects diagnostic copies before they reach HUD, trace, sinks, network diagnostics, reports, and text bundle components. Passing null restores defaults; `RedactionPolicy.disabled()` is an explicit opt-out that can expose secrets.

Configuration exports include policy state and counts, never configured literal secret values. See [Sensitive-data redaction](security/redaction.md).

## Detailed reference

Use the [configuration builders reference](reference/configuration.md) for the complete tables and validation ranges, or the [generated public API catalog](reference/public-api-catalog.md) for exact signatures.
