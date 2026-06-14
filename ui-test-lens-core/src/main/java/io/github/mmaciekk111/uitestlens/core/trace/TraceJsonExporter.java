package io.github.mmaciekk111.uitestlens.core.trace;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class TraceJsonExporter {

    public String export(UiTestLensSession session) {
        return export(session, true);
    }

    public String export(UiTestLensSession session, boolean includeStackTraces) {
        if (session == null) {
            return "{}";
        }
        StringBuilder out = new StringBuilder();
        out.append('{');
        appendMetadata(out, session.metadata(), true);
        appendEvents(out, session.events(), includeStackTraces);
        appendArtifacts(out, "artifacts", session.artifacts());
        out.append('}');
        return out.toString();
    }

    private void appendMetadata(StringBuilder out, TraceMetadata metadata, boolean first) {
        comma(out, first);
        fieldName(out, "metadata");
        out.append('{');
        appendField(out, "sessionId", metadata.sessionId(), true);
        appendField(out, "name", metadata.name(), false);
        appendField(out, "startedAt", metadata.startedAt() == null ? "" : metadata.startedAt().toString(), false);
        appendField(out, "finishedAt", metadata.finishedAt() == null ? "" : metadata.finishedAt().toString(), false);
        appendField(out, "status", metadata.status() == null ? "" : metadata.status().name(), false);
        appendField(out, "environment", metadata.environment(), false);
        appendMap(out, "labels", metadata.labels(), false);
        out.append('}');
    }

    private void appendEvents(StringBuilder out, List<TraceEvent> events, boolean includeStackTraces) {
        comma(out, false);
        fieldName(out, "events");
        out.append('[');
        int written = 0;
        for (TraceEvent event : events == null ? List.<TraceEvent>of() : events) {
            if (event == null) {
                continue;
            }
            if (written++ > 0) {
                out.append(',');
            }
            appendEvent(out, event, includeStackTraces);
        }
        out.append(']');
    }

    private void appendEvent(StringBuilder out, TraceEvent event, boolean includeStackTraces) {
        out.append('{');
        appendField(out, "id", event.id(), true);
        appendField(out, "type", event.type() == null ? "" : event.type().name(), false);
        appendField(out, "status", event.status() == null ? "" : event.status().name(), false);
        appendField(out, "name", event.name(), false);
        appendField(out, "message", event.message(), false);
        appendField(out, "timestamp", event.timestamp() == null ? "" : event.timestamp().toString(), false);
        appendField(out, "durationMs", String.valueOf(event.duration() == null ? 0 : event.duration().toMillis()), false);
        appendField(out, "parentId", event.parentId(), false);
        appendFailure(out, "failure", event.failure(), false, includeStackTraces);
        appendArtifacts(out, "artifacts", event.artifacts());
        appendMap(out, "attributes", event.attributes(), false);
        out.append('}');
    }

    private void appendArtifacts(StringBuilder out, String name, List<TraceArtifact> artifacts) {
        comma(out, false);
        fieldName(out, name);
        out.append('[');
        int written = 0;
        for (TraceArtifact artifact : artifacts == null ? List.<TraceArtifact>of() : artifacts) {
            if (artifact == null) {
                continue;
            }
            if (written++ > 0) {
                out.append(',');
            }
            appendArtifact(out, artifact);
        }
        out.append(']');
    }

    private void appendArtifact(StringBuilder out, TraceArtifact artifact) {
        out.append('{');
        appendField(out, "name", artifact.name(), true);
        appendField(out, "type", artifact.type() == null ? "" : artifact.type().name(), false);
        appendField(out, "path", artifact.path(), false);
        appendField(out, "url", artifact.url(), false);
        appendField(out, "mediaType", artifact.mediaType(), false);
        appendField(out, "createdAt", artifact.createdAt() == null ? "" : artifact.createdAt().toString(), false);
        appendMap(out, "metadata", artifact.metadata(), false);
        out.append('}');
    }

    private void appendFailure(StringBuilder out, String name, TraceFailure failure, boolean first, boolean includeStackTraces) {
        comma(out, first);
        fieldName(out, name);
        if (failure == null) {
            out.append("null");
            return;
        }
        out.append('{');
        appendField(out, "message", failure.message(), true);
        appendField(out, "exceptionType", failure.exceptionType(), false);
        appendField(out, "stackTrace", includeStackTraces ? failure.stackTrace() : "", false);
        appendMap(out, "details", failure.details(), false);
        out.append('}');
    }

    private void appendMap(StringBuilder out, String name, Map<String, String> map, boolean first) {
        comma(out, first);
        fieldName(out, name);
        out.append('{');
        int written = 0;
        for (Map.Entry<String, String> entry : new TreeMap<>(map == null ? Map.of() : map).entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            if (written++ > 0) {
                out.append(',');
            }
            string(out, entry.getKey());
            out.append(':');
            string(out, entry.getValue());
        }
        out.append('}');
    }

    private void appendField(StringBuilder out, String name, String value, boolean first) {
        comma(out, first);
        fieldName(out, name);
        string(out, value);
    }

    private void fieldName(StringBuilder out, String name) {
        string(out, name);
        out.append(':');
    }

    private void comma(StringBuilder out, boolean first) {
        if (!first) {
            out.append(',');
        }
    }

    private void string(StringBuilder out, String value) {
        out.append('"').append(escape(value)).append('"');
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
