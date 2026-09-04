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
NetworkCaptureMode captureMode()
Optional<NetworkCaptureMode> activeCaptureMode()
NetworkEvent addManualEvent(NetworkEvent event)
String exportJson()
```

`MANUAL` remains the default and accepts caller-supplied events. `BIDI` passively subscribes through Selenium 4.39's beta `org.openqa.selenium.bidi.module.Network`; the browser session must have BiDi enabled when it is created. `AUTO` attempts the same subscription and reports `UNSUPPORTED` when it cannot establish it. Neither mode falls back to `MANUAL` or performance logs. `PERFORMANCE_LOGS` remains unsupported and `OFF` remains stopped.

`captureMode()` is the requested mode. `activeCaptureMode()` is present only while a source is active: `MANUAL` for manual capture and `BIDI` for both successful `BIDI` and `AUTO`. A successful start registers one listener each for before-request, response-completed, and fetch-error; `stop()` removes the module subscriptions without closing the driver. Repeated starts replace the prior generation, and late callbacks are discarded. Event snapshots are immutable and safe while BiDi callback threads are active.

## HUD-only filtering

<!-- API SIGNATURES: io.github.testlens.selenium.network.NetworkHudFilter -->
```java
static NetworkHudFilter defaults()
static NetworkHudFilter all()
static NetworkHudFilter none()
static NetworkHudFilter.Builder builder()
boolean showRequests()
boolean showResponses()
boolean showFailures()
boolean showFailedResponses()
List<String> includeUrlPatterns()
List<String> excludeUrlPatterns()
```

<!-- API SIGNATURES: io.github.testlens.selenium.network.NetworkHudFilter$Builder -->
```java
NetworkHudFilter.Builder showRequests(boolean value)
NetworkHudFilter.Builder showResponses(boolean value)
NetworkHudFilter.Builder showFailures(boolean value)
NetworkHudFilter.Builder showFailedResponses(boolean value)
NetworkHudFilter.Builder includeUrlPattern(String regex)
NetworkHudFilter.Builder excludeUrlPattern(String regex)
NetworkHudFilter build()
```

`NetworkDiagnosticsOptions.hudFilter(...)` controls only raw `REQUEST`, `RESPONSE`, and `FAILED` lines rendered in the HUD. The default hides request lines, shows responses and failures, and shows HTTP responses at or above `failedStatusThreshold`. `all()` shows all three raw types; `none()` hides all three. Control entries such as capture lifecycle, waits, assertions, attach/export, mode information, and diagnostic warnings remain visible for every preset.

```java
NetworkDiagnosticsOptions options = NetworkDiagnosticsOptions.builder()
        .captureMode(NetworkCaptureMode.BIDI)
        .hudFilter(NetworkHudFilter.defaults())
        .build();

NetworkDiagnosticsOptions apiOnly = NetworkDiagnosticsOptions.builder()
        .hudFilter(NetworkHudFilter.builder()
                .includeUrlPattern("/api/.*")
                .excludeUrlPattern("/api/health(?:\\?.*)?$")
                .build())
        .build();
```

Includes restrict ordinary requests and successful responses. Enabled fetch failures and failed HTTP responses bypass includes so errors remain prominent. An exclude always wins, including for failures. Matching uses the original full event URL; HUD messages and log metadata use only a sanitized path without userinfo, query, or fragment. Invalid regular expressions fail while the filter is built. Passing a null filter to the options builder restores `defaults()`.

This is distinct from `ignoreUrlPattern(...)`: ignore rules remove matching traffic from capture, events, counts, waits, trace, JSON, and failure evidence and increment `ignoredEvents`. A HUD filter never changes those data. Every hidden raw entry still reaches the session trace and external `UiTestLensLogSink`s with `hudVisible=false`.

## Enabling WebDriver BiDi

```java
ChromeOptions browserOptions = new ChromeOptions().enableBiDi();
WebDriver driver = new ChromeDriver(browserOptions);

TestLens lens = TestLens.attach(driver);
lens.startSession("network-test");
NetworkDiagnostics network = lens.network().start(
        NetworkDiagnosticsOptions.builder()
                .captureMode(NetworkCaptureMode.BIDI)
                .build());

driver.get(baseUrl);
network.waitForResponse("/api/orders", 200);
network.assertNoFailedRequests();
lens.finishPassed();
driver.quit();
```

The Firefox session setup is equivalent:

```java
FirefoxOptions browserOptions = new FirefoxOptions().enableBiDi();
WebDriver driver = new FirefoxDriver(browserOptions);
```

Lens does not retrofit BiDi after session creation. A local or wrapped driver is unwrapped through the official `WrapsDriver` contract with cycle/depth protection. A `RemoteWebDriver` can work only when its Grid/node exposes the WebSocket capability; Grid execution is not part of the current tested matrix.

## Assertions and waits

<!-- API SIGNATURES: io.github.testlens.selenium.network.NetworkDiagnostics -->
```java
NetworkDiagnosticsResult assertNoFailedRequests()
NetworkWaitResult waitForResponse(String urlContains, int status)
NetworkWaitResult waitForResponse(NetworkWaitCondition condition)
NetworkResponseExpectation expectResponse()
Optional<NetworkEvent> findMatchingEvent(NetworkWaitCondition condition)
```

The convenience overload builds a URL-substring/status condition. BiDi callbacks signal active waits immediately; the condition still uses a global deadline and tolerates spurious wakeups. Unsupported capture returns `SKIPPED/UNSUPPORTED_CAPTURE_MODE` with zero attempts, failed BiDi startup returns `FAILED/CAPTURE_START_FAILED` with zero attempts, and `OFF`, a stopped capture, or `stop()` during a wait returns `CAPTURE_NOT_STARTED`. `expectResponse()` provides a fluent API. Failed assertions throw `NetworkAssertionError` with summary/wait context.

## Session attachment

<!-- API SIGNATURES: io.github.testlens.selenium.network.NetworkDiagnostics -->
```java
NetworkDiagnosticsResult attachToSession(UiTestLensSession session)
NetworkDiagnosticsResult attachToSession(UiTestLensSession session, Path outputPath)
```

Exports/attaches network JSON evidence. Attachment occurs only when one of these methods is called explicitly. The former no-op `NetworkDiagnosticsOptions.attachToSession(boolean)` option was removed in the 0.2.0 development line. Paths and export failures appear in the result. Finalizing a failed Test Lens session still snapshots the current network summary into its failure bundle without invoking these attachment methods.

## Supporting results and statuses

`NetworkEvent` classifies captured activity with `NetworkEventType` (`REQUEST`, `RESPONSE`, `FAILED`, `INFO`, or `WARNING`). BiDi request IDs are preserved. Redirect correlation uses request ID plus redirect count, and response/fetch-error events are retained even if capture began after their request event. HTTP 4xx/5xx remain responses; `failedStatusThreshold` controls failed-response counts. The JSON object records requested/active modes, status, ignored/dropped counts, events, timestamps, and correlation attributes.

`NetworkDiagnosticsStatus` reports started, stopped, attached, assertion-passed, assertion-failed, unsupported, or failed service outcomes. `NetworkWaitStatus` reports matched, timed-out, failed, or skipped waits, while `NetworkWaitFailureReason` distinguishes no matching response/request, a matching failed response, capture not started, unsupported capture mode, and unknown failures. `NetworkDiagnosticsException` is the service exception type; result-returning operations should still be checked before assuming that output was attached.

## Limits, headers, and security

`maxCapturedEvents` defaults to 10,000 and must be positive. Once full, new request/response/failure events are dropped, `droppedEvents` increases, and one warning is emitted per capture generation. Ignored URL patterns are applied before header mapping and increment `ignoredEvents` without waking a matching wait.

Headers are disabled by default. When enabled, string and base64 BiDi values are converted deterministically and repeated names are preserved in order. `Authorization`, `Proxy-Authorization`, `Cookie`, `Set-Cookie`, `X-Api-Key`, and `X-Auth-Token` are masked case-insensitively by default. The central `RedactionPolicy` is the final artifact boundary: `maskSensitiveHeaders(false)` alone cannot expose recognized credentials while central redaction remains enabled. Raw headers require both protections to be deliberately disabled. Request/response bodies and BiDi's separate cookie collection are never collected. Matching uses captured URLs internally, while public events, waits, trace, JSON, reports, and bundle data receive the effective policy; query parameters can still contain unknown sensitive data.

This is passive diagnostics, not interception, blocking, mocking, body capture, CDP, performance logs, or a general BiDi wrapper. The implementation deliberately isolates Selenium's beta 4.39 BiDi API behind an internal adapter.

The typed `RequestData` shipped in Selenium Java 4.39.0 does not expose destination or a fetch/XHR/beacon initiator classification. Lens does not inspect raw protocol events and does not guess from URL suffixes, MIME types, or headers. Consequently BiDi `NetworkRequest.resourceType()` can be empty and raw log metadata reports `resourceTypeAvailable=false`. Manual events retain a resource type explicitly supplied by their producer. Configure successful API traffic in the HUD with explicit URL patterns.

See [network options](../reference/configuration.md#network-options) and the [complete signatures](../reference/public-api-catalog.md).
