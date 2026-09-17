# Screenshots and evidence

Screenshots capture browser pixels as PNG evidence. `VIEWPORT` is the compatible default. The opt-in `FULL_PAGE` mode captures a bounded snapshot of the current top-level document by scrolling and stitching standard Selenium screenshots. Configured [visual redaction](../security/visual-redaction.md) is applied at the shared capture seam in both modes. Use an explicit screenshot at a meaningful checkpoint, or let failed-session finalization attempt one automatically.

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

Failed finalization first attempts `failure-diagnostic.png` with the current HUD/highlight, then temporarily hides only Test Lens artifacts for the separately requested `failure-bundle/failure-clean.png`. In full-page mode the diagnostic image contains one frozen snapshot of the complete overlay; the clean image starts from the intentionally hidden overlay state. Sensitive masks are installed independently on both images: “clean” never means unredacted. Both are controlled by `screenshotOnFailure`; independent flags live in `FailureBundleOptions`. Previous HUD visibility is restored in `finally` before normal cleanup. No failed action, click, locator resolve, frame switch, or navigation is repeated. Passed and skipped finalization never requests either screenshot. See [Failure bundles](failure-bundles.md).

## Explicit screenshots

Call `captureScreenshot(...)` when a checkpoint is useful even if the test ultimately passes:

```java
ScreenshotCaptureResult screenshot = lens.captureScreenshot("Order summary");
if (!screenshot.isCaptured()) {
    System.err.println(screenshot.message());
}
```

Explicit capture is not automatic. It uses [`ScreenshotCaptureOptions.defaults()`](../reference/configuration.md#screenshotcaptureoptions) unless options are supplied.

### Portable full-page capture

```java
ScreenshotCaptureOptions fullPage = ScreenshotCaptureOptions.builder()
        .captureMode(ScreenshotCaptureMode.FULL_PAGE)
        .build();

ScreenshotCaptureResult result = lens.captureScreenshot("checkout-page", fullPage);
```

At the beginning of the overall capture, the implementation snapshots the complete Test Lens overlay host and open shadow tree. The immutable clone is positioned in document coordinates at the initial viewport rectangle; the live host is hidden as one unit, so HUD updates and decoration cleanup cannot mix later overlay states into subsequent tiles. Before measuring each attempt, capture installs a temporary page guard and waits for two animation frames. The guard pauses application CSS animations, disables transitions and smooth/snap scrolling, and hides the blinking caret without stopping application JavaScript or replacing timers. Capture then takes a CSS-pixel snapshot of document dimensions, viewport dimensions, and scroll position. It keeps the current window size and responsive layout, scrolls horizontally and vertically, derives independent image scales from the returned PNG dimensions, removes clamped overlap, and publishes the final PNG only after stitching succeeds. It uses `TakesScreenshot`, `JavascriptExecutor`, `BufferedImage`, and `ImageIO`; it does not use CDP, change the browser window size, or ask JavaScript for screenshot pixels.

Full-page capture is bounded by `maxPixelCount` (40 million by default) and `maxTileCount` (200 by default). Dimensions are checked before every tile and once more after stitching. A document/viewport dimension change retries the entire guarded capture once; the first partial image is discarded. A second dimension change reports the reason and the two-attempt count. Limits, changing tile scale, invalid PNG, or restoration failure are not retried and produce an explicit non-captured result with no partial final file.

The original scroll position is restored and the guard stylesheet is removed in `finally`, including capture failures and retry boundaries. One overlay snapshot is retained across a dimension retry, then removed in the outer `finally`; the live host's exact prior inline visibility and priority are restored without reinjection. Highlight cleanup timers and HUD rendering may continue normally on the hidden live tree, but cannot change the frozen evidence clone; after capture the live overlay resumes at its naturally elapsed lifecycle state. Visual-mask locators and document rectangles are refreshed after every scroll and before every tile. Other visible `fixed` and `sticky` page elements are captured in the first tile in which they appear and then hidden with `visibility` for later tiles; their original inline value and priority are restored. Scanning covers the open document tree, not closed shadow roots.

`FULL_PAGE` is supported only in the current top-level browsing context. It never changes windows or frames. When invoked inside a frame it returns `SKIPPED` and preserves that context; switch to default content explicitly if a top-level image is wanted. Rendered iframe and shadow-DOM pixels visible in the top-level page are captured, but iframe documents and nested scroll containers are not expanded. Scroll events can run application code during capture.

### TestLens.captureScreenshot overloads

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
ScreenshotCaptureResult captureScreenshot(String name)
ScreenshotCaptureResult captureScreenshot(String name, ScreenshotCaptureOptions options)
```

The first uses defaults. Capture requires a driver implementing Selenium `TakesScreenshot`. It creates a PNG path and, by default, attaches a trace artifact when a session is active. Configure output directory, prefix, timestamps, overwrite behavior, and session attachment through [`ScreenshotCaptureOptions`](../reference/configuration.md#screenshotcaptureoptions).

The current `TestLens.captureScreenshot(...)` path represents unsupported-driver, I/O, and runtime capture failures in `ScreenshotCaptureResult`; it does not throw `ScreenshotCaptureException` for those failures. `ScreenshotCaptureException` remains a public exception type but is not thrown by the current `ScreenshotCapture.capture(...)` implementation.

## ScreenshotCapture

Advanced direct service:

<!-- API SIGNATURES: io.github.testlens.selenium.evidence.ScreenshotCapture -->
```java
ScreenshotCapture(WebDriver driver)
ScreenshotCapture(WebDriver driver, VisualRedactionOptions visualRedaction)
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
ScreenshotCaptureMode requestedMode()
ScreenshotCaptureMode capturedMode()
int width()
int height()
int tileCount()
```

`status()` is `CAPTURED`, `FAILED`, or `SKIPPED`, and `isCaptured()` is true only for `CAPTURED`. The result distinguishes requested and completed modes and reports final PNG dimensions and tile count; a viewport capture uses one tile. `capturedMode()` is null and dimensions are zero when no image was completed. `name()`, `message()`, and `capturedAt()` are never null. `path()` is null for skipped results and can be null when failure happens before a destination is determined. `artifact()` is non-null only when capture succeeded and attachment to a non-null session was requested.

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

Screenshots and video can expose credentials, personal data, tokens, and internal URLs. Central text redaction does not modify pixels; configure [visual redaction](../security/visual-redaction.md) for screenshot pixels. Video remains outside this protection. Keep `target/ui-test-lens*`, CI report archives, and auth/network output out of source control and apply retention/access controls.
