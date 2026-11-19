package io.github.testlens.core.redaction;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defines immutable, thread-safe redaction of common credentials and caller-configured secrets in diagnostics.
 * Redaction is applied to diagnostic copies before logger fan-out; it does not mutate the original exception
 * that controls test execution. Diagnostic exception messages, causes, suppressed failures, and stack traces are
 * redacted while structured reports retain the original exception class name. Screenshots and video pixels are
 * outside this text-redaction boundary.
 */
public final class RedactionPolicy {
    private static final String DEFAULT_REPLACEMENT = "[REDACTED]";
    private static final String FAILURE_REPLACEMENT = "[REDACTION_FAILED]";
    private static final Set<String> DEFAULT_KEYS = Set.of(
            "authorization", "proxy-authorization", "cookie", "set-cookie", "x-api-key",
            "api-key", "apikey", "password", "passwd", "pwd", "secret", "client-secret",
            "client_secret", "token", "access-token", "access_token", "refresh-token",
            "refresh_token", "id-token", "id_token", "session", "sessionid", "jsessionid",
            "csrf", "xsrf");
    private static final Pattern CREDENTIAL = Pattern.compile(
            "(?i)\\b(Bearer|Basic)(\\s+)[A-Za-z0-9._~+/-]+=*");
    private static final Pattern JWT = Pattern.compile(
            "(?<![A-Za-z0-9_-])[A-Za-z0-9_-]{8,}+\\.[A-Za-z0-9_-]{8,}+\\.[A-Za-z0-9_-]{8,}+(?![A-Za-z0-9_-])");
    private static final Pattern JWT_COMPACT_CANDIDATE = Pattern.compile(
            "(?<![A-Za-z0-9_-])([A-Za-z0-9_-]+)\\.([A-Za-z0-9_-]+)\\.([A-Za-z0-9_-]*)(?![A-Za-z0-9_-])");
    private static final Pattern HEADER_PAIR = Pattern.compile(
            "(?im)^(\\s*)([A-Za-z0-9_.%_-]+)(\\s*:\\s*)([^\\r\\n]*)$");
    private static final Pattern PLAIN_PAIR = Pattern.compile(
            "(?im)(^|[?&;,\\s{])([A-Za-z0-9_.%_-]+)(\\s*[:=]\\s*)([^&;,}\\r\\n]+)");
    private static final Pattern QUOTED_JSON_KEY = Pattern.compile(
            "\"([A-Za-z0-9_.%_-]+)\"\\s*:");
    private static final Set<String> SAFE_DIAGNOSTIC_PATH_SEGMENTS = Set.of(
            "schemaVersion", "result", "resultType", "payload", "header", "status", "evidence", "limitations",
            "confidence", "scenarioId", "sourcePatch", "selectorAccessPolicy", "pageObjectApisUsed",
            "missingCapabilities", "pageId", "elementId", "requiredCapability", "reason", "capability");

    private final boolean enabled;
    private final String replacement;
    private final Set<String> sensitiveKeys;
    private final List<String> literalSecrets;
    private final int additionalSensitiveKeyCount;

    private RedactionPolicy(Builder builder) {
        this.enabled = builder.enabled;
        this.replacement = builder.replacement;
        LinkedHashSet<String> keys = new LinkedHashSet<>(DEFAULT_KEYS);
        keys.addAll(builder.sensitiveKeys);
        this.sensitiveKeys = Set.copyOf(keys);
        this.literalSecrets = builder.literalSecrets.stream()
                .sorted((left, right) -> Integer.compare(right.length(), left.length()))
                .toList();
        this.additionalSensitiveKeyCount = builder.sensitiveKeys.size();
    }

    public static RedactionPolicy defaults() { return builder().build(); }

    public static RedactionPolicy disabled() { return builder().enabled(false).build(); }

    public static Builder builder() { return new Builder(); }

    public boolean enabled() { return enabled; }

    public String replacement() { return replacement; }

    /** Number of caller-added key names; their values are intentionally not exposed. */
    public int additionalSensitiveKeyCount() { return additionalSensitiveKeyCount; }

    /** Number of caller-added literal secrets; their values are intentionally not exposed. */
    public int literalSecretCount() { return literalSecrets.size(); }

    /**
     * Redacts a bounded structured agent document while treating the selected JSON string fields as generated
     * source code. JSON keys and every other string retain the normal diagnostic policy. Generated source still
     * blocks configured literal secrets, Authorization credentials, JWTs, and credential-bearing URLs, but Java
     * assignments are not interpreted as diagnostic key/value pairs.
     */
    public String redactStructuredAgentOutput(String input, Set<String> generatedSourcePaths) {
        if (input == null || !enabled) return input;
        Set<String> sourcePaths = Set.copyOf(generatedSourcePaths == null ? Set.of() : generatedSourcePaths);
        try {
            String json = JsonRedactor.redact(input, this::isSensitiveKey,
                    (path, value) -> sourcePaths.contains(path)
                            ? redactGeneratedSource(value) : redactPlainText(value), replacement);
            return json == null ? redact(input) : json;
        } catch (RuntimeException failure) {
            return FAILURE_REPLACEMENT;
        }
    }

    /** Inspects a structured agent response and returns only bounded, value-free diagnostics. */
    public List<StructuredRedactionFinding> inspectStructuredAgentOutput(String input,
                                                                         Set<String> generatedSourcePaths) {
        return inspectStructuredAgentOutput(input, generatedSourcePaths, (path, value) -> false);
    }

    /**
     * Inspects a structured agent response while allowing the caller to identify trusted structured values.
     * Trust suppresses only broad JWT-like matches; structurally valid JWTs and every other sensitive-content
     * rule remain blocking.
     */
    public List<StructuredRedactionFinding> inspectStructuredAgentOutput(
            String input,
            Set<String> generatedSourcePaths,
            BiPredicate<String, String> trustedStructuredValue) {
        if (input == null || input.isEmpty() || !enabled) return List.of();
        Set<String> sourcePaths = Set.copyOf(generatedSourcePaths == null ? Set.of() : generatedSourcePaths);
        BiPredicate<String, String> trusted = trustedStructuredValue == null
                ? (path, value) -> false : trustedStructuredValue;
        List<StructuredRedactionFinding> findings = new ArrayList<>();
        try {
            String json = JsonRedactor.redact(input, this::isSensitiveKey, (path, value) -> {
                boolean generatedSource = sourcePaths.contains(path);
                boolean trustedValue = !path.endsWith(".<key>") && trusted.test(path, value);
                List<SensitiveContentFinding> valueFindings = generatedSource
                        ? inspectGeneratedSource(value) : inspectPlainText(value, trustedValue);
                valueFindings.forEach(finding -> findings.add(new StructuredRedactionFinding(
                        finding.rule().name(), finding.category(), path)));
                String safe = generatedSource ? redactGeneratedSource(value) : redactPlainText(value);
                boolean onlyTrustedJwtLikeChanged = trustedValue && !generatedSource
                        && safe.equals(JWT.matcher(value).replaceAll(Matcher.quoteReplacement(replacement)));
                if (valueFindings.isEmpty() && !safe.equals(value) && !onlyTrustedJwtLikeChanged) {
                    findings.add(new StructuredRedactionFinding(
                            generatedSource ? "SOURCE_SENSITIVE_CONTENT" : "UNREDACTED_TEXT",
                            generatedSource ? "GENERATED_SOURCE" : "JSON_STRING", path));
                }
                return value;
            }, replacement, (path, kind) -> {
                if (kind == JsonRedactor.ChangeKind.SENSITIVE_KEY_VALUE) {
                    findings.add(new StructuredRedactionFinding(
                            "SENSITIVE_KEY_VALUE", "JSON_KEY", sensitiveFieldLocation(path)));
                }
            });
            if (json == null) {
                if (!input.equals(redact(input))) {
                    findings.add(new StructuredRedactionFinding(
                            "UNREDACTED_TEXT", "MALFORMED_OR_PLAIN_TEXT", "result"));
                }
            }
        } catch (RuntimeException failure) {
            findings.add(new StructuredRedactionFinding(
                    "REDACTION_FAILURE", "STRUCTURED_DOCUMENT", "result"));
        }
        return findings.stream().distinct()
                .sorted(java.util.Comparator.comparing(StructuredRedactionFinding::rule)
                        .thenComparing(StructuredRedactionFinding::location)
                        .thenComparing(StructuredRedactionFinding::category))
                .toList();
    }

    /** Returns value-free categories for a generated-source rejection. */
    public List<SensitiveContentFinding> inspectGeneratedSource(String source) {
        if (source == null || source.isEmpty() || !enabled) return List.of();
        LinkedHashSet<SensitiveContentRule> rules = new LinkedHashSet<>();
        if (containsCredentialUrl(source)) rules.add(SensitiveContentRule.CREDENTIAL_URL);
        if (CREDENTIAL.matcher(source).find()) rules.add(SensitiveContentRule.AUTHORIZATION_CREDENTIAL);
        if (containsStructuralJwt(source) || containsInJavaLiteralOrComment(source, JWT)) {
            rules.add(SensitiveContentRule.JWT);
        }
        if (literalSecrets.stream().anyMatch(source::contains)) rules.add(SensitiveContentRule.LITERAL_SECRET);
        if (!source.equals(redactGeneratedSourceJsonPairs(source))) {
            rules.add(SensitiveContentRule.SENSITIVE_JSON_KEY_VALUE);
        }
        return rules.stream().sorted().map(rule -> new SensitiveContentFinding(rule, "GENERATED_SOURCE")).toList();
    }

    private static boolean containsInJavaLiteralOrComment(String source, Pattern pattern) {
        boolean[] text = javaLiteralOrCommentRegions(source);
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) if (text[matcher.start()]) return true;
        return false;
    }

    private static boolean containsStructuralJwt(String input) {
        Matcher matcher = JWT_COMPACT_CANDIDATE.matcher(input);
        while (matcher.find()) {
            if (isStructuralJwt(input, matcher)) return true;
        }
        return false;
    }

    private static String redactStructuralJwts(String input, String replacement) {
        Matcher matcher = JWT_COMPACT_CANDIDATE.matcher(input);
        StringBuilder output = null;
        int copied = 0;
        while (matcher.find()) {
            if (!isStructuralJwt(input, matcher)) continue;
            if (output == null) output = new StringBuilder(input.length());
            output.append(input, copied, matcher.start()).append(replacement);
            copied = matcher.end();
        }
        return output == null ? input : output.append(input, copied, input.length()).toString();
    }

    private static boolean isStructuralJwt(String input, Matcher candidate) {
        boolean followsAnotherSegment = candidate.start() > 0 && input.charAt(candidate.start() - 1) == '.';
        boolean hasAnotherSegmentOrPadding = candidate.end() < input.length()
                && (input.charAt(candidate.end()) == '.' || input.charAt(candidate.end()) == '=');
        if (followsAnotherSegment || hasAnotherSegmentOrPadding) {
            return false;
        }
        byte[] headerBytes = decodeCanonicalBase64Url(candidate.group(1));
        byte[] payloadBytes = decodeCanonicalBase64Url(candidate.group(2));
        if (headerBytes == null || payloadBytes == null) return false;
        String header = decodeUtf8(headerBytes);
        String payload = decodeUtf8(payloadBytes);
        if (header == null || payload == null || !JsonRedactor.isJsonObject(payload)) return false;
        String algorithm = JsonRedactor.nonEmptyTopLevelStringMember(header, "alg");
        if (algorithm == null) return false;
        String signature = candidate.group(3);
        if ("none".equals(algorithm)) return signature.isEmpty();
        return !signature.isEmpty() && decodeCanonicalBase64Url(signature) != null;
    }

    private static byte[] decodeCanonicalBase64Url(String segment) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(segment);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(decoded).equals(segment)
                    ? decoded : null;
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    private static String decodeUtf8(byte[] encoded) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(encoded)).toString();
        } catch (CharacterCodingException malformed) {
            return null;
        }
    }

    private static boolean[] javaLiteralOrCommentRegions(String source) {
        boolean[] text = new boolean[source.length()];
        for (int index = 0; index < source.length();) {
            char current = source.charAt(index);
            if (current == '/' && index + 1 < source.length() && source.charAt(index + 1) == '/') {
                int end = source.indexOf('\n', index + 2);
                if (end < 0) end = source.length();
                java.util.Arrays.fill(text, index, end, true);
                index = end;
            } else if (current == '/' && index + 1 < source.length() && source.charAt(index + 1) == '*') {
                int closing = source.indexOf("*/", index + 2);
                int end = closing < 0 ? source.length() : closing + 2;
                java.util.Arrays.fill(text, index, end, true);
                index = end;
            } else if (current == '"') {
                boolean block = index + 2 < source.length() && source.startsWith("\"\"\"", index);
                int end = block ? source.indexOf("\"\"\"", index + 3) : quotedEnd(source, index, '"');
                end = end < 0 ? source.length() : end + (block ? 3 : 1);
                java.util.Arrays.fill(text, index, end, true);
                index = end;
            } else if (current == '\'') {
                int end = quotedEnd(source, index, '\'');
                end = end < 0 ? source.length() : end + 1;
                java.util.Arrays.fill(text, index, end, true);
                index = end;
            } else {
                index++;
            }
        }
        return text;
    }

    private static String redactInJavaLiteralOrComment(String source, Pattern pattern, String replacement) {
        boolean[] text = javaLiteralOrCommentRegions(source);
        Matcher matcher = pattern.matcher(source);
        StringBuilder result = null;
        int copied = 0;
        while (matcher.find()) {
            if (!text[matcher.start()]) continue;
            if (result == null) result = new StringBuilder(source.length());
            result.append(source, copied, matcher.start()).append(replacement);
            copied = matcher.end();
        }
        return result == null ? source : result.append(source, copied, source.length()).toString();
    }

    private static int quotedEnd(String source, int opening, char quote) {
        boolean escaped = false;
        for (int index = opening + 1; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == quote && !escaped) return index;
            if (current == '\\' && !escaped) escaped = true;
            else escaped = false;
        }
        return -1;
    }

    private List<SensitiveContentFinding> inspectPlainText(String text) {
        return inspectPlainText(text, false);
    }

    private List<SensitiveContentFinding> inspectPlainText(String text, boolean trustedJwtLike) {
        if (text == null || text.isEmpty() || !enabled) return List.of();
        LinkedHashSet<SensitiveContentRule> rules = new LinkedHashSet<>();
        if (containsCredentialUrl(text)) rules.add(SensitiveContentRule.CREDENTIAL_URL);
        if (CREDENTIAL.matcher(text).find()) rules.add(SensitiveContentRule.AUTHORIZATION_CREDENTIAL);
        if (containsStructuralJwt(text) || !trustedJwtLike && JWT.matcher(text).find()) {
            rules.add(SensitiveContentRule.JWT);
        }
        if (literalSecrets.stream().anyMatch(text::contains)) rules.add(SensitiveContentRule.LITERAL_SECRET);
        if (!text.equals(replaceSensitivePairs(text))) rules.add(SensitiveContentRule.SENSITIVE_KEY_VALUE);
        return rules.stream().sorted().map(rule -> new SensitiveContentFinding(rule, "JSON_STRING")).toList();
    }

    private static String sensitiveFieldLocation(String path) {
        if (path == null || path.isBlank() || !path.startsWith("$")) return "$.[sensitive]";
        int separator = Math.max(path.lastIndexOf('.'), path.lastIndexOf('['));
        String parent = separator > 0 ? path.substring(0, separator) : "$";
        return parent + ".[sensitive]";
    }

    private static String diagnosticLocation(String raw) {
        if (raw == null || raw.isBlank() || !raw.startsWith("$")) return "result";
        StringBuilder safe = new StringBuilder("$");
        for (String segment : raw.substring(1).split("\\.")) {
            if (segment.isEmpty()) continue;
            int bracket = segment.indexOf('[');
            String key = bracket < 0 ? segment : segment.substring(0, bracket);
            if (!key.isEmpty()) {
                safe.append('.').append(SAFE_DIAGNOSTIC_PATH_SEGMENTS.contains(key) ? key : "[field]");
            }
            if (bracket >= 0) {
                String suffix = segment.substring(bracket);
                if (suffix.equals("[sensitive]") || suffix.equals("[field]")) {
                    safe.append('.').append(suffix);
                } else {
                    safe.append(suffix.matches("(\\[\\d+])*") ? suffix : "[item]");
                }
            }
        }
        if (safe.length() > 160) return safe.substring(0, 147) + ".[truncated]";
        return safe.toString();
    }

    public String redact(String input) {
        if (input == null || !enabled) return input;
        try {
            String json = JsonRedactor.redact(input, this::isSensitiveKey, this::redactPlainText, replacement);
            return json == null ? redactPlainText(input) : json;
        } catch (RuntimeException failure) {
            return FAILURE_REPLACEMENT;
        }
    }

    public String redact(String key, String value) {
        if (value == null || !enabled) return value;
        try {
            if (value.equals(replacement) || value.equals("***")) return value;
            return isSensitiveKey(key) ? replacement : redact(value);
        } catch (RuntimeException failure) {
            return FAILURE_REPLACEMENT;
        }
    }

    public String redactUrl(String url) {
        if (url == null || !enabled) return url;
        try {
            URI uri = new URI(url);
            String query = redactQuery(uri.getRawQuery());
            String authority = uri.getRawAuthority();
            if (authority != null && uri.getRawUserInfo() != null) {
                int at = authority.lastIndexOf('@');
                authority = at >= 0 ? authority.substring(at + 1) : authority;
            }
            StringBuilder safe = new StringBuilder();
            if (uri.getScheme() != null) safe.append(uri.getScheme()).append(':');
            if (authority != null) safe.append("//").append(authority);
            if (uri.getRawPath() != null) safe.append(uri.getRawPath());
            if (query != null) safe.append('?').append(query);
            return redactPlainTextWithoutEmbeddedUrls(safe.toString());
        } catch (URISyntaxException | RuntimeException failure) {
            return "url[length=" + url.length() + "]";
        }
    }

    private String replaceSensitivePairs(String input) {
        String headers = replacePairs(input, HEADER_PAIR);
        String quoted = replaceJsonLikeSensitivePairs(headers);
        return replacePairs(quoted, PLAIN_PAIR);
    }

    private String redactPlainText(String input) {
        return redactPlainTextWithoutEmbeddedUrls(redactEmbeddedAbsoluteUrls(input));
    }

    private String redactPlainTextWithoutEmbeddedUrls(String input) {
        String result = replaceSensitivePairs(input);
        result = CREDENTIAL.matcher(result).replaceAll("$1$2" + Matcher.quoteReplacement(replacement));
        result = redactStructuralJwts(result, replacement);
        result = JWT.matcher(result).replaceAll(Matcher.quoteReplacement(replacement));
        for (String secret : literalSecrets) result = result.replace(secret, replacement);
        return result;
    }

    private String redactGeneratedSource(String input) {
        String result = redactGeneratedSourceJsonPairs(input);
        result = redactCredentialUrls(result);
        result = CREDENTIAL.matcher(result).replaceAll("$1$2" + Matcher.quoteReplacement(replacement));
        result = redactStructuralJwts(result, replacement);
        result = redactInJavaLiteralOrComment(result, JWT, replacement);
        for (String secret : literalSecrets) result = result.replace(secret, replacement);
        return result;
    }

    private String redactGeneratedSourceJsonPairs(String input) {
        String result = replaceJsonLikeSensitivePairs(input);
        String normalizedQuotes = result.replace("\\\"", "\"");
        Matcher matcher = QUOTED_JSON_KEY.matcher(normalizedQuotes);
        while (matcher.find()) {
            if (isSensitiveKey(matcher.group(1))) return replacement + result;
        }
        return result;
    }

    private String redactEmbeddedAbsoluteUrls(String input) {
        StringBuilder output = null;
        int copied = 0;
        int index = 0;
        while (index < input.length()) {
            int start = findAbsoluteUrlStart(input, index);
            if (start < 0) break;
            int end = start;
            while (end < input.length() && !isUrlBoundary(input.charAt(end))) end++;
            if (output == null) output = new StringBuilder(input.length());
            output.append(input, copied, start).append(redactUrl(input.substring(start, end)));
            copied = end;
            index = end;
        }
        if (output == null) return input;
        output.append(input, copied, input.length());
        return output.toString();
    }

    private static int findAbsoluteUrlStart(String input, int from) {
        for (int index = from; index < input.length(); index++) {
            if (input.regionMatches(true, index, "https://", 0, 8)
                    || input.regionMatches(true, index, "http://", 0, 7)) return index;
        }
        return -1;
    }

    private static boolean isUrlBoundary(char value) {
        return Character.isWhitespace(value) || value == '\"' || value == '<' || value == '>';
    }

    private String redactCredentialUrls(String input) {
        StringBuilder output = null;
        int copied = 0;
        int index = 0;
        while (index < input.length()) {
            int start = findAbsoluteUrlStart(input, index);
            if (start < 0) break;
            int end = start;
            while (end < input.length() && !isUrlBoundary(input.charAt(end))) end++;
            String candidate = input.substring(start, end);
            if (isCredentialUrl(candidate)) {
                if (output == null) output = new StringBuilder(input.length());
                output.append(input, copied, start).append(redactUrl(candidate));
                copied = end;
            }
            index = Math.max(end, start + 1);
        }
        if (output == null) return input;
        output.append(input, copied, input.length());
        return output.toString();
    }

    private boolean containsCredentialUrl(String input) {
        int index = 0;
        while (index < input.length()) {
            int start = findAbsoluteUrlStart(input, index);
            if (start < 0) return false;
            int end = start;
            while (end < input.length() && !isUrlBoundary(input.charAt(end))) end++;
            if (isCredentialUrl(input.substring(start, end))) return true;
            index = Math.max(end, start + 1);
        }
        return false;
    }

    private boolean isCredentialUrl(String candidate) {
        try {
            URI uri = new URI(candidate);
            if (uri.getRawUserInfo() != null) return true;
            String query = uri.getRawQuery();
            if (query == null) return false;
            for (String part : query.split("&", -1)) {
                int equals = part.indexOf('=');
                String rawKey = equals < 0 ? part : part.substring(0, equals);
                String key;
                try { key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8); }
                catch (IllegalArgumentException malformed) { key = rawKey; }
                if (isSensitiveKey(key)) return true;
            }
            return false;
        } catch (URISyntaxException malformed) {
            return false;
        }
    }

    private String replaceJsonLikeSensitivePairs(String input) {
        StringBuilder output = new StringBuilder(input.length());
        int copied = 0;
        int index = 0;
        while (index < input.length()) {
            char quote = input.charAt(index);
            if (quote != '\"' && quote != '\'') {
                index++;
                continue;
            }
            int keyEnd = findQuotedEnd(input, index + 1, quote);
            if (keyEnd < 0) break;
            int colon = keyEnd + 1;
            while (colon < input.length() && Character.isWhitespace(input.charAt(colon))) colon++;
            if (colon >= input.length() || input.charAt(colon) != ':') {
                index = keyEnd + 1;
                continue;
            }
            String rawKey = input.substring(index + 1, keyEnd);
            String key = quote == '\"' ? JsonRedactor.decodeStringContent(rawKey) : rawKey;
            if (key == null || !isSensitiveKey(key)) {
                index = keyEnd + 1;
                continue;
            }
            int valueStart = colon + 1;
            while (valueStart < input.length() && Character.isWhitespace(input.charAt(valueStart))) valueStart++;
            if (valueStart >= input.length()) break;
            int valueEnd = findFallbackValueEnd(input, valueStart);
            output.append(input, copied, valueStart);
            output.append('\"').append(JsonRedactor.escape(replacement)).append('\"');
            copied = valueEnd;
            index = valueEnd;
        }
        if (copied == 0) return input;
        output.append(input, copied, input.length());
        return output.toString();
    }

    private static int findQuotedEnd(String input, int index, char quote) {
        boolean escaped = false;
        for (int i = index; i < input.length(); i++) {
            char current = input.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == quote) {
                return i;
            }
        }
        return -1;
    }

    private static int findFallbackValueEnd(String input, int start) {
        char first = input.charAt(start);
        if (first == '\"' || first == '\'') {
            int end = findQuotedEnd(input, start + 1, first);
            return end < 0 ? input.length() : end + 1;
        }
        int objectDepth = 0;
        int arrayDepth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < input.length(); i++) {
            char current = input.charAt(i);
            if (inString) {
                if (escaped) escaped = false;
                else if (current == '\\') escaped = true;
                else if (current == '\"') inString = false;
                continue;
            }
            if (current == '\"') inString = true;
            else if (current == '{') objectDepth++;
            else if (current == '[') arrayDepth++;
            else if (current == '}') {
                if (objectDepth == 0 && arrayDepth == 0) return i;
                objectDepth--;
            } else if (current == ']') {
                if (objectDepth == 0 && arrayDepth == 0) return i;
                arrayDepth--;
            } else if ((current == ',' || current == '&' || current == ';' || current == '\r' || current == '\n')
                    && objectDepth == 0 && arrayDepth == 0) {
                return i;
            }
        }
        return input.length();
    }

    private String replacePairs(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer output = new StringBuffer(input.length());
        while (matcher.find()) {
            String key = matcher.group(2);
            if (!isSensitiveKey(key)) continue;
            String replacementText = matcher.group(1) + key + matcher.group(3) + replacement;
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacementText));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private String redactQuery(String rawQuery) {
        if (rawQuery == null) return null;
        String[] parts = rawQuery.split("&", -1);
        List<String> safe = new ArrayList<>(parts.length);
        for (String part : parts) {
            int equals = part.indexOf('=');
            String rawKey = equals < 0 ? part : part.substring(0, equals);
            String key;
            try { key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8); }
            catch (IllegalArgumentException malformed) { key = rawKey; }
            if (isSensitiveKey(key)) safe.add(rawKey + (equals < 0 ? "" : "=" + replacement));
            else safe.add(redact(part));
        }
        return String.join("&", safe);
    }

    private boolean isSensitiveKey(String key) {
        if (key == null || key.isBlank()) return false;
        String normalized;
        try { normalized = URLDecoder.decode(key, StandardCharsets.UTF_8); }
        catch (IllegalArgumentException malformed) { normalized = key; }
        normalized = normalized.toLowerCase(Locale.ROOT);
        int separator = Math.max(normalized.lastIndexOf('.'), normalized.lastIndexOf('['));
        if (separator >= 0) normalized = normalized.substring(separator + 1).replace("]", "");
        return sensitiveKeys.contains(normalized);
    }

    /**
     * Stable, value-free rule identifiers for generated-source inspection.
     *
     * @since 0.5.1
     */
    public enum SensitiveContentRule {
        AUTHORIZATION_CREDENTIAL,
        CREDENTIAL_URL,
        JWT,
        LITERAL_SECRET,
        SENSITIVE_KEY_VALUE,
        SENSITIVE_JSON_KEY_VALUE
    }

    /**
     * Value-free diagnostic emitted by generated-source inspection.
     *
     * @since 0.5.1
     */
    public record SensitiveContentFinding(SensitiveContentRule rule, String category) {
        public SensitiveContentFinding {
            Objects.requireNonNull(rule, "rule");
            if (category == null || category.isBlank()) throw new IllegalArgumentException("category is required");
        }
    }

    /**
     * Bounded structured diagnostic. It deliberately contains no matched value or excerpt.
     *
     * @since 0.5.1
     */
    public record StructuredRedactionFinding(String rule, String category, String location) {
        public StructuredRedactionFinding {
            if (rule == null || !rule.matches("[A-Z][A-Z0-9_]{0,63}")) {
                throw new IllegalArgumentException("rule is invalid");
            }
            if (category == null || !category.matches("[A-Z][A-Z0-9_]{0,63}")) {
                throw new IllegalArgumentException("category is invalid");
            }
            location = diagnosticLocation(location);
            if (location.isBlank() || location.length() > 160
                    || location.chars().anyMatch(value -> !Character.isLetterOrDigit(value)
                    && "_$.[-]".indexOf(value) < 0)) {
                throw new IllegalArgumentException("location is invalid");
            }
        }
    }

    /** Builder that only accepts literal key names and secrets, never caller-supplied regexes. */
    public static final class Builder {
        private boolean enabled = true;
        private String replacement = DEFAULT_REPLACEMENT;
        private final LinkedHashSet<String> sensitiveKeys = new LinkedHashSet<>();
        private final List<String> literalSecrets = new ArrayList<>();

        private Builder() {}

        public Builder enabled(boolean value) { enabled = value; return this; }

        public Builder replacement(String value) {
            if (value == null || value.isEmpty()) throw new IllegalArgumentException("replacement must not be empty");
            replacement = value;
            return this;
        }

        public Builder sensitiveKey(String key) {
            if (key == null || key.isBlank()) throw new IllegalArgumentException("sensitive key must not be blank");
            sensitiveKeys.add(key.trim().toLowerCase(Locale.ROOT));
            return this;
        }

        public Builder secret(String literalSecret) {
            if (literalSecret != null && !literalSecret.isEmpty() && !literalSecrets.contains(literalSecret)) {
                literalSecrets.add(literalSecret);
            }
            return this;
        }

        public RedactionPolicy build() { return new RedactionPolicy(this); }
    }
}
