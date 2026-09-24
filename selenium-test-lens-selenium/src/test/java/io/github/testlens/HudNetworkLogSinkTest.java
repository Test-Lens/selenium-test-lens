package io.github.testlens;

import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.browser.BrowserScriptExecutor;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
