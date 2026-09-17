package io.github.testlens;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Immutable configuration of manual and automatic element-state decoration. @since 0.3.1 */
public final class HighlightOptions {
    private final boolean enabled;
    private final boolean automaticFeedback;
    private final String actionColor;
    private final String waitingColor;
    private final String retryColor;
    private final String successColor;
    private final String failureColor;
    private final long durationMs;
    private final int borderWidthPx;
    private final boolean showLabels;
    private final Set<Field> explicit;

    private HighlightOptions(Builder builder) {
        enabled = builder.enabled;
        automaticFeedback = builder.automaticFeedback;
        actionColor = builder.actionColor;
        waitingColor = builder.waitingColor;
        retryColor = builder.retryColor;
        successColor = builder.successColor;
        failureColor = builder.failureColor;
        durationMs = builder.durationMs;
        borderWidthPx = builder.borderWidthPx;
        showLabels = builder.showLabels;
        explicit = Set.copyOf(builder.explicit);
    }

    public static HighlightOptions defaults() { return builder().build(); }
    public static Builder builder() { return new Builder(); }
    public boolean enabled() { return enabled; }
    public boolean automaticFeedback() { return automaticFeedback; }
    public String actionColor() { return actionColor; }
    public String waitingColor() { return waitingColor; }
    public String retryColor() { return retryColor; }
    public String successColor() { return successColor; }
    public String failureColor() { return failureColor; }
    public long durationMs() { return durationMs; }
    public int borderWidthPx() { return borderWidthPx; }
    public boolean showLabels() { return showLabels; }

    public String color(HighlightState state) {
        return switch (state == null ? HighlightState.ACTION : state) {
            case ACTION -> actionColor;
            case WAITING -> waitingColor;
            case RETRY -> retryColor;
            case SUCCESS -> successColor;
            case FAILURE -> failureColor;
        };
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    HighlightOptions withLegacyDefaults(String legacyActionColor, long legacyDurationMs) {
        if (explicit.contains(Field.ACTION_COLOR) && explicit.contains(Field.DURATION_MS)) return this;
        Builder copy = toBuilder();
        if (!explicit.contains(Field.ACTION_COLOR)) copy.actionColor = legacyActionColor;
        if (!explicit.contains(Field.DURATION_MS)) copy.durationMs = legacyDurationMs;
        return copy.build();
    }

    /** Browser-runtime values; labels are supplied separately after redaction. */
    public Map<String, Object> toRuntimeMap(HighlightState state) {
        Map<String, Object> values = new LinkedHashMap<>();
        HighlightState effective = state == null ? HighlightState.ACTION : state;
        values.put("duration", durationMs);
        values.put("color", color(effective));
        values.put("borderWidth", borderWidthPx);
        values.put("showLabel", showLabels);
        values.put("state", effective.name().toLowerCase(java.util.Locale.ROOT));
        return values;
    }

    /** Builder for immutable highlight configuration. @since 0.3.1 */
    public static final class Builder {
        private boolean enabled = true;
        private boolean automaticFeedback = true;
        private String actionColor = "#ffeb3b";
        private String waitingColor = "#2196f3";
        private String retryColor = "#ff9800";
        private String successColor = "#4caf50";
        private String failureColor = "#f44336";
        private long durationMs = 1500L;
        private int borderWidthPx = 2;
        private boolean showLabels = true;
        private final EnumSet<Field> explicit = EnumSet.noneOf(Field.class);

        private Builder() {}
        private Builder(HighlightOptions source) {
            enabled = source.enabled; automaticFeedback = source.automaticFeedback;
            actionColor = source.actionColor; waitingColor = source.waitingColor; retryColor = source.retryColor;
            successColor = source.successColor; failureColor = source.failureColor; durationMs = source.durationMs;
            borderWidthPx = source.borderWidthPx; showLabels = source.showLabels;
            explicit.addAll(source.explicit);
        }
        public Builder enabled(boolean value) { enabled = value; explicit.add(Field.ENABLED); return this; }
        public Builder automaticFeedback(boolean value) { automaticFeedback = value; explicit.add(Field.AUTOMATIC_FEEDBACK); return this; }
        public Builder actionColor(String value) { actionColor = color(value, "actionColor"); explicit.add(Field.ACTION_COLOR); return this; }
        public Builder waitingColor(String value) { waitingColor = color(value, "waitingColor"); explicit.add(Field.WAITING_COLOR); return this; }
        public Builder retryColor(String value) { retryColor = color(value, "retryColor"); explicit.add(Field.RETRY_COLOR); return this; }
        public Builder successColor(String value) { successColor = color(value, "successColor"); explicit.add(Field.SUCCESS_COLOR); return this; }
        public Builder failureColor(String value) { failureColor = color(value, "failureColor"); explicit.add(Field.FAILURE_COLOR); return this; }
        public Builder durationMs(long value) {
            if (value < 0) throw new IllegalArgumentException("durationMs must be >= 0");
            durationMs = value; explicit.add(Field.DURATION_MS); return this;
        }
        public Builder borderWidthPx(int value) {
            if (value < 1 || value > 16) throw new IllegalArgumentException("borderWidthPx must be between 1 and 16");
            borderWidthPx = value; explicit.add(Field.BORDER_WIDTH); return this;
        }
        public Builder showLabels(boolean value) { showLabels = value; explicit.add(Field.SHOW_LABELS); return this; }
        public HighlightOptions build() { return new HighlightOptions(this); }

        private static String color(String value, String field) {
            if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
            return value;
        }
    }

    private enum Field {
        ENABLED, AUTOMATIC_FEEDBACK, ACTION_COLOR, WAITING_COLOR, RETRY_COLOR,
        SUCCESS_COLOR, FAILURE_COLOR, DURATION_MS, BORDER_WIDTH, SHOW_LABELS
    }
}
