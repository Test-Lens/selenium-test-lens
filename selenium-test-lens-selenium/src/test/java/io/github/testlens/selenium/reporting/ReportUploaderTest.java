package io.github.testlens.selenium.reporting;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.testlens.TestLensFinalizationResult;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.core.trace.TraceEventType;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ReportUploaderTest {
    @TempDir Path temp;
    private final List<HttpServer> servers = new ArrayList<>();

    @AfterEach void stopServers() { servers.forEach(server -> server.stop(0)); }

    @Test
    void uploadsSessionReportForPassedAndSkippedWithStableHeadersAndZipContract() throws Exception {
        for (TraceStatus status : List.of(TraceStatus.PASSED, TraceStatus.SKIPPED)) {
            CaptureServer server = server(201, "accepted");
            TestLensFinalizationResult finalized = finalized(status, false);

            ReportUploadResult result = uploader(server.uri("/api/test-lens/reports?tenant=safe"), 1).upload(finalized);

            assertEquals(ReportUploadStatus.UPLOADED, result.status());
            assertEquals(ReportArtifactKind.SESSION_REPORT, result.artifactKind());
            assertEquals(201, result.httpStatus());
            assertEquals(1, result.attempts());
            assertEquals("http://127.0.0.1:" + server.port() + "/api/test-lens/reports", result.endpoint());
            assertEquals("application/zip", server.header("Content-Type"));
            assertEquals("1", server.header("X-Test-Lens-Schema-Version"));
            assertEquals("SESSION_REPORT", server.header("X-Test-Lens-Artifact-Kind"));
            assertEquals(status.name(), server.header("X-Test-Lens-Status"));
            assertEquals(String.valueOf(server.body().length), server.header("Content-Length"));
            String sha = sha256(server.body());
            assertEquals(sha, server.header("X-Test-Lens-SHA256"));
            assertEquals(sha, result.sha256());
            assertEquals("test-lens-session_report-" + sha, server.header("Idempotency-Key"));
            assertEquals(List.of("manifest.json", "trace.json", "report.html"), zipNames(server.body()));
            String manifest = zipText(server.body(), "manifest.json");
            assertTrue(manifest.contains("\"schemaVersion\":\"1\""));
            assertTrue(manifest.contains("\"artifactKind\":\"SESSION_REPORT\""));
            assertTrue(manifest.contains("\"status\":\"" + status.name() + "\""));
            assertTrue(manifest.contains(sha256("trace".getBytes(StandardCharsets.UTF_8))));
            assertEquals("trace", zipText(server.body(), "trace.json"));
            assertEquals("<html>report</html>", zipText(server.body(), "report.html"));
        }
    }

    @Test
    void uploadsCompletedFailureBundleWithoutDeletingOrRepackingIt() throws Exception {
        CaptureServer server = server(204, "");
        TestLensFinalizationResult finalized = finalized(TraceStatus.FAILED, true);
        Path archive = finalized.failureBundleArchive().orElseThrow();
        byte[] before = Files.readAllBytes(archive);

        ReportUploadResult result = uploader(server.uri("/reports"), 1).upload(finalized);

        assertTrue(result.isUploaded());
        assertEquals(ReportArtifactKind.FAILURE_BUNDLE, result.artifactKind());
        assertEquals("FAILURE_BUNDLE", server.header("X-Test-Lens-Artifact-Kind"));
        assertArrayEquals(before, server.body());
        assertArrayEquals(before, Files.readAllBytes(archive));
    }

    @Test
    void bearerCustomHeadersAndResponseDiagnosticsAreBoundedAndRedacted() throws Exception {
        String token = "TL_UPLOAD_TOKEN_CANARY";
        String responseSecret = "TL_RESPONSE_SECRET_CANARY";
        CaptureServer server = server(202, "echo " + token + " tenant-a {\"password\":\"" + responseSecret
                + "\",\"message\":\"Authorization: Bearer abc123456789\"}" + "x".repeat(200));
        server.responseHeader("X-Test-Lens-Report-Id", "report-" + token + "-" + responseSecret);
        URI endpoint = server.uri("/reports?token=TL_ENDPOINT_QUERY_CANARY");
        ReportUploadOptions options = ReportUploadOptions.builder()
                .endpoint(endpoint)
                .bearerToken(token)
                .header("X-Tenant", "tenant-a")
                .maxResponsePreviewBytes(100)
                .redactionPolicy(RedactionPolicy.builder().secret(responseSecret).build())
                .proxy(ReportProxyOptions.direct())
                .build();

        TestLensFinalizationResult finalized = finalized(TraceStatus.PASSED, false);
        int eventCount = finalized.session().events().size();
        ReportUploadResult result = new ReportUploader(options).upload(finalized);

        assertEquals("Bearer " + token, server.header("Authorization"));
        assertEquals("tenant-a", server.header("X-Tenant"));
        assertTrue(result.responsePreview().contains("[REDACTED]"));
        assertTrue(result.responsePreview().contains("[truncated]"));
        assertFalse(result.responsePreview().contains(responseSecret));
        assertFalse(result.responsePreview().contains(token));
        assertFalse(result.responsePreview().contains("tenant-a"));
        assertFalse(result.serverReportId().contains(responseSecret));
        String diagnostics = options + " " + result + " " + result.message() + " " + result.endpoint();
        assertFalse(diagnostics.contains(token));
        assertFalse(diagnostics.contains("TL_ENDPOINT_QUERY_CANARY"));
        assertFalse(diagnostics.contains(responseSecret));
        assertEquals(eventCount, finalized.session().events().size());
        assertFalse(finalized.session().events().toString().contains(token));
    }

    @Test
    void rejectsManagedHeadersAndUnsafeConfiguration() {
        URI endpoint = URI.create("https://example.test/reports");
        for (String name : List.of("Content-Type", "content-length", "HOST", "Authorization", "Proxy-Authorization",
                "Idempotency-Key", "X-Test-Lens-SHA256", "X-Test-Lens-Status")) {
            assertThrows(IllegalArgumentException.class,
                    () -> ReportUploadOptions.builder().endpoint(endpoint).header(name, "value"));
        }
        assertThrows(IllegalArgumentException.class,
                () -> ReportUploadOptions.builder().endpoint(URI.create("ftp://example.test/reports")));
        assertThrows(IllegalArgumentException.class,
                () -> ReportUploadOptions.builder().endpoint(URI.create("https://user@example.test/reports")));
        assertThrows(IllegalArgumentException.class,
                () -> ReportUploadOptions.builder().endpoint(URI.create("https://example.test/reports#fragment")));
        assertThrows(IllegalArgumentException.class,
                () -> ReportUploadOptions.builder().endpoint(endpoint).header("X-Test\r\nLens", "value"));
        assertThrows(IllegalArgumentException.class,
                () -> ReportUploadOptions.builder().endpoint(endpoint).header("X-Test", "bad\nvalue"));
    }

    @Test
    void credentialsHeadersAndProxyDetailsHaveNoPublicAccessors() {
        Set<String> optionMethods = Arrays.stream(ReportUploadOptions.class.getMethods())
                .map(java.lang.reflect.Method::getName)
                .collect(java.util.stream.Collectors.toSet());
        assertFalse(optionMethods.contains("bearerToken"));
        assertFalse(optionMethods.contains("headers"));
        assertFalse(optionMethods.contains("proxy"));

        Set<String> proxyMethods = Arrays.stream(ReportProxyOptions.class.getMethods())
                .map(java.lang.reflect.Method::getName)
                .collect(java.util.stream.Collectors.toSet());
        assertFalse(proxyMethods.contains("mode"));
        assertFalse(proxyMethods.contains("host"));
        assertFalse(proxyMethods.contains("port"));
        assertFalse(proxyMethods.contains("noProxy"));
    }

    @Test
    void classifiesHttpFailures() throws Exception {
        Map<Integer, ReportUploadFailureCategory> expected = new LinkedHashMap<>();
        expected.put(401, ReportUploadFailureCategory.AUTHENTICATION);
        expected.put(403, ReportUploadFailureCategory.AUTHENTICATION);
        expected.put(413, ReportUploadFailureCategory.PAYLOAD_TOO_LARGE);
        expected.put(429, ReportUploadFailureCategory.RATE_LIMITED);
        expected.put(500, ReportUploadFailureCategory.SERVER_ERROR);
        expected.put(503, ReportUploadFailureCategory.SERVER_ERROR);
        for (Map.Entry<Integer, ReportUploadFailureCategory> entry : expected.entrySet()) {
            CaptureServer server = server(entry.getKey(), "failure");
            ReportUploadResult result = uploader(server.uri("/reports"), 1)
                    .upload(finalized(TraceStatus.PASSED, false));
            assertEquals(ReportUploadStatus.FAILED, result.status());
            assertEquals(entry.getValue(), result.failureCategory());
            assertEquals(1, result.attempts());
            assertEquals(1, server.requests());
            assertThrows(ReportUploadException.class, result::requireSuccess);
        }
    }

    @Test
    void doesNotRetryTerminalHttpResponses() throws Exception {
        for (int status : List.of(400, 401, 403, 404, 409, 413, 500)) {
            CaptureServer server = server(status, "terminal");

            ReportUploadResult result = uploader(server.uri("/reports"), 3)
                    .upload(finalized(TraceStatus.PASSED, false));

            assertEquals(ReportUploadStatus.FAILED, result.status());
            assertEquals(1, result.attempts());
            assertEquals(1, server.requests());
        }
    }

    @Test
    void retriesOnlyRetryableResponsesWithOneIdempotencyKeyAndBoundedRetryAfter() throws Exception {
        CaptureServer server = server(List.of(503, 429, 200), "done");
        server.responseHeader("Retry-After", "999999");
        ReportUploadOptions options = options(server.uri("/reports"), 3)
                .maxRetryAfter(Duration.ZERO)
                .build();

        ReportUploadResult result = new ReportUploader(options).upload(finalized(TraceStatus.PASSED, false));

        assertTrue(result.isUploaded());
        assertEquals(3, result.attempts());
        assertEquals(3, server.requests());
        assertEquals(1, Set.copyOf(server.headers("Idempotency-Key")).size());
    }

    @Test
    void transportTimeoutIsReportedWithOriginalCauseAndLocalReportsRemain() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        HttpServer http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext("/slow", exchange -> {
            entered.countDown();
            try { release.await(2, TimeUnit.SECONDS); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            try { exchange.sendResponseHeaders(204, -1); } finally { exchange.close(); }
        });
        http.start(); servers.add(http);
        TestLensFinalizationResult finalized = finalized(TraceStatus.PASSED, false);
        ReportUploadOptions options = options(URI.create("http://127.0.0.1:" + http.getAddress().getPort() + "/slow"), 1)
                .requestTimeout(Duration.ofMillis(100)).build();
        ReportUploadResult result;
        try {
            result = new ReportUploader(options).upload(finalized);
        } finally { release.countDown(); }

        assertTrue(entered.await(1, TimeUnit.SECONDS));
        assertEquals(ReportUploadFailureCategory.TIMEOUT, result.failureCategory());
        assertInstanceOf(HttpTimeoutException.class, result.exception());
        ReportUploadException failure = assertThrows(ReportUploadException.class, result::requireSuccess);
        assertSame(result.exception(), failure.getCause());
        assertTrue(Files.exists(finalized.jsonReport()));
        assertTrue(Files.exists(finalized.htmlReport()));
    }

    @Test
    void requestTimeoutAlsoBoundsAResponseBodyThatStallsAfterHeaders() throws Exception {
        CountDownLatch bodyStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        HttpServer http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext("/stalled-body", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write('x');
                body.flush();
                bodyStarted.countDown();
                release.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        http.start();
        servers.add(http);
        ReportUploadOptions options = options(
                URI.create("http://127.0.0.1:" + http.getAddress().getPort() + "/stalled-body"), 1)
                .requestTimeout(Duration.ofMillis(100))
                .build();
        var executor = Executors.newSingleThreadExecutor();
        Future<ReportUploadResult> upload = executor.submit(
                () -> new ReportUploader(options).upload(finalized(TraceStatus.PASSED, false)));
        try {
            assertTrue(bodyStarted.await(1, TimeUnit.SECONDS));
            ReportUploadResult result = upload.get(1, TimeUnit.SECONDS);
            assertEquals(ReportUploadFailureCategory.TIMEOUT, result.failureCategory());
            assertInstanceOf(HttpTimeoutException.class, result.exception());
            assertInstanceOf(java.util.concurrent.TimeoutException.class, result.exception().getCause());
        } finally {
            release.countDown();
            upload.cancel(true);
            executor.shutdownNow();
        }
    }

    @Test
    void retriesTransportIoWithTheConfiguredBound() throws Exception {
        HttpServer unavailable = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = unavailable.getAddress().getPort();
        unavailable.start();
        unavailable.stop(0);

        ReportUploadResult result = uploader(URI.create("http://127.0.0.1:" + port + "/reports"), 3)
                .upload(finalized(TraceStatus.PASSED, false));

        assertEquals(ReportUploadStatus.FAILED, result.status());
        assertEquals(ReportUploadFailureCategory.TRANSPORT, result.failureCategory());
        assertEquals(3, result.attempts());
        assertNotNull(result.exception());
    }

    @Test
    void redirectIsNotFollowed() throws Exception {
        AtomicInteger target = new AtomicInteger();
        HttpServer http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/target");
            exchange.sendResponseHeaders(307, -1); exchange.close();
        });
        http.createContext("/target", exchange -> { target.incrementAndGet(); exchange.sendResponseHeaders(204, -1); exchange.close(); });
        http.start(); servers.add(http);

        ReportUploadResult result = uploader(URI.create("http://127.0.0.1:" + http.getAddress().getPort() + "/redirect"), 2)
                .upload(finalized(TraceStatus.PASSED, false));

        assertEquals(307, result.httpStatus());
        assertEquals(ReportUploadFailureCategory.REJECTED_REQUEST, result.failureCategory());
        assertEquals(1, result.attempts());
        assertEquals(0, target.get());
    }

    @Test
    void validatesArtifactSizePresenceCompletionAndSymlinksBeforeConnecting() throws Exception {
        CaptureServer server = server(204, "");
        TestLensFinalizationResult finalized = finalized(TraceStatus.PASSED, false);
        ReportUploadOptions tiny = options(server.uri("/reports"), 1).maxPayloadBytes(10).build();
        ReportUploadResult tooLarge = new ReportUploader(tiny).upload(finalized);
        assertEquals(ReportUploadFailureCategory.PAYLOAD_TOO_LARGE, tooLarge.failureCategory());
        assertEquals(0, server.requests());

        Files.delete(finalized.jsonReport());
        ReportUploadResult missing = uploader(server.uri("/reports"), 1).upload(finalized);
        assertEquals(ReportUploadFailureCategory.INVALID_ARTIFACT, missing.failureCategory());
        assertEquals(0, missing.attempts());
        assertEquals(0, server.requests());

        TestLensFinalizationResult incompleteBundle = finalized(TraceStatus.FAILED, true);
        Path archive = incompleteBundle.failureBundleArchive().orElseThrow();
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            put(zip, "trace.json", "trace");
        }
        ReportUploadResult incomplete = uploader(server.uri("/reports"), 1).upload(incompleteBundle);
        assertEquals(ReportUploadFailureCategory.INVALID_ARTIFACT, incomplete.failureCategory());
        assertEquals(0, server.requests());
    }

    @Test
    void rejectsSymbolicReportSourcesWhenTheFileSystemSupportsLinks() throws Exception {
        CaptureServer server = server(204, "");
        TestLensFinalizationResult finalized = finalized(TraceStatus.PASSED, false);
        Path realTrace = finalized.outputDirectory().resolve("real-trace.json");
        Files.move(finalized.jsonReport(), realTrace);
        try {
            Files.createSymbolicLink(finalized.jsonReport(), realTrace.getFileName());
        } catch (UnsupportedOperationException | IOException | SecurityException unavailable) {
            Assumptions.abort("Symbolic links are unavailable in this test environment");
        }

        ReportUploadResult result = uploader(server.uri("/reports"), 1).upload(finalized);

        assertEquals(ReportUploadFailureCategory.INVALID_ARTIFACT, result.failureCategory());
        assertEquals(0, result.attempts());
        assertEquals(0, server.requests());
    }

    @Test
    void removesTemporarySessionZipAfterSuccessAndFailure() throws Exception {
        Set<String> before = sessionReportTemporaryFiles();
        CaptureServer success = server(204, "");
        CaptureServer failure = server(500, "failed");

        assertTrue(uploader(success.uri("/reports"), 1).upload(finalized(TraceStatus.PASSED, false)).isUploaded());
        assertEquals(ReportUploadStatus.FAILED,
                uploader(failure.uri("/reports"), 1).upload(finalized(TraceStatus.PASSED, false)).status());

        assertEquals(before, sessionReportTemporaryFiles());
    }

    @Test
    void appliesCustomRedactionAndPreservesExplicitDisabledOptOutForResponsePreview() throws Exception {
        String secret = "TL_UPLOAD_RESPONSE_SECRET";
        CaptureServer maskedServer = server(400, "{\"tenant-key\":\"" + secret + "\"}");
        RedactionPolicy masked = RedactionPolicy.builder().sensitiveKey("tenant-key")
                .secret(secret).replacement("MASK\"\\\n").build();
        ReportUploadResult maskedResult = new ReportUploader(options(maskedServer.uri("/reports"), 1)
                .redactionPolicy(masked).build()).upload(finalized(TraceStatus.PASSED, false));
        assertFalse(maskedResult.responsePreview().contains(secret));
        assertTrue(maskedResult.responsePreview().contains("MASK"));
        assertFalse(maskedResult.toString().contains(secret));

        CaptureServer rawServer = server(400, "plain " + secret);
        ReportUploadResult rawResult = new ReportUploader(options(rawServer.uri("/reports"), 1)
                .redactionPolicy(RedactionPolicy.disabled()).build()).upload(finalized(TraceStatus.PASSED, false));
        assertTrue(rawResult.responsePreview().contains(secret));
        assertFalse(rawResult.toString().contains(secret));
    }

    @Test
    void noSessionIsSkippedAndUploadDoesNotMutateFinalizedSessionLifecycle() throws Exception {
        CaptureServer server = server(204, "");
        ReportUploadResult skipped = uploader(server.uri("/reports"), 1)
                .upload(new TestLensFinalizationResult(null, null, null, null, null, List.of()));
        assertEquals(ReportUploadStatus.SKIPPED, skipped.status());
        assertEquals(0, server.requests());

        TestLensFinalizationResult finalized = finalized(TraceStatus.FAILED, true);
        UiTestLensSession session = finalized.session();
        int events = session.events().size();
        long terminal = session.events().stream().filter(event -> event.type() == TraceEventType.SESSION_FINISHED).count();
        var retry = session.retrySummary();
        ReportUploadResult uploaded = uploader(server.uri("/reports"), 1).upload(finalized);
        assertTrue(uploaded.isUploaded());
        assertEquals(TraceStatus.FAILED, session.metadata().status());
        assertEquals(events, session.events().size());
        assertEquals(terminal, session.events().stream().filter(event -> event.type() == TraceEventType.SESSION_FINISHED).count());
        assertEquals(retry, session.retrySummary());
    }

    @Test
    void directSystemExplicitAndNoProxySelectionAreIndependent() throws Exception {
        CaptureServer endpoint = server(204, "");
        CaptureServer proxy = server(202, "proxied");
        ProxySelector original = ProxySelector.getDefault();
        CountingSelector system = new CountingSelector();
        try {
            ProxySelector.setDefault(system);
            ReportUploadResult direct = new ReportUploader(options(endpoint.uri("/direct"), 1)
                    .proxy(ReportProxyOptions.direct()).build()).upload(finalized(TraceStatus.PASSED, false));
            assertEquals(204, direct.httpStatus());
            assertEquals(0, system.calls.get());

            ReportUploadResult viaSystem = new ReportUploader(options(endpoint.uri("/system"), 1)
                    .proxy(ReportProxyOptions.system()).build()).upload(finalized(TraceStatus.PASSED, false));
            assertEquals(204, viaSystem.httpStatus());
            assertTrue(system.calls.get() > 0);
        } finally { ProxySelector.setDefault(original); }

        ReportProxyOptions explicit = ReportProxyOptions.builder().mode(ReportProxyMode.EXPLICIT)
                .host("127.0.0.1").port(proxy.port()).build();
        ReportUploadResult proxied = new ReportUploader(options(endpoint.uri("/explicit"), 1)
                .proxy(explicit).build()).upload(finalized(TraceStatus.PASSED, false));
        assertEquals(202, proxied.httpStatus());
        assertEquals(1, proxy.requests());

        ReportProxyOptions bypass = ReportProxyOptions.builder().mode(ReportProxyMode.EXPLICIT)
                .host("127.0.0.1").port(proxy.port()).noProxy("127.0.0.1:" + endpoint.port()).build();
        ReportUploadResult bypassed = new ReportUploader(options(endpoint.uri("/bypass"), 1)
                .proxy(bypass).build()).upload(finalized(TraceStatus.PASSED, false));
        assertEquals(204, bypassed.httpStatus());
        assertEquals(1, proxy.requests());
    }

    @Test
    void noProxyRulesCoverExactSuffixPortLocalhostIpv4Ipv6AndWildcardWithoutRegexOrCidr() {
        ReportProxyOptions rules = ReportProxyOptions.builder().mode(ReportProxyMode.EXPLICIT)
                .host("proxy.test").port(8080)
                .noProxy("exact.test")
                .noProxy(".example.test")
                .noProxy("localhost:8081")
                .noProxy("127.0.0.1")
                .noProxy("[::1]:8443")
                .build();
        assertTrue(ReportUploader.bypassesExplicitProxy(rules, URI.create("https://exact.test/a")));
        assertTrue(ReportUploader.bypassesExplicitProxy(rules, URI.create("https://sub.example.test/a")));
        assertTrue(ReportUploader.bypassesExplicitProxy(rules, URI.create("http://localhost:8081/a")));
        assertFalse(ReportUploader.bypassesExplicitProxy(rules, URI.create("http://localhost:8082/a")));
        assertTrue(ReportUploader.bypassesExplicitProxy(rules, URI.create("http://127.0.0.1/a")));
        assertTrue(ReportUploader.bypassesExplicitProxy(rules, URI.create("https://[::1]:8443/a")));
        assertFalse(ReportUploader.bypassesExplicitProxy(rules, URI.create("https://[::1]:9443/a")));
        ReportProxyOptions wildcard = ReportProxyOptions.builder().mode(ReportProxyMode.EXPLICIT)
                .host("proxy.test").port(8080).noProxy("*").build();
        assertTrue(ReportUploader.bypassesExplicitProxy(wildcard, URI.create("https://anything.test/a")));
        assertThrows(IllegalArgumentException.class, () -> ReportProxyOptions.builder().noProxy("10.0.0.0/8"));
    }

    @Test
    void concurrentUploadersShareNoMutableTransportState() throws Exception {
        CaptureServer server = server(204, "");
        TestLensFinalizationResult finalized = finalized(TraceStatus.FAILED, true);
        ReportUploader uploader = uploader(server.uri("/reports"), 1);
        var executor = Executors.newFixedThreadPool(4);
        try {
            List<java.util.concurrent.Callable<ReportUploadResult>> calls = new ArrayList<>();
            for (int index = 0; index < 8; index++) calls.add(() -> uploader.upload(finalized));
            for (var future : executor.invokeAll(calls)) assertTrue(future.get().isUploaded());
        } finally { executor.shutdownNow(); }
        assertEquals(8, server.requests());
        assertEquals(1, Set.copyOf(server.headers("Idempotency-Key")).size());
    }

    @Test
    void constructionAloneNeverConnectsAndSuccessfulRequireSuccessReturnsSameResult() throws Exception {
        CaptureServer server = server(200, "ok");
        ReportUploader uploader = uploader(server.uri("/reports"), 1);
        assertEquals(0, server.requests());
        ReportUploadResult result = uploader.upload(finalized(TraceStatus.PASSED, false));
        assertSame(result, result.requireSuccess());
        assertEquals(1, server.requests());
    }

    private ReportUploader uploader(URI endpoint, int attempts) {
        return new ReportUploader(options(endpoint, attempts).build());
    }

    private static Set<String> sessionReportTemporaryFiles() throws IOException {
        Path temporaryRoot = Path.of(System.getProperty("java.io.tmpdir"));
        try (var files = Files.list(temporaryRoot)) {
            return files.map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith("test-lens-session-report-") && name.endsWith(".zip"))
                    .collect(java.util.stream.Collectors.toSet());
        }
    }

    private ReportUploadOptions.Builder options(URI endpoint, int attempts) {
        return ReportUploadOptions.builder().endpoint(endpoint).maxAttempts(attempts)
                .connectTimeout(Duration.ofSeconds(2)).requestTimeout(Duration.ofSeconds(2))
                .proxy(ReportProxyOptions.direct());
    }

    private TestLensFinalizationResult finalized(TraceStatus status, boolean bundle) throws Exception {
        Path root = Files.createTempDirectory(temp, "finalized-");
        Path trace = Files.writeString(root.resolve("trace.json"), "trace");
        Path html = Files.writeString(root.resolve("report.html"), "<html>report</html>");
        UiTestLensSession session = UiTestLensSession.start("upload");
        switch (status) {
            case PASSED -> session.finishPassed();
            case FAILED -> session.finishFailed(new AssertionError("original"));
            case SKIPPED -> session.finishSkipped("reason");
            default -> { }
        }
        if (bundle) {
            Path directory = Files.createDirectories(root.resolve("failure-bundle"));
            Files.writeString(directory.resolve("manifest.json"), "{}");
            Path archive = root.resolve("failure-bundle.zip");
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
                put(zip, "manifest.json", "{}");
                put(zip, "trace.json", "trace");
                put(zip, "report.html", "<html>report</html>");
            }
        }
        return new TestLensFinalizationResult(session, root, trace, html, null, List.of());
    }

    private CaptureServer server(int status, String response) throws IOException {
        return server(List.of(status), response);
    }

    private CaptureServer server(List<Integer> statuses, String response) throws IOException {
        CaptureServer fixture = new CaptureServer(statuses, response);
        fixture.start(); servers.add(fixture.server);
        return fixture;
    }

    private static void put(ZipOutputStream zip, String name, String value) throws IOException {
        ZipEntry entry = new ZipEntry(name); entry.setTime(0); zip.putNextEntry(entry);
        zip.write(value.getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
    }

    private static List<String> zipNames(byte[] content) throws IOException {
        Path file = Files.createTempFile("upload-zip-test-", ".zip");
        try {
            Files.write(file, content);
            try (ZipFile zip = new ZipFile(file.toFile())) {
                return Collections.list(zip.entries()).stream().map(ZipEntry::getName).toList();
            }
        } finally { Files.deleteIfExists(file); }
    }

    private static String zipText(byte[] content, String name) throws IOException {
        Path file = Files.createTempFile("upload-zip-test-", ".zip");
        try {
            Files.write(file, content);
            try (ZipFile zip = new ZipFile(file.toFile())) {
                return new String(zip.getInputStream(zip.getEntry(name)).readAllBytes(), StandardCharsets.UTF_8);
            }
        } finally { Files.deleteIfExists(file); }
    }

    private static String sha256(byte[] value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }

    private static final class CountingSelector extends ProxySelector {
        private final AtomicInteger calls = new AtomicInteger();
        @Override public List<Proxy> select(URI uri) { calls.incrementAndGet(); return List.of(Proxy.NO_PROXY); }
        @Override public void connectFailed(URI uri, SocketAddress sa, IOException ioe) { }
    }

    private static final class CaptureServer {
        private final HttpServer server;
        private final List<Integer> statuses;
        private final String response;
        private final AtomicInteger requests = new AtomicInteger();
        private final AtomicReference<byte[]> body = new AtomicReference<>(new byte[0]);
        private final Map<String, List<String>> requestHeaders = Collections.synchronizedMap(new LinkedHashMap<>());
        private final Map<String, String> responseHeaders = Collections.synchronizedMap(new LinkedHashMap<>());

        private CaptureServer(List<Integer> statuses, String response) throws IOException {
            this.statuses = statuses; this.response = response;
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
        }

        private void start() { server.start(); }
        private int port() { return server.getAddress().getPort(); }
        private URI uri(String path) { return URI.create("http://127.0.0.1:" + port() + path); }
        private int requests() { return requests.get(); }
        private byte[] body() { return body.get(); }
        private String header(String name) { return headers(name).get(0); }
        private List<String> headers(String name) {
            synchronized (requestHeaders) {
                return requestHeaders.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(name))
                        .findFirst().map(entry -> List.copyOf(entry.getValue())).orElse(List.of());
            }
        }
        private void responseHeader(String name, String value) { responseHeaders.put(name, value); }

        private void handle(HttpExchange exchange) throws IOException {
            int request = requests.getAndIncrement();
            body.set(exchange.getRequestBody().readAllBytes());
            exchange.getRequestHeaders().forEach((name, values) ->
                    requestHeaders.computeIfAbsent(name, ignored -> Collections.synchronizedList(new ArrayList<>())).addAll(values));
            responseHeaders.forEach((name, value) -> exchange.getResponseHeaders().set(name, value));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            int status = statuses.get(Math.min(request, statuses.size() - 1));
            if (status == 204) exchange.sendResponseHeaders(status, -1);
            else { exchange.sendResponseHeaders(status, bytes.length); exchange.getResponseBody().write(bytes); }
            exchange.close();
        }
    }
}
