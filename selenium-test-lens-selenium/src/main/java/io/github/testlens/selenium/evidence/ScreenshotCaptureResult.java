package io.github.testlens.selenium.evidence;

import io.github.testlens.core.trace.TraceArtifact;

import java.nio.file.Path;
import java.time.Instant;

/** Describes a completed, failed, or unsupported screenshot request without exposing pixel content in metadata. */
public final class ScreenshotCaptureResult {
    private final ScreenshotCaptureStatus status;
    private final String name;
    private final Path path;
    private final TraceArtifact artifact;
    private final String message;
    private final Throwable exception;
    private final Instant capturedAt;
    private final ScreenshotCaptureMode requestedMode;
    private final ScreenshotCaptureMode capturedMode;
    private final int width;
    private final int height;
    private final int tileCount;

    private ScreenshotCaptureResult(ScreenshotCaptureStatus status,
                                    String name,
                                    Path path,
                                    TraceArtifact artifact,
                                    String message,
                                    Throwable exception,
                                    Instant capturedAt,
                                    ScreenshotCaptureMode requestedMode,
                                    ScreenshotCaptureMode capturedMode,
                                    int width,
                                    int height,
                                    int tileCount) {
        this.status = status == null ? ScreenshotCaptureStatus.FAILED : status;
        this.name = name == null ? "" : name;
        this.path = path;
        this.artifact = artifact;
        this.message = message == null ? "" : message;
        this.exception = exception;
        this.capturedAt = capturedAt == null ? Instant.now() : capturedAt;
        this.requestedMode = requestedMode == null ? ScreenshotCaptureMode.VIEWPORT : requestedMode;
        this.capturedMode = capturedMode;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.tileCount = Math.max(0, tileCount);
    }

    public static ScreenshotCaptureResult captured(String name, Path path, TraceArtifact artifact, String message) {
        return new ScreenshotCaptureResult(ScreenshotCaptureStatus.CAPTURED, name, path, artifact, message, null,
                Instant.now(), ScreenshotCaptureMode.VIEWPORT, ScreenshotCaptureMode.VIEWPORT, 0, 0, 1);
    }

    public static ScreenshotCaptureResult failed(String name, Path path, String message, Throwable exception) {
        return new ScreenshotCaptureResult(ScreenshotCaptureStatus.FAILED, name, path, null, message, exception,
                Instant.now(), ScreenshotCaptureMode.VIEWPORT, null, 0, 0, 0);
    }

    public static ScreenshotCaptureResult skipped(String name, String message) {
        return new ScreenshotCaptureResult(ScreenshotCaptureStatus.SKIPPED, name, null, null, message, null,
                Instant.now(), ScreenshotCaptureMode.VIEWPORT, null, 0, 0, 0);
    }

    static ScreenshotCaptureResult captured(String name, Path path, TraceArtifact artifact, String message,
                                            ScreenshotCaptureMode mode, int width, int height, int tileCount) {
        return new ScreenshotCaptureResult(ScreenshotCaptureStatus.CAPTURED, name, path, artifact, message, null,
                Instant.now(), mode, mode, width, height, tileCount);
    }

    static ScreenshotCaptureResult failed(String name, Path path, String message, Throwable exception,
                                          ScreenshotCaptureMode requestedMode) {
        return new ScreenshotCaptureResult(ScreenshotCaptureStatus.FAILED, name, path, null, message, exception,
                Instant.now(), requestedMode, null, 0, 0, 0);
    }

    static ScreenshotCaptureResult skipped(String name, String message, ScreenshotCaptureMode requestedMode) {
        return new ScreenshotCaptureResult(ScreenshotCaptureStatus.SKIPPED, name, null, null, message, null,
                Instant.now(), requestedMode, null, 0, 0, 0);
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

    /** Returns the mode requested by the caller. */
    public ScreenshotCaptureMode requestedMode() { return requestedMode; }

    /** Returns the completed mode, or {@code null} when no image was captured. */
    public ScreenshotCaptureMode capturedMode() { return capturedMode; }

    /** Returns the result PNG width in pixels, or zero when unavailable. */
    public int width() { return width; }

    /** Returns the result PNG height in pixels, or zero when unavailable. */
    public int height() { return height; }

    /** Returns the number of viewport images used by the capture, or zero when no image was completed. */
    public int tileCount() { return tileCount; }
}

