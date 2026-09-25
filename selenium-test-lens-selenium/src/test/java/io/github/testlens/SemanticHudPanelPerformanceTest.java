package io.github.testlens;

import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.browser.BrowserScriptExecutor;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

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

    @Test
    void readySemanticEntriesUseOneBrowserCallAndKeepOrder() {
        RecordingExecutor executor = new RecordingExecutor();
        OverlayConfig config = OverlayConfig.builder().build();
        SemanticHudPanel panel = new SemanticHudPanel(executor, new OverlayRootManager(executor, config), config);
        panel.init("performance", "local");
        executor.calls.set(0);
        UiTestLensLogEntry one = entry("one");
        UiTestLensLogEntry two = entry("two");

        assertEquals(SemanticHudPanel.AppendResult.APPENDED, panel.appendSemanticBatch(List.of(
                new SemanticHudPanel.SemanticEntry(one, one.message(), null, null, HudEventSemantics.from(one)),
                new SemanticHudPanel.SemanticEntry(two, two.message(), null, null, HudEventSemantics.from(two)))));

        assertEquals(1, executor.calls.get());
        assertEquals(List.of("one", "two"), executor.lastMessages);
    }

    @Test
    void ambiguousDispatchFailureIsNotReplayed() {
        RecordingExecutor executor = new RecordingExecutor();
        OverlayConfig config = OverlayConfig.builder().build();
        SemanticHudPanel panel = new SemanticHudPanel(executor, new OverlayRootManager(executor, config), config);
        panel.init("performance", "local");
        executor.calls.set(0);
        executor.throwNextAppend = true;
        UiTestLensLogEntry value = entry("possibly applied");

        assertEquals(SemanticHudPanel.AppendResult.SKIPPED, panel.appendSemantic(
                value, value.message(), null, null, HudEventSemantics.from(value)));

        assertEquals(1, executor.calls.get(), "an ambiguous transport failure must not replay a possibly applied batch");
    }

    private static UiTestLensLogEntry entry(String message) {
        return UiTestLensLogEntry.builder().level(UiTestLensLogLevel.INFO)
                .eventType(UiTestLensEventType.HUD).status(UiTestLensStatus.INFO)
                .action("hud.log").message(message).build();
    }

    private static final class RecordingExecutor implements BrowserScriptExecutor {
        private final AtomicInteger calls = new AtomicInteger();
        private List<String> lastMessages = List.of();
        private boolean failNextAppend;
        private boolean throwNextAppend;

        @Override
        public Object execute(String script, Object... args) {
            calls.incrementAndGet();
            if (script.contains("test-lens:hud-semantic-batch") && failNextAppend) {
                failNextAppend = false;
                return false;
            }
            if (script.contains("test-lens:hud-semantic-batch")) {
                if (throwNextAppend) {
                    throwNextAppend = false;
                    throw new IllegalStateException("response lost after dispatch");
                }
                @SuppressWarnings("unchecked")
                List<java.util.Map<String, Object>> payload = (List<java.util.Map<String, Object>>) args[0];
                lastMessages = payload.stream().map(value -> String.valueOf(value.get("message"))).toList();
                return true;
            }
            return null;
        }
    }
}
