package utils.jsExecHelper.core.logging;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryLogSinkTest {

    @Test
    void acceptStoresEntriesInOrder() {
        InMemoryLogSink sink = new InMemoryLogSink();
        UiTestLensLogEntry first = UiTestLensLogEntry.info("first");
        UiTestLensLogEntry second = UiTestLensLogEntry.warn("second");

        sink.accept(first);
        sink.accept(second);

        assertEquals(List.of(first, second), sink.entries());
    }

    @Test
    void entriesReturnsImmutableCopy() {
        InMemoryLogSink sink = new InMemoryLogSink();
        sink.accept(UiTestLensLogEntry.info("entry"));

        List<UiTestLensLogEntry> entries = sink.entries();

        assertThrows(UnsupportedOperationException.class, () -> entries.add(UiTestLensLogEntry.info("other")));

        sink.accept(UiTestLensLogEntry.info("new"));
        assertEquals(1, entries.size());
        assertEquals(2, sink.entries().size());
    }

    @Test
    void clearRemovesEntries() {
        InMemoryLogSink sink = new InMemoryLogSink();
        sink.accept(UiTestLensLogEntry.info("entry"));

        sink.clear();

        assertTrue(sink.entries().isEmpty());
    }
}
