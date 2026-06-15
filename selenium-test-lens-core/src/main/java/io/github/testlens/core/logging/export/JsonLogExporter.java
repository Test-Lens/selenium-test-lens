package io.github.testlens.core.logging.export;

import io.github.testlens.core.logging.TargetDescriptor;
import io.github.testlens.core.logging.UiTestLensLogEntry;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class JsonLogExporter implements UiTestLensLogExporter {
    private final LogExportOptions options;

    public JsonLogExporter() {
        this(LogExportOptions.defaults());
    }

    public JsonLogExporter(LogExportOptions options) {
        this.options = options != null ? options : LogExportOptions.defaults();
    }

    @Override
    public String export(List<UiTestLensLogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "[]";
        }
        StringBuilder out = new StringBuilder();
        boolean pretty = options.prettyPrint();
        out.append('[');
        int written = 0;
        for (UiTestLensLogEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            if (written > 0) {
                out.append(',');
            }
            newline(out, pretty);
            indent(out, pretty, 1);
            appendEntry(out, entry, pretty, 1);
            written++;
        }
        if (written > 0) {
            newline(out, pretty);
        }
        out.append(']');
        return out.toString();
    }

    private void appendEntry(StringBuilder out, UiTestLensLogEntry entry, boolean pretty, int depth) {
        out.append('{');
        newline(out, pretty);
        appendField(out, "timestamp", entry.timestamp() == null ? null : entry.timestamp().toString(), true, pretty, depth + 1);
        appendField(out, "level", entry.level() == null ? null : entry.level().name(), false, pretty, depth + 1);
        appendField(out, "eventType", entry.eventType() == null ? null : entry.eventType().name(), false, pretty, depth + 1);
        appendField(out, "status", entry.status() == null ? null : entry.status().name(), false, pretty, depth + 1);
        appendField(out, "message", limit(entry.message()), false, pretty, depth + 1);
        appendField(out, "step", limit(entry.step()), false, pretty, depth + 1);
        appendField(out, "action", limit(entry.action()), false, pretty, depth + 1);
        appendTargetField(out, entry.target(), false, pretty, depth + 1);
        appendMetadataField(out, "metadata", options.includeMetadata() ? entry.metadata() : Map.of(), false, pretty, depth + 1);
        appendThrowableField(out, entry.throwable(), false, pretty, depth + 1);
        newline(out, pretty);
        indent(out, pretty, depth);
        out.append('}');
    }

    private void appendField(StringBuilder out,
                             String name,
                             String value,
                             boolean first,
                             boolean pretty,
                             int depth) {
        if (!first) {
            out.append(',');
            newline(out, pretty);
        }
        indent(out, pretty, depth);
        out.append('"').append(escape(name)).append('"').append(':');
        space(out, pretty);
        appendStringOrNull(out, value);
    }

    private void appendTargetField(StringBuilder out, TargetDescriptor target, boolean first, boolean pretty, int depth) {
        if (!first) {
            out.append(',');
            newline(out, pretty);
        }
        indent(out, pretty, depth);
        out.append("\"target\":");
        space(out, pretty);
        if (target == null) {
            out.append("null");
            return;
        }
        out.append('{');
        newline(out, pretty);
        appendField(out, "selector", limit(target.selector()), true, pretty, depth + 1);
        appendField(out, "label", limit(target.label()), false, pretty, depth + 1);
        appendField(out, "tagName", limit(target.tagName()), false, pretty, depth + 1);
        appendField(out, "text", limit(target.text()), false, pretty, depth + 1);
        appendMetadataField(out, "metadata", options.includeMetadata() ? target.metadata() : Map.of(), false, pretty, depth + 1);
        newline(out, pretty);
        indent(out, pretty, depth);
        out.append('}');
    }

    private void appendMetadataField(StringBuilder out,
                                     String name,
                                     Map<String, String> metadata,
                                     boolean first,
                                     boolean pretty,
                                     int depth) {
        if (!first) {
            out.append(',');
            newline(out, pretty);
        }
        indent(out, pretty, depth);
        out.append('"').append(escape(name)).append('"').append(':');
        space(out, pretty);
        out.append('{');
        Map<String, String> sorted = metadata == null ? Map.of() : new TreeMap<>(metadata);
        int written = 0;
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            if (written > 0) {
                out.append(',');
            }
            newline(out, pretty);
            indent(out, pretty, depth + 1);
            out.append('"').append(escape(entry.getKey())).append('"').append(':');
            space(out, pretty);
            appendStringOrNull(out, limit(entry.getValue()));
            written++;
        }
        if (written > 0) {
            newline(out, pretty);
            indent(out, pretty, depth);
        }
        out.append('}');
    }

    private void appendThrowableField(StringBuilder out, Throwable throwable, boolean first, boolean pretty, int depth) {
        if (!first) {
            out.append(',');
            newline(out, pretty);
        }
        indent(out, pretty, depth);
        out.append("\"throwable\":");
        space(out, pretty);
        if (!options.includeThrowable() || throwable == null) {
            out.append("null");
            return;
        }
        out.append('{');
        newline(out, pretty);
        appendField(out, "type", throwable.getClass().getName(), true, pretty, depth + 1);
        appendField(out, "message", limit(throwable.getMessage()), false, pretty, depth + 1);
        newline(out, pretty);
        indent(out, pretty, depth);
        out.append('}');
    }

    private void appendStringOrNull(StringBuilder out, String value) {
        if (value == null) {
            out.append("null");
        } else {
            out.append('"').append(escape(value)).append('"');
        }
    }

    private String limit(String value) {
        if (value == null) {
            return null;
        }
        int max = options.maxFieldLength();
        return value.length() <= max ? value : value.substring(0, max) + "...";
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

    private void newline(StringBuilder out, boolean pretty) {
        if (pretty) {
            out.append('\n');
        }
    }

    private void indent(StringBuilder out, boolean pretty, int depth) {
        if (pretty) {
            out.append("  ".repeat(Math.max(0, depth)));
        }
    }

    private void space(StringBuilder out, boolean pretty) {
        if (pretty) {
            out.append(' ');
        }
    }
}
