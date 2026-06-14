package io.github.mmaciekk111.uitestlens.core.trace;

import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensEventType;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogLevel;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensStatus;
import io.github.mmaciekk111.uitestlens.core.logging.TargetDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TraceLogSinkTest {

    @Test
    void convertsLogEntryToTraceEvent() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");
        TraceLogSink sink = new TraceLogSink(session);

        sink.accept(UiTestLensLogEntry.builder()
                .level(UiTestLensLogLevel.INFO)
                .eventType(UiTestLensEventType.STEP_PASSED)
                .status(UiTestLensStatus.PASSED)
                .step("Save form")
                .message("Step passed")
                .metadata("attempt", "1")
                .build());

        TraceEvent event = session.events().get(session.events().size() - 1);
        assertEquals(TraceEventType.STEP_PASSED, event.type());
        assertEquals(TraceStatus.PASSED, event.status());
        assertEquals("Save form", event.name());
        assertEquals("1", event.attributes().get("metadata.attempt"));
    }

    @Test
    void mapsLocatorActionAndResolveEvents() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");
        TraceLogSink sink = new TraceLogSink(session);

        sink.accept(entry(UiTestLensEventType.LOCATOR_RESOLVE_PASSED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO));
        sink.accept(entry(UiTestLensEventType.LOCATOR_ACTION_FAILED, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR));

        assertEquals(TraceEventType.LOCATOR_RESOLVE, session.events().get(session.events().size() - 2).type());
        assertEquals(TraceEventType.ACTION_FAILED, session.events().get(session.events().size() - 1).type());
        assertEquals(TraceStatus.FAILED, session.events().get(session.events().size() - 1).status());
    }

    @Test
    void mapsOverlayActionabilityNetworkAndArtifacts() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");
        TraceLogSink sink = new TraceLogSink(session);

        sink.accept(entry(UiTestLensEventType.OVERLAY_DETECTED, UiTestLensStatus.WARN, UiTestLensLogLevel.WARN));
        sink.accept(entry(UiTestLensEventType.ACTIONABILITY_CHECK_FAILED, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR));
        sink.accept(entry(UiTestLensEventType.NETWORK_RESPONSE_RECORDED, UiTestLensStatus.INFO, UiTestLensLogLevel.INFO));
        sink.accept(entry(UiTestLensEventType.NETWORK_WAIT_TIMED_OUT, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR));
        sink.accept(entry(UiTestLensEventType.SCREENSHOT_CAPTURE_PASSED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO));
        sink.accept(entry(UiTestLensEventType.VIDEO_ATTACHED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO));

        int size = session.events().size();
        assertEquals(TraceEventType.OVERLAY_DETECTED, session.events().get(size - 6).type());
        assertEquals(TraceEventType.ACTIONABILITY_CHECK, session.events().get(size - 5).type());
        assertEquals(TraceEventType.NETWORK_EVENT, session.events().get(size - 4).type());
        assertEquals(TraceEventType.NETWORK_WAIT, session.events().get(size - 3).type());
        assertEquals(TraceEventType.SCREENSHOT, session.events().get(size - 2).type());
        assertEquals(TraceEventType.VIDEO, session.events().get(size - 1).type());
    }

    @Test
    void preservesTargetAttributesAndThrowableFailure() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");
        TraceLogSink sink = new TraceLogSink(session);

        sink.accept(UiTestLensLogEntry.builder()
                .eventType(UiTestLensEventType.ASSERTION_FAILED)
                .status(UiTestLensStatus.FAILED)
                .level(UiTestLensLogLevel.ERROR)
                .message("Assertion failed")
                .target(TargetDescriptor.selector("[data-testid='toast']").withMetadata("locator", "toast"))
                .throwable(new IllegalStateException("bad toast"))
                .build());

        TraceEvent event = session.events().get(session.events().size() - 1);
        assertEquals(TraceEventType.ASSERTION_FAILED, event.type());
        assertEquals("[data-testid='toast']", event.attributes().get("target.selector"));
        assertEquals("toast", event.attributes().get("target.metadata.locator"));
        assertEquals("ASSERTION_FAILED", event.attributes().get("uiEventType"));
        assertNotNull(event.failure());
        assertEquals("java.lang.IllegalStateException", event.failure().exceptionType());
    }

    @Test
    void mapsUnknownEventToCustomWithUiEventAttribute() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");
        TraceLogSink sink = new TraceLogSink(session);

        sink.accept(entry(UiTestLensEventType.HUD, UiTestLensStatus.INFO, UiTestLensLogLevel.INFO));

        TraceEvent event = session.events().get(session.events().size() - 1);
        assertEquals(TraceEventType.CUSTOM, event.type());
        assertEquals("HUD", event.attributes().get("uiEventType"));
    }

    private static UiTestLensLogEntry entry(UiTestLensEventType eventType,
                                            UiTestLensStatus status,
                                            UiTestLensLogLevel level) {
        return UiTestLensLogEntry.builder()
                .eventType(eventType)
                .status(status)
                .level(level)
                .message(eventType.name())
                .action(eventType.name().toLowerCase())
                .build();
    }
}
