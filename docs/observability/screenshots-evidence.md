# Screenshots and evidence

Screenshots capture the browser's visible state as PNG evidence. Use an explicit screenshot at a meaningful checkpoint, or let failed-session finalization attempt one automatically. Explicit capture is opt-in; the failure screenshot is enabled by default through [`TestLensOptions.screenshotOnFailure`](../reference/configuration.md#testlensoptions).

## Automatic failure screenshot

Start and finalize a normal Test Lens session. When the test fails, pass the original failure to [`finishFailed(...)`](../reference/test-lens.md#creation-and-lifecycle):

```java
TestLens lens = TestLens.attach(driver);
lens.startSession("Checkout");

try {
    lens.getByRole("button", "Pay").click();
    lens.finishPassed();
} catch (Throwable failure) {
    lens.finishFailed(failure);
    throw failure;
}
```

Failed finalization first attempts `failure-diagnostic.png` with the current HUD/highlight, then temporarily hides only Test Lens artifacts for `failure-bundle/failure-clean.png`. Both are controlled by `screenshotOnFailure`; independent flags live in `FailureBundleOptions`. Previous HUD visibility is restored in `finally` before normal cleanup. No failed action, click, locator resolve, frame switch, or navigation is repeated. Passed and skipped finalization never requests either screenshot. See [Failure bundles](failure-bundles.md).

## Explicit screenshots

Call `captureScreenshot(...)` when a checkpoint is useful even if the test ultimately passes:

```java
ScreenshotCaptureResult screenshot = lens.captureScreenshot("Order summary");
if (!screenshot.isCaptured()) {
    System.err.println(screenshot.message());
}
```

Explicit capture is not automatic. It uses [`ScreenshotCaptureOptions.defaults()`](../reference/configuration.md#screenshotcaptureoptions) unless options are supplied.

### TestLens.captureScreenshot overloads

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
ScreenshotCaptureResult captureScreenshot(String name)
ScreenshotCaptureResult captureScreenshot(String name, ScreenshotCaptureOptions options)
```

The first uses defaults. Capture requires a driver implementing Selenium `TakesScreenshot`. It creates a PNG path and, by default, attaches a trace artifact when a session is active. Configure output directory, prefix, timestamps, overwrite behavior, and session attachment through [`ScreenshotCaptureOptions`](../reference/configuration.md#screenshotcaptureoptions).

The current `TestLens.captureScreenshot(...)` path represents unsupported-driver, I/O, and runtime capture failures in `ScreenshotCaptureResult`; it does not throw `ScreenshotCaptureException` for those failures. `ScreenshotCaptureException` remains a public exception type but is not thrown by the current `ScreenshotCapture.capture(...)` implementation.

<!-- SCREENSHOT TODO: assets/screenshots/failure-screenshot.png
Show an actual screenshot captured by finishFailed() for a deterministic failing test.
The application failure state and relevant target should be visible; remove secrets and personal data.
Feature documented: automatic failure screenshot evidence.
Suggested alt text: Application failure state captured automatically by Test Lens.
-->

## ScreenshotCapture

Advanced direct service:

<!-- API SIGNATURES: io.github.testlens.selenium.evidence.ScreenshotCapture -->
```java
ScreenshotCapture(WebDriver driver)
ScreenshotCaptureResult capture(String name, ScreenshotCaptureOptions options)
ScreenshotCaptureResult capture(String name, ScreenshotCaptureOptions options, UiTestLensSession session)
```

The constructor rejects a null driver. A null options argument uses `ScreenshotCaptureOptions.defaults()`. A null or blank name becomes `"Screenshot"`. The two-argument overload captures without a session. The three-argument overload can attach the resulting `TraceArtifact` when `options.attachToSession()` is true and `session` is non-null. A driver without `TakesScreenshot`, or an I/O/runtime failure during capture, produces a failed result rather than throwing from these methods.

Normal consumers use `TestLens.captureScreenshot(...)`.

## ScreenshotCaptureResult

<!-- API SIGNATURES: io.github.testlens.selenium.evidence.ScreenshotCaptureResult -->
```java
ScreenshotCaptureStatus status()
String name()
Path path()
TraceArtifact artifact()
String message()
Throwable exception()
Instant capturedAt()
boolean isCaptured()
```

`status()` is `CAPTURED`, `FAILED`, or `SKIPPED`, and `isCaptured()` is true only for `CAPTURED`. `name()` and `message()` are never null; null constructor inputs are normalized to empty strings by the result factories. `capturedAt()` is never null. `path()` is null for skipped results and can be null when failure happens before a destination is determined. `artifact()` is non-null only when capture succeeded and attachment to a non-null session was requested; a successfully written screenshot can therefore have a null artifact. `exception()` is normally non-null only for failures caused by an exception, but can be null for a capability failure such as a driver that does not implement `TakesScreenshot`.

## VideoEvidence

Video evidence links a video produced by Selenium Grid, a cloud provider, CI, or another recorder to the Test Lens trace. It is opt-in: Test Lens does **not** record video.

The ergonomic advanced facade uses its attached session:

```java
VideoEvidenceResult video = overlay.attachVideoFile(
        "Browser recording",
        Path.of("target/recordings/checkout.mp4")
);
```

Use [`VideoEvidenceOptions`](../reference/configuration.md#videoevidenceoptions) to describe the source/media type, validate a local path, control session attachment, or add metadata. URL attachment creates a reference; it does not download or validate the remote resource.

The direct advanced service exposes these methods:

<!-- API SIGNATURES: io.github.testlens.selenium.evidence.VideoEvidence -->
```java
VideoEvidence()
VideoEvidenceResult attachFile(String name, Path path, VideoEvidenceOptions options, UiTestLensSession session)
VideoEvidenceResult attachUrl(String name, String url, VideoEvidenceOptions options, UiTestLensSession session)
```

`attachFile(...)` creates a reference to an existing local file. It checks existence only when `validateLocalFileExists()` is true. `attachUrl(...)` requires a non-blank URL but does not fetch or validate the remote resource. A null options argument uses defaults. A blank evidence name throws `IllegalArgumentException`. When `attachToSession()` is true and the session is null, the result is `SKIPPED`; when attachment is disabled, the result is `ATTACHED` with a null artifact because only the reference was prepared.

`VideoEvidenceStatus` is `ATTACHED`, `FAILED`, or `SKIPPED`. `VideoEvidenceException` remains available for evidence integrations, but the two direct attachment methods return validation/attachment failures through `VideoEvidenceResult` for their normal failure paths.

`JsOverlayDebug.attachVideoFile(...)` and `attachVideoUrl(...)` are the more ergonomic advanced facade overloads: they use defaults when options are omitted and use the facade's currently attached session.

## Security

Screenshots and video can expose credentials, personal data, tokens, and internal URLs. Evidence metadata may also contain sensitive strings. Keep `target/ui-test-lens*`, CI report archives, and auth/network output out of source control and apply retention/access controls.
