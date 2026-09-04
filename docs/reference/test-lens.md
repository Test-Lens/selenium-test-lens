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
RetrySummary retrySummary()
NetworkDiagnostics network()
TestLensFinalizationResult finishPassed()
TestLensFinalizationResult finishFailed(Throwable originalFailure)
TestLensFinalizationResult finishSkipped(String reason)
```

All `attach` overloads require a usable existing driver; the first uses all defaults, the second changes overlay configuration, and the third accepts complete facade options. Lens never creates or closes the driver. `startSession` activates a new trace and attempts HUD initialization. `session` is empty before start.

Finalization completes the active session, writes JSON and HTML, and applies configured HUD cleanup. `finishPassed()` normally records `PASSED`, but a configured retry fail policy can finalize it as `FAILED` and throw `RetryPolicyViolationException` only after evidence, reports, cleanup, manifest, and ZIP. `finishFailed(...)` always records `FAILED`, including when its argument is null; `finishSkipped(reason)` records `SKIPPED`. A policy never replaces explicit failed/skipped status. Any final `FAILED` status can request the automatic failure bundle. Diagnostics remain secondary. Calling a finish method without a session returns a diagnostic result. Finalization never closes the driver. `failureBundleDirectory()`, `failureBundleManifest()`, and `failureBundleArchive()` expose successfully created paths without changing the finalization-result record constructor.

`TestLensOptions.redactionPolicy(...)` supplies one enabled-by-default policy to the facade, logger/HUD fan-out, session, network and API overlays, exports, and failure-bundle text. The exception returned to the runner remains the original; stored diagnostic throwable text is a redacted copy. See [Sensitive-data redaction](../security/redaction.md).

```java
TestLens lens = TestLens.attach(driver);
lens.startSession("checkout");
Throwable testFailure = null;
try {
    lens.getByTestId("save").click();
} catch (Throwable failure) {
    testFailure = failure;
    throw failure;
} finally {
    if (testFailure == null) lens.finishPassed();
    else lens.finishFailed(testFailure);
}
```

## Operation groups

- Locator/expect overloads: [Locators](../elements/locators.md), [Assertions](../elements/assertions.md)
- Screenshot overloads: [Screenshots and evidence](../observability/screenshots-evidence.md)
- `step` overloads: [Steps](../advanced/steps-business-assertions.md)
- Frame/window/alert methods: [Browser context](../browser-context/index.md)
- Passive/manual network diagnostics: [Network diagnostics](../advanced/network.md)
- Specialized visual helpers: [Advanced visual helpers](../advanced/visual-helpers.md)
