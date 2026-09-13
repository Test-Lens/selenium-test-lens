package io.github.testlens;

import io.github.testlens.hud.HudTheme;
import io.github.testlens.hud.HudThemePreset;
import io.github.testlens.hud.HudPreset;
import io.github.testlens.hud.HudOptions;
import io.github.testlens.hud.HudPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayConfigTest {

    @Test
    void defaultsIncludeDefaultHudTheme() {
        OverlayConfig config = OverlayConfig.builder().build();

        assertEquals(HudThemePreset.DEFAULT, config.getHudThemePreset());
        assertNotNull(config.getHudTheme());
        assertEquals("rgba(15, 23, 42, 0.96)", config.getHudTheme().background());
        assertEquals(420, config.getHudMaxWidthPx());
        assertEquals(HudPreset.COMPACT, config.getHudOptions().preset());
        assertTrue(config.isHudOptionsAuthoritative());
    }

    @Test
    void presetSetsHudTheme() {
        OverlayConfig config = OverlayConfig.builder()
                .hudTheme(HudThemePreset.GLASS)
                .build();

        assertEquals(HudThemePreset.GLASS, config.getHudThemePreset());
        assertEquals("#38bdf8", config.getHudTheme().accent());
        assertFalse(config.isHudOptionsAuthoritative());
    }

    @Test
    void userConfiguredHudWidthOverridesDefault() {
        OverlayConfig config = OverlayConfig.builder()
                .hudMaxWidthPx(360)
                .build();

        assertEquals(360, config.getHudMaxWidthPx());
    }

    @Test
    void customThemeClearsPresetMarker() {
        HudTheme custom = HudTheme.builder()
                .background("#111")
                .foreground("#eee")
                .build();

        OverlayConfig config = OverlayConfig.builder()
                .hudTheme(HudThemePreset.DARK)
                .hudTheme(custom)
                .build();

        assertNull(config.getHudThemePreset());
        assertEquals("#111", config.getHudTheme().background());
        assertEquals("#eee", config.getHudTheme().foreground());
        assertFalse(config.isHudOptionsAuthoritative());
    }

    @Test
    void explicitHudOptionsWinOverLegacyHudSettingsRegardlessOfCallOrder() {
        HudOptions hud = HudOptions.builder().preset(HudPreset.DEBUG)
                .position(HudPosition.TOP_LEFT).widthPx(480).build();

        OverlayConfig legacyFirst = OverlayConfig.builder()
                .hudPosition(HudPosition.BOTTOM_RIGHT).hudMaxWidthPx(360)
                .hudTheme(HudThemePreset.GLASS).hudOptions(hud).build();
        OverlayConfig legacyLast = OverlayConfig.builder()
                .hudOptions(hud).hudPosition(HudPosition.BOTTOM_RIGHT).hudMaxWidthPx(360)
                .hudTheme(HudThemePreset.GLASS).build();

        assertEquals(HudPosition.TOP_LEFT, legacyFirst.getHudPosition());
        assertEquals(HudPosition.TOP_LEFT, legacyLast.getHudPosition());
        assertEquals(480, legacyFirst.getHudMaxWidthPx());
        assertEquals(480, legacyLast.getHudMaxWidthPx());
        assertEquals(hud, legacyFirst.getHudOptions());
        assertEquals(hud, legacyLast.getHudOptions());
        assertTrue(legacyFirst.isHudOptionsAuthoritative());
        assertTrue(legacyLast.isHudOptionsAuthoritative());
    }
}

