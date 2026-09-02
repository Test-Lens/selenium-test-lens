---
search:
  exclude: true
---

# selenium-test-lens-core: `io.github.testlens.core.trace`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.core.trace.RetryOutcomePolicy` {#io-github-testlens-core-trace-retryoutcomepolicy}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/observability/flakiness.md](../../observability/flakiness.md)

```java
public static final io.github.testlens.core.trace.RetryOutcomePolicy REPORT_ONLY
public static final io.github.testlens.core.trace.RetryOutcomePolicy WARN
public static final io.github.testlens.core.trace.RetryOutcomePolicy FAIL_AFTER_N
public static final io.github.testlens.core.trace.RetryOutcomePolicy FAIL_ON_ANY_RETRY
public static io.github.testlens.core.trace.RetryOutcomePolicy[] values()
public static io.github.testlens.core.trace.RetryOutcomePolicy valueOf(java.lang.String)
```

## `io.github.testlens.core.trace.RetryPolicyViolationException` {#io-github-testlens-core-trace-retrypolicyviolationexception}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/flakiness.md](../../observability/flakiness.md)

```java
public io.github.testlens.core.trace.RetryPolicyViolationException(io.github.testlens.core.trace.RetryOutcomePolicy, io.github.testlens.core.trace.RetrySummary)
public io.github.testlens.core.trace.RetryOutcomePolicy policy()
public io.github.testlens.core.trace.RetrySummary retrySummary()
```

## `io.github.testlens.core.trace.RetrySummary` {#io-github-testlens-core-trace-retrysummary}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `USER_API`
- Type kind: `record`
- Functional documentation: [docs/observability/flakiness.md](../../observability/flakiness.md)

```java
public io.github.testlens.core.trace.RetrySummary(long, java.time.Duration, boolean, io.github.testlens.core.trace.RetryOutcomePolicy, boolean, java.util.Map<java.lang.String, java.lang.Long>, java.util.Map<java.lang.String, java.lang.Long>, java.util.Map<java.lang.String, java.lang.Long>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public long totalRetries()
public java.time.Duration timeLost()
public boolean flakyCandidate()
public io.github.testlens.core.trace.RetryOutcomePolicy policy()
public boolean policyTriggered()
public java.util.Map<java.lang.String, java.lang.Long> byAction()
public java.util.Map<java.lang.String, java.lang.Long> byLocator()
public java.util.Map<java.lang.String, java.lang.Long> byException()
```

## `io.github.testlens.core.trace.TraceArtifact` {#io-github-testlens-core-trace-traceartifact}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.core.trace.TraceArtifact screenshot(java.lang.String, java.nio.file.Path)
public static io.github.testlens.core.trace.TraceArtifact video(java.lang.String, java.nio.file.Path)
public static io.github.testlens.core.trace.TraceArtifact url(java.lang.String, io.github.testlens.core.trace.TraceArtifactType, java.lang.String)
public static io.github.testlens.core.trace.TraceArtifact customFile(java.lang.String, java.nio.file.Path, java.lang.String)
public static io.github.testlens.core.trace.TraceArtifact networkLog(java.lang.String, java.nio.file.Path)
public io.github.testlens.core.trace.TraceArtifact withMetadata(java.lang.String, java.lang.String)
public java.lang.String name()
public io.github.testlens.core.trace.TraceArtifactType type()
public java.lang.String path()
public java.lang.String url()
public java.lang.String mediaType()
public java.time.Instant createdAt()
public java.util.Map<java.lang.String, java.lang.String> metadata()
```

## `io.github.testlens.core.trace.TraceArtifactType` {#io-github-testlens-core-trace-traceartifacttype}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.core.trace.TraceArtifactType SCREENSHOT
public static final io.github.testlens.core.trace.TraceArtifactType VIDEO
public static final io.github.testlens.core.trace.TraceArtifactType HTML
public static final io.github.testlens.core.trace.TraceArtifactType JSON
public static final io.github.testlens.core.trace.TraceArtifactType TEXT_LOG
public static final io.github.testlens.core.trace.TraceArtifactType BROWSER_LOG
public static final io.github.testlens.core.trace.TraceArtifactType NETWORK_LOG
public static final io.github.testlens.core.trace.TraceArtifactType CUSTOM_FILE
public static final io.github.testlens.core.trace.TraceArtifactType CUSTOM_URL
public static io.github.testlens.core.trace.TraceArtifactType[] values()
public static io.github.testlens.core.trace.TraceArtifactType valueOf(java.lang.String)
```

## `io.github.testlens.core.trace.TraceEvent$Builder` {#io-github-testlens-core-trace-traceevent-builder}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.core.trace.TraceEvent$Builder id(java.lang.String)
public io.github.testlens.core.trace.TraceEvent$Builder type(io.github.testlens.core.trace.TraceEventType)
public io.github.testlens.core.trace.TraceEvent$Builder status(io.github.testlens.core.trace.TraceStatus)
public io.github.testlens.core.trace.TraceEvent$Builder name(java.lang.String)
public io.github.testlens.core.trace.TraceEvent$Builder message(java.lang.String)
public io.github.testlens.core.trace.TraceEvent$Builder timestamp(java.time.Instant)
public io.github.testlens.core.trace.TraceEvent$Builder duration(java.time.Duration)
public io.github.testlens.core.trace.TraceEvent$Builder parentId(java.lang.String)
public io.github.testlens.core.trace.TraceEvent$Builder failure(io.github.testlens.core.trace.TraceFailure)
public io.github.testlens.core.trace.TraceEvent$Builder artifact(io.github.testlens.core.trace.TraceArtifact)
public io.github.testlens.core.trace.TraceEvent$Builder artifacts(java.util.List<io.github.testlens.core.trace.TraceArtifact>)
public io.github.testlens.core.trace.TraceEvent$Builder attribute(java.lang.String, java.lang.String)
public io.github.testlens.core.trace.TraceEvent$Builder attributes(java.util.Map<java.lang.String, java.lang.String>)
public io.github.testlens.core.trace.TraceEvent build()
```

## `io.github.testlens.core.trace.TraceEvent` {#io-github-testlens-core-trace-traceevent}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.core.trace.TraceEvent$Builder builder(io.github.testlens.core.trace.TraceEventType, io.github.testlens.core.trace.TraceStatus, java.lang.String)
public static io.github.testlens.core.trace.TraceEvent started(io.github.testlens.core.trace.TraceEventType, java.lang.String)
public static io.github.testlens.core.trace.TraceEvent passed(io.github.testlens.core.trace.TraceEventType, java.lang.String, java.time.Duration)
public static io.github.testlens.core.trace.TraceEvent failed(io.github.testlens.core.trace.TraceEventType, java.lang.String, java.lang.Throwable, java.time.Duration)
public static io.github.testlens.core.trace.TraceEvent info(java.lang.String, java.lang.String)
public static io.github.testlens.core.trace.TraceEvent custom(java.lang.String, java.lang.String)
public io.github.testlens.core.trace.TraceEvent$Builder toBuilder()
public java.lang.String id()
public io.github.testlens.core.trace.TraceEventType type()
public io.github.testlens.core.trace.TraceStatus status()
public java.lang.String name()
public java.lang.String message()
public java.time.Instant timestamp()
public java.time.Duration duration()
public java.lang.String parentId()
public io.github.testlens.core.trace.TraceFailure failure()
public java.util.List<io.github.testlens.core.trace.TraceArtifact> artifacts()
public java.util.Map<java.lang.String, java.lang.String> attributes()
```

## `io.github.testlens.core.trace.TraceEventType` {#io-github-testlens-core-trace-traceeventtype}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.core.trace.TraceEventType SESSION_STARTED
public static final io.github.testlens.core.trace.TraceEventType SESSION_FINISHED
public static final io.github.testlens.core.trace.TraceEventType STEP_STARTED
public static final io.github.testlens.core.trace.TraceEventType STEP_PASSED
public static final io.github.testlens.core.trace.TraceEventType STEP_FAILED
public static final io.github.testlens.core.trace.TraceEventType ACTION_STARTED
public static final io.github.testlens.core.trace.TraceEventType ACTION_PASSED
public static final io.github.testlens.core.trace.TraceEventType ACTION_FAILED
public static final io.github.testlens.core.trace.TraceEventType ASSERTION_STARTED
public static final io.github.testlens.core.trace.TraceEventType ASSERTION_PASSED
public static final io.github.testlens.core.trace.TraceEventType ASSERTION_FAILED
public static final io.github.testlens.core.trace.TraceEventType BUSINESS_ASSERTION_STARTED
public static final io.github.testlens.core.trace.TraceEventType BUSINESS_ASSERTION_PASSED
public static final io.github.testlens.core.trace.TraceEventType BUSINESS_ASSERTION_FAILED
public static final io.github.testlens.core.trace.TraceEventType OVERLAY_DETECTED
public static final io.github.testlens.core.trace.TraceEventType OVERLAY_HANDLED
public static final io.github.testlens.core.trace.TraceEventType ACTIONABILITY_CHECK
public static final io.github.testlens.core.trace.TraceEventType LOCATOR_RESOLVE
public static final io.github.testlens.core.trace.TraceEventType LOCATOR_ACTION
public static final io.github.testlens.core.trace.TraceEventType RETRY
public static final io.github.testlens.core.trace.TraceEventType RETRY_SUMMARY
public static final io.github.testlens.core.trace.TraceEventType FAILURE_BUNDLE
public static final io.github.testlens.core.trace.TraceEventType NETWORK_EVENT
public static final io.github.testlens.core.trace.TraceEventType NETWORK_WAIT
public static final io.github.testlens.core.trace.TraceEventType SCREENSHOT
public static final io.github.testlens.core.trace.TraceEventType VIDEO
public static final io.github.testlens.core.trace.TraceEventType ARTIFACT_ATTACHED
public static final io.github.testlens.core.trace.TraceEventType CUSTOM
public static io.github.testlens.core.trace.TraceEventType[] values()
public static io.github.testlens.core.trace.TraceEventType valueOf(java.lang.String)
```

## `io.github.testlens.core.trace.TraceFailure` {#io-github-testlens-core-trace-tracefailure}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.core.trace.TraceFailure(java.lang.String, java.lang.String, java.lang.String, java.util.Map<java.lang.String, java.lang.String>)
public static io.github.testlens.core.trace.TraceFailure from(java.lang.Throwable, boolean)
public io.github.testlens.core.trace.TraceFailure withDetail(java.lang.String, java.lang.String)
public java.lang.String message()
public java.lang.String exceptionType()
public java.lang.String stackTrace()
public java.util.Map<java.lang.String, java.lang.String> details()
```

## `io.github.testlens.core.trace.TraceJsonExportOptions$Builder` {#io-github-testlens-core-trace-tracejsonexportoptions-builder}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.core.trace.TraceJsonExportOptions$Builder includeStackTraces(boolean)
public io.github.testlens.core.trace.TraceJsonExportOptions$Builder includeArtifactMetadata(boolean)
public io.github.testlens.core.trace.TraceJsonExportOptions$Builder includeMissingArtifacts(boolean)
public io.github.testlens.core.trace.TraceJsonExportOptions$Builder artifactBaseDirectory(java.nio.file.Path)
public io.github.testlens.core.trace.TraceJsonExportOptions build()
```

## `io.github.testlens.core.trace.TraceJsonExportOptions` {#io-github-testlens-core-trace-tracejsonexportoptions}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.core.trace.TraceJsonExportOptions defaults()
public static io.github.testlens.core.trace.TraceJsonExportOptions$Builder builder()
public boolean includeStackTraces()
public boolean includeArtifactMetadata()
public boolean includeMissingArtifacts()
public java.nio.file.Path artifactBaseDirectory()
public io.github.testlens.core.trace.TraceJsonExportOptions$Builder toBuilder()
```

## `io.github.testlens.core.trace.TraceJsonExporter` {#io-github-testlens-core-trace-tracejsonexporter}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static final java.nio.file.Path DEFAULT_SUITE_OUTPUT_PATH
public io.github.testlens.core.trace.TraceJsonExporter()
public java.lang.String export(io.github.testlens.core.trace.UiTestLensSession)
public java.lang.String export(io.github.testlens.core.trace.UiTestLensSession, boolean)
public java.lang.String export(io.github.testlens.core.trace.UiTestLensSession, io.github.testlens.core.trace.TraceJsonExportOptions)
public java.nio.file.Path exportTo(io.github.testlens.core.trace.UiTestLensSession, java.nio.file.Path)
public java.nio.file.Path exportTo(io.github.testlens.core.trace.UiTestLensSession, java.nio.file.Path, io.github.testlens.core.trace.TraceJsonExportOptions)
public java.nio.file.Path exportToDefault(io.github.testlens.core.trace.UiTestLensSession)
public java.nio.file.Path exportToDefault(io.github.testlens.core.trace.UiTestLensSession, io.github.testlens.core.trace.TraceJsonExportOptions)
public java.lang.String exportSuite(java.util.List<io.github.testlens.core.trace.UiTestLensSession>)
public java.lang.String exportSuite(java.util.List<io.github.testlens.core.trace.UiTestLensSession>, io.github.testlens.core.trace.TraceJsonExportOptions)
public java.nio.file.Path exportSuiteTo(java.util.List<io.github.testlens.core.trace.UiTestLensSession>, java.nio.file.Path)
public java.nio.file.Path exportSuiteTo(java.util.List<io.github.testlens.core.trace.UiTestLensSession>, java.nio.file.Path, io.github.testlens.core.trace.TraceJsonExportOptions)
public java.nio.file.Path exportSuiteToDefault(java.util.List<io.github.testlens.core.trace.UiTestLensSession>)
public java.nio.file.Path exportSuiteToDefault(java.util.List<io.github.testlens.core.trace.UiTestLensSession>, io.github.testlens.core.trace.TraceJsonExportOptions)
```

## `io.github.testlens.core.trace.TraceLogSink` {#io-github-testlens-core-trace-tracelogsink}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.core.trace.TraceLogSink(io.github.testlens.core.trace.UiTestLensSession)
public void accept(io.github.testlens.core.logging.UiTestLensLogEntry)
```

## `io.github.testlens.core.trace.TraceMetadata$Builder` {#io-github-testlens-core-trace-tracemetadata-builder}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.core.trace.TraceMetadata$Builder startedAt(java.time.Instant)
public io.github.testlens.core.trace.TraceMetadata$Builder finishedAt(java.time.Instant)
public io.github.testlens.core.trace.TraceMetadata$Builder status(io.github.testlens.core.trace.TraceStatus)
public io.github.testlens.core.trace.TraceMetadata$Builder environment(java.lang.String)
public io.github.testlens.core.trace.TraceMetadata$Builder label(java.lang.String, java.lang.String)
public io.github.testlens.core.trace.TraceMetadata$Builder labels(java.util.Map<java.lang.String, java.lang.String>)
public io.github.testlens.core.trace.TraceMetadata build()
```

## `io.github.testlens.core.trace.TraceMetadata` {#io-github-testlens-core-trace-tracemetadata}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.core.trace.TraceMetadata$Builder builder(java.lang.String, java.lang.String)
public io.github.testlens.core.trace.TraceMetadata$Builder toBuilder()
public java.lang.String sessionId()
public java.lang.String name()
public java.time.Instant startedAt()
public java.time.Instant finishedAt()
public io.github.testlens.core.trace.TraceStatus status()
public java.lang.String environment()
public java.util.Map<java.lang.String, java.lang.String> labels()
```

## `io.github.testlens.core.trace.TraceStatus` {#io-github-testlens-core-trace-tracestatus}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.core.trace.TraceStatus STARTED
public static final io.github.testlens.core.trace.TraceStatus PASSED
public static final io.github.testlens.core.trace.TraceStatus FAILED
public static final io.github.testlens.core.trace.TraceStatus SKIPPED
public static final io.github.testlens.core.trace.TraceStatus INFO
public static final io.github.testlens.core.trace.TraceStatus WARNING
public static final io.github.testlens.core.trace.TraceStatus ERROR
public static io.github.testlens.core.trace.TraceStatus[] values()
public static io.github.testlens.core.trace.TraceStatus valueOf(java.lang.String)
```

## `io.github.testlens.core.trace.TraceStep` {#io-github-testlens-core-trace-tracestep}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `ADVANCED_API`
- Type kind: `record`

```java
public io.github.testlens.core.trace.TraceStep(java.lang.String, java.lang.String, io.github.testlens.core.trace.TraceStatus, java.time.Instant, java.time.Instant, java.time.Duration, io.github.testlens.core.trace.TraceFailure)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String id()
public java.lang.String name()
public io.github.testlens.core.trace.TraceStatus status()
public java.time.Instant startedAt()
public java.time.Instant endedAt()
public java.time.Duration duration()
public io.github.testlens.core.trace.TraceFailure failure()
```

## `io.github.testlens.core.trace.TraceTimeline` {#io-github-testlens-core-trace-tracetimeline}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.core.trace.TraceTimeline()
public synchronized io.github.testlens.core.trace.TraceEvent add(io.github.testlens.core.trace.TraceEvent)
public synchronized java.util.List<io.github.testlens.core.trace.TraceEvent> events()
```

## `io.github.testlens.core.trace.UiTestLensSession` {#io-github-testlens-core-trace-uitestlenssession}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.core.trace.UiTestLensSession start(java.lang.String)
public static io.github.testlens.core.trace.UiTestLensSession start(java.lang.String, io.github.testlens.core.trace.RetryOutcomePolicy, int)
public java.lang.String id()
public synchronized io.github.testlens.core.trace.TraceMetadata metadata()
public synchronized java.util.List<io.github.testlens.core.trace.TraceEvent> events()
public synchronized java.util.List<io.github.testlens.core.trace.TraceArtifact> artifacts()
public synchronized io.github.testlens.core.trace.RetrySummary retrySummary()
public synchronized io.github.testlens.core.trace.TraceEvent addEvent(io.github.testlens.core.trace.TraceEvent)
public synchronized io.github.testlens.core.trace.TraceArtifact attachArtifact(io.github.testlens.core.trace.TraceArtifact)
public io.github.testlens.core.trace.TraceArtifact attachScreenshot(java.lang.String, java.nio.file.Path)
public io.github.testlens.core.trace.TraceArtifact attachVideo(java.lang.String, java.nio.file.Path)
public io.github.testlens.core.trace.TraceArtifact attachUrl(java.lang.String, io.github.testlens.core.trace.TraceArtifactType, java.lang.String)
public synchronized void finishPassed()
public synchronized void finishFailed(java.lang.Throwable)
public synchronized void finishSkipped(java.lang.String)
public java.lang.String exportJson()
public java.lang.String exportJson(io.github.testlens.core.trace.TraceJsonExportOptions)
public java.nio.file.Path exportJson(java.nio.file.Path)
public java.nio.file.Path exportJson(java.nio.file.Path, io.github.testlens.core.trace.TraceJsonExportOptions)
public java.nio.file.Path exportJsonReport()
public java.nio.file.Path exportJsonReport(io.github.testlens.core.trace.TraceJsonExportOptions)
public java.lang.String exportHtml()
public java.lang.String exportHtml(io.github.testlens.core.trace.export.TraceHtmlExportOptions)
public java.nio.file.Path exportHtml(java.nio.file.Path)
public java.nio.file.Path exportHtml(java.nio.file.Path, io.github.testlens.core.trace.export.TraceHtmlExportOptions)
public java.nio.file.Path exportHtmlReport()
public java.nio.file.Path exportHtmlReport(io.github.testlens.core.trace.export.TraceHtmlExportOptions)
```
