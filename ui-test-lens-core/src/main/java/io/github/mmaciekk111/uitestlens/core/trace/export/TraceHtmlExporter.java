package io.github.mmaciekk111.uitestlens.core.trace.export;

import io.github.mmaciekk111.uitestlens.core.trace.TraceArtifact;
import io.github.mmaciekk111.uitestlens.core.trace.TraceArtifactType;
import io.github.mmaciekk111.uitestlens.core.trace.TraceEvent;
import io.github.mmaciekk111.uitestlens.core.trace.TraceEventType;
import io.github.mmaciekk111.uitestlens.core.trace.TraceFailure;
import io.github.mmaciekk111.uitestlens.core.trace.TraceJsonExporter;
import io.github.mmaciekk111.uitestlens.core.trace.TraceMetadata;
import io.github.mmaciekk111.uitestlens.core.trace.TraceStatus;
import io.github.mmaciekk111.uitestlens.core.trace.UiTestLensSession;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Exports a {@link UiTestLensSession} as a standalone HTML trace report.
 *
 * <p>The exporter renders metadata, categorized timeline events, failures, artifacts, and optional raw JSON.
 */
public final class TraceHtmlExporter {
    public static final Path DEFAULT_OUTPUT_PATH = Path.of("target", "ui-test-lens-report", "index.html");

    private enum EventCategory {
        SESSION("Session"),
        STEPS("Steps"),
        LOCATORS("Locators"),
        ACTIONS("Actions"),
        ACTIONABILITY("Actionability"),
        ASSERTIONS("Assertions"),
        BUSINESS("Business"),
        OVERLAYS("Overlays"),
        EVIDENCE("Evidence"),
        NETWORK("Network"),
        OTHER("Other");

        private final String label;

        EventCategory(String label) {
            this.label = label;
        }
    }

    public String export(UiTestLensSession session) {
        return export(session, TraceHtmlExportOptions.defaults());
    }

    public String export(UiTestLensSession session, TraceHtmlExportOptions options) {
        return export(session, options, null);
    }

    private String export(UiTestLensSession session, TraceHtmlExportOptions options, Path artifactBaseDirectory) {
        if (session == null) {
            return emptyReport(options);
        }
        TraceHtmlExportOptions effectiveOptions = options == null ? TraceHtmlExportOptions.defaults() : options;
        StringBuilder out = new StringBuilder(48_000);
        out.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>").append(escape(effectiveOptions.title())).append("</title>")
                .append("<style>").append(css(effectiveOptions.theme())).append("</style></head><body>");
        appendHeader(out, session, effectiveOptions);
        appendSummary(out, session, effectiveOptions);
        if (effectiveOptions.includeFailureSummary()) {
            appendFailureSummary(out, session.events(), effectiveOptions);
        }
        if (effectiveOptions.includeEventTypeSummary()) {
            appendEventTypeSummary(out, session.events());
        }
        appendTimeline(out, session.events(), effectiveOptions);
        appendSteps(out, session.events(), effectiveOptions);
        if (effectiveOptions.includeArtifacts()) {
            appendArtifacts(out, session.artifacts(), effectiveOptions, artifactBaseDirectory);
        }
        if (effectiveOptions.includeJsonPayload()) {
            appendRawJson(out, session, effectiveOptions);
        }
        out.append("</body></html>");
        return out.toString();
    }

    public Path exportTo(UiTestLensSession session, Path outputPath) {
        return exportTo(session, outputPath, TraceHtmlExportOptions.defaults());
    }

    public Path exportTo(UiTestLensSession session, Path outputPath, TraceHtmlExportOptions options) {
        if (outputPath == null) {
            throw new IllegalArgumentException("outputPath must not be null");
        }
        try {
            Path parent = outputPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputPath, export(session, options, parent));
            return outputPath;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path exportToDefault(UiTestLensSession session) {
        return exportTo(session, DEFAULT_OUTPUT_PATH, TraceHtmlExportOptions.defaults());
    }

    public Path exportToDefault(UiTestLensSession session, TraceHtmlExportOptions options) {
        return exportTo(session, DEFAULT_OUTPUT_PATH, options);
    }

    public String exportSuite(List<UiTestLensSession> sessions) {
        return exportSuite(sessions, TraceHtmlExportOptions.defaults());
    }

    public String exportSuite(List<UiTestLensSession> sessions, TraceHtmlExportOptions options) {
        return exportSuite(sessions, options, null);
    }

    private String exportSuite(List<UiTestLensSession> sessions, TraceHtmlExportOptions options, Path artifactBaseDirectory) {
        TraceHtmlExportOptions effectiveOptions = options == null ? TraceHtmlExportOptions.defaults() : options;
        List<UiTestLensSession> safeSessions = sessions == null
                ? List.of()
                : sessions.stream().filter(Objects::nonNull).toList();
        StringBuilder out = new StringBuilder(96_000);
        out.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>").append(escape(effectiveOptions.title())).append("</title>")
                .append("<style>").append(css(effectiveOptions.theme())).append("</style></head><body>");
        appendSuiteHeader(out, safeSessions, effectiveOptions);
        appendSuiteSummary(out, safeSessions);
        appendSuiteFailures(out, safeSessions, effectiveOptions);
        appendSuiteTable(out, safeSessions);
        appendSuiteDetails(out, safeSessions, effectiveOptions, artifactBaseDirectory);
        out.append("</body></html>");
        return out.toString();
    }

    public Path exportSuiteTo(List<UiTestLensSession> sessions, Path outputPath) {
        return exportSuiteTo(sessions, outputPath, TraceHtmlExportOptions.defaults());
    }

    public Path exportSuiteTo(List<UiTestLensSession> sessions, Path outputPath, TraceHtmlExportOptions options) {
        if (outputPath == null) {
            throw new IllegalArgumentException("outputPath must not be null");
        }
        try {
            Path parent = outputPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputPath, exportSuite(sessions, options, parent));
            return outputPath;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path exportSuiteToDefault(List<UiTestLensSession> sessions) {
        return exportSuiteTo(sessions, DEFAULT_OUTPUT_PATH, TraceHtmlExportOptions.defaults());
    }

    public Path exportSuiteToDefault(List<UiTestLensSession> sessions, TraceHtmlExportOptions options) {
        return exportSuiteTo(sessions, DEFAULT_OUTPUT_PATH, options);
    }

    private String emptyReport(TraceHtmlExportOptions options) {
        TraceHtmlExportOptions effectiveOptions = options == null ? TraceHtmlExportOptions.defaults() : options;
        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>"
                + escape(effectiveOptions.title())
                + "</title></head><body><h1>"
                + escape(effectiveOptions.title())
                + "</h1><p>No UI Test Lens session available.</p></body></html>";
    }

    private void appendSuiteHeader(StringBuilder out, List<UiTestLensSession> sessions, TraceHtmlExportOptions options) {
        out.append("<header class=\"hero\"><p class=\"eyebrow\">UI Test Lens suite report</p><h1>")
                .append(escape(options.title()))
                .append("</h1><div class=\"metadata-grid\">");
        metadata(out, "Sessions", String.valueOf(sessions.size()));
        metadata(out, "Generated", shortInstant(Instant.now()));
        metadataHtml(out, "Overall status", badge(suiteStatus(sessions)));
        metadata(out, "Theme", options.theme().name());
        out.append("</div></header>");
    }

    private void appendSuiteSummary(StringBuilder out, List<UiTestLensSession> sessions) {
        long passed = sessions.stream().filter(session -> suiteSessionStatus(session) == TraceStatus.PASSED).count();
        long failed = sessions.stream().filter(session -> isFailedOrErrorStatus(suiteSessionStatus(session))).count();
        long warning = sessions.stream().filter(this::hasWarning).count();
        long artifacts = sessions.stream().mapToLong(session -> session.artifacts().size()).sum();
        long screenshots = sessions.stream()
                .flatMap(session -> session.artifacts().stream())
                .filter(artifact -> artifact.type() == TraceArtifactType.SCREENSHOT)
                .count();
        long events = sessions.stream().mapToLong(session -> session.events().size()).sum();
        out.append("<section><h2>Suite summary</h2><div class=\"cards\">");
        card(out, "Total tests", String.valueOf(sessions.size()));
        card(out, "Passed", String.valueOf(passed));
        card(out, "Failed", String.valueOf(failed));
        card(out, "Warnings", String.valueOf(warning));
        card(out, "Total events", String.valueOf(events));
        card(out, "Artifacts", String.valueOf(artifacts));
        card(out, "Screenshots", String.valueOf(screenshots));
        card(out, "Total duration", duration(totalSessionDuration(sessions)));
        out.append("</div></section>");
    }

    private void appendSuiteFailures(StringBuilder out, List<UiTestLensSession> sessions, TraceHtmlExportOptions options) {
        List<UiTestLensSession> failedSessions = sessions.stream()
                .filter(session -> isFailedOrErrorStatus(suiteSessionStatus(session)) || failureCount(session) > 0)
                .toList();
        out.append("<section><h2>Suite failures</h2>");
        if (failedSessions.isEmpty()) {
            out.append("<p class=\"ok-line\">No failed sessions recorded.</p></section>");
            return;
        }
        out.append("<div class=\"event-list\">");
        for (UiTestLensSession session : failedSessions) {
            String anchor = sessionAnchor(session);
            out.append("<article class=\"event-card failure\"><div class=\"event-title\"><strong>")
                    .append(escape(session.metadata().name()))
                    .append("</strong>")
                    .append(badge(suiteSessionStatus(session)))
                    .append("</div><p><a href=\"#")
                    .append(escape(anchor))
                    .append("\">Open test details</a></p>");
            List<TraceEvent> failures = session.events().stream()
                    .filter(event -> event.failure() != null || isFailedOrError(event))
                    .toList();
            if (failures.isEmpty()) {
                out.append("<p class=\"muted\">Session status is failed but no failed timeline event was recorded.</p>");
            } else {
                for (TraceEvent event : failures) {
                    out.append("<p>")
                            .append(typeBadge(event.type()))
                            .append(" <strong>")
                            .append(escape(event.name()))
                            .append("</strong> ")
                            .append(escape(preview(event.message(), options.maxMessageLength())))
                            .append("</p>");
                    appendFailureBlock(out, event.failure(), options);
                }
            }
            out.append("</article>");
        }
        out.append("</div></section>");
    }

    private void appendSuiteTable(StringBuilder out, List<UiTestLensSession> sessions) {
        out.append("<section><h2>Tests</h2><div class=\"table-wrap\"><table><thead><tr>")
                .append("<th>Test</th><th>Status</th><th>Duration</th><th>Events</th><th>Failures</th><th>Artifacts</th><th>Details</th>")
                .append("</tr></thead><tbody>");
        for (UiTestLensSession session : sessions) {
            String anchor = sessionAnchor(session);
            out.append("<tr><td><strong>")
                    .append(escape(session.metadata().name()))
                    .append("</strong><br><span class=\"muted mono\">")
                    .append(escape(session.id()))
                    .append("</span></td><td>")
                    .append(badge(suiteSessionStatus(session)))
                    .append("</td><td class=\"mono\">")
                    .append(escape(duration(session.metadata().startedAt(), session.metadata().finishedAt())))
                    .append("</td><td>")
                    .append(session.events().size())
                    .append("</td><td>")
                    .append(failureCount(session))
                    .append("</td><td>")
                    .append(session.artifacts().size())
                    .append("</td><td><a href=\"#")
                    .append(escape(anchor))
                    .append("\">Open</a></td></tr>");
        }
        if (sessions.isEmpty()) {
            out.append("<tr><td colspan=\"7\" class=\"muted\">No UI Test Lens sessions recorded.</td></tr>");
        }
        out.append("</tbody></table></div></section>");
    }

    private void appendSuiteDetails(StringBuilder out,
                                    List<UiTestLensSession> sessions,
                                    TraceHtmlExportOptions options,
                                    Path artifactBaseDirectory) {
        out.append("<section><h2>Test details</h2>");
        if (sessions.isEmpty()) {
            out.append("<p class=\"muted\">No session details available.</p></section>");
            return;
        }
        for (UiTestLensSession session : sessions) {
            out.append("<article class=\"session-detail\" id=\"")
                    .append(escape(sessionAnchor(session)))
                    .append("\"><div class=\"event-title\"><h3>")
                    .append(escape(session.metadata().name()))
                    .append("</h3>")
                    .append(badge(suiteSessionStatus(session)))
                    .append("</div><div class=\"metadata-grid\">");
            metadata(out, "Session ID", session.id());
            metadata(out, "Started", shortInstant(session.metadata().startedAt()));
            metadata(out, "Finished", shortInstant(session.metadata().finishedAt()));
            metadata(out, "Duration", duration(session.metadata().startedAt(), session.metadata().finishedAt()));
            metadata(out, "Events", String.valueOf(session.events().size()));
            metadata(out, "Artifacts", String.valueOf(session.artifacts().size()));
            out.append("</div>");
            if (!session.metadata().labels().isEmpty()) {
                out.append("<details class=\"details\"><summary>Session labels</summary>");
                appendMap(out, session.metadata().labels(), true);
                out.append("</details>");
            }
            appendFailureSummary(out, session.events(), options);
            appendTimeline(out, session.events(), options);
            if (options.includeArtifacts()) {
                appendArtifacts(out, session.artifacts(), options, artifactBaseDirectory);
            }
            out.append("</article>");
        }
        out.append("</section>");
    }

    private void appendHeader(StringBuilder out, UiTestLensSession session, TraceHtmlExportOptions options) {
        TraceMetadata metadata = session.metadata();
        out.append("<header class=\"hero\"><p class=\"eyebrow\">UI Test Lens trace</p><h1>")
                .append(escape(options.title()))
                .append("</h1><div class=\"metadata-grid\">");
        metadata(out, "Session", metadata.name());
        metadata(out, "Session ID", metadata.sessionId());
        metadataHtml(out, "Status", badge(metadata.status()));
        metadata(out, "Started", shortInstant(metadata.startedAt()));
        metadata(out, "Finished", shortInstant(metadata.finishedAt()));
        metadata(out, "Duration", duration(metadata.startedAt(), metadata.finishedAt()));
        metadata(out, "Generated", shortInstant(Instant.now()));
        metadata(out, "Environment", metadata.environment());
        out.append("</div>");
        if (!metadata.labels().isEmpty()) {
            out.append("<h2>Labels</h2>");
            appendMap(out, metadata.labels(), false);
        }
        out.append("</header>");
    }

    private void appendSummary(StringBuilder out, UiTestLensSession session, TraceHtmlExportOptions options) {
        List<TraceEvent> events = session.events();
        long failed = events.stream().filter(this::isFailedOrError).count();
        long warnings = events.stream().filter(event -> event.status() == TraceStatus.WARNING).count();
        long passed = events.stream().filter(event -> event.status() == TraceStatus.PASSED).count();
        long info = events.stream().filter(event -> event.status() == TraceStatus.INFO).count();
        long skipped = events.stream().filter(event -> event.status() == TraceStatus.SKIPPED).count();
        long stepPassed = count(events, TraceEventType.STEP_PASSED, TraceStatus.PASSED);
        long stepFailed = count(events, TraceEventType.STEP_FAILED, TraceStatus.FAILED) + count(events, TraceEventType.STEP_FAILED, TraceStatus.ERROR);
        long assertionPassed = count(events, TraceEventType.ASSERTION_PASSED, TraceStatus.PASSED);
        long assertionFailed = count(events, TraceEventType.ASSERTION_FAILED, TraceStatus.FAILED) + count(events, TraceEventType.ASSERTION_FAILED, TraceStatus.ERROR);
        long locatorFailures = events.stream().filter(event ->
                (event.type() == TraceEventType.LOCATOR_RESOLVE || event.type() == TraceEventType.LOCATOR_ACTION
                        || event.type() == TraceEventType.ACTION_FAILED)
                        && isFailedOrError(event)).count();
        long networkFailures = events.stream().filter(event ->
                (event.type() == TraceEventType.NETWORK_EVENT || event.type() == TraceEventType.NETWORK_WAIT)
                        && isFailedOrError(event)).count();
        long screenshots = session.artifacts().stream().filter(artifact -> artifact.type() == TraceArtifactType.SCREENSHOT).count();
        long videos = session.artifacts().stream().filter(artifact -> artifact.type() == TraceArtifactType.VIDEO).count();
        out.append("<section><h2>Summary</h2><div class=\"cards\">");
        card(out, "Total events", String.valueOf(events.size()));
        card(out, "PASS", String.valueOf(passed));
        card(out, "Failed/Error events", String.valueOf(failed));
        card(out, "WARN", String.valueOf(warnings));
        card(out, "INFO", String.valueOf(info));
        card(out, "SKIPPED", String.valueOf(skipped));
        card(out, "Steps passed/failed", stepPassed + " / " + stepFailed);
        card(out, "Assertions passed/failed", assertionPassed + " / " + assertionFailed);
        card(out, "Locator/action failures", String.valueOf(locatorFailures));
        card(out, "Network failures/waits", String.valueOf(networkFailures));
        card(out, "Artifacts", String.valueOf(session.artifacts().size()));
        card(out, "Screenshots", String.valueOf(screenshots));
        card(out, "Videos", String.valueOf(videos));
        if (options.includeDurationSummary()) {
            card(out, "Total event duration", duration(totalDuration(events)));
            card(out, "Slowest event", slowestEvent(events));
        }
        out.append("</div></section>");
    }

    private void appendEventTypeSummary(StringBuilder out, List<TraceEvent> events) {
        Map<EventCategory, List<TraceEvent>> grouped = groupedEvents(events);
        out.append("<section><h2>Event type summary</h2><div class=\"table-wrap\"><table><thead><tr>")
                .append("<th>Category</th><th>Total</th><th>Passed</th><th>Failed/Error</th><th>Warnings</th>")
                .append("</tr></thead><tbody>");
        for (EventCategory category : EventCategory.values()) {
            List<TraceEvent> categoryEvents = grouped.getOrDefault(category, List.of());
            if (categoryEvents.isEmpty()) {
                continue;
            }
            out.append("<tr><td>").append(categoryBadge(category)).append("</td>")
                    .append("<td>").append(categoryEvents.size()).append("</td>")
                    .append("<td>").append(categoryEvents.stream().filter(event -> event.status() == TraceStatus.PASSED).count()).append("</td>")
                    .append("<td>").append(categoryEvents.stream().filter(this::isFailedOrError).count()).append("</td>")
                    .append("<td>").append(categoryEvents.stream().filter(event -> event.status() == TraceStatus.WARNING).count()).append("</td></tr>");
        }
        out.append("</tbody></table></div></section>");
    }

    private void appendTimeline(StringBuilder out, List<TraceEvent> events, TraceHtmlExportOptions options) {
        out.append("<section><h2>Timeline</h2>");
        if (options.groupTimelineByCategory()) {
            Map<EventCategory, List<TraceEvent>> grouped = groupedEvents(filteredTimelineEvents(events, options));
            for (EventCategory category : EventCategory.values()) {
                List<TraceEvent> categoryEvents = grouped.getOrDefault(category, List.of());
                if (categoryEvents.isEmpty()) {
                    continue;
                }
                out.append("<h3>").append(escape(category.label)).append("</h3>");
                appendTimelineTable(out, categoryEvents, options);
            }
        } else {
            appendTimelineTable(out, filteredTimelineEvents(events, options), options);
        }
        out.append("</section>");
    }

    private List<TraceEvent> filteredTimelineEvents(List<TraceEvent> events, TraceHtmlExportOptions options) {
        return events.stream()
                .filter(event -> !(options.collapsePassedEvents() && event.status() == TraceStatus.PASSED))
                .toList();
    }

    private void appendTimelineTable(StringBuilder out, List<TraceEvent> events, TraceHtmlExportOptions options) {
        boolean showAttributes = options.includeAttributes() && !options.compactTimeline();
        int messageLimit = options.compactTimeline() ? Math.min(options.maxMessageLength(), 160) : options.maxMessageLength();
        Instant firstTimestamp = events.stream()
                .map(TraceEvent::timestamp)
                .filter(timestamp -> timestamp != null)
                .min(Instant::compareTo)
                .orElse(null);
        out.append("<div class=\"table-wrap\"><table class=\"timeline\"><thead><tr>")
                .append("<th>Time</th><th>Offset</th><th>Category</th><th>Type</th><th>Status</th><th>Name</th><th>Message</th><th>Duration</th><th>Parent</th>");
        if (showAttributes) {
            out.append("<th>Details</th>");
        }
        out.append("</tr></thead><tbody>");
        for (TraceEvent event : events) {
            out.append("<tr id=\"event-").append(escape(event.id())).append("\"><td class=\"mono\">")
                    .append(escape(shortInstant(event.timestamp()))).append("</td>")
                    .append("<td class=\"mono\">").append(escape(offset(firstTimestamp, event.timestamp()))).append("</td>")
                    .append("<td>").append(categoryBadge(categoryFor(event))).append("</td>")
                    .append("<td>").append(typeBadge(event.type())).append("</td>")
                    .append("<td>").append(badge(event.status())).append("</td>")
                    .append("<td><strong>").append(escape(event.name())).append("</strong></td>")
                    .append("<td>").append(escape(preview(event.message(), messageLimit))).append("</td>")
                    .append("<td class=\"mono\">").append(escape(duration(event.duration()))).append("</td>")
                    .append("<td class=\"muted mono\">").append(escape(event.parentId())).append("</td>");
            if (showAttributes) {
                out.append("<td>");
                appendDetailsMap(out, "Attributes", event.attributes());
                out.append("</td>");
            }
            out.append("</tr>");
        }
        if (events.isEmpty()) {
            int colspan = showAttributes ? 10 : 9;
            out.append("<tr><td colspan=\"").append(colspan).append("\" class=\"muted\">No timeline events recorded.</td></tr>");
        }
        out.append("</tbody></table></div>");
    }

    private void appendSteps(StringBuilder out, List<TraceEvent> events, TraceHtmlExportOptions options) {
        out.append("<section><h2>Steps</h2><div class=\"event-list\">");
        int written = 0;
        for (TraceEvent event : events) {
            if (!isStepEvent(event)) {
                continue;
            }
            written++;
            out.append("<article class=\"event-card\"><div class=\"event-title\"><strong>")
                    .append(escape(event.name()))
                    .append("</strong>")
                    .append(badge(event.status()))
                    .append("</div><p>")
                    .append(escape(preview(event.message(), options.maxMessageLength())))
                    .append("</p><p class=\"muted mono\">")
                    .append(escape(event.type().name()))
                    .append(" / ")
                    .append(escape(duration(event.duration())))
                    .append("</p>");
            appendFailureBlock(out, event.failure(), options);
            out.append("</article>");
        }
        if (written == 0) {
            out.append("<p class=\"muted\">No step events recorded.</p>");
        }
        out.append("</div></section>");
    }

    private void appendFailureSummary(StringBuilder out, List<TraceEvent> events, TraceHtmlExportOptions options) {
        List<TraceEvent> failures = events.stream()
                .filter(event -> event.failure() != null || isFailedOrError(event))
                .toList();
        out.append("<section><h2>Failure summary</h2>");
        if (failures.isEmpty()) {
            out.append("<p class=\"ok-line\">No failures recorded.</p></section>");
            return;
        }
        out.append("<div class=\"event-list\">");
        for (TraceEvent event : failures) {
            out.append("<article class=\"event-card failure\"><div class=\"event-title\"><strong>")
                    .append(typeBadge(event.type()))
                    .append(" ")
                    .append(escape(event.name()))
                    .append("</strong>")
                    .append(badge(event.status()))
                    .append("</div><p>")
                    .append(escape(preview(event.message(), options.maxMessageLength())))
                    .append("</p><p><a href=\"#event-")
                    .append(escape(event.id()))
                    .append("\">Open timeline event</a></p>");
            appendFailureBlock(out, event.failure(), options);
            out.append("</article>");
        }
        out.append("</div></section>");
    }

    private void appendArtifacts(StringBuilder out, List<TraceArtifact> artifacts, TraceHtmlExportOptions options, Path artifactBaseDirectory) {
        out.append("<section><h2>Artifacts</h2>");
        if (artifacts.isEmpty()) {
            out.append("<p class=\"muted\">No artifacts attached.</p></section>");
            return;
        }
        if (options.includeArtifactPreview()) {
            out.append("<div class=\"artifact-grid\">");
            for (TraceArtifact artifact : artifacts) {
                out.append("<article class=\"artifact-card\"><div class=\"event-title\"><strong>")
                        .append(escape(artifact.name()))
                        .append("</strong>")
                        .append(artifactBadge(artifact.type()))
                        .append("</div><p class=\"muted\">")
                        .append(escape(artifact.mediaType()))
                        .append("</p>");
                appendArtifactFileLink(out, "Path", artifact.path(), artifactBaseDirectory);
                appendArtifactLink(out, "URL", artifact.url());
                appendDetailsMap(out, "Metadata", artifact.metadata());
                out.append("</article>");
            }
            out.append("</div>");
        } else {
            out.append("<div class=\"table-wrap\"><table><thead><tr>")
                    .append("<th>Name</th><th>Type</th><th>Path</th><th>URL</th><th>Media type</th><th>Created</th><th>Metadata</th>")
                    .append("</tr></thead><tbody>");
            for (TraceArtifact artifact : artifacts) {
                out.append("<tr><td>").append(escape(artifact.name())).append("</td>")
                        .append("<td>").append(artifactBadge(artifact.type())).append("</td>")
                        .append("<td>").append(fileLinkOrText(artifact.path(), artifactBaseDirectory)).append("</td>")
                        .append("<td>").append(linkOrText(artifact.url())).append("</td>")
                        .append("<td>").append(escape(artifact.mediaType())).append("</td>")
                        .append("<td>").append(escape(shortInstant(artifact.createdAt()))).append("</td><td>");
                appendDetailsMap(out, "Metadata", artifact.metadata());
                out.append("</td></tr>");
            }
            out.append("</tbody></table></div>");
        }
        out.append("</section>");
    }

    private void appendArtifactFileLink(StringBuilder out, String label, String value, Path artifactBaseDirectory) {
        if (value == null || value.isBlank()) {
            return;
        }
        out.append("<p><span class=\"muted\">")
                .append(escape(label))
                .append(":</span> ")
                .append(fileLinkOrText(value, artifactBaseDirectory))
                .append("</p>");
    }

    private void appendArtifactLink(StringBuilder out, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        out.append("<p><span class=\"muted\">")
                .append(escape(label))
                .append(":</span> ")
                .append(linkOrText(value))
                .append("</p>");
    }

    private void appendRawJson(StringBuilder out, UiTestLensSession session, TraceHtmlExportOptions options) {
        out.append("<section><details><summary>Raw JSON</summary><pre>")
                .append(escape(new TraceJsonExporter().export(session, options.includeStackTraces())))
                .append("</pre></details></section>");
    }

    private void appendFailureBlock(StringBuilder out, TraceFailure failure, TraceHtmlExportOptions options) {
        if (failure == null) {
            return;
        }
        out.append("<div class=\"failure-box\"><p><strong>Failure:</strong> ")
                .append(escape(failure.message()))
                .append("</p><p><strong>Exception:</strong> <span class=\"mono\">")
                .append(escape(failure.exceptionType()))
                .append("</span></p>");
        appendDetailsMap(out, "Failure details", failure.details());
        if (options.includeStackTraces() && !failure.stackTrace().isBlank()) {
            out.append("<details><summary>Stack trace</summary><pre>")
                    .append(escape(failure.stackTrace()))
                    .append("</pre></details>");
        }
        out.append("</div>");
    }

    private void appendMap(StringBuilder out, Map<String, String> map, boolean compact) {
        if (map == null || map.isEmpty()) {
            out.append("<span class=\"muted\">-</span>");
            return;
        }
        out.append("<dl class=\"kv").append(compact ? " compact" : "").append("\">");
        for (Map.Entry<String, String> entry : new TreeMap<>(map).entrySet()) {
            out.append("<dt>").append(escape(entry.getKey())).append("</dt><dd>")
                    .append(escape(entry.getValue()))
                    .append("</dd>");
        }
        out.append("</dl>");
    }

    private void appendDetailsMap(StringBuilder out, String summary, Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            out.append("<span class=\"muted\">-</span>");
            return;
        }
        out.append("<details class=\"details\"><summary>")
                .append(escape(summary))
                .append("</summary>");
        appendMap(out, map, true);
        out.append("</details>");
    }

    private void metadata(StringBuilder out, String label, String value) {
        out.append("<div class=\"meta\"><span>").append(escape(label)).append("</span><strong>")
                .append(escape(value == null || value.isBlank() ? "-" : value))
                .append("</strong></div>");
    }

    private void metadataHtml(StringBuilder out, String label, String html) {
        out.append("<div class=\"meta\"><span>").append(escape(label)).append("</span><strong>")
                .append(html == null || html.isBlank() ? "-" : html)
                .append("</strong></div>");
    }

    private void card(StringBuilder out, String label, String value) {
        out.append("<div class=\"card\"><span>").append(escape(label)).append("</span><strong>")
                .append(escape(value))
                .append("</strong></div>");
    }

    private String badge(TraceStatus status) {
        TraceStatus effectiveStatus = status == null ? TraceStatus.INFO : status;
        return "<span class=\"badge status " + cssClass(effectiveStatus) + "\">" + escape(statusLabel(effectiveStatus)) + "</span>";
    }

    private String typeBadge(TraceEventType type) {
        TraceEventType effectiveType = type == null ? TraceEventType.CUSTOM : type;
        return "<span class=\"badge type\">" + escape(effectiveType.name()) + "</span>";
    }

    private String categoryBadge(EventCategory category) {
        EventCategory effectiveCategory = category == null ? EventCategory.OTHER : category;
        return "<span class=\"badge category category-" + effectiveCategory.name().toLowerCase() + "\">"
                + escape(effectiveCategory.label)
                + "</span>";
    }

    private String artifactBadge(TraceArtifactType type) {
        TraceArtifactType effectiveType = type == null ? TraceArtifactType.CUSTOM_FILE : type;
        return "<span class=\"badge artifact artifact-" + effectiveType.name().toLowerCase() + "\">"
                + escape(effectiveType.name())
                + "</span>";
    }

    private String linkOrText(String value) {
        if (value == null || value.isBlank()) {
            return "<span class=\"muted\">-</span>";
        }
        String escaped = escape(value);
        return "<a class=\"mono\" href=\"" + escaped + "\">" + escaped + "</a>";
    }

    private String fileLinkOrText(String value, Path artifactBaseDirectory) {
        if (value == null || value.isBlank()) {
            return "<span class=\"muted\">-</span>";
        }

        Path artifactPath;
        try {
            artifactPath = Path.of(value);
        } catch (RuntimeException e) {
            return linkOrText(value);
        }

        Path absolutePath = artifactPath.isAbsolute()
                ? artifactPath.normalize()
                : Path.of("").toAbsolutePath().resolve(artifactPath).normalize();
        boolean exists = Files.exists(absolutePath);
        String href = value;
        if (artifactBaseDirectory != null) {
            try {
                href = artifactBaseDirectory.toAbsolutePath().normalize().relativize(absolutePath).toString();
            } catch (IllegalArgumentException ignored) {
                href = value;
            }
        }
        href = href.replace('\\', '/');
        String escapedHref = escape(href);
        String escapedLabel = escape(value);
        String link = "<a class=\"mono\" href=\"" + escapedHref + "\">" + escapedLabel + "</a>";
        if (!exists) {
            return link + " <span class=\"artifact-warning\">missing file</span>";
        }
        if (isImagePath(value)) {
            return link + "<img class=\"artifact-thumb\" src=\"" + escapedHref + "\" alt=\"" + escapedLabel + "\">";
        }
        return link;
    }

    private Map<EventCategory, List<TraceEvent>> groupedEvents(List<TraceEvent> events) {
        Map<EventCategory, List<TraceEvent>> grouped = new EnumMap<>(EventCategory.class);
        for (TraceEvent event : events == null ? List.<TraceEvent>of() : events) {
            grouped.computeIfAbsent(categoryFor(event), ignored -> new ArrayList<>()).add(event);
        }
        return grouped;
    }

    private EventCategory categoryFor(TraceEvent event) {
        if (event == null || event.type() == null) {
            return EventCategory.OTHER;
        }
        return switch (event.type()) {
            case SESSION_STARTED, SESSION_FINISHED -> EventCategory.SESSION;
            case STEP_STARTED, STEP_PASSED, STEP_FAILED -> EventCategory.STEPS;
            case LOCATOR_RESOLVE, LOCATOR_ACTION -> EventCategory.LOCATORS;
            case ACTION_STARTED, ACTION_PASSED, ACTION_FAILED -> EventCategory.ACTIONS;
            case ACTIONABILITY_CHECK -> EventCategory.ACTIONABILITY;
            case ASSERTION_STARTED, ASSERTION_PASSED, ASSERTION_FAILED -> EventCategory.ASSERTIONS;
            case BUSINESS_ASSERTION_STARTED, BUSINESS_ASSERTION_PASSED, BUSINESS_ASSERTION_FAILED -> EventCategory.BUSINESS;
            case OVERLAY_DETECTED, OVERLAY_HANDLED -> EventCategory.OVERLAYS;
            case SCREENSHOT, VIDEO, ARTIFACT_ATTACHED -> EventCategory.EVIDENCE;
            case NETWORK_EVENT, NETWORK_WAIT -> EventCategory.NETWORK;
            case CUSTOM -> EventCategory.OTHER;
        };
    }

    private boolean isStepEvent(TraceEvent event) {
        return event.type() == TraceEventType.STEP_STARTED
                || event.type() == TraceEventType.STEP_PASSED
                || event.type() == TraceEventType.STEP_FAILED;
    }

    private boolean isFailedOrError(TraceEvent event) {
        return event.status() == TraceStatus.FAILED || event.status() == TraceStatus.ERROR;
    }

    private boolean isFailedOrErrorStatus(TraceStatus status) {
        return status == TraceStatus.FAILED || status == TraceStatus.ERROR;
    }

    private TraceStatus suiteStatus(List<UiTestLensSession> sessions) {
        if (sessions.stream().anyMatch(session -> isFailedOrErrorStatus(suiteSessionStatus(session)))) {
            return TraceStatus.FAILED;
        }
        if (sessions.stream().anyMatch(this::hasWarning)) {
            return TraceStatus.WARNING;
        }
        if (sessions.isEmpty()) {
            return TraceStatus.INFO;
        }
        if (sessions.stream().allMatch(session -> suiteSessionStatus(session) == TraceStatus.SKIPPED)) {
            return TraceStatus.SKIPPED;
        }
        return TraceStatus.PASSED;
    }

    private TraceStatus suiteSessionStatus(UiTestLensSession session) {
        TraceStatus status = session.metadata().status();
        if (status == TraceStatus.STARTED && failureCount(session) > 0) {
            return TraceStatus.FAILED;
        }
        return status == null ? TraceStatus.INFO : status;
    }

    private boolean hasWarning(UiTestLensSession session) {
        return session.events().stream().anyMatch(event -> event.status() == TraceStatus.WARNING);
    }

    private long failureCount(UiTestLensSession session) {
        return session.events().stream()
                .filter(event -> event.failure() != null || isFailedOrError(event))
                .count();
    }

    private Duration totalSessionDuration(List<UiTestLensSession> sessions) {
        Duration total = Duration.ZERO;
        for (UiTestLensSession session : sessions) {
            Instant startedAt = session.metadata().startedAt();
            Instant finishedAt = session.metadata().finishedAt();
            if (startedAt != null && finishedAt != null) {
                total = total.plus(Duration.between(startedAt, finishedAt));
            }
        }
        return total;
    }

    private String sessionAnchor(UiTestLensSession session) {
        return "session-" + session.id().replaceAll("[^A-Za-z0-9_-]", "-");
    }

    private long count(List<TraceEvent> events, TraceEventType type, TraceStatus status) {
        return events.stream().filter(event -> event.type() == type && event.status() == status).count();
    }

    private Duration totalDuration(List<TraceEvent> events) {
        Duration total = Duration.ZERO;
        for (TraceEvent event : events) {
            if (event.duration() != null) {
                total = total.plus(event.duration());
            }
        }
        return total;
    }

    private String slowestEvent(List<TraceEvent> events) {
        return events.stream()
                .filter(event -> event.duration() != null)
                .max(Comparator.comparing(TraceEvent::duration))
                .map(event -> event.name() + " / " + duration(event.duration()))
                .orElse("-");
    }

    private String cssClass(TraceStatus status) {
        return switch (status) {
            case PASSED -> "passed";
            case FAILED, ERROR -> "failed";
            case WARNING -> "warning";
            case SKIPPED -> "skipped";
            case STARTED -> "started";
            case INFO -> "info";
        };
    }

    private String statusLabel(TraceStatus status) {
        return switch (status) {
            case PASSED -> "PASS";
            case FAILED, ERROR -> "FAIL";
            case WARNING -> "WARN";
            case SKIPPED -> "SKIPPED";
            case STARTED -> "STARTED";
            case INFO -> "INFO";
        };
    }

    private String duration(Instant startedAt, Instant finishedAt) {
        if (startedAt == null || finishedAt == null) {
            return "-";
        }
        return duration(Duration.between(startedAt, finishedAt));
    }

    private String duration(Duration duration) {
        if (duration == null) {
            return "-";
        }
        return Math.max(0, duration.toMillis()) + " ms";
    }

    private String offset(Instant firstTimestamp, Instant timestamp) {
        if (firstTimestamp == null || timestamp == null) {
            return "-";
        }
        return "+" + Math.max(0, Duration.between(firstTimestamp, timestamp).toMillis()) + " ms";
    }

    private boolean isImagePath(String value) {
        String lower = value == null ? "" : value.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".webp");
    }

    private String shortInstant(Instant instant) {
        if (instant == null) {
            return "";
        }
        String text = instant.toString();
        return text.endsWith("Z") ? text.replace("T", " ").replace("Z", " UTC") : text;
    }

    private String preview(String value, int maxLength) {
        String safe = value == null ? "" : value;
        int effectiveLimit = Math.max(0, maxLength);
        if (safe.length() <= effectiveLimit) {
            return safe;
        }
        return safe.substring(0, effectiveLimit) + "...";
    }

    private String escape(String value) {
        return TraceHtmlEscaper.escape(value);
    }

    private String css(HtmlReportTheme theme) {
        return themeVariables(theme == null ? HtmlReportTheme.AUTO : theme) + """
                *{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:14px/1.5 system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif}body:before{content:"";display:block;height:4px;background:linear-gradient(90deg,var(--success),var(--accent),var(--info),var(--warning),var(--danger))}
                .hero,section{max-width:1240px;margin:22px auto;padding:24px;background:var(--panel);border:1px solid var(--border);border-radius:8px;box-shadow:var(--shadow)}.hero{margin-top:0;border-radius:0 0 8px 8px}.session-detail{margin:18px 0;padding:18px;border:1px solid var(--border);border-radius:8px;background:var(--panel-muted)}
                .eyebrow{margin:0 0 4px;color:var(--accent);text-transform:uppercase;font-size:12px;letter-spacing:.08em}h1{margin:0 0 18px;font-size:28px;letter-spacing:0}h2{margin:0 0 14px;font-size:19px;letter-spacing:0}h3{margin:18px 0 10px;font-size:15px;color:var(--heading);letter-spacing:0}
                .metadata-grid,.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:12px}.meta,.card{padding:12px;border:1px solid var(--border);border-radius:8px;background:var(--panel-muted)}.meta span,.card span{display:block;color:var(--muted);font-size:12px}.meta strong,.card strong{display:block;margin-top:4px;word-break:break-word}
                .badge{display:inline-flex;align-items:center;gap:4px;margin:0 4px 0 0;padding:2px 8px;border-radius:999px;font-size:12px;font-weight:800;white-space:nowrap;border:1px solid transparent}.status.passed{background:var(--success-bg);border-color:var(--success-border);color:var(--success-text)}.status.failed{background:var(--danger-bg);border-color:var(--danger-border);color:var(--danger-text)}.status.warning{background:var(--warning-bg);border-color:var(--warning-border);color:var(--warning-text)}.status.skipped,.status.info{background:var(--badge-bg);color:var(--badge-text)}.status.started{background:var(--info-bg);border-color:var(--info-border);color:var(--info-text)}.type{background:var(--type-bg);color:var(--type-text)}.category{background:var(--category-bg);color:var(--category-text)}.category-network{background:var(--network-bg);color:var(--network-text)}.category-evidence{background:var(--evidence-bg);color:var(--evidence-text)}.artifact{background:var(--artifact-bg);color:var(--artifact-text)}
                .table-wrap{overflow:auto;border:1px solid var(--border);border-radius:8px;background:var(--panel-muted)}table{width:100%;border-collapse:collapse}th,td{padding:9px;border-bottom:1px solid var(--border-soft);vertical-align:top;text-align:left}tr:hover td{background:var(--row-hover)}th{position:sticky;top:0;background:var(--table-head);color:var(--table-head-text);font-size:12px;text-transform:uppercase;z-index:1}
                .timeline td{min-width:90px}.event-list{display:grid;gap:12px}.event-card,.artifact-card{padding:14px;border:1px solid var(--border);border-radius:8px;background:var(--panel-muted)}.event-card.failure{border-color:var(--danger-border);background:var(--danger-bg)}.event-title{display:flex;align-items:flex-start;justify-content:space-between;gap:8px}.artifact-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:12px}
                .failure-box{margin-top:10px;padding:10px;border-left:4px solid var(--danger);background:var(--code-bg)}.ok-line{padding:10px 12px;border:1px solid var(--success-border);background:var(--success-bg);border-radius:8px;color:var(--success-text)}.muted{color:var(--muted)}.mono{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:12px}.kv{margin:0;display:grid;grid-template-columns:max-content 1fr;gap:4px 10px}.kv.compact{margin-top:8px}.kv dt{font-weight:700}.kv dd{margin:0;word-break:break-word}.details summary{cursor:pointer;color:var(--heading);font-weight:700}pre{white-space:pre-wrap;overflow:auto;background:var(--code-bg);color:var(--code-text);padding:14px;border-radius:8px;border:1px solid var(--border)}a{color:var(--link);word-break:break-word}.artifact-warning{display:inline-flex;margin-left:6px;padding:1px 6px;border-radius:999px;background:var(--warning-bg);color:var(--warning-text);font-size:12px;font-weight:700}.artifact-thumb{display:block;max-width:100%;max-height:180px;margin-top:10px;border:1px solid var(--border);border-radius:8px;object-fit:contain;background:var(--code-bg)}
                @media (max-width:720px){.hero,section{margin:14px 10px;padding:16px}.event-title{display:block}.badge{margin-top:4px}}@media print{body{background:#fff;color:#111}.hero,section,.session-detail{box-shadow:none;break-inside:avoid}.table-wrap{overflow:visible}th{position:static}}
                """;
    }

    private String themeVariables(HtmlReportTheme theme) {
        return switch (theme) {
            case LIGHT -> lightVariables();
            case DARK -> darkVariables();
            case AUTO -> lightVariables() + "@media (prefers-color-scheme: dark){" + darkVariableBody() + "}";
        };
    }

    private String lightVariables() {
        return ":root{" + lightVariableBody() + "}";
    }

    private String darkVariables() {
        return ":root{" + darkVariableBody() + "}";
    }

    private String lightVariableBody() {
        return "color-scheme:light;--bg:#f5f7fb;--panel:#ffffff;--panel-muted:#f8fafc;--text:#172033;--muted:#667085;--border:#d8dee9;--border-soft:#e8edf5;--accent:#2563eb;--success:#16a34a;--warning:#ca8a04;--danger:#dc2626;--info:#0891b2;--code-bg:#111827;--code-text:#f9fafb;--heading:#27364a;--shadow:0 10px 28px rgba(15,23,42,.08);--success-bg:#dcfce7;--success-border:#86efac;--success-text:#166534;--danger-bg:#fee2e2;--danger-border:#fecaca;--danger-text:#991b1b;--warning-bg:#fef3c7;--warning-border:#fde68a;--warning-text:#92400e;--info-bg:#dbeafe;--info-border:#93c5fd;--info-text:#1d4ed8;--badge-bg:#e5e7eb;--badge-text:#374151;--type-bg:#eef2ff;--type-text:#3730a3;--category-bg:#f1f5f9;--category-text:#334155;--network-bg:#ccfbf1;--network-text:#115e59;--evidence-bg:#ede9fe;--evidence-text:#5b21b6;--artifact-bg:#ecfeff;--artifact-text:#155e75;--table-head:#f9fafb;--table-head-text:#374151;--row-hover:#f8fbff;--link:#1d4ed8;";
    }

    private String darkVariableBody() {
        return "color-scheme:dark;--bg:#070b12;--panel:#101722;--panel-muted:#0c121b;--text:#e6edf7;--muted:#93a4b8;--border:#273244;--border-soft:#1c2635;--accent:#38bdf8;--success:#39ff14;--warning:#facc15;--danger:#ff4d6d;--info:#2dd4bf;--code-bg:#05070b;--code-text:#e6edf7;--heading:#c8d4e3;--shadow:0 18px 44px rgba(0,0,0,.32);--success-bg:rgba(57,255,20,.12);--success-border:rgba(57,255,20,.45);--success-text:#9cff8d;--danger-bg:rgba(255,77,109,.14);--danger-border:rgba(255,77,109,.5);--danger-text:#ff9aad;--warning-bg:rgba(250,204,21,.14);--warning-border:rgba(250,204,21,.45);--warning-text:#fde68a;--info-bg:rgba(56,189,248,.14);--info-border:rgba(56,189,248,.45);--info-text:#7dd3fc;--badge-bg:#243044;--badge-text:#cbd5e1;--type-bg:#172554;--type-text:#bfdbfe;--category-bg:#172033;--category-text:#cbd5e1;--network-bg:rgba(45,212,191,.14);--network-text:#99f6e4;--evidence-bg:rgba(192,132,252,.16);--evidence-text:#e9d5ff;--artifact-bg:rgba(56,189,248,.12);--artifact-text:#bae6fd;--table-head:#121b29;--table-head-text:#aab8ca;--row-hover:#121b29;--link:#7dd3fc;";
    }
}
