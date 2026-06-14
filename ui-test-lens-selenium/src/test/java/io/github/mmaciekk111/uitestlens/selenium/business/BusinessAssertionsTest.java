package io.github.mmaciekk111.uitestlens.selenium.business;

import io.github.mmaciekk111.uitestlens.selenium.assertions.UiAssertionFailureReason;
import io.github.mmaciekk111.uitestlens.selenium.assertions.UiAssertionResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessAssertionsTest {

    @Test
    void groupPassesWhenAllChecksPass() {
        BusinessAssertionResult result = new BusinessAssertions("Order summary", BusinessAssertionOptions.defaults(), null)
                .check("shows total", () -> {
                })
                .check("contains product", () -> UiAssertionResult.passed(
                        "toContainText", "Product", "Premium", "Premium", 1, Duration.ofMillis(2), "matched"))
                .verify();

        assertTrue(result.isPassed());
    }

    @Test
    void collectsMultipleFailuresByDefault() {
        BusinessAssertions group = new BusinessAssertions("Order summary", BusinessAssertionOptions.defaults(), null)
                .check("shows total", () -> {
                    throw new AssertionError("wrong total");
                })
                .check("contains product", () -> {
                    throw new IllegalStateException("missing product");
                });

        BusinessAssertionError error = assertThrows(BusinessAssertionError.class, group::verify);

        assertEquals(2, group.results().size());
        assertEquals(2, error.results().stream()
                .filter(result -> result.status() == BusinessAssertionStatus.FAILED)
                .count());
        assertTrue(error.getMessage().contains("wrong total"));
        assertTrue(error.getMessage().contains("missing product"));
    }

    @Test
    void failFastStopsAfterFirstFailure() {
        AtomicInteger calls = new AtomicInteger();
        BusinessAssertionOptions options = BusinessAssertionOptions.builder()
                .failFast(true)
                .build();
        BusinessAssertions group = new BusinessAssertions("Order summary", options, null)
                .check("first", () -> {
                    calls.incrementAndGet();
                    throw new AssertionError("first failed");
                })
                .check("second", () -> {
                    calls.incrementAndGet();
                });

        assertThrows(BusinessAssertionError.class, group::verify);

        assertEquals(1, calls.get());
        assertEquals(1, group.results().size());
    }

    @Test
    void wrapsUiAssertionFailureResult() {
        BusinessAssertions group = new BusinessAssertions("Order summary", BusinessAssertionOptions.defaults(), null)
                .check("shows total", () -> UiAssertionResult.failed(
                        "toHaveText",
                        UiAssertionFailureReason.TEXT_MISMATCH,
                        "Total",
                        "123.00 PLN",
                        "120.00 PLN",
                        1,
                        Duration.ofMillis(3),
                        "Text mismatch"));

        BusinessAssertionError error = assertThrows(BusinessAssertionError.class, group::verify);

        assertTrue(error.getMessage().contains("TEXT_MISMATCH"));
    }

    @Test
    void unexpectedRuntimeExceptionIsReported() {
        BusinessAssertions group = new BusinessAssertions("Order summary", BusinessAssertionOptions.defaults(), null)
                .check("contains product", () -> {
                    throw new IllegalArgumentException("bad adapter");
                });

        BusinessAssertionError error = assertThrows(BusinessAssertionError.class, group::verify);

        assertTrue(error.getMessage().contains("bad adapter"));
    }
}
