package io.github.testlens.selenium.evidence;

import io.github.testlens.core.trace.TraceArtifact;

import java.nio.file.Path;
import java.time.Instant;

public final class ScreenshotCaptureResult {
    private final ScreenshotCaptureStatus status;
    private final String name;
    private final Path path;
    private final TraceArtifact artifact;
    private final String message;
    private final Throwable exception;
    private final Instant capturedAt;

    private ScreenshotCaptureResult(ScreenshotCaptureStatus status,
                                    String name,
                                    Path path,
                                    TraceArtifact artifact,
                                    String message,
                                    Throwable exception,
                                    Instant capturedAt) {
        this.status = status == null ? ScreenshotCaptureStatus.FAILED : status;
        this.name = name == null ? "" : name;
        this.path = path;
        this.artifact = artifact;
        this.message = message == null ? "" : message;
        this.exception = exception;
        this.capturedAt = capturedAt == null ? Instant.now() : capturedAt;
    }

    public static ScreenshotCaptureResult captured(String name, Path path, TraceArtifact artifact, String message) {
        return new ScreenshotCaptureResult(ScreenshotCaptureStatus.CAPTURED, name, path, artifact, message, null, Instant.now());
    }

    public static ScreenshotCaptureResult failed(String name, Path path, String message, Throwable exception) {
        return new ScreenshotCaptureResult(ScreenshotCaptureStatus.FAILED, name, path, null, message, exception, Instant.now());
    }

    public static ScreenshotCaptureResult skipped(String name, String message) {
        return new ScreenshotCaptureResult(ScreenshotCaptureStatus.SKIPPED, name, null, null, message, null, Instant.now());
    }

    public ScreenshotCaptureStatus status() {
        return status;
    }

    public String name() {
        return name;
    }

    public Path path() {
        return path;
    }

    public TraceArtifact artifact() {
        return artifact;
    }

    public String message() {
        return message;
    }

    public Throwable exception() {
        return exception;
    }

    public Instant capturedAt() {
        return capturedAt;
    }

    public boolean isCaptured() {
        return status == ScreenshotCaptureStatus.CAPTURED;
    }
}
