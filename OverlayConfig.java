package utils.jsExecHelper;

import utils.jsExecHelper.hud.HudPosition;

/**
 * Konfiguracja overlaya – steruje tym, czy overlay jest włączony,
 * czy ma być widoczny HUD oraz jak długo mają się wyświetlać dekoracje
 * (ramki, dymki itd.), a także pozwala zdefiniować:
 * - globalny selektor przycisku zamykającego overlay/popup,
 * - pozycję i rozmiar HUD-a,
 * - kolor highlighta (ramek / badge’y).
 */
public final class OverlayConfig {

    private final boolean enabled;
    private final boolean showHudPanel;
    private final long decorationDurationMs;

    /**
     * Opcjonalny globalny selektor przycisku zamykającego overlay/popup.
     * Np. "button#acceptCookies" albo ".cookie-accept-all".
     * Może być null, jeśli nie chcesz nic narzucać.
     */
    private final String globalOverlayCloseButtonSelector;

    // HUD
    private final HudPosition hudPosition;
    private final int hudOffsetX;
    private final int hudOffsetY;
    private final int hudMaxWidthPx;

    // Highlight (ramki, badge'e)
    /**
     * Kolor highlighta (ramka wokół elementu, tło badge'a)
     * w formacie CSS (np. "#ffeb3b" albo "rgba(255,235,59,1)").
     */
    private final String highlightColor;

    // w przyszłości można dodać np. theme, maskowanie haseł, itp.

    private OverlayConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.showHudPanel = builder.showHudPanel;
        this.decorationDurationMs = builder.decorationDurationMs;
        this.globalOverlayCloseButtonSelector = builder.globalOverlayCloseButtonSelector;
        this.hudPosition = builder.hudPosition;
        this.hudOffsetX = builder.hudOffsetX;
        this.hudOffsetY = builder.hudOffsetY;
        this.hudMaxWidthPx = builder.hudMaxWidthPx;
        this.highlightColor = builder.highlightColor;
    }

    /**
     * Tworzy nowego buildera konfiguracji.
     */
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

    /**
     * Zwraca globalny selektor przycisku zamykającego overlay/popup,
     * jeśli został ustawiony (może być null).
     */
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

    /**
     * Zwraca kolor highlighta używany do ramek i badge'y.
     */
    public String getHighlightColor() {
        return highlightColor;
    }

    /**
     * Builder dla {@link OverlayConfig}.
     */
    public static final class Builder {

        // wartości domyślne
        private boolean enabled = true;
        private boolean showHudPanel = true;
        private long decorationDurationMs = 1500L;
        private String globalOverlayCloseButtonSelector = null;

        // HUD – domyślnie: prawy-dolny róg, małe offsety, ~280px
        private HudPosition hudPosition = HudPosition.BOTTOM_RIGHT;
        private int hudOffsetX = 10;   // od krawędzi poziomej (px)
        private int hudOffsetY = 10;   // od krawędzi pionowej (px)
        private int hudMaxWidthPx = 280;

        // Highlight – domyślnie żółty
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

        /**
         * Globalny selektor przycisku zamykającego overlay/popup,
         * np. "#acceptCookies" albo ".cookie-accept-all".
         */
        public Builder globalOverlayCloseButtonSelector(String selector) {
            this.globalOverlayCloseButtonSelector = selector;
            return this;
        }

        /**
         * Pozycja HUD-a na ekranie (jeden z czterech rogów).
         */
        public Builder hudPosition(HudPosition position) {
            if (position != null) {
                this.hudPosition = position;
            }
            return this;
        }

        /**
         * Offset HUD-a od krawędzi (w pikselach).
         * X – od lewej/prawej, Y – od góry/dół (w zależności od pozycji).
         */
        public Builder hudOffset(int offsetX, int offsetY) {
            if (offsetX >= 0) {
                this.hudOffsetX = offsetX;
            }
            if (offsetY >= 0) {
                this.hudOffsetY = offsetY;
            }
            return this;
        }

        /**
         * Maksymalna szerokość HUD-a w px.
         */
        public Builder hudMaxWidthPx(int hudMaxWidthPx) {
            if (hudMaxWidthPx > 0) {
                this.hudMaxWidthPx = hudMaxWidthPx;
            }
            return this;
        }

        /**
         * Kolor highlighta (ramka + badge) w formacie CSS.
         * Np. "#ffeb3b", "#00ff00", "rgba(0,255,0,0.9)".
         */
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
