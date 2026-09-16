package io.github.testlens;

import java.util.LinkedHashMap;
import java.util.Map;

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
        return builder().enabled(enabled).automaticFeedback(automaticFeedback)
                .actionColor(actionColor).waitingColor(waitingColor).retryColor(retryColor)
                .successColor(successColor).failureColor(failureColor).durationMs(durationMs)
                .borderWidthPx(borderWidthPx).showLabels(showLabels);
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

    public static final class Builder {
        private boolean enabled = true;
        private boolean automaticFeedback = true;
        private String actionColor = "#ffeb3b";
        private String waitingColor = "#38bdf8";
        private String retryColor = "#f59e0b";
        private String successColor = "#22c55e";
        private String failureColor = "#ef4444";
        private long durationMs = 1500L;
        private int borderWidthPx = 2;
        private boolean showLabels = true;

        private Builder() {}
        public Builder enabled(boolean value) { enabled = value; return this; }
        public Builder automaticFeedback(boolean value) { automaticFeedback = value; return this; }
        public Builder actionColor(String value) { actionColor = color(value, "actionColor"); return this; }
        public Builder waitingColor(String value) { waitingColor = color(value, "waitingColor"); return this; }
        public Builder retryColor(String value) { retryColor = color(value, "retryColor"); return this; }
        public Builder successColor(String value) { successColor = color(value, "successColor"); return this; }
        public Builder failureColor(String value) { failureColor = color(value, "failureColor"); return this; }
        public Builder durationMs(long value) {
            if (value < 0) throw new IllegalArgumentException("durationMs must be >= 0");
            durationMs = value; return this;
        }
        public Builder borderWidthPx(int value) {
            if (value < 1 || value > 16) throw new IllegalArgumentException("borderWidthPx must be between 1 and 16");
            borderWidthPx = value; return this;
        }
        public Builder showLabels(boolean value) { showLabels = value; return this; }
        public HighlightOptions build() { return new HighlightOptions(this); }

        private static String color(String value, String field) {
            if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
            return value;
        }
    }
}
