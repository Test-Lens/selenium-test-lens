package io.github.testlens.core.trace.export;

import java.util.List;
import java.util.Map;

/**
 * Small JSON writer used by static report exporters.
 */
public final class TraceJsonWriter {
    private TraceJsonWriter() {
    }

    public static String write(Object value) {
        StringBuilder out = new StringBuilder(16_384);
        append(out, value);
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private static void append(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String text) {
            string(out, text);
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            int written = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                if (written++ > 0) {
                    out.append(',');
                }
                string(out, String.valueOf(entry.getKey()));
                out.append(':');
                append(out, entry.getValue());
            }
            out.append('}');
        } else if (value instanceof List<?> list) {
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                append(out, list.get(i));
            }
            out.append(']');
        } else if (value instanceof Iterable<?> iterable) {
            out.append('[');
            int written = 0;
            for (Object item : iterable) {
                if (written++ > 0) {
                    out.append(',');
                }
                append(out, item);
            }
            out.append(']');
        } else {
            string(out, String.valueOf(value));
        }
    }

    private static void string(StringBuilder out, String value) {
        out.append('"').append(escape(value)).append('"');
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int pad = hex.length(); pad < 4; pad++) {
                            out.append('0');
                        }
                        out.append(hex);
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
