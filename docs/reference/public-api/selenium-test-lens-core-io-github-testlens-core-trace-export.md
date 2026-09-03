---
search:
  exclude: true
---

# selenium-test-lens-core: `io.github.testlens.core.trace.export`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.core.trace.export.HtmlReportTheme` {#io-github-testlens-core-trace-export-htmlreporttheme}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace.export`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.core.trace.export.HtmlReportTheme LIGHT
public static final io.github.testlens.core.trace.export.HtmlReportTheme DARK
public static final io.github.testlens.core.trace.export.HtmlReportTheme AUTO
public static io.github.testlens.core.trace.export.HtmlReportTheme[] values()
public static io.github.testlens.core.trace.export.HtmlReportTheme valueOf(java.lang.String)
```

## `io.github.testlens.core.trace.export.TraceBundleExportOptions$Builder` {#io-github-testlens-core-trace-export-tracebundleexportoptions-builder}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace.export`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.core.trace.export.TraceBundleExportOptions$Builder includeStackTraces(boolean)
public io.github.testlens.core.trace.export.TraceBundleExportOptions$Builder includeArtifactMetadata(boolean)
public io.github.testlens.core.trace.export.TraceBundleExportOptions$Builder includeMissingArtifacts(boolean)
public io.github.testlens.core.trace.export.TraceBundleExportOptions$Builder copyArtifacts(boolean)
public io.github.testlens.core.trace.export.TraceBundleExportOptions$Builder bundleName(java.lang.String)
public io.github.testlens.core.trace.export.TraceBundleExportOptions$Builder outputDirectory(java.nio.file.Path)
public io.github.testlens.core.trace.export.TraceBundleExportOptions$Builder htmlTheme(io.github.testlens.core.trace.export.HtmlReportTheme)
public io.github.testlens.core.trace.export.TraceBundleExportOptions build()
```

## `io.github.testlens.core.trace.export.TraceBundleExportOptions` {#io-github-testlens-core-trace-export-tracebundleexportoptions}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace.export`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.core.trace.export.TraceBundleExportOptions defaults()
public static io.github.testlens.core.trace.export.TraceBundleExportOptions$Builder builder()
public boolean includeStackTraces()
public boolean includeArtifactMetadata()
public boolean includeMissingArtifacts()
public boolean copyArtifacts()
public java.lang.String bundleName()
public java.nio.file.Path outputDirectory()
public io.github.testlens.core.trace.export.HtmlReportTheme htmlTheme()
```

## `io.github.testlens.core.trace.export.TraceHtmlExportOptions$Builder` {#io-github-testlens-core-trace-export-tracehtmlexportoptions-builder}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace.export`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.core.trace.export.TraceHtmlExportOptions$Builder title(java.lang.String)
public io.github.testlens.core.trace.export.TraceHtmlExportOptions$Builder includeJsonPayload(boolean)
public io.github.testlens.core.trace.export.TraceHtmlExportOptions$Builder includeArtifacts(boolean)
public io.github.testlens.core.trace.export.TraceHtmlExportOptions$Builder includeStackTraces(boolean)
public io.github.testlens.core.trace.export.TraceHtmlExportOptions$Builder includeAttributes(boolean)
public io.github.testlens.core.trace.export.TraceHtmlExportOptions$Builder collapsePassedEvents(boolean)
public io.github.testlens.core.trace.export.TraceHtmlExportOptions$Builder groupTimelineByCategory(boolean)
public io.github.testlens.core.trace.export.TraceHtmlExportOptions$Builder includeEventTypeSummary(boolean)
public io.github.testlens.core.trace.export.TraceHtmlExportOptions$Builder includeFailureSummary(boolean)
public io.github.testlens.core.trace.export.TraceHtmlExportOptions$Builder includeArtifactPreview(boolean)
public io.github.testlens.core.trace.export.TraceHtmlExportOptions$Builder includeDurationSummary(boolean)
public io.github.testlens.core.trace.export.TraceHtmlExportOptions$Builder compactTimeline(boolean)
public io.github.testlens.core.trace.export.TraceHtmlExportOptions$Builder theme(io.github.testlens.core.trace.export.HtmlReportTheme)
public io.github.testlens.core.trace.export.TraceHtmlExportOptions$Builder maxMessageLength(int)
public io.github.testlens.core.trace.export.TraceHtmlExportOptions build()
```

## `io.github.testlens.core.trace.export.TraceHtmlExportOptions` {#io-github-testlens-core-trace-export-tracehtmlexportoptions}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace.export`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.core.trace.export.TraceHtmlExportOptions defaults()
public static io.github.testlens.core.trace.export.TraceHtmlExportOptions$Builder builder()
public java.lang.String title()
public boolean includeJsonPayload()
public boolean includeArtifacts()
public boolean includeStackTraces()
public boolean includeAttributes()
public boolean collapsePassedEvents()
public boolean groupTimelineByCategory()
public boolean includeEventTypeSummary()
public boolean includeFailureSummary()
public boolean includeArtifactPreview()
public boolean includeDurationSummary()
public boolean compactTimeline()
public io.github.testlens.core.trace.export.HtmlReportTheme theme()
public int maxMessageLength()
```

## `io.github.testlens.core.trace.export.TraceHtmlExporter` {#io-github-testlens-core-trace-export-tracehtmlexporter}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace.export`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static final java.nio.file.Path DEFAULT_OUTPUT_PATH
public io.github.testlens.core.trace.export.TraceHtmlExporter()
public java.lang.String export(io.github.testlens.core.trace.UiTestLensSession)
public java.lang.String export(io.github.testlens.core.trace.UiTestLensSession, io.github.testlens.core.trace.export.TraceHtmlExportOptions)
public java.nio.file.Path exportTo(io.github.testlens.core.trace.UiTestLensSession, java.nio.file.Path)
public java.nio.file.Path exportTo(io.github.testlens.core.trace.UiTestLensSession, java.nio.file.Path, io.github.testlens.core.trace.export.TraceHtmlExportOptions)
public java.nio.file.Path exportToDefault(io.github.testlens.core.trace.UiTestLensSession)
public java.nio.file.Path exportToDefault(io.github.testlens.core.trace.UiTestLensSession, io.github.testlens.core.trace.export.TraceHtmlExportOptions)
public java.lang.String exportSuite(java.util.List<io.github.testlens.core.trace.UiTestLensSession>)
public java.lang.String exportSuite(java.util.List<io.github.testlens.core.trace.UiTestLensSession>, io.github.testlens.core.trace.export.TraceHtmlExportOptions)
public java.nio.file.Path exportSuiteTo(java.util.List<io.github.testlens.core.trace.UiTestLensSession>, java.nio.file.Path)
public java.nio.file.Path exportSuiteTo(java.util.List<io.github.testlens.core.trace.UiTestLensSession>, java.nio.file.Path, io.github.testlens.core.trace.export.TraceHtmlExportOptions)
public java.nio.file.Path exportSuiteToDefault(java.util.List<io.github.testlens.core.trace.UiTestLensSession>)
public java.nio.file.Path exportSuiteToDefault(java.util.List<io.github.testlens.core.trace.UiTestLensSession>, io.github.testlens.core.trace.export.TraceHtmlExportOptions)
```

## `io.github.testlens.core.trace.export.TraceHtmlReportSection` {#io-github-testlens-core-trace-export-tracehtmlreportsection}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace.export`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.core.trace.export.TraceHtmlReportSection HEADER
public static final io.github.testlens.core.trace.export.TraceHtmlReportSection SUMMARY
public static final io.github.testlens.core.trace.export.TraceHtmlReportSection TIMELINE
public static final io.github.testlens.core.trace.export.TraceHtmlReportSection STEPS
public static final io.github.testlens.core.trace.export.TraceHtmlReportSection FAILURES
public static final io.github.testlens.core.trace.export.TraceHtmlReportSection ARTIFACTS
public static final io.github.testlens.core.trace.export.TraceHtmlReportSection RAW_JSON
public static io.github.testlens.core.trace.export.TraceHtmlReportSection[] values()
public static io.github.testlens.core.trace.export.TraceHtmlReportSection valueOf(java.lang.String)
```

## `io.github.testlens.core.trace.export.TraceJsonWriter` {#io-github-testlens-core-trace-export-tracejsonwriter}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace.export`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public static java.lang.String write(java.lang.Object)
public static java.lang.String escape(java.lang.String)
```

## `io.github.testlens.core.trace.export.TraceReportBundleExporter` {#io-github-testlens-core-trace-export-tracereportbundleexporter}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace.export`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static final java.nio.file.Path DEFAULT_OUTPUT_PATH
public io.github.testlens.core.trace.export.TraceReportBundleExporter()
public java.nio.file.Path exportSuite(java.util.List<io.github.testlens.core.trace.UiTestLensSession>)
public java.nio.file.Path exportSuite(java.util.List<io.github.testlens.core.trace.UiTestLensSession>, io.github.testlens.core.trace.export.TraceBundleExportOptions)
public java.nio.file.Path exportSuiteToDefault(java.util.List<io.github.testlens.core.trace.UiTestLensSession>)
public java.nio.file.Path exportSuiteToDefault(java.util.List<io.github.testlens.core.trace.UiTestLensSession>, io.github.testlens.core.trace.export.TraceBundleExportOptions)
public java.nio.file.Path exportSuiteTo(java.util.List<io.github.testlens.core.trace.UiTestLensSession>, java.nio.file.Path)
public java.nio.file.Path exportSuiteTo(java.util.List<io.github.testlens.core.trace.UiTestLensSession>, java.nio.file.Path, io.github.testlens.core.trace.export.TraceBundleExportOptions)
public java.nio.file.Path export(io.github.testlens.core.trace.UiTestLensSession)
public java.nio.file.Path exportTo(io.github.testlens.core.trace.UiTestLensSession, java.nio.file.Path)
```

## `io.github.testlens.core.trace.export.TraceReportSupport` {#io-github-testlens-core-trace-export-tracereportsupport}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.trace.export`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public static final java.lang.String SCHEMA_VERSION
public static final java.nio.file.Path DEFAULT_REPORT_DIRECTORY
public static final java.nio.file.Path DEFAULT_SUITE_JSON_PATH
public static final java.nio.file.Path DEFAULT_BUNDLE_PATH
public static java.util.List<io.github.testlens.core.trace.UiTestLensSession> safeSessions(java.util.List<io.github.testlens.core.trace.UiTestLensSession>)
public static io.github.testlens.core.trace.TraceStatus suiteStatus(java.util.List<io.github.testlens.core.trace.UiTestLensSession>)
public static io.github.testlens.core.trace.TraceStatus sessionStatus(io.github.testlens.core.trace.UiTestLensSession)
public static boolean hasWarning(io.github.testlens.core.trace.UiTestLensSession)
public static boolean isFailedOrError(io.github.testlens.core.trace.TraceEvent)
public static boolean isFailedOrErrorStatus(io.github.testlens.core.trace.TraceStatus)
public static long failureCount(io.github.testlens.core.trace.UiTestLensSession)
public static java.time.Duration sessionDuration(io.github.testlens.core.trace.UiTestLensSession)
public static java.time.Duration totalSessionDuration(java.util.List<io.github.testlens.core.trace.UiTestLensSession>)
public static long screenshotCount(io.github.testlens.core.trace.UiTestLensSession)
public static long screenshotCount(java.util.List<io.github.testlens.core.trace.UiTestLensSession>)
public static long artifactCount(java.util.List<io.github.testlens.core.trace.UiTestLensSession>)
public static long eventCount(java.util.List<io.github.testlens.core.trace.UiTestLensSession>)
public static java.lang.String safeFileName(java.lang.String, java.lang.String)
public static java.lang.String sessionAnchor(io.github.testlens.core.trace.UiTestLensSession)
public static java.nio.file.Path artifactPath(io.github.testlens.core.trace.TraceArtifact)
public static java.nio.file.Path absoluteArtifactPath(io.github.testlens.core.trace.TraceArtifact)
public static boolean artifactExists(io.github.testlens.core.trace.TraceArtifact)
public static long artifactSizeBytes(io.github.testlens.core.trace.TraceArtifact)
public static java.lang.String relativeArtifactPath(io.github.testlens.core.trace.TraceArtifact, java.nio.file.Path)
```
