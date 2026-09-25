package io.github.testlens;

import io.github.testlens.core.HudPanelJs;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.browser.BrowserScriptExecutor;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.hud.HudOptions;
import io.github.testlens.hud.HudPanel;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.UnhandledAlertException;

/** Internal structured-event bridge; the public HudPanel API remains unchanged. */
final class SemanticHudPanel extends HudPanel {
    private final BrowserScriptExecutor executor;
    private final OverlayConfig config;
    private String testName = "-";
    private String pipelineId = "-";

    SemanticHudPanel(BrowserScriptExecutor executor, OverlayRootManager rootManager, OverlayConfig config) {
        super(executor, rootManager, config);
        this.executor = executor;
        this.config = config;
    }

    @Override
    public void init(String testName, String pipelineId) {
        this.testName = blankToDash(testName);
        this.pipelineId = blankToDash(pipelineId);
        super.init(this.testName, this.pipelineId);
    }

    AppendResult appendSemantic(UiTestLensLogEntry entry,
                                String message,
                                String sourceLabel,
                                String navigationTarget,
                                HudEventSemantics semantics) {
        if (entry == null || semantics == null) {
            return AppendResult.SKIPPED;
        }
        return appendSemanticBatch(List.of(new SemanticEntry(entry, message, sourceLabel,
                navigationTarget, semantics)));
    }

    AppendResult appendSemanticBatch(List<SemanticEntry> entries) {
        if (!config.isEnabled() || !config.isShowHudPanel() || entries == null || entries.isEmpty()) {
            return AppendResult.SKIPPED;
        }
        List<Map<String, Object>> payload = new ArrayList<>(entries.size());
        for (SemanticEntry item : entries) {
            if (item == null || item.entry() == null || item.semantics() == null) continue;
            payload.add(payload(item));
        }
        if (payload.isEmpty()) return AppendResult.SKIPPED;
        try {
            Object appended = append(payload);
            if (!Boolean.TRUE.equals(appended)) {
                // A navigation replaces the document and its JS runtime. Reinstall only on that cold path.
                super.init(testName, pipelineId);
                appended = append(payload);
            }
            return Boolean.TRUE.equals(appended) ? AppendResult.APPENDED : AppendResult.SKIPPED;
        } catch (UnhandledAlertException alert) {
            return AppendResult.DEFERRED_BY_ALERT;
        } catch (RuntimeException ignored) {
            // The HUD remains best-effort and cannot alter the browser operation.
            return AppendResult.SKIPPED;
        }
    }

    private Object append(List<Map<String, Object>> payload) {
        return executor.execute(HudPanelJs.bridgeScript()
                            + "/* test-lens:hud-semantic-batch */"
                            + "if (!hud || !hud.logBatch || !window.__seleniumOverlayRoot) { return false; }"
                            + "hud.logBatch(arguments[0]); return true;",
                    payload);
    }

    private Map<String, Object> payload(SemanticEntry item) {
        UiTestLensLogEntry entry = item.entry();
        Instant timestamp = entry.timestamp() == null ? Instant.now() : entry.timestamp();
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("message", item.message());
        value.put("level", entry.level().name().toLowerCase(Locale.ROOT));
        value.put("timestamp", timestamp.toString());
        value.put("eventType", entry.eventType().name());
        value.put("sourceLabel", item.sourceLabel());
        value.put("navigationTarget", item.navigationTarget());
        value.put("presentationTimestamp", formatTimestamp(timestamp, config.getHudOptions()));
        value.put("semantics", item.semantics().toBrowserMap(entry.level().name()));
        return value;
    }

    record SemanticEntry(UiTestLensLogEntry entry, String message, String sourceLabel,
                         String navigationTarget, HudEventSemantics semantics) { }

    enum AppendResult { APPENDED, DEFERRED_BY_ALERT, SKIPPED }

    private static String formatTimestamp(Instant instant, HudOptions options) {
        var zone = options.usesSystemTimestampZone()
                ? options.timestampFormat() == io.github.testlens.hud.HudTimestampFormat.ISO_UTC
                    ? ZoneOffset.UTC : options.effectiveTimestampZone()
                : options.effectiveTimestampZone();
        return DateTimeFormatter.ofPattern(options.effectiveTimestampPattern(), Locale.ROOT)
                .withZone(zone).format(instant);
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
