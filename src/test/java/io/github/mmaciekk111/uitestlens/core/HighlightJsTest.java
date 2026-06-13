package io.github.mmaciekk111.uitestlens.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HighlightJsTest {

    @Test
    void initLoadsHighlightResource() {
        assertFalse(HighlightJs.INIT.isBlank());
        assertTrue(HighlightJs.INIT.contains("__uiTestLens"));
        assertTrue(HighlightJs.INIT.contains("modules.highlight"));
        assertTrue(HighlightJs.INIT.contains("element: element"));
        assertTrue(HighlightJs.INIT.contains("parent: parent"));
        assertTrue(HighlightJs.INIT.contains("ancestor: ancestor"));
        assertTrue(HighlightJs.INIT.contains("closest: closest"));
        assertTrue(HighlightJs.INIT.contains("clear: clear"));
    }

    @Test
    void bridgeScriptUsesPrimaryHighlightModule() {
        String script = HighlightJs.bridgeScript();

        assertTrue(script.contains("modules.highlight"));
    }
}
