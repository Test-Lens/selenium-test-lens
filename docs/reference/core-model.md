# Core model

`RedactionPolicy` is the immutable core security boundary for diagnostic strings. Its builder adds literal sensitive keys and exact secret values without accepting regular expressions or exposing configured secrets. `UiTestLensLogger.Builder.redactionPolicy(...)` applies it before all sink fan-out, while direct session storage uses the same policy independently. See [Sensitive-data redaction](../security/redaction.md).

The public core module provides structured [trace](../observability/trace.md) and [logging](../observability/logging.md) models without requiring Selenium. Direct model construction/export is advanced; normal facade users receive populated sessions/results.

User-relevant types include `UiTestLensSession`, `TraceMetadata`, `TraceTimeline`, `TraceEvent`, `TraceFailure`, `TraceArtifact`, `TraceStep`, logging entries/targets, sinks, and exporters. Builder factories, enum constants, record accessors, and all overloads are enumerated in the [public API catalog](public-api-catalog.md).

`BrowserScriptExecutor` is a supported low-level, framework-neutral SPI used to execute overlay scripts. JSON/report support and resource loading utilities remain internal-style public only where current package or Maven boundaries require them; the HTML escaper is now implementation-private. See [Advanced and low-level API](advanced-low-level.md).
