package utils.jsExecHelper.core.logging;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record TargetDescriptor(
        String selector,
        String label,
        String tagName,
        String text,
        Map<String, String> metadata
) {
    public TargetDescriptor {
        metadata = immutableCopy(metadata);
    }

    public static TargetDescriptor none() {
        return new TargetDescriptor(null, null, null, null, Map.of());
    }

    public static TargetDescriptor selector(String selector) {
        return new TargetDescriptor(selector, null, null, null, Map.of());
    }

    public static TargetDescriptor label(String label) {
        return new TargetDescriptor(null, label, null, null, Map.of());
    }

    public TargetDescriptor withMetadata(String key, String value) {
        if (key == null || key.isBlank() || value == null) {
            return this;
        }
        Map<String, String> copy = new LinkedHashMap<>(metadata);
        copy.put(key, value);
        return new TargetDescriptor(selector, label, tagName, text, copy);
    }

    private static Map<String, String> immutableCopy(Map<String, String> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }
}
