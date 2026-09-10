# Session lifecycle

**Selenium Test Lens 0.1.0 — stable**

The consuming test owns the `WebDriver`. Test Lens attaches diagnostics to it and owns only the session it starts.

```java
TestLens lens = TestLens.attach(driver);
lens.startSession("checkout");
try {
    lens.getByTestId("place-order").click();
    lens.finishPassed();
} catch (RuntimeException | Error failure) {
    lens.finishFailed(failure);
    throw failure;
} finally {
    driver.quit();
}
```

`finishPassed()` or `finishFailed(Throwable)` updates the session, writes `trace.json` and `report.html` below the configured output root, optionally captures a viewport failure screenshot, and cleans the HUD. Diagnostic failures are returned in `TestLensFinalizationResult` rather than replacing the original test failure.

Important 0.1.0 boundaries:

- There is no `finishSkipped(...)` method on the `TestLens` facade.
- Finalization does not call `driver.quit()`.
- Do not call the facade finalizer more than once. Version 0.1.0 did not yet guarantee first-writer-wins or exactly-once side effects.
- Framework adapters for JUnit 5 and TestNG were not published in 0.1.0; integrate this sequence in your own fixture or extension.

See the [`TestLens` Javadoc](https://javadoc.io/doc/io.github.test-lens/selenium-test-lens/0.1.0/io/github/testlens/TestLens.html).
