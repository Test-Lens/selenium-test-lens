package io.github.testlens.selenium.network;

import java.time.Instant;

public final class NetworkFailure {
    private final String requestId;
    private final String url;
    private final String message;
    private final String failureType;
    private final Instant timestamp;

    public NetworkFailure(String requestId, String url, String message, String failureType, Instant timestamp) {
        this.requestId = requestId == null ? "" : requestId;
        this.url = url == null ? "" : url;
        this.message = message == null ? "" : message;
        this.failureType = failureType == null ? "" : failureType;
        this.timestamp = timestamp == null ? Instant.now() : timestamp;
    }

    public static NetworkFailure of(String requestId, String url, String message) {
        return new NetworkFailure(requestId, url, message, "", Instant.now());
    }

    public String requestId() { return requestId; }
    public String url() { return url; }
    public String message() { return message; }
    public String failureType() { return failureType; }
    public Instant timestamp() { return timestamp; }
}

