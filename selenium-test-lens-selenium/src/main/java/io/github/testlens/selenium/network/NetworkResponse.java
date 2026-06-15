package io.github.testlens.selenium.network;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public final class NetworkResponse {
    private final String requestId;
    private final String url;
    private final int status;
    private final String statusText;
    private final String mimeType;
    private final Duration duration;
    private final Instant timestamp;
    private final Map<String, String> headers;

    public NetworkResponse(String requestId, String url, int status, String statusText, String mimeType,
                           Duration duration, Instant timestamp, Map<String, String> headers) {
        this.requestId = requestId == null ? "" : requestId;
        this.url = url == null ? "" : url;
        this.status = status;
        this.statusText = statusText == null ? "" : statusText;
        this.mimeType = mimeType == null ? "" : mimeType;
        this.duration = duration == null ? Duration.ZERO : duration;
        this.timestamp = timestamp == null ? Instant.now() : timestamp;
        this.headers = NetworkRequest.immutableCopy(headers);
    }

    public static NetworkResponse of(String requestId, String url, int status) {
        return new NetworkResponse(requestId, url, status, "", "", Duration.ZERO, Instant.now(), Map.of());
    }

    public String requestId() { return requestId; }
    public String url() { return url; }
    public int status() { return status; }
    public String statusText() { return statusText; }
    public String mimeType() { return mimeType; }
    public Duration duration() { return duration; }
    public Instant timestamp() { return timestamp; }
    public Map<String, String> headers() { return headers; }
}
