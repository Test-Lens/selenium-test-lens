# Reports

Test Lens produces two complementary session reports: `report.html` is a human-readable investigation view, while `trace.json` is the machine-readable event model for CI tooling or custom processing. Use the HTML report to understand a failed journey and the JSON report when another system needs structured results.

## Get the reports from a normal session

Reports are attempted automatically when an active `TestLens` session is finalized; no exporter setup is required for the standard path:

```java
TestLens lens = TestLens.attach(driver);
lens.startSession("Checkout");

lens.getByRole("button", "Pay").click();
TestLensFinalizationResult result = lens.finishPassed();

Path htmlReport = result.htmlReport();
Path jsonReport = result.jsonReport();
```

Open the returned HTML path in a browser to inspect the status, timeline, failures, evidence, and the dedicated `Flakiness` section. Feed the JSON path to reporting or archival tooling when structured data is required. On failure, call `finishFailed(Throwable)` with the original exception; for an aborted or skipped test, call `finishSkipped(reason)`.

All three facade finalizers—`finishPassed()`, `finishFailed(Throwable)`, and `finishSkipped(String)`—attempt per-session `trace.json` and `report.html`. One caller owns the complete pipeline; concurrent callers wait and receive the same fully built result, while later calls reuse it without rewriting either report or recreating evidence. The reports retain the first `PASSED`, `FAILED`, or `SKIPPED` outcome, and a skip reason is stored on the single `SESSION_FINISHED` event. Each export is best effort: its `Path` can be null and the error appears in `TestLensFinalizationResult.diagnosticFailures()` without changing the session status.

The session JSON always contains a top-level `flakiness` object with `flakyCandidate`, `totalRetries`, `timeLostMs`, `policy`, `policyTriggered`, `byAction`, `byLocator`, and `byException`. HTML renders the same data neutrally for zero retries, as information for `REPORT_ONLY`, as a warning for `WARN`, and as a failure for a triggered fail policy. The destination is derived from the session output configuration. See [Flakiness and retry outcomes](flakiness.md), [session lifecycle and finalization](../reference/test-lens.md#creation-and-lifecycle), and [`TestLensOptions`](../reference/configuration.md#testlensoptions).

For final `FAILED`, HTML also contains a `Failure bundle` section linking the predictable ZIP and listing every component status, size, path, or collection error. The ZIP is assembled after final trace/report exports, so it contains their final versions without recursively containing itself. See [Failure bundles](failure-bundles.md).

!!! info "Coming in 0.2.0"
    The automatic failure-bundle section and hardened exactly-once facade finalization are part of the development line and are not available in Maven Central `0.1.0`. Session HTML/JSON reports are available in `0.1.0`.

An explicitly attached network JSON export is an object containing the requested and active capture modes, capture status, ignored/dropped counters, and request/response/fetch-error events with correlation attributes. Its public models and assertion diagnostics are immutable redacted snapshots; raw URLs and headers remain private to matching and correlation. A failed session's bundle contains a smaller `network-summary.json` snapshot taken before Lens stops its active capture; it does not start capture or include request/response bodies.

Polling assertions add their start/retry/pass/timeout/failure events to the normal trace and HTML timeline. Count and state results include actual attempt and elapsed values, while attribute diagnostics expose only the attribute name, presence, and value lengths. Class diagnostics bound the expected token, and CSS previews redact `url(...)` contents. These assertion events do not create recovery `RETRY` entries or affect the Flakiness section.

## Advanced exporters

Use the exporter classes only when you need an in-memory string, an explicit destination, a suite report, or a ZIP bundle.

### TraceHtmlExporter

`TraceHtmlExporter` has only a public no-argument constructor:

<!-- API SIGNATURES: io.github.testlens.core.trace.export.TraceHtmlExporter -->
```java
TraceHtmlExporter()
```

Export overloads cover a single session and suites, returning HTML strings or writing explicit/default paths. Pass `TraceHtmlExportOptions` to the corresponding option-bearing `export(...)`, `exportTo(...)`, `exportToDefault(...)`, `exportSuite(...)`, `exportSuiteTo(...)`, or `exportSuiteToDefault(...)` overload. Options control title, embedded JSON, artifacts, stack traces, attributes, grouping/summary sections, previews, compact mode, theme, and maximum message length.

Suite status uses one shared aggregation rule for HTML, JSON, and portable bundles: failed/error state first, then warnings, then an unfinished `STARTED` session, followed by empty `INFO`, all-skipped `SKIPPED`, and finally `PASSED` for completed passed/skipped suites containing at least one pass. A `STARTED` suite is an incomplete diagnostic snapshot and is never presented as passed. HTML shows a `Started / incomplete` count and warning; JSON exposes the additive `summary.started` count. An unfinished session has no `endedAt` or completed `durationMs` in its suite entry.

Exporting is observational. It does not finish a session, add `SESSION_FINISHED`, set `finishedAt`, invent a failure, or wait for completion. The same session can be finalized later, after which a new export reflects its terminal outcome.

`TraceHtmlReportSection` names the renderer's logical `HEADER`, `SUMMARY`, `TIMELINE`, `STEPS`, `FAILURES`, `ARTIFACTS`, and `RAW_JSON` sections. It is useful when an integration needs to identify report sections; section presence in normal exports is controlled by `TraceHtmlExportOptions` rather than by passing this enum to the exporter constructor.

### TraceJsonExporter

Exports equivalent trace data as JSON with `TraceJsonExportOptions`. Default suite JSON is `target/ui-test-lens-report/report.json`.

### TraceReportBundleExporter

Creates a report directory/ZIP for one or many sessions. `TraceBundleExportOptions` controls stack traces, artifact metadata, missing artifacts, copying artifacts, bundle name, output directory, and HTML theme. Default suite targets include `target/ui-test-lens-report/index.html` and `ui-test-lens-report.zip`.

When screenshot artifact metadata is included, reports preserve the completed mode, dimensions, and tile count. The report exporter does not recapture pixels; full-page stitching occurs only in the Selenium evidence producer.

```java
List<UiTestLensSession> sessions = List.of(firstSession, secondSession);
Path html = new TraceHtmlExporter().exportSuiteToDefault(sessions);
Path json = new TraceJsonExporter().exportSuiteToDefault(sessions);
Path zip = new TraceReportBundleExporter().exportSuiteToDefault(sessions);
```

Each exporter provides string/default-path and explicit-path method families; option-bearing overloads accept the corresponding immutable options type. The [binary catalog](../reference/public-api-catalog.md) records every overload, while [configuration](../reference/configuration.md#trace-and-report-options) explains their behavior.

## Result paths

Never assume an automatic output exists solely because finalization returned. Check nullable paths and `diagnosticFailures()`. Bundle/export methods can throw I/O-related runtime failures; callers decide whether report failure should fail a test/build.

## Security

HTML/JSON/ZIP diagnostic text is written from already-redacted logger/session data, and failure-bundle collectors apply the same effective policy at their write boundary. Complete JSON documents use structural, escape-aware redaction; malformed fragments use a fail-closed tolerant fallback. This protects recognized structured secrets and configured literals, including with new sinks/exporters. It is not arbitrary personal-data detection: screenshots/video are unchanged and optional DOM/console processing is best effort. Review artifacts before publishing them and see [Sensitive-data redaction](../security/redaction.md).

Failure records preserve the original exception class in their structural `exceptionType` field. This does not expose the original throwable to a report or sink: the persisted message, cause, suppressed failures, and stack representation come from the redacted diagnostic copy. Internal wrapper class names are implementation details and are not reported as the source failure type.
