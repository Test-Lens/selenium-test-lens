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
```

`session` and all paths can be null when no session exists or a corresponding operation failed. `failureScreenshot` is normally null for passed finalization or when disabled/failed. `diagnosticFailures` is an immutable/non-null list of best-effort capture/export/cleanup failures. `fullySuccessful()` means only that this list is empty; it does not redefine the test outcome.

## Operation results

- `UiAssertionResult`, `UiStepResult`, `BusinessAssertionResult`: status, failure reason/detail, operation description, attempts/elapsed/message as applicable.
- `ScreenshotCaptureResult`: `status`, `name`, nullable `path`, nullable `artifact`, `message`, nullable `exception`, non-null `capturedAt`, and `isCaptured()`.
- `VideoEvidenceResult`: `status`, `name`, nullable `path`, URL string, nullable `artifact`, `source`, `message`, nullable `exception`, non-null `attachedAt`, immutable metadata, and `isAttached()`.
- `AuthRestoreResult`: status plus restored cookie/local/session-storage counts, exception, elapsed.
- `NetworkDiagnosticsResult`, `NetworkWaitResult`, `NetworkSummary`: capture/assert/wait status, matches/failures/counts and diagnostic context.
- `ActionabilityReport`/`ActionabilityResult` and React equivalents: overall/per-check readiness with details.
- `OverlayHandlingResult`: policy outcome and executed action details.

Nullable/empty semantics differ by result; consult the relevant functional page and the [exact accessor/factory signatures](public-api-catalog.md). Exceptions stored in results may contain driver/session/environment details.

`UiLocatorResult` and its builder/status/failure enums are binary-public but are not returned by the current recommended `UiLocator` operations. They are classified `INTERNAL_STYLE_PUBLIC`; normal locator failures use `UiLocatorException`.
