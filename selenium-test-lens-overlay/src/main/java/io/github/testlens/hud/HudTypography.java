package io.github.testlens.hud;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Optional font-stack overrides for semantic HUD sections.
 *
 * <p>Sections without an override inherit {@link HudOptions#fontPreset()}. Metadata includes
 * labels, pipeline information, and branding. Timestamps remain part of the event-log section.
 */
public final class HudTypography {
    private final HudFontPreset header;
    private final HudFontPreset currentStep;
    private final HudFontPreset eventLog;
    private final HudFontPreset metadata;

    private HudTypography(Builder builder) {
        this.header = builder.header;
        this.currentStep = builder.currentStep;
        this.eventLog = builder.eventLog;
        this.metadata = builder.metadata;
    }

    /** Returns typography with every section inheriting the global font preset. */
    public static HudTypography inheritAll() { return builder().build(); }

    /** Returns a new section-typography builder. */
    public static Builder builder() { return new Builder(); }

    /** Returns the header override, or empty when the global font is inherited. */
    public Optional<HudFontPreset> header() { return Optional.ofNullable(header); }

    /** Returns the current-step override, or empty when the global font is inherited. */
    public Optional<HudFontPreset> currentStep() { return Optional.ofNullable(currentStep); }

    /** Returns the event-log override, or empty when the global font is inherited. */
    public Optional<HudFontPreset> eventLog() { return Optional.ofNullable(eventLog); }

    /** Returns the metadata override, or empty when the global font is inherited. */
    public Optional<HudFontPreset> metadata() { return Optional.ofNullable(metadata); }

    Map<String, Object> toRuntimeMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        if (header != null) values.put("header", header.name());
        if (currentStep != null) values.put("currentStep", currentStep.name());
        if (eventLog != null) values.put("eventLog", eventLog.name());
        if (metadata != null) values.put("metadata", metadata.name());
        return Collections.unmodifiableMap(values);
    }

    /** Builds immutable section typography. */
    public static final class Builder {
        private HudFontPreset header;
        private HudFontPreset currentStep;
        private HudFontPreset eventLog;
        private HudFontPreset metadata;

        private Builder() {}

        /** Overrides the test-name/header font stack. */
        public Builder header(HudFontPreset value) { header = Objects.requireNonNull(value, "header must not be null"); return this; }

        /** Overrides the current-step font stack. */
        public Builder currentStep(HudFontPreset value) { currentStep = Objects.requireNonNull(value, "currentStep must not be null"); return this; }

        /** Overrides the event-log font stack, including timestamps and status text. */
        public Builder eventLog(HudFontPreset value) { eventLog = Objects.requireNonNull(value, "eventLog must not be null"); return this; }

        /** Overrides labels, pipeline information, and branding. */
        public Builder metadata(HudFontPreset value) { metadata = Objects.requireNonNull(value, "metadata must not be null"); return this; }

        /** Creates the immutable typography overrides. */
        public HudTypography build() { return new HudTypography(this); }
    }
}
