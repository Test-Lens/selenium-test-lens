package io.github.testlens;

import io.github.testlens.hud.HudBranding;
import io.github.testlens.hud.HudFontPreset;
import io.github.testlens.hud.HudLogoPlacement;
import io.github.testlens.hud.HudOptions;
import io.github.testlens.hud.HudPosition;
import io.github.testlens.hud.HudPreset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HudOptionsFacadeTest {
    @Test void facadeAcceptsStudioStyleConfiguration() {
        HudOptions hud = HudOptions.builder()
                .preset(HudPreset.COMPACT)
                .position(HudPosition.TOP_RIGHT)
                .offsetXPx(16)
                .offsetYPx(24)
                .maxHeightPx(420)
                .fontPreset(HudFontPreset.SYSTEM)
                .logoPlacement(HudLogoPlacement.HEADER_RIGHT)
                .showPipeline(false)
                .showNetwork(true)
                .background("#071018")
                .backgroundOpacity(.88)
                .accentColor("#39ff88")
                .build();

        TestLensOptions options = TestLensOptions.builder().hud(hud).build();
        assertSame(hud, options.hud());
        assertSame(hud, options.overlayConfig().getHudOptions());
        assertEquals(HudPosition.TOP_RIGHT, options.overlayConfig().getHudPosition());
        assertEquals(16, options.overlayConfig().getHudOffsetX());
        assertEquals(24, options.overlayConfig().getHudOffsetY());
    }

    @Test void representativePresetConfigurationsCompileAndBuild() {
        for (HudPreset preset : HudPreset.values()) {
            HudOptions options = HudOptions.builder().preset(preset)
                    .branding(HudBranding.TEST_LENS)
                    .showAssertions(true)
                    .build();
            assertEquals(preset, TestLensOptions.builder().hud(options).build().hud().preset());
        }
    }

    @Test void nullHudRestoresProductDefault() {
        assertEquals(HudPreset.COMPACT, TestLensOptions.builder().hud(null).build().hud().preset());
    }
}
