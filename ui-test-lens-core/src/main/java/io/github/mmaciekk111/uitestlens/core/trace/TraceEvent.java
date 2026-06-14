package io.github.mmaciekk111.uitestlens.core.trace;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TraceEvent {
    private final String id;
    private final TraceEventType type;
    private final TraceStatus status;
    private final String name;
    private final String message;
    private final Instant timestamp;
    private final Duration duration;
    private final String parentId;
    private final TraceFailure failure;
    private final List<TraceArtifact> artifacts;
    private final Map<String, String> attributes;

    private TraceEvent(Builder builder) {
        this.id = builder.id == null || builder.id.isBlank() ? UUID.randomUUID().toString() : builder.id;
        this.type = builder.type == null ? TraceEventType.CUSTOM : builder.type;
        this.status = builder.status == null ? TraceStatus.INFO : builder.status;
        this.name = safe(builder.name);
        this.message = safe(builder.message);
        this.timestamp = builder.timestamp == null ? Instant.now() : builder.timestamp;
        this.duration = builder.duration == null || builder.duration.isNegative() ? Duration.ZERO : builder.duration;
        this.parentId = safe(builder.parentId);
        this.failure = builder.failure;
        this.artifacts = List.copyOf(builder.artifacts);
        this.attributes = immutableCopy(builder.attributes);
    }

    public static Builder builder(TraceEventType type, TraceStatus status, String name) {
        return new Builder().type(type).status(status).name(name);
    }

    public static TraceEvent started(TraceEventType type, String name) {
        return builder(type, TraceStatus.STARTED, name).build();
    }

    public static TraceEvent passed(TraceEventType type, String name, Duration duration) {
        return builder(type, TraceStatus.PASSED, name).duration(duration).build();
    }

    public static TraceEvent failed(TraceEventType type, String name, Throwable throwable, Duration duration) {
        return builder(type, TraceStatus.FAILED, name)
                .duration(duration)
                .failure(TraceFailure.from(throwable, false))
                .message(throwable == null ? "" : throwable.getMessage())
                .build();
    }

    public static TraceEvent info(String name, String message) {
        return builder(TraceEventType.CUSTOM, TraceStatus.INFO, name).message(message).build();
    }

    public static TraceEvent custom(String name, String message) {
        return info(name, message);
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .type(type)
                .status(status)
                .name(name)
                .message(message)
                .timestamp(timestamp)
                .duration(duration)
                .parentId(parentId)
                .failure(failure)
                .artifacts(artifacts)
                .attributes(attributes);
    }

    public String id() {
        return id;
    }

    public TraceEventType type() {
        return type;
    }

    public TraceStatus status() {
        return status;
    }

    public String name() {
        return name;
    }

    public String message() {
        return message;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public Duration duration() {
        return duration;
    }

    public String parentId() {
        return parentId;
    }

    public TraceFailure failure() {
        return failure;
    }

    public List<TraceArtifact> artifacts() {
        return artifacts;
    }

    public Map<String, String> attributes() {
        return attributes;
    }

    public static final class Builder {
        private String id;
        private TraceEventType type = TraceEventType.CUSTOM;
        private TraceStatus status = TraceStatus.INFO;
        private String name = "";
        private String message = "";
        private Instant timestamp;
        private Duration duration = Duration.ZERO;
        private String parentId = "";
        private TraceFailure failure;
        private List<TraceArtifact> artifacts = List.of();
        private Map<String, String> attributes = Map.of();

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(TraceEventType type) {
            this.type = type;
            return this;
        }

        public Builder status(TraceStatus status) {
            this.status = status;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        public Builder parentId(String parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder failure(TraceFailure failure) {
            this.failure = failure;
            return this;
        }

        public Builder artifact(TraceArtifact artifact) {
            if (artifact == null) {
                return this;
            }
            List<TraceArtifact> copy = new ArrayList<>(this.artifacts);
            copy.add(artifact);
            this.artifacts = Collections.unmodifiableList(copy);
            return this;
        }

        public Builder artifacts(List<TraceArtifact> artifacts) {
            this.artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
            return this;
        }

        public Builder attribute(String key, String value) {
            if (key == null || key.isBlank() || value == null) {
                return this;
            }
            Map<String, String> copy = new LinkedHashMap<>(this.attributes);
            copy.put(key, value);
            this.attributes = Collections.unmodifiableMap(copy);
            return this;
        }

        public Builder attributes(Map<String, String> attributes) {
            this.attributes = immutableCopy(attributes);
            return this;
        }

        public TraceEvent build() {
            Objects.requireNonNull(type, "type must not be null");
            return new TraceEvent(this);
        }
    }

    private static Map<String, String> immutableCopy(Map<String, String> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
