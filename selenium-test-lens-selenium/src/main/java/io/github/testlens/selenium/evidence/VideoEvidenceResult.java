package io.github.testlens.selenium.evidence;

import io.github.testlens.core.trace.TraceArtifact;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class VideoEvidenceResult {
    private final VideoEvidenceStatus status;
    private final String name;
    private final Path path;
    private final String url;
    private final TraceArtifact artifact;
    private final VideoEvidenceSource source;
    private final String message;
    private final Throwable exception;
    private final Instant attachedAt;
    private final Map<String, String> metadata;

    private VideoEvidenceResult(VideoEvidenceStatus status,
                                String name,
                                Path path,
                                String url,
                                TraceArtifact artifact,
                                VideoEvidenceSource source,
                                String message,
                                Throwable exception,
                                Instant attachedAt,
                                Map<String, String> metadata) {
        this.status = status == null ? VideoEvidenceStatus.FAILED : status;
        this.name = name == null ? "" : name;
        this.path = path;
        this.url = url == null ? "" : url;
        this.artifact = artifact;
        this.source = source == null ? VideoEvidenceSource.CUSTOM : source;
        this.message = message == null ? "" : message;
        this.exception = exception;
        this.attachedAt = attachedAt == null ? Instant.now() : attachedAt;
        this.metadata = immutableCopy(metadata);
    }

    public static VideoEvidenceResult attached(String name,
                                               Path path,
                                               String url,
                                               TraceArtifact artifact,
                                               VideoEvidenceSource source,
                                               String message,
                                               Map<String, String> metadata) {
        return new VideoEvidenceResult(VideoEvidenceStatus.ATTACHED, name, path, url, artifact, source, message, null, Instant.now(), metadata);
    }

    public static VideoEvidenceResult failed(String name,
                                             Path path,
                                             String url,
                                             VideoEvidenceSource source,
                                             String message,
                                             Throwable exception,
                                             Map<String, String> metadata) {
        return new VideoEvidenceResult(VideoEvidenceStatus.FAILED, name, path, url, null, source, message, exception, Instant.now(), metadata);
    }

    public static VideoEvidenceResult skipped(String name,
                                              Path path,
                                              String url,
                                              VideoEvidenceSource source,
                                              String message,
                                              Map<String, String> metadata) {
        return new VideoEvidenceResult(VideoEvidenceStatus.SKIPPED, name, path, url, null, source, message, null, Instant.now(), metadata);
    }

    public VideoEvidenceStatus status() {
        return status;
    }

    public String name() {
        return name;
    }

    public Path path() {
        return path;
    }

    public String url() {
        return url;
    }

    public TraceArtifact artifact() {
        return artifact;
    }

    public VideoEvidenceSource source() {
        return source;
    }

    public String message() {
        return message;
    }

    public Throwable exception() {
        return exception;
    }

    public Instant attachedAt() {
        return attachedAt;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public boolean isAttached() {
        return status == VideoEvidenceStatus.ATTACHED;
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

