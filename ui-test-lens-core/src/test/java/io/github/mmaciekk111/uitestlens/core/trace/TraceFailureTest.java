package io.github.mmaciekk111.uitestlens.core.trace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceFailureTest {

    @Test
    void fromThrowableKeepsMessageAndType() {
        IllegalStateException throwable = new IllegalStateException("bad state");

        TraceFailure failure = TraceFailure.from(throwable, false);

        assertEquals("bad state", failure.message());
        assertEquals(IllegalStateException.class.getName(), failure.exceptionType());
        assertTrue(failure.stackTrace().isBlank());
    }

    @Test
    void fromThrowableCanIncludeStackTrace() {
        TraceFailure failure = TraceFailure.from(new RuntimeException("boom"), true);

        assertFalse(failure.stackTrace().isBlank());
    }
}
