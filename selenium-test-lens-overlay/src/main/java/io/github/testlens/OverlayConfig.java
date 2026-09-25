package io.github.testlens;

import io.github.testlens.hud.HudPosition;
import io.github.testlens.hud.HudOptions;
import io.github.testlens.hud.HudTheme;
import io.github.testlens.hud.HudThemePreset;

/**
 * Visual overlay configuration shared by the browser runtime bridges.
 */
public final class OverlayConfig {

    private final boolean enabled;
    private final boolean showHudPanel;
    private final boolean showHudPanelExplicit;
    private final long decorationDurationMs;
    private final String globalOverlayCloseButtonSelector;
    private final HudPosition hudPosition;
    private final int hudOffsetX;
    private final int hudOffsetY;
    private final int hudMaxWidthPx;
    private final HudTheme hudTheme;
    private final HudThemePreset hudThemePreset;
    private final String highlightColor;
    private final HighlightOptions highlightOptions;
    private final boolean highlightOptionsAuthoritative;
    private final HudOptions hudOptions;
    private final boolean hudOptionsAuthoritative;

    private OverlayConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.showHudPanel = builder.showHudPanel;
        this.showHudPanelExplicit = builder.showHudPanelExplicit;
        this.decorationDurationMs = builder.decorationDurationMs;
        this.globalOverlayCloseButtonSelector = builder.globalOverlayCloseButtonSelector;
        this.hudPosition = builder.hudPosition;
        this.hudOffsetX = builder.hudOffsetX;
        this.hudOffsetY = builder.hudOffsetY;
        this.hudMaxWidthPx = builder.hudMaxWidthPx;
        this.hudTheme = builder.hudTheme;
        this.hudThemePreset = builder.hudThemePreset;
        this.highlightOptions = builder.highlightOptions.withLegacyDefaults(
                builder.highlightColor, builder.decorationDurationMs);
        this.highlightColor = this.highlightOptions.actionColor();
        this.highlightOptionsAuthoritative = builder.highlightOptionsExplicit;
        this.hudOptions = builder.hudOptions;
        this.hudOptionsAuthoritative = builder.hudOptionsAuthoritative;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isShowHudPanel() {
        return showHudPanel;
    }

    boolean isShowHudPanelExplicit() { return showHudPanelExplicit; }

    public long getDecorationDurationMs() {
        return decorationDurationMs;
    }

    public String getGlobalOverlayCloseButtonSelector() {
        return globalOverlayCloseButtonSelector;
    }

    public HudPosition getHudPosition() {
        return hudPosition;
    }

    public int getHudOffsetX() {
        return hudOffsetX;
    }

    public int getHudOffsetY() {
        return hudOffsetY;
    }

    public int getHudMaxWidthPx() {
        return hudMaxWidthPx;
    }

    public HudTheme getHudTheme() {
        return hudTheme;
    }

    public HudThemePreset getHudThemePreset() {
        return hudThemePreset;
    }

    public String getHighlightColor() {
        return highlightOptions.actionColor();
    }

    /** Returns the element-state decoration configuration. @since 0.3.1 */
    public HighlightOptions getHighlightOptions() { return highlightOptions; }

    /** Whether explicit typed options override overlapping legacy setters. @since 0.3.1 */
    public boolean isHighlightOptionsAuthoritative() { return highlightOptionsAuthoritative; }

    /**
     * Returns the product-level HUD configuration.
     * @return effective immutable HUD options
     * @since 0.3.0
     */
    public HudOptions getHudOptions() { return hudOptions; }

    /**
     * Returns whether {@link #getHudOptions()} is the authoritative visual HUD configuration.
     * A legacy {@link HudTheme} remains authoritative until explicit product-level HUD options
     * are supplied.
     * @return whether product-level options override legacy HUD position, sizing, and theme values
     * @since 0.3.0
     */
    public boolean isHudOptionsAuthoritative() { return hudOptionsAuthoritative; }

    OverlayConfig withHudOptions(HudOptions value) {
        return builder()
                .enabled(enabled)
                .showHudPanel(showHudPanel, showHudPanelExplicit)
                .decorationDurationMs(decorationDurationMs)
                .globalOverlayCloseButtonSelector(globalOverlayCloseButtonSelector)
                .hudOffset(hudOffsetX, hudOffsetY)
                .highlightColor(highlightColor)
                .highlightOptions(highlightOptions)
                .hudOptions(value)
                .build();
    }

    OverlayConfig withHighlightOptions(HighlightOptions value) {
        Builder copy = builder().enabled(enabled).showHudPanel(showHudPanel, showHudPanelExplicit)
                .decorationDurationMs(decorationDurationMs)
                .globalOverlayCloseButtonSelector(globalOverlayCloseButtonSelector)
                .hudOffset(hudOffsetX, hudOffsetY).highlightColor(highlightColor);
        if (hudOptionsAuthoritative) copy.hudOptions(hudOptions); else copy.hudTheme(hudTheme);
        return copy.highlightOptions(value).build();
    }

    OverlayConfig withPresentationPolicy(boolean liveHud, boolean automaticFeedback) {
        HighlightOptions effectiveHighlights = highlightOptions.toBuilder()
                .automaticFeedback(automaticFeedback)
                .build();
        Builder copy = builder().enabled(enabled).showHudPanel(liveHud, showHudPanelExplicit)
                .decorationDurationMs(decorationDurationMs)
                .globalOverlayCloseButtonSelector(globalOverlayCloseButtonSelector)
                .hudOffset(hudOffsetX, hudOffsetY).highlightColor(highlightColor);
        if (hudOptionsAuthoritative) copy.hudOptions(hudOptions); else copy.hudTheme(hudTheme);
        return copy.highlightOptions(effectiveHighlights).build();
    }

    public static final class Builder {

        private boolean enabled = true;
        private boolean showHudPanel = true;
        private boolean showHudPanelExplicit;
        private long decorationDurationMs = 1500L;
        private String globalOverlayCloseButtonSelector = null;
        private HudPosition hudPosition = HudPosition.BOTTOM_RIGHT;
        private int hudOffsetX = 10;
        private int hudOffsetY = 10;
        private int hudMaxWidthPx = HudOptions.defaults().widthPx();
        private HudTheme hudTheme = HudTheme.defaultTheme();
        private HudThemePreset hudThemePreset = HudThemePreset.DEFAULT;
        private String highlightColor = "#ffeb3b";
        private HighlightOptions highlightOptions = HighlightOptions.defaults();
        private boolean highlightOptionsExplicit;
        private HudOptions hudOptions = HudOptions.defaults();
        private boolean hudOptionsExplicit;
        private boolean hudOptionsAuthoritative = true;

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder showHudPanel(boolean showHudPanel) {
            return showHudPanel(showHudPanel, true);
        }

        private Builder showHudPanel(boolean showHudPanel, boolean explicit) {
            this.showHudPanel = showHudPanel;
            this.showHudPanelExplicit = explicit;
            return this;
        }

        public Builder decorationDurationMs(long ms) {
            if (ms < 0) {
                throw new IllegalArgumentException("decorationDurationMs must be >= 0");
            }
            this.decorationDurationMs = ms;
            return this;
        }

        public Builder globalOverlayCloseButtonSelector(String selector) {
            this.globalOverlayCloseButtonSelector = selector;
            return this;
        }

        public Builder hudPosition(HudPosition position) {
            if (hudOptionsExplicit) return this;
            if (position != null) {
                this.hudPosition = position;
                this.hudOptions = hudOptions.toBuilder().position(position).build();
            }
            return this;
        }

        public Builder hudOffset(int offsetX, int offsetY) {
            if (hudOptionsExplicit) return this;
            if (offsetX >= 0) {
                this.hudOffsetX = offsetX;
            }
            if (offsetY >= 0) {
                this.hudOffsetY = offsetY;
            }
            if (offsetX >= 0 && offsetX <= 500 && offsetY >= 0 && offsetY <= 500) {
                this.hudOptions = hudOptions.toBuilder().offsetXPx(offsetX).offsetYPx(offsetY).build();
            }
            return this;
        }

        public Builder hudMaxWidthPx(int hudMaxWidthPx) {
            if (hudOptionsExplicit) return this;
            if (hudMaxWidthPx > 0) {
                this.hudMaxWidthPx = hudMaxWidthPx;
                if (hudMaxWidthPx >= 240 && hudMaxWidthPx <= 960) {
                    this.hudOptions = hudOptions.toBuilder().widthPx(hudMaxWidthPx).build();
                }
            }
            return this;
        }

        public Builder hudTheme(HudTheme hudTheme) {
            if (hudOptionsExplicit) return this;
            if (hudTheme != null) {
                this.hudTheme = hudTheme;
                this.hudThemePreset = null;
                this.hudOptionsAuthoritative = false;
            }
            return this;
        }

        /**
         * Uses one cohesive, immutable HUD configuration. Explicit product-level options are
         * authoritative over overlapping legacy HUD setters, independently of call order.
         * @param value options; null leaves the existing configuration unchanged
         * @return this builder
         * @since 0.3.0
         */
        public Builder hudOptions(HudOptions value) {
            if (value != null) {
                this.hudOptions = value;
                this.hudOptionsExplicit = true;
                this.hudOptionsAuthoritative = true;
                this.hudPosition = value.position();
                this.hudOffsetX = value.offsetXPx();
                this.hudOffsetY = value.offsetYPx();
                this.hudMaxWidthPx = value.widthPx();
                this.hudThemePreset = null;
                this.hudTheme = HudTheme.builder()
                        .background(value.background())
                        .foreground(value.primaryTextColor())
                        .mutedForeground(value.mutedTextColor())
                        .accent(value.accentColor())
                        .success(value.successColor())
                        .warning(value.warningColor())
                        .danger(value.failureColor())
                        .opacity(1.0)
                        .maxHeightPx(value.maxHeightPx())
                        .build();
            }
            return this;
        }

        public Builder hudTheme(HudThemePreset preset) {
            if (hudOptionsExplicit) return this;
            if (preset != null) {
                this.hudThemePreset = preset;
                this.hudTheme = HudTheme.fromPreset(preset);
                this.hudOptionsAuthoritative = preset == HudThemePreset.DEFAULT;
            }
            return this;
        }

        public Builder highlightColor(String highlightColor) {
            if (highlightColor != null && !highlightColor.isBlank()) {
                this.highlightColor = highlightColor;
            }
            return this;
        }

        /** Sets typed decoration options; these win over legacy setters in either call order. @since 0.3.1 */
        public Builder highlightOptions(HighlightOptions value) {
            if (value != null) {
                highlightOptions = value;
                highlightOptionsExplicit = true;
            }
            return this;
        }

        public OverlayConfig build() {
            return new OverlayConfig(this);
        }
    }
}

