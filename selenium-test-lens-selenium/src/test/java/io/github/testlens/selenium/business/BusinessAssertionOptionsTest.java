package io.github.testlens.selenium.business;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessAssertionOptionsTest {

    @Test
    void defaultsCollectFailuresWithoutStackTraces() {
        BusinessAssertionOptions options = BusinessAssertionOptions.defaults();

        assertTrue(options.collectFailures());
        assertFalse(options.failFast());
        assertFalse(options.includeStackTrace());
        assertEquals(500, options.messagePreviewLimit());
    }

    @Test
    void validatesPreviewLimit() {
        assertThrows(IllegalArgumentException.class, () -> BusinessAssertionOptions.builder()
                .messagePreviewLimit(-1)
                .build());
    }
}
