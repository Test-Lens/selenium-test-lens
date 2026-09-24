# Network diagnostics

Manual network events, waits, and assertions are available in `0.1.0`.

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

BiDi capture observes traffic. It does not intercept, modify, block, mock, or replay requests, and it is not a CDP fallback.

`captureMode()` is the requested mode. `activeCaptureMode()` is present only while a source is active: `MANUAL` for manual capture and `BIDI` for both successful `BIDI` and `AUTO`. A successful start registers one listener each for before-request, response-completed, and fetch-error; `stop()` removes the module subscriptions without closing the driver. `stop()` also invalidates an initialization that is still registering its source: a late success or failure cannot reactivate capture or overwrite `STOPPED`, and any source returned afterward is closed without holding the lifecycle lock. Repeated starts replace the prior generation, and late callbacks are discarded. Event snapshots are immutable and safe while BiDi callback threads are active. Connection initialization is attempted at most twice, without a fixed sleep, and only before any listener is registered; subscription failures are never retried because registration may be ambiguous.

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

Lens cannot add BiDi to a browser session that was created without it. For an already BiDi-enabled local driver, Lens uses `HasBiDi.getBiDi()` so Selenium may initialize a lazy connection; an empty `maybeGetBiDi()` before first use is not treated as unsupported. Official `WrapsDriver` and Selenium `Decorated` chains are inspected with cycle/depth protection without replacing the consumer's driver reference.

Selenium 4.39's raw `RemoteWebDriver` does not directly implement `HasBiDi`. When the remote session returns a valid `webSocketUrl`, Lens applies Selenium's `Augmenter` once to a private BiDi view of that same session, initializes the connection, and gives that view only to Selenium's Network module. Normal application commands continue through the original driver/decorator. Lens closes subscriptions and any connection it created, but never calls `quit()` during network finalization. The capability is diagnostic evidence, while the Selenium interface and successful connection remain the authority for activation.

### BrowserStack prerequisite

BrowserStack's current Selenium documentation requires `seleniumBidi: true` under `bstack:options`; BrowserStack then requests/returns the standard `webSocketUrl` needed by Selenium. The provider also recommends selecting a Selenium 4 version. This example uses the repository's supported Selenium version and contains no credentials:

```java
MutableCapabilities capabilities = new MutableCapabilities();
capabilities.setCapability("browserName", "chrome");
capabilities.setCapability("bstack:options", Map.of(
        "os", "Windows",
        "osVersion", "11",
        "seleniumVersion", "4.39.0",
        "seleniumBidi", true));

WebDriver driver = new RemoteWebDriver(browserStackUrl, capabilities);
TestLens lens = TestLens.attach(driver);
NetworkDiagnostics network = lens.network().start(
        NetworkDiagnosticsOptions.builder()
                .captureMode(NetworkCaptureMode.BIDI)
                .build());
```

See BrowserStack's [official Selenium BiDi instructions](https://www.browserstack.com/docs/automate/selenium/bidi-event-driven-testing). Do not log a hub URL containing credentials. A successful `start(...)` has `isStarted() == true`, `summary().status() == STARTED`, and `activeCaptureMode() == Optional.of(BIDI)`. Merely requesting `BIDI` does not prove activation.

### Startup diagnostics and graceful degradation

BiDi startup diagnostics distinguish `UNSUPPORTED`, `INITIALIZATION_FAILED`, `REMOTE_ENDPOINT_UNAVAILABLE`, `SESSION_CLOSED`, `PROTOCOL_ERROR`, and `CAPTURE_START_FAILED` internally. HUD/log lifecycle entries use the semantic `SYSTEM` category: successful activation is informational/running capture state, while unsupported or failed optional capture is a warning rather than a failed test action. Metadata records only safe facts such as driver type, local/remote shape, `HasBiDi` availability, whether `webSocketUrl` was present, lazy/initialized state, augmentation, bounded attempt count, category, and stage. Endpoint values, credentials, headers, cookies, and tokens are not logged.

`BIDI` and `AUTO` startup remains non-throwing: inspect `isStarted()`, `summary()`, and `activeCaptureMode()` before relying on capture. An unavailable optional source does not fail the Selenium test, does not silently switch to manual capture, and cannot produce a valid network assertion snapshot. Existing network assertions still fail closed if the requested capture never became active.

### Optional remote smoke test

With provider credentials supplied through environment variables and never printed:

1. Create a remote session with the provider's Selenium BiDi option enabled.
2. Start `BIDI` capture and assert `network.isStarted()` plus active mode `BIDI`.
3. Navigate to a page that performs one known request.
4. Wait for that request and attach/export the network diagnostic.
5. Stop capture, verify the driver can still read the page title, then finalize Lens and let the test owner quit the driver.

To validate a local development build in a consumer without publishing it:

```text
mvn -DskipTests install
mvn dependency:tree -Dincludes=io.github.test-lens
```

Configure the consumer for `0.4.0-SNAPSHOT`, confirm every Test Lens artifact resolves to that version, and run one remote BiDi test using the checks above.

## Assertions and waits

<!-- API SIGNATURES: io.github.testlens.selenium.network.NetworkDiagnostics -->
```java
NetworkDiagnosticsResult assertNoFailedRequests()
NetworkWaitResult waitForResponse(String urlContains, int status)
NetworkWaitResult waitForResponse(NetworkWaitCondition condition)
NetworkResponseExpectation expectResponse()
Optional<NetworkEvent> findMatchingEvent(NetworkWaitCondition condition)
```

`assertNoFailedRequests()` means “the valid capture snapshot contains no failures,” not “the event buffer happens to contain no failures.” It throws `NetworkAssertionError` when capture was never started, is `OFF`, is unsupported, failed or is still initializing. Zero events are valid only after the current generation successfully became active. A normally stopped generation retains its valid snapshot and remains assertable; starting a later invalid generation invalidates that permission without clearing historical events.

The convenience wait overload builds a URL-substring/status condition. BiDi callbacks signal active waits immediately; the condition still uses a global deadline and tolerates spurious wakeups. Unsupported capture returns `SKIPPED/UNSUPPORTED_CAPTURE_MODE` with zero attempts, failed BiDi startup returns `FAILED/CAPTURE_START_FAILED` with zero attempts, and `OFF`, a stopped capture, or `stop()` during a wait returns `CAPTURE_NOT_STARTED`. These wait outcomes are unchanged. `expectResponse()` converts every non-match into `NetworkAssertionError`. Failed assertions preserve the lifecycle status in their summary and expose a redacted message and diagnostic cause/suppressed graph.

## Session attachment

<!-- API SIGNATURES: io.github.testlens.selenium.network.NetworkDiagnostics -->
```java
NetworkDiagnosticsResult attachToSession(UiTestLensSession session)
NetworkDiagnosticsResult attachToSession(UiTestLensSession session, Path outputPath)
```

Exports/attaches network JSON evidence. Attachment occurs only when one of these methods is called explicitly. The former no-op `NetworkDiagnosticsOptions.attachToSession(boolean)` option was removed in 0.2.0. Paths and export failures appear in the result. Finalizing a failed Test Lens session still snapshots the current network summary into its failure bundle without invoking these attachment methods.

## Supporting results and statuses

`NetworkEvent` classifies captured activity with `NetworkEventType` (`REQUEST`, `RESPONSE`, `FAILED`, `INFO`, or `WARNING`). BiDi request IDs are preserved. Redirect correlation uses request ID plus redirect count, and response/fetch-error events are retained even if capture began after their request event. HTTP 4xx/5xx remain responses; `failedStatusThreshold` controls failed-response counts. The JSON object records requested/active modes, status, ignored/dropped counts, events, timestamps, and correlation attributes.

`NetworkDiagnosticsStatus` reports started, stopped, attached, assertion-passed, assertion-failed, unsupported, or failed service outcomes. `NetworkWaitStatus` reports matched, timed-out, failed, or skipped waits, while `NetworkWaitFailureReason` distinguishes no matching response/request, a matching failed response, capture not started, unsupported capture mode, and unknown failures. `NetworkDiagnosticsException` is the service exception type; result-returning operations should still be checked before assuming that output was attached.

## Limits, headers, and security

`maxCapturedEvents` defaults to 10,000 and must be positive. Once full, new request/response/failure events are dropped, `droppedEvents` increases, and one warning is emitted per capture generation. Ignored URL patterns are applied before header mapping and increment `ignoredEvents` without waking a matching wait.

Headers are disabled by default. When enabled, string and base64 BiDi values are converted deterministically and repeated names are preserved in order. `Authorization`, `Proxy-Authorization`, `Cookie`, `Set-Cookie`, `X-Api-Key`, and `X-Auth-Token` are masked case-insensitively by default. The central `RedactionPolicy` is the final artifact boundary: `maskSensitiveHeaders(false)` alone cannot expose recognized credentials while central redaction remains enabled. Complete JSON diagnostic values use structural, escape-aware field redaction, while malformed fragments use a fail-closed fallback. Raw headers require both protections to be deliberately disabled. Request/response bodies and BiDi's separate cookie collection are never collected.

Matching, method/status evaluation, request-response correlation, and redirect handling use the private raw session buffer. Public `events()`, `summary()`/`firstFailure()`, wait results, assertion errors, logger entries, and exporters are separate immutable diagnostic snapshots. Their URLs remove userinfo and fragments, mask recognized query values, preserve safe query values, and fail closed for malformed input. This split prevents redaction from changing network behavior while ensuring a later capture event or restart cannot mutate an already returned summary. Unknown query parameters can still contain sensitive data; configure additional keys or literal secrets when needed.

This is passive diagnostics, not interception, blocking, mocking, body capture, CDP, performance logs, or a general BiDi wrapper. The implementation deliberately isolates Selenium's beta 4.39 BiDi API behind an internal adapter.

The typed `RequestData` shipped in Selenium Java 4.39.0 does not expose destination or a fetch/XHR/beacon initiator classification. Lens does not inspect raw protocol events and does not guess from URL suffixes, MIME types, or headers. Consequently BiDi `NetworkRequest.resourceType()` can be empty and raw log metadata reports `resourceTypeAvailable=false`. Manual events retain a resource type explicitly supplied by their producer. Configure successful API traffic in the HUD with explicit URL patterns.

See [network options](../reference/configuration.md#network-options) and the [complete signatures](../reference/public-api-catalog.md).
