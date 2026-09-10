# Screenshots and video evidence

**Selenium Test Lens 0.1.0 — stable**

Version 0.1.0 can capture the current browser viewport and attach the resulting PNG to the active trace session.

```java
ScreenshotCaptureResult screenshot = lens.captureScreenshot("after-save");
```

Use `ScreenshotCaptureOptions` to select the output directory, filename policy, overwrite behavior, and whether an active HUD should be hidden during capture. The 0.1.0 implementation delegates to Selenium `TakesScreenshot`; it does not scroll and stitch the whole document.

Step helpers can request a screenshot when a step fails:

```java
UiStepOptions stepOptions = UiStepOptions.builder()
    .captureScreenshotOnFailure(true)
    .build();

lens.step("Save profile", stepOptions, () ->
    lens.getByRole("button", "Save").click());
```

Video methods attach an existing local file or a CI-accessible URL to the trace. They do not start or control a recorder.

```java
overlay.attachVideoFile("checkout recording", Path.of("target/videos/checkout.mp4"));
overlay.attachVideoUrl("CI recording", "https://ci.example.test/artifacts/checkout.mp4");
```

Screenshots and video can contain visible credentials or personal data. Version 0.1.0 does not redact pixels, so control the page state and access to exported evidence.

See the [trace and reports](trace-and-reports.md) page for artifact export and the [HUD and overlay](hud-and-overlay.md) page for visual decoration.
