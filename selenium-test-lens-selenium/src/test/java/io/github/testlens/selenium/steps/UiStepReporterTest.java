package io.github.testlens.selenium.steps;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.InMemoryLogSink;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogger;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiStepReporterTest {

    @Test
    void emitsStepEvents() {
        InMemoryLogSink sink = new InMemoryLogSink();
        UiStepReporter reporter = new UiStepReporter(OverlayLogger.from(UiTestLensLogger.builder()
                .sink(sink)
                .build()));

        reporter.started("Save order", UiStepOptions.defaults());
        reporter.finished(UiStepResult.passed("Save order", Instant.now(), Instant.now()), UiStepOptions.defaults());

        assertEquals(UiTestLensEventType.STEP_STARTED, sink.entries().get(0).eventType());
        assertEquals(UiTestLensEventType.STEP_PASSED, sink.entries().get(1).eventType());
    }
}

