package io.github.testlens.core.trace;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Immutable per-session aggregation of physical failures that caused another operation attempt. */
public record RetrySummary(
        long totalRetries,
        Duration timeLost,
        boolean flakyCandidate,
        RetryOutcomePolicy policy,
        boolean policyTriggered,
        Map<String, Long> byAction,
        Map<String, Long> byLocator,
        Map<String, Long> byException) {

    public RetrySummary {
        if (totalRetries < 0) throw new IllegalArgumentException("totalRetries must not be negative");
        timeLost = timeLost == null || timeLost.isNegative() ? Duration.ZERO : timeLost;
        flakyCandidate = totalRetries > 0;
        policy = policy == null ? RetryOutcomePolicy.REPORT_ONLY : policy;
        byAction = sortedCopy(byAction);
        byLocator = sortedCopy(byLocator);
        byException = sortedCopy(byException);
    }

    private static Map<String, Long> sortedCopy(Map<String, Long> source) {
        if (source == null || source.isEmpty()) return Map.of();
        TreeMap<String, Long> sorted = new TreeMap<>();
        source.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null && value > 0) sorted.put(key, value);
        });
        return sorted.isEmpty() ? Map.of() : Collections.unmodifiableMap(sorted);
    }
}
