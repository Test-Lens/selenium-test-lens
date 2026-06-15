package io.github.testlens.core.trace;

import io.github.testlens.core.trace.export.TraceHtmlExportOptions;
import io.github.testlens.core.trace.export.TraceHtmlExporter;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * In-memory trace/evidence session for a single UI automation flow.
 *
 * <p>The session collects timeline events and artifact references and can export JSON or HTML reports.
 */
public final class UiTestLensSession {
    private final List<TraceEvent> events = new ArrayList<>();
    private final List<TraceArtifact> artifacts = new ArrayList<>();
    private TraceMetadata metadata;

    private UiTestLensSession(String name) {
        String id = UUID.randomUUID().toString();
        this.metadata = TraceMetadata.builder(id, name == null || name.isBlank() ? "Selenium Test Lens session" : name.trim())
                .status(TraceStatus.STARTED)
                .build();
        addEvent(TraceEvent.started(TraceEventType.SESSION_STARTED, this.metadata.name())
                .toBuilder()
                .attribute("sessionId", id)
                .build());
    }

    public static UiTestLensSession start(String name) {
        return new UiTestLensSession(name);
    }

    public String id() {
        return metadata.sessionId();
    }

    public synchronized TraceMetadata metadata() {
        return metadata;
    }

    public synchronized List<TraceEvent> events() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public synchronized List<TraceArtifact> artifacts() {
        return Collections.unmodifiableList(new ArrayList<>(artifacts));
    }

    public synchronized TraceEvent addEvent(TraceEvent event) {
        if (event == null) {
            return null;
        }
        events.add(event);
        return event;
    }

    public synchronized TraceArtifact attachArtifact(TraceArtifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("artifact must not be null");
        }
        artifacts.add(artifact);
        addEvent(TraceEvent.builder(TraceEventType.ARTIFACT_ATTACHED, TraceStatus.INFO, artifact.name())
                .message("Artifact attached")
                .attribute("artifactType", artifact.type().name())
                .attribute("path", artifact.path())
                .attribute("url", artifact.url())
                .artifact(artifact)
                .build());
        return artifact;
    }

    public TraceArtifact attachScreenshot(String name, Path path) {
        return attachArtifact(TraceArtifact.screenshot(name, path));
    }

    public TraceArtifact attachVideo(String name, Path path) {
        return attachArtifact(TraceArtifact.video(name, path));
    }

    public TraceArtifact attachUrl(String name, TraceArtifactType type, String url) {
        return attachArtifact(TraceArtifact.url(name, type, url));
    }

    public synchronized void finishPassed() {
        finish(TraceStatus.PASSED, null, "");
    }

    public synchronized void finishFailed(Throwable throwable) {
        finish(TraceStatus.FAILED, throwable, "");
    }

    public synchronized void finishSkipped(String reason) {
        finish(TraceStatus.SKIPPED, null, reason);
    }

    public String exportJson() {
        return new TraceJsonExporter().export(this);
    }

    public String exportJson(TraceJsonExportOptions options) {
        return new TraceJsonExporter().export(this, options);
    }

    public Path exportJson(Path outputPath) {
        return new TraceJsonExporter().exportTo(this, outputPath);
    }

    public Path exportJson(Path outputPath, TraceJsonExportOptions options) {
        return new TraceJsonExporter().exportTo(this, outputPath, options);
    }

    public Path exportJsonReport() {
        return new TraceJsonExporter().exportToDefault(this);
    }

    public Path exportJsonReport(TraceJsonExportOptions options) {
        return new TraceJsonExporter().exportToDefault(this, options);
    }

    public String exportHtml() {
        return new TraceHtmlExporter().export(this);
    }

    public String exportHtml(TraceHtmlExportOptions options) {
        return new TraceHtmlExporter().export(this, options);
    }

    public Path exportHtml(Path outputPath) {
        return new TraceHtmlExporter().exportTo(this, outputPath);
    }

    public Path exportHtml(Path outputPath, TraceHtmlExportOptions options) {
        return new TraceHtmlExporter().exportTo(this, outputPath, options);
    }

    public Path exportHtmlReport() {
        return new TraceHtmlExporter().exportToDefault(this);
    }

    public Path exportHtmlReport(TraceHtmlExportOptions options) {
        return new TraceHtmlExporter().exportToDefault(this, options);
    }

    private void finish(TraceStatus status, Throwable throwable, String message) {
        Instant finishedAt = Instant.now();
        metadata = metadata.toBuilder()
                .status(status)
                .finishedAt(finishedAt)
                .build();
        TraceEvent.Builder event = TraceEvent.builder(TraceEventType.SESSION_FINISHED, status, metadata.name())
                .timestamp(finishedAt)
                .message(message == null ? "" : message)
                .attribute("sessionId", id());
        if (throwable != null) {
            event.failure(TraceFailure.from(throwable, false)).message(throwable.getMessage());
        }
        addEvent(event.build());
    }
}
