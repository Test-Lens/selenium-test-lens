package io.github.mmaciekk111.uitestlens.core.trace;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Reference to evidence produced or attached during a trace session.
 *
 * <p>The artifact stores paths or URLs only; binary content is not embedded.
 */
public final class TraceArtifact {
    private final String name;
    private final TraceArtifactType type;
    private final String path;
    private final String url;
    private final String mediaType;
    private final Instant createdAt;
    private final Map<String, String> metadata;

    private TraceArtifact(String name,
                          TraceArtifactType type,
                          String path,
                          String url,
                          String mediaType,
                          Instant createdAt,
                          Map<String, String> metadata) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("artifact name must not be blank");
        }
        this.name = name.trim();
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.path = safe(path);
        this.url = safe(url);
        this.mediaType = safe(mediaType);
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.metadata = immutableCopy(metadata);
    }

    public static TraceArtifact screenshot(String name, Path path) {
        return customPath(name, TraceArtifactType.SCREENSHOT, path, "image/png");
    }

    public static TraceArtifact video(String name, Path path) {
        return customPath(name, TraceArtifactType.VIDEO, path, "video/mp4");
    }

    public static TraceArtifact url(String name, TraceArtifactType type, String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url must not be blank");
        }
        TraceArtifactType effectiveType = type == null ? TraceArtifactType.CUSTOM_URL : type;
        return new TraceArtifact(name, effectiveType, "", url, "", Instant.now(), Map.of());
    }

    public static TraceArtifact customFile(String name, Path path, String mediaType) {
        return customPath(name, TraceArtifactType.CUSTOM_FILE, path, mediaType);
    }

    public static TraceArtifact networkLog(String name, Path path) {
        return customPath(name, TraceArtifactType.NETWORK_LOG, path, "application/json");
    }

    static TraceArtifact of(String name,
                            TraceArtifactType type,
                            String path,
                            String url,
                            String mediaType,
                            Instant createdAt,
                            Map<String, String> metadata) {
        return new TraceArtifact(name, type, path, url, mediaType, createdAt, metadata);
    }

    private static TraceArtifact customPath(String name, TraceArtifactType type, Path path, String mediaType) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        return new TraceArtifact(name, type, path.toString(), "", mediaType, Instant.now(), Map.of());
    }

    public TraceArtifact withMetadata(String key, String value) {
        if (key == null || key.isBlank() || value == null) {
            return this;
        }
        Map<String, String> copy = new LinkedHashMap<>(metadata);
        copy.put(key, value);
        return new TraceArtifact(name, type, path, url, mediaType, createdAt, copy);
    }

    public String name() {
        return name;
    }

    public TraceArtifactType type() {
        return type;
    }

    public String path() {
        return path;
    }

    public String url() {
        return url;
    }

    public String mediaType() {
        return mediaType;
    }

    public Instant createdAt() {
        return createdAt;
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
