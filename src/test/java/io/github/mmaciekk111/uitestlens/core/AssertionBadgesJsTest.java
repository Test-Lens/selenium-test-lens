package io.github.mmaciekk111.uitestlens.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssertionBadgesJsTest {

    @Test
    void initLoadsAssertionBadgesResource() {
        assertFalse(AssertionBadgesJs.INIT.isBlank());
        assertTrue(AssertionBadgesJs.INIT.contains("__uiTestLens"));
        assertTrue(AssertionBadgesJs.INIT.contains("modules.assertionBadges"));
        assertTrue(AssertionBadgesJs.INIT.contains("show: show"));
        assertTrue(AssertionBadgesJs.INIT.contains("clear: clear"));
        assertTrue(AssertionBadgesJs.INIT.contains("selenium-assert-badge"));
    }

    @Test
    void bridgeScriptUsesPrimaryAssertionBadgesModule() {
        String script = AssertionBadgesJs.bridgeScript();

        assertTrue(script.contains("modules.assertionBadges"));
    }
}
