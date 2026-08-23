# TestLens

Package: `io.github.testlens`<br>
Module: `selenium-test-lens-selenium` (`artifactId: selenium-test-lens`)<br>
API level: **Recommended**

`TestLens` is the runner-neutral facade. Obtain it by attaching to a driver your test owns. One instance should follow one driver/test lifecycle.

## Creation and lifecycle

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
static TestLens attach(WebDriver driver)
static TestLens attach(WebDriver driver, OverlayConfig config)
static TestLens attach(WebDriver driver, TestLensOptions options)
WebDriver driver()
UiTestLensSession startSession(String name)
Optional<UiTestLensSession> session()
TestLensFinalizationResult finishPassed()
TestLensFinalizationResult finishFailed(Throwable originalFailure)
```

All `attach` overloads require a usable existing driver; the first uses all defaults, the second changes overlay configuration, and the third accepts complete facade options. Lens never creates or closes the driver. `startSession` activates a new trace and attempts HUD initialization. `session` is empty before start.

Finalization completes the active session, attempts failure screenshot (failed finish only), JSON, HTML, and HUD cleanup. Diagnostics are best effort and collected in the result. Calling finish without a session returns a result containing an `IllegalStateException` diagnostic rather than throwing it.

```java
TestLens lens = TestLens.attach(driver);
lens.startSession("checkout");
try {
    lens.getByTestId("save").click();
    lens.finishPassed();
} catch (Throwable failure) {
    lens.finishFailed(failure);
    throw failure;
}
```

## Operation groups

- Locator/expect overloads: [Locators](../elements/locators.md), [Assertions](../elements/assertions.md)
- Screenshot overloads: [Screenshots and evidence](../observability/screenshots-evidence.md)
- `step` overloads: [Steps](../advanced/steps-business-assertions.md)
- Frame/window/alert methods: [Browser context](../browser-context/index.md)
- Specialized visual helpers: [Advanced visual helpers](../advanced/visual-helpers.md)
