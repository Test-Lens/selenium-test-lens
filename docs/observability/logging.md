# Structured logging

Package: `io.github.testlens.core.logging`<br>
Module: `selenium-test-lens-core`<br>
API level: **Advanced**

`UiTestLensLogger` fans immutable `UiTestLensLogEntry` values to one or more `UiTestLensLogSink`s. Sink exceptions are swallowed so diagnostic logging does not break browser automation.

## When to use this API

Normal `TestLens` users do **not** need to construct a `UiTestLensLogger`. Facade operations already emit diagnostic events through the configured HUD, trace, and report pipeline after a session starts. Use the direct logging API only when a custom integration needs an additional sink, custom structured entries, or a standalone export outside that facade pipeline.

Direct logging is opt-in. A minimal custom logger can write to the console:

```java
UiTestLensLogger logger = UiTestLensLogger.builder()
        .sink(new ConsoleLogSink())
        .build();

logger.info("Starting external checkout integration");
```

Adding a sink does not change browser actions, retry behavior, or test outcomes. Sink failures are deliberately isolated. To put custom log entries into an existing trace, use `TraceLogSink` as shown in [Trace](trace.md#direct-session-and-log-integration). For export formatting, see [trace and report options](../reference/configuration.md#trace-and-report-options).

## UiTestLensLogger

<!-- API SIGNATURES: io.github.testlens.core.logging.UiTestLensLogger -->
```java
static UiTestLensLogger noop()
static UiTestLensLogger.Builder builder()
void emit(UiTestLensLogEntry entry)
void trace(String message)
void debug(String message)
void info(String message)
void warn(String message)
void error(String message)
void error(String message, Throwable throwable)
UiTestLensLogger withSink(UiTestLensLogSink sink)
```

`noop()` returns a logger without sinks. `builder().sink(...)` ignores null sinks and `build()` returns `noop()` when none were added. `emit(...)` ignores null entries and sends a non-null entry to every sink. The level helpers construct an entry and delegate to `emit(...)`; `error(String)` is equivalent to `error(message, null)`. `withSink(...)` returns the same logger for a null sink or a new logger containing the existing sinks plus the supplied sink.

## Sinks

- `ConsoleLogSink`: writes formatted entries to a `PrintStream`.
- `ConsumerLogSink`: forwards entries to a Java `Consumer`.
- `CompositeLogSink`: combines sinks.
- `InMemoryLogSink`: accumulates snapshots and offers HTML/JSON/text export helpers.
- `TraceLogSink`: maps log entries into a session trace.

## Entries and targets

`UiTestLensLogEntry` is a record with timestamp, level, event type, status, message, step/action, target, metadata, and throwable. Use `builder()`, `info(String)`, or `toBuilder()`. `TargetDescriptor` describes label/selector/element metadata. Enums define levels, event types, and statuses.

`UiTestLensLogLevel` supplies `TRACE`, `DEBUG`, `INFO`, `WARN`, and `ERROR`. `UiTestLensStatus` describes lifecycle outcome (`STARTED`, `PASSED`, `FAILED`, `SKIPPED`, `INFO`, or `WARN`). `UiTestLensEventType` is the detailed event taxonomy used by sinks and exporters, covering general, step, action, wait, assertion, HUD/overlay, actionability, locator, business assertion, evidence, auth, network, React, cleanup, and error events. Consumers normally select these values when building custom entries or filtering a sink; ordinary facade operations populate them automatically.

## Exporters

`PlainTextLogExporter`, `JsonLogExporter`, and `HtmlLogExporter` implement `UiTestLensLogExporter`. They export strings and/or explicit/default paths; HTML overloads can reuse trace HTML report options.

`LogExportOptions` has exactly four record components:

<!-- API SIGNATURES: io.github.testlens.core.logging.export.LogExportOptions -->
```java
LogExportOptions(boolean includeMetadata, boolean includeThrowable, boolean prettyPrint, int maxFieldLength)
static LogExportOptions defaults()
static LogExportOptions compact()
```

`defaults()` enables metadata, throwables, and pretty printing and uses a maximum field length of 500. `compact()` disables those three flags and also uses 500. A non-positive `maxFieldLength` passed to the record constructor is normalized to 500. These options control exported entry content and formatting; they do not select an output path or report title.

`UiTestLensLogger` applies its immutable `RedactionPolicy` once before fan-out. Consequently the HUD, trace sink, built-in exporters, and every external `UiTestLensLogSink` receive the same safe copy, including redacted messages, step/action names, target fields and metadata, and a diagnostic copy of throwable/cause/suppressed messages. The exception propagated to test code is never replaced. `withSink(...)` preserves the logger policy. See [Sensitive-data redaction](../security/redaction.md) for recognized formats, customization, and explicit opt-out behavior.

Network capture always emits raw traffic entries through the complete logger fan-out. `NetworkHudFilter` records its presentation decision as `hudVisible`; only the built-in HUD sink suppresses raw network request/response/failure entries explicitly marked false. The trace sink and external sinks receive them unchanged. Older or manually created network log entries without this metadata remain visible. Control events are never suppressed by this filter.
