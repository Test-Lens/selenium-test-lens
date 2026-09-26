package io.github.testlens;

import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.browser.BrowserScriptExecutor;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensStatus;
import io.github.testlens.hud.HudPanel;
import io.github.testlens.hud.HudOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;

import java.time.Instant;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudNetworkLogSinkTest {
    @TempDir Path temp;

    @Test
    void disabledSourceNavigationDoesNotCreateOrInvokeResolutionPipeline() {
        SourceFileResolver.resetMetrics();
        IdeNavigationUriProvider.resetMetrics();
        RecordingHud hud = new RecordingHud();
        JsOverlayDebug.HudLogSink sink = new JsOverlayDebug.HudLogSink();

        sink.attach(hud, null, HudOptions.defaults());
        sink.accept(UiTestLensLogEntry.builder().eventType(UiTestLensEventType.ACTION).message("disabled").build());

        assertEquals(0, SourceFileResolver.createdCount());
        assertEquals(0, SourceFileResolver.resolutionCount());
        assertEquals(0, SourceFileResolver.discoveryCount());
        assertEquals(0, IdeNavigationUriProvider.targetInvocationCount());
    }

    @Test
    void skipsOnlyRawNetworkEntriesExplicitlyMarkedHidden() {
        RecordingHud hud = new RecordingHud();
        JsOverlayDebug.HudLogSink sink = new JsOverlayDebug.HudLogSink();
        sink.attach(hud, null, io.github.testlens.hud.HudOptions.defaults());

        sink.accept(entry(UiTestLensEventType.NETWORK_REQUEST_RECORDED, "hidden", "false"));
        sink.accept(entry(UiTestLensEventType.NETWORK_RESPONSE_RECORDED, "visible", "true"));
        sink.accept(entry(UiTestLensEventType.NETWORK_FAILURE_RECORDED, "legacy", null));
        sink.accept(entry(UiTestLensEventType.NETWORK_WAIT_STARTED, "control", "false"));
        sink.accept(entry(UiTestLensEventType.ACTION, "ordinary", "false"));

        assertEquals(List.of("visible", "legacy", "control", "ordinary"), hud.messages);
    }

    @Test
    void hiddenRawEntryIsDiscardedBeforeAlertProbeAndCannotBeDeferred() {
        AtomicInteger switchCalls = new AtomicInteger();
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) -> {
                    if (method.getName().equals("switchTo")) switchCalls.incrementAndGet();
                    return null;
                });
        RecordingHud hud = new RecordingHud();
        JsOverlayDebug.HudLogSink sink = new JsOverlayDebug.HudLogSink();
        sink.attach(hud, driver, io.github.testlens.hud.HudOptions.defaults());

        sink.accept(entry(UiTestLensEventType.NETWORK_RESPONSE_RECORDED, "hidden", "false"));

        assertEquals(0, switchCalls.get());
        assertEquals(List.of(), hud.messages);
    }

    @Test
    void disabledHudRenderingDoesNotProbeTheBrowser() {
        AtomicInteger calls = new AtomicInteger();
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) -> {
                    calls.incrementAndGet();
                    return null;
                });
        RecordingHud hud = new RecordingHud();
        JsOverlayDebug.HudLogSink sink = new JsOverlayDebug.HudLogSink();
        sink.attach(hud, driver, HudOptions.defaults(), false);

        sink.accept(entry(UiTestLensEventType.ACTION, "invisible", null));

        assertEquals(0, calls.get());
        assertEquals(List.of(), hud.messages);
    }

    @Test
    void failedAlertProbeDoesNotBuildADeferredBacklog() {
        AtomicBoolean probeFails = new AtomicBoolean(true);
        WebDriver.TargetLocator target = (WebDriver.TargetLocator) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{WebDriver.TargetLocator.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("alert")) {
                        if (probeFails.get()) throw new IllegalStateException("session unavailable");
                        throw new NoAlertPresentException();
                    }
                    return null;
                });
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) ->
                        method.getName().equals("switchTo") ? target : null);
        RecordingHud hud = new RecordingHud();
        JsOverlayDebug.HudLogSink sink = new JsOverlayDebug.HudLogSink();
        sink.attach(hud, driver, HudOptions.defaults());

        sink.accept(entry(UiTestLensEventType.ACTION, "unavailable", null));
        probeFails.set(false);
        sink.accept(entry(UiTestLensEventType.ACTION, "current", null));

        assertEquals(List.of("current"), hud.messages);
    }

    @Test
    void productVisibilityFiltersAreAppliedBeforeTheInternalHudBridge() {
        RecordingHud hud = new RecordingHud();
        JsOverlayDebug.HudLogSink sink = new JsOverlayDebug.HudLogSink();
        sink.attach(hud, null, HudOptions.builder().showNetwork(false).showRetries(false)
                .showWaits(false).showAssertions(false).build());

        sink.accept(entry(UiTestLensEventType.NETWORK_RESPONSE_RECORDED, "network", null));
        sink.accept(entry(UiTestLensEventType.LOCATOR_RETRY, "retry", null));
        sink.accept(entry(UiTestLensEventType.WAIT, "wait", null));
        sink.accept(entry(UiTestLensEventType.ASSERTION_PASSED, "assertion", null));
        sink.accept(entry(UiTestLensEventType.ACTION, "action", null));

        assertEquals(List.of("action"), hud.messages);
    }

    @Test
    void allVisibleLogFamiliesKeepTheirEventTimestampExactlyOnce() {
        RecordingHud hud = new RecordingHud();
        JsOverlayDebug.HudLogSink sink = new JsOverlayDebug.HudLogSink();
        sink.attach(hud, null, HudOptions.builder().showNetwork(true).showRetries(true)
                .showWaits(true).showAssertions(true).build());
        Instant eventTime = Instant.parse("2026-07-15T22:00:00.123456789Z");
        List<UiTestLensEventType> visibleFamilies = List.of(
                UiTestLensEventType.STEP, UiTestLensEventType.ACTION,
                UiTestLensEventType.WAIT, UiTestLensEventType.LOCATOR_RETRY,
                UiTestLensEventType.ASSERTION_PASSED, UiTestLensEventType.NETWORK_WAIT_STARTED,
                UiTestLensEventType.NETWORK_RESPONSE_RECORDED, UiTestLensEventType.AUTH_STATE_CREATED,
                UiTestLensEventType.SCREENSHOT_CAPTURE_PASSED, UiTestLensEventType.OVERLAY_DETECTED,
                UiTestLensEventType.ERROR);

        visibleFamilies.forEach(type -> sink.accept(UiTestLensLogEntry.builder()
                .timestamp(eventTime).eventType(type).message(type.name()).build()));

        assertEquals(visibleFamilies.size(), hud.messages.size());
        assertEquals(visibleFamilies.size(), hud.timestamps.size());
        hud.timestamps.forEach(value -> assertEquals(eventTime.toString(), value));
    }

    @Test
    void standardHidesSuccessfulTechnicalNoiseAndDebugShowsIt() {
        RecordingHud standardHud = new RecordingHud();
        JsOverlayDebug.HudLogSink standard = new JsOverlayDebug.HudLogSink();
        standard.attach(standardHud, null, HudOptions.builder().preset(io.github.testlens.hud.HudPreset.STANDARD).build());
        standard.accept(UiTestLensLogEntry.builder().eventType(UiTestLensEventType.LOCATOR_RESOLVE_PASSED)
                .status(io.github.testlens.core.logging.UiTestLensStatus.PASSED).message("resolved").build());
        standard.accept(UiTestLensLogEntry.builder().eventType(UiTestLensEventType.HIGHLIGHT)
                .status(io.github.testlens.core.logging.UiTestLensStatus.INFO).message("rendered").build());
        standard.accept(UiTestLensLogEntry.builder().eventType(UiTestLensEventType.ASSERTION_RETRY)
                .status(io.github.testlens.core.logging.UiTestLensStatus.WARN).message("poll mismatch")
                .metadata("retryKind", "poll").build());
        assertEquals(List.of(), standardHud.messages);

        RecordingHud debugHud = new RecordingHud();
        JsOverlayDebug.HudLogSink debug = new JsOverlayDebug.HudLogSink();
        debug.attach(debugHud, null, HudOptions.builder().preset(io.github.testlens.hud.HudPreset.DEBUG).build());
        debug.accept(UiTestLensLogEntry.builder().eventType(UiTestLensEventType.LOCATOR_RESOLVE_PASSED)
                .status(io.github.testlens.core.logging.UiTestLensStatus.PASSED).message("resolved").build());
        debug.accept(UiTestLensLogEntry.builder().eventType(UiTestLensEventType.HIGHLIGHT)
                .status(io.github.testlens.core.logging.UiTestLensStatus.INFO).message("rendered").build());
        debug.accept(UiTestLensLogEntry.builder().eventType(UiTestLensEventType.ASSERTION_RETRY)
                .status(io.github.testlens.core.logging.UiTestLensStatus.WARN).message("poll mismatch")
                .metadata("retryKind", "poll").build());
        assertEquals(List.of("resolved", "rendered"), debugHud.messages);
    }

    @Test
    void entryDeferredByAlertKeepsOriginalTimestampAndIsNotDuplicated() {
        AtomicBoolean alertOpen = new AtomicBoolean(true);
        Alert alert = (Alert) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Alert.class},
                (proxy, method, args) -> null);
        WebDriver.TargetLocator target = (WebDriver.TargetLocator) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{WebDriver.TargetLocator.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("alert")) {
                        if (alertOpen.get()) return alert;
                        throw new NoAlertPresentException();
                    }
                    return null;
                });
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) ->
                        method.getName().equals("switchTo") ? target : null);
        RecordingHud hud = new RecordingHud();
        JsOverlayDebug.HudLogSink sink = new JsOverlayDebug.HudLogSink();
        sink.attach(hud, driver, HudOptions.defaults());
        Instant original = Instant.parse("2026-01-15T10:20:30.123456789Z");

        sink.accept(UiTestLensLogEntry.builder().timestamp(original)
                .eventType(UiTestLensEventType.ACTION).message("deferred").build());
        assertEquals(List.of(), hud.messages);
        alertOpen.set(false);
        sink.accept(UiTestLensLogEntry.builder().timestamp(original.plusSeconds(1))
                .eventType(UiTestLensEventType.ACTION).message("current").build());

        assertEquals(List.of("deferred", "current"), hud.messages);
        assertEquals(List.of(original.toString(), original.plusSeconds(1).toString()), hud.timestamps);
    }

    @Test
    void operationDiagnosticsAndTerminalResultShareOneSafeDispatch() {
        AtomicInteger probes = new AtomicInteger();
        WebDriver.TargetLocator target = (WebDriver.TargetLocator) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{WebDriver.TargetLocator.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("alert")) {
                        probes.incrementAndGet();
                        throw new NoAlertPresentException();
                    }
                    return null;
                });
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) ->
                        method.getName().equals("switchTo") ? target : null);
        BatchRecordingExecutor executor = new BatchRecordingExecutor();
        OverlayConfig config = OverlayConfig.builder().build();
        SemanticHudPanel hud = new SemanticHudPanel(executor, new OverlayRootManager(executor, config), config);
        hud.init("batch", "local");
        executor.batches.clear();
        JsOverlayDebug.HudLogSink sink = new JsOverlayDebug.HudLogSink();
        sink.attach(hud, driver, HudOptions.builder().preset(io.github.testlens.hud.HudPreset.DEBUG).build());

        sink.accept(operation(UiTestLensEventType.LOCATOR_ACTION_STARTED,
                io.github.testlens.core.logging.UiTestLensStatus.STARTED, "start", "operation-1"));
        sink.accept(operation(UiTestLensEventType.LOCATOR_RESOLVE_STARTED,
                io.github.testlens.core.logging.UiTestLensStatus.STARTED, "resolve", "operation-1"));
        sink.accept(operation(UiTestLensEventType.LOCATOR_RESOLVE_PASSED,
                io.github.testlens.core.logging.UiTestLensStatus.PASSED, "resolved", "operation-1"));
        sink.accept(operation(UiTestLensEventType.HIGHLIGHT,
                io.github.testlens.core.logging.UiTestLensStatus.INFO, "outlined", "highlight-1"));
        sink.accept(operation(UiTestLensEventType.LOCATOR_ACTION_PASSED,
                io.github.testlens.core.logging.UiTestLensStatus.PASSED, "passed", "operation-1"));

        assertEquals(2, probes.get(), "RUNNING and terminal flush each use one alert probe");
        assertEquals(2, executor.batches.size());
        assertEquals(List.of("start"), executor.batches.get(0));
        assertEquals(List.of("resolve", "resolved", "outlined", "passed"), executor.batches.get(1));
    }

    @Test
    void targetEnrichmentRevisesTheSameActionRowWithoutEmptySegments() {
        BatchRecordingExecutor executor = new BatchRecordingExecutor();
        OverlayConfig config = OverlayConfig.builder().build();
        SemanticHudPanel hud = new SemanticHudPanel(executor, new OverlayRootManager(executor, config), config);
        hud.init("labels", "local");
        executor.batches.clear();
        executor.operationIds.clear();
        JsOverlayDebug.HudLogSink sink = new JsOverlayDebug.HudLogSink();
        sink.attach(hud, null, HudOptions.defaults());

        sink.accept(actionTarget(UiTestLensStatus.STARTED, "operation-label", null, "#save", false));
        sink.accept(actionTarget(UiTestLensStatus.STARTED, "operation-label", "Zapisz", "#save", true));
        sink.accept(actionTarget(UiTestLensStatus.PASSED, "operation-label", null, "#save", false));

        assertEquals(List.of(List.of("Click :: #save"), List.of("Click :: Zapisz :: #save"),
                List.of("Click :: Zapisz :: #save")), executor.batches);
        assertEquals(List.of("operation-label", "operation-label", "operation-label"),
                executor.operationIds.stream().flatMap(List::stream).toList());
    }

    @Test
    void lastUserMessageFlushesImmediatelyAndIsNeverMergedByText() {
        BatchRecordingExecutor executor = new BatchRecordingExecutor();
        OverlayConfig config = OverlayConfig.builder().build();
        SemanticHudPanel hud = new SemanticHudPanel(executor, new OverlayRootManager(executor, config), config);
        hud.init("batch", "local");
        executor.batches.clear();
        JsOverlayDebug.HudLogSink sink = new JsOverlayDebug.HudLogSink();
        sink.attach(hud, null, HudOptions.defaults());
        UiTestLensLogEntry duplicate = UiTestLensLogEntry.builder().eventType(UiTestLensEventType.HUD)
                .message("same \uD83D\uDE80 <script>alert(1)</script>").build();

        sink.accept(duplicate);
        sink.accept(duplicate);

        assertEquals(2, executor.batches.size());
        assertEquals(List.of(duplicate.message()), executor.batches.get(0));
        assertEquals(List.of(duplicate.message()), executor.batches.get(1));
    }

    @Test
    void newLensSessionCannotFlushThePreviousSessionsPendingDiagnostics() {
        BatchRecordingExecutor executor = new BatchRecordingExecutor();
        OverlayConfig config = OverlayConfig.builder().build();
        SemanticHudPanel hud = new SemanticHudPanel(executor, new OverlayRootManager(executor, config), config);
        hud.init("batch", "local");
        executor.batches.clear();
        JsOverlayDebug.HudLogSink sink = new JsOverlayDebug.HudLogSink();
        sink.attach(hud, null, HudOptions.builder().preset(io.github.testlens.hud.HudPreset.DEBUG).build());
        sink.beginSession();
        sink.accept(operation(UiTestLensEventType.LOCATOR_ACTION_STARTED,
                io.github.testlens.core.logging.UiTestLensStatus.STARTED, "old start", "old"));
        sink.accept(operation(UiTestLensEventType.LOCATOR_RESOLVE_PASSED,
                io.github.testlens.core.logging.UiTestLensStatus.PASSED, "old diagnostic", "old"));

        sink.beginSession();
        sink.accept(operation(UiTestLensEventType.LOCATOR_ACTION_STARTED,
                io.github.testlens.core.logging.UiTestLensStatus.STARTED, "new start", "new"));
        sink.accept(operation(UiTestLensEventType.LOCATOR_ACTION_PASSED,
                io.github.testlens.core.logging.UiTestLensStatus.PASSED, "new passed", "new"));

        assertTrue(executor.batches.stream().noneMatch(batch -> batch.contains("old diagnostic")));
        assertEquals(List.of("new start"), executor.batches.get(1));
        assertEquals(List.of("new passed"), executor.batches.get(2));
    }

    @Test
    void alertBacklogIsBoundedByTheHudRetentionLimit() {
        AtomicBoolean alertOpen = new AtomicBoolean(true);
        Alert alert = (Alert) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Alert.class},
                (proxy, method, args) -> null);
        WebDriver.TargetLocator target = (WebDriver.TargetLocator) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{WebDriver.TargetLocator.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("alert")) {
                        if (alertOpen.get()) return alert;
                        throw new NoAlertPresentException();
                    }
                    return null;
                });
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) ->
                        method.getName().equals("switchTo") ? target : null);
        RecordingHud hud = new RecordingHud();
        JsOverlayDebug.HudLogSink sink = new JsOverlayDebug.HudLogSink();
        sink.attach(hud, driver, HudOptions.defaults());
        for (int index = 0; index < 300; index++) {
            sink.accept(entry(UiTestLensEventType.ACTION, "deferred-" + index, null));
        }

        alertOpen.set(false);
        sink.accept(entry(UiTestLensEventType.ACTION, "current", null));

        assertTrue(hud.messages.size() <= 251);
        assertEquals("current", hud.messages.get(hud.messages.size() - 1));
        assertTrue(hud.messages.contains("deferred-299"));
        assertTrue(!hud.messages.contains("deferred-0"));
    }

    @Test
    void actionableCompatibilityIsLoggedAndPublishedOnce() {
        JetBrainsEnvironment environment = new JetBrainsEnvironment(Optional.of(new JetBrainsIdeInstallation(
                "IntelliJ IDEA", SemanticVersion.parse("2025.2.5"), "252.28238.7",
                Path.of("D:/JetBrains/idea64.exe"))), ProbeResult.ABSENT, ProbeResult.ABSENT,
                ProbeResult.ABSENT, "test");
        JsOverlayDebug.HudLogSink sink = new JsOverlayDebug.HudLogSink(
                new SourceNavigationCompatibilityProbe(() -> environment));
        RecordingHud hud = new RecordingHud();
        HudOptions options = HudOptions.builder().sourceNavigation(io.github.testlens.hud.SourceNavigationOptions
                .builder().enabled(true).intellijProject("Test Project", temp).build()).build();
        Logger logger = Logger.getLogger("io.github.testlens.source-navigation");
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) { records.add(record); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.addHandler(handler);
        try {
            sink.attach(hud, null, options);
            sink.accept(entry(UiTestLensEventType.ACTION, "first", null));
            sink.accept(entry(UiTestLensEventType.ACTION, "second", null));
        } finally {
            logger.removeHandler(handler);
        }

        assertEquals(1, records.stream().filter(record -> record.getLevel() == Level.WARNING).count());
        assertEquals(1, hud.messages.stream().filter(message -> message.contains("Toolbox 3.3+")).count());
        assertEquals(List.of("first", "second"), hud.messages.stream()
                .filter(message -> !message.contains("Toolbox 3.3+")).toList());
    }

    private static UiTestLensLogEntry entry(UiTestLensEventType type, String message, String hudVisible) {
        UiTestLensLogEntry.Builder builder = UiTestLensLogEntry.builder().eventType(type).message(message);
        if (hudVisible != null) builder.metadata("hudVisible", hudVisible);
        return builder.build();
    }

    private static UiTestLensLogEntry operation(UiTestLensEventType type,
                                                io.github.testlens.core.logging.UiTestLensStatus status,
                                                String message, String operationId) {
        return UiTestLensLogEntry.builder().eventType(type).status(status).message(message)
                .metadata("operationId", operationId).build();
    }

    private static UiTestLensLogEntry actionTarget(UiTestLensStatus status, String operationId,
                                                    String label, String selector, boolean update) {
        UiTestLensLogEntry.Builder builder = UiTestLensLogEntry.builder()
                .eventType(UiTestLensEventType.LOCATOR_ACTION_STARTED)
                .status(status)
                .action("locator.click")
                .message("click")
                .target(new io.github.testlens.core.logging.TargetDescriptor(selector, label, null, null, Map.of()))
                .metadata("operationId", operationId);
        if (update) builder.metadata("testlens.internal.hud.operationTargetUpdate", "true");
        return builder.build();
    }

    private static final class BatchRecordingExecutor implements BrowserScriptExecutor {
        private final List<List<String>> batches = new ArrayList<>();
        private final List<List<String>> operationIds = new ArrayList<>();

        @Override
        public Object execute(String script, Object... arguments) {
            if (!script.contains("test-lens:hud-semantic-batch")) return null;
            @SuppressWarnings("unchecked")
            List<java.util.Map<String, Object>> payload =
                    (List<java.util.Map<String, Object>>) arguments[0];
            batches.add(payload.stream().map(value -> String.valueOf(value.get("message"))).toList());
            operationIds.add(payload.stream().map(value -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> semantics = (Map<String, Object>) value.get("semantics");
                return String.valueOf(semantics.get("operationId"));
            }).toList());
            return true;
        }
    }

    private static final class RecordingHud extends HudPanel {
        private static final BrowserScriptExecutor NOOP = (script, arguments) -> null;
        private final List<String> messages = new ArrayList<>();
        private final List<String> timestamps = new ArrayList<>();

        private RecordingHud() {
            super(NOOP, new OverlayRootManager(NOOP, OverlayConfig.builder().build()), OverlayConfig.builder().build());
        }

        @Override
        public void appendLog(String level, String message, String timestamp) {
            messages.add(message);
            timestamps.add(timestamp);
        }

        @Override
        public void appendLog(String level, String message, String timestamp, String eventType,
                              String sourceLabel, String navigationTarget) {
            messages.add(message);
            timestamps.add(timestamp);
        }
    }
}
