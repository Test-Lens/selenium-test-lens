package io.github.mmaciekk111.uitestlens.core.trace;

import io.github.mmaciekk111.uitestlens.core.trace.export.TraceJsonWriter;
import io.github.mmaciekk111.uitestlens.core.trace.export.TraceReportSupport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Exports Selenium Test Lens trace sessions as machine-readable JSON reports.
 */
public final class TraceJsonExporter {
    public static final Path DEFAULT_SUITE_OUTPUT_PATH = TraceReportSupport.DEFAULT_SUITE_JSON_PATH;

    public String export(UiTestLensSession session) {
        return export(session, TraceJsonExportOptions.defaults());
    }

    public String export(UiTestLensSession session, boolean includeStackTraces) {
        return export(session, TraceJsonExportOptions.builder()
                .includeStackTraces(includeStackTraces)
                .build());
    }

    public String export(UiTestLensSession session, TraceJsonExportOptions options) {
        TraceJsonExportOptions effectiveOptions = options == null ? TraceJsonExportOptions.defaults() : options;
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", TraceReportSupport.SCHEMA_VERSION);
        root.put("generatedAt", Instant.now().toString());
        root.put("reportType", "session");
        if (session != null) {
            root.put("session", sessionMap(session, effectiveOptions));
            root.put("metadata", metadataMap(session.metadata()));
            root.put("events", session.events().stream()
                    .map(event -> eventMap(event, effectiveOptions))
                    .toList());
            root.put("artifacts", session.artifacts().stream()
                    .map(artifact -> artifactMap(artifact, effectiveOptions))
                    .filter(map -> effectiveOptions.includeMissingArtifacts() || !Boolean.FALSE.equals(map.get("exists")))
                    .toList());
        }
        return TraceJsonWriter.write(root);
    }

    public Path exportTo(UiTestLensSession session, Path outputPath) {
        return exportTo(session, outputPath, TraceJsonExportOptions.defaults());
    }

    public Path exportTo(UiTestLensSession session, Path outputPath, TraceJsonExportOptions options) {
        if (outputPath == null) {
            throw new IllegalArgumentException("outputPath must not be null");
        }
        try {
            Path parent = outputPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            TraceJsonExportOptions effectiveOptions = (options == null ? TraceJsonExportOptions.defaults() : options)
                    .toBuilder()
                    .artifactBaseDirectory(parent)
                    .build();
            Files.writeString(outputPath, export(session, effectiveOptions));
            return outputPath;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path exportToDefault(UiTestLensSession session) {
        return exportToDefault(session, TraceJsonExportOptions.defaults());
    }

    public Path exportToDefault(UiTestLensSession session, TraceJsonExportOptions options) {
        String name = session == null ? "session" : session.metadata().name();
        Path outputPath = TraceReportSupport.DEFAULT_REPORT_DIRECTORY
                .resolve(TraceReportSupport.safeFileName(name, "session") + ".json");
        return exportTo(session, outputPath, options);
    }

    public String exportSuite(List<UiTestLensSession> sessions) {
        return exportSuite(sessions, TraceJsonExportOptions.defaults());
    }

    public String exportSuite(List<UiTestLensSession> sessions, TraceJsonExportOptions options) {
        TraceJsonExportOptions effectiveOptions = options == null ? TraceJsonExportOptions.defaults() : options;
        List<UiTestLensSession> safeSessions = TraceReportSupport.safeSessions(sessions);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", TraceReportSupport.SCHEMA_VERSION);
        root.put("generatedAt", Instant.now().toString());
        root.put("reportType", "suite");
        root.put("name", "Selenium Test Lens Report");
        root.put("status", TraceReportSupport.suiteStatus(safeSessions).name());
        root.put("summary", suiteSummary(safeSessions));
        root.put("sessions", safeSessions.stream()
                .map(session -> sessionMap(session, effectiveOptions))
                .toList());
        return TraceJsonWriter.write(root);
    }

    public Path exportSuiteTo(List<UiTestLensSession> sessions, Path outputPath) {
        return exportSuiteTo(sessions, outputPath, TraceJsonExportOptions.defaults());
    }

    public Path exportSuiteTo(List<UiTestLensSession> sessions, Path outputPath, TraceJsonExportOptions options) {
        if (outputPath == null) {
            throw new IllegalArgumentException("outputPath must not be null");
        }
        try {
            Path parent = outputPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            TraceJsonExportOptions effectiveOptions = (options == null ? TraceJsonExportOptions.defaults() : options)
                    .toBuilder()
                    .artifactBaseDirectory(parent)
                    .build();
            Files.writeString(outputPath, exportSuite(sessions, effectiveOptions));
            return outputPath;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path exportSuiteToDefault(List<UiTestLensSession> sessions) {
        return exportSuiteTo(sessions, DEFAULT_SUITE_OUTPUT_PATH, TraceJsonExportOptions.defaults());
    }

    public Path exportSuiteToDefault(List<UiTestLensSession> sessions, TraceJsonExportOptions options) {
        return exportSuiteTo(sessions, DEFAULT_SUITE_OUTPUT_PATH, options);
    }

    private Map<String, Object> sessionMap(UiTestLensSession session, TraceJsonExportOptions options) {
        TraceMetadata metadata = session.metadata();
        Map<String, Object> out = new LinkedHashMap<>();
        put(out, "id", metadata.sessionId());
        put(out, "name", metadata.name());
        out.put("status", TraceReportSupport.sessionStatus(session).name());
        put(out, "startedAt", metadata.startedAt());
        put(out, "endedAt", metadata.finishedAt());
        out.put("durationMs", TraceReportSupport.sessionDuration(session).toMillis());
        put(out, "environment", metadata.environment());
        if (!metadata.labels().isEmpty()) {
            out.put("labels", sortedMap(metadata.labels()));
        }
        out.put("summary", sessionSummary(session));
        out.put("events", session.events().stream()
                .map(event -> eventMap(event, options))
                .toList());
        out.put("artifacts", session.artifacts().stream()
                .map(artifact -> artifactMap(artifact, options))
                .filter(map -> options.includeMissingArtifacts() || !Boolean.FALSE.equals(map.get("exists")))
                .toList());
        return out;
    }

    private Map<String, Object> metadataMap(TraceMetadata metadata) {
        Map<String, Object> out = new LinkedHashMap<>();
        put(out, "sessionId", metadata.sessionId());
        put(out, "name", metadata.name());
        put(out, "startedAt", metadata.startedAt());
        put(out, "finishedAt", metadata.finishedAt());
        out.put("status", metadata.status().name());
        put(out, "environment", metadata.environment());
        if (!metadata.labels().isEmpty()) {
            out.put("labels", sortedMap(metadata.labels()));
        }
        return out;
    }

    private Map<String, Object> sessionSummary(UiTestLensSession session) {
        List<TraceEvent> events = session.events();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalEvents", events.size());
        out.put("passed", events.stream().filter(event -> event.status() == TraceStatus.PASSED).count());
        out.put("failed", events.stream().filter(TraceReportSupport::isFailedOrError).count());
        out.put("warnings", events.stream().filter(event -> event.status() == TraceStatus.WARNING).count());
        out.put("info", events.stream().filter(event -> event.status() == TraceStatus.INFO).count());
        out.put("skipped", events.stream().filter(event -> event.status() == TraceStatus.SKIPPED).count());
        out.put("failures", TraceReportSupport.failureCount(session));
        out.put("totalArtifacts", session.artifacts().size());
        out.put("screenshots", TraceReportSupport.screenshotCount(session));
        out.put("durationMs", TraceReportSupport.sessionDuration(session).toMillis());
        return out;
    }

    private Map<String, Object> suiteSummary(List<UiTestLensSession> sessions) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalSessions", sessions.size());
        out.put("passed", sessions.stream().filter(session -> TraceReportSupport.sessionStatus(session) == TraceStatus.PASSED).count());
        out.put("failed", sessions.stream().filter(session -> TraceReportSupport.isFailedOrErrorStatus(TraceReportSupport.sessionStatus(session))).count());
        out.put("warnings", sessions.stream().filter(TraceReportSupport::hasWarning).count());
        out.put("totalEvents", TraceReportSupport.eventCount(sessions));
        out.put("totalArtifacts", TraceReportSupport.artifactCount(sessions));
        out.put("screenshots", TraceReportSupport.screenshotCount(sessions));
        out.put("durationMs", TraceReportSupport.totalSessionDuration(sessions).toMillis());
        return out;
    }

    private Map<String, Object> eventMap(TraceEvent event, TraceJsonExportOptions options) {
        Map<String, Object> out = new LinkedHashMap<>();
        put(out, "id", event.id());
        put(out, "parentId", event.parentId());
        out.put("type", event.type().name());
        out.put("status", event.status().name());
        put(out, "name", event.name());
        put(out, "message", event.message());
        put(out, "timestamp", event.timestamp());
        out.put("durationMs", event.duration() == null ? 0 : event.duration().toMillis());
        if (event.failure() != null) {
            out.put("failure", failureMap(event.failure(), options));
        }
        if (!event.attributes().isEmpty()) {
            out.put("attributes", sortedMap(event.attributes()));
            out.put("metadata", sortedMap(event.attributes()));
        }
        if (!event.artifacts().isEmpty()) {
            out.put("artifacts", event.artifacts().stream()
                    .map(artifact -> artifactMap(artifact, options))
                    .filter(map -> options.includeMissingArtifacts() || !Boolean.FALSE.equals(map.get("exists")))
                    .toList());
        }
        return out;
    }

    private Map<String, Object> artifactMap(TraceArtifact artifact, TraceJsonExportOptions options) {
        Map<String, Object> out = new LinkedHashMap<>();
        put(out, "name", artifact.name());
        out.put("type", artifact.type().name());
        put(out, "mediaType", artifact.mediaType());
        put(out, "path", artifact.path());
        String relativePath = TraceReportSupport.relativeArtifactPath(artifact, options.artifactBaseDirectory());
        put(out, "relativePath", relativePath);
        put(out, "url", artifact.url());
        put(out, "createdAt", artifact.createdAt());
        if (artifact.path() != null && !artifact.path().isBlank()) {
            boolean exists = TraceReportSupport.artifactExists(artifact);
            out.put("exists", exists);
            long sizeBytes = TraceReportSupport.artifactSizeBytes(artifact);
            if (sizeBytes >= 0) {
                out.put("sizeBytes", sizeBytes);
            }
        }
        if (options.includeArtifactMetadata() && !artifact.metadata().isEmpty()) {
            out.put("metadata", sortedMap(artifact.metadata()));
        }
        return out;
    }

    private Map<String, Object> failureMap(TraceFailure failure, TraceJsonExportOptions options) {
        Map<String, Object> out = new LinkedHashMap<>();
        put(out, "message", failure.message());
        put(out, "exceptionType", failure.exceptionType());
        if (options.includeStackTraces()) {
            put(out, "stackTrace", failure.stackTrace());
        }
        if (!failure.details().isEmpty()) {
            out.put("details", sortedMap(failure.details()));
        }
        return out;
    }

    private Map<String, String> sortedMap(Map<String, String> input) {
        return new TreeMap<>(input == null ? Map.of() : input);
    }

    private void put(Map<String, Object> out, String name, Instant value) {
        if (value != null) {
            out.put(name, value.toString());
        }
    }

    private void put(Map<String, Object> out, String name, String value) {
        if (value != null && !value.isBlank()) {
            out.put(name, value);
        }
    }

    static String escape(String value) {
        return TraceJsonWriter.escape(value);
    }
}
