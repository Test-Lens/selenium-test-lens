package io.github.mmaciekk111.uitestlens.selenium.auth;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AuthStateJsonParser {
    public AuthState parse(String json) {
        Object root = new Parser(json).parseValue();
        if (!(root instanceof Map<?, ?> map)) {
            throw new AuthStateException("Auth state JSON must be an object");
        }
        Map<String, Object> object = stringObject(map);
        AuthStateMetadata metadata = parseMetadata(asObject(object.get("metadata")));
        return new AuthState(
                metadata,
                parseCookies(asList(object.get("cookies"))),
                parseStorage(asList(object.get("localStorage")), AuthStorageType.LOCAL_STORAGE),
                parseStorage(asList(object.get("sessionStorage")), AuthStorageType.SESSION_STORAGE)
        );
    }

    private AuthStateMetadata parseMetadata(Map<String, Object> map) {
        AuthStateMetadata.Builder builder = AuthStateMetadata.builder()
                .id(string(map.get("id")))
                .label(string(map.get("label")))
                .role(string(map.get("role")))
                .origin(string(map.get("origin")))
                .domain(string(map.get("domain")))
                .createdBy(string(map.get("createdBy")))
                .createdAt(instant(string(map.get("createdAt"))))
                .expiresAt(instant(string(map.get("expiresAt"))));
        asStringMap(map.get("labels")).forEach(builder::labelEntry);
        asStringMap(map.get("notes")).forEach(builder::note);
        return builder.build();
    }

    private List<AuthCookie> parseCookies(List<Object> list) {
        List<AuthCookie> cookies = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> map = asObject(item);
            cookies.add(new AuthCookie(
                    string(map.get("name")),
                    string(map.get("value")),
                    string(map.get("domain")),
                    string(map.get("path")),
                    instant(string(map.get("expiry"))),
                    bool(map.get("secure")),
                    bool(map.get("httpOnly")),
                    string(map.get("sameSite"))
            ));
        }
        return cookies;
    }

    private List<AuthStorageEntry> parseStorage(List<Object> list, AuthStorageType fallbackType) {
        List<AuthStorageEntry> entries = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> map = asObject(item);
            AuthStorageType type = fallbackType;
            String rawType = string(map.get("type"));
            if (!rawType.isBlank()) {
                type = AuthStorageType.valueOf(rawType);
            }
            entries.add(new AuthStorageEntry(
                    string(map.get("origin")),
                    string(map.get("key")),
                    string(map.get("value")),
                    type
            ));
        }
        return entries;
    }

    private static Instant instant(String value) {
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean bool(Object value) {
        return value instanceof Boolean b && b;
    }

    private static Map<String, Object> asObject(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        return stringObject(map);
    }

    private static List<Object> asList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return new ArrayList<>(list);
    }

    private static Map<String, String> asStringMap(Object value) {
        Map<String, String> result = new LinkedHashMap<>();
        asObject(value).forEach((key, mapValue) -> result.put(key, string(mapValue)));
        return result;
    }

    private static Map<String, Object> stringObject(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private static final class Parser {
        private final String input;
        private int index;

        private Parser(String input) {
            this.input = input == null ? "" : input;
        }

        Object parseValue() {
            skipWhitespace();
            if (index >= input.length()) {
                throw error("Unexpected end of JSON");
            }
            char c = input.charAt(index);
            if (c == '{') {
                return parseObject();
            }
            if (c == '[') {
                return parseArray();
            }
            if (c == '"') {
                return parseString();
            }
            if (startsWith("true")) {
                index += 4;
                return Boolean.TRUE;
            }
            if (startsWith("false")) {
                index += 5;
                return Boolean.FALSE;
            }
            if (startsWith("null")) {
                index += 4;
                return null;
            }
            throw error("Unsupported JSON value");
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return object;
            }
            while (true) {
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                object.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return list;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (index < input.length()) {
                char c = input.charAt(index++);
                if (c == '"') {
                    return out.toString();
                }
                if (c == '\\') {
                    if (index >= input.length()) {
                        throw error("Invalid escape");
                    }
                    char escaped = input.charAt(index++);
                    switch (escaped) {
                        case '"' -> out.append('"');
                        case '\\' -> out.append('\\');
                        case '/' -> out.append('/');
                        case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case 'u' -> {
                            if (index + 4 > input.length()) {
                                throw error("Invalid unicode escape");
                            }
                            String hex = input.substring(index, index + 4);
                            out.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                        }
                        default -> throw error("Unsupported escape");
                    }
                } else {
                    out.append(c);
                }
            }
            throw error("Unterminated string");
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private boolean startsWith(String value) {
            return input.startsWith(value, index);
        }

        private boolean peek(char expected) {
            skipWhitespace();
            return index < input.length() && input.charAt(index) == expected;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= input.length() || input.charAt(index) != expected) {
                throw error("Expected '" + expected + "'");
            }
            index++;
        }

        private AuthStateException error(String message) {
            return new AuthStateException(message + " at position " + index);
        }
    }
}
