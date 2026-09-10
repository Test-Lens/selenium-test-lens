# Report upload

!!! info "Coming in 0.2.0"
    Explicit report upload is part of the current development line and is not available in Maven Central `0.1.0`.

`ReportUploader` sends an already finalized Test Lens report to an HTTP endpoint. Upload is synchronous and explicit: configuring an endpoint or finalizing a session never opens a connection by itself.

```java
TestLensFinalizationResult finalized = lens.finishPassed();

ReportUploadOptions options = ReportUploadOptions.builder()
        .endpoint(URI.create("https://reports.example.test/api/test-lens/reports"))
        .bearerToken(System.getenv("TEST_LENS_REPORT_TOKEN"))
        .build();

ReportUploadResult upload = new ReportUploader(options).upload(finalized);
upload.requireSuccess();

driver.quit();
```

The ordering is deliberately `finish -> upload -> quit`. The uploader needs completed files, not a live driver. It does not finalize again, append trace events, recapture evidence, mutate session status or retry summary, or own `WebDriver`. A failed upload leaves local reports and failure evidence in place. The same finalized result can be uploaded again deliberately.

## Payload selection

Every request contains exactly one ZIP file:

| Finalization output | `X-Test-Lens-Artifact-Kind` | ZIP contents |
| --- | --- | --- |
| completed `failure-bundle.zip` is present | `FAILURE_BUNDLE` | the existing, validated bundle, unchanged |
| otherwise | `SESSION_REPORT` | `manifest.json`, `trace.json`, `report.html`, in that order |

The uploader accepts only a terminal `PASSED`, `FAILED`, or `SKIPPED` session. Required files must be regular, non-symbolic files below the finalization output directory. An existing failure bundle must be a readable ZIP with unique safe relative entry names and `manifest.json`. Missing, partial, oversized, symbolic, or out-of-directory artifacts fail before HTTP is attempted. A temporary session ZIP is deleted in `finally`; an existing failure bundle is never deleted.

The session-report manifest has schema version `1`:

```json
{
  "schemaVersion": "1",
  "testLensVersion": "0.2.0-SNAPSHOT",
  "status": "PASSED",
  "artifactKind": "SESSION_REPORT",
  "createdAt": "2026-01-01T00:00:00Z",
  "artifacts": [
    {"name": "trace.json", "size": 1234, "sha256": "lowercase-hex"},
    {"name": "report.html", "size": 5678, "sha256": "lowercase-hex"}
  ]
}
```

Entry order and names are deterministic. `createdAt` describes package creation, so independently created session-report ZIPs need not be byte-identical. The request checksum covers the exact completed ZIP payload.

## Receiver contract

The endpoint receives:

```http
POST /api/test-lens/reports
Content-Type: application/zip
Authorization: Bearer <optional token>
Idempotency-Key: test-lens-<artifact-kind>-<sha256>
X-Test-Lens-Schema-Version: 1
X-Test-Lens-Artifact-Kind: SESSION_REPORT | FAILURE_BUNDLE
X-Test-Lens-Status: PASSED | FAILED | SKIPPED
X-Test-Lens-SHA256: <lowercase SHA-256 of request body>
Content-Length: <bytes>
```

The body is streamed from disk with JDK `HttpClient.BodyPublishers.ofFile`; it is not buffered as a byte array. Any `2xx` response is success. A receiver may return an opaque identifier in `X-Test-Lens-Report-Id`. Test Lens reads no other response metadata and limits/redacts its response preview.

Failure classification is stable at the client boundary: `401/403` are authentication failures, `413` is too large, `429` is rate limiting, other `4xx` are rejected requests, `5xx` are server failures, request timeout is timeout, and I/O/TLS failures are transport failures. Redirects are never followed, including `307/308`; deploy the final endpoint URI directly.

### Minimal Spring Boot receiver

The example streams into quarantine, enforces a limit while copying, verifies the checksum, and publishes under a server-generated name. Production code should persist idempotency keys atomically in shared storage rather than in process memory.

```java
@RestController
final class TestLensReportController {
    private static final long MAX_BYTES = 100L * 1024 * 1024;
    private final Path quarantine = Path.of("/srv/test-lens/quarantine");
    private final Set<String> acceptedKeys = ConcurrentHashMap.newKeySet();
    private final String expectedToken = System.getenv("TEST_LENS_REPORT_TOKEN");

    @PostMapping(path = "/api/test-lens/reports", consumes = "application/zip")
    ResponseEntity<Void> upload(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-Test-Lens-SHA256") String expectedSha256,
            @RequestHeader("X-Test-Lens-Artifact-Kind") String artifactKind,
            @RequestHeader("X-Test-Lens-Status") String status,
            HttpServletRequest request) throws Exception {
        if (expectedToken == null
                || !MessageDigest.isEqual(
                        ("Bearer " + expectedToken).getBytes(StandardCharsets.UTF_8),
                        authorization.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!Set.of("SESSION_REPORT", "FAILURE_BUNDLE").contains(artifactKind)
                || !Set.of("PASSED", "FAILED", "SKIPPED").contains(status)
                || !expectedSha256.matches("[0-9a-f]{64}")) {
            return ResponseEntity.badRequest().build();
        }
        if (!acceptedKeys.add(idempotencyKey)) {
            return ResponseEntity.noContent().build();
        }

        Files.createDirectories(quarantine);
        Path temporary = Files.createTempFile(quarantine, "incoming-", ".part");
        boolean published = false;
        try (InputStream input = request.getInputStream();
             OutputStream output = Files.newOutputStream(temporary)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            long size = 0;
            for (int read; (read = input.read(buffer)) >= 0; ) {
                if (read == 0) continue;
                size = Math.addExact(size, read);
                if (size > MAX_BYTES) return ResponseEntity.status(413).build();
                digest.update(buffer, 0, read);
                output.write(buffer, 0, read);
            }
            String actual = HexFormat.of().formatHex(digest.digest());
            if (!MessageDigest.isEqual(
                    actual.getBytes(StandardCharsets.US_ASCII),
                    expectedSha256.getBytes(StandardCharsets.US_ASCII))) {
                return ResponseEntity.badRequest().build();
            }
            Path destination = quarantine.resolve(UUID.randomUUID() + ".zip");
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            published = true;
            return ResponseEntity.accepted()
                    .header("X-Test-Lens-Report-Id", destination.getFileName().toString())
                    .build();
        } finally {
            if (!published) {
                Files.deleteIfExists(temporary);
                acceptedKeys.remove(idempotencyKey);
            }
        }
    }
}
```

The receiver must also constrain `Content-Length` before reading, authenticate before accepting bytes, rate-limit callers, use a durable idempotency store, scan quarantined content, and apply retention/access controls. It must not trust a client filename. If it unpacks a ZIP, it must independently enforce entry count, compressed and expanded sizes, nesting, duplicate names, and normalized destination containment to prevent zip-slip and zip bombs. The example deliberately does not unpack the archive.

## Retry and idempotency

The default `maxAttempts` is `1`, so there is no automatic retry. Setting a higher value permits retry only for transport I/O, `408`, `429`, `502`, `503`, and `504`. The same file, checksum, and deterministic `Idempotency-Key` are used for every attempt. `400`, `401`, `403`, `404`, `409`, `413`, redirects, and other terminal `4xx` are never retried.

An integer-seconds `Retry-After` is honored only up to `maxRetryAfter`; invalid or date-form values do not add a delay. Attempts and monotonic elapsed time are reported by `ReportUploadResult`. These are transport attempts and never affect session `RetrySummary` or flakiness policy.

```java
ReportUploadOptions options = ReportUploadOptions.builder()
        .endpoint(endpoint)
        .maxAttempts(3)
        .maxRetryAfter(Duration.ofSeconds(10))
        .build();
```

## Proxy selection

`SYSTEM` is the default and delegates to the JVM's current `ProxySelector`. Standard JVM proxy properties/truststore configuration therefore apply. Test Lens does not promise automatic interpretation of `HTTP_PROXY`, `HTTPS_PROXY`, or `NO_PROXY`; translate those values into the builder explicitly when that is your deployment policy. Browser proxy capabilities are unrelated: the uploader runs in the test JVM and never uses the browser's network stack.

Direct connection, regardless of JVM proxy settings:

```java
.proxy(ReportProxyOptions.direct())
```

Explicit HTTP proxy for HTTP and HTTPS endpoints:

```java
.proxy(ReportProxyOptions.builder()
        .mode(ReportProxyMode.EXPLICIT)
        .host("proxy.corp.example")
        .port(8080)
        .build())
```

Explicit proxy with bypass rules:

```java
.proxy(ReportProxyOptions.builder()
        .mode(ReportProxyMode.EXPLICIT)
        .host("proxy.corp.example")
        .port(8080)
        .noProxy("localhost")
        .noProxy("127.0.0.1")
        .noProxy(".internal.example")
        .noProxy("reports.internal.example:8443")
        .build())
```

Rules are literal and case-insensitive: exact host, domain suffix (`.example.test` or `*.example.test`), optional port, IPv4, bracketed or bare IPv6, and `*` are supported. They are not regular expressions. CIDR is deliberately unsupported. The explicit proxy has no credential field; configure supported proxy authentication through controlled JVM infrastructure. Corporate certificate authorities belong in the JVM truststore; insecure TLS is not supported.

## Headers, diagnostics, and limits

The builder accepts an optional bearer token and custom headers. Header names and values reject CR/LF. Matching is case-insensitive and callers cannot replace managed transport headers, including authorization, content type/length, host, idempotency, schema, kind, status, and checksum. Values are absent from option/result `toString()` and never enter the session trace, reports, or failure bundle.

Endpoint diagnostics retain scheme, host, effective explicit port, and path, but omit query, userinfo, and fragment. Userinfo and fragments are rejected at configuration time. Response preview defaults to 16 KiB, is decoded as UTF-8, bounded, and passed through the configured `RedactionPolicy`. Configured bearer and custom-header values are additionally treated as exact transport secrets, including when an endpoint echoes them; they are never exposed by the result. The default payload limit is 100 MiB. Connect timeout, request timeout, payload size, attempts, retry delay, and preview size are all bounded by immutable options.

`requireSuccess()` converts `FAILED` or `SKIPPED` into `ReportUploadException`; it leaves the finalized session unchanged and preserves a transport exception as its cause when one exists. For best-effort publishing, inspect the immutable result instead:

```java
ReportUploadResult upload = new ReportUploader(options).upload(finalized);
if (!upload.isUploaded()) {
    publishBuildAnnotation(upload.failureCategory(), upload.message());
}
```

Report artifacts can contain application data. Central text redaction applies while Test Lens creates reports and to uploader response diagnostics, but screenshot/video pixels are not redacted. Apply TLS, endpoint authentication, server-side access controls, quarantine, and retention policies appropriate for test evidence.
