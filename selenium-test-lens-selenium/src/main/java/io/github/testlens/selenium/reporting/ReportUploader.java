package io.github.testlens.selenium.reporting;

import io.github.testlens.TestLensFinalizationResult;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.export.TraceJsonWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Synchronously uploads one completed Test Lens ZIP with JDK HTTP APIs.
 * Construction and finalization perform no network I/O; only {@link #upload(TestLensFinalizationResult)} connects.
 */
public final class ReportUploader {
    /** The receiver and session-report manifest schema version. */
    public static final String SCHEMA_VERSION = "1";
    private static final Set<Integer> RETRYABLE_STATUS = Set.of(408, 429, 502, 503, 504);
    private final ReportUploadOptions options;

    /**
     * Creates an uploader without opening a network connection.
     *
     * @param options immutable endpoint, transport, proxy, retry, and redaction options
     */
    public ReportUploader(ReportUploadOptions options) {
        if (options == null) throw new IllegalArgumentException("options must not be null");
        this.options = options;
    }

    /**
     * Uploads a completed failure bundle when available, otherwise packages the final trace and HTML report.
     * The source finalization result, session, reports, and WebDriver ownership are never modified.
     *
     * @param finalized completed facade finalization result
     * @return the immutable upload outcome; no transport failure is thrown directly
     */
    public ReportUploadResult upload(TestLensFinalizationResult finalized) {
        long started = System.nanoTime();
        String endpoint = options.redactDiagnostic(ReportUploadOptions.safeEndpoint(options.endpoint()));
        if (finalized == null || finalized.session() == null) {
            return result(ReportUploadStatus.SKIPPED, null, endpoint, null, 0, started, 0, "", null, "",
                    ReportUploadFailureCategory.INVALID_ARTIFACT, null, "No finalized Test Lens session is available");
        }
        TraceStatus finalStatus = finalized.session().metadata().status();
        if (finalStatus != TraceStatus.PASSED && finalStatus != TraceStatus.FAILED && finalStatus != TraceStatus.SKIPPED) {
            return result(ReportUploadStatus.FAILED, null, endpoint, null, 0, started, 0, "", null, "",
                    ReportUploadFailureCategory.INVALID_ARTIFACT, null, "The Test Lens session is not finalized");
        }

        PreparedArtifact prepared = null;
        try {
            prepared = prepare(finalized, finalStatus);
            long size = Files.size(prepared.path());
            if (size > options.maxPayloadBytes()) {
                return result(ReportUploadStatus.FAILED, prepared.kind(), endpoint, null, 0, started, size, "", null, "",
                        ReportUploadFailureCategory.PAYLOAD_TOO_LARGE, null,
                        "Payload exceeds maxPayloadBytes=" + options.maxPayloadBytes());
            }
            String sha256 = sha256(prepared.path());
            HttpClient client;
            try {
                client = client();
            } catch (RuntimeException invalidProxy) {
                return result(ReportUploadStatus.FAILED, prepared.kind(), endpoint, null, 0, started, size, sha256,
                        null, "", ReportUploadFailureCategory.PROXY_CONFIGURATION, invalidProxy,
                        "HTTP proxy configuration could not be applied");
            }
            return send(client, prepared, finalStatus, size, sha256, endpoint, started);
        } catch (IOException | RuntimeException invalidArtifact) {
            return result(ReportUploadStatus.FAILED, prepared == null ? null : prepared.kind(), endpoint, null, 0,
                    started, 0, "", null, "", ReportUploadFailureCategory.INVALID_ARTIFACT, invalidArtifact,
                    safeMessage("Report artifact could not be prepared", invalidArtifact));
        } finally {
            if (prepared != null && prepared.temporary()) {
                try { Files.deleteIfExists(prepared.path()); } catch (IOException ignored) { }
            }
        }
    }

    private ReportUploadResult send(HttpClient client, PreparedArtifact artifact, TraceStatus finalStatus,
                                    long size, String sha256, String endpoint, long started) {
        String idempotencyKey = "test-lens-" + artifact.kind().name().toLowerCase(Locale.ROOT) + "-" + sha256;
        HttpRequest.BodyPublisher payload;
        try {
            payload = HttpRequest.BodyPublishers.ofFile(artifact.path());
        } catch (IOException missingArtifact) {
            return result(ReportUploadStatus.FAILED, artifact.kind(), endpoint, null, 0, started, size, sha256,
                    null, "", ReportUploadFailureCategory.INVALID_ARTIFACT, missingArtifact,
                    "The completed report artifact is no longer readable");
        }
        Throwable lastFailure = null;
        for (int attempt = 1; attempt <= options.maxAttempts(); attempt++) {
            try {
                HttpRequest.Builder request = HttpRequest.newBuilder(options.endpoint())
                        .timeout(options.requestTimeout())
                        .header("Content-Type", "application/zip")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Test-Lens-Schema-Version", SCHEMA_VERSION)
                        .header("X-Test-Lens-Artifact-Kind", artifact.kind().name())
                        .header("X-Test-Lens-Status", finalStatus.name())
                        .header("X-Test-Lens-SHA256", sha256);
                if (options.bearerTokenValue() != null) {
                    request.header("Authorization", "Bearer " + options.bearerTokenValue());
                }
                options.headerValues().forEach(request::header);
                CompletableFuture<HttpResponse<byte[]>> pending = client.sendAsync(
                        request.POST(payload).build(),
                        responseBodyHandler(options.maxResponsePreviewBytes()));
                HttpResponse<byte[]> response;
                try {
                    response = pending.get(options.requestTimeout().toNanos(), TimeUnit.NANOSECONDS);
                } catch (TimeoutException timeout) {
                    pending.cancel(true);
                    if (attempt < options.maxAttempts()) continue;
                    HttpTimeoutException requestTimeout = new HttpTimeoutException("request timed out");
                    requestTimeout.initCause(timeout);
                    return result(ReportUploadStatus.FAILED, artifact.kind(), endpoint, null, attempt, started, size,
                            sha256, null, "", ReportUploadFailureCategory.TIMEOUT, requestTimeout,
                            "Upload timed out");
                }
                String preview = preview(response.body(), options.maxResponsePreviewBytes());
                int status = response.statusCode();
                String serverId = response.headers().firstValue("X-Test-Lens-Report-Id")
                        .map(value -> bounded(options.redactDiagnostic(value), 512)).orElse(null);
                if (status >= 200 && status < 300) {
                    return result(ReportUploadStatus.UPLOADED, artifact.kind(), endpoint, status, attempt, started,
                            size, sha256, serverId, preview, ReportUploadFailureCategory.NONE, null, "Uploaded");
                }
                ReportUploadFailureCategory category = category(status);
                if (attempt < options.maxAttempts() && RETRYABLE_STATUS.contains(status)) {
                    awaitRetry(response.headers().firstValue("Retry-After"));
                    continue;
                }
                return result(ReportUploadStatus.FAILED, artifact.kind(), endpoint, status, attempt, started,
                        size, sha256, serverId, preview, category, null, "Endpoint returned HTTP " + status);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                lastFailure = interrupted;
                return result(ReportUploadStatus.FAILED, artifact.kind(), endpoint, null, attempt, started, size,
                        sha256, null, "", ReportUploadFailureCategory.TRANSPORT, interrupted,
                        "Upload was interrupted");
            } catch (ExecutionException failedRequest) {
                Throwable transport = failedRequest.getCause() == null ? failedRequest : failedRequest.getCause();
                lastFailure = transport;
                if (attempt < options.maxAttempts()) continue;
                ReportUploadFailureCategory category = transport instanceof HttpTimeoutException
                        ? ReportUploadFailureCategory.TIMEOUT : ReportUploadFailureCategory.TRANSPORT;
                return result(ReportUploadStatus.FAILED, artifact.kind(), endpoint, null, attempt, started, size,
                        sha256, null, "", category, transport,
                        category == ReportUploadFailureCategory.TIMEOUT ? "Upload timed out" : "HTTP transport failed");
            }
        }
        return result(ReportUploadStatus.FAILED, artifact.kind(), endpoint, null, options.maxAttempts(), started,
                size, sha256, null, "", ReportUploadFailureCategory.TRANSPORT, lastFailure, "HTTP transport failed");
    }

    private HttpClient client() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(options.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER);
        switch (options.proxy().mode()) {
            case DIRECT -> builder.proxy(new FixedProxySelector(null, List.of()));
            case SYSTEM -> {
                ProxySelector selector = ProxySelector.getDefault();
                if (selector != null) builder.proxy(selector);
            }
            case EXPLICIT -> builder.proxy(new FixedProxySelector(
                    InetSocketAddress.createUnresolved(options.proxy().host(), options.proxy().port()),
                    options.proxy().noProxy()));
        }
        return builder.build();
    }

    static boolean bypassesExplicitProxy(ReportProxyOptions proxy, URI endpoint) {
        if (proxy == null || proxy.mode() != ReportProxyMode.EXPLICIT) return false;
        return proxy.noProxy().stream().map(NoProxyRule::parse).anyMatch(rule -> rule.matches(endpoint));
    }

    private PreparedArtifact prepare(TestLensFinalizationResult finalized, TraceStatus status) throws IOException {
        Optional<Path> failureBundle = finalized.failureBundleArchive();
        if (failureBundle.isPresent()) {
            Path path = requireSource(finalized.outputDirectory(), failureBundle.get(), "failure bundle");
            validateCompletedZip(path);
            return new PreparedArtifact(path, ReportArtifactKind.FAILURE_BUNDLE, false);
        }
        Path trace = requireSource(finalized.outputDirectory(), finalized.jsonReport(), "trace report");
        Path html = requireSource(finalized.outputDirectory(), finalized.htmlReport(), "HTML report");
        Path temporary = Files.createTempFile("test-lens-session-report-", ".zip");
        boolean complete = false;
        try {
            createSessionReport(temporary, status, trace, html);
            complete = true;
            return new PreparedArtifact(temporary, ReportArtifactKind.SESSION_REPORT, true);
        } finally {
            if (!complete) Files.deleteIfExists(temporary);
        }
    }

    private static Path requireSource(Path root, Path source, String label) throws IOException {
        if (root == null || source == null) throw new IOException("Missing " + label);
        Path normalized = source.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(normalized) || !Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("The " + label + " is not a regular non-symbolic file");
        }
        Path normalizedRoot = root.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(normalizedRoot) || !Files.isDirectory(normalizedRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("The finalization directory is not a regular non-symbolic directory");
        }
        if (!normalized.startsWith(normalizedRoot)) throw new IOException("The " + label + " is outside the finalization directory");
        Path current = normalizedRoot;
        for (Path component : normalizedRoot.relativize(normalized)) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) throw new IOException("The " + label + " crosses a symbolic link");
        }
        return normalized;
    }

    private static void validateCompletedZip(Path path) throws IOException {
        Set<String> names = new LinkedHashSet<>();
        boolean manifest = false;
        try (ZipFile zip = new ZipFile(path.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!safeEntryName(name) || !names.add(name)) throw new IOException("Failure bundle has an unsafe or duplicate ZIP entry");
                if ("manifest.json".equals(name)) manifest = true;
            }
        }
        if (!manifest) throw new IOException("Failure bundle is incomplete: manifest.json is missing");
    }

    private static void createSessionReport(Path destination, TraceStatus status, Path trace, Path html) throws IOException {
        List<SourceEntry> sources = List.of(
                new SourceEntry("trace.json", trace, Files.size(trace), sha256(trace)),
                new SourceEntry("report.html", html, Files.size(html), sha256(html)));
        List<Map<String, Object>> artifacts = new ArrayList<>();
        for (SourceEntry source : sources) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", source.name());
            entry.put("size", source.size());
            entry.put("sha256", source.sha256());
            artifacts.add(entry);
        }
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schemaVersion", SCHEMA_VERSION);
        manifest.put("testLensVersion", testLensVersion());
        manifest.put("status", status.name());
        manifest.put("artifactKind", ReportArtifactKind.SESSION_REPORT.name());
        manifest.put("createdAt", Instant.now().toString());
        manifest.put("artifacts", artifacts);
        byte[] manifestBytes = TraceJsonWriter.write(manifest).getBytes(StandardCharsets.UTF_8);

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(destination))) {
            writeEntry(zip, "manifest.json", manifestBytes);
            for (SourceEntry source : sources) writeEntry(zip, source.name(), source.path());
        }
    }

    private static void writeEntry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0L);
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }

    private static void writeEntry(ZipOutputStream zip, String name, Path source) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0L);
        zip.putNextEntry(entry);
        Files.copy(source, zip);
        zip.closeEntry();
    }

    private void awaitRetry(Optional<String> retryAfter) throws InterruptedException {
        long seconds = retryAfter.flatMap(ReportUploader::positiveSeconds).orElse(0L);
        Duration requested = Duration.ofSeconds(Math.min(seconds, Integer.MAX_VALUE));
        Duration bounded = requested.compareTo(options.maxRetryAfter()) > 0 ? options.maxRetryAfter() : requested;
        long remaining = bounded.toNanos();
        while (remaining > 0) {
            long before = System.nanoTime();
            LockSupport.parkNanos(remaining);
            if (Thread.interrupted()) throw new InterruptedException("Interrupted during bounded Retry-After delay");
            remaining = Math.max(0, remaining - Math.max(0, System.nanoTime() - before));
        }
    }

    private static Optional<Long> positiveSeconds(String value) {
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed < 0 ? Optional.empty() : Optional.of(parsed);
        } catch (RuntimeException ignored) { return Optional.empty(); }
    }

    private static ReportUploadFailureCategory category(int status) {
        if (status == 401 || status == 403) return ReportUploadFailureCategory.AUTHENTICATION;
        if (status == 413) return ReportUploadFailureCategory.PAYLOAD_TOO_LARGE;
        if (status == 429) return ReportUploadFailureCategory.RATE_LIMITED;
        if (status == 408) return ReportUploadFailureCategory.TIMEOUT;
        if (status >= 500) return ReportUploadFailureCategory.SERVER_ERROR;
        if (status >= 300 && status < 500) return ReportUploadFailureCategory.REJECTED_REQUEST;
        return ReportUploadFailureCategory.INVALID_RESPONSE;
    }

    private static HttpResponse.BodyHandler<byte[]> responseBodyHandler(int previewLimit) {
        return ignored -> new LimitedBodySubscriber(previewLimit);
    }

    private String preview(byte[] bytes, int limit) {
        if (limit == 0) return "";
        boolean truncated = bytes.length > limit;
        int length = Math.min(bytes.length, limit);
        String text = new String(bytes, 0, length, StandardCharsets.UTF_8);
        String safe = options.redactDiagnostic(text);
        return safe + (truncated ? "…[truncated]" : "");
    }

    private static String sha256(Path path) throws IOException {
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) if (read > 0) digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private ReportUploadResult result(ReportUploadStatus status, ReportArtifactKind kind, String endpoint,
                                      Integer httpStatus, int attempts, long started, long size, String sha,
                                      String serverId, String preview, ReportUploadFailureCategory category,
                                      Throwable failure, String message) {
        String safePreview = options.redactDiagnostic(preview);
        String safeMessage = options.redactDiagnostic(message);
        return new ReportUploadResult(status, kind, endpoint, httpStatus, attempts,
                Duration.ofNanos(Math.max(0, System.nanoTime() - started)), size, sha,
                serverId, safePreview, category, failure, safeMessage);
    }

    private String safeMessage(String prefix, Throwable failure) {
        String detail = failure == null ? "" : failure.getMessage();
        return options.redactDiagnostic(prefix + (detail == null || detail.isBlank() ? "" : ": " + detail));
    }

    private static String bounded(String value, int maximum) {
        if (value == null) return null;
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }

    private static boolean safeEntryName(String name) {
        if (name == null || name.isBlank() || name.startsWith("/") || name.startsWith("\\") || name.contains("\\")) return false;
        Path path = Path.of(name).normalize();
        return !path.isAbsolute() && !path.startsWith("..") && path.toString().replace('\\', '/').equals(name);
    }

    private static String testLensVersion() {
        String implementation = ReportUploader.class.getPackage().getImplementationVersion();
        if (implementation != null && !implementation.isBlank()) return implementation;
        try (InputStream input = ReportUploader.class.getClassLoader().getResourceAsStream(
                "META-INF/maven/io.github.test-lens/selenium-test-lens/pom.properties")) {
            if (input != null) {
                Properties properties = new Properties();
                properties.load(input);
                String version = properties.getProperty("version");
                if (version != null && !version.isBlank()) return version;
            }
        } catch (IOException ignored) { }
        return "development";
    }

    private record PreparedArtifact(Path path, ReportArtifactKind kind, boolean temporary) { }
    private record SourceEntry(String name, Path path, long size, String sha256) { }

    private static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final int maximumBytes;
        private final ByteArrayOutputStream bytes;
        private final CompletableFuture<byte[]> body = new CompletableFuture<>();
        private Flow.Subscription subscription;

        private LimitedBodySubscriber(int previewLimit) {
            maximumBytes = Math.addExact(previewLimit, 1);
            bytes = new ByteArrayOutputStream(Math.min(maximumBytes, 8192));
        }

        @Override public CompletionStage<byte[]> getBody() { return body; }

        @Override public void onSubscribe(Flow.Subscription value) {
            if (subscription != null) {
                value.cancel();
                return;
            }
            subscription = value;
            value.request(1);
        }

        @Override public void onNext(List<ByteBuffer> buffers) {
            if (body.isDone()) return;
            for (ByteBuffer buffer : buffers) {
                while (buffer.hasRemaining() && bytes.size() < maximumBytes) {
                    int count = Math.min(buffer.remaining(), Math.min(8192, maximumBytes - bytes.size()));
                    byte[] chunk = new byte[count];
                    buffer.get(chunk);
                    bytes.writeBytes(chunk);
                }
                if (bytes.size() == maximumBytes) {
                    subscription.cancel();
                    body.complete(bytes.toByteArray());
                    return;
                }
            }
            subscription.request(1);
        }

        @Override public void onError(Throwable failure) { body.completeExceptionally(failure); }

        @Override public void onComplete() { body.complete(bytes.toByteArray()); }
    }

    private static final class FixedProxySelector extends ProxySelector {
        private final InetSocketAddress proxy;
        private final List<NoProxyRule> noProxy;

        private FixedProxySelector(InetSocketAddress proxy, List<String> rules) {
            this.proxy = proxy;
            noProxy = rules.stream().map(NoProxyRule::parse).toList();
        }

        @Override public List<Proxy> select(URI uri) {
            if (uri == null) throw new IllegalArgumentException("URI must not be null");
            if (proxy == null || noProxy.stream().anyMatch(rule -> rule.matches(uri))) return List.of(Proxy.NO_PROXY);
            return List.of(new Proxy(Proxy.Type.HTTP, proxy));
        }

        @Override public void connectFailed(URI uri, SocketAddress address, IOException failure) { }
    }

    private record NoProxyRule(boolean all, boolean suffix, String host, Integer port) {
        private static NoProxyRule parse(String raw) {
            if ("*".equals(raw)) return new NoProxyRule(true, false, "", null);
            String value = raw;
            Integer port = null;
            String host;
            if (value.startsWith("[")) {
                int close = value.indexOf(']');
                if (close < 0) throw new IllegalArgumentException("Invalid bracketed IPv6 no-proxy rule");
                host = value.substring(1, close);
                if (close + 1 < value.length()) {
                    if (value.charAt(close + 1) != ':') throw new IllegalArgumentException("Invalid no-proxy rule");
                    port = parsePort(value.substring(close + 2));
                }
            } else {
                int first = value.indexOf(':');
                int last = value.lastIndexOf(':');
                if (first > 0 && first == last) {
                    host = value.substring(0, first);
                    port = parsePort(value.substring(first + 1));
                } else host = value;
            }
            boolean suffix = host.startsWith(".") || host.startsWith("*.");
            if (host.startsWith("*.")) host = host.substring(1);
            if (host.startsWith(".")) host = host.substring(1);
            if (host.isBlank()) throw new IllegalArgumentException("Invalid no-proxy host");
            return new NoProxyRule(false, suffix, host.toLowerCase(Locale.ROOT), port);
        }

        private boolean matches(URI uri) {
            if (all) return true;
            String candidate = uri.getHost();
            if (candidate == null) return false;
            candidate = candidate.toLowerCase(Locale.ROOT);
            if (candidate.startsWith("[") && candidate.endsWith("]")) {
                candidate = candidate.substring(1, candidate.length() - 1);
            }
            int candidatePort = uri.getPort() >= 0 ? uri.getPort()
                    : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            if (port != null && port != candidatePort) return false;
            return suffix ? candidate.equals(host) || candidate.endsWith("." + host) : candidate.equals(host);
        }

        private static int parsePort(String text) {
            try {
                int parsed = Integer.parseInt(text);
                if (parsed < 1 || parsed > 65_535) throw new IllegalArgumentException("Invalid no-proxy port");
                return parsed;
            } catch (NumberFormatException failure) { throw new IllegalArgumentException("Invalid no-proxy port", failure); }
        }
    }
}
