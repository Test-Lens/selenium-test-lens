package io.github.testlens.core.trace;

import io.github.testlens.core.logging.*;
import io.github.testlens.core.logging.export.HtmlLogExporter;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MeasuredOperationDurationAuditTest {
    @Test void measuredTerminalOperationSurvivesJsonAndBothHtmlPaths() {
        for (UiTestLensEventType type : List.of(UiTestLensEventType.ACTION, UiTestLensEventType.LOCATOR_ACTION_PASSED,
                UiTestLensEventType.ASSERTION_PASSED, UiTestLensEventType.STEP_PASSED)) {
            var entry = entry(type, UiTestLensStatus.PASSED, "53", true);
            var session = UiTestLensSession.start("duration audit");
            new TraceLogSink(session).accept(entry);
            var event = session.events().get(session.events().size() - 1);
            assertEquals(Duration.ofMillis(53), event.duration(), type.name());
            assertTrue(session.exportJson().contains("\"durationMs\":53"));
            assertTrue(session.exportHtml().contains(">53 ms<"));
            assertTrue(new HtmlLogExporter().export(List.of(entry)).contains(">53 ms<"));
        }
    }
    @Test void nanosRemainExactAndMillisAreTruncatedWithoutSyntheticMinimum() {
        var session = UiTestLensSession.start("precision");
        var sink = new TraceLogSink(session);
        sink.accept(entry(UiTestLensEventType.ACTION, UiTestLensStatus.PASSED, "0", true).toBuilder()
                .metadata("durationNanos", "999999").build());
        assertEquals(Duration.ofNanos(999999), session.events().get(1).duration());
    }
    @Test void descriptionsAndAnimationSettingsDoNotBecomeElapsedTime() {
        var session = UiTestLensSession.start("descriptive");
        var sink = new TraceLogSink(session);
        sink.accept(entry(UiTestLensEventType.ACTION, UiTestLensStatus.STARTED, "800", true));
        sink.accept(entry(UiTestLensEventType.ACTION, UiTestLensStatus.PASSED, "800", false));
        sink.accept(entry(UiTestLensEventType.ACTION, UiTestLensStatus.INFO, "800", true));
        for (var event : session.events()) assertEquals(Duration.ZERO, event.duration());
    }
    @Test void malformedDurationDoesNotBreakTrace() {
        var session = UiTestLensSession.start("bad duration");
        var sink = new TraceLogSink(session);
        for (String value : List.of("-3", "wrong", "9223372036854775808"))
            sink.accept(entry(UiTestLensEventType.ACTION, UiTestLensStatus.PASSED, value, true));
        for (var event : session.events()) assertEquals(Duration.ZERO, event.duration());
    }
    private static UiTestLensLogEntry entry(UiTestLensEventType type, UiTestLensStatus status, String millis, boolean measured) {
        var builder = UiTestLensLogEntry.builder().eventType(type).status(status).action("audit")
                .metadata("durationMs", millis);
        if (measured) builder.metadata("operationId", "measured-1");
        return builder.build();
    }
}
