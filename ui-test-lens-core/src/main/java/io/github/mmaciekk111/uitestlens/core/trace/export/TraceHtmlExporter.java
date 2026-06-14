package io.github.mmaciekk111.uitestlens.core.trace.export;

import io.github.mmaciekk111.uitestlens.core.trace.TraceArtifact;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class TraceHtmlExporter {

    public String export(UiTestLensSession session) {
        return export(session, TraceHtmlExportOptions.defaults());
    }

    public String export(UiTestLensSession session, TraceHtmlExportOptions options) {
        if (session == null) {
            return emptyReport(options);
        }
        TraceHtmlExportOptions effectiveOptions = options == null ? TraceHtmlExportOptions.defaults() : options;
        StringBuilder out = new StringBuilder(32_768);
        out.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>").append(escape(effectiveOptions.title())).append("</title>")
                .append("<style>").append(css()).append("</style></head><body>");
        appendHeader(out, session, effectiveOptions);
        appendSummary(out, session);
        appendTimeline(out, session.events(), effectiveOptions);
        appendSteps(out, session.events(), effectiveOptions);
        appendFailures(out, session.events(), effectiveOptions);
        if (effectiveOptions.includeArtifacts()) {
            appendArtifacts(out, session.artifacts());
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
        metadata(out, "Started", string(metadata.startedAt()));
        metadata(out, "Finished", string(metadata.finishedAt()));
        metadata(out, "Duration", duration(metadata.startedAt(), metadata.finishedAt()));
        metadata(out, "Environment", metadata.environment());
        out.append("</div>");
        if (!metadata.labels().isEmpty()) {
            out.append("<h2>Labels</h2>");
            appendMap(out, metadata.labels());
        }
        out.append("</header>");
    }

    private void appendSummary(StringBuilder out, UiTestLensSession session) {
        List<TraceEvent> events = session.events();
        long passed = events.stream().filter(event -> event.status() == TraceStatus.PASSED).count();
        long failed = events.stream().filter(event -> event.status() == TraceStatus.FAILED || event.status() == TraceStatus.ERROR).count();
        long warnings = events.stream().filter(event -> event.status() == TraceStatus.WARNING).count();
        out.append("<section><h2>Summary</h2><div class=\"cards\">");
        card(out, "Events", String.valueOf(events.size()));
        card(out, "Passed", String.valueOf(passed));
        card(out, "Failed/Error", String.valueOf(failed));
        card(out, "Warnings", String.valueOf(warnings));
        card(out, "Artifacts", String.valueOf(session.artifacts().size()));
        out.append("</div></section>");
    }

    private void appendTimeline(StringBuilder out, List<TraceEvent> events, TraceHtmlExportOptions options) {
        out.append("<section><h2>Timeline</h2><div class=\"table-wrap\"><table class=\"timeline\"><thead><tr>")
                .append("<th>Timestamp</th><th>Type</th><th>Status</th><th>Name</th><th>Message</th><th>Duration</th><th>Parent</th>");
        if (options.includeAttributes()) {
            out.append("<th>Attributes</th>");
        }
        out.append("</tr></thead><tbody>");
        for (TraceEvent event : events) {
            if (options.collapsePassedEvents() && event.status() == TraceStatus.PASSED) {
                continue;
            }
            out.append("<tr><td>").append(escape(string(event.timestamp()))).append("</td>")
                    .append("<td>").append(escape(event.type().name())).append("</td>")
                    .append("<td>").append(badge(event.status())).append("</td>")
                    .append("<td>").append(escape(event.name())).append("</td>")
                    .append("<td>").append(escape(preview(event.message(), options.maxMessageLength()))).append("</td>")
                    .append("<td>").append(escape(duration(event.duration()))).append("</td>")
                    .append("<td>").append(escape(event.parentId())).append("</td>");
            if (options.includeAttributes()) {
                out.append("<td>");
                appendMap(out, event.attributes());
                out.append("</td>");
            }
            out.append("</tr>");
        }
        out.append("</tbody></table></div></section>");
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
                    .append("</p><p class=\"muted\">")
                    .append(escape(event.type().name()))
                    .append(" · ")
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

    private void appendFailures(StringBuilder out, List<TraceEvent> events, TraceHtmlExportOptions options) {
        out.append("<section><h2>Failures</h2><div class=\"event-list\">");
        int written = 0;
        for (TraceEvent event : events) {
            if (event.failure() == null && event.status() != TraceStatus.FAILED && event.status() != TraceStatus.ERROR) {
                continue;
            }
            written++;
            out.append("<article class=\"event-card failure\"><div class=\"event-title\"><strong>")
                    .append(escape(event.name()))
                    .append("</strong>")
                    .append(badge(event.status()))
                    .append("</div><p>")
                    .append(escape(preview(event.message(), options.maxMessageLength())))
                    .append("</p>");
            appendFailureBlock(out, event.failure(), options);
            out.append("</article>");
        }
        if (written == 0) {
            out.append("<p class=\"muted\">No failures recorded.</p>");
        }
        out.append("</div></section>");
    }

    private void appendArtifacts(StringBuilder out, List<TraceArtifact> artifacts) {
        out.append("<section><h2>Artifacts</h2><div class=\"table-wrap\"><table><thead><tr>")
                .append("<th>Name</th><th>Type</th><th>Path</th><th>URL</th><th>Media type</th><th>Created</th><th>Metadata</th>")
                .append("</tr></thead><tbody>");
        for (TraceArtifact artifact : artifacts) {
            out.append("<tr><td>").append(escape(artifact.name())).append("</td>")
                    .append("<td>").append(escape(artifact.type().name())).append("</td>")
                    .append("<td>").append(linkOrText(artifact.path())).append("</td>")
                    .append("<td>").append(linkOrText(artifact.url())).append("</td>")
                    .append("<td>").append(escape(artifact.mediaType())).append("</td>")
                    .append("<td>").append(escape(string(artifact.createdAt()))).append("</td><td>");
            appendMap(out, artifact.metadata());
            out.append("</td></tr>");
        }
        if (artifacts.isEmpty()) {
            out.append("<tr><td colspan=\"7\" class=\"muted\">No artifacts attached.</td></tr>");
        }
        out.append("</tbody></table></div></section>");
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
                .append("</p><p><strong>Exception:</strong> ")
                .append(escape(failure.exceptionType()))
                .append("</p>");
        if (!failure.details().isEmpty()) {
            appendMap(out, failure.details());
        }
        if (options.includeStackTraces() && !failure.stackTrace().isBlank()) {
            out.append("<details><summary>Stack trace</summary><pre>")
                    .append(escape(failure.stackTrace()))
                    .append("</pre></details>");
        }
        out.append("</div>");
    }

    private void appendMap(StringBuilder out, Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            out.append("<span class=\"muted\">-</span>");
            return;
        }
        out.append("<dl class=\"kv\">");
        for (Map.Entry<String, String> entry : new TreeMap<>(map).entrySet()) {
            out.append("<dt>").append(escape(entry.getKey())).append("</dt><dd>")
                    .append(escape(entry.getValue()))
                    .append("</dd>");
        }
        out.append("</dl>");
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
        return "<span class=\"badge " + cssClass(effectiveStatus) + "\">" + escape(effectiveStatus.name()) + "</span>";
    }

    private String linkOrText(String value) {
        if (value == null || value.isBlank()) {
            return "<span class=\"muted\">-</span>";
        }
        String escaped = escape(value);
        return "<a href=\"" + escaped + "\">" + escaped + "</a>";
    }

    private boolean isStepEvent(TraceEvent event) {
        return event.type() == TraceEventType.STEP_STARTED
                || event.type() == TraceEventType.STEP_PASSED
                || event.type() == TraceEventType.STEP_FAILED;
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

    private String string(Instant instant) {
        return instant == null ? "" : instant.toString();
    }

    private String preview(String value, int maxLength) {
        String safe = value == null ? "" : value;
        if (safe.length() <= maxLength) {
            return safe;
        }
        return safe.substring(0, maxLength) + "...";
    }

    private String escape(String value) {
        return TraceHtmlEscaper.escape(value);
    }

    private String css() {
        return """
                :root{color-scheme:light;--bg:#f7f8fb;--panel:#fff;--text:#1f2937;--muted:#6b7280;--line:#d8dee9;--blue:#dbeafe;--green:#dcfce7;--red:#fee2e2;--yellow:#fef3c7;--gray:#f3f4f6}
                *{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:14px/1.5 system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif}
                .hero,section{max-width:1180px;margin:24px auto;padding:24px;background:var(--panel);border:1px solid var(--line);border-radius:8px}
                .hero{margin-top:0;border-radius:0 0 8px 8px}.eyebrow{margin:0 0 4px;color:var(--muted);text-transform:uppercase;font-size:12px;letter-spacing:.08em}h1{margin:0 0 18px;font-size:28px}h2{margin:0 0 14px;font-size:18px}
                .metadata-grid,.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:12px}.meta,.card{padding:12px;border:1px solid var(--line);border-radius:8px;background:#fbfcfe}.meta span,.card span{display:block;color:var(--muted);font-size:12px}.meta strong,.card strong{display:block;margin-top:4px;word-break:break-word}
                .badge{display:inline-block;margin-left:8px;padding:2px 8px;border-radius:999px;font-size:12px;font-weight:700}.passed{background:var(--green);color:#166534}.failed{background:var(--red);color:#991b1b}.warning{background:var(--yellow);color:#92400e}.skipped,.info{background:var(--gray);color:#374151}.started{background:var(--blue);color:#1d4ed8}
                .table-wrap{overflow:auto}table{width:100%;border-collapse:collapse}th,td{padding:9px;border-bottom:1px solid var(--line);vertical-align:top;text-align:left}th{background:#f9fafb;color:#374151;font-size:12px;text-transform:uppercase}
                .event-list{display:grid;gap:12px}.event-card{padding:14px;border:1px solid var(--line);border-radius:8px;background:#fbfcfe}.event-card.failure{border-color:#fecaca;background:#fff7f7}.event-title{display:flex;align-items:center;justify-content:space-between;gap:8px}
                .failure-box{margin-top:10px;padding:10px;border-left:4px solid #ef4444;background:#fff}.muted{color:var(--muted)}.kv{margin:0;display:grid;grid-template-columns:max-content 1fr;gap:4px 10px}.kv dt{font-weight:700}.kv dd{margin:0;word-break:break-word}pre{white-space:pre-wrap;overflow:auto;background:#111827;color:#f9fafb;padding:14px;border-radius:8px}a{color:#1d4ed8;word-break:break-word}
                """;
    }
}
