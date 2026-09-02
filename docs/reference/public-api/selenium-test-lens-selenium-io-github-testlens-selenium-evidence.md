---
search:
  exclude: true
---

# selenium-test-lens-selenium: `io.github.testlens.selenium.evidence`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.selenium.evidence.FailureBundleOptions$Builder` {#io-github-testlens-selenium-evidence-failurebundleoptions-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.evidence`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/failure-bundles.md](../../observability/failure-bundles.md)

```java
public io.github.testlens.selenium.evidence.FailureBundleOptions$Builder enabled(boolean)
public io.github.testlens.selenium.evidence.FailureBundleOptions$Builder diagnosticScreenshot(boolean)
public io.github.testlens.selenium.evidence.FailureBundleOptions$Builder cleanScreenshot(boolean)
public io.github.testlens.selenium.evidence.FailureBundleOptions$Builder context(boolean)
public io.github.testlens.selenium.evidence.FailureBundleOptions$Builder diagnostics(boolean)
public io.github.testlens.selenium.evidence.FailureBundleOptions$Builder pageSource(boolean)
public io.github.testlens.selenium.evidence.FailureBundleOptions$Builder browserConsole(boolean)
public io.github.testlens.selenium.evidence.FailureBundleOptions$Builder networkSummary(boolean)
public io.github.testlens.selenium.evidence.FailureBundleOptions$Builder runtimeMetadata(boolean)
public io.github.testlens.selenium.evidence.FailureBundleOptions$Builder configurationSnapshot(boolean)
public io.github.testlens.selenium.evidence.FailureBundleOptions$Builder zipArchive(boolean)
public io.github.testlens.selenium.evidence.FailureBundleOptions$Builder maxTextArtifactBytes(long)
public io.github.testlens.selenium.evidence.FailureBundleOptions$Builder maxConsoleEntries(int)
public io.github.testlens.selenium.evidence.FailureBundleOptions build()
```

## `io.github.testlens.selenium.evidence.FailureBundleOptions` {#io-github-testlens-selenium-evidence-failurebundleoptions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.evidence`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/failure-bundles.md](../../observability/failure-bundles.md)

```java
public static final long DEFAULT_MAX_TEXT_ARTIFACT_BYTES
public static final int DEFAULT_MAX_CONSOLE_ENTRIES
public static io.github.testlens.selenium.evidence.FailureBundleOptions defaults()
public static io.github.testlens.selenium.evidence.FailureBundleOptions complete()
public static io.github.testlens.selenium.evidence.FailureBundleOptions$Builder builder()
public boolean enabled()
public boolean diagnosticScreenshot()
public boolean cleanScreenshot()
public boolean context()
public boolean diagnostics()
public boolean pageSource()
public boolean browserConsole()
public boolean networkSummary()
public boolean runtimeMetadata()
public boolean configurationSnapshot()
public boolean zipArchive()
public long maxTextArtifactBytes()
public int maxConsoleEntries()
```

## `io.github.testlens.selenium.evidence.ScreenshotCapture` {#io-github-testlens-selenium-evidence-screenshotcapture}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.evidence`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.evidence.ScreenshotCapture(org.openqa.selenium.WebDriver)
public io.github.testlens.selenium.evidence.ScreenshotCaptureResult capture(java.lang.String, io.github.testlens.selenium.evidence.ScreenshotCaptureOptions)
public io.github.testlens.selenium.evidence.ScreenshotCaptureResult capture(java.lang.String, io.github.testlens.selenium.evidence.ScreenshotCaptureOptions, io.github.testlens.core.trace.UiTestLensSession)
```

## `io.github.testlens.selenium.evidence.ScreenshotCaptureException` {#io-github-testlens-selenium-evidence-screenshotcaptureexception}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.evidence`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.evidence.ScreenshotCaptureException(java.lang.String, java.lang.Throwable)
```

## `io.github.testlens.selenium.evidence.ScreenshotCaptureOptions$Builder` {#io-github-testlens-selenium-evidence-screenshotcaptureoptions-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.evidence`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.evidence.ScreenshotCaptureOptions$Builder outputDirectory(java.nio.file.Path)
public io.github.testlens.selenium.evidence.ScreenshotCaptureOptions$Builder fileNamePrefix(java.lang.String)
public io.github.testlens.selenium.evidence.ScreenshotCaptureOptions$Builder includeTimestamp(boolean)
public io.github.testlens.selenium.evidence.ScreenshotCaptureOptions$Builder overwriteExisting(boolean)
public io.github.testlens.selenium.evidence.ScreenshotCaptureOptions$Builder attachToSession(boolean)
public io.github.testlens.selenium.evidence.ScreenshotCaptureOptions build()
```

## `io.github.testlens.selenium.evidence.ScreenshotCaptureOptions` {#io-github-testlens-selenium-evidence-screenshotcaptureoptions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.evidence`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.evidence.ScreenshotCaptureOptions defaults()
public static io.github.testlens.selenium.evidence.ScreenshotCaptureOptions$Builder builder()
public java.nio.file.Path outputDirectory()
public java.lang.String fileNamePrefix()
public boolean includeTimestamp()
public boolean overwriteExisting()
public boolean attachToSession()
```

## `io.github.testlens.selenium.evidence.ScreenshotCaptureResult` {#io-github-testlens-selenium-evidence-screenshotcaptureresult}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.evidence`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.evidence.ScreenshotCaptureResult captured(java.lang.String, java.nio.file.Path, io.github.testlens.core.trace.TraceArtifact, java.lang.String)
public static io.github.testlens.selenium.evidence.ScreenshotCaptureResult failed(java.lang.String, java.nio.file.Path, java.lang.String, java.lang.Throwable)
public static io.github.testlens.selenium.evidence.ScreenshotCaptureResult skipped(java.lang.String, java.lang.String)
public io.github.testlens.selenium.evidence.ScreenshotCaptureStatus status()
public java.lang.String name()
public java.nio.file.Path path()
public io.github.testlens.core.trace.TraceArtifact artifact()
public java.lang.String message()
public java.lang.Throwable exception()
public java.time.Instant capturedAt()
public boolean isCaptured()
```

## `io.github.testlens.selenium.evidence.ScreenshotCaptureStatus` {#io-github-testlens-selenium-evidence-screenshotcapturestatus}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.evidence`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.evidence.ScreenshotCaptureStatus CAPTURED
public static final io.github.testlens.selenium.evidence.ScreenshotCaptureStatus FAILED
public static final io.github.testlens.selenium.evidence.ScreenshotCaptureStatus SKIPPED
public static io.github.testlens.selenium.evidence.ScreenshotCaptureStatus[] values()
public static io.github.testlens.selenium.evidence.ScreenshotCaptureStatus valueOf(java.lang.String)
```

## `io.github.testlens.selenium.evidence.VideoEvidence` {#io-github-testlens-selenium-evidence-videoevidence}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.evidence`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.evidence.VideoEvidence()
public io.github.testlens.selenium.evidence.VideoEvidenceResult attachFile(java.lang.String, java.nio.file.Path, io.github.testlens.selenium.evidence.VideoEvidenceOptions, io.github.testlens.core.trace.UiTestLensSession)
public io.github.testlens.selenium.evidence.VideoEvidenceResult attachUrl(java.lang.String, java.lang.String, io.github.testlens.selenium.evidence.VideoEvidenceOptions, io.github.testlens.core.trace.UiTestLensSession)
```

## `io.github.testlens.selenium.evidence.VideoEvidenceException` {#io-github-testlens-selenium-evidence-videoevidenceexception}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.evidence`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.evidence.VideoEvidenceException(java.lang.String)
public io.github.testlens.selenium.evidence.VideoEvidenceException(java.lang.String, java.lang.Throwable)
```

## `io.github.testlens.selenium.evidence.VideoEvidenceOptions$Builder` {#io-github-testlens-selenium-evidence-videoevidenceoptions-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.evidence`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.evidence.VideoEvidenceOptions$Builder source(io.github.testlens.selenium.evidence.VideoEvidenceSource)
public io.github.testlens.selenium.evidence.VideoEvidenceOptions$Builder mediaType(java.lang.String)
public io.github.testlens.selenium.evidence.VideoEvidenceOptions$Builder validateLocalFileExists(boolean)
public io.github.testlens.selenium.evidence.VideoEvidenceOptions$Builder attachToSession(boolean)
public io.github.testlens.selenium.evidence.VideoEvidenceOptions$Builder metadata(java.lang.String, java.lang.String)
public io.github.testlens.selenium.evidence.VideoEvidenceOptions$Builder metadata(java.util.Map<java.lang.String, java.lang.String>)
public io.github.testlens.selenium.evidence.VideoEvidenceOptions build()
```

## `io.github.testlens.selenium.evidence.VideoEvidenceOptions` {#io-github-testlens-selenium-evidence-videoevidenceoptions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.evidence`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.evidence.VideoEvidenceOptions defaults()
public static io.github.testlens.selenium.evidence.VideoEvidenceOptions$Builder builder()
public io.github.testlens.selenium.evidence.VideoEvidenceSource source()
public java.lang.String mediaType()
public boolean validateLocalFileExists()
public boolean attachToSession()
public java.util.Map<java.lang.String, java.lang.String> metadata()
```

## `io.github.testlens.selenium.evidence.VideoEvidenceResult` {#io-github-testlens-selenium-evidence-videoevidenceresult}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.evidence`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.evidence.VideoEvidenceResult attached(java.lang.String, java.nio.file.Path, java.lang.String, io.github.testlens.core.trace.TraceArtifact, io.github.testlens.selenium.evidence.VideoEvidenceSource, java.lang.String, java.util.Map<java.lang.String, java.lang.String>)
public static io.github.testlens.selenium.evidence.VideoEvidenceResult failed(java.lang.String, java.nio.file.Path, java.lang.String, io.github.testlens.selenium.evidence.VideoEvidenceSource, java.lang.String, java.lang.Throwable, java.util.Map<java.lang.String, java.lang.String>)
public static io.github.testlens.selenium.evidence.VideoEvidenceResult skipped(java.lang.String, java.nio.file.Path, java.lang.String, io.github.testlens.selenium.evidence.VideoEvidenceSource, java.lang.String, java.util.Map<java.lang.String, java.lang.String>)
public io.github.testlens.selenium.evidence.VideoEvidenceStatus status()
public java.lang.String name()
public java.nio.file.Path path()
public java.lang.String url()
public io.github.testlens.core.trace.TraceArtifact artifact()
public io.github.testlens.selenium.evidence.VideoEvidenceSource source()
public java.lang.String message()
public java.lang.Throwable exception()
public java.time.Instant attachedAt()
public java.util.Map<java.lang.String, java.lang.String> metadata()
public boolean isAttached()
```

## `io.github.testlens.selenium.evidence.VideoEvidenceSource` {#io-github-testlens-selenium-evidence-videoevidencesource}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.evidence`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.evidence.VideoEvidenceSource LOCAL_FILE
public static final io.github.testlens.selenium.evidence.VideoEvidenceSource REMOTE_URL
public static final io.github.testlens.selenium.evidence.VideoEvidenceSource SELENIUM_GRID
public static final io.github.testlens.selenium.evidence.VideoEvidenceSource SELENOID
public static final io.github.testlens.selenium.evidence.VideoEvidenceSource BROWSERSTACK
public static final io.github.testlens.selenium.evidence.VideoEvidenceSource SAUCE_LABS
public static final io.github.testlens.selenium.evidence.VideoEvidenceSource CI_ARTIFACT
public static final io.github.testlens.selenium.evidence.VideoEvidenceSource CUSTOM
public static io.github.testlens.selenium.evidence.VideoEvidenceSource[] values()
public static io.github.testlens.selenium.evidence.VideoEvidenceSource valueOf(java.lang.String)
```

## `io.github.testlens.selenium.evidence.VideoEvidenceStatus` {#io-github-testlens-selenium-evidence-videoevidencestatus}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.evidence`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.evidence.VideoEvidenceStatus ATTACHED
public static final io.github.testlens.selenium.evidence.VideoEvidenceStatus FAILED
public static final io.github.testlens.selenium.evidence.VideoEvidenceStatus SKIPPED
public static io.github.testlens.selenium.evidence.VideoEvidenceStatus[] values()
public static io.github.testlens.selenium.evidence.VideoEvidenceStatus valueOf(java.lang.String)
```
