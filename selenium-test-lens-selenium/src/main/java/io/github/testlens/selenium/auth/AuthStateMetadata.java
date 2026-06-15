package io.github.testlens.selenium.auth;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class AuthStateMetadata {
    private final String id;
    private final String label;
    private final String role;
    private final String origin;
    private final String domain;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final String createdBy;
    private final Map<String, String> labels;
    private final Map<String, String> notes;

    private AuthStateMetadata(Builder builder) {
        this.id = blankToDefault(builder.id, UUID.randomUUID().toString());
        this.label = safe(builder.label);
        this.role = safe(builder.role);
        this.origin = safe(builder.origin);
        this.domain = safe(builder.domain);
        this.createdAt = builder.createdAt == null ? Instant.now() : builder.createdAt;
        this.expiresAt = builder.expiresAt;
        this.createdBy = blankToDefault(builder.createdBy, "selenium-test-lens");
        this.labels = immutableCopy(builder.labels);
        this.notes = immutableCopy(builder.notes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder()
                .id(id)
                .label(label)
                .role(role)
                .origin(origin)
                .domain(domain)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .createdBy(createdBy)
                .labels(labels)
                .notes(notes);
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public String role() {
        return role;
    }

    public String origin() {
        return origin;
    }

    public String domain() {
        return domain;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public String createdBy() {
        return createdBy;
    }

    public Map<String, String> labels() {
        return labels;
    }

    public Map<String, String> notes() {
        return notes;
    }

    private static Map<String, String> immutableCopy(Map<String, String> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                copy.put(key.trim(), value);
            }
        });
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public static final class Builder {
        private String id;
        private String label;
        private String role;
        private String origin;
        private String domain;
        private Instant createdAt;
        private Instant expiresAt;
        private String createdBy = "selenium-test-lens";
        private final Map<String, String> labels = new LinkedHashMap<>();
        private final Map<String, String> notes = new LinkedHashMap<>();

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder origin(String origin) {
            this.origin = origin;
            return this;
        }

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder labelEntry(String key, String value) {
            if (key != null && !key.isBlank() && value != null) {
                labels.put(key.trim(), value);
            }
            return this;
        }

        public Builder note(String key, String value) {
            if (key != null && !key.isBlank() && value != null) {
                notes.put(key.trim(), value);
            }
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            if (labels != null) {
                labels.forEach(this::labelEntry);
            }
            return this;
        }

        public Builder notes(Map<String, String> notes) {
            if (notes != null) {
                notes.forEach(this::note);
            }
            return this;
        }

        public AuthStateMetadata build() {
            return new AuthStateMetadata(this);
        }
    }
}
