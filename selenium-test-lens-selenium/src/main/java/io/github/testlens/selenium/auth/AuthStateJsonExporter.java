package io.github.testlens.selenium.auth;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class AuthStateJsonExporter {
    public String export(AuthState state) {
        if (state == null) {
            return "{}";
        }
        StringBuilder out = new StringBuilder();
        out.append('{');
        appendMetadata(out, state.metadata(), true);
        appendCookies(out, state.cookies());
        appendStorage(out, "localStorage", state.localStorage());
        appendStorage(out, "sessionStorage", state.sessionStorage());
        out.append('}');
        return out.toString();
    }

    private void appendMetadata(StringBuilder out, AuthStateMetadata metadata, boolean first) {
        comma(out, first);
        fieldName(out, "metadata");
        out.append('{');
        appendField(out, "id", metadata.id(), true);
        appendField(out, "label", metadata.label(), false);
        appendField(out, "role", metadata.role(), false);
        appendField(out, "origin", metadata.origin(), false);
        appendField(out, "domain", metadata.domain(), false);
        appendField(out, "createdAt", metadata.createdAt() == null ? "" : metadata.createdAt().toString(), false);
        appendField(out, "expiresAt", metadata.expiresAt() == null ? "" : metadata.expiresAt().toString(), false);
        appendField(out, "createdBy", metadata.createdBy(), false);
        appendMap(out, "labels", metadata.labels(), false);
        appendMap(out, "notes", metadata.notes(), false);
        out.append('}');
    }

    private void appendCookies(StringBuilder out, List<AuthCookie> cookies) {
        comma(out, false);
        fieldName(out, "cookies");
        out.append('[');
        int written = 0;
        for (AuthCookie cookie : cookies == null ? List.<AuthCookie>of() : cookies) {
            if (cookie == null) {
                continue;
            }
            if (written++ > 0) {
                out.append(',');
            }
            out.append('{');
            appendField(out, "name", cookie.name(), true);
            appendField(out, "value", cookie.value(), false);
            appendField(out, "domain", cookie.domain(), false);
            appendField(out, "path", cookie.path(), false);
            appendField(out, "expiry", cookie.expiry() == null ? "" : cookie.expiry().toString(), false);
            appendBoolean(out, "secure", cookie.secure(), false);
            appendBoolean(out, "httpOnly", cookie.httpOnly(), false);
            appendField(out, "sameSite", cookie.sameSite(), false);
            out.append('}');
        }
        out.append(']');
    }

    private void appendStorage(StringBuilder out, String name, List<AuthStorageEntry> entries) {
        comma(out, false);
        fieldName(out, name);
        out.append('[');
        int written = 0;
        for (AuthStorageEntry entry : entries == null ? List.<AuthStorageEntry>of() : entries) {
            if (entry == null) {
                continue;
            }
            if (written++ > 0) {
                out.append(',');
            }
            out.append('{');
            appendField(out, "origin", entry.origin(), true);
            appendField(out, "key", entry.key(), false);
            appendField(out, "value", entry.value(), false);
            appendField(out, "type", entry.type().name(), false);
            out.append('}');
        }
        out.append(']');
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

    private void appendBoolean(StringBuilder out, String name, boolean value, boolean first) {
        comma(out, first);
        fieldName(out, name);
        out.append(value);
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

