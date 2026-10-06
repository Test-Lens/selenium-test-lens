package io.github.testlens.application.tooling.source;

import java.util.Map;
import java.util.TreeMap;

/** Declarative stable-ID correlations supplied by a trusted user or project configuration. @since 0.5.0 */
public record CorrelationOverrides(Map<String,String> classToPage, Map<String,String> sourceElementToApplicationElement) {
    public CorrelationOverrides {
        classToPage = Map.copyOf(new TreeMap<>(classToPage == null ? Map.of() : classToPage));
        sourceElementToApplicationElement = Map.copyOf(new TreeMap<>(sourceElementToApplicationElement == null ? Map.of() : sourceElementToApplicationElement));
    }
    public static CorrelationOverrides none() { return new CorrelationOverrides(Map.of(), Map.of()); }
}
