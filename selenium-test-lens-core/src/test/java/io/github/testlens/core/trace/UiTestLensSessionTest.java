package io.github.testlens.core.trace;

import io.github.testlens.core.redaction.RedactionPolicy;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiTestLensSessionTest {

    @Test
    void startCreatesMetadataAndStartEvent() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");

        assertFalse(session.id().isBlank());
        assertEquals("Checkout flow", session.metadata().name());
        assertEquals(TraceStatus.STARTED, session.metadata().status());
        assertEquals(TraceEventType.SESSION_STARTED, session.events().get(0).type());
    }

    @Test
    void addEventAndAttachArtifacts() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");

        session.addEvent(TraceEvent.info("note", "hello"));
        TraceArtifact screenshot = session.attachScreenshot("Save form", Path.of("target/screenshots/save.png"));
        TraceArtifact video = session.attachVideo("Video", Path.of("target/videos/test.mp4"));

        assertEquals(TraceArtifactType.SCREENSHOT, screenshot.type());
        assertEquals(TraceArtifactType.VIDEO, video.type());
        assertEquals(2, session.artifacts().size());
    }

    @Test
    void finishUpdatesStatus() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");

        session.finishPassed();

        assertEquals(TraceStatus.PASSED, session.metadata().status());
        assertEquals(TraceEventType.SESSION_FINISHED, session.events().get(session.events().size() - 1).type());
    }

    @Test
    void attachNullArtifactIsRejected() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");

        assertThrows(IllegalArgumentException.class, () -> session.attachArtifact(null));
    }

    @Test
    void directEventsArtifactsAndExportsAreAlreadyRedacted() {
        String secret = "trace-canary-293a";
        UiTestLensSession session = UiTestLensSession.start("session " + secret,
                RetryOutcomePolicy.REPORT_ONLY, 0, RedactionPolicy.builder().secret(secret).build());
        session.addEvent(TraceEvent.builder(TraceEventType.CUSTOM, TraceStatus.FAILED, "name " + secret)
                .message("token=" + secret).attribute("authorization", secret)
                .failure(new TraceFailure("failure " + secret, "Example", "stack " + secret,
                        java.util.Map.of("password", secret))).build());
        session.attachArtifact(TraceArtifact.url("artifact " + secret, TraceArtifactType.CUSTOM_URL,
                "https://user:" + secret + "@example.test/a?token=" + secret + "#fragment"));

        String memory = session.metadata() + session.events().toString() + session.artifacts();
        assertFalse(memory.contains(secret));
        assertFalse(session.exportJson().contains(secret));
        assertFalse(session.exportHtml().contains(secret));
        assertTrue(session.exportJson().contains("[REDACTED]"));
    }
}

