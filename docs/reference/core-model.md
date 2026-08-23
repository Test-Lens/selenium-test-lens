# Core model

The public core module provides structured [trace](../observability/trace.md) and [logging](../observability/logging.md) models without requiring Selenium. Direct model construction/export is advanced; normal facade users receive populated sessions/results.

User-relevant types include `UiTestLensSession`, `TraceMetadata`, `TraceTimeline`, `TraceEvent`, `TraceFailure`, `TraceArtifact`, `TraceStep`, logging entries/targets, sinks, and exporters. Builder factories, enum constants, record accessors, and all overloads are enumerated in the [public API catalog](public-api-catalog.md).

`BrowserScriptExecutor`, JSON/HTML writer/support/escaper helpers, and resource loading utilities are formal public API but internal-style infrastructure; see [Advanced and low-level API](advanced-low-level.md).
