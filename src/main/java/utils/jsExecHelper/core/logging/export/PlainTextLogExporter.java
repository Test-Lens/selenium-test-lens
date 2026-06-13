package utils.jsExecHelper.core.logging.export;

import utils.jsExecHelper.core.logging.TargetDescriptor;
import utils.jsExecHelper.core.logging.UiTestLensLogEntry;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class PlainTextLogExporter implements UiTestLensLogExporter {
    private final LogExportOptions options;

    public PlainTextLogExporter() {
        this(LogExportOptions.defaults());
    }

    public PlainTextLogExporter(LogExportOptions options) {
        this.options = options != null ? options : LogExportOptions.defaults();
    }

    @Override
    public String export(List<UiTestLensLogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (UiTestLensLogEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(System.lineSeparator());
            }
            out.append(entry.timestamp())
                    .append(' ')
                    .append(entry.level())
                    .append(' ')
                    .append(entry.eventType())
                    .append(' ')
                    .append(entry.status())
                    .append(" - ")
                    .append(limit(entry.message()));
            appendIfPresent(out, "step", entry.step());
            appendIfPresent(out, "action", entry.action());
            String target = formatTarget(entry.target());
            if (!target.isBlank()) {
                out.append(" target=").append(target);
            }
            if (options.includeMetadata() && entry.metadata() != null && !entry.metadata().isEmpty()) {
                out.append(" metadata=").append(formatMap(entry.metadata()));
            }
            if (options.includeThrowable() && entry.throwable() != null) {
                out.append(" throwable=")
                        .append(entry.throwable().getClass().getName())
                        .append(":")
                        .append(limit(entry.throwable().getMessage()));
            }
        }
        return out.toString();
    }

    private void appendIfPresent(StringBuilder out, String key, String value) {
        if (value != null && !value.isBlank()) {
            out.append(' ').append(key).append('=').append(limit(value));
        }
    }

    private String formatTarget(TargetDescriptor target) {
        if (target == null) {
            return "";
        }
        if (target.selector() != null) {
            return limit(target.selector());
        }
        if (target.label() != null) {
            return limit(target.label());
        }
        if (target.tagName() != null) {
            return limit(target.tagName());
        }
        if (target.text() != null) {
            return limit(target.text());
        }
        return "";
    }

    private String formatMap(Map<String, String> input) {
        Map<String, String> sorted = new TreeMap<>(input);
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (!first) {
                out.append(", ");
            }
            out.append(entry.getKey()).append('=').append(limit(entry.getValue()));
            first = false;
        }
        return out.append('}').toString();
    }

    private String limit(String value) {
        if (value == null) {
            return "";
        }
        int max = options.maxFieldLength();
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
