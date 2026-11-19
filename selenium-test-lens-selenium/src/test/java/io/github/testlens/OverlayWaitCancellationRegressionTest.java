package io.github.testlens;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class OverlayWaitCancellationRegressionTest {
    @Test
    void canceledWaitMustNotExecuteConditionEvenWhenItWouldSucceedImmediately() {
        AtomicInteger evaluations = new AtomicInteger();
        OverlayWait wait = new OverlayWait(null, Duration.ofSeconds(1), null);
        Thread.currentThread().interrupt();
        try {
            assertThrows(WebDriverException.class, () -> wait.until(driver -> {
                evaluations.incrementAndGet();
                return true;
            }, "canceled condition"));
            assertEquals(0, evaluations.get());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
