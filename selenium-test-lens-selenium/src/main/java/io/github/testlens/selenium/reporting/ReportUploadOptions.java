package io.github.testlens.selenium.reporting;

import io.github.testlens.core.redaction.RedactionPolicy;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Immutable HTTP, retry, proxy, and diagnostic limits for explicit report upload. */
public final class ReportUploadOptions {
    /** Default upper bound for one ZIP payload: 100 MiB. */
    public static final long DEFAULT_MAX_PAYLOAD_BYTES = 100L * 1024 * 1024;
    /** Default upper bound for a redacted server-response preview: 16 KiB. */
    public static final int DEFAULT_MAX_RESPONSE_PREVIEW_BYTES = 16 * 1024;
    private static final Pattern HEADER_NAME = Pattern.compile("[!#$%&'*+.^_`|~0-9A-Za-z-]+");
    private static final java.util.Set<String> MANAGED_HEADERS = java.util.Set.of(
            "authorization", "proxy-authorization", "content-type", "content-length", "host", "idempotency-key",
            "x-test-lens-schema-version", "x-test-lens-artifact-kind", "x-test-lens-status", "x-test-lens-sha256");

    private final URI endpoint;
    private final String bearerToken;
    private final Map<String, String> headers;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final long maxPayloadBytes;
    private final int maxAttempts;
    private final Duration maxRetryAfter;
    private final int maxResponsePreviewBytes;
    private final ReportProxyOptions proxy;
    private final RedactionPolicy redactionPolicy;

    private ReportUploadOptions(Builder builder) {
        endpoint = validateEndpoint(builder.endpoint);
        bearerToken = builder.bearerToken;
        headers = Collections.unmodifiableMap(new TreeMap<>(builder.headers));
        connectTimeout = builder.connectTimeout;
        requestTimeout = builder.requestTimeout;
        maxPayloadBytes = builder.maxPayloadBytes;
        maxAttempts = builder.maxAttempts;
        maxRetryAfter = builder.maxRetryAfter;
        maxResponsePreviewBytes = builder.maxResponsePreviewBytes;
        proxy = builder.proxy;
        redactionPolicy = builder.redactionPolicy;
    }

    /** Creates a builder whose proxy mode is {@link ReportProxyMode#SYSTEM}. */
    public static Builder builder() { return new Builder(); }
    /** Returns the configured HTTP(S) endpoint. */
    public URI endpoint() { return endpoint; }
    /** Returns the HTTP connection timeout. */
    public Duration connectTimeout() { return connectTimeout; }
    /** Returns the timeout applied to each request attempt. */
    public Duration requestTimeout() { return requestTimeout; }
    /** Returns the maximum accepted ZIP size in bytes. */
    public long maxPayloadBytes() { return maxPayloadBytes; }
    /** Returns the total allowed attempt count. */
    public int maxAttempts() { return maxAttempts; }
    /** Returns the upper bound applied to an integer-seconds {@code Retry-After}. */
    public Duration maxRetryAfter() { return maxRetryAfter; }
    /** Returns the maximum number of response bytes decoded for diagnostics. */
    public int maxResponsePreviewBytes() { return maxResponsePreviewBytes; }
    /** Returns uploader-only proxy selection. */
    public ReportProxyOptions proxy() { return proxy; }
    /** Returns the policy protecting response and failure diagnostics. */
    public RedactionPolicy redactionPolicy() { return redactionPolicy; }

    String bearerTokenValue() { return bearerToken; }
    Map<String, String> headerValues() { return headers; }

    String redactDiagnostic(String input) {
        String safe = redactionPolicy.redact(input);
        if (safe == null) return null;
        if (bearerToken != null && !bearerToken.isEmpty()) {
            safe = safe.replace(bearerToken, redactionPolicy.replacement());
        }
        for (String value : headers.values()) {
            if (!value.isEmpty()) safe = safe.replace(value, redactionPolicy.replacement());
        }
        return safe;
    }

    @Override public String toString() {
        return "ReportUploadOptions[endpoint=" + redactDiagnostic(safeEndpoint(endpoint)) + ", tokenConfigured="
                + (bearerToken != null) + ", customHeaders=" + headers.size() + ", maxAttempts=" + maxAttempts
                + ", proxy=" + proxy + "]";
    }

    /** Builds report upload options. Header and credential values are intentionally absent from {@code toString()}. */
    public static final class Builder {
        private URI endpoint;
        private String bearerToken;
        private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration requestTimeout = Duration.ofSeconds(30);
        private long maxPayloadBytes = DEFAULT_MAX_PAYLOAD_BYTES;
        private int maxAttempts = 1;
        private Duration maxRetryAfter = Duration.ofSeconds(30);
        private int maxResponsePreviewBytes = DEFAULT_MAX_RESPONSE_PREVIEW_BYTES;
        private ReportProxyOptions proxy = ReportProxyOptions.system();
        private RedactionPolicy redactionPolicy = RedactionPolicy.defaults();

        private Builder() { }

        /** Sets the required absolute HTTP(S) receiver URI. */
        public Builder endpoint(URI value) { endpoint = validateEndpoint(value); return this; }

        /** Sets an optional bearer credential; blank removes it. */
        public Builder bearerToken(String value) {
            if (value == null || value.isBlank()) { bearerToken = null; return this; }
            if (containsLineBreak(value)) throw new IllegalArgumentException("bearer token must not contain line breaks");
            bearerToken = value;
            return this;
        }

        /** Adds a validated custom header that is not managed by Test Lens. */
        public Builder header(String name, String value) {
            if (name == null || !HEADER_NAME.matcher(name).matches()) throw new IllegalArgumentException("Invalid HTTP header name");
            if (MANAGED_HEADERS.contains(name.toLowerCase(java.util.Locale.ROOT))) {
                throw new IllegalArgumentException("Header is managed by Test Lens: " + name);
            }
            if (value == null || containsLineBreak(value)) throw new IllegalArgumentException("Invalid HTTP header value");
            headers.put(name, value);
            return this;
        }

        /** Sets the positive connect timeout. */
        public Builder connectTimeout(Duration value) { connectTimeout = positive(value, "connectTimeout"); return this; }
        /** Sets the positive timeout for each request attempt. */
        public Builder requestTimeout(Duration value) { requestTimeout = positive(value, "requestTimeout"); return this; }
        /** Sets the positive ZIP payload limit in bytes. */
        public Builder maxPayloadBytes(long value) {
            if (value < 1) throw new IllegalArgumentException("maxPayloadBytes must be positive");
            maxPayloadBytes = value; return this;
        }
        /** Sets the positive total attempt count; {@code 1} disables retry. */
        public Builder maxAttempts(int value) {
            if (value < 1) throw new IllegalArgumentException("maxAttempts must be positive");
            maxAttempts = value; return this;
        }
        /** Sets the non-negative cap applied to integer-seconds {@code Retry-After}. */
        public Builder maxRetryAfter(Duration value) {
            if (value == null || value.isNegative()) throw new IllegalArgumentException("maxRetryAfter must not be negative");
            maxRetryAfter = value; return this;
        }
        /** Sets the non-negative response-preview byte limit. */
        public Builder maxResponsePreviewBytes(int value) {
            if (value < 0) throw new IllegalArgumentException("maxResponsePreviewBytes must not be negative");
            maxResponsePreviewBytes = value; return this;
        }
        /** Sets uploader-only proxy behavior. */
        public Builder proxy(ReportProxyOptions value) {
            if (value == null) throw new IllegalArgumentException("proxy must not be null");
            proxy = value; return this;
        }
        /** Sets the policy applied to bounded transport diagnostics. */
        public Builder redactionPolicy(RedactionPolicy value) {
            if (value == null) throw new IllegalArgumentException("redactionPolicy must not be null");
            redactionPolicy = value; return this;
        }
        /** Builds immutable upload options. */
        public ReportUploadOptions build() { return new ReportUploadOptions(this); }
    }

    static String safeEndpoint(URI endpoint) {
        if (endpoint == null) return "endpoint[unconfigured]";
        String host = endpoint.getHost();
        return endpoint.getScheme() + "://" + (host != null && host.contains(":") ? "[" + host + "]" : host)
                + (endpoint.getPort() >= 0 ? ":" + endpoint.getPort() : "")
                + (endpoint.getRawPath() == null || endpoint.getRawPath().isEmpty() ? "/" : endpoint.getRawPath());
    }

    private static URI validateEndpoint(URI value) {
        if (value == null) throw new IllegalArgumentException("endpoint must not be null");
        String scheme = value.getScheme();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || value.getHost() == null) {
            throw new IllegalArgumentException("endpoint must be an absolute HTTP or HTTPS URI with a host");
        }
        if (value.getRawUserInfo() != null) throw new IllegalArgumentException("endpoint must not contain userinfo");
        if (value.getRawFragment() != null) throw new IllegalArgumentException("endpoint must not contain a fragment");
        return value;
    }

    private static Duration positive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }
    private static boolean containsLineBreak(String value) { return value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0; }
}
