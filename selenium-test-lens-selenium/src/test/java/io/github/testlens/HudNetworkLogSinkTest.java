package io.github.testlens;

import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.browser.BrowserScriptExecutor;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.hud.HudPanel;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudNetworkLogSinkTest {
    @Test
    void skipsOnlyRawNetworkEntriesExplicitlyMarkedHidden() {
        RecordingHud hud = new RecordingHud();
        JsOverlayDebug.HudLogSink sink = new JsOverlayDebug.HudLogSink();
        sink.attach(hud, null);

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
        sink.attach(hud, driver);

        sink.accept(entry(UiTestLensEventType.NETWORK_RESPONSE_RECORDED, "hidden", "false"));

        assertEquals(0, switchCalls.get());
        assertEquals(List.of(), hud.messages);
    }

    private static UiTestLensLogEntry entry(UiTestLensEventType type, String message, String hudVisible) {
        UiTestLensLogEntry.Builder builder = UiTestLensLogEntry.builder().eventType(type).message(message);
        if (hudVisible != null) builder.metadata("hudVisible", hudVisible);
        return builder.build();
    }

    private static final class RecordingHud extends HudPanel {
        private static final BrowserScriptExecutor NOOP = (script, arguments) -> null;
        private final List<String> messages = new ArrayList<>();

        private RecordingHud() {
            super(NOOP, new OverlayRootManager(NOOP, OverlayConfig.builder().build()), OverlayConfig.builder().build());
        }

        @Override
        public void appendLog(String level, String message, String timestamp) {
            messages.add(message);
        }
    }
}
