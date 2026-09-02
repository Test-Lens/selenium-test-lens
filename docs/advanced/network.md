# Network diagnostics

Package: `io.github.testlens.selenium.network`<br>
Module: `selenium-test-lens-selenium`<br>
API level: **Advanced**

## Lifecycle and reads

<!-- API SIGNATURES: io.github.testlens.selenium.network.NetworkDiagnostics -->
```java
NetworkDiagnostics(WebDriver driver)
NetworkDiagnostics(WebDriver driver, OverlayLogger logger)
NetworkDiagnostics start(NetworkDiagnosticsOptions options)
NetworkDiagnostics stop()
boolean isStarted()
List<NetworkEvent> events()
NetworkSummary summary()
NetworkEvent addManualEvent(NetworkEvent event)
String exportJson()
```

`MANUAL` is the only implemented active capture mode and is the default. Call `addManualEvent(...)` from your own HTTP instrumentation, test fixture, proxy, or application hook. `OFF` leaves capture stopped. The retained `AUTO`, `BIDI`, and `PERFORMANCE_LOGS` enum values are not implemented and produce `NetworkDiagnosticsStatus.UNSUPPORTED`; no mode silently falls back to `MANUAL`. Snapshots are immutable copies where exposed.

## Assertions and waits

<!-- API SIGNATURES: io.github.testlens.selenium.network.NetworkDiagnostics -->
```java
NetworkDiagnosticsResult assertNoFailedRequests()
NetworkWaitResult waitForResponse(String urlContains, int status)
NetworkWaitResult waitForResponse(NetworkWaitCondition condition)
NetworkResponseExpectation expectResponse()
Optional<NetworkEvent> findMatchingEvent(NetworkWaitCondition condition)
```

The convenience overload builds a URL-substring/status condition. In `MANUAL` mode, the condition overload polls events supplied through `addManualEvent(...)`. In an unsupported mode it returns immediately as `SKIPPED` with `UNSUPPORTED_CAPTURE_MODE` and zero polling attempts; with `OFF` or before `start(...)`, it returns `CAPTURE_NOT_STARTED`. `expectResponse()` provides a fluent `urlContains`, `urlRegex`, `exactUrl`, `method`, `status`, `statusBetween`, `within`, `waitNow` API. Failed assertions throw `NetworkAssertionError` with summary/wait context.

## Session attachment

<!-- API SIGNATURES: io.github.testlens.selenium.network.NetworkDiagnostics -->
```java
NetworkDiagnosticsResult attachToSession(UiTestLensSession session)
NetworkDiagnosticsResult attachToSession(UiTestLensSession session, Path outputPath)
```

Exports/attaches network JSON evidence. Attachment occurs only when one of these methods is called explicitly. The deprecated `NetworkDiagnosticsOptions.attachToSession(boolean)` option has no automatic effect and is scheduled for removal in 0.2.0. Paths and export failures appear in the result.

## Supporting results and statuses

`NetworkEvent` classifies captured activity with `NetworkEventType` (`REQUEST`, `RESPONSE`, `FAILED`, `INFO`, or `WARNING`) and can carry a `NetworkRequest`, `NetworkResponse`, or `NetworkFailure`. `NetworkLogExporter` serializes/exports the collected diagnostic model as a `String`; callers decide whether and where to persist it. Exported URLs, headers, and bodies can be sensitive.

`NetworkDiagnosticsStatus` reports started, stopped, attached, assertion-passed, assertion-failed, unsupported, or failed service outcomes. `NetworkWaitStatus` reports matched, timed-out, failed, or skipped waits, while `NetworkWaitFailureReason` distinguishes no matching response/request, a matching failed response, capture not started, unsupported capture mode, and unknown failures. `NetworkDiagnosticsException` is the service exception type; result-returning operations should still be checked before assuming that output was attached.

## Limitations and security

No automatic browser capture, Selenium BiDi integration, performance-log collector, request blocking, modification, mocking, body rewrite, or general CDP/BiDi wrapper is implemented. Headers/URLs can contain credentials, query tokens, internal hostnames, and personal data. Sensitive-header masking defaults on, but custom headers, URLs, bodies, manual events, and screenshots still require caller review.

See [network options](../reference/configuration.md#network-options) and the [complete signatures](../reference/public-api-catalog.md).
