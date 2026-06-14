package io.github.mmaciekk111.uitestlens.core.trace;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TraceEventTest {

    @Test
    void startedEventGetsIdAndStartedStatus() {
        TraceEvent event = TraceEvent.started(TraceEventType.STEP_STARTED, "Save form");

        assertFalse(event.id().isBlank());
        assertEquals(TraceEventType.STEP_STARTED, event.type());
        assertEquals(TraceStatus.STARTED, event.status());
    }

    @Test
    void failedEventIncludesFailure() {
        RuntimeException cause = new RuntimeException("boom");

        TraceEvent event = TraceEvent.failed(TraceEventType.ACTION_FAILED, "Click save", cause, Duration.ofMillis(25));

        assertEquals(TraceStatus.FAILED, event.status());
        assertNotNull(event.failure());
        assertEquals("boom", event.failure().message());
        assertEquals(Duration.ofMillis(25), event.duration());
    }

    @Test
    void builderAddsAttributesAndArtifacts() {
        TraceArtifact artifact = TraceArtifact.screenshot("screen", java.nio.file.Path.of("target/screen.png"));

        TraceEvent event = TraceEvent.builder(TraceEventType.ARTIFACT_ATTACHED, TraceStatus.INFO, "screen")
                .attribute("key", "value")
                .artifact(artifact)
                .build();

        assertEquals("value", event.attributes().get("key"));
        assertEquals(1, event.artifacts().size());
    }
}
