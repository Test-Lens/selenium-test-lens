package io.github.testlens.core.trace;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TraceMetadata {
    private final String sessionId;
    private final String name;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final TraceStatus status;
    private final String environment;
    private final Map<String, String> labels;

    private TraceMetadata(Builder builder) {
        this.sessionId = builder.sessionId == null ? "" : builder.sessionId;
        this.name = builder.name == null ? "" : builder.name;
        this.startedAt = builder.startedAt == null ? Instant.now() : builder.startedAt;
        this.finishedAt = builder.finishedAt;
        this.status = builder.status == null ? TraceStatus.STARTED : builder.status;
        this.environment = builder.environment == null ? "" : builder.environment;
        this.labels = immutableCopy(builder.labels);
    }

    public static Builder builder(String sessionId, String name) {
        return new Builder(sessionId, name);
    }

    public Builder toBuilder() {
        return new Builder(sessionId, name)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .status(status)
                .environment(environment)
                .labels(labels);
    }

    public String sessionId() {
        return sessionId;
    }

    public String name() {
        return name;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public TraceStatus status() {
        return status;
    }

    public String environment() {
        return environment;
    }

    public Map<String, String> labels() {
        return labels;
    }

    public static final class Builder {
        private final String sessionId;
        private final String name;
        private Instant startedAt = Instant.now();
        private Instant finishedAt;
        private TraceStatus status = TraceStatus.STARTED;
        private String environment = "";
        private Map<String, String> labels = Map.of();

        private Builder(String sessionId, String name) {
            this.sessionId = sessionId;
            this.name = name;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder finishedAt(Instant finishedAt) {
            this.finishedAt = finishedAt;
            return this;
        }

        public Builder status(TraceStatus status) {
            this.status = status;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public Builder label(String key, String value) {
            if (key == null || key.isBlank() || value == null) {
                return this;
            }
            Map<String, String> copy = new LinkedHashMap<>(labels);
            copy.put(key, value);
            labels = Collections.unmodifiableMap(copy);
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            this.labels = immutableCopy(labels);
            return this;
        }

        public TraceMetadata build() {
            return new TraceMetadata(this);
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
}

