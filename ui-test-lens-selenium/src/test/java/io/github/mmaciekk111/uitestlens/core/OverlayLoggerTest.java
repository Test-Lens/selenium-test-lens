package io.github.mmaciekk111.uitestlens.core;

import org.junit.jupiter.api.Test;
import io.github.mmaciekk111.uitestlens.core.logging.InMemoryLogSink;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensEventType;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogLevel;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogger;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OverlayLoggerTest {

    @Test
    void noopDoesNotThrow() {
        OverlayLogger logger = OverlayLogger.noop();

        assertDoesNotThrow(() -> logger.info("info"));
        assertDoesNotThrow(() -> logger.warn("warn"));
        assertDoesNotThrow(() -> logger.error("error"));
        assertDoesNotThrow(() -> logger.emit(UiTestLensLogEntry.info("entry")));
    }

    @Test
    void fromLoggerDelegatesShortcutMethods() {
        InMemoryLogSink sink = new InMemoryLogSink();
        OverlayLogger logger = OverlayLogger.from(UiTestLensLogger.builder().sink(sink).build());

        logger.info("info");
        logger.warn("warn");
        logger.error("error");

        assertEquals(List.of(
                UiTestLensLogLevel.INFO,
                UiTestLensLogLevel.WARN,
                UiTestLensLogLevel.ERROR
        ), sink.entries().stream().map(UiTestLensLogEntry::level).toList());
    }

    @Test
    void emitDelegatesTypedEntry() {
        InMemoryLogSink sink = new InMemoryLogSink();
        OverlayLogger logger = OverlayLogger.from(UiTestLensLogger.builder().sink(sink).build());
        UiTestLensLogEntry entry = UiTestLensLogEntry.builder()
                .eventType(UiTestLensEventType.ACTION)
                .status(UiTestLensStatus.PASSED)
                .message("typed")
                .build();

        logger.emit(entry);

        assertEquals(List.of(entry), sink.entries());
    }

    @Test
    void nullLoggerCreatesNoopBridge() {
        OverlayLogger logger = OverlayLogger.from(null);

        assertDoesNotThrow(() -> logger.info("ignored"));
        assertDoesNotThrow(() -> logger.emit(UiTestLensLogEntry.info("ignored")));
    }
}
