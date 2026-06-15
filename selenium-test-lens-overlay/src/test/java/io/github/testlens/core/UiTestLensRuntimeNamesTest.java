package io.github.testlens.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiTestLensRuntimeNamesTest {

    @Test
    void exposesPrimaryNamespaceAndResourceRoot() {
        assertEquals("__uiTestLens", UiTestLensRuntimeNames.GLOBAL_NAMESPACE);
        assertEquals("uitestlens/runtime/", UiTestLensRuntimeNames.RUNTIME_RESOURCE_ROOT);
    }

    @Test
    void namespaceScriptInitializesUiTestLensAndWaitState() {
        String script = UiTestLensRuntimeNames.ensureNamespaceScript();

        assertTrue(script.contains("window.__uiTestLens"));
        assertTrue(script.contains("state.wait"));
        assertTrue(script.contains("modules"));
    }
}

