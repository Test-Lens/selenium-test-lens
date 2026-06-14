package io.github.mmaciekk111.uitestlens.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollArrowJsTest {

    @Test
    void initLoadsScrollArrowResource() {
        assertFalse(ScrollArrowJs.INIT.isBlank());
        assertTrue(ScrollArrowJs.INIT.contains("__uiTestLens"));
        assertTrue(ScrollArrowJs.INIT.contains("modules.scrollArrow"));
        assertTrue(ScrollArrowJs.INIT.contains("show: show"));
        assertTrue(ScrollArrowJs.INIT.contains("hide: hide"));
        assertTrue(ScrollArrowJs.INIT.contains("clear: clear"));
        assertTrue(ScrollArrowJs.INIT.contains("scrollToElementWithArrow"));
    }

    @Test
    void bridgeScriptUsesPrimaryScrollArrowModule() {
        String script = ScrollArrowJs.bridgeScript();

        assertTrue(script.contains("modules.scrollArrow"));
    }
}
