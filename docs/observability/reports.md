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

All three facade finalizers—`finishPassed()`, `finishFailed(Throwable)`, and `finishSkipped(String)`—attempt per-session `trace.json` and `report.html`. The reports retain `PASSED`, `FAILED`, or `SKIPPED` respectively, and a skip reason is stored on the `SESSION_FINISHED` event. Each export is best effort: its `Path` can be null and the error appears in `TestLensFinalizationResult.diagnosticFailures()` without changing the session status.

The session JSON always contains a top-level `flakiness` object with `flakyCandidate`, `totalRetries`, `timeLostMs`, `policy`, `policyTriggered`, `byAction`, `byLocator`, and `byException`. HTML renders the same data neutrally for zero retries, as information for `REPORT_ONLY`, as a warning for `WARN`, and as a failure for a triggered fail policy. The destination is derived from the session output configuration. See [Flakiness and retry outcomes](flakiness.md), [session lifecycle and finalization](../reference/test-lens.md#creation-and-lifecycle), and [`TestLensOptions`](../reference/configuration.md#testlensoptions).

For final `FAILED`, HTML also contains a `Failure bundle` section linking the predictable ZIP and listing every component status, size, path, or collection error. The ZIP is assembled after final trace/report exports, so it contains their final versions without recursively containing itself. See [Failure bundles](failure-bundles.md).

An explicitly attached network JSON export is an object containing the requested and active capture modes, capture status, ignored/dropped counters, and request/response/fetch-error events with correlation attributes. A failed session's bundle contains a smaller `network-summary.json` snapshot taken before Lens stops its active capture; it does not start capture or include request/response bodies.

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

`TraceHtmlReportSection` names the renderer's logical `HEADER`, `SUMMARY`, `TIMELINE`, `STEPS`, `FAILURES`, `ARTIFACTS`, and `RAW_JSON` sections. It is useful when an integration needs to identify report sections; section presence in normal exports is controlled by `TraceHtmlExportOptions` rather than by passing this enum to the exporter constructor.

<!-- SCREENSHOT TODO: assets/screenshots/html-report-overview.png
Show the generated HTML report overview with session status, summaries, and timeline visible.
Use synthetic test names/data and a real exported report.
Feature documented: report-level navigation and summary.
Suggested alt text: Selenium Test Lens HTML report overview with status summaries and timeline.
-->

<!-- SCREENSHOT TODO: assets/screenshots/html-report-failure-detail.png
Show an expanded failed event/step with failure context and an evidence link or preview.
Use a real exported report with sanitized stack paths and application data.
Feature documented: failure investigation inside the HTML report.
Suggested alt text: Expanded failed report event showing diagnostic context and linked evidence.
-->

### TraceJsonExporter

Exports equivalent trace data as JSON with `TraceJsonExportOptions`. Default suite JSON is `target/ui-test-lens-report/report.json`.

### TraceReportBundleExporter

Creates a report directory/ZIP for one or many sessions. `TraceBundleExportOptions` controls stack traces, artifact metadata, missing artifacts, copying artifacts, bundle name, output directory, and HTML theme. Default suite targets include `target/ui-test-lens-report/index.html` and `ui-test-lens-report.zip`.

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

HTML/JSON/ZIP diagnostic text is written from already-redacted logger/session data, and failure-bundle collectors apply the same effective policy at their write boundary. This protects recognized structured secrets and configured literals, including with new sinks/exporters. It is not arbitrary personal-data detection: screenshots/video are unchanged and optional DOM/console processing is best effort. Review artifacts before publishing them and see [Sensitive-data redaction](../security/redaction.md).
