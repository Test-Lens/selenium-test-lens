# Trace

Package: `io.github.testlens.core.trace`<br>
Module: `selenium-test-lens-core`<br>
API level: **Advanced** for direct construction; facade recording is recommended.

`UiTestLensSession` owns `TraceMetadata`, a `TraceTimeline`, and artifacts/events. `TestLens.startSession` obtains and activates it; normal locator/step/context operations append trace events through the facade's event/reporting pipeline.

## What a trace gives you

A trace is the structured history of a Test Lens session: steps, browser actions, waits, assertions, failures, and attached evidence in execution order. Use it when a failed test needs more context than its exception alone provides, or when a CI integration needs machine-readable execution data.

The trace is the spine of the observability workflow: operations append events, finalization fixes the terminal session outcome, HTML/JSON exporters present the timeline, and failure bundles collect the resulting reports with other evidence. Trace and reports themselves are available in stable `0.1.0`.

## Bounded retention (0.4.0)

Every session uses one bounded store behind the existing trace pipeline. The default limits are 4,096 retained
event/artifact references, 8 MiB of deterministically estimated retained diagnostic data, and 256 KiB for one
event. These values were selected after the repository's Chrome/Firefox fixtures produced 18–65 events,
approximately 42–141 KiB JSON reports, and a largest observed event of about 1.8 KiB. The defaults leave a
large margin for ordinary tests while imposing a real upper bound; binary PNG/ZIP/network artifacts keep their
own limits and only their inline metadata counts here.

```java
TestLensOptions options = TestLensOptions.builder()
        .traceRetention(TraceRetentionOptions.builder()
                .maxEvents(2_000)
                .maxBytes(4L * 1024 * 1024)
                .maxEventBytes(128L * 1024)
                .passedSessionRetention(PassedTraceRetention.RETAIN_TRACE)
                .build())
        .build();
```

`RETAIN_TRACE` retains the bounded trace after a passed session; it does **not** mean unlimited history.
`SUMMARY_ONLY` releases passed-session detail after the immutable terminal summary is formed. Failed sessions
preserve a bounded tail before the accepted terminal failure, a compact root-failure record, and bounded
post-failure diagnostics. A caught action error does not freeze the test: the freeze occurs only when the runner
or `finishFailed(...)` accepts the terminal outcome.

JSON contains an additive `retention` object, and HTML contains a Trace completeness section. Count/byte
evictions, oversized truncation/drop, late callbacks, and partial artifact capture are explicit. Normal bounded
eviction never changes the functional test status. Strings are truncated only after central redaction and at
Unicode grapheme boundaries. Finalization closes one immutable snapshot used by JSON, HTML, and the failure
bundle; later callbacks are dropped rather than reopening a completed report.

Trace recording is automatic after a normal session starts. You do not need to create a trace recorder or call a record method for each operation:

```java
TestLens lens = TestLens.attach(driver);
lens.startSession("Checkout");

lens.getByRole("button", "Pay").click();
TestLensFinalizationResult result = lens.finishPassed();

System.out.println(result.jsonReport());
```

Finalization attempts to write `trace.json` below the configured session output directory. The returned path can be null if export failed; inspect `diagnosticFailures()` rather than assuming the file exists. See [session lifecycle and finalization](../reference/test-lens.md#creation-and-lifecycle), [`TestLensOptions`](../reference/configuration.md#testlensoptions), and [trace/report export options](../reference/configuration.md#trace-and-report-options).

The facade maps the first finalization directly to trace status: `finishPassed()` produces `PASSED`, `finishFailed(...)` produces `FAILED` even for a null throwable, and `finishSkipped(reason)` produces `SKIPPED`. The first terminal outcome is immutable; later and concurrent finalizers add neither another retry decision nor another terminal event. Every session therefore has exactly one `RETRY_SUMMARY` and one `SESSION_FINISHED`. The skip reason is the message of that event; a null reason is normalized to an empty message by the session model.

Recovery retries use `TraceEventType.RETRY`; the summary and policy decision are recorded before `SESSION_FINISHED`. Polling events retain their timeline visibility but never become retry events or make `flakyCandidate` true. The default policy observes without changing outcomes; configured fail policies act only during `finishPassed()`. Names, messages, paths, and attached metadata are persisted, so avoid putting credentials or tokens in them.

Network diagnostics emit typed start/stop, request, response, fetch-error, wait, and assertion log events into the attached trace. Request/response/failure URLs, messages, attributes, and headers cross the effective central redaction policy before entering the bounded network buffer; exporters retain a defensive safe-data boundary. Lens-owned capture is stopped before `SESSION_FINISHED`.

An invalid network capture generation emits `NETWORK_ASSERTION_FAILED` when `assertNoFailedRequests()` is attempted and never emits a corresponding success. Its summary retains the real lifecycle status (`STOPPED`, `UNSUPPORTED`, or `FAILED`). This failure is neither recovery retry nor a flaky-candidate signal.

## Direct session and log integration

Direct `UiTestLensSession` construction is advanced API for integrations that do not use the `TestLens` facade. Its public methods cover `start`, event and artifact recording, finalization, snapshots/accessors, and HTML/JSON export overloads. There is no public general-purpose log-recording method on the session. To include structured log entries, create a `TraceLogSink` and add it to a `UiTestLensLogger`; the sink maps accepted entries to trace events:

```java
UiTestLensSession session = UiTestLensSession.start("checkout");
UiTestLensLogger logger = UiTestLensLogger.builder()
        .sink(new TraceLogSink(session))
        .build();
logger.info("Opening checkout");
```

A directly managed session is mutable and is not a cross-thread coordination primitive. Normal Test Lens users should use the automatic facade pipeline above. Refer to [Structured logging](logging.md) for custom sinks and to the [exhaustive signatures](../reference/public-api-catalog.md) when building an integration.

## Model

- `TraceEvent`: builder-based event with id, type, status, name, message, timestamp, duration, parent, failure, artifacts, and string attributes.
- `TraceFailure`: exception type/message/stack/cause representation.
- `TraceArtifact`: typed file/URL evidence with metadata.
- `TraceMetadata`: session identity, time/status, environment, labels.
- `TraceStep`, `TraceTimeline`: step/event views.
- enums: `TraceEventType`, `TraceStatus`, `TraceArtifactType`.

Screenshot artifacts include completed capture mode, pixel width/height, tile count, and bounded duration metadata. Capture failures report the requested mode and safe reason through screenshot operation events. Neither viewport nor full-page capture is a recovery retry.

Direct `UiTestLensSession.addEvent(...)` and artifact attachment store a redacted immutable copy, covering names, messages, attributes, failure text, and artifact diagnostic fields even when no logger is involved. Logger-originated trace events have already crossed the same policy before sink fan-out. Original runtime exceptions still control the test; trace failure data is a safe diagnostic copy. Its `exceptionType` is structural provenance captured from the original throwable before wrapping, while message, cause, suppressed, and stack text remain redacted. Avoid arbitrary personal data because central redaction recognizes structured/configured secrets rather than every sensitive value.

## TraceJsonExporter

Exports one session or suite to a string/default/custom path. `TraceJsonExportOptions` controls stack traces, artifact metadata, missing artifacts, and an optional artifact base directory. Stack traces and artifact paths can disclose local infrastructure.

In suite snapshots, `STARTED` means that the source session had not been finalized when it was observed. The suite summary reports these sessions separately as `started`; they do not contribute to `passed` or `failed`. Export is read-only and leaves metadata, events, artifacts, and the ability to perform the later first terminal finalization unchanged.

See [Reports](reports.md) for defaults and bundle behavior.
