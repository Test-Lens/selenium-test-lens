# Trace and reports

**Selenium Test Lens 0.1.0 — stable**

Session actions, waits, assertions, screenshots, and custom logging can become structured `TraceEvent` and `TraceArtifact` entries.

```java
UiTestLensSession session = UiTestLensSession.start("checkout");
session.attachScreenshot("checkout", Path.of("target", "checkout.png"));
session.finishPassed();
session.exportJson(Path.of("target", "trace.json"));
session.exportHtml(Path.of("target", "report.html"));
```

The `TestLens` facade starts and attaches the session for you, then writes final JSON and HTML reports during finalization. Core exporters also support suite JSON/HTML and `TraceReportBundleExporter` ZIP output.

The report ZIP is an export container for trace reports and attached artifacts. It is **not** the automatic failure-evidence bundle added in the development line. Video support in 0.1.0 attaches a file produced by an external recorder; Test Lens does not start video recording.

Treat reports as diagnostic evidence. Version 0.1.0 predates central redaction, hardened exception provenance, incomplete-suite aggregation, and exactly-once facade finalization. Avoid placing secrets in log messages, labels, URLs, attached text, or page content.
