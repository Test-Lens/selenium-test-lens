package io.github.testlens.selenium.network;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class NetworkLogExporter {
    public String export(NetworkDiagnostics diagnostics) {
        if (diagnostics == null) {
            return "[]";
        }
        return export(diagnostics.events());
    }

    public String export(List<NetworkEvent> events) {
        StringBuilder out = new StringBuilder();
        out.append('[');
        int written = 0;
        for (NetworkEvent event : events == null ? List.<NetworkEvent>of() : events) {
            if (event == null) continue;
            if (written++ > 0) out.append(',');
            appendEvent(out, event);
        }
        out.append(']');
        return out.toString();
    }

    private void appendEvent(StringBuilder out, NetworkEvent event) {
        out.append('{');
        field(out, "id", event.id(), true);
        field(out, "type", event.type().name(), false);
        field(out, "message", event.message(), false);
        field(out, "timestamp", event.timestamp().toString(), false);
        appendRequest(out, event.request());
        appendResponse(out, event.response());
        appendFailure(out, event.failure());
        map(out, "attributes", event.attributes(), false);
        out.append('}');
    }

    private void appendRequest(StringBuilder out, NetworkRequest request) {
        comma(out, false);
        name(out, "request");
        if (request == null) {
            out.append("null");
            return;
        }
        out.append('{');
        field(out, "id", request.id(), true);
        field(out, "method", request.method(), false);
        field(out, "url", request.url(), false);
        field(out, "resourceType", request.resourceType(), false);
        field(out, "timestamp", request.timestamp().toString(), false);
        map(out, "headers", request.headers(), false);
        out.append('}');
    }

    private void appendResponse(StringBuilder out, NetworkResponse response) {
        comma(out, false);
        name(out, "response");
        if (response == null) {
            out.append("null");
            return;
        }
        out.append('{');
        field(out, "requestId", response.requestId(), true);
        field(out, "url", response.url(), false);
        number(out, "status", response.status(), false);
        field(out, "statusText", response.statusText(), false);
        field(out, "mimeType", response.mimeType(), false);
        number(out, "durationMs", response.duration().toMillis(), false);
        field(out, "timestamp", response.timestamp().toString(), false);
        map(out, "headers", response.headers(), false);
        out.append('}');
    }

    private void appendFailure(StringBuilder out, NetworkFailure failure) {
        comma(out, false);
        name(out, "failure");
        if (failure == null) {
            out.append("null");
            return;
        }
        out.append('{');
        field(out, "requestId", failure.requestId(), true);
        field(out, "url", failure.url(), false);
        field(out, "message", failure.message(), false);
        field(out, "failureType", failure.failureType(), false);
        field(out, "timestamp", failure.timestamp().toString(), false);
        out.append('}');
    }

    private void map(StringBuilder out, String field, Map<String, String> map, boolean first) {
        comma(out, first);
        name(out, field);
        out.append('{');
        int written = 0;
        for (Map.Entry<String, String> entry : new TreeMap<>(map == null ? Map.of() : map).entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            if (written++ > 0) out.append(',');
            string(out, entry.getKey());
            out.append(':');
            string(out, entry.getValue());
        }
        out.append('}');
    }

    private void field(StringBuilder out, String name, String value, boolean first) {
        comma(out, first);
        name(out, name);
        string(out, value);
    }

    private void number(StringBuilder out, String name, long value, boolean first) {
        comma(out, first);
        name(out, name);
        out.append(value);
    }

    private void name(StringBuilder out, String name) {
        string(out, name);
        out.append(':');
    }

    private void comma(StringBuilder out, boolean first) {
        if (!first) out.append(',');
    }

    private void string(StringBuilder out, String value) {
        out.append('"').append(escape(value)).append('"');
    }

    static String escape(String value) {
        if (value == null) return "";
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
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }
}
