# Observability

Observability is a primary Test Lens surface, not a side effect of element helpers.

After a session is finalized locally, the [report uploader](report-upload.md) can explicitly stream one completed ZIP to an HTTP receiver. Transport is separate from evidence capture: it is synchronous, never automatic, and never owns WebDriver.

The persistent path is `operation → trace → finalization → report`; a final failed outcome can extend it with an automatic evidence bundle. The in-browser HUD is the live view of this pipeline, not its durable source of truth.

Automatic failure bundles and central redaction were added in 0.2.0. Trace, HTML/JSON reports, screenshots, and the HUD originated in 0.1.0.

For a final failed session, the normal trace/report are complemented by an automatic [failure bundle](failure-bundles.md) with a versioned manifest and deterministic ZIP. Bundle collectors are independent and best effort.

Diagnostic text uses a shared, enabled-by-default [redaction policy](../security/redaction.md) before HUD/sink fan-out and at direct trace, network, API-overlay, and bundle write boundaries. Pixel evidence and replayable authentication state have deliberately different security boundaries.

## Runtime visual diagnostics

[Visual diagnostics](visual-diagnostics.md) covers the HUD, target highlights, wait/assertion feedback, themes, and cleanup. These features decorate the current page and may need reinjection after navigation.

## Persistent artifacts

- [Screenshots and evidence](screenshots-evidence.md)
- [Trace model and JSON](trace.md)
- [HTML, JSON, suite, and ZIP reports](reports.md)
- [Flakiness and retry outcomes](flakiness.md)
- [Structured logging](logging.md)

Artifacts can contain URLs, page text, screenshots, exception stacks, headers, cookies-related state, and caller metadata. Store them as CI artifacts with appropriate access controls; do not commit sensitive output.
