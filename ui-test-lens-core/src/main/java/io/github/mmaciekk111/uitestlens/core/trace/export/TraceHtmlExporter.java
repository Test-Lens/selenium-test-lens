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
import java.util.TreeMap;

/**
 * Exports a {@link UiTestLensSession} as a standalone HTML trace report.
 *
 * <p>The exporter renders metadata, categorized timeline events, failures, artifacts, and optional raw JSON.
 */
public final class TraceHtmlExporter {
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
        if (session == null) {
            return emptyReport(options);
        }
        TraceHtmlExportOptions effectiveOptions = options == null ? TraceHtmlExportOptions.defaults() : options;
        StringBuilder out = new StringBuilder(48_000);
        out.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>").append(escape(effectiveOptions.title())).append("</title>")
                .append("<style>").append(css()).append("</style></head><body>");
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
            appendArtifacts(out, session.artifacts(), effectiveOptions);
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
            Files.writeString(outputPath, export(session, options));
            return outputPath;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String emptyReport(TraceHtmlExportOptions options) {
        TraceHtmlExportOptions effectiveOptions = options == null ? TraceHtmlExportOptions.defaults() : options;
        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>"
                + escape(effectiveOptions.title())
                + "</title></head><body><h1>"
                + escape(effectiveOptions.title())
                + "</h1><p>No UI Test Lens session available.</p></body></html>";
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
        card(out, "Failed/Error events", String.valueOf(failed));
        card(out, "Warnings", String.valueOf(warnings));
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
        out.append("<div class=\"table-wrap\"><table class=\"timeline\"><thead><tr>")
                .append("<th>Time</th><th>Category</th><th>Type</th><th>Status</th><th>Name</th><th>Message</th><th>Duration</th><th>Parent</th>");
        if (showAttributes) {
            out.append("<th>Details</th>");
        }
        out.append("</tr></thead><tbody>");
        for (TraceEvent event : events) {
            out.append("<tr id=\"event-").append(escape(event.id())).append("\"><td class=\"mono\">")
                    .append(escape(shortInstant(event.timestamp()))).append("</td>")
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
            int colspan = showAttributes ? 9 : 8;
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

    private void appendArtifacts(StringBuilder out, List<TraceArtifact> artifacts, TraceHtmlExportOptions options) {
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
                appendArtifactLink(out, "Path", artifact.path());
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
                        .append("<td>").append(linkOrText(artifact.path())).append("</td>")
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
        return "<span class=\"badge status " + cssClass(effectiveStatus) + "\">" + escape(effectiveStatus.name()) + "</span>";
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

    private String css() {
        return """
                :root{color-scheme:light;--bg:#f6f7fb;--panel:#fff;--text:#172033;--muted:#667085;--line:#d8dee9;--line-soft:#edf1f7;--blue:#dbeafe;--green:#dcfce7;--red:#fee2e2;--yellow:#fef3c7;--gray:#f3f4f6;--purple:#ede9fe;--teal:#ccfbf1}
                *{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:14px/1.5 system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif}body:before{content:"";display:block;height:6px;background:linear-gradient(90deg,#2563eb,#0f766e,#7c3aed)}
                .hero,section{max-width:1220px;margin:24px auto;padding:24px;background:var(--panel);border:1px solid var(--line);border-radius:8px;box-shadow:0 1px 2px rgba(15,23,42,.04)}.hero{margin-top:0;border-radius:0 0 8px 8px}
                .eyebrow{margin:0 0 4px;color:var(--muted);text-transform:uppercase;font-size:12px;letter-spacing:.08em}h1{margin:0 0 18px;font-size:28px;letter-spacing:0}h2{margin:0 0 14px;font-size:19px;letter-spacing:0}h3{margin:18px 0 10px;font-size:15px;color:#344054;letter-spacing:0}
                .metadata-grid,.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:12px}.meta,.card{padding:12px;border:1px solid var(--line);border-radius:8px;background:#fbfcfe}.meta span,.card span{display:block;color:var(--muted);font-size:12px}.meta strong,.card strong{display:block;margin-top:4px;word-break:break-word}
                .badge{display:inline-flex;align-items:center;gap:4px;margin:0 4px 0 0;padding:2px 8px;border-radius:999px;font-size:12px;font-weight:700;white-space:nowrap}.status.passed{background:var(--green);color:#166534}.status.failed{background:var(--red);color:#991b1b}.status.warning{background:var(--yellow);color:#92400e}.status.skipped,.status.info{background:var(--gray);color:#374151}.status.started{background:var(--blue);color:#1d4ed8}.type{background:#eef2ff;color:#3730a3}.category{background:#f1f5f9;color:#334155}.category-network{background:var(--teal);color:#115e59}.category-evidence{background:var(--purple);color:#5b21b6}.artifact{background:#ecfeff;color:#155e75}
                .table-wrap{overflow:auto;border:1px solid var(--line);border-radius:8px}table{width:100%;border-collapse:collapse}th,td{padding:9px;border-bottom:1px solid var(--line-soft);vertical-align:top;text-align:left}tr:hover td{background:#fafcff}th{position:sticky;top:0;background:#f9fafb;color:#374151;font-size:12px;text-transform:uppercase;z-index:1}
                .timeline td{min-width:90px}.event-list{display:grid;gap:12px}.event-card,.artifact-card{padding:14px;border:1px solid var(--line);border-radius:8px;background:#fbfcfe}.event-card.failure{border-color:#fecaca;background:#fff7f7}.event-title{display:flex;align-items:flex-start;justify-content:space-between;gap:8px}.artifact-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:12px}
                .failure-box{margin-top:10px;padding:10px;border-left:4px solid #ef4444;background:#fff}.ok-line{padding:10px 12px;border:1px solid #bbf7d0;background:#f0fdf4;border-radius:8px;color:#166534}.muted{color:var(--muted)}.mono{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:12px}.kv{margin:0;display:grid;grid-template-columns:max-content 1fr;gap:4px 10px}.kv.compact{margin-top:8px}.kv dt{font-weight:700}.kv dd{margin:0;word-break:break-word}.details summary{cursor:pointer;color:#344054;font-weight:700}pre{white-space:pre-wrap;overflow:auto;background:#111827;color:#f9fafb;padding:14px;border-radius:8px}a{color:#1d4ed8;word-break:break-word}
                @media print{body{background:#fff}.hero,section{box-shadow:none;break-inside:avoid}.table-wrap{overflow:visible}th{position:static}}
                """;
    }
}
