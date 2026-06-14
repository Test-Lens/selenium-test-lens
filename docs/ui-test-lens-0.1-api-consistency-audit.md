# UI Test Lens 0.1 API consistency audit

## Scope

This audit reviews the public API surface and documentation consistency before a 0.1 release candidate. It covers production public classes, primary entry points, naming conventions, option defaults, event names, examples, module boundaries, and safety/privacy wording.

No code changes are proposed in this document. Larger cleanup items should be handled as explicit pre-1.0 follow-up commits.

## Summary

The current API is coherent enough for a pre-1.0 `0.1.0-SNAPSHOT` baseline. The main entry point remains `JsOverlayDebug`, while specialized APIs are grouped under clear domains: overlay policy, actionability, locators, assertions, business checks, steps, evidence, auth, and network diagnostics.

No blocking API issue was found. The main consistency risks are expected for a fast-growing pre-1.0 codebase:

- several helper/reporting/exporter/parser classes are public even though they look more like implementation details,
- legacy Selenium action/helper classes are still public next to newer fluent APIs,
- there are pairs such as `attachVideo(...)` and `attachVideoFile(...)` that should be documented carefully before a stable release,
- test fixtures use obvious dummy secret-like strings, which is acceptable but worth cleaning up later to avoid noisy secret scans.

## Public entry points reviewed

Primary `JsOverlayDebug` entry points reviewed:

```java
overlay.setOverlayPolicy(...);
overlay.checkActionability(...);
overlay.locator(...);
overlay.getByTestId(...);
overlay.expect(...);
overlay.business(...);
overlay.step(...);
overlay.setStep(...);
overlay.startSession(...);
overlay.attachSession(...);
overlay.attachScreenshot(...);
overlay.captureScreenshot(...);
overlay.attachVideo(...);
overlay.attachVideoFile(...);
overlay.attachVideoUrl(...);
overlay.attachArtifact(...);
overlay.auth();
overlay.captureAuthState(...);
overlay.restoreAuthState(...);
overlay.network();
overlay.attachNetworkLog(...);
overlay.exportTraceHtml(...);
```

Assessment:

- The names are broadly consistent with the implemented concepts.
- `step(...)` versus `setStep(...)` is documented: `setStep(...)` updates the HUD label, while `step(...)` executes and records a measured step.
- `attachScreenshot(...)` versus `captureScreenshot(...)` is a deliberate distinction: attach references an existing artifact, capture uses Selenium `TakesScreenshot`.
- `attachVideo(...)`, `attachVideoFile(...)`, and `attachVideoUrl(...)` are a mild naming duplication. This is not a blocker, but pre-1.0 docs should keep `attachVideoFile(...)` and `attachVideoUrl(...)` as the preferred explicit APIs.
- `auth()` and `network()` are good domain entry points and avoid overloading `JsOverlayDebug` with every operation directly.

## Module boundary checks

Boundary scans were run for core/overlay Selenium imports, Selenium-to-React coupling, forbidden legacy imports, and console output.

| Check | Result | Notes |
| --- | --- | --- |
| `ui-test-lens-core` Selenium-free scan | OK | No production Selenium imports found. |
| `ui-test-lens-overlay` Selenium-free scan | OK | No production Selenium imports found. |
| `ui-test-lens-selenium -> ui-test-lens-react` scan | OK | No React coupling found in Selenium production code. |
| Forbidden imports scan | OK | No `LogWraper`, `TimeStamp`, `ContentIssueCollector`, `LocalDateTimeUtils`, or `io.restassured` production imports found. |
| `System.out/System.err` production scan | OK | No production console output found. |

## Public class visibility review

Most public model/API classes are intentional because this project exposes structured diagnostics and user-extensible configuration. Examples include options, result, status, failure reason, artifact, auth state, and network event types.

Classes that are intentionally public:

- Core logging: `UiTestLensLogger`, log entries, log levels/statuses, sinks, exporters.
- Core trace/evidence: `UiTestLensSession`, trace events/artifacts/failures/metadata, JSON/HTML exporters.
- Selenium user APIs: `JsOverlayDebug`, `OverlayPolicy`, `ActionabilityChecker`, `UiLocator`, `UiExpect`, `BusinessAssertions`, step/evidence/auth/network APIs.
- React user APIs: `ReactSupport`, `ReactSafeExecutor`, `ReactSelectHelper`, `ReactActionabilityChecker`, options/report/result models.

Classes that look like pre-1.0 visibility cleanup candidates:

- `TraceHtmlEscaper` and `TraceHtmlReportSection`: useful internally to the HTML exporter, probably not primary API.
- `UiAssertionReporter`, `BusinessAssertionReporter`, and `UiStepReporter`: currently public, but mainly internal event formatting/logging helpers.
- `AuthStateJsonExporter` and `AuthStateJsonParser`: public utility classes; `AuthState.exportJson()`, `save(...)`, and `load(...)` are likely the preferred user API.
- `NetworkLogExporter`: public utility may be acceptable, but `NetworkDiagnostics.exportJson()` is the primary user API.
- Legacy public action/core helpers such as `AssertActions`, `HighlightActions`, `ScrollActions`, `SmartClickActions`, `SmartInputActions`, `TargetResolverActions`, `TypingActions`, `PopupDetector`, `BlockingOverlayHelper`, `PageWaits`, `ScriptExecutor`, and `Guards`: these may need a compatibility policy before a stable release.

No visibility changes were made in this audit to avoid accidental breaking changes.

## Naming consistency

Naming is mostly consistent:

- `Options` classes use builders and hold configuration.
- `Result` classes represent operation outcomes.
- `Status` enums represent high-level result states.
- `FailureReason` enums are used for diagnostic failure causes.
- `Error` is used for assertion-style failures, such as `UiAssertionError`, `BusinessAssertionError`, and `NetworkAssertionError`.
- `Exception` is used for operational failures, such as screenshot, video, auth state, locator, and network diagnostics exceptions.
- `Support` is used for React entry points.
- `Checker`, `Executor`, `Manager`, `Exporter`, and `Reporter` suffixes are semantically understandable.

Minor consistency observations:

- `NetworkResponseExpectation` is fluent/assertion-style and pairs well with `expectResponse()`, but it differs from the `UiExpect` naming pattern.
- `attachVideo(...)` is less explicit than `attachVideoFile(...)`; keep the explicit names prominent in docs.
- Some older classes do not use the newer domain package naming style. Treat them as compatibility surface until a migration guide exists.

## Builder/default options review

| Options class | Key defaults | Looks OK? | Notes |
| ------------- | ------------ | --------- | ----- |
| `OverlayHandler` | optional true, timeout 2s, fail-if-still-visible false | Yes | Good default for common popups. |
| `ActionabilityOptions` | timeout 3s, poll 100ms, core checks enabled, overlay policy enabled | Yes | Conservative default for Selenium checks. |
| `ReactActionabilityOptions` | base actionability defaults, timeout 3s, poll 100ms, aria/data/spinner/skeleton/dialog checks enabled | Yes | React heuristics are opt-configurable. |
| `UiLocatorOptions` | timeout 3s, poll 100ms, max retries 3, stale/intercept/not-interactable retry enabled, highlight before action enabled | Yes | Good for locator reliability; highlight default should be watched for report noise. |
| `UiAssertionOptions` | timeout 3s, poll 100ms, normalize whitespace true, case sensitive true, preview limit 300, fail-fast-on-missing false | Yes | Matches retryable assertion behavior. |
| `BusinessAssertionOptions` | collect failures true, fail fast false, stack traces false, preview limit 500 | Yes | Good business-friendly aggregate default. |
| `UiStepOptions` | fail fast true, log to HUD true, capture nested events true, stack traces false, screenshot-on-failure false | Yes | Non-breaking by default; screenshot capture is opt-in. |
| `ScreenshotCaptureOptions` | `target/ui-test-lens/screenshots`, timestamp true, overwrite false, attach to session true | Yes | Safe filesystem defaults. |
| `VideoEvidenceOptions` | custom source, `video/mp4`, validate local file false, attach to session true | Yes | Attachment-only behavior is clear. |
| `AuthStateOptions` | include cookies/localStorage/sessionStorage true, origin from current URL if possible | Yes | Safety caveat is documentation, not default behavior. |
| `AuthRestoreOptions` | navigate to origin true, clear existing cookies/storage true, restore all true, validate origin true, fail if expired true | Yes | Safer restore default. |
| `NetworkDiagnosticsOptions` | capture mode AUTO, headers omitted, sensitive headers masked if included, threshold 400, attach to session true | Yes | Manual fallback and privacy defaults are appropriate. |
| `NetworkWaitCondition` | timeout 5s, poll 100ms, include failed responses true, request-only false | Yes | Slightly longer than UI assertions; acceptable for network waits. |
| `TraceHtmlExportOptions` | title set, raw JSON/artifacts/attributes included, stack traces false, passed events expanded, message limit 1000 | Yes | Good default report detail without stack trace leakage. |

## Event naming review

`UiTestLensEventType` is broad but coherent. Most domain events use predictable suffixes:

- `_STARTED`
- `_PASSED`
- `_FAILED`
- `_TIMED_OUT`
- `_SKIPPED`
- `_RETRY`

Reviewed event groups include overlay policy, actionability, locator, assertion, business assertion, step, screenshot, video, auth, and network diagnostics/waits.

No duplicate semantic event names were found that require immediate correction. A later cleanup could separate low-level generic events (`GENERAL`, `STEP`, `ACTION`, `WAIT`, `ASSERTION`, `HUD`, `OVERLAY`) from specific structured events in documentation, but no enum change is recommended in this commit.

## Documentation and examples review

README and examples use the current public API shape:

- `ReactSupport.reactSafe(...)` and `ReactSupport.checkActionability(...)` are React-side entry points.
- No obsolete `JsOverlayDebug.reactSafe()` usage was found.
- Network docs describe `MANUAL` as implemented, `AUTO` as fallback, and `PERFORMANCE_LOGS`/`BIDI` as modeled/unsupported.
- Video docs describe attachments only and do not promise recording or provider download.
- Auth docs warn that state files can contain cookies/tokens and do not promise encryption.
- Trace docs describe the HTML report as static, not a full interactive trace viewer.
- Locator docs describe `getByRole`, `getByLabel`, and `getByText` as future work, not current API.

Examples:

- Executable examples without browser dependency: `LoggingExportExampleTest`, `CustomLoggerSinkExampleTest`.
- Browser/network/video/auth examples are disabled documentation-only tests.
- Disabled examples consistently use `@Disabled` with a real WebDriver, browser/network-capable WebDriver, authenticated app, or external video artifact explanation.

No documentation correction was required beyond adding this audit link to README.

## Safety/privacy review

Safety scan results:

- Documentation warnings mention cookies, tokens, passwords, and sensitive headers intentionally.
- No real secrets were found.
- Test code contains obvious dummy values such as `Bearer secret`, `token=secret`, `secret-token`, and sample auth cookie values. These are not real credentials, but they create expected secret-scan noise.
- Network headers are omitted by default and sensitive header names are masked when included.
- Auth state documentation warns not to commit generated state files.
- Screenshot/video/HTML trace docs warn indirectly through artifact/report behavior; users should still treat these as potentially sensitive generated artifacts.

## Findings

### OK

- Core and overlay production modules remain Selenium-free.
- Selenium module still has no React dependency.
- Public entry points are grouped by domain and match the current feature set.
- Builder defaults are mostly consistent and privacy-conscious.
- Event names are coherent enough for pre-1.0.
- README/examples do not promise network mocking, video recording, encrypted auth state, or an interactive trace viewer.
- Browser/network/video examples are disabled and documentation-only.

### Minor documentation cleanup

- Keep `attachVideoFile(...)` and `attachVideoUrl(...)` as the preferred names in user docs; mention `attachVideo(...)` as compatibility/convenience if needed.
- Add a short generated-artifacts privacy note near trace/screenshot/video docs if reports are expected to be shared outside CI.
- Consider making examples path/output guidance consistent across trace, screenshots, network logs, and auth state.

### Pre-1.0 API cleanup candidates

- Decide whether `TraceHtmlEscaper`, `TraceHtmlReportSection`, reporter classes, auth JSON parser/exporter, and network JSON exporter should remain public.
- Decide compatibility policy for legacy public Selenium helper classes under `actions` and `core`.
- Consider a clearer naming relationship between `UiExpect` and `NetworkResponseExpectation`.
- Consider whether `attachVideo(...)` should be deprecated later in favor of explicit `attachVideoFile(...)`.
- Consider replacing dummy secret-like test strings with less scanner-noisy values.

### Future feature work

- Add `getByText`, `getByLabel`, and `getByRole` locator helpers.
- Improve trace event mapping from locator/assertion/actionability/network operations into `UiTestLensSession`.
- Improve HTML trace UX.
- Add optional real browser network capture providers behind guarded support.
- Add provider-specific artifact discovery/download only if it remains optional.
- Add Maven Wrapper and publishing metadata when API naming is stable.

## Recommended follow-up tasks

1. Add `getByText`, `getByLabel`, and `getByRole` locators.
2. Improve trace event mapping from logger to session.
3. Polish HTML trace UX.
4. Add a real browser network capture provider behind optional/guarded support.
5. Add Maven Wrapper.
6. Add API migration guide from historical JsTestTools names.
7. Review public helper/reporter/exporter visibility before the first non-SNAPSHOT release.
8. Reduce dummy secret-scan noise in tests without weakening safety coverage.
