package io.github.testlens.selector.engine;

import java.util.Locale;
import java.util.Objects;

/** Internal tooling model; not part of the Test Lens runtime API. */
public record SelectorSubject(SubjectKind kind, String strategy, ValueState valueState, String canonicalValue,
                              String declarationRef, String modulePath, String logicalPath, String declaringSymbol,
                              String usageClass, String usageMethod, String contextFingerprint,
                              InputTrust inputTrust, String templateInformation) {
    public static final int MAX_VALUE_CODE_POINTS = 16_384;

    public SelectorSubject {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(valueState, "valueState");
        Objects.requireNonNull(inputTrust, "inputTrust");
        strategy = normalizeStrategy(strategy);
        if (valueState == ValueState.KNOWN) {
            Objects.requireNonNull(canonicalValue, "canonicalValue");
            if (canonicalValue.codePointCount(0, canonicalValue.length()) > MAX_VALUE_CODE_POINTS) {
                throw new IllegalArgumentException("canonicalValue exceeds " + MAX_VALUE_CODE_POINTS + " code points");
            }
        } else if (canonicalValue != null) {
            throw new IllegalArgumentException("canonicalValue must be absent when valueState is UNAVAILABLE");
        }
        modulePath = normalizePath(modulePath);
        logicalPath = normalizePath(logicalPath);
    }

    public static SelectorSubject trusted(String strategy, String value) {
        return new SelectorSubject(SubjectKind.STATIC_DECLARATION, strategy, ValueState.KNOWN, value,
                null, null, null, null, null, null, null, InputTrust.SOURCE_CANONICAL, null);
    }

    public boolean hasTrustedExactValue() {
        return valueState == ValueState.KNOWN && inputTrust != InputTrust.RUNTIME_REDACTED;
    }

    public static String normalizeStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) return "";
        return strategy.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String normalizePath(String value) { return value == null ? null : value.replace('\\', '/'); }

    public enum SubjectKind { STATIC_DECLARATION, RUNTIME_OBSERVATION, TEMPLATE, CUSTOM }
    public enum ValueState { KNOWN, UNAVAILABLE }
    public enum InputTrust { SOURCE_CANONICAL, RUNTIME_RAW_LOCAL, RUNTIME_REDACTED }
}
