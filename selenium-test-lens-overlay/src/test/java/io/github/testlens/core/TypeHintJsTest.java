package io.github.testlens.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeHintJsTest {

    @Test
    void initLoadsTypeHintResource() {
        assertFalse(TypeHintJs.INIT.isBlank());
        assertTrue(TypeHintJs.INIT.contains("__uiTestLens"));
        assertTrue(TypeHintJs.INIT.contains("modules.typeHint"));
        assertTrue(TypeHintJs.INIT.contains("show: show"));
        assertTrue(TypeHintJs.INIT.contains("hide: hide"));
        assertTrue(TypeHintJs.INIT.contains("clear: clear"));
    }

    @Test
    void bridgeScriptUsesPrimaryTypeHintModule() {
        String script = TypeHintJs.bridgeScript();

        assertTrue(script.contains("modules.typeHint"));
    }
}
