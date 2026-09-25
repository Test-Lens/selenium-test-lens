package io.github.testlens;

import io.github.testlens.core.trace.PassedTraceRetention;
import io.github.testlens.core.trace.TraceRetentionOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestLensOptionsTraceRetentionTest {
    @Test
    void exposesConfiguredRetentionAndNullRestoresDefaults() {
        TraceRetentionOptions configured = TraceRetentionOptions.builder()
                .maxEvents(12).maxBytes(4_096).maxEventBytes(1_024)
                .passedSessionRetention(PassedTraceRetention.SUMMARY_ONLY).build();
        TestLensOptions options = TestLensOptions.builder().traceRetention(configured).build();
        TestLensOptions reset = TestLensOptions.builder().traceRetention(configured).traceRetention(null).build();

        assertEquals(12, options.traceRetention().maxEvents());
        assertEquals(PassedTraceRetention.SUMMARY_ONLY, options.traceRetention().passedSessionRetention());
        assertEquals(TraceRetentionOptions.defaults().maxEvents(), reset.traceRetention().maxEvents());
    }
}
