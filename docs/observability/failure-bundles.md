# Failure bundles

!!! info "Coming in 0.2.0"
    Automatic failure bundles are part of the current development line and are not available in Maven Central `0.1.0`.

Every final `FAILED` facade session creates a best-effort, versioned failure bundle by default. This includes `finishFailed(...)`, `finishFailed(null)`, and a passed test rejected by `FAIL_ON_ANY_RETRY` or `FAIL_AFTER_N`. `PASSED`, `SKIPPED`, `REPORT_ONLY`, and `WARN` outcomes do not create one. Finalization never closes the driver.

The bundle is the final step of the observable evidence path—operation events feed trace, finalization writes HTML/JSON, and a failed outcome assembles those reports with available browser diagnostics. It is not a replacement for trace, and it does not turn collector availability into a test-result decision.

The session directory retains `trace.json`, `report.html`, and `failure-diagnostic.png`. `failure-bundle/` contains `manifest.json`, failure, context, trace-derived diagnostics, runtime, allowlisted configuration, the current network summary, and `failure-clean.png`. The network snapshot reports requested and active modes, status, requests/responses/failures, ignored events, and dropped events; it is derived from the same redacted immutable summary boundary used by network assertions and never starts capture during failure handling. Active Lens-owned capture is stopped only after this snapshot and before `SESSION_FINISHED`. `failure-bundle.zip` contains the manifest, every successfully captured component, the final reports, and the diagnostic screenshot.

The bundle reports the observed lifecycle state; `STOPPED`, `UNSUPPORTED`, or `FAILED` is not rewritten as “zero network failures.” Bundle collection itself does not validate or start capture, while an explicit `assertNoFailedRequests()` rejects a generation that never became active.

`NetworkHudFilter` does not alter this snapshot or any captured network evidence. A raw entry hidden from the on-page HUD remains in network events, summaries, trace, reports, network JSON, and the failure bundle. Only capture-level `ignoreUrlPattern(...)` removes it.

The diagnostic screenshot is taken with the current HUD/highlight. For the clean screenshot only the `selenium-overlay-host` is temporarily hidden and restored in `finally`; application DOM, frame, window, and failed actions are not touched. Normal `cleanupHudOnFinish` runs later.

Both images use `VIEWPORT` by default. Full-page failure evidence is an explicit development-line option:

```java
FailureBundleOptions bundle = FailureBundleOptions.builder()
        .screenshotCaptureMode(ScreenshotCaptureMode.FULL_PAGE)
        .build();
```

The same mode applies to diagnostic and clean images, whose names remain `failure-diagnostic.png` and `failure-clean.png`. The manifest records requested/completed mode, image dimensions, tile count, and a safe failure reason. A stitching failure remains a collector failure: it cannot replace the original test failure or cause finalization to repeat. See [Screenshots and evidence](screenshots-evidence.md#portable-full-page-capture).

## Safe defaults and complete capture

Raw page source and browser console are disabled by default because they can contain credentials, personal data, tokens, or application secrets. When enabled, complete JSON is redacted structurally—including sensitive values containing apostrophes or escaped quotes—while mixed or malformed content uses the fail-closed tolerant text fallback. Recognized structured secrets and configured literal values receive best-effort central redaction before writing. Enable them deliberately:

```java
TestLensOptions options = TestLensOptions.builder()
        .failureBundleOptions(FailureBundleOptions.complete())
        .build();
```

Redaction cannot infer arbitrary personal data, and screenshots/video are not pixel-redacted. The configuration component records only the redaction enabled flag, replacement, and counts of caller-added keys/secrets. Replayable auth/storage state is not transformed or automatically bundled. See [Sensitive-data redaction](../security/redaction.md).

The equivalent explicit builder is:

```java
FailureBundleOptions bundle = FailureBundleOptions.builder()
        .pageSource(true)
        .browserConsole(true)
        .maxTextArtifactBytes(5L * 1024 * 1024)
        .maxConsoleEntries(1_000)
        .build();
```

`screenshotOnFailure(false)` disables both failure screenshots but leaves the other collectors active. `FailureBundleOptions.enabled(false)` disables the additional bundle and ZIP but preserves the historical screenshot controlled by `screenshotOnFailure`.

Browser logs are read once through `driver.manage().logs().get(LogType.BROWSER)` and may consume the current Selenium log buffer. Firefox may report this collector as `UNSUPPORTED`; no BiDi/CDP fallback is attempted. Page source is read exactly once from the current frame context and no HTTP request is made.

## Manifest and resilience

Every component is described as `CAPTURED`, `EMPTY`, `SKIPPED`, `UNSUPPORTED`, `FAILED`, `TRUNCATED`, or `SKIPPED_TOO_LARGE`. The default text limit is 5 MiB and console limit is 1000 entries. Exceeding a limit is explicit; data is never silently shortened.

Collectors are independent. One unavailable probe or write does not stop the others, replace the original throwable, change session status, or prevent runner-owned `driver.quit()`. Capture errors appear in `TestLensFinalizationResult.diagnosticFailures()`; for retry-policy failure they are suppressed on the propagated violation.

`runtime.json` exports only browser name/version/platform, Selenium, Java, OS, window, and viewport data. It does not dump capabilities. `configuration.json` is an allowlist and excludes environment variables, system-property dumps, cookies, storage, headers, arbitrary capabilities, passwords, and tokens.
