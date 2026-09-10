# Configuration

**Selenium Test Lens 0.1.0 — stable**

Configure the main facade with immutable option objects:

```java
UiLocatorOptions locatorOptions = UiLocatorOptions.builder()
    .timeout(Duration.ofSeconds(5))
    .pollInterval(Duration.ofMillis(100))
    .maxRetries(2)
    .retryOnStaleElement(true)
    .retryOnClickIntercepted(true)
    .build();

TestLensOptions options = TestLensOptions.builder()
    .overlayConfig(OverlayConfig.builder().showHudPanel(true).build())
    .locatorOptions(locatorOptions)
    .outputRoot(Path.of("target", "ui-test-lens"))
    .screenshotOnFailure(true)
    .cleanupHudOnFinish(true)
    .build();

TestLens lens = TestLens.attach(driver, options);
```

The configured locator options apply to `TestLens.locator(By...)`. In 0.1.0, semantic factories on `TestLens` delegate to `JsOverlayDebug` defaults instead of inheriting the facade's configured `locatorOptions`; use `locator(By, ...)` with explicit selectors when those settings must apply.

Assertion options are supplied through `locator.expect(UiAssertionOptions)`. Page waits on `JsOverlayDebug` use their own default timeout unless a timeout overload is selected.
