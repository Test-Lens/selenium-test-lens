---
search:
  exclude: true
---

# selenium-test-lens-core: `io.github.testlens.core.logging`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.core.logging.CompositeLogSink` {#io-github-testlens-core-logging-compositelogsink}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.core.logging.CompositeLogSink(java.util.List<io.github.testlens.core.logging.UiTestLensLogSink>)
public static io.github.testlens.core.logging.CompositeLogSink of(io.github.testlens.core.logging.UiTestLensLogSink...)
public void accept(io.github.testlens.core.logging.UiTestLensLogEntry)
```

## `io.github.testlens.core.logging.ConsoleLogSink` {#io-github-testlens-core-logging-consolelogsink}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.core.logging.ConsoleLogSink()
public void accept(io.github.testlens.core.logging.UiTestLensLogEntry)
```

## `io.github.testlens.core.logging.ConsumerLogSink` {#io-github-testlens-core-logging-consumerlogsink}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.core.logging.ConsumerLogSink(java.util.function.Consumer<io.github.testlens.core.logging.UiTestLensLogEntry>)
public void accept(io.github.testlens.core.logging.UiTestLensLogEntry)
```

## `io.github.testlens.core.logging.InMemoryLogSink` {#io-github-testlens-core-logging-inmemorylogsink}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.core.logging.InMemoryLogSink()
public void accept(io.github.testlens.core.logging.UiTestLensLogEntry)
public java.util.List<io.github.testlens.core.logging.UiTestLensLogEntry> entries()
public void clear()
public java.lang.String export(io.github.testlens.core.logging.export.UiTestLensLogExporter)
public java.lang.String exportAsText()
public java.lang.String exportAsJson()
public java.nio.file.Path exportJson(java.nio.file.Path)
public java.lang.String exportAsHtml()
public java.nio.file.Path exportHtml(java.nio.file.Path)
public java.nio.file.Path exportHtml(java.nio.file.Path, io.github.testlens.core.trace.export.TraceHtmlExportOptions)
public java.nio.file.Path exportHtmlReport()
public java.nio.file.Path exportHtmlReport(io.github.testlens.core.trace.export.TraceHtmlExportOptions)
```

## `io.github.testlens.core.logging.TargetDescriptor` {#io-github-testlens-core-logging-targetdescriptor}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging`
- Classification: `ADVANCED_API`
- Type kind: `record`

```java
public io.github.testlens.core.logging.TargetDescriptor(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Map<java.lang.String, java.lang.String>)
public static io.github.testlens.core.logging.TargetDescriptor none()
public static io.github.testlens.core.logging.TargetDescriptor selector(java.lang.String)
public static io.github.testlens.core.logging.TargetDescriptor label(java.lang.String)
public io.github.testlens.core.logging.TargetDescriptor withMetadata(java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String selector()
public java.lang.String label()
public java.lang.String tagName()
public java.lang.String text()
public java.util.Map<java.lang.String, java.lang.String> metadata()
```

## `io.github.testlens.core.logging.UiTestLensEventType` {#io-github-testlens-core-logging-uitestlenseventtype}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.core.logging.UiTestLensEventType GENERAL
public static final io.github.testlens.core.logging.UiTestLensEventType STEP
public static final io.github.testlens.core.logging.UiTestLensEventType ACTION
public static final io.github.testlens.core.logging.UiTestLensEventType WAIT
public static final io.github.testlens.core.logging.UiTestLensEventType ASSERTION
public static final io.github.testlens.core.logging.UiTestLensEventType HUD
public static final io.github.testlens.core.logging.UiTestLensEventType OVERLAY
public static final io.github.testlens.core.logging.UiTestLensEventType OVERLAY_POLICY_STARTED
public static final io.github.testlens.core.logging.UiTestLensEventType OVERLAY_DETECTED
public static final io.github.testlens.core.logging.UiTestLensEventType OVERLAY_ACTION_STARTED
public static final io.github.testlens.core.logging.UiTestLensEventType OVERLAY_ACTION_PASSED
public static final io.github.testlens.core.logging.UiTestLensEventType OVERLAY_ACTION_FAILED
public static final io.github.testlens.core.logging.UiTestLensEventType OVERLAY_HANDLED
public static final io.github.testlens.core.logging.UiTestLensEventType OVERLAY_STILL_VISIBLE
public static final io.github.testlens.core.logging.UiTestLensEventType ACTIONABILITY_CHECK_STARTED
public static final io.github.testlens.core.logging.UiTestLensEventType ACTIONABILITY_CHECK_PASSED
public static final io.github.testlens.core.logging.UiTestLensEventType ACTIONABILITY_CHECK_FAILED
public static final io.github.testlens.core.logging.UiTestLensEventType ACTIONABILITY_READY
public static final io.github.testlens.core.logging.UiTestLensEventType ACTIONABILITY_NOT_READY
public static final io.github.testlens.core.logging.UiTestLensEventType LOCATOR_RESOLVE_STARTED
public static final io.github.testlens.core.logging.UiTestLensEventType LOCATOR_RESOLVE_PASSED
public static final io.github.testlens.core.logging.UiTestLensEventType LOCATOR_RESOLVE_FAILED
public static final io.github.testlens.core.logging.UiTestLensEventType LOCATOR_ACTION_STARTED
public static final io.github.testlens.core.logging.UiTestLensEventType LOCATOR_ACTION_PASSED
public static final io.github.testlens.core.logging.UiTestLensEventType LOCATOR_ACTION_FAILED
public static final io.github.testlens.core.logging.UiTestLensEventType LOCATOR_RETRY
public static final io.github.testlens.core.logging.UiTestLensEventType ASSERTION_STARTED
public static final io.github.testlens.core.logging.UiTestLensEventType ASSERTION_PASSED
public static final io.github.testlens.core.logging.UiTestLensEventType ASSERTION_FAILED
public static final io.github.testlens.core.logging.UiTestLensEventType ASSERTION_RETRY
public static final io.github.testlens.core.logging.UiTestLensEventType ASSERTION_TIMED_OUT
public static final io.github.testlens.core.logging.UiTestLensEventType BUSINESS_ASSERTION_GROUP_STARTED
public static final io.github.testlens.core.logging.UiTestLensEventType BUSINESS_ASSERTION_STARTED
public static final io.github.testlens.core.logging.UiTestLensEventType BUSINESS_ASSERTION_PASSED
public static final io.github.testlens.core.logging.UiTestLensEventType BUSINESS_ASSERTION_FAILED
public static final io.github.testlens.core.logging.UiTestLensEventType BUSINESS_ASSERTION_GROUP_PASSED
public static final io.github.testlens.core.logging.UiTestLensEventType BUSINESS_ASSERTION_GROUP_FAILED
public static final io.github.testlens.core.logging.UiTestLensEventType STEP_STARTED
public static final io.github.testlens.core.logging.UiTestLensEventType STEP_PASSED
public static final io.github.testlens.core.logging.UiTestLensEventType STEP_FAILED
public static final io.github.testlens.core.logging.UiTestLensEventType STEP_SKIPPED
public static final io.github.testlens.core.logging.UiTestLensEventType SCREENSHOT_CAPTURE_STARTED
public static final io.github.testlens.core.logging.UiTestLensEventType SCREENSHOT_CAPTURE_PASSED
public static final io.github.testlens.core.logging.UiTestLensEventType SCREENSHOT_CAPTURE_FAILED
public static final io.github.testlens.core.logging.UiTestLensEventType VIDEO_ATTACHED
public static final io.github.testlens.core.logging.UiTestLensEventType VIDEO_ATTACH_FAILED
public static final io.github.testlens.core.logging.UiTestLensEventType VIDEO_ATTACH_SKIPPED
public static final io.github.testlens.core.logging.UiTestLensEventType AUTH_STATE_CAPTURE_STARTED
public static final io.github.testlens.core.logging.UiTestLensEventType AUTH_STATE_CAPTURE_PASSED
public static final io.github.testlens.core.logging.UiTestLensEventType AUTH_STATE_CAPTURE_FAILED
public static final io.github.testlens.core.logging.UiTestLensEventType AUTH_STATE_RESTORE_STARTED
public static final io.github.testlens.core.logging.UiTestLensEventType AUTH_STATE_RESTORE_PASSED
public static final io.github.testlens.core.logging.UiTestLensEventType AUTH_STATE_RESTORE_FAILED
public static final io.github.testlens.core.logging.UiTestLensEventType AUTH_STATE_RESTORE_SKIPPED
public static final io.github.testlens.core.logging.UiTestLensEventType NETWORK_DIAGNOSTICS_STARTED
public static final io.github.testlens.core.logging.UiTestLensEventType NETWORK_DIAGNOSTICS_STOPPED
public static final io.github.testlens.core.logging.UiTestLensEventType NETWORK_REQUEST_RECORDED
public static final io.github.testlens.core.logging.UiTestLensEventType NETWORK_RESPONSE_RECORDED
public static final io.github.testlens.core.logging.UiTestLensEventType NETWORK_FAILURE_RECORDED
public static final io.github.testlens.core.logging.UiTestLensEventType NETWORK_ASSERTION_PASSED
public static final io.github.testlens.core.logging.UiTestLensEventType NETWORK_ASSERTION_FAILED
public static final io.github.testlens.core.logging.UiTestLensEventType NETWORK_LOG_ATTACHED
public static final io.github.testlens.core.logging.UiTestLensEventType NETWORK_WAIT_STARTED
public static final io.github.testlens.core.logging.UiTestLensEventType NETWORK_WAIT_PASSED
public static final io.github.testlens.core.logging.UiTestLensEventType NETWORK_WAIT_FAILED
public static final io.github.testlens.core.logging.UiTestLensEventType NETWORK_WAIT_TIMED_OUT
public static final io.github.testlens.core.logging.UiTestLensEventType HIGHLIGHT
public static final io.github.testlens.core.logging.UiTestLensEventType API
public static final io.github.testlens.core.logging.UiTestLensEventType REACT
public static final io.github.testlens.core.logging.UiTestLensEventType CLEANUP
public static final io.github.testlens.core.logging.UiTestLensEventType ERROR
public static io.github.testlens.core.logging.UiTestLensEventType[] values()
public static io.github.testlens.core.logging.UiTestLensEventType valueOf(java.lang.String)
```

## `io.github.testlens.core.logging.UiTestLensLogEntry$Builder` {#io-github-testlens-core-logging-uitestlenslogentry-builder}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.core.logging.UiTestLensLogEntry$Builder timestamp(java.time.Instant)
public io.github.testlens.core.logging.UiTestLensLogEntry$Builder level(io.github.testlens.core.logging.UiTestLensLogLevel)
public io.github.testlens.core.logging.UiTestLensLogEntry$Builder eventType(io.github.testlens.core.logging.UiTestLensEventType)
public io.github.testlens.core.logging.UiTestLensLogEntry$Builder status(io.github.testlens.core.logging.UiTestLensStatus)
public io.github.testlens.core.logging.UiTestLensLogEntry$Builder message(java.lang.String)
public io.github.testlens.core.logging.UiTestLensLogEntry$Builder step(java.lang.String)
public io.github.testlens.core.logging.UiTestLensLogEntry$Builder action(java.lang.String)
public io.github.testlens.core.logging.UiTestLensLogEntry$Builder target(io.github.testlens.core.logging.TargetDescriptor)
public io.github.testlens.core.logging.UiTestLensLogEntry$Builder metadata(java.util.Map<java.lang.String, java.lang.String>)
public io.github.testlens.core.logging.UiTestLensLogEntry$Builder metadata(java.lang.String, java.lang.String)
public io.github.testlens.core.logging.UiTestLensLogEntry$Builder throwable(java.lang.Throwable)
public io.github.testlens.core.logging.UiTestLensLogEntry build()
```

## `io.github.testlens.core.logging.UiTestLensLogEntry` {#io-github-testlens-core-logging-uitestlenslogentry}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging`
- Classification: `ADVANCED_API`
- Type kind: `record`

```java
public io.github.testlens.core.logging.UiTestLensLogEntry(java.time.Instant, io.github.testlens.core.logging.UiTestLensLogLevel, io.github.testlens.core.logging.UiTestLensEventType, io.github.testlens.core.logging.UiTestLensStatus, java.lang.String, java.lang.String, java.lang.String, io.github.testlens.core.logging.TargetDescriptor, java.util.Map<java.lang.String, java.lang.String>, java.lang.Throwable)
public static io.github.testlens.core.logging.UiTestLensLogEntry$Builder builder()
public static io.github.testlens.core.logging.UiTestLensLogEntry info(java.lang.String)
public static io.github.testlens.core.logging.UiTestLensLogEntry warn(java.lang.String)
public static io.github.testlens.core.logging.UiTestLensLogEntry error(java.lang.String, java.lang.Throwable)
public io.github.testlens.core.logging.UiTestLensLogEntry$Builder toBuilder()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.time.Instant timestamp()
public io.github.testlens.core.logging.UiTestLensLogLevel level()
public io.github.testlens.core.logging.UiTestLensEventType eventType()
public io.github.testlens.core.logging.UiTestLensStatus status()
public java.lang.String message()
public java.lang.String step()
public java.lang.String action()
public io.github.testlens.core.logging.TargetDescriptor target()
public java.util.Map<java.lang.String, java.lang.String> metadata()
public java.lang.Throwable throwable()
```

## `io.github.testlens.core.logging.UiTestLensLogLevel` {#io-github-testlens-core-logging-uitestlensloglevel}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.core.logging.UiTestLensLogLevel TRACE
public static final io.github.testlens.core.logging.UiTestLensLogLevel DEBUG
public static final io.github.testlens.core.logging.UiTestLensLogLevel INFO
public static final io.github.testlens.core.logging.UiTestLensLogLevel WARN
public static final io.github.testlens.core.logging.UiTestLensLogLevel ERROR
public static io.github.testlens.core.logging.UiTestLensLogLevel[] values()
public static io.github.testlens.core.logging.UiTestLensLogLevel valueOf(java.lang.String)
```

## `io.github.testlens.core.logging.UiTestLensLogSink` {#io-github-testlens-core-logging-uitestlenslogsink}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging`
- Classification: `ADVANCED_API`
- Type kind: `interface`

```java
public abstract void accept(io.github.testlens.core.logging.UiTestLensLogEntry)
```

## `io.github.testlens.core.logging.UiTestLensLogger$Builder` {#io-github-testlens-core-logging-uitestlenslogger-builder}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.core.logging.UiTestLensLogger$Builder sink(io.github.testlens.core.logging.UiTestLensLogSink)
public io.github.testlens.core.logging.UiTestLensLogger build()
```

## `io.github.testlens.core.logging.UiTestLensLogger` {#io-github-testlens-core-logging-uitestlenslogger}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.core.logging.UiTestLensLogger noop()
public static io.github.testlens.core.logging.UiTestLensLogger$Builder builder()
public void emit(io.github.testlens.core.logging.UiTestLensLogEntry)
public void trace(java.lang.String)
public void debug(java.lang.String)
public void info(java.lang.String)
public void warn(java.lang.String)
public void error(java.lang.String)
public void error(java.lang.String, java.lang.Throwable)
public io.github.testlens.core.logging.UiTestLensLogger withSink(io.github.testlens.core.logging.UiTestLensLogSink)
```

## `io.github.testlens.core.logging.UiTestLensStatus` {#io-github-testlens-core-logging-uitestlensstatus}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.core.logging.UiTestLensStatus STARTED
public static final io.github.testlens.core.logging.UiTestLensStatus PASSED
public static final io.github.testlens.core.logging.UiTestLensStatus FAILED
public static final io.github.testlens.core.logging.UiTestLensStatus SKIPPED
public static final io.github.testlens.core.logging.UiTestLensStatus INFO
public static final io.github.testlens.core.logging.UiTestLensStatus WARN
public static io.github.testlens.core.logging.UiTestLensStatus[] values()
public static io.github.testlens.core.logging.UiTestLensStatus valueOf(java.lang.String)
```
