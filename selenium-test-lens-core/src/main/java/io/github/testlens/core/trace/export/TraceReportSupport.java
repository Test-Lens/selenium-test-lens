package io.github.testlens.core.trace.export;

import io.github.testlens.core.trace.TraceArtifact;
import io.github.testlens.core.trace.TraceArtifactType;
import io.github.testlens.core.trace.TraceEvent;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.UiTestLensSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class TraceReportSupport {
    public static final String SCHEMA_VERSION = "1.0";
    public static final Path DEFAULT_REPORT_DIRECTORY = Path.of("target", "ui-test-lens-report");
    public static final Path DEFAULT_SUITE_JSON_PATH = DEFAULT_REPORT_DIRECTORY.resolve("report.json");
    public static final Path DEFAULT_BUNDLE_PATH = DEFAULT_REPORT_DIRECTORY.resolve("ui-test-lens-report.zip");

    private TraceReportSupport() {
    }

    public static List<UiTestLensSession> safeSessions(List<UiTestLensSession> sessions) {
        return sessions == null ? List.of() : sessions.stream().filter(Objects::nonNull).toList();
    }

    public static TraceStatus suiteStatus(List<UiTestLensSession> sessions) {
        List<UiTestLensSession> safeSessions = safeSessions(sessions);
        if (safeSessions.stream().anyMatch(session -> isFailedOrErrorStatus(sessionStatus(session)))) {
            return TraceStatus.FAILED;
        }
        if (safeSessions.stream().anyMatch(TraceReportSupport::hasWarning)) {
            return TraceStatus.WARNING;
        }
        if (safeSessions.isEmpty()) {
            return TraceStatus.INFO;
        }
        if (safeSessions.stream().allMatch(session -> sessionStatus(session) == TraceStatus.SKIPPED)) {
            return TraceStatus.SKIPPED;
        }
        return TraceStatus.PASSED;
    }

    public static TraceStatus sessionStatus(UiTestLensSession session) {
        if (session == null) {
            return TraceStatus.INFO;
        }
        TraceStatus status = session.metadata().status();
        if (status == TraceStatus.STARTED && failureCount(session) > 0) {
            return TraceStatus.FAILED;
        }
        return status == null ? TraceStatus.INFO : status;
    }

    public static boolean hasWarning(UiTestLensSession session) {
        return session != null && session.events().stream().anyMatch(event -> event.status() == TraceStatus.WARNING);
    }

    public static boolean isFailedOrError(TraceEvent event) {
        return event != null && isFailedOrErrorStatus(event.status());
    }

    public static boolean isFailedOrErrorStatus(TraceStatus status) {
        return status == TraceStatus.FAILED || status == TraceStatus.ERROR;
    }

    public static long failureCount(UiTestLensSession session) {
        if (session == null) {
            return 0;
        }
        return session.events().stream()
                .filter(event -> event.failure() != null || isFailedOrError(event))
                .count();
    }

    public static Duration sessionDuration(UiTestLensSession session) {
        if (session == null) {
            return Duration.ZERO;
        }
        Instant startedAt = session.metadata().startedAt();
        Instant finishedAt = session.metadata().finishedAt();
        if (startedAt == null || finishedAt == null) {
            return Duration.ZERO;
        }
        return Duration.between(startedAt, finishedAt);
    }

    public static Duration totalSessionDuration(List<UiTestLensSession> sessions) {
        Duration total = Duration.ZERO;
        for (UiTestLensSession session : safeSessions(sessions)) {
            total = total.plus(sessionDuration(session));
        }
        return total;
    }

    public static long screenshotCount(UiTestLensSession session) {
        if (session == null) {
            return 0;
        }
        return session.artifacts().stream().filter(artifact -> artifact.type() == TraceArtifactType.SCREENSHOT).count();
    }

    public static long screenshotCount(List<UiTestLensSession> sessions) {
        return safeSessions(sessions).stream().mapToLong(TraceReportSupport::screenshotCount).sum();
    }

    public static long artifactCount(List<UiTestLensSession> sessions) {
        return safeSessions(sessions).stream().mapToLong(session -> session.artifacts().size()).sum();
    }

    public static long eventCount(List<UiTestLensSession> sessions) {
        return safeSessions(sessions).stream().mapToLong(session -> session.events().size()).sum();
    }

    public static String safeFileName(String value, String fallback) {
        String base = value == null || value.isBlank() ? fallback : value.trim();
        String safe = base.replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        return safe.isBlank() ? fallback : safe;
    }

    public static String sessionAnchor(UiTestLensSession session) {
        return "session-" + safeFileName(session == null ? "" : session.id(), "unknown");
    }

    public static Path artifactPath(TraceArtifact artifact) {
        if (artifact == null || artifact.path() == null || artifact.path().isBlank()) {
            return null;
        }
        try {
            return Path.of(artifact.path());
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static Path absoluteArtifactPath(TraceArtifact artifact) {
        Path path = artifactPath(artifact);
        if (path == null) {
            return null;
        }
        return path.isAbsolute()
                ? path.normalize()
                : Path.of("").toAbsolutePath().resolve(path).normalize();
    }

    public static boolean artifactExists(TraceArtifact artifact) {
        Path path = absoluteArtifactPath(artifact);
        return path != null && Files.exists(path);
    }

    public static long artifactSizeBytes(TraceArtifact artifact) {
        Path path = absoluteArtifactPath(artifact);
        if (path == null || !Files.exists(path)) {
            return -1;
        }
        try {
            return Files.size(path);
        } catch (Exception e) {
            return -1;
        }
    }

    public static String relativeArtifactPath(TraceArtifact artifact, Path baseDirectory) {
        if (artifact == null || artifact.path() == null || artifact.path().isBlank() || baseDirectory == null) {
            return "";
        }
        Path absolutePath = absoluteArtifactPath(artifact);
        if (absolutePath == null) {
            return "";
        }
        try {
            return baseDirectory.toAbsolutePath().normalize().relativize(absolutePath).toString().replace('\\', '/');
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}
