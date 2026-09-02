# Observability

Observability is a primary Test Lens surface, not a side effect of element helpers.

## Runtime visual diagnostics

[Visual diagnostics](visual-diagnostics.md) covers the HUD, target highlights, wait/assertion feedback, themes, and cleanup. These features decorate the current page and may need reinjection after navigation.

## Persistent artifacts

- [Screenshots and evidence](screenshots-evidence.md)
- [Trace model and JSON](trace.md)
- [HTML, JSON, suite, and ZIP reports](reports.md)
- [Flakiness and retry outcomes](flakiness.md)
- [Structured logging](logging.md)

Artifacts can contain URLs, page text, screenshots, exception stacks, headers, cookies-related state, and caller metadata. Store them as CI artifacts with appropriate access controls; do not commit sensitive output.
