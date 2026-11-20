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
AuthStateManager authState()
ScenarioStateManager scenarioState()
ScenarioResourceManager resources()
SuiteStateManager suiteState()
TestLensFinalizationResult finishPassed()
TestLensFinalizationResult finishFailed(Throwable originalFailure)
TestLensFinalizationResult finishSkipped(String reason)
```

All `attach` overloads require a usable existing driver; the first uses all defaults, the second changes overlay configuration, and the third accepts complete facade options. Lens never creates or closes the driver. `startSession` activates a new trace and attempts HUD initialization. `session` is empty before start.

Finalization completes the active session, closes its managed state/resources, writes JSON and HTML, and applies configured HUD cleanup. The first terminal call wins: `finishPassed()`, `finishFailed(...)`, or `finishSkipped(...)` fixes the session status, completion time, failure/reason, and retry decision. Later or concurrent calls wait for that same facade pipeline and return the identical `TestLensFinalizationResult` without repeating resource cleanup, screenshots, network shutdown, exports, HUD cleanup, manifest, or ZIP creation. A session finished directly through `UiTestLensSession` likewise cannot be overwritten; the facade may complete its still-missing export pipeline once while respecting that terminal state.

`finishPassed()` normally records `PASSED`, but a configured retry fail policy can finalize it as `FAILED` and throw `RetryPolicyViolationException` only after evidence, reports, cleanup, manifest, and ZIP. That exact exception is retained and rethrown by later facade finalizers without rerunning the pipeline. `finishFailed(...)` always records `FAILED`, including when its argument is null; `finishSkipped(reason)` records `SKIPPED`. A policy never replaces explicit failed/skipped status. Any final `FAILED` status can request the automatic failure bundle. Diagnostics remain secondary. Calling a finish method without a session returns a diagnostic result. Starting a later session on the same facade creates independent finalization ownership. Finalization never closes the driver. `failureBundleDirectory()`, `failureBundleManifest()`, and `failureBundleArchive()` expose successfully created paths without changing the finalization-result record constructor.

In 0.5.2, an `Error` from scenario-resource cleanup is retained and rethrown unchanged after the remaining resource cleanups are attempted. If the test already supplied a failure, that original failure remains the session failure and the cleanup error is attached to it as suppressed; secondary cleanup failures are suppressed on the selected cleanup error. A critical evidence failure also attempts resource cleanup and network shutdown before it is rethrown. Repeated finalizers rethrow the retained terminal error without running the finalization pipeline again.

A resource cleanup that throws `InterruptedException` restores the thread's interrupt flag. Cleanup failure can make a still-active session fail, but an already terminal session remains immutable: completing its missing facade cleanup/export work cannot rewrite its earlier result.

Locator metadata diagnostics also propagate `VirtualMachineError`, `ThreadDeath` and `LinkageError` from custom locator formatting or remote parameters. Ordinary optional diagnostic failures can still use an opaque description; fatal errors are not treated as successful metadata capture.

`TestLensOptions.redactionPolicy(...)` supplies one enabled-by-default policy to the facade, logger/HUD fan-out, session, network and API overlays, exports, and failure-bundle text. The exception returned to the runner remains the original; stored diagnostic throwable text is a redacted copy. See [Sensitive-data redaction](../security/redaction.md).

`scenarioState()` and `resources()` belong to the current session's unique invocation ID and close during facade finalization. `suiteState()` is available when a runner adapter or explicit `TestRunScope` supplied a logical run boundary. See [Managed Test State & Resources](../features/managed-test-state.md).

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
- Invocation/suite state and temporary resources: [Managed Test State & Resources](../features/managed-test-state.md)
