# Failure bundles

Every final `FAILED` facade session creates a best-effort, versioned failure bundle by default. This includes `finishFailed(...)`, `finishFailed(null)`, and a passed test rejected by `FAIL_ON_ANY_RETRY` or `FAIL_AFTER_N`. `PASSED`, `SKIPPED`, `REPORT_ONLY`, and `WARN` outcomes do not create one. Finalization never closes the driver.

The session directory retains `trace.json`, `report.html`, and `failure-diagnostic.png`. `failure-bundle/` contains `manifest.json`, failure, context, trace-derived diagnostics, runtime, allowlisted configuration, manual-network summary, and `failure-clean.png`. `failure-bundle.zip` contains the manifest, every successfully captured component, the final reports, and the diagnostic screenshot. ZIP entries are relative, sorted, confined to the session directory, and never follow symlinks.

The diagnostic screenshot is taken with the current HUD/highlight. For the clean screenshot only the `selenium-overlay-host` is temporarily hidden and restored in `finally`; application DOM, frame, window, and failed actions are not touched. Normal `cleanupHudOnFinish` runs later.

## Safe defaults and complete capture

Raw page source and browser console are disabled by default because they can contain credentials, personal data, tokens, or application secrets. They are not redacted. Enable them deliberately:

```java
TestLensOptions options = TestLensOptions.builder()
        .failureBundleOptions(FailureBundleOptions.complete())
        .build();
```

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
