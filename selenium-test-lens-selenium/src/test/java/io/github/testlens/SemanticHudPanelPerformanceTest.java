package io.github.testlens;

import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.browser.BrowserScriptExecutor;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticHudPanelPerformanceTest {

    @Test
    void warmSemanticAppendUsesOneBrowserCallPerEvent() {
        RecordingExecutor executor = new RecordingExecutor();
        OverlayConfig config = OverlayConfig.builder().build();
        SemanticHudPanel panel = new SemanticHudPanel(executor, new OverlayRootManager(executor, config), config);
        panel.init("performance", "local");
        executor.calls.set(0);

        assertEquals(SemanticHudPanel.AppendResult.APPENDED, panel.appendSemantic(
                entry("one"), "one", null, null, HudEventSemantics.from(entry("one"))));
        assertEquals(SemanticHudPanel.AppendResult.APPENDED, panel.appendSemantic(
                entry("two"), "two", null, null, HudEventSemantics.from(entry("two"))));

        assertEquals(2, executor.calls.get(), "warm HUD updates must not reinject the runtime");
    }

    @Test
    void missingDocumentRuntimeReinitializesOnceThenReturnsToFastPath() {
        RecordingExecutor executor = new RecordingExecutor();
        OverlayConfig config = OverlayConfig.builder().build();
        SemanticHudPanel panel = new SemanticHudPanel(executor, new OverlayRootManager(executor, config), config);
        panel.init("performance", "local");
        executor.calls.set(0);
        executor.failNextAppend = true;

        UiTestLensLogEntry first = entry("after navigation");
        assertEquals(SemanticHudPanel.AppendResult.APPENDED, panel.appendSemantic(
                first, first.message(), null, null, HudEventSemantics.from(first)));
        int coldCalls = executor.calls.get();
        assertTrue(coldCalls > 1, "cold document must reinstall its overlay runtime");

        UiTestLensLogEntry second = entry("warm again");
        panel.appendSemantic(second, second.message(), null, null, HudEventSemantics.from(second));
        assertEquals(coldCalls + 1, executor.calls.get(), "the recovered document must use the fast path");
    }

    private static UiTestLensLogEntry entry(String message) {
        return UiTestLensLogEntry.builder().level(UiTestLensLogLevel.INFO)
                .eventType(UiTestLensEventType.HUD).status(UiTestLensStatus.INFO)
                .action("hud.log").message(message).build();
    }

    private static final class RecordingExecutor implements BrowserScriptExecutor {
        private final AtomicInteger calls = new AtomicInteger();
        private boolean failNextAppend;

        @Override
        public Object execute(String script, Object... args) {
            calls.incrementAndGet();
            if (script.contains("hud.log(arguments[0]") && failNextAppend) {
                failNextAppend = false;
                return false;
            }
            return script.contains("hud.log(arguments[0]") ? true : null;
        }
    }
}
