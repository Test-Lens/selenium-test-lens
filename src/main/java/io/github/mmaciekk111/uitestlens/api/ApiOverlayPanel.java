package io.github.mmaciekk111.uitestlens.api;

import io.github.mmaciekk111.uitestlens.OverlayConfig;
import io.github.mmaciekk111.uitestlens.core.OverlayRootManager;
import io.github.mmaciekk111.uitestlens.core.browser.BrowserScriptExecutor;
import io.github.mmaciekk111.uitestlens.core.browser.SeleniumBrowserScriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ApiOverlayPanel {

    private final BrowserScriptExecutor scriptExecutor;
    private final OverlayRootManager rootManager;
    private final OverlayConfig config;

    public ApiOverlayPanel(WebDriver driver, OverlayRootManager rootManager, OverlayConfig config) {
        this(new SeleniumBrowserScriptExecutor(driver), rootManager, config);
    }

    public ApiOverlayPanel(BrowserScriptExecutor scriptExecutor, OverlayRootManager rootManager, OverlayConfig config) {
        this.scriptExecutor = Objects.requireNonNull(scriptExecutor, "scriptExecutor must not be null");
        this.rootManager = Objects.requireNonNull(rootManager, "rootManager must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    public String showRequest(String title, String method, String url, String payloadPreview) {
        Object result = executeApiOverlayScript(
                "return window.__seleniumApiModal.showRequest(arguments[0], arguments[1], arguments[2], arguments[3]);",
                title, method, url, payloadPreview
        );

        if (result == null) {
            throw new IllegalStateException("ApiOverlayJs.showRequest returned null");
        }
        return result.toString();
    }

    public void setPending(String requestId, long timeoutMs) {
        try {
            executeApiOverlayScript(
                    "window.__seleniumApiModal.setPending(arguments[0], arguments[1]);",
                    requestId, timeoutMs
            );
        } catch (Exception ignored) {}
    }

    public void setResponse(String requestId,
                            int status,
                            long durationMs,
                            String headersPreview,
                            String bodyPreview) {
        try {
            executeApiOverlayScript(
                    "window.__seleniumApiModal.setResponse(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]);",
                    requestId, status, durationMs, headersPreview, bodyPreview
            );
        } catch (Exception ignored) {}
    }

    public void setError(String requestId, String message, String details) {
        try {
            executeApiOverlayScript(
                    "window.__seleniumApiModal.setError(arguments[0], arguments[1], arguments[2]);",
                    requestId, message, details
            );
        } catch (Exception ignored) {}
    }

    public void hide() {
        try {
            executeApiOverlayScript("window.__seleniumApiModal.hide();");
        } catch (Exception ignored) {}
    }

    public boolean apiHighlightJsonPath(String path) {
        try {
            Object r = executeApiOverlayScript(
                    "return window.__seleniumApiModal.highlightPath(arguments[0]);",
                    path
            );
            return r instanceof Boolean && (Boolean) r;
        } catch (Exception ignored) {
            return false;
        }
    }

    public int apiHighlightKeyAnimated(String key, long delayMs, int maxHits) {
        try {
            Object r = executeApiOverlayScript(
                    "return window.__seleniumApiModal.highlightKeyAnimated(arguments[0], arguments[1], arguments[2]);",
                    key, delayMs, maxHits
            );
            if (r instanceof Number) return ((Number) r).intValue();
            return 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    public void ensureOpen() {
        try {
            executeApiOverlayScript(
                    "var el = (window.__seleniumOverlayRoot && window.__seleniumOverlayRoot.querySelector) " +
                            "  ? window.__seleniumOverlayRoot.querySelector('#selenium-api-modal') : null;" +
                            "if (el) { el.style.display='block'; el.style.visibility='visible'; el.style.opacity='1'; el.style.zIndex='2147483647'; }"
            );
        } catch (Exception e) {
            throw new RuntimeException("ensureOpen failed: " + e.getMessage(), e);
        }
    }

    public void highlightPathAnimated(String jsonPath, int stepDelayMs) {
        executeApiOverlayScript(
                "return window.__seleniumApiModal && window.__seleniumApiModal.highlightPathAnimated(arguments[0], arguments[1]);",
                jsonPath, stepDelayMs
        );
    }

    public void highlightPathsAnimated(List<String> jsonPaths, int stepDelayMs, int betweenPathsMs) {
        executeApiOverlayScript(
                "return window.__seleniumApiModal && window.__seleniumApiModal.highlightPathsAnimated(arguments[0], arguments[1], arguments[2]);",
                jsonPaths, stepDelayMs, betweenPathsMs
        );
    }

    public boolean highlightPathsAnimatedAndWait(List<String> jsonPaths, int stepDelayMs, int betweenPathsMs) {
        Object ret = executeApiOverlayAsyncScript(
                "var done = arguments[arguments.length-1];" +
                        "if (!window.__seleniumApiModal) { done(false); return; }" +
                        "window.__seleniumApiModal.highlightPathsAnimatedAsync(arguments[0], arguments[1], arguments[2], done);",
                jsonPaths, stepDelayMs, betweenPathsMs
        );
        return Boolean.TRUE.equals(ret);
    }

    public List<String> findPathsByKey(String key) {
        Object r = executeApiOverlayScript(
                "return (window.__seleniumApiModal && window.__seleniumApiModal.findPathsByKey(arguments[0])) || [];",
                key
        );

        List<String> out = new ArrayList<>();
        if (r instanceof List<?>) {
            for (Object o : (List<?>) r) {
                if (o != null) out.add(o.toString());
            }
        }
        return out;
    }

    public boolean highlightPathsCandyAnimatedAndWait(List<String> jsonPaths,
                                                      int stepDelayMs,
                                                      int betweenPathsMs,
                                                      int keepColorMs,
                                                      int focusFadeMs) {
        Object ret = executeApiOverlayAsyncScript(
                "var done = arguments[arguments.length-1];" +
                        "if (!window.__seleniumApiModal) { done(false); return; }" +
                        "window.__seleniumApiModal.highlightPathsCandyAnimatedAsync(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], done);",
                jsonPaths, stepDelayMs, betweenPathsMs, keepColorMs, focusFadeMs
        );
        return Boolean.TRUE.equals(ret);
    }

    public void resetApiFocus() {
        executeApiOverlayScript(
                "return window.__seleniumApiModal && window.__seleniumApiModal.resetFocus();"
        );
    }

    public boolean filterToPaths(List<String> jsonPaths, boolean keepParents) {
        Object ret = executeApiOverlayScript(
                "return window.__seleniumApiModal && window.__seleniumApiModal.filterToPaths(arguments[0], arguments[1]);",
                jsonPaths, keepParents
        );
        return Boolean.TRUE.equals(ret);
    }

    public boolean clearFilter() {
        Object ret = executeApiOverlayScript(
                "return window.__seleniumApiModal && window.__seleniumApiModal.clearFilter();"
        );
        return Boolean.TRUE.equals(ret);
    }

    public void setAutoCloseMs(long okMs, long errMs) {
        try {
            executeApiOverlayScript(
                    "window.__seleniumApiModal.setAutoCloseMs(arguments[0], arguments[1]);",
                    okMs, errMs
            );
        } catch (Exception ignored) {}
    }

    public void setDelayAutoCloseUntilSearch(boolean on) {
        try {
            executeApiOverlayScript(
                    "window.__seleniumApiModal.setDelayAutoCloseUntilSearch(arguments[0]);",
                    on
            );
        } catch (Exception ignored) {}
    }

    private Object executeApiOverlayScript(String script, Object... args) {
        rootManager.ensureRootExists();
        ApiOverlayJs.inject(scriptExecutor);
        return scriptExecutor.execute(script, args);
    }

    private Object executeApiOverlayAsyncScript(String script, Object... args) {
        rootManager.ensureRootExists();
        ApiOverlayJs.inject(scriptExecutor);
        return scriptExecutor.executeAsync(script, args);
    }
}
