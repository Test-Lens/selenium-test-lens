# HUD and overlay

**Selenium Test Lens 0.1.0 — stable**

The overlay module injects visual diagnostics into the current page: a HUD panel, step/wait messages, highlights, and arrows. It is attached to the existing browser document and is re-created when later operations run after navigation.

```java
OverlayConfig overlay = OverlayConfig.builder()
    .showHudPanel(true)
    .decorationDurationMs(750)
    .highlightColor("#7c3aed")
    .build();

TestLens lens = TestLens.attach(driver, overlay);
```

HUD operations are diagnostic. Application actions remain Selenium operations, and a HUD rendering failure must not be treated as proof that the application action failed. `finishPassed()` and `finishFailed(...)` can remove HUD artifacts when `cleanupHudOnFinish` is enabled.

The overlay policy can describe explicit popup handling, including a configured close-button selector. It is not a general ad blocker and does not make arbitrary overlays safe to dismiss. CSP, closed shadow roots, cross-origin frames, and application styling can restrict injected visual features.

Screenshots in 0.1.0 capture the current viewport. They are pixel evidence; no central content-redaction policy existed in this release.

See [screenshots and video evidence](screenshots-and-video.md) for capture and attachment behavior.
