package io.github.testlens.hud;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Optional font-stack overrides for semantic HUD sections.
 *
 * <p>Sections without a font override inherit {@link HudOptions#fontPreset()}. Metadata includes
 * labels, pipeline information, and branding. Timestamp font size is independent from the
 * event-log text size and defaults to 9 CSS pixels.
 *
 * @since 0.3.0
 */
public final class HudTypography {
    private final HudFontPreset header;
    private final HudFontPreset currentStep;
    private final HudFontPreset eventLog;
    private final HudFontPreset metadata;
    private final int timestampFontSizePx;

    private HudTypography(Builder builder) {
        this.header = builder.header;
        this.currentStep = builder.currentStep;
        this.eventLog = builder.eventLog;
        this.metadata = builder.metadata;
        this.timestampFontSizePx = builder.timestampFontSizePx;
    }

    /**
     * Returns typography with every section inheriting the global font preset.
     * @return empty section overrides
     * @since 0.3.0
     */
    public static HudTypography inheritAll() { return builder().build(); }

    /**
     * Returns a new section-typography builder.
     * @return a builder with every section set to inherit
     * @since 0.3.0
     */
    public static Builder builder() { return new Builder(); }

    /** Returns a builder containing every value from this configuration. @since 0.3.1 */
    public Builder toBuilder() { return new Builder(this); }

    /**
     * Returns the test-name/header override.
     * @return the override, or empty when the global font is inherited
     * @since 0.3.0
     */
    public Optional<HudFontPreset> header() { return Optional.ofNullable(header); }

    /**
     * Returns the current-step override.
     * @return the override, or empty when the global font is inherited
     * @since 0.3.0
     */
    public Optional<HudFontPreset> currentStep() { return Optional.ofNullable(currentStep); }

    /**
     * Returns the event-log override, including timestamps and status text.
     * @return the override, or empty when the global font is inherited
     * @since 0.3.0
     */
    public Optional<HudFontPreset> eventLog() { return Optional.ofNullable(eventLog); }

    /**
     * Returns the labels, pipeline, and branding override.
     * @return the override, or empty when the global font is inherited
     * @since 0.3.0
     */
    public Optional<HudFontPreset> metadata() { return Optional.ofNullable(metadata); }

    /** Returns the independent timestamp font size. @return CSS pixels @since 0.3.1 */
    public int timestampFontSizePx() { return timestampFontSizePx; }

    Map<String, Object> toRuntimeMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        if (header != null) values.put("header", header.name());
        if (currentStep != null) values.put("currentStep", currentStep.name());
        if (eventLog != null) values.put("eventLog", eventLog.name());
        if (metadata != null) values.put("metadata", metadata.name());
        values.put("timestampFontSize", timestampFontSizePx);
        return Collections.unmodifiableMap(values);
    }

    /**
     * Builds immutable section typography.
     * @since 0.3.0
     */
    public static final class Builder {
        private HudFontPreset header;
        private HudFontPreset currentStep;
        private HudFontPreset eventLog;
        private HudFontPreset metadata;
        private int timestampFontSizePx = 9;

        private Builder() {}
        private Builder(HudTypography source) {
            header = source.header;
            currentStep = source.currentStep;
            eventLog = source.eventLog;
            metadata = source.metadata;
            timestampFontSizePx = source.timestampFontSizePx;
        }

        /**
         * Overrides the test-name/header font stack.
         * @param value local font preset
         * @return this builder
         * @since 0.3.0
         */
        public Builder header(HudFontPreset value) { header = Objects.requireNonNull(value, "header must not be null"); return this; }

        /**
         * Overrides the current-step font stack.
         * @param value local font preset
         * @return this builder
         * @since 0.3.0
         */
        public Builder currentStep(HudFontPreset value) { currentStep = Objects.requireNonNull(value, "currentStep must not be null"); return this; }

        /**
         * Overrides the event-log font stack, including timestamps and status text.
         * @param value local font preset
         * @return this builder
         * @since 0.3.0
         */
        public Builder eventLog(HudFontPreset value) { eventLog = Objects.requireNonNull(value, "eventLog must not be null"); return this; }

        /**
         * Overrides labels, pipeline information, and branding.
         * @param value local font preset
         * @return this builder
         * @since 0.3.0
         */
        public Builder metadata(HudFontPreset value) { metadata = Objects.requireNonNull(value, "metadata must not be null"); return this; }

        /**
         * Sets the timestamp font size independently from event-log messages.
         * @param value size from 8 through 18 CSS pixels
         * @return this builder
         * @since 0.3.1
         */
        public Builder timestampFontSizePx(int value) {
            if (value < 8 || value > 18) {
                throw new IllegalArgumentException("timestampFontSizePx must be between 8 and 18");
            }
            timestampFontSizePx = value;
            return this;
        }

        /**
         * Creates the immutable typography overrides.
         * @return immutable section overrides
         * @since 0.3.0
         */
        public HudTypography build() { return new HudTypography(this); }
    }
}
