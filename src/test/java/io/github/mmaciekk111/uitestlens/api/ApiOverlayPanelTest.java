package io.github.mmaciekk111.uitestlens.api;

import io.github.mmaciekk111.uitestlens.OverlayConfig;
import io.github.mmaciekk111.uitestlens.core.OverlayRootManager;
import io.github.mmaciekk111.uitestlens.core.browser.BrowserScriptExecutor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiOverlayPanelTest {

    @Test
    void showRequestInjectsRuntimeAndCallsApiOverlayBridge() {
        RecordingBrowserScriptExecutor executor = new RecordingBrowserScriptExecutor();
        ApiOverlayPanel panel = apiOverlayPanel(executor);

        String requestId = panel.showRequest("Create order", "POST", "/orders", "{ }");

        assertEquals("request-1", requestId);
        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains("modules.apiOverlay")));
        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains("__seleniumApiModal.showRequest")));
        assertTrue(executor.args.stream().anyMatch(args ->
                Arrays.asList(args).contains("Create order") &&
                        Arrays.asList(args).contains("POST") &&
                        Arrays.asList(args).contains("/orders")
        ));
    }

    @Test
    void statusMethodsCallApiOverlayBridge() {
        RecordingBrowserScriptExecutor executor = new RecordingBrowserScriptExecutor();
        ApiOverlayPanel panel = apiOverlayPanel(executor);

        panel.setPending("request-1", 1000L);
        panel.setResponse("request-1", 200, 33L, "headers", "body");
        panel.setError("request-1", "error", "details");
        panel.hide();

        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains("setPending")));
        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains("setResponse")));
        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains("setError")));
        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains("hide")));
    }

    @Test
    void highlightAndFilterMethodsCallApiOverlayBridge() {
        RecordingBrowserScriptExecutor executor = new RecordingBrowserScriptExecutor();
        ApiOverlayPanel panel = apiOverlayPanel(executor);

        assertTrue(panel.apiHighlightJsonPath("$.id"));
        assertEquals(2, panel.apiHighlightKeyAnimated("id", 10L, 5));
        panel.highlightPathAnimated("$.id", 10);
        panel.highlightPathsAnimated(List.of("$.id"), 10, 20);
        assertTrue(panel.filterToPaths(List.of("$.id"), true));
        assertFalse(panel.clearFilter());

        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains("highlightPath")));
        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains("highlightKeyAnimated")));
        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains("highlightPathsAnimated")));
        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains("filterToPaths")));
        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains("clearFilter")));
    }

    @Test
    void asyncHighlightMethodsUseAsyncExecutor() {
        RecordingBrowserScriptExecutor executor = new RecordingBrowserScriptExecutor();
        ApiOverlayPanel panel = apiOverlayPanel(executor);

        assertTrue(panel.highlightPathsAnimatedAndWait(List.of("$.id"), 10, 20));
        assertTrue(panel.highlightPathsCandyAnimatedAndWait(List.of("$.id"), 10, 20, 30, 40));

        assertTrue(executor.asyncScripts.stream().anyMatch(script -> script.contains("highlightPathsAnimatedAsync")));
        assertTrue(executor.asyncScripts.stream().anyMatch(script -> script.contains("highlightPathsCandyAnimatedAsync")));
    }

    private static ApiOverlayPanel apiOverlayPanel(RecordingBrowserScriptExecutor executor) {
        OverlayConfig config = OverlayConfig.builder().build();
        return new ApiOverlayPanel(executor, new OverlayRootManager(executor, config), config);
    }

    private static final class RecordingBrowserScriptExecutor implements BrowserScriptExecutor {
        private final List<String> scripts = new ArrayList<>();
        private final List<Object[]> args = new ArrayList<>();
        private final List<String> asyncScripts = new ArrayList<>();

        @Override
        public Object execute(String script, Object... args) {
            scripts.add(script);
            this.args.add(Arrays.copyOf(args, args.length));
            if (script.contains("showRequest")) {
                return "request-1";
            }
            if (script.contains("highlightKeyAnimated")) {
                return 2;
            }
            if (script.contains("highlightPath(arguments[0])") || script.contains("filterToPaths")) {
                return true;
            }
            return null;
        }

        @Override
        public Object executeAsync(String script, Object... args) {
            asyncScripts.add(script);
            this.args.add(Arrays.copyOf(args, args.length));
            return true;
        }
    }
}
