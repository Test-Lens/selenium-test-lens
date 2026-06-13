package io.github.mmaciekk111.uitestlens.core.logging;

import org.junit.jupiter.api.Test;
import io.github.mmaciekk111.uitestlens.core.logging.export.PlainTextLogExporter;

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

    @Test
    void exportDelegatesToExporterAndKeepsEntries() {
        InMemoryLogSink sink = new InMemoryLogSink();
        sink.accept(UiTestLensLogEntry.info("entry"));

        String text = sink.export(new PlainTextLogExporter());

        assertTrue(text.contains("entry"));
        assertEquals(1, sink.entries().size());
    }

    @Test
    void exportRejectsNullExporter() {
        InMemoryLogSink sink = new InMemoryLogSink();

        assertThrows(IllegalArgumentException.class, () -> sink.export(null));
    }

    @Test
    void convenienceExportsReturnStringsAndKeepEntries() {
        InMemoryLogSink sink = new InMemoryLogSink();
        sink.accept(UiTestLensLogEntry.info("entry"));

        assertTrue(sink.exportAsText().contains("entry"));
        assertTrue(sink.exportAsJson().contains("\"message\""));
        assertTrue(sink.exportAsHtml().contains("<html"));
        assertEquals(1, sink.entries().size());
    }
}
