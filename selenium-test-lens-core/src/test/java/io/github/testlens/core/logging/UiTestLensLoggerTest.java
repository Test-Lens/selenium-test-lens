package io.github.testlens.core.logging;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UiTestLensLoggerTest {

    @Test
    void noopDoesNotThrow() {
        UiTestLensLogger logger = UiTestLensLogger.noop();

        assertDoesNotThrow(() -> logger.info("info"));
        assertDoesNotThrow(() -> logger.warn("warn"));
        assertDoesNotThrow(() -> logger.error("error"));
        assertDoesNotThrow(() -> logger.emit(UiTestLensLogEntry.info("entry")));
        assertDoesNotThrow(() -> logger.emit(null));
    }

    @Test
    void loggerWithMemorySinkStoresShortcutEntriesInOrder() {
        InMemoryLogSink sink = new InMemoryLogSink();
        UiTestLensLogger logger = UiTestLensLogger.builder().sink(sink).build();

        logger.info("info");
        logger.warn("warn");
        logger.error("error");

        assertEquals(List.of(
                UiTestLensLogLevel.INFO,
                UiTestLensLogLevel.WARN,
                UiTestLensLogLevel.ERROR
        ), sink.entries().stream().map(UiTestLensLogEntry::level).toList());
        assertEquals(List.of("info", "warn", "error"), sink.entries().stream().map(UiTestLensLogEntry::message).toList());
    }

    @Test
    void loggerEmitsSameEntryToMultipleSinks() {
        InMemoryLogSink first = new InMemoryLogSink();
        InMemoryLogSink second = new InMemoryLogSink();
        UiTestLensLogger logger = UiTestLensLogger.builder()
                .sink(first)
                .sink(second)
                .build();
        UiTestLensLogEntry entry = UiTestLensLogEntry.info("same");

        logger.emit(entry);

        assertSame(entry, first.entries().get(0));
        assertSame(entry, second.entries().get(0));
    }

    @Test
    void failingSinkDoesNotBlockNextSink() {
        InMemoryLogSink goodSink = new InMemoryLogSink();
        UiTestLensLogger logger = UiTestLensLogger.builder()
                .sink(entry -> {
                    throw new IllegalStateException("sink failed");
                })
                .sink(goodSink)
                .build();

        assertDoesNotThrow(() -> logger.info("survives"));

        assertEquals(1, goodSink.entries().size());
        assertEquals("survives", goodSink.entries().get(0).message());
    }

    @Test
    void withSinkReturnsNewLoggerAndDoesNotMutateOldOne() {
        InMemoryLogSink first = new InMemoryLogSink();
        InMemoryLogSink second = new InMemoryLogSink();
        UiTestLensLogger original = UiTestLensLogger.builder().sink(first).build();
        UiTestLensLogger extended = original.withSink(second);

        original.info("original");
        extended.info("extended");

        assertEquals(List.of("original", "extended"), first.entries().stream().map(UiTestLensLogEntry::message).toList());
        assertEquals(List.of("extended"), second.entries().stream().map(UiTestLensLogEntry::message).toList());
    }

    @Test
    void builderIgnoresNullSink() {
        List<UiTestLensLogEntry> received = new ArrayList<>();
        UiTestLensLogger logger = UiTestLensLogger.builder()
                .sink(null)
                .sink(received::add)
                .build();

        logger.info("entry");

        assertEquals(1, received.size());
    }
}
