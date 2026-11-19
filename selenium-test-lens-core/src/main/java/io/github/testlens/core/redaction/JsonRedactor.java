package io.github.testlens.core.redaction;

import java.util.function.Predicate;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

/** Bounded JSON scanner used only at the diagnostic redaction boundary. */
final class JsonRedactor {
    private static final int MAX_DEPTH = 128;

    private JsonRedactor() {}

    static String redact(String input,
                         Predicate<String> sensitiveKey,
                         UnaryOperator<String> redactText,
                         String replacement) {
        return redact(input, sensitiveKey, (path, value) -> redactText.apply(value), replacement);
    }

    static String redact(String input,
                         Predicate<String> sensitiveKey,
                         BiFunction<String, String, String> redactText,
                         String replacement) {
        return redact(input, sensitiveKey, redactText, replacement, (path, kind) -> { });
    }

    static String redact(String input,
                         Predicate<String> sensitiveKey,
                         BiFunction<String, String, String> redactText,
                         String replacement,
                         BiConsumer<String, ChangeKind> changes) {
        int first = firstNonWhitespace(input);
        if (first >= input.length() || !couldBeJson(input.charAt(first))) return null;
        try {
            Parser parser = new Parser(input, sensitiveKey, redactText, replacement, changes);
            StringBuilder output = new StringBuilder(input.length());
            parser.copyWhitespace(output);
            parser.parseValue(output, 0, "$");
            parser.copyWhitespace(output);
            if (!parser.atEnd()) throw new ParseFailure();
            return output.toString();
        } catch (ParseFailure invalidJson) {
            return null;
        }
    }

    static String decodeStringContent(String raw) {
        try {
            Parser parser = new Parser("\"" + raw + "\"", ignored -> false, (path, value) -> value, "",
                    (path, kind) -> { });
            StringToken token = parser.parseString();
            return parser.atEnd() ? token.decoded() : null;
        } catch (ParseFailure invalid) {
            return null;
        }
    }

    static boolean isJsonObject(String input) {
        int first = firstNonWhitespace(input);
        return first < input.length() && input.charAt(first) == '{'
                && redact(input, ignored -> false, value -> value, "") != null;
    }

    static String nonEmptyTopLevelStringMember(String input, String member) {
        int first = firstNonWhitespace(input);
        if (first >= input.length() || input.charAt(first) != '{') return null;
        String[] matched = new String[1];
        String parsed = redact(input, ignored -> false, (path, value) -> {
            if (("$." + member).equals(path) && !value.isEmpty()) matched[0] = value;
            return value;
        }, "");
        return parsed == null ? null : matched[0];
    }

    static String escape(String value) {
        StringBuilder output = new StringBuilder(value.length() + 2);
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '\"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\b' -> output.append("\\b");
                case '\f' -> output.append("\\f");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (current < 0x20) {
                        output.append("\\u");
                        String hex = Integer.toHexString(current);
                        output.append("0".repeat(4 - hex.length())).append(hex);
                    } else {
                        output.append(current);
                    }
                }
            }
        }
        return output.toString();
    }

    private static int firstNonWhitespace(String input) {
        int index = 0;
        while (index < input.length() && isJsonWhitespace(input.charAt(index))) index++;
        return index;
    }

    private static boolean couldBeJson(char value) {
        return value == '{' || value == '[' || value == '\"' || value == '-'
                || value >= '0' && value <= '9' || value == 't' || value == 'f' || value == 'n';
    }

    private static boolean isJsonWhitespace(char value) {
        return value == ' ' || value == '\t' || value == '\r' || value == '\n';
    }

    private record StringToken(int start, int end, String decoded) {}

    enum ChangeKind { SENSITIVE_KEY_VALUE, TEXT }

    private static final class Parser {
        private final String input;
        private final Predicate<String> sensitiveKey;
        private final BiFunction<String, String, String> redactText;
        private final String replacement;
        private final BiConsumer<String, ChangeKind> changes;
        private int index;

        private Parser(String input,
                       Predicate<String> sensitiveKey,
                       BiFunction<String, String, String> redactText,
                       String replacement,
                       BiConsumer<String, ChangeKind> changes) {
            this.input = input;
            this.sensitiveKey = sensitiveKey;
            this.redactText = redactText;
            this.replacement = replacement;
            this.changes = changes;
        }

        private boolean atEnd() {
            return index == input.length();
        }

        private void parseValue(StringBuilder output, int depth, String path) {
            if (depth > MAX_DEPTH || index >= input.length()) throw new ParseFailure();
            switch (input.charAt(index)) {
                case '{' -> parseObject(output, depth + 1, path);
                case '[' -> parseArray(output, depth + 1, path);
                case '\"' -> copyRedactedString(output, path);
                case 't' -> copyLiteral(output, "true");
                case 'f' -> copyLiteral(output, "false");
                case 'n' -> copyLiteral(output, "null");
                default -> copyNumber(output);
            }
        }

        private void parseObject(StringBuilder output, int depth, String path) {
            append(output, index++);
            copyWhitespace(output);
            if (consume('}', output)) return;
            while (true) {
                if (index >= input.length() || input.charAt(index) != '\"') throw new ParseFailure();
                StringToken key = parseString();
                String safeKey = redactText.apply(path + ".<key>", key.decoded());
                appendString(output, key, safeKey, path + ".<key>");
                copyWhitespace(output);
                require(':', output);
                copyWhitespace(output);
                String childPath = path + "." + key.decoded();
                if (sensitiveKey.test(key.decoded())) {
                    changes.accept(childPath, ChangeKind.SENSITIVE_KEY_VALUE);
                    parseValue(null, depth, childPath);
                    appendEncoded(output, replacement);
                } else {
                    parseValue(output, depth, childPath);
                }
                copyWhitespace(output);
                if (consume('}', output)) return;
                require(',', output);
                copyWhitespace(output);
            }
        }

        private void parseArray(StringBuilder output, int depth, String path) {
            append(output, index++);
            copyWhitespace(output);
            if (consume(']', output)) return;
            while (true) {
                parseValue(output, depth, path + "[]");
                copyWhitespace(output);
                if (consume(']', output)) return;
                require(',', output);
                copyWhitespace(output);
            }
        }

        private void copyRedactedString(StringBuilder output, String path) {
            StringToken token = parseString();
            appendString(output, token, redactText.apply(path, token.decoded()), path);
        }

        private StringToken parseString() {
            int start = index;
            if (index >= input.length() || input.charAt(index++) != '\"') throw new ParseFailure();
            StringBuilder decoded = new StringBuilder();
            while (index < input.length()) {
                char current = input.charAt(index++);
                if (current == '\"') return new StringToken(start, index, decoded.toString());
                if (current < 0x20) throw new ParseFailure();
                if (current != '\\') {
                    decoded.append(current);
                    continue;
                }
                if (index >= input.length()) throw new ParseFailure();
                char escaped = input.charAt(index++);
                switch (escaped) {
                    case '\"', '\\', '/' -> decoded.append(escaped);
                    case 'b' -> decoded.append('\b');
                    case 'f' -> decoded.append('\f');
                    case 'n' -> decoded.append('\n');
                    case 'r' -> decoded.append('\r');
                    case 't' -> decoded.append('\t');
                    case 'u' -> decoded.append(parseUnicodeEscape());
                    default -> throw new ParseFailure();
                }
            }
            throw new ParseFailure();
        }

        private char parseUnicodeEscape() {
            if (index + 4 > input.length()) throw new ParseFailure();
            int value = 0;
            for (int i = 0; i < 4; i++) {
                int digit = Character.digit(input.charAt(index++), 16);
                if (digit < 0) throw new ParseFailure();
                value = value * 16 + digit;
            }
            return (char) value;
        }

        private void copyNumber(StringBuilder output) {
            int start = index;
            if (consumeRaw('-') && index >= input.length()) throw new ParseFailure();
            if (consumeRaw('0')) {
                if (index < input.length() && Character.isDigit(input.charAt(index))) throw new ParseFailure();
            } else {
                requireDigits();
            }
            if (consumeRaw('.')) requireDigits();
            if (index < input.length() && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
                index++;
                if (index < input.length() && (input.charAt(index) == '+' || input.charAt(index) == '-')) index++;
                requireDigits();
            }
            if (output != null) output.append(input, start, index);
        }

        private void requireDigits() {
            int start = index;
            while (index < input.length() && Character.isDigit(input.charAt(index))) index++;
            if (start == index) throw new ParseFailure();
        }

        private void copyLiteral(StringBuilder output, String literal) {
            int end = index + literal.length();
            if (end > input.length() || !input.regionMatches(index, literal, 0, literal.length())) {
                throw new ParseFailure();
            }
            if (output != null) output.append(literal);
            index = end;
        }

        private void copyWhitespace(StringBuilder output) {
            int start = index;
            while (index < input.length() && isJsonWhitespace(input.charAt(index))) index++;
            if (output != null) output.append(input, start, index);
        }

        private void require(char expected, StringBuilder output) {
            if (!consume(expected, output)) throw new ParseFailure();
        }

        private boolean consume(char expected, StringBuilder output) {
            if (index >= input.length() || input.charAt(index) != expected) return false;
            append(output, index++);
            return true;
        }

        private boolean consumeRaw(char expected) {
            if (index >= input.length() || input.charAt(index) != expected) return false;
            index++;
            return true;
        }

        private void appendString(StringBuilder output, StringToken token, String safeValue, String path) {
            if (output == null) return;
            if (safeValue.equals(token.decoded())) output.append(input, token.start(), token.end());
            else {
                changes.accept(path, ChangeKind.TEXT);
                appendEncoded(output, safeValue);
            }
        }

        private static void appendEncoded(StringBuilder output, String value) {
            if (output != null) output.append('\"').append(escape(value)).append('\"');
        }

        private void append(StringBuilder output, int position) {
            if (output != null) output.append(input.charAt(position));
        }
    }

    private static final class ParseFailure extends RuntimeException {
        private ParseFailure() {
            super(null, null, false, false);
        }
    }
}
