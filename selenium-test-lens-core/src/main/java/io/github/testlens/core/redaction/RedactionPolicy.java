package io.github.testlens.core.redaction;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Immutable policy for removing common credentials from Test Lens diagnostics. */
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
    private static final Pattern QUOTED_PAIR = Pattern.compile(
            "([\\\"'])([A-Za-z0-9_.%_-]+)\\1(\\s*:\\s*)([\\\"'])([^\\\"'\\r\\n]*)\\4");
    private static final Pattern HEADER_PAIR = Pattern.compile(
            "(?im)^(\\s*)([A-Za-z0-9_.%_-]+)(\\s*:\\s*)([^\\r\\n]*)$");
    private static final Pattern PLAIN_PAIR = Pattern.compile(
            "(?im)(^|[?&;,\\s{])([A-Za-z0-9_.%_-]+)(\\s*[:=]\\s*)([^&;,}\\r\\n]+)");

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

    public String redact(String input) {
        if (input == null || !enabled) return input;
        try {
            String result = replaceSensitivePairs(input);
            result = CREDENTIAL.matcher(result).replaceAll("$1$2" + Matcher.quoteReplacement(replacement));
            result = JWT.matcher(result).replaceAll(Matcher.quoteReplacement(replacement));
            for (String secret : literalSecrets) result = result.replace(secret, replacement);
            return result;
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
            return redact(safe.toString());
        } catch (URISyntaxException | RuntimeException failure) {
            return "url[length=" + url.length() + "]";
        }
    }

    private String replaceSensitivePairs(String input) {
        String headers = replacePairs(input, HEADER_PAIR, false);
        String quoted = replacePairs(headers, QUOTED_PAIR, true);
        return replacePairs(quoted, PLAIN_PAIR, false);
    }

    private String replacePairs(String input, Pattern pattern, boolean quoted) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer output = new StringBuffer(input.length());
        while (matcher.find()) {
            String key = matcher.group(quoted ? 2 : 2);
            if (!isSensitiveKey(key)) continue;
            String replacementText;
            if (quoted) {
                replacementText = matcher.group(1) + key + matcher.group(1) + matcher.group(3)
                        + matcher.group(4) + replacement + matcher.group(4);
            } else {
                replacementText = matcher.group(1) + key + matcher.group(3) + replacement;
            }
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
