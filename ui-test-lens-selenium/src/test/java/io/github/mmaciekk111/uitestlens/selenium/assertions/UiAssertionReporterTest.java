package io.github.mmaciekk111.uitestlens.selenium.assertions;

import io.github.mmaciekk111.uitestlens.core.OverlayLogger;
import io.github.mmaciekk111.uitestlens.core.logging.InMemoryLogSink;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensEventType;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogger;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiAssertionReporterTest {

    @Test
    void emitsAssertionEvents() {
        InMemoryLogSink sink = new InMemoryLogSink();
        UiAssertionReporter reporter = new UiAssertionReporter(OverlayLogger.from(UiTestLensLogger.builder()
                .sink(sink)
                .build()));

        reporter.started("toBeVisible", "Save button");
        reporter.passed(UiAssertionResult.passed("toBeVisible", "Save button", "", "visible", 1,
                Duration.ofMillis(5), "Element is visible"));

        assertEquals(UiTestLensEventType.ASSERTION_STARTED, sink.entries().get(0).eventType());
        assertEquals(UiTestLensEventType.ASSERTION_PASSED, sink.entries().get(1).eventType());
    }
}
