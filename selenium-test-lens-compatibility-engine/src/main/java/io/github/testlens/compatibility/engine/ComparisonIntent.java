package io.github.testlens.compatibility.engine;

import java.util.Objects;

/** Explicit caller intent for an offline compatibility comparison. */
public record ComparisonIntent(Axis axis, String baselineRole, String variantRole) {
    public ComparisonIntent {
        axis = Objects.requireNonNull(axis, "axis");
        baselineRole = bounded(baselineRole, "baselineRole");
        variantRole = bounded(variantRole, "variantRole");
    }

    public enum Axis { HEADLESS_MODE, OBSERVABILITY_MODE, VIEWPORT, BROWSER, DRIVER, DATASET, BUILD, OTHER }

    private static String bounded(String value, String field) {
        if (value == null || value.isBlank() || value.codePointCount(0, value.length()) > 128
                || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(field + " must be a bounded nonblank label");
        }
        return value;
    }
}
