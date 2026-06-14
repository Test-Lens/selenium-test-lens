package io.github.mmaciekk111.uitestlens.selenium.evidence;

import io.github.mmaciekk111.uitestlens.core.trace.TraceArtifact;
import io.github.mmaciekk111.uitestlens.core.trace.TraceArtifactType;
import io.github.mmaciekk111.uitestlens.core.trace.UiTestLensSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class VideoEvidence {
    public VideoEvidenceResult attachFile(String name,
                                          Path path,
                                          VideoEvidenceOptions options,
                                          UiTestLensSession session) {
        VideoEvidenceOptions effectiveOptions = options == null ? VideoEvidenceOptions.defaults() : options;
        String effectiveName = normalizeName(name);
        if (path == null) {
            return VideoEvidenceResult.failed(effectiveName, null, "", effectiveOptions.source(),
                    "Video path must not be null", null, metadataFor(effectiveOptions));
        }
        if (effectiveOptions.validateLocalFileExists() && !Files.exists(path)) {
            return VideoEvidenceResult.failed(effectiveName, path, "", effectiveOptions.source(),
                    "Video file does not exist: " + path, null, metadataFor(effectiveOptions));
        }
        TraceArtifact artifact = TraceArtifact.video(effectiveName, path);
        return attachReference(effectiveName, path, "", artifact, effectiveOptions, session);
    }

    public VideoEvidenceResult attachUrl(String name,
                                         String url,
                                         VideoEvidenceOptions options,
                                         UiTestLensSession session) {
        VideoEvidenceOptions effectiveOptions = options == null ? VideoEvidenceOptions.defaults() : options;
        String effectiveName = normalizeName(name);
        if (url == null || url.isBlank()) {
            return VideoEvidenceResult.failed(effectiveName, null, "", effectiveOptions.source(),
                    "Video URL must not be blank", null, metadataFor(effectiveOptions));
        }
        TraceArtifact artifact = TraceArtifact.url(effectiveName, TraceArtifactType.VIDEO, url.trim());
        return attachReference(effectiveName, null, url.trim(), artifact, effectiveOptions, session);
    }

    private static VideoEvidenceResult attachReference(String name,
                                                       Path path,
                                                       String url,
                                                       TraceArtifact artifact,
                                                       VideoEvidenceOptions options,
                                                       UiTestLensSession session) {
        Map<String, String> metadata = metadataFor(options);
        TraceArtifact enriched = artifact;
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            enriched = enriched.withMetadata(entry.getKey(), entry.getValue());
        }
        if (options.attachToSession()) {
            if (session == null) {
                return VideoEvidenceResult.skipped(name, path, url, options.source(), "No UiTestLensSession attached", metadata);
            }
            TraceArtifact attached = session.attachArtifact(enriched);
            return VideoEvidenceResult.attached(name, path, url, attached, options.source(), "Video evidence attached", metadata);
        }
        return VideoEvidenceResult.attached(name, path, url, null, options.source(), "Video evidence reference prepared without session attach", metadata);
    }

    private static Map<String, String> metadataFor(VideoEvidenceOptions options) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("video.source", options.source().name());
        metadata.put("video.mediaType", options.mediaType());
        metadata.put("video.attachedAt", Instant.now().toString());
        metadata.putAll(options.metadata());
        return metadata;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("video name must not be blank");
        }
        return name.trim();
    }
}
