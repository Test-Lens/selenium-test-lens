---
search:
  exclude: true
---

# selenium-test-lens-core: `io.github.testlens.core.logging.export`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.core.logging.export.HtmlLogExporter` {#io-github-testlens-core-logging-export-htmllogexporter}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging.export`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.core.logging.export.HtmlLogExporter()
public io.github.testlens.core.logging.export.HtmlLogExporter(io.github.testlens.core.logging.export.LogExportOptions)
public java.lang.String export(java.util.List<io.github.testlens.core.logging.UiTestLensLogEntry>)
public java.lang.String export(java.util.List<io.github.testlens.core.logging.UiTestLensLogEntry>, io.github.testlens.core.trace.export.TraceHtmlExportOptions)
public java.nio.file.Path exportTo(java.util.List<io.github.testlens.core.logging.UiTestLensLogEntry>, java.nio.file.Path)
public java.nio.file.Path exportTo(java.util.List<io.github.testlens.core.logging.UiTestLensLogEntry>, java.nio.file.Path, io.github.testlens.core.trace.export.TraceHtmlExportOptions)
public java.nio.file.Path exportToDefault(java.util.List<io.github.testlens.core.logging.UiTestLensLogEntry>)
public java.nio.file.Path exportToDefault(java.util.List<io.github.testlens.core.logging.UiTestLensLogEntry>, io.github.testlens.core.trace.export.TraceHtmlExportOptions)
```

## `io.github.testlens.core.logging.export.JsonLogExporter` {#io-github-testlens-core-logging-export-jsonlogexporter}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging.export`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.core.logging.export.JsonLogExporter()
public io.github.testlens.core.logging.export.JsonLogExporter(io.github.testlens.core.logging.export.LogExportOptions)
public java.lang.String export(java.util.List<io.github.testlens.core.logging.UiTestLensLogEntry>)
```

## `io.github.testlens.core.logging.export.LogExportOptions` {#io-github-testlens-core-logging-export-logexportoptions}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging.export`
- Classification: `ADVANCED_API`
- Type kind: `record`

```java
public io.github.testlens.core.logging.export.LogExportOptions(boolean, boolean, boolean, int)
public static io.github.testlens.core.logging.export.LogExportOptions defaults()
public static io.github.testlens.core.logging.export.LogExportOptions compact()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public boolean includeMetadata()
public boolean includeThrowable()
public boolean prettyPrint()
public int maxFieldLength()
```

## `io.github.testlens.core.logging.export.PlainTextLogExporter` {#io-github-testlens-core-logging-export-plaintextlogexporter}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging.export`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.core.logging.export.PlainTextLogExporter()
public io.github.testlens.core.logging.export.PlainTextLogExporter(io.github.testlens.core.logging.export.LogExportOptions)
public java.lang.String export(java.util.List<io.github.testlens.core.logging.UiTestLensLogEntry>)
```

## `io.github.testlens.core.logging.export.UiTestLensLogExporter` {#io-github-testlens-core-logging-export-uitestlenslogexporter}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.logging.export`
- Classification: `ADVANCED_API`
- Type kind: `interface`

```java
public abstract java.lang.String export(java.util.List<io.github.testlens.core.logging.UiTestLensLogEntry>)
```
