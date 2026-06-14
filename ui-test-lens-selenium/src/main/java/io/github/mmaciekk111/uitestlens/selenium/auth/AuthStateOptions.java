package io.github.mmaciekk111.uitestlens.selenium.auth;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AuthStateOptions {
    private final String label;
    private final String role;
    private final String origin;
    private final boolean includeCookies;
    private final boolean includeLocalStorage;
    private final boolean includeSessionStorage;
    private final Instant expiresAt;
    private final Map<String, String> labels;
    private final Map<String, String> notes;

    private AuthStateOptions(Builder builder) {
        this.label = safe(builder.label);
        this.role = safe(builder.role);
        this.origin = safe(builder.origin);
        this.includeCookies = builder.includeCookies;
        this.includeLocalStorage = builder.includeLocalStorage;
        this.includeSessionStorage = builder.includeSessionStorage;
        this.expiresAt = builder.expiresAt;
        this.labels = Map.copyOf(builder.labels);
        this.notes = Map.copyOf(builder.notes);
    }

    public static AuthStateOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
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

    public boolean includeCookies() {
        return includeCookies;
    }

    public boolean includeLocalStorage() {
        return includeLocalStorage;
    }

    public boolean includeSessionStorage() {
        return includeSessionStorage;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Map<String, String> labels() {
        return labels;
    }

    public Map<String, String> notes() {
        return notes;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Builder {
        private String label;
        private String role;
        private String origin;
        private boolean includeCookies = true;
        private boolean includeLocalStorage = true;
        private boolean includeSessionStorage = true;
        private Instant expiresAt;
        private final Map<String, String> labels = new LinkedHashMap<>();
        private final Map<String, String> notes = new LinkedHashMap<>();

        private Builder() {}

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

        public Builder includeCookies(boolean includeCookies) {
            this.includeCookies = includeCookies;
            return this;
        }

        public Builder includeLocalStorage(boolean includeLocalStorage) {
            this.includeLocalStorage = includeLocalStorage;
            return this;
        }

        public Builder includeSessionStorage(boolean includeSessionStorage) {
            this.includeSessionStorage = includeSessionStorage;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
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

        public AuthStateOptions build() {
            return new AuthStateOptions(this);
        }
    }
}
