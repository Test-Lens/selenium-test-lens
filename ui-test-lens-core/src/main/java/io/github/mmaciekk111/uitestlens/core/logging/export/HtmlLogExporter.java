package io.github.mmaciekk111.uitestlens.core.logging.export;

import io.github.mmaciekk111.uitestlens.core.logging.TargetDescriptor;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class HtmlLogExporter implements UiTestLensLogExporter {
    private final LogExportOptions options;

    public HtmlLogExporter() {
        this(LogExportOptions.defaults());
    }

    public HtmlLogExporter(LogExportOptions options) {
        this.options = options != null ? options : LogExportOptions.defaults();
    }

    @Override
    public String export(List<UiTestLensLogEntry> entries) {
        StringBuilder out = new StringBuilder();
        out.append("<!doctype html>\n")
                .append("<html lang=\"en\">\n")
                .append("<head>\n")
                .append("<meta charset=\"UTF-8\">\n")
                .append("<title>UI Test Lens Log</title>\n")
                .append("<style>")
                .append("body{font-family:Arial,sans-serif;margin:24px;color:#1f2933;}")
                .append("table{border-collapse:collapse;width:100%;font-size:13px;}")
                .append("th,td{border:1px solid #d9e2ec;padding:6px 8px;text-align:left;vertical-align:top;}")
                .append("th{background:#f0f4f8;}")
                .append("tr.ERROR,td.ERROR{background:#fff5f5;}")
                .append("code{white-space:pre-wrap;}")
                .append("</style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("<h1>UI Test Lens Log</h1>\n")
                .append("<table>\n")
                .append("<thead><tr>")
                .append("<th>timestamp</th><th>level</th><th>eventType</th><th>status</th>")
                .append("<th>message</th><th>step</th><th>action</th><th>target</th><th>metadata</th>")
                .append("</tr></thead>\n")
                .append("<tbody>\n");

        if (entries != null) {
            for (UiTestLensLogEntry entry : entries) {
                if (entry != null) {
                    appendRow(out, entry);
                }
            }
        }

        out.append("</tbody>\n")
                .append("</table>\n")
                .append("</body>\n")
                .append("</html>\n");
        return out.toString();
    }

    private void appendRow(StringBuilder out, UiTestLensLogEntry entry) {
        out.append("<tr class=\"").append(html(entry.level() == null ? "" : entry.level().name())).append("\">")
                .append("<td>").append(html(entry.timestamp() == null ? "" : entry.timestamp().toString())).append("</td>")
                .append("<td>").append(html(entry.level() == null ? "" : entry.level().name())).append("</td>")
                .append("<td>").append(html(entry.eventType() == null ? "" : entry.eventType().name())).append("</td>")
                .append("<td>").append(html(entry.status() == null ? "" : entry.status().name())).append("</td>")
                .append("<td>").append(html(limit(entry.message()))).append("</td>")
                .append("<td>").append(html(limit(entry.step()))).append("</td>")
                .append("<td>").append(html(limit(entry.action()))).append("</td>")
                .append("<td>").append(html(formatTarget(entry.target()))).append("</td>")
                .append("<td><code>").append(html(options.includeMetadata() ? formatMap(entry.metadata()) : "")).append("</code></td>")
                .append("</tr>\n");
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
        if (input == null || input.isEmpty()) {
            return "";
        }
        Map<String, String> sorted = new TreeMap<>(input);
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (!first) {
                out.append(", ");
            }
            out.append(entry.getKey()).append('=').append(limit(entry.getValue()));
            first = false;
        }
        return out.toString();
    }

    private String limit(String value) {
        if (value == null) {
            return "";
        }
        int max = options.maxFieldLength();
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    static String html(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
