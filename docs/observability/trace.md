# Trace

Package: `io.github.testlens.core.trace`<br>
Module: `selenium-test-lens-core`<br>
API level: **Advanced** for direct construction; facade recording is recommended.

`UiTestLensSession` owns `TraceMetadata`, a `TraceTimeline`, and artifacts/events. `TestLens.startSession` obtains and activates it; normal locator/step/context operations append trace events through the facade's event/reporting pipeline.

## What a trace gives you

A trace is the structured history of a Test Lens session: steps, browser actions, waits, assertions, failures, and attached evidence in execution order. Use it when a failed test needs more context than its exception alone provides, or when a CI integration needs machine-readable execution data.

Trace recording is automatic after a normal session starts. You do not need to create a trace recorder or call a record method for each operation:

```java
TestLens lens = TestLens.attach(driver);
lens.startSession("Checkout");

lens.getByRole("button", "Pay").click();
TestLensFinalizationResult result = lens.finishPassed();

System.out.println(result.jsonReport());
```

Finalization attempts to write `trace.json` below the configured session output directory. The returned path can be null if export failed; inspect `diagnosticFailures()` rather than assuming the file exists. See [session lifecycle and finalization](../reference/test-lens.md#creation-and-lifecycle), [`TestLensOptions`](../reference/configuration.md#testlensoptions), and [trace/report export options](../reference/configuration.md#trace-and-report-options).

Trace collection observes operations; it does not change their wait conditions, retry intervals, or failure outcome. Names, messages, paths, and attached metadata are persisted, so avoid putting credentials or tokens in them.

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

Builder inputs are copied where implemented, but the model may contain file paths and arbitrary caller strings. Do not place secrets in names, labels, messages, attributes, or artifact metadata.

## TraceJsonExporter

Exports one session or suite to a string/default/custom path. `TraceJsonExportOptions` controls stack traces, artifact metadata, missing artifacts, and an optional artifact base directory. Stack traces and artifact paths can disclose local infrastructure.

See [Reports](reports.md) for defaults and bundle behavior.
