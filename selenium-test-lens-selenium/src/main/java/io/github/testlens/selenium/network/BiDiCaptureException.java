package io.github.testlens.selenium.network;

import java.util.LinkedHashMap;
import java.util.Map;

/** Internal, redaction-safe BiDi failure carrying structured startup diagnostics. */
class BiDiCaptureException extends RuntimeException {
    private final BiDiFailureCategory category;
    private final Map<String, String> diagnostics;

    BiDiCaptureException(BiDiFailureCategory category, String message,
                         Map<String, String> diagnostics, Throwable cause) {
        super(message, cause);
        this.category = category;
        Map<String, String> values = diagnostics == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(diagnostics);
        values.putIfAbsent("failureCategory", category.name());
        this.diagnostics = Map.copyOf(values);
    }

    BiDiFailureCategory category() {
        return category;
    }

    Map<String, String> diagnostics() {
        return diagnostics;
    }
}
