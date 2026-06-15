package io.github.testlens.core.logging;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CompositeLogSinkTest {

    @Test
    void forwardsEntryToAllSinks() {
        List<UiTestLensLogEntry> first = new ArrayList<>();
        List<UiTestLensLogEntry> second = new ArrayList<>();
        CompositeLogSink sink = CompositeLogSink.of(first::add, second::add);
        UiTestLensLogEntry entry = UiTestLensLogEntry.info("event");

        sink.accept(entry);

        assertEquals(List.of(entry), first);
        assertEquals(List.of(entry), second);
    }

    @Test
    void ignoresNullSinksAndEntries() {
        List<UiTestLensLogEntry> entries = new ArrayList<>();
        CompositeLogSink sink = CompositeLogSink.of(null, entries::add);

        assertDoesNotThrow(() -> sink.accept(null));
        sink.accept(UiTestLensLogEntry.info("event"));

        assertEquals(1, entries.size());
    }

    @Test
    void failingSinkDoesNotBlockNextSink() {
        List<UiTestLensLogEntry> entries = new ArrayList<>();
        CompositeLogSink sink = CompositeLogSink.of(
                entry -> {
                    throw new IllegalStateException("sink failed");
                },
                entries::add
        );

        assertDoesNotThrow(() -> sink.accept(UiTestLensLogEntry.info("event")));

        assertEquals(1, entries.size());
    }
}
