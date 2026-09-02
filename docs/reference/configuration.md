# Configuration builders

All option objects are immutable after `build()` unless their API explicitly exposes a mutable collaborator. Durations described as positive reject null/zero/negative values. Defaults below come from the current Java field initializers and constructors.

## TestLensOptions

`TestLensOptions.defaults()` or `builder()`; API level **Recommended**.

| Builder method | Type | Default | Effect / validation |
| --- | --- | --- | --- |
| `overlayConfig(value)` | `OverlayConfig` | `OverlayConfig.builder().build()` | Visual runtime behavior; null is rejected when options are built/used. |
| `locatorOptions(value)` | `UiLocatorOptions` | `UiLocatorOptions.defaults()` | Locator wait, retry, and actionability. The nested retained `highlightBeforeAction` value is currently not consulted by `UiLocator`. |
| `outputRoot(value)` | `Path` | `target/ui-test-lens` | Root for per-session artifacts; must be usable/non-null. Do not point at a tracked or public directory. |
| `screenshotOnFailure(value)` | `boolean` | `true` | Enables best-effort automatic screenshot for a final `FAILED` result, including policy-induced failure; final passed/skipped results never request it. |
| `failureBundleOptions(value)` | `FailureBundleOptions` | safe defaults | Configures automatic bundle collectors and limits for final `FAILED`; raw page source and browser console default to disabled. |
| `cleanupHudOnFinish(value)` | `boolean` | `true` | Best-effort removal of injected visual artifacts. |
| `retryOutcomePolicy(value)` | `RetryOutcomePolicy` | `REPORT_ONLY` | Policy evaluated only by `finishPassed()` when the session contains recovery retries. |
| `allowedRetries(value)` | `int` | `0` | Non-negative number allowed by `FAIL_AFTER_N`; failure occurs when `totalRetries > allowedRetries`. |

Accessor methods have the same names without arguments; `build()` returns options.

`FailureBundleOptions.defaults()` enables the bundle, diagnostic/clean screenshots, context, trace diagnostics, manual-network summary, runtime/configuration allowlists, and ZIP. It disables page source and browser console. `complete()` explicitly enables both sensitive collectors. Defaults are 5 MiB per text artifact and 1000 console entries. `screenshotOnFailure(false)` disables both screenshots only; `FailureBundleOptions.enabled(false)` disables the additional bundle while preserving the historical screenshot setting.

Runner adapters consume the same immutable options per invocation. JUnit 5 configures them with `TestLensExtension.Builder.lensOptions(...)`. TestNG factories override `TestLensTestNgFactory.lensOptions()` and may override `sessionName(ITestResult)`; the factory type must have a public no-argument constructor and is instantiated afresh for each physical invocation. See [TestNG integration](../integrations/testng.md).

## UiLocatorOptions

| Builder method | Type | Default | Effect / validation |
| --- | --- | --- | --- |
| `actionabilityOptions(value)` | `ActionabilityOptions` | defaults | Pre-action readiness checks. |
| `timeout(value)` | `Duration` | 3 s | Explicit wait deadline; positive. |
| `pollInterval(value)` | `Duration` | 100 ms | Explicit wait/retry polling; positive. |
| `maxRetries(value)` | `int` | 3 | Operation attempts; at least 1. |
| `retryOnStaleElement(value)` | `boolean` | `true` | Retry stale element with fresh resolution. |
| `retryOnClickIntercepted(value)` | `boolean` | `true` | Retry intercepted action. |
| `retryOnNotInteractable(value)` | `boolean` | `true` | Retry not-interactable action. |
| `highlightBeforeAction(value)` | `boolean` | `true` | Retained public option, but the current `UiLocator` implementation does not consult it. Click decoration is controlled by `OverlayConfig.enabled`; other locator actions currently do not highlight. No effect on waits. |

## UiAssertionOptions

| Builder method | Type | Default | Effect / validation |
| --- | --- | --- | --- |
| `timeout(value)` | `Duration` | 3 s | Assertion deadline; positive. |
| `pollInterval(value)` | `Duration` | 100 ms | Assertion polling; positive. |
| `normalizeWhitespace(value)` | `boolean` | `true` | Collapse whitespace before text/value comparison. |
| `caseSensitive(value)` | `boolean` | `true` | Case-sensitive comparison. |
| `actualTextPreviewLimit(value)` | `int` | 300 | Diagnostic preview length; non-negative. |
| `trimText(value)` | `boolean` | `true` | Trim before comparison. |
| `failFastOnMissingElement(value)` | `boolean` | `false` | For assertions requiring presence, `false` retries a missing element until pass/`TIMED_OUT`; `true` ends the first missing observation as `FAILED`. `toBeHidden` still passes for missing elements. Stale elements remain separately retryable and time out as `STALE_ELEMENT`. |

Comparison settings affect retry success; preview length affects diagnostics/reports/HUD, not matching input itself.

## ActionabilityOptions

| Builder method | Type | Default | Notes |
| --- | --- | --- | --- |
| `timeout`, `pollInterval` | `Duration` | 3 s / 100 ms | Positive; readiness polling. |
| `checkAttached` | `boolean` | true | Require DOM attachment. |
| `checkVisible` | `boolean` | true | Require displayed target. |
| `checkEnabled` | `boolean` | true | Require enabled target. |
| `checkStableBounds` | `boolean` | true | Sample element bounds. |
| `scrollIntoView` | `boolean` | true | May scroll the page before action. |
| `checkReceivesClickPoint` | `boolean` | true | Hit-test click point. |
| `checkOverlayPolicy` | `boolean` | true | Include configured blocker policy. |
| `stableBoundsSamples` | `int` | 2 | At least 2. |
| `stableBoundsSampleDelay` | `Duration` | 100 ms | Non-negative. |

## OverlayConfig

| Builder method | Type | Default | Effect / validation |
| --- | --- | --- | --- |
| `enabled(value)` | boolean | true | Master visual overlay switch. |
| `showHudPanel(value)` | boolean | true | Show runtime HUD. |
| `decorationDurationMs(ms)` | long | 1500 | Highlight/decoration duration; `>= 0`. |
| `globalOverlayCloseButtonSelector(selector)` | String | null | Optional global blocker close selector; trusted CSS. |
| `hudPosition(position)` | `HudPosition` | `BOTTOM_RIGHT` | HUD anchor; non-null. |
| `hudOffset(x,y)` | int,int | 10,10 | CSS pixel offsets. |
| `hudMaxWidthPx(value)` | int | 520 | Positive maximum width. |
| `hudTheme(theme)` | `HudTheme` | default theme | Custom immutable theme; non-null. |
| `hudTheme(preset)` | `HudThemePreset` | `DEFAULT` | Select preset and derived theme; non-null. |
| `highlightColor(value)` | String | `#ffeb3b` | Non-blank trusted CSS color. |

Accessors use JavaBean `is...`/`get...` names shown in the [catalog](public-api-catalog.md).

## HudTheme

Factories: `defaultTheme`, `dark`, `light`, `glass`, `compact`, `highContrast`, `blackAndColors`, `minimal`, `fromPreset`, and `builder`.

<!-- API SIGNATURES: io.github.testlens.hud.HudTheme -->
```java
static HudTheme.Builder builder()
static HudTheme defaultTheme()
static HudTheme dark()
static HudTheme light()
static HudTheme glass()
static HudTheme compact()
static HudTheme highContrast()
static HudTheme blackAndColors()
static HudTheme minimal()
static HudTheme fromPreset(HudThemePreset preset)
Map<String, Object> toMap()
```

The builder exposes **all** of: `background`, `foreground`, `mutedForeground`, `accent`, `success`, `warning`, `danger`, `borderColor`, `fontFamily`, `boxShadow`, `backdropFilter` (`String`); `borderRadiusPx`, `fontSizePx`, `zIndex`, `paddingPx`, `gapPx`, `maxHeightPx` (`Integer`); and `opacity(Double)`. Blank strings normalize to null/inherited values. Sizes are validated non-negative/positive according to property; opacity is 0..1. `toMap()` is consumed by overlay JavaScript. Theme strings are trusted CSS, not untrusted user input.

## UiStepOptions

| Method | Type | Default | Effect |
| --- | --- | --- | --- |
| `failFast` | boolean | true | Throw `UiStepError` on body failure. |
| `logToHud` | boolean | true | Runtime visual feedback. |
| `captureNestedEvents` | boolean | true | Associate nested operations. |
| `includeStackTrace` | boolean | false | Persist stack; may disclose code/environment. |
| `captureScreenshotOnFailure` | boolean | false | Best-effort artifact. |
| `screenshotCaptureOptions` | options | defaults | Screenshot path/naming/attachment. |
| `messagePreviewLimit` | int | 500 | Non-negative diagnostic bound. |

## BusinessAssertionOptions

`collectFailures(boolean)=true`, `failFast(boolean)=false`, `includeStackTrace(boolean)=false`, and `messagePreviewLimit(int)=500` (non-negative). Collection/fail-fast controls whether failures accumulate or throw; stack/message settings affect stored artifacts.

## ScreenshotCaptureOptions

| Method | Default | Effect |
| --- | --- | --- |
| `outputDirectory(Path)` | `target/ui-test-lens/screenshots` | Destination; protect from source control/public access. |
| `fileNamePrefix(String)` | `screenshot` | Sanitized/validated filename prefix. |
| `includeTimestamp(boolean)` | true | Unique timestamp suffix. |
| `overwriteExisting(boolean)` | false | Whether an existing path may be replaced. |
| `attachToSession(boolean)` | true | Adds trace evidence when a session exists. |

## VideoEvidenceOptions

`source(VideoEvidenceSource)=CUSTOM`, `mediaType(String)="video/mp4"`, `validateLocalFileExists(boolean)=false`, `attachToSession(boolean)=true`, and `metadata(key,value)`/`metadata(Map)`. Validation applies to local paths; it does not fetch/validate remote URLs. Blank media type falls back to `video/mp4`; null/blank metadata keys and null values are ignored. Metadata may be persisted and must not contain secrets.

## Authentication options

`AuthStateOptions`: `label`, `role`, `origin`, `expiresAt` default null; `includeCookies`, `includeLocalStorage`, `includeSessionStorage` default true; repeatable `labelEntry` and `note` maps default empty. Origin/expiry affect capture metadata and later validation. Every included store can contain credentials.

`AuthRestoreOptions`: `navigateToOrigin`, `clearExistingCookies`, `clearExistingStorage`, `restoreCookies`, `restoreLocalStorage`, `restoreSessionStorage`, `validateOrigin`, and `failIfExpired` all default true. Navigation and clearing mutate browser state; disable only with a deliberate lifecycle plan.

## Network options

`NetworkDiagnosticsOptions`: `captureMode(MANUAL)`, `includeHeaders(false)`, `maskSensitiveHeaders(true)`, `failedStatusThreshold(400)` (values `<= 0` normalize to 400), and repeatable `ignoreUrlPattern(regex)` (regex validated). `MANUAL` is the only implemented active mode. `OFF` is inactive; `AUTO`, `BIDI`, and `PERFORMANCE_LOGS` return `UNSUPPORTED` without fallback. The retained `attachToSession(boolean)` option is deprecated for removal in 0.2.0 and has no automatic effect. Attach only through an explicit `NetworkDiagnostics.attachToSession(...)` call. Headers and exported artifacts can contain sensitive data.

`NetworkWaitCondition`: URL substring/regex/exact URL, method, exact/min/max/range status are unset by default; timeout 5 s; poll interval 100 ms; `includeFailedResponses=true`; `matchRequestOnly=false`. Regex is compiled/validated; durations must be positive and status bounds must be coherent.

## Trace and report options

- `TraceJsonExportOptions`: stack traces, artifact metadata, and missing artifacts default true; optional artifact base directory.
- `TraceHtmlExportOptions`: title `Selenium Test Lens Trace`; JSON/artifacts/attributes/grouping/type summary/failure summary/artifact preview/duration summary true; stack traces/collapse-passed/compact false; theme `AUTO`; message max 1000 (non-negative).
- `TraceBundleExportOptions`: stack traces/artifact metadata/missing artifacts/copy artifacts true; name `Selenium Test Lens Report`; output `target/ui-test-lens-report`; theme `AUTO`.
- `LogExportOptions`: record fields are `includeMetadata`, `includeThrowable`, `prettyPrint`, and `maxFieldLength`. `defaults()` is true/true/true/500; `compact()` is false/false/false/500; non-positive maximum length normalizes to 500.

## ReactActionabilityOptions

Base options default to `ActionabilityOptions.defaults()`. Checks for ARIA disabled/busy, data loading/pending, progressbar, spinner, skeleton, focus lock, and dialog/modal all default true. Timeout is 3 s and polling 100 ms (positive). `customBusyIndicator(By)` and `customBlockingOverlay(By)` append non-null locators. These checks affect React readiness only, not standard locator retry configuration.
