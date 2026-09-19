package io.github.testlens.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudTypographyTest {
    @Test void sectionsInheritUnlessExplicitlyOverridden() {
        HudTypography inherited = HudTypography.inheritAll();
        assertTrue(inherited.header().isEmpty());
        assertTrue(inherited.currentStep().isEmpty());
        assertTrue(inherited.eventLog().isEmpty());
        assertTrue(inherited.metadata().isEmpty());
        assertEquals(9, inherited.timestampFontSizePx());
        assertEquals(9, inherited.toRuntimeMap().get("timestampFontSize"));

        HudTypography mixed = HudTypography.builder()
                .header(HudFontPreset.MONOSPACE)
                .currentStep(HudFontPreset.SYSTEM)
                .eventLog(HudFontPreset.UI_SANS)
                .metadata(HudFontPreset.MONOSPACE)
                .timestampFontSizePx(12)
                .build();
        assertEquals(HudFontPreset.MONOSPACE, mixed.header().orElseThrow());
        assertEquals(HudFontPreset.SYSTEM, mixed.currentStep().orElseThrow());
        assertEquals(HudFontPreset.UI_SANS, mixed.eventLog().orElseThrow());
        assertEquals(HudFontPreset.MONOSPACE, mixed.metadata().orElseThrow());
        assertEquals(12, mixed.timestampFontSizePx());
        assertEquals(12, mixed.toBuilder().build().timestampFontSizePx());
    }

    @Test void rejectsNullSectionOverrides() {
        assertThrows(NullPointerException.class, () -> HudTypography.builder().header(null));
        assertThrows(NullPointerException.class, () -> HudTypography.builder().currentStep(null));
        assertThrows(NullPointerException.class, () -> HudTypography.builder().eventLog(null));
        assertThrows(NullPointerException.class, () -> HudTypography.builder().metadata(null));
    }
}
