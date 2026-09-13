package io.github.testlens.selenium.evidence;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualRedactionOptionsTest {
    @Test
    void defaultsAreSmallAndPasswordSafe() {
        VisualRedactionOptions options = VisualRedactionOptions.defaults();
        assertTrue(options.maskPasswordInputs());
        assertEquals(VisualRedactionFailurePolicy.STRICT, options.failurePolicy());
        assertEquals(VisualRedactionOptions.DEFAULT_SOLID_COLOR, options.solidColor());
        assertEquals(VisualRedactionOptions.DEFAULT_BLUR_RADIUS_PX, options.blurRadiusPx());
        assertTrue(options.rules().isEmpty());
    }

    @Test
    void builderValidatesBoundedAppearanceAndKeepsRulesImmutable() {
        VisualRedactionOptions options = VisualRedactionOptions.builder()
                .maskPasswordInputs(false)
                .mask(By.id("email"), VisualMaskMode.BLUR)
                .failurePolicy(VisualRedactionFailurePolicy.STRICT)
                .solidColor("#12abEF")
                .blurRadiusPx(14)
                .paddingPx(3)
                .maskLabel("<b>REDACTED</b>")
                .build();
        assertFalse(options.maskPasswordInputs());
        assertEquals("#12ABEF", options.solidColor());
        assertEquals(14, options.blurRadiusPx());
        assertEquals(3, options.paddingPx());
        assertEquals("<b>REDACTED</b>", options.maskLabel());
        assertEquals(VisualMaskMode.BLUR, options.rules().get(0).mode());
        assertThrows(UnsupportedOperationException.class,
                () -> options.rules().add(new VisualMaskRule(By.id("x"), VisualMaskMode.SOLID)));
        assertThrows(IllegalArgumentException.class, () -> VisualRedactionOptions.builder().solidColor("red"));
        assertThrows(IllegalArgumentException.class, () -> VisualRedactionOptions.builder().blurRadiusPx(1));
        assertThrows(IllegalArgumentException.class, () -> VisualRedactionOptions.builder().blurRadiusPx(33));
    }

    @Test
    void disabledHasNoMaskWork() {
        assertFalse(VisualRedactionOptions.disabled().hasMasks());
    }
}
