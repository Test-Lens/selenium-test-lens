# Result and data types

## TestLensFinalizationResult

```java
record TestLensFinalizationResult(
    UiTestLensSession session,
    Path outputDirectory,
    Path jsonReport,
    Path htmlReport,
    Path failureScreenshot,
    List<Throwable> diagnosticFailures)
boolean fullySuccessful()
RetrySummary retrySummary()
Optional<Path> failureBundleDirectory()
Optional<Path> failureBundleManifest()
Optional<Path> failureBundleArchive()
```

`session` and all record-component paths can be null when no session exists or a corresponding operation failed. `failureScreenshot` retains its existing meaning: the diagnostic screenshot with current Test Lens HUD/highlight. It is null for a final `PASSED` or `SKIPPED` session, and can also be null when failed-session capture is disabled or unsuccessful. The three bundle methods are computed from the session output and do not change the public record constructor. `diagnosticFailures` is immutable/non-null. `retrySummary()` is computed from the attached session. `fullySuccessful()` means only that diagnostics are empty; it does not redefine the outcome.

## Operation results

- `UiAssertionResult`, `UiStepResult`, `BusinessAssertionResult`: status, failure reason/detail, operation description, attempts/elapsed/message as applicable.
- `ScreenshotCaptureResult`: `status`, `name`, nullable `path`, nullable `artifact`, `message`, nullable `exception`, non-null `capturedAt`, and `isCaptured()`.
- `VideoEvidenceResult`: `status`, `name`, nullable `path`, URL string, nullable `artifact`, `source`, `message`, nullable `exception`, non-null `attachedAt`, immutable metadata, and `isAttached()`.
- `AuthRestoreResult`: status plus restored cookie/local/session-storage counts, exception, elapsed.
- `NetworkDiagnosticsResult`, `NetworkWaitResult`, `NetworkSummary`: capture/assert/wait status, matches/failures/counts and diagnostic context.
- `ActionabilityReport`/`ActionabilityResult` and React equivalents: overall/per-check readiness with details.
- `OverlayHandlingResult`: policy outcome and executed action details.

Nullable/empty semantics differ by result; consult the relevant functional page and the [exact accessor/factory signatures](public-api-catalog.md). Exceptions stored in results may contain driver/session/environment details.

Locator resolution plumbing is implementation-private. Normal locator failures use the supported `UiLocatorException`; no separate low-level locator result is returned by the recommended API.
