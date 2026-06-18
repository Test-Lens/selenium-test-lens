package io.github.testlens;

import io.github.testlens.hud.HudTheme;
import io.github.testlens.hud.HudThemePreset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class OverlayConfigTest {

    @Test
    void defaultsIncludeDefaultHudTheme() {
        OverlayConfig config = OverlayConfig.builder().build();

        assertEquals(HudThemePreset.DEFAULT, config.getHudThemePreset());
        assertNotNull(config.getHudTheme());
        assertEquals("rgba(15, 23, 42, 0.96)", config.getHudTheme().background());
        assertEquals(520, config.getHudMaxWidthPx());
    }

    @Test
    void presetSetsHudTheme() {
        OverlayConfig config = OverlayConfig.builder()
                .hudTheme(HudThemePreset.GLASS)
                .build();

        assertEquals(HudThemePreset.GLASS, config.getHudThemePreset());
        assertEquals("#38bdf8", config.getHudTheme().accent());
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
    }
}

