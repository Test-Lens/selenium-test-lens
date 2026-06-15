package io.github.testlens.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PageWaitsTest {

    @Test
    void namespaceScriptInitializesNetworkDomAndWaitState() {
        String script = UiTestLensRuntimeNames.ensureNamespaceScript();

        assertTrue(script.contains("window.__uiTestLens"));
        assertTrue(script.contains("state.wait"));
        assertTrue(script.contains("state.network"));
        assertTrue(script.contains("state.dom"));
    }

    @Test
    void waitMessageScriptWritesPrimaryStateAndLegacyAlias() {
        String script = PageWaits.rememberLastWaitMessageScript();

        assertTrue(script.contains("state.wait"));
        assertTrue(script.contains("waitState.lastMessage"));
        assertTrue(script.contains("__seleniumLastWaitMessage"));
    }

    @Test
    void networkActiveRequestsScriptReadsPrimaryStateBeforeLegacyAlias() {
        String script = PageWaits.networkActiveRequestsScript();

        assertTrue(script.contains("state.network"));
        assertTrue(script.contains("networkState.activeRequests"));
        assertTrue(script.contains("__seleniumActiveRequests"));
        assertTrue(script.indexOf("networkState.activeRequests") < script.indexOf("__seleniumActiveRequests"));
    }

    @Test
    void networkTrackerScriptUsesPrimaryNetworkStateAndSynchronizesLegacyAliases() {
        String script = PageWaits.networkTrackerScript();

        assertTrue(script.contains("window.__uiTestLens"));
        assertTrue(script.contains("state.network"));
        assertTrue(script.contains("var networkState = window.__uiTestLens.state.network"));
        assertTrue(script.contains("networkState.activeRequests"));
        assertTrue(script.contains("__seleniumActiveRequests"));
        assertTrue(script.contains("__seleniumNetworkTrackerInstalled"));
        assertTrue(script.contains("syncLegacyActiveRequests"));
        assertTrue(script.contains("XMLHttpRequest"));
        assertTrue(script.contains("window.fetch"));
    }

    @Test
    void domStableScriptInitializesDomStateButKeepsElementLocalMutationMarkers() {
        String script = PageWaits.domStableMutationScript();

        assertTrue(script.contains("state.dom"));
        assertTrue(script.contains("MutationObserver"));
        assertTrue(script.contains("__seleniumDomStableInit"));
        assertTrue(script.contains("__seleniumLastMutation"));
    }
}

