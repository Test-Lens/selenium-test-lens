package io.github.testlens.core.trace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TraceRetentionOptionsTest {
    @Test
    void defaultsAreBoundedAndRetainPassedTrace() {
        TraceRetentionOptions first = TraceRetentionOptions.defaults();
        TraceRetentionOptions second = TraceRetentionOptions.defaults();
        assertEquals(4_096, first.maxEvents());
        assertEquals(8L * 1024 * 1024, first.maxBytes());
        assertEquals(256L * 1024, first.maxEventBytes());
        assertEquals(PassedTraceRetention.RETAIN_TRACE, first.passedSessionRetention());
        assertNotSame(first, second);
    }

    @Test
    void validatesEveryBoundAndPolicy() {
        assertThrows(IllegalArgumentException.class, () -> TraceRetentionOptions.builder().maxEvents(0).build());
        assertThrows(IllegalArgumentException.class, () -> TraceRetentionOptions.builder().maxBytes(0).build());
        assertThrows(IllegalArgumentException.class, () -> TraceRetentionOptions.builder().maxEventBytes(0).build());
        assertThrows(IllegalArgumentException.class, () -> TraceRetentionOptions.builder()
                .maxBytes(10).maxEventBytes(11).build());
        assertThrows(NullPointerException.class, () -> TraceRetentionOptions.builder().passedSessionRetention(null));
    }
}
