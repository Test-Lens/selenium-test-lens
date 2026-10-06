package io.github.testlens.application.tooling.ai.workflow;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Bounded, comment/string-aware policy validation for generated Java test patches. @since 0.5.0 */
public final class GeneratedTestPolicyValidator {
    private final Policy policy;

    public GeneratedTestPolicyValidator(Policy policy) { this.policy = policy == null ? Policy.defaults() : policy; }

    public ValidationResult validate(Path relativePath, String source) {
        if (relativePath == null || relativePath.isAbsolute()) throw new IllegalArgumentException("relativePath must be relative");
        if (source == null) throw new IllegalArgumentException("source is required");
        List<Violation> violations = new ArrayList<>();
        String normalizedPath = relativePath.normalize().toString().replace('\\', '/');
        if (normalizedPath.startsWith("../") || normalizedPath.equals("..")) {
            violations.add(new Violation(Rule.FORBIDDEN_PATH, 1, 1, normalizedPath));
        }
        for (String forbidden : policy.forbiddenPathPrefixes()) {
            String prefix = forbidden.replace('\\', '/');
            if (normalizedPath.equals(prefix) || normalizedPath.startsWith(prefix.endsWith("/") ? prefix : prefix + "/")) {
                violations.add(new Violation(Rule.FORBIDDEN_PATH, 1, 1, normalizedPath));
            }
        }
        if (source.length() > policy.maxCharacters()) {
            violations.add(new Violation(Rule.SIZE_LIMIT, 1, 1, "characters=" + source.length()));
            return new ValidationResult(violations);
        }

        List<Token> tokens = tokenize(maskCommentsAndLiterals(source));
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (policy.blockRawBy() && sequence(tokens, i, "By", ".")) add(violations, Rule.RAW_BY, token, source);
            if (policy.blockDriverLookup() && token.text().equals("driver") && i + 2 < tokens.size()
                    && tokens.get(i + 1).text().equals(".")
                    && (tokens.get(i + 2).text().equals("findElement") || tokens.get(i + 2).text().equals("findElements"))) {
                add(violations, Rule.DRIVER_LOOKUP, token, source);
            }
            if (policy.blockJavascript() && (token.text().equals("JavascriptExecutor") || token.text().equals("executeScript"))) {
                add(violations, Rule.JAVASCRIPT, token, source);
            }
            if (policy.blockSleeps() && sequence(tokens, i, "Thread", ".", "sleep")) add(violations, Rule.THREAD_SLEEP, token, source);
            if (policy.blockRetryAnnotations() && token.text().equals("@") && i + 1 < tokens.size()
                    && isRetryAnnotation(tokens.get(i + 1).text())) add(violations, Rule.RETRY_ANNOTATION, token, source);
            if (policy.blockLoops() && isRetryLoop(tokens, i)) {
                add(violations, Rule.RETRY_LOOP, token, source);
            }
        }
        return new ValidationResult(violations);
    }

    public ValidationResult validate(TestEngineeringRequest request, Path relativePath, String source) {
        ValidationResult base = validate(relativePath, source);
        String path = relativePath.normalize().toString().replace('\\', '/');
        boolean allowed = request.allowedPaths().stream().map(value -> value.replace('\\', '/'))
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix.endsWith("/") ? prefix : prefix + "/"));
        if (allowed) return base;
        List<Violation> violations = new ArrayList<>(base.violations());
        violations.add(new Violation(Rule.OUTSIDE_ALLOWED_PATHS, 1, 1, path));
        return new ValidationResult(violations);
    }

    private static boolean isRetryAnnotation(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        return lower.contains("retry") || lower.equals("repeatedtest");
    }

    private static boolean isRetryLoop(List<Token> tokens, int loopIndex) {
        String keyword = tokens.get(loopIndex).text();
        if (!keyword.equals("for") && !keyword.equals("while") && !keyword.equals("do")) return false;
        int limit = Math.min(tokens.size(), loopIndex + 128);
        int braces = 0;
        boolean enteredBody = false;
        for (int i = loopIndex + 1; i < limit; i++) {
            String token = tokens.get(i).text();
            if (token.equals("{")) { braces++; enteredBody = true; }
            else if (token.equals("}") && enteredBody && --braces <= 0) break;
            String lower = token.toLowerCase(Locale.ROOT);
            if (lower.contains("retry") || lower.contains("rerun")) return true;
            if (!enteredBody && token.equals(";")) break;
        }
        return false;
    }

    private static void add(List<Violation> violations, Rule rule, Token token, String source) {
        int start = Math.max(0, token.offset() - 24);
        int end = Math.min(source.length(), token.offset() + token.text().length() + 40);
        String evidence = source.substring(start, end).replace('\n', ' ').replace('\r', ' ').strip();
        violations.add(new Violation(rule, token.line(), token.column(), evidence));
    }

    private static boolean sequence(List<Token> tokens, int offset, String... expected) {
        if (offset < 0 || offset + expected.length > tokens.size()) return false;
        for (int i = 0; i < expected.length; i++) if (!tokens.get(offset + i).text().equals(expected[i])) return false;
        return true;
    }

    private static char[] maskCommentsAndLiterals(String source) {
        char[] result = source.toCharArray();
        Mode mode = Mode.CODE;
        boolean escaped = false;
        for (int i = 0; i < result.length; i++) {
            char c = result[i];
            char next = i + 1 < result.length ? result[i + 1] : '\0';
            if (mode == Mode.CODE && c == '/' && next == '/') { result[i] = result[++i] = ' '; mode = Mode.LINE_COMMENT; continue; }
            if (mode == Mode.CODE && c == '/' && next == '*') { result[i] = result[++i] = ' '; mode = Mode.BLOCK_COMMENT; continue; }
            if (mode == Mode.CODE && c == '"' && next == '"' && i + 2 < result.length && result[i + 2] == '"') {
                result[i] = result[++i] = result[++i] = ' '; mode = Mode.TEXT_BLOCK; continue;
            }
            if (mode == Mode.CODE && c == '"') { result[i] = ' '; mode = Mode.STRING; escaped = false; continue; }
            if (mode == Mode.CODE && c == '\'') { result[i] = ' '; mode = Mode.CHARACTER; escaped = false; continue; }
            if (mode == Mode.LINE_COMMENT) { if (c == '\n') mode = Mode.CODE; else result[i] = ' '; continue; }
            if (mode == Mode.BLOCK_COMMENT) {
                if (c == '*' && next == '/') { result[i] = result[++i] = ' '; mode = Mode.CODE; }
                else if (c != '\n' && c != '\r') result[i] = ' ';
                continue;
            }
            if (mode == Mode.TEXT_BLOCK) {
                if (c == '"' && next == '"' && i + 2 < result.length && result[i + 2] == '"') {
                    result[i] = result[++i] = result[++i] = ' '; mode = Mode.CODE;
                } else if (c != '\n' && c != '\r') result[i] = ' ';
                continue;
            }
            if (mode == Mode.STRING || mode == Mode.CHARACTER) {
                char closing = mode == Mode.STRING ? '"' : '\'';
                if (c == closing && !escaped) mode = Mode.CODE;
                if (c != '\n' && c != '\r') result[i] = ' ';
                if (c == '\\' && !escaped) escaped = true; else escaped = false;
            }
        }
        return result;
    }

    private static List<Token> tokenize(char[] source) {
        List<Token> tokens = new ArrayList<>();
        int line = 1, column = 1;
        for (int i = 0; i < source.length;) {
            char c = source[i];
            if (c == '\n') { line++; column = 1; i++; continue; }
            if (Character.isWhitespace(c)) { column++; i++; continue; }
            int offset = i, startColumn = column;
            if (Character.isJavaIdentifierStart(c)) {
                i++; column++;
                while (i < source.length && Character.isJavaIdentifierPart(source[i])) { i++; column++; }
            } else { i++; column++; }
            tokens.add(new Token(new String(source, offset, i - offset), offset, line, startColumn));
        }
        return tokens;
    }

    public record Policy(List<String> forbiddenPathPrefixes, int maxCharacters, boolean blockRawBy,
                         boolean blockDriverLookup, boolean blockSleeps, boolean blockJavascript,
                         boolean blockRetryAnnotations, boolean blockLoops) {
        public Policy {
            forbiddenPathPrefixes = List.copyOf(forbiddenPathPrefixes == null ? List.of() : forbiddenPathPrefixes);
            if (maxCharacters < 1 || maxCharacters > 4_000_000) throw new IllegalArgumentException("invalid maxCharacters");
        }
        public static Policy defaults() {
            return new Policy(List.of("src/main", "pom.xml", ".github"), 500_000, true, true, true, true, true, true);
        }
    }

    public record ValidationResult(List<Violation> violations) {
        public ValidationResult { violations = List.copyOf(violations); }
        public boolean accepted() { return violations.isEmpty(); }
    }
    public record Violation(Rule rule, int line, int column, String evidence) { }
    public enum Rule { RAW_BY, DRIVER_LOOKUP, THREAD_SLEEP, JAVASCRIPT, RETRY_ANNOTATION, RETRY_LOOP,
        FORBIDDEN_PATH, OUTSIDE_ALLOWED_PATHS, SIZE_LIMIT }
    private record Token(String text, int offset, int line, int column) { }
    private enum Mode { CODE, LINE_COMMENT, BLOCK_COMMENT, STRING, CHARACTER, TEXT_BLOCK }
}
