package io.github.testlens.selenium.business;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessAssertionErrorTest {

    @Test
    void summaryContainsSubjectAndFailedChecks() {
        BusinessAssertionFailure first = new BusinessAssertionFailure(
                "Order summary", "shows total", "Expected text: 123.00 PLN", null, "", Duration.ofMillis(4));
        BusinessAssertionFailure second = new BusinessAssertionFailure(
                "Order summary", "contains product", "Element not found", null, "", Duration.ofMillis(5));
        List<BusinessAssertionResult> results = List.of(
                BusinessAssertionResult.failed("Order summary", "shows total", first, Duration.ofMillis(4)),
                BusinessAssertionResult.failed("Order summary", "contains product", second, Duration.ofMillis(5))
        );

        BusinessAssertionError error = new BusinessAssertionError("Order summary", results, BusinessAssertionOptions.defaults());

        assertEquals("Order summary", error.subject());
        assertEquals(2, error.results().size());
        assertTrue(error.getMessage().contains("Business assertions failed for: Order summary"));
        assertTrue(error.getMessage().contains("1. shows total"));
        assertTrue(error.getMessage().contains("2. contains product"));
    }
}
