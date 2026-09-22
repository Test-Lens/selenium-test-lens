package io.github.testlens.core.logging.export;

import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensStatus;

import java.util.Map;

/** Internal report projection of the existing structured event type and status. */
record LogEventSemantics(String category, String phase, String operationId) {
    static LogEventSemantics from(UiTestLensLogEntry entry) {
        UiTestLensEventType type = entry == null || entry.eventType() == null
                ? UiTestLensEventType.GENERAL : entry.eventType();
        Map<String, String> metadata = entry == null ? Map.of() : entry.metadata();
        String category = category(type);
        return new LogEventSemantics(category, phase(type,
                entry == null ? UiTestLensStatus.INFO : entry.status(), metadata, technical(category)),
                metadata.getOrDefault("operationId", ""));
    }

    private static String category(UiTestLensEventType type) {
        String name = type.name();
        if (type == UiTestLensEventType.HIGHLIGHT) return "HIGHLIGHT";
        if (name.startsWith("ACTIONABILITY_")) return "ACTIONABILITY";
        if (name.startsWith("LOCATOR_RESOLVE_")) return "LOCATOR";
        if (name.startsWith("ASSERTION_") || name.startsWith("BUSINESS_ASSERTION_")
                || name.startsWith("NETWORK_ASSERTION_")) return "ASSERTION";
        if (type == UiTestLensEventType.HUD) return "USER";
        if (type == UiTestLensEventType.ACTION || type == UiTestLensEventType.WAIT
                || name.startsWith("LOCATOR_ACTION_") || type == UiTestLensEventType.LOCATOR_RETRY
                || name.startsWith("OVERLAY_ACTION_")) return "ACTION";
        return "SYSTEM";
    }

    private static String phase(UiTestLensEventType type, UiTestLensStatus status,
                                Map<String, String> metadata, boolean technical) {
        UiTestLensStatus effective = status == null ? UiTestLensStatus.INFO : status;
        if (technical) return effective == UiTestLensStatus.FAILED || effective == UiTestLensStatus.WARN
                ? "WARNING" : "DEBUG";
        if (type == UiTestLensEventType.ASSERTION_RETRY) {
            return "poll".equals(metadata.get("retryKind")) ? "RUNNING" : "RETRYING";
        }
        if (type == UiTestLensEventType.LOCATOR_RETRY) {
            return "poll".equals(metadata.get("retryKind")) ? "RUNNING" : "RETRYING";
        }
        return switch (effective) {
            case STARTED -> "RUNNING";
            case PASSED -> "PASSED";
            case FAILED -> "FAILED";
            case WARN -> "WARNING";
            case INFO, SKIPPED -> "INFO";
        };
    }

    private static boolean technical(String category) {
        return "LOCATOR".equals(category) || "ACTIONABILITY".equals(category) || "HIGHLIGHT".equals(category);
    }
}
