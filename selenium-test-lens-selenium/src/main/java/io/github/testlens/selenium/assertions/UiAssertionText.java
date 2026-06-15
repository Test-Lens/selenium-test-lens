package io.github.testlens.selenium.assertions;

final class UiAssertionText {
    private UiAssertionText() {
    }

    static String normalize(String value, UiAssertionOptions options) {
        String normalized = value == null ? "" : value;
        UiAssertionOptions effectiveOptions = options != null ? options : UiAssertionOptions.defaults();
        if (effectiveOptions.normalizeWhitespace()) {
            normalized = normalized.replaceAll("\\s+", " ");
        }
        if (effectiveOptions.trimText()) {
            normalized = normalized.trim();
        }
        if (!effectiveOptions.caseSensitive()) {
            normalized = normalized.toLowerCase();
        }
        return normalized;
    }

    static String preview(String value, int limit) {
        String safe = value == null ? "" : value;
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative");
        }
        if (safe.length() <= limit) {
            return safe;
        }
        return safe.substring(0, limit) + "...";
    }

    static String valuePreview(String value) {
        return "length=" + (value == null ? 0 : value.length());
    }
}
