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
        if (!config.isEnabled() || !config.isShowHudPanel() || entry == null || semantics == null) {
            return AppendResult.SKIPPED;
        }
        try {
            Instant timestamp = entry.timestamp() == null ? Instant.now() : entry.timestamp();
            Object appended = append(timestamp, entry, message, sourceLabel, navigationTarget, semantics);
            if (!Boolean.TRUE.equals(appended)) {
                // A navigation replaces the document and its JS runtime. Reinstall only on that cold path.
                super.init(testName, pipelineId);
                appended = append(timestamp, entry, message, sourceLabel, navigationTarget, semantics);
            }
            return Boolean.TRUE.equals(appended) ? AppendResult.APPENDED : AppendResult.SKIPPED;
        } catch (UnhandledAlertException alert) {
            return AppendResult.DEFERRED_BY_ALERT;
        } catch (RuntimeException ignored) {
            // The HUD remains best-effort and cannot alter the browser operation.
            return AppendResult.SKIPPED;
        }
    }

    private Object append(Instant timestamp,
                          UiTestLensLogEntry entry,
                          String message,
                          String sourceLabel,
                          String navigationTarget,
                          HudEventSemantics semantics) {
        return executor.execute(HudPanelJs.bridgeScript()
                            + "if (!hud || !hud.log || !window.__seleniumOverlayRoot) { return false; }"
                            + "hud.log(arguments[0], arguments[1], arguments[2], arguments[3],"
                            + " arguments[4], arguments[5], arguments[6], arguments[7]); return true;",
                    message,
                    entry.level().name().toLowerCase(Locale.ROOT),
                    timestamp.toString(),
                    entry.eventType().name(),
                    sourceLabel,
                    navigationTarget,
                    formatTimestamp(timestamp, config.getHudOptions()),
                    semantics.toBrowserMap(entry.level().name()));
    }

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
