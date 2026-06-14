package io.github.mmaciekk111.uitestlens.selenium.evidence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class VideoEvidenceOptions {
    private final VideoEvidenceSource source;
    private final String mediaType;
    private final boolean validateLocalFileExists;
    private final boolean attachToSession;
    private final Map<String, String> metadata;

    private VideoEvidenceOptions(Builder builder) {
        this.source = builder.source == null ? VideoEvidenceSource.CUSTOM : builder.source;
        this.mediaType = builder.mediaType == null || builder.mediaType.isBlank()
                ? "video/mp4"
                : builder.mediaType.trim();
        this.validateLocalFileExists = builder.validateLocalFileExists;
        this.attachToSession = builder.attachToSession;
        this.metadata = immutableCopy(builder.metadata);
    }

    public static VideoEvidenceOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public VideoEvidenceSource source() {
        return source;
    }

    public String mediaType() {
        return mediaType;
    }

    public boolean validateLocalFileExists() {
        return validateLocalFileExists;
    }

    public boolean attachToSession() {
        return attachToSession;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    private static Map<String, String> immutableCopy(Map<String, String> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null) {
                copy.put(entry.getKey().trim(), entry.getValue());
            }
        }
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }

    public static final class Builder {
        private VideoEvidenceSource source = VideoEvidenceSource.CUSTOM;
        private String mediaType = "video/mp4";
        private boolean validateLocalFileExists;
        private boolean attachToSession = true;
        private final Map<String, String> metadata = new LinkedHashMap<>();

        private Builder() {}

        public Builder source(VideoEvidenceSource source) {
            this.source = source;
            return this;
        }

        public Builder mediaType(String mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public Builder validateLocalFileExists(boolean validateLocalFileExists) {
            this.validateLocalFileExists = validateLocalFileExists;
            return this;
        }

        public Builder attachToSession(boolean attachToSession) {
            this.attachToSession = attachToSession;
            return this;
        }

        public Builder metadata(String key, String value) {
            if (key != null && !key.isBlank() && value != null) {
                metadata.put(key.trim(), value);
            }
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            if (metadata != null) {
                metadata.forEach(this::metadata);
            }
            return this;
        }

        public VideoEvidenceOptions build() {
            return new VideoEvidenceOptions(this);
        }
    }
}
