package io.github.testlens;

import io.github.testlens.hud.HudPosition;
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

    public static final class Builder {

        private boolean enabled = true;
        private boolean showHudPanel = true;
        private long decorationDurationMs = 1500L;
        private String globalOverlayCloseButtonSelector = null;
        private HudPosition hudPosition = HudPosition.BOTTOM_RIGHT;
        private int hudOffsetX = 10;
        private int hudOffsetY = 10;
        private int hudMaxWidthPx = 280;
        private HudTheme hudTheme = HudTheme.defaultTheme();
        private HudThemePreset hudThemePreset = HudThemePreset.DEFAULT;
        private String highlightColor = "#ffeb3b";

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
            if (position != null) {
                this.hudPosition = position;
            }
            return this;
        }

        public Builder hudOffset(int offsetX, int offsetY) {
            if (offsetX >= 0) {
                this.hudOffsetX = offsetX;
            }
            if (offsetY >= 0) {
                this.hudOffsetY = offsetY;
            }
            return this;
        }

        public Builder hudMaxWidthPx(int hudMaxWidthPx) {
            if (hudMaxWidthPx > 0) {
                this.hudMaxWidthPx = hudMaxWidthPx;
            }
            return this;
        }

        public Builder hudTheme(HudTheme hudTheme) {
            if (hudTheme != null) {
                this.hudTheme = hudTheme;
                this.hudThemePreset = null;
            }
            return this;
        }

        public Builder hudTheme(HudThemePreset preset) {
            if (preset != null) {
                this.hudThemePreset = preset;
                this.hudTheme = HudTheme.fromPreset(preset);
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
