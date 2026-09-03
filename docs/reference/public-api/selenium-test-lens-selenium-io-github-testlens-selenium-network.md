---
search:
  exclude: true
---

# selenium-test-lens-selenium: `io.github.testlens.selenium.network`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.selenium.network.NetworkAssertionError` {#io-github-testlens-selenium-network-networkassertionerror}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.network.NetworkAssertionError(java.lang.String, io.github.testlens.selenium.network.NetworkSummary)
public io.github.testlens.selenium.network.NetworkAssertionError(java.lang.String, io.github.testlens.selenium.network.NetworkSummary, io.github.testlens.selenium.network.NetworkWaitResult)
public io.github.testlens.selenium.network.NetworkSummary summary()
public io.github.testlens.selenium.network.NetworkWaitResult waitResult()
```

## `io.github.testlens.selenium.network.NetworkCaptureMode` {#io-github-testlens-selenium-network-networkcapturemode}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.network.NetworkCaptureMode OFF
public static final io.github.testlens.selenium.network.NetworkCaptureMode MANUAL
public static final io.github.testlens.selenium.network.NetworkCaptureMode PERFORMANCE_LOGS
public static final io.github.testlens.selenium.network.NetworkCaptureMode BIDI
public static final io.github.testlens.selenium.network.NetworkCaptureMode AUTO
public static io.github.testlens.selenium.network.NetworkCaptureMode[] values()
public static io.github.testlens.selenium.network.NetworkCaptureMode valueOf(java.lang.String)
```

## `io.github.testlens.selenium.network.NetworkDiagnostics` {#io-github-testlens-selenium-network-networkdiagnostics}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.network.NetworkDiagnostics(org.openqa.selenium.WebDriver)
public io.github.testlens.selenium.network.NetworkDiagnostics(org.openqa.selenium.WebDriver, io.github.testlens.core.OverlayLogger)
public io.github.testlens.selenium.network.NetworkDiagnostics start(io.github.testlens.selenium.network.NetworkDiagnosticsOptions)
public io.github.testlens.selenium.network.NetworkDiagnostics stop()
public boolean isStarted()
public java.util.List<io.github.testlens.selenium.network.NetworkEvent> events()
public io.github.testlens.selenium.network.NetworkSummary summary()
public io.github.testlens.selenium.network.NetworkCaptureMode captureMode()
public java.util.Optional<io.github.testlens.selenium.network.NetworkCaptureMode> activeCaptureMode()
public io.github.testlens.selenium.network.NetworkEvent addManualEvent(io.github.testlens.selenium.network.NetworkEvent)
public io.github.testlens.selenium.network.NetworkDiagnosticsResult assertNoFailedRequests()
public io.github.testlens.selenium.network.NetworkWaitResult waitForResponse(java.lang.String, int)
public io.github.testlens.selenium.network.NetworkWaitResult waitForResponse(io.github.testlens.selenium.network.NetworkWaitCondition)
public io.github.testlens.selenium.network.NetworkResponseExpectation expectResponse()
public java.util.Optional<io.github.testlens.selenium.network.NetworkEvent> findMatchingEvent(io.github.testlens.selenium.network.NetworkWaitCondition)
public io.github.testlens.selenium.network.NetworkDiagnosticsResult attachToSession(io.github.testlens.core.trace.UiTestLensSession)
public io.github.testlens.selenium.network.NetworkDiagnosticsResult attachToSession(io.github.testlens.core.trace.UiTestLensSession, java.nio.file.Path)
public java.lang.String exportJson()
```

## `io.github.testlens.selenium.network.NetworkDiagnosticsException` {#io-github-testlens-selenium-network-networkdiagnosticsexception}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.network.NetworkDiagnosticsException(java.lang.String)
public io.github.testlens.selenium.network.NetworkDiagnosticsException(java.lang.String, java.lang.Throwable)
```

## `io.github.testlens.selenium.network.NetworkDiagnosticsOptions$Builder` {#io-github-testlens-selenium-network-networkdiagnosticsoptions-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.network.NetworkDiagnosticsOptions$Builder captureMode(io.github.testlens.selenium.network.NetworkCaptureMode)
public io.github.testlens.selenium.network.NetworkDiagnosticsOptions$Builder includeHeaders(boolean)
public io.github.testlens.selenium.network.NetworkDiagnosticsOptions$Builder maskSensitiveHeaders(boolean)
public io.github.testlens.selenium.network.NetworkDiagnosticsOptions$Builder failedStatusThreshold(int)
public io.github.testlens.selenium.network.NetworkDiagnosticsOptions$Builder ignoreUrlPattern(java.lang.String)
public io.github.testlens.selenium.network.NetworkDiagnosticsOptions$Builder maxCapturedEvents(int)
public io.github.testlens.selenium.network.NetworkDiagnosticsOptions build()
```

## `io.github.testlens.selenium.network.NetworkDiagnosticsOptions` {#io-github-testlens-selenium-network-networkdiagnosticsoptions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static final int DEFAULT_MAX_CAPTURED_EVENTS
public static io.github.testlens.selenium.network.NetworkDiagnosticsOptions defaults()
public static io.github.testlens.selenium.network.NetworkDiagnosticsOptions$Builder builder()
public io.github.testlens.selenium.network.NetworkCaptureMode captureMode()
public boolean includeHeaders()
public boolean maskSensitiveHeaders()
public int failedStatusThreshold()
public java.util.List<java.util.regex.Pattern> ignoredUrlPatterns()
public int maxCapturedEvents()
public boolean isIgnored(java.lang.String)
```

## `io.github.testlens.selenium.network.NetworkDiagnosticsResult` {#io-github-testlens-selenium-network-networkdiagnosticsresult}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.network.NetworkDiagnosticsResult(io.github.testlens.selenium.network.NetworkDiagnosticsStatus, java.lang.String, io.github.testlens.selenium.network.NetworkSummary, java.lang.Throwable, java.time.Duration)
public static io.github.testlens.selenium.network.NetworkDiagnosticsResult of(io.github.testlens.selenium.network.NetworkDiagnosticsStatus, java.lang.String, io.github.testlens.selenium.network.NetworkSummary, java.time.Duration)
public static io.github.testlens.selenium.network.NetworkDiagnosticsResult failed(java.lang.String, io.github.testlens.selenium.network.NetworkSummary, java.lang.Throwable, java.time.Duration)
public io.github.testlens.selenium.network.NetworkDiagnosticsStatus status()
public java.lang.String message()
public io.github.testlens.selenium.network.NetworkSummary summary()
public java.lang.Throwable exception()
public java.time.Duration elapsed()
```

## `io.github.testlens.selenium.network.NetworkDiagnosticsStatus` {#io-github-testlens-selenium-network-networkdiagnosticsstatus}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.network.NetworkDiagnosticsStatus STARTED
public static final io.github.testlens.selenium.network.NetworkDiagnosticsStatus STOPPED
public static final io.github.testlens.selenium.network.NetworkDiagnosticsStatus ATTACHED
public static final io.github.testlens.selenium.network.NetworkDiagnosticsStatus ASSERTION_PASSED
public static final io.github.testlens.selenium.network.NetworkDiagnosticsStatus ASSERTION_FAILED
public static final io.github.testlens.selenium.network.NetworkDiagnosticsStatus UNSUPPORTED
public static final io.github.testlens.selenium.network.NetworkDiagnosticsStatus FAILED
public static io.github.testlens.selenium.network.NetworkDiagnosticsStatus[] values()
public static io.github.testlens.selenium.network.NetworkDiagnosticsStatus valueOf(java.lang.String)
```

## `io.github.testlens.selenium.network.NetworkEvent` {#io-github-testlens-selenium-network-networkevent}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.network.NetworkEvent request(io.github.testlens.selenium.network.NetworkRequest)
public static io.github.testlens.selenium.network.NetworkEvent response(io.github.testlens.selenium.network.NetworkResponse)
public static io.github.testlens.selenium.network.NetworkEvent failed(io.github.testlens.selenium.network.NetworkFailure)
public static io.github.testlens.selenium.network.NetworkEvent info(java.lang.String)
public static io.github.testlens.selenium.network.NetworkEvent warning(java.lang.String)
public java.lang.String id()
public io.github.testlens.selenium.network.NetworkEventType type()
public io.github.testlens.selenium.network.NetworkRequest request()
public io.github.testlens.selenium.network.NetworkResponse response()
public io.github.testlens.selenium.network.NetworkFailure failure()
public java.lang.String message()
public java.time.Instant timestamp()
public java.util.Map<java.lang.String, java.lang.String> attributes()
public java.lang.String url()
```

## `io.github.testlens.selenium.network.NetworkEventType` {#io-github-testlens-selenium-network-networkeventtype}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.network.NetworkEventType REQUEST
public static final io.github.testlens.selenium.network.NetworkEventType RESPONSE
public static final io.github.testlens.selenium.network.NetworkEventType FAILED
public static final io.github.testlens.selenium.network.NetworkEventType INFO
public static final io.github.testlens.selenium.network.NetworkEventType WARNING
public static io.github.testlens.selenium.network.NetworkEventType[] values()
public static io.github.testlens.selenium.network.NetworkEventType valueOf(java.lang.String)
```

## `io.github.testlens.selenium.network.NetworkFailure` {#io-github-testlens-selenium-network-networkfailure}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.network.NetworkFailure(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.time.Instant)
public static io.github.testlens.selenium.network.NetworkFailure of(java.lang.String, java.lang.String, java.lang.String)
public java.lang.String requestId()
public java.lang.String url()
public java.lang.String message()
public java.lang.String failureType()
public java.time.Instant timestamp()
```

## `io.github.testlens.selenium.network.NetworkLogExporter` {#io-github-testlens-selenium-network-networklogexporter}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.network.NetworkLogExporter()
public java.lang.String export(io.github.testlens.selenium.network.NetworkDiagnostics)
public java.lang.String export(java.util.List<io.github.testlens.selenium.network.NetworkEvent>)
```

## `io.github.testlens.selenium.network.NetworkRequest` {#io-github-testlens-selenium-network-networkrequest}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.network.NetworkRequest(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.time.Instant, java.util.Map<java.lang.String, java.lang.String>)
public static io.github.testlens.selenium.network.NetworkRequest of(java.lang.String, java.lang.String)
public java.lang.String id()
public java.lang.String method()
public java.lang.String url()
public java.lang.String resourceType()
public java.time.Instant timestamp()
public java.util.Map<java.lang.String, java.lang.String> headers()
```

## `io.github.testlens.selenium.network.NetworkResponse` {#io-github-testlens-selenium-network-networkresponse}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.network.NetworkResponse(java.lang.String, java.lang.String, int, java.lang.String, java.lang.String, java.time.Duration, java.time.Instant, java.util.Map<java.lang.String, java.lang.String>)
public static io.github.testlens.selenium.network.NetworkResponse of(java.lang.String, java.lang.String, int)
public java.lang.String requestId()
public java.lang.String url()
public int status()
public java.lang.String statusText()
public java.lang.String mimeType()
public java.time.Duration duration()
public java.time.Instant timestamp()
public java.util.Map<java.lang.String, java.lang.String> headers()
```

## `io.github.testlens.selenium.network.NetworkResponseExpectation` {#io-github-testlens-selenium-network-networkresponseexpectation}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.network.NetworkResponseExpectation urlContains(java.lang.String)
public io.github.testlens.selenium.network.NetworkResponseExpectation urlRegex(java.lang.String)
public io.github.testlens.selenium.network.NetworkResponseExpectation exactUrl(java.lang.String)
public io.github.testlens.selenium.network.NetworkResponseExpectation method(java.lang.String)
public io.github.testlens.selenium.network.NetworkResponseExpectation status(int)
public io.github.testlens.selenium.network.NetworkResponseExpectation statusBetween(int, int)
public io.github.testlens.selenium.network.NetworkWaitResult within(java.time.Duration)
public io.github.testlens.selenium.network.NetworkWaitResult waitNow()
```

## `io.github.testlens.selenium.network.NetworkSummary` {#io-github-testlens-selenium-network-networksummary}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.network.NetworkSummary(int, int, int, int, int, io.github.testlens.selenium.network.NetworkEvent, io.github.testlens.selenium.network.NetworkDiagnosticsStatus)
public io.github.testlens.selenium.network.NetworkSummary(int, int, int, int, int, int, io.github.testlens.selenium.network.NetworkEvent, io.github.testlens.selenium.network.NetworkDiagnosticsStatus)
public static io.github.testlens.selenium.network.NetworkSummary from(java.util.List<io.github.testlens.selenium.network.NetworkEvent>, int, int, io.github.testlens.selenium.network.NetworkDiagnosticsStatus)
public int totalRequests()
public int totalResponses()
public int failedResponses()
public int failedRequests()
public int ignoredEvents()
public int droppedEvents()
public java.util.Optional<io.github.testlens.selenium.network.NetworkEvent> firstFailure()
public io.github.testlens.selenium.network.NetworkDiagnosticsStatus status()
public boolean hasFailures()
public java.lang.String failureSummary()
```

## `io.github.testlens.selenium.network.NetworkWaitCondition$Builder` {#io-github-testlens-selenium-network-networkwaitcondition-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.network.NetworkWaitCondition$Builder urlContains(java.lang.String)
public io.github.testlens.selenium.network.NetworkWaitCondition$Builder urlRegex(java.lang.String)
public io.github.testlens.selenium.network.NetworkWaitCondition$Builder exactUrl(java.lang.String)
public io.github.testlens.selenium.network.NetworkWaitCondition$Builder method(java.lang.String)
public io.github.testlens.selenium.network.NetworkWaitCondition$Builder status(int)
public io.github.testlens.selenium.network.NetworkWaitCondition$Builder minStatus(int)
public io.github.testlens.selenium.network.NetworkWaitCondition$Builder maxStatus(int)
public io.github.testlens.selenium.network.NetworkWaitCondition$Builder statusBetween(int, int)
public io.github.testlens.selenium.network.NetworkWaitCondition$Builder timeout(java.time.Duration)
public io.github.testlens.selenium.network.NetworkWaitCondition$Builder pollInterval(java.time.Duration)
public io.github.testlens.selenium.network.NetworkWaitCondition$Builder includeFailedResponses(boolean)
public io.github.testlens.selenium.network.NetworkWaitCondition$Builder matchRequestOnly(boolean)
public io.github.testlens.selenium.network.NetworkWaitCondition build()
```

## `io.github.testlens.selenium.network.NetworkWaitCondition` {#io-github-testlens-selenium-network-networkwaitcondition}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.network.NetworkWaitCondition$Builder builder()
public boolean matches(io.github.testlens.selenium.network.NetworkEvent, java.util.List<io.github.testlens.selenium.network.NetworkEvent>)
public java.lang.String summary()
public java.lang.String urlContains()
public java.lang.String urlRegex()
public java.lang.String exactUrl()
public java.lang.String method()
public java.lang.Integer status()
public java.lang.Integer minStatus()
public java.lang.Integer maxStatus()
public java.time.Duration timeout()
public java.time.Duration pollInterval()
public boolean includeFailedResponses()
public boolean matchRequestOnly()
```

## `io.github.testlens.selenium.network.NetworkWaitFailureReason` {#io-github-testlens-selenium-network-networkwaitfailurereason}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.network.NetworkWaitFailureReason NO_MATCHING_RESPONSE
public static final io.github.testlens.selenium.network.NetworkWaitFailureReason NO_MATCHING_REQUEST
public static final io.github.testlens.selenium.network.NetworkWaitFailureReason FAILED_RESPONSE_MATCHED
public static final io.github.testlens.selenium.network.NetworkWaitFailureReason CAPTURE_NOT_STARTED
public static final io.github.testlens.selenium.network.NetworkWaitFailureReason UNSUPPORTED_CAPTURE_MODE
public static final io.github.testlens.selenium.network.NetworkWaitFailureReason CAPTURE_START_FAILED
public static final io.github.testlens.selenium.network.NetworkWaitFailureReason UNKNOWN
public static io.github.testlens.selenium.network.NetworkWaitFailureReason[] values()
public static io.github.testlens.selenium.network.NetworkWaitFailureReason valueOf(java.lang.String)
```

## `io.github.testlens.selenium.network.NetworkWaitResult` {#io-github-testlens-selenium-network-networkwaitresult}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.network.NetworkWaitResult matched(io.github.testlens.selenium.network.NetworkWaitCondition, io.github.testlens.selenium.network.NetworkEvent, io.github.testlens.selenium.network.NetworkRequest, int, java.time.Duration)
public static io.github.testlens.selenium.network.NetworkWaitResult timedOut(io.github.testlens.selenium.network.NetworkWaitCondition, int, java.time.Duration, io.github.testlens.selenium.network.NetworkSummary)
public static io.github.testlens.selenium.network.NetworkWaitResult failed(io.github.testlens.selenium.network.NetworkWaitCondition, java.lang.String, io.github.testlens.selenium.network.NetworkWaitFailureReason, java.lang.Throwable, int, java.time.Duration)
public static io.github.testlens.selenium.network.NetworkWaitResult skipped(io.github.testlens.selenium.network.NetworkWaitCondition, java.lang.String, io.github.testlens.selenium.network.NetworkWaitFailureReason, int, java.time.Duration)
public io.github.testlens.selenium.network.NetworkWaitStatus status()
public java.lang.String conditionSummary()
public io.github.testlens.selenium.network.NetworkEvent matchedEvent()
public io.github.testlens.selenium.network.NetworkRequest matchedRequest()
public io.github.testlens.selenium.network.NetworkResponse matchedResponse()
public int attempts()
public java.time.Duration elapsed()
public java.lang.String message()
public io.github.testlens.selenium.network.NetworkWaitFailureReason failureReason()
public java.lang.Throwable exception()
```

## `io.github.testlens.selenium.network.NetworkWaitStatus` {#io-github-testlens-selenium-network-networkwaitstatus}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.network`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.network.NetworkWaitStatus MATCHED
public static final io.github.testlens.selenium.network.NetworkWaitStatus TIMED_OUT
public static final io.github.testlens.selenium.network.NetworkWaitStatus FAILED
public static final io.github.testlens.selenium.network.NetworkWaitStatus SKIPPED
public static io.github.testlens.selenium.network.NetworkWaitStatus[] values()
public static io.github.testlens.selenium.network.NetworkWaitStatus valueOf(java.lang.String)
```
