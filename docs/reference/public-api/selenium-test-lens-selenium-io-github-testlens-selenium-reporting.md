---
search:
  exclude: true
---

# selenium-test-lens-selenium: `io.github.testlens.selenium.reporting`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.selenium.reporting.ReportArtifactKind` {#io-github-testlens-selenium-reporting-reportartifactkind}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.reporting`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/observability/report-upload.md](../../observability/report-upload.md)

```java
public static final io.github.testlens.selenium.reporting.ReportArtifactKind SESSION_REPORT
public static final io.github.testlens.selenium.reporting.ReportArtifactKind FAILURE_BUNDLE
public static io.github.testlens.selenium.reporting.ReportArtifactKind[] values()
public static io.github.testlens.selenium.reporting.ReportArtifactKind valueOf(java.lang.String)
```

## `io.github.testlens.selenium.reporting.ReportProxyMode` {#io-github-testlens-selenium-reporting-reportproxymode}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.reporting`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/observability/report-upload.md](../../observability/report-upload.md)

```java
public static final io.github.testlens.selenium.reporting.ReportProxyMode DIRECT
public static final io.github.testlens.selenium.reporting.ReportProxyMode SYSTEM
public static final io.github.testlens.selenium.reporting.ReportProxyMode EXPLICIT
public static io.github.testlens.selenium.reporting.ReportProxyMode[] values()
public static io.github.testlens.selenium.reporting.ReportProxyMode valueOf(java.lang.String)
```

## `io.github.testlens.selenium.reporting.ReportProxyOptions$Builder` {#io-github-testlens-selenium-reporting-reportproxyoptions-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.reporting`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/report-upload.md](../../observability/report-upload.md)

```java
public io.github.testlens.selenium.reporting.ReportProxyOptions$Builder mode(io.github.testlens.selenium.reporting.ReportProxyMode)
public io.github.testlens.selenium.reporting.ReportProxyOptions$Builder host(java.lang.String)
public io.github.testlens.selenium.reporting.ReportProxyOptions$Builder port(int)
public io.github.testlens.selenium.reporting.ReportProxyOptions$Builder noProxy(java.lang.String)
public io.github.testlens.selenium.reporting.ReportProxyOptions build()
```

## `io.github.testlens.selenium.reporting.ReportProxyOptions` {#io-github-testlens-selenium-reporting-reportproxyoptions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.reporting`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/report-upload.md](../../observability/report-upload.md)

```java
public static io.github.testlens.selenium.reporting.ReportProxyOptions system()
public static io.github.testlens.selenium.reporting.ReportProxyOptions direct()
public static io.github.testlens.selenium.reporting.ReportProxyOptions$Builder builder()
public java.lang.String toString()
```

## `io.github.testlens.selenium.reporting.ReportUploadException` {#io-github-testlens-selenium-reporting-reportuploadexception}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.reporting`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/report-upload.md](../../observability/report-upload.md)

```java
public io.github.testlens.selenium.reporting.ReportUploadResult result()
```

## `io.github.testlens.selenium.reporting.ReportUploadFailureCategory` {#io-github-testlens-selenium-reporting-reportuploadfailurecategory}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.reporting`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/observability/report-upload.md](../../observability/report-upload.md)

```java
public static final io.github.testlens.selenium.reporting.ReportUploadFailureCategory NONE
public static final io.github.testlens.selenium.reporting.ReportUploadFailureCategory INVALID_ARTIFACT
public static final io.github.testlens.selenium.reporting.ReportUploadFailureCategory PAYLOAD_TOO_LARGE
public static final io.github.testlens.selenium.reporting.ReportUploadFailureCategory AUTHENTICATION
public static final io.github.testlens.selenium.reporting.ReportUploadFailureCategory REJECTED_REQUEST
public static final io.github.testlens.selenium.reporting.ReportUploadFailureCategory RATE_LIMITED
public static final io.github.testlens.selenium.reporting.ReportUploadFailureCategory SERVER_ERROR
public static final io.github.testlens.selenium.reporting.ReportUploadFailureCategory TIMEOUT
public static final io.github.testlens.selenium.reporting.ReportUploadFailureCategory TRANSPORT
public static final io.github.testlens.selenium.reporting.ReportUploadFailureCategory INVALID_RESPONSE
public static final io.github.testlens.selenium.reporting.ReportUploadFailureCategory PROXY_CONFIGURATION
public static io.github.testlens.selenium.reporting.ReportUploadFailureCategory[] values()
public static io.github.testlens.selenium.reporting.ReportUploadFailureCategory valueOf(java.lang.String)
```

## `io.github.testlens.selenium.reporting.ReportUploadOptions$Builder` {#io-github-testlens-selenium-reporting-reportuploadoptions-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.reporting`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/report-upload.md](../../observability/report-upload.md)

```java
public io.github.testlens.selenium.reporting.ReportUploadOptions$Builder endpoint(java.net.URI)
public io.github.testlens.selenium.reporting.ReportUploadOptions$Builder bearerToken(java.lang.String)
public io.github.testlens.selenium.reporting.ReportUploadOptions$Builder header(java.lang.String, java.lang.String)
public io.github.testlens.selenium.reporting.ReportUploadOptions$Builder connectTimeout(java.time.Duration)
public io.github.testlens.selenium.reporting.ReportUploadOptions$Builder requestTimeout(java.time.Duration)
public io.github.testlens.selenium.reporting.ReportUploadOptions$Builder maxPayloadBytes(long)
public io.github.testlens.selenium.reporting.ReportUploadOptions$Builder maxAttempts(int)
public io.github.testlens.selenium.reporting.ReportUploadOptions$Builder maxRetryAfter(java.time.Duration)
public io.github.testlens.selenium.reporting.ReportUploadOptions$Builder maxResponsePreviewBytes(int)
public io.github.testlens.selenium.reporting.ReportUploadOptions$Builder proxy(io.github.testlens.selenium.reporting.ReportProxyOptions)
public io.github.testlens.selenium.reporting.ReportUploadOptions$Builder redactionPolicy(io.github.testlens.core.redaction.RedactionPolicy)
public io.github.testlens.selenium.reporting.ReportUploadOptions build()
```

## `io.github.testlens.selenium.reporting.ReportUploadOptions` {#io-github-testlens-selenium-reporting-reportuploadoptions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.reporting`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/report-upload.md](../../observability/report-upload.md)

```java
public static final long DEFAULT_MAX_PAYLOAD_BYTES
public static final int DEFAULT_MAX_RESPONSE_PREVIEW_BYTES
public static io.github.testlens.selenium.reporting.ReportUploadOptions$Builder builder()
public java.net.URI endpoint()
public java.time.Duration connectTimeout()
public java.time.Duration requestTimeout()
public long maxPayloadBytes()
public int maxAttempts()
public java.time.Duration maxRetryAfter()
public int maxResponsePreviewBytes()
public io.github.testlens.core.redaction.RedactionPolicy redactionPolicy()
public java.lang.String toString()
```

## `io.github.testlens.selenium.reporting.ReportUploadResult` {#io-github-testlens-selenium-reporting-reportuploadresult}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.reporting`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/report-upload.md](../../observability/report-upload.md)

```java
public io.github.testlens.selenium.reporting.ReportUploadStatus status()
public io.github.testlens.selenium.reporting.ReportArtifactKind artifactKind()
public java.lang.String endpoint()
public java.lang.Integer httpStatus()
public int attempts()
public java.time.Duration elapsed()
public long payloadSize()
public java.lang.String sha256()
public java.lang.String serverReportId()
public java.lang.String responsePreview()
public io.github.testlens.selenium.reporting.ReportUploadFailureCategory failureCategory()
public java.lang.Throwable exception()
public java.lang.String message()
public boolean isUploaded()
public io.github.testlens.selenium.reporting.ReportUploadResult requireSuccess()
public java.lang.String toString()
```

## `io.github.testlens.selenium.reporting.ReportUploadStatus` {#io-github-testlens-selenium-reporting-reportuploadstatus}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.reporting`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/observability/report-upload.md](../../observability/report-upload.md)

```java
public static final io.github.testlens.selenium.reporting.ReportUploadStatus UPLOADED
public static final io.github.testlens.selenium.reporting.ReportUploadStatus FAILED
public static final io.github.testlens.selenium.reporting.ReportUploadStatus SKIPPED
public static io.github.testlens.selenium.reporting.ReportUploadStatus[] values()
public static io.github.testlens.selenium.reporting.ReportUploadStatus valueOf(java.lang.String)
```

## `io.github.testlens.selenium.reporting.ReportUploader` {#io-github-testlens-selenium-reporting-reportuploader}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.reporting`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/report-upload.md](../../observability/report-upload.md)

```java
public static final java.lang.String SCHEMA_VERSION
public io.github.testlens.selenium.reporting.ReportUploader(io.github.testlens.selenium.reporting.ReportUploadOptions)
public io.github.testlens.selenium.reporting.ReportUploadResult upload(io.github.testlens.TestLensFinalizationResult)
```
