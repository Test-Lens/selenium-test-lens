package utils.jsExecHelper.core.logging;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConsumerLogSinkTest {

    @Test
    void consumerReceivesEntry() {
        List<UiTestLensLogEntry> received = new ArrayList<>();
        ConsumerLogSink sink = new ConsumerLogSink(received::add);
        UiTestLensLogEntry entry = UiTestLensLogEntry.info("entry");

        sink.accept(entry);

        assertEquals(List.of(entry), received);
    }

    @Test
    void nullConsumerIsNoop() {
        ConsumerLogSink sink = new ConsumerLogSink(null);

        assertDoesNotThrow(() -> sink.accept(UiTestLensLogEntry.info("entry")));
        assertDoesNotThrow(() -> sink.accept(null));
    }
}
