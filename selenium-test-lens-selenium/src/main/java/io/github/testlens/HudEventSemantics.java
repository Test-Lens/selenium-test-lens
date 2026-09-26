package io.github.testlens;

import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/** Internal semantic projection used by the browser HUD. */
record HudEventSemantics(String category,
                         String phase,
                         String operationId,
                         int attempt,
                         long durationMs,
                         boolean technical,
                         String customIcon) {
    static final String USER_ICON_METADATA = "hudIcon";

    static HudEventSemantics from(UiTestLensLogEntry entry) {
        UiTestLensEventType type = entry == null || entry.eventType() == null
                ? UiTestLensEventType.GENERAL : entry.eventType();
        Map<String, String> metadata = entry == null ? Map.of() : entry.metadata();
        if ("true".equals(metadata.get("testlens.internal.hud.operationTargetUpdate"))) {
            return new HudEventSemantics("ACTION", "RUNNING",
                    metadata.getOrDefault("operationId", ""), 0, 0, false, "");
        }
        String category = category(type);
        boolean technical = isTechnical(category);
        String phase = phase(type, entry == null ? UiTestLensStatus.INFO : entry.status(), metadata, technical);
        return new HudEventSemantics(category, phase,
                metadata.getOrDefault("operationId", ""),
                nonNegativeInt(metadata.get("attempt")),
                nonNegativeLong(metadata.get("durationMs")),
                technical,
                "USER".equals(category) ? nonBlank(metadata.get(USER_ICON_METADATA)) : "");
    }

    Map<String, Object> toBrowserMap(String severity) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("category", category);
        values.put("phase", phase);
        values.put("operationId", operationId);
        values.put("attempt", attempt);
        values.put("durationMs", durationMs);
        values.put("technical", technical);
        values.put("severity", severity == null ? "INFO" : severity);
        values.put("customIcon", customIcon);
        return values;
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? "" : value;
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

    private static String phase(UiTestLensEventType type,
                                UiTestLensStatus status,
                                Map<String, String> metadata,
                                boolean technical) {
        UiTestLensStatus effective = status == null ? UiTestLensStatus.INFO : status;
        if (technical) {
            return effective == UiTestLensStatus.FAILED || effective == UiTestLensStatus.WARN
                    ? "WARNING" : "DEBUG";
        }
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

    private static boolean isTechnical(String category) {
        return "LOCATOR".equals(category) || "ACTIONABILITY".equals(category)
                || "HIGHLIGHT".equals(category);
    }

    private static int nonNegativeInt(String value) {
        try { return Math.max(0, Integer.parseInt(value)); }
        catch (RuntimeException ignored) { return 0; }
    }

    private static long nonNegativeLong(String value) {
        try { return Math.max(0L, Long.parseLong(value)); }
        catch (RuntimeException ignored) { return 0L; }
    }
}
