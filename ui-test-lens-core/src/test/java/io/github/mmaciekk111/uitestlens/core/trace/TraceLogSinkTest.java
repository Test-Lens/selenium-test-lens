package io.github.mmaciekk111.uitestlens.core.trace;

import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensEventType;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogLevel;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
