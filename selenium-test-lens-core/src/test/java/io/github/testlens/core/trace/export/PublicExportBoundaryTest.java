package io.github.testlens.core.trace.export;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertFalse;

class PublicExportBoundaryTest {
    @Test
    void htmlEscaperRemainsAnImplementationDetail() {
        assertFalse(Modifier.isPublic(TraceHtmlEscaper.class.getModifiers()));
    }
}
