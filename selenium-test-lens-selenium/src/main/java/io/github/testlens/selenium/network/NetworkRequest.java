package io.github.testlens.selenium.network;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class NetworkRequest {
    private final String id;
    private final String method;
    private final String url;
    private final String resourceType;
    private final Instant timestamp;
    private final Map<String, String> headers;

    public NetworkRequest(String id, String method, String url, String resourceType, Instant timestamp, Map<String, String> headers) {
        this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        this.method = safe(method);
        this.url = safe(url);
        this.resourceType = safe(resourceType);
        this.timestamp = timestamp == null ? Instant.now() : timestamp;
        this.headers = immutableCopy(headers);
    }

    public static NetworkRequest of(String method, String url) {
        return new NetworkRequest(null, method, url, "", Instant.now(), Map.of());
    }

    public String id() { return id; }
    public String method() { return method; }
    public String url() { return url; }
    public String resourceType() { return resourceType; }
    public Instant timestamp() { return timestamp; }
    public Map<String, String> headers() { return headers; }

    static Map<String, String> immutableCopy(Map<String, String> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null && value != null) {
                copy.put(key, value);
            }
        });
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

