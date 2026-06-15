package io.github.testlens.core.trace.export;

import io.github.testlens.core.trace.TraceArtifact;
import io.github.testlens.core.trace.TraceJsonExportOptions;
import io.github.testlens.core.trace.TraceJsonExporter;
import io.github.testlens.core.trace.UiTestLensSession;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exports portable static report bundles for CI artifacts and API uploads.
 */
public final class TraceReportBundleExporter {
    public static final Path DEFAULT_OUTPUT_PATH = TraceReportSupport.DEFAULT_BUNDLE_PATH;
    private static final String BUNDLE_VERSION = "1.0";

    public Path exportSuite(List<UiTestLensSession> sessions) {
        return exportSuite(sessions, TraceBundleExportOptions.defaults());
    }

    public Path exportSuite(List<UiTestLensSession> sessions, TraceBundleExportOptions options) {
        TraceBundleExportOptions effectiveOptions = options == null ? TraceBundleExportOptions.defaults() : options;
        return exportSuiteTo(sessions, effectiveOptions.outputDirectory().resolve("ui-test-lens-report.zip"), effectiveOptions);
    }

    public Path exportSuiteToDefault(List<UiTestLensSession> sessions) {
        return exportSuiteTo(sessions, DEFAULT_OUTPUT_PATH, TraceBundleExportOptions.defaults());
    }

    public Path exportSuiteToDefault(List<UiTestLensSession> sessions, TraceBundleExportOptions options) {
        return exportSuiteTo(sessions, DEFAULT_OUTPUT_PATH, options);
    }

    public Path exportSuiteTo(List<UiTestLensSession> sessions, Path outputPath) {
        return exportSuiteTo(sessions, outputPath, TraceBundleExportOptions.defaults());
    }

    public Path exportSuiteTo(List<UiTestLensSession> sessions, Path outputPath, TraceBundleExportOptions options) {
        if (outputPath == null) {
            throw new IllegalArgumentException("outputPath must not be null");
        }
        TraceBundleExportOptions effectiveOptions = options == null ? TraceBundleExportOptions.defaults() : options;
        List<UiTestLensSession> safeSessions = TraceReportSupport.safeSessions(sessions);
        List<String> includedFiles = new ArrayList<>();
        List<String> missingArtifacts = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> usedEntries = new HashSet<>();
        Map<String, String> artifactPathOverrides = new LinkedHashMap<>();

        try {
            Path parent = outputPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(outputPath))) {
                List<BundleArtifact> bundleArtifacts = effectiveOptions.copyArtifacts()
                        ? collectArtifacts(safeSessions, usedEntries, artifactPathOverrides, missingArtifacts, warnings)
                        : List.of();
                TraceHtmlExportOptions htmlOptions = TraceHtmlExportOptions.builder()
                        .theme(effectiveOptions.htmlTheme())
                        .includeStackTraces(effectiveOptions.includeStackTraces())
                        .includeAttributes(false)
                        .build();
                addTextEntry(zip,
                        "index.html",
                        new TraceHtmlExporter().exportSuite(safeSessions, htmlOptions, artifactPathOverrides),
                        usedEntries,
                        includedFiles);

                TraceJsonExportOptions jsonOptions = TraceJsonExportOptions.builder()
                        .includeStackTraces(effectiveOptions.includeStackTraces())
                        .includeArtifactMetadata(effectiveOptions.includeArtifactMetadata())
                        .includeMissingArtifacts(effectiveOptions.includeMissingArtifacts())
                        .build();
                addTextEntry(zip, "report.json", new TraceJsonExporter().exportSuite(safeSessions, jsonOptions), usedEntries, includedFiles);

                if (effectiveOptions.copyArtifacts()) {
                    copyArtifacts(zip, bundleArtifacts, usedEntries, includedFiles);
                }

                List<String> manifestIncludedFiles = new ArrayList<>(includedFiles);
                manifestIncludedFiles.add("manifest.json");
                addTextEntry(zip, "manifest.json",
                        manifest(effectiveOptions, safeSessions, manifestIncludedFiles, missingArtifacts, warnings),
                        usedEntries,
                        includedFiles);
            }
            return outputPath;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path export(UiTestLensSession session) {
        return exportSuite(List.of(session), TraceBundleExportOptions.defaults());
    }

    public Path exportTo(UiTestLensSession session, Path outputPath) {
        return exportSuiteTo(List.of(session), outputPath, TraceBundleExportOptions.defaults());
    }

    private List<BundleArtifact> collectArtifacts(List<UiTestLensSession> sessions,
                                                  Set<String> usedEntries,
                                                  Map<String, String> artifactPathOverrides,
                                                  List<String> missingArtifacts,
                                                  List<String> warnings) {
        List<BundleArtifact> bundleArtifacts = new ArrayList<>();
        for (UiTestLensSession session : sessions) {
            String sessionPrefix = TraceReportSupport.safeFileName(session.metadata().name(), "session");
            for (TraceArtifact artifact : session.artifacts()) {
                if (artifact.path() == null || artifact.path().isBlank()) {
                    continue;
                }
                Path source = TraceReportSupport.absoluteArtifactPath(artifact);
                if (source == null || !Files.exists(source) || !Files.isRegularFile(source)) {
                    missingArtifacts.add(artifact.name() + " (" + artifact.path() + ")");
                    warnings.add("Missing artifact: " + artifact.name());
                    continue;
                }
                String fileName = source.getFileName() == null
                        ? TraceReportSupport.safeFileName(artifact.name(), "artifact")
                        : TraceReportSupport.safeFileName(source.getFileName().toString(), "artifact");
                String baseEntry = "artifacts/" + sessionPrefix + "/" + fileName;
                String entryName = uniqueEntryName(baseEntry, usedEntries);
                usedEntries.add(entryName);
                artifactPathOverrides.put(artifact.path(), entryName);
                bundleArtifacts.add(new BundleArtifact(entryName, source));
            }
        }
        return bundleArtifacts;
    }

    private void copyArtifacts(ZipOutputStream zip,
                               List<BundleArtifact> bundleArtifacts,
                               Set<String> usedEntries,
                               List<String> includedFiles) throws IOException {
        for (BundleArtifact artifact : bundleArtifacts) {
            addFileEntry(zip, artifact.entryName(), artifact.source(), usedEntries, includedFiles);
        }
    }

    private String manifest(TraceBundleExportOptions options,
                            List<UiTestLensSession> sessions,
                            List<String> includedFiles,
                            List<String> missingArtifacts,
                            List<String> warnings) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schemaVersion", TraceReportSupport.SCHEMA_VERSION);
        manifest.put("bundleVersion", BUNDLE_VERSION);
        manifest.put("generatedAt", Instant.now().toString());
        manifest.put("reportName", options.bundleName());
        manifest.put("sessionCount", sessions.size());
        manifest.put("includedFiles", List.copyOf(includedFiles));
        manifest.put("missingArtifacts", List.copyOf(missingArtifacts));
        manifest.put("warnings", List.copyOf(warnings));
        return TraceJsonWriter.write(manifest);
    }

    private void addTextEntry(ZipOutputStream zip,
                              String entryName,
                              String content,
                              Set<String> usedEntries,
                              List<String> includedFiles) throws IOException {
        String safeEntryName = safeZipEntryName(entryName);
        ZipEntry entry = new ZipEntry(safeEntryName);
        zip.putNextEntry(entry);
        zip.write((content == null ? "" : content).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
        usedEntries.add(safeEntryName);
        includedFiles.add(safeEntryName);
    }

    private void addFileEntry(ZipOutputStream zip,
                              String entryName,
                              Path source,
                              Set<String> usedEntries,
                              List<String> includedFiles) throws IOException {
        String safeEntryName = safeZipEntryName(entryName);
        ZipEntry entry = new ZipEntry(safeEntryName);
        zip.putNextEntry(entry);
        Files.copy(source, zip);
        zip.closeEntry();
        usedEntries.add(safeEntryName);
        includedFiles.add(safeEntryName);
    }

    private String uniqueEntryName(String baseEntry, Set<String> usedEntries) {
        String safeBase = safeZipEntryName(baseEntry);
        if (!usedEntries.contains(safeBase)) {
            return safeBase;
        }
        int dot = safeBase.lastIndexOf('.');
        String prefix = dot > 0 ? safeBase.substring(0, dot) : safeBase;
        String suffix = dot > 0 ? safeBase.substring(dot) : "";
        int index = 2;
        String candidate;
        do {
            candidate = prefix + "-" + index + suffix;
            index++;
        } while (usedEntries.contains(candidate));
        return candidate;
    }

    static String safeZipEntryName(String entryName) {
        if (entryName == null || entryName.isBlank()) {
            throw new IllegalArgumentException("zip entry name must not be blank");
        }
        String normalized = entryName.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.contains(":")) {
            throw new IllegalArgumentException("zip entry name must be relative");
        }
        Path normalizedPath = Path.of(normalized).normalize();
        String safe = normalizedPath.toString().replace('\\', '/');
        if (safe.isBlank() || safe.startsWith("../") || safe.equals("..") || safe.contains("/../")) {
            throw new IllegalArgumentException("unsafe zip entry name: " + entryName);
        }
        return safe;
    }

    private record BundleArtifact(String entryName, Path source) {
    }
}

