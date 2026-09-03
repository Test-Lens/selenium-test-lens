package io.github.testlens.selenium.network;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class NetworkEvent {
    private final String id;
    private final NetworkEventType type;
    private final NetworkRequest request;
    private final NetworkResponse response;
    private final NetworkFailure failure;
    private final String message;
    private final Instant timestamp;
    private final Map<String, String> attributes;

    private NetworkEvent(NetworkEventType type, NetworkRequest request, NetworkResponse response,
                         NetworkFailure failure, String message, Instant timestamp, Map<String, String> attributes) {
        this.id = UUID.randomUUID().toString();
        this.type = type == null ? NetworkEventType.INFO : type;
        this.request = request;
        this.response = response;
        this.failure = failure;
        this.message = message == null ? "" : message;
        this.timestamp = timestamp == null ? Instant.now() : timestamp;
        this.attributes = NetworkRequest.immutableCopy(attributes);
    }

    public static NetworkEvent request(NetworkRequest request) {
        return new NetworkEvent(NetworkEventType.REQUEST, request, null, null, "Request recorded", Instant.now(), Map.of());
    }

    static NetworkEvent request(NetworkRequest request, Instant timestamp, Map<String, String> attributes) {
        return new NetworkEvent(NetworkEventType.REQUEST, request, null, null, "Request recorded", timestamp, attributes);
    }

    public static NetworkEvent response(NetworkResponse response) {
        return new NetworkEvent(NetworkEventType.RESPONSE, null, response, null, "Response recorded", Instant.now(), Map.of());
    }

    static NetworkEvent response(NetworkResponse response, Instant timestamp, Map<String, String> attributes) {
        return new NetworkEvent(NetworkEventType.RESPONSE, null, response, null, "Response recorded", timestamp, attributes);
    }

    public static NetworkEvent failed(NetworkFailure failure) {
        return new NetworkEvent(NetworkEventType.FAILED, null, null, failure, "Network failure recorded", Instant.now(), Map.of());
    }

    static NetworkEvent failed(NetworkFailure failure, Instant timestamp, Map<String, String> attributes) {
        return new NetworkEvent(NetworkEventType.FAILED, null, null, failure, "Network failure recorded", timestamp, attributes);
    }

    public static NetworkEvent info(String message) {
        return new NetworkEvent(NetworkEventType.INFO, null, null, null, message, Instant.now(), Map.of());
    }

    public static NetworkEvent warning(String message) {
        return new NetworkEvent(NetworkEventType.WARNING, null, null, null, message, Instant.now(), Map.of());
    }

    public String id() { return id; }
    public NetworkEventType type() { return type; }
    public NetworkRequest request() { return request; }
    public NetworkResponse response() { return response; }
    public NetworkFailure failure() { return failure; }
    public String message() { return message; }
    public Instant timestamp() { return timestamp; }
    public Map<String, String> attributes() { return attributes; }

    public String url() {
        if (request != null) return request.url();
        if (response != null) return response.url();
        if (failure != null) return failure.url();
        return "";
    }
}

