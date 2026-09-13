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
    private final long decorationDurationMs;
    private final String globalOverlayCloseButtonSelector;
    private final HudPosition hudPosition;
    private final int hudOffsetX;
    private final int hudOffsetY;
    private final int hudMaxWidthPx;
    private final HudTheme hudTheme;
    private final HudThemePreset hudThemePreset;
    private final String highlightColor;
    private final HudOptions hudOptions;
    private final boolean hudOptionsAuthoritative;

    private OverlayConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.showHudPanel = builder.showHudPanel;
        this.decorationDurationMs = builder.decorationDurationMs;
        this.globalOverlayCloseButtonSelector = builder.globalOverlayCloseButtonSelector;
        this.hudPosition = builder.hudPosition;
        this.hudOffsetX = builder.hudOffsetX;
        this.hudOffsetY = builder.hudOffsetY;
        this.hudMaxWidthPx = builder.hudMaxWidthPx;
        this.hudTheme = builder.hudTheme;
        this.hudThemePreset = builder.hudThemePreset;
        this.highlightColor = builder.highlightColor;
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
        return highlightColor;
    }

    /** Returns the product-level HUD configuration. */
    public HudOptions getHudOptions() { return hudOptions; }

    /**
     * Returns whether {@link #getHudOptions()} is the authoritative visual HUD configuration.
     * A legacy {@link HudTheme} remains authoritative until explicit product-level HUD options
     * are supplied.
     */
    public boolean isHudOptionsAuthoritative() { return hudOptionsAuthoritative; }

    OverlayConfig withHudOptions(HudOptions value) {
        return builder()
                .enabled(enabled)
                .showHudPanel(showHudPanel)
                .decorationDurationMs(decorationDurationMs)
                .globalOverlayCloseButtonSelector(globalOverlayCloseButtonSelector)
                .hudOffset(hudOffsetX, hudOffsetY)
                .highlightColor(highlightColor)
                .hudOptions(value)
                .build();
    }

    public static final class Builder {

        private boolean enabled = true;
        private boolean showHudPanel = true;
        private long decorationDurationMs = 1500L;
        private String globalOverlayCloseButtonSelector = null;
        private HudPosition hudPosition = HudPosition.BOTTOM_RIGHT;
        private int hudOffsetX = 10;
        private int hudOffsetY = 10;
        private int hudMaxWidthPx = HudOptions.defaults().widthPx();
        private HudTheme hudTheme = HudTheme.defaultTheme();
        private HudThemePreset hudThemePreset = HudThemePreset.DEFAULT;
        private String highlightColor = "#ffeb3b";
        private HudOptions hudOptions = HudOptions.defaults();
        private boolean hudOptionsExplicit;
        private boolean hudOptionsAuthoritative = true;

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder showHudPanel(boolean showHudPanel) {
            this.showHudPanel = showHudPanel;
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

        /** Uses one cohesive, immutable HUD configuration. */
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

        public OverlayConfig build() {
            return new OverlayConfig(this);
        }
    }
}

