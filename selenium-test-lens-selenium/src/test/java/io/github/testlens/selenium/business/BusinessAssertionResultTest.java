package io.github.testlens.selenium.business;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessAssertionResultTest {

    @Test
    void passedFactoryCreatesReadableResult() {
        BusinessAssertionResult result = BusinessAssertionResult.passed("Order summary", "shows total", Duration.ofMillis(15));

        assertTrue(result.isPassed());
        assertEquals(BusinessAssertionStatus.PASSED, result.status());
        assertEquals("Order summary", result.subject());
        assertEquals("shows total", result.description());
        assertNull(result.failure());
        assertTrue(result.summary().contains("shows total PASSED"));
    }

    @Test
    void failedFactoryCarriesFailure() {
        BusinessAssertionFailure failure = new BusinessAssertionFailure(
                "Order summary", "shows total", "Expected total", null, "assertion summary", Duration.ofMillis(10));

        BusinessAssertionResult result = BusinessAssertionResult.failed("Order summary", "shows total", failure, Duration.ofMillis(10));

        assertEquals(BusinessAssertionStatus.FAILED, result.status());
        assertEquals(failure, result.failure());
        assertEquals("Expected total", result.message());
    }
}

