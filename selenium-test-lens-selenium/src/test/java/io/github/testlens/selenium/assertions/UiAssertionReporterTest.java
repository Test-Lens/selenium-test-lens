package io.github.testlens.selenium.assertions;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.InMemoryLogSink;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogger;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UiAssertionReporterTest {

    @Test
    void emitsAssertionEvents() {
        InMemoryLogSink sink = new InMemoryLogSink();
        UiAssertionReporter reporter = new UiAssertionReporter(OverlayLogger.from(UiTestLensLogger.builder()
                .sink(sink)
                .build()));

        reporter.started("toBeVisible", "Save button");
        reporter.retry("toBeVisible", "Save button", 2, "visible", "hidden");
        reporter.passed(UiAssertionResult.passed("toBeVisible", "Save button", "", "visible", 1,
                Duration.ofMillis(5), "Element is visible"));

        assertEquals(UiTestLensEventType.ASSERTION_STARTED, sink.entries().get(0).eventType());
        assertEquals(UiTestLensEventType.ASSERTION_RETRY, sink.entries().get(1).eventType());
        assertEquals(UiTestLensEventType.ASSERTION_PASSED, sink.entries().get(2).eventType());
        String operationId = sink.entries().get(0).metadata().get("operationId");
        assertFalse(operationId.isBlank());
        sink.entries().forEach(entry -> assertEquals(operationId, entry.metadata().get("operationId")));
        assertFalse(sink.entries().get(2).metadata().get("durationMs").isBlank());
    }
}

