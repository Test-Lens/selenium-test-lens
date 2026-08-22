package io.github.testlens.selenium.steps;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiStepScopeTest {

    @Test
    void passedStepReturnsResultAndWritesHudHooks() {
        List<String> labels = new ArrayList<>();
        List<String> hud = new ArrayList<>();
        UiStepScope scope = new UiStepScope(null, labels::add, hud::add);

        UiStepResult result = scope.run("Fill checkout form", UiStepOptions.defaults(), () -> {
        });

        assertEquals(UiStepStatus.PASSED, result.status());
        assertEquals(List.of("Fill checkout form"), labels);
        assertEquals(List.of("Step passed: Fill checkout form"), hud);
        assertTrue(result.elapsed().toNanos() >= 0);
    }

    @Test
    void failFastStepPreservesOriginalAssertionError() {
        UiStepScope scope = new UiStepScope(null, null, null);
        AssertionError original = new AssertionError("Business assertions failed for: Order summary");

        AssertionError error = assertThrows(AssertionError.class, () -> scope.run(
                "Verify order summary",
                UiStepOptions.defaults(),
                () -> { throw original; }));

        assertTrue(error == original);
    }

    @Test
    void failFastStepPreservesOriginalRuntimeException() {
        UiStepScope scope = new UiStepScope(null, null, null);
        org.openqa.selenium.TimeoutException original = new org.openqa.selenium.TimeoutException("timeout");
        assertTrue(assertThrows(org.openqa.selenium.TimeoutException.class,
                () -> scope.run("wait", UiStepOptions.defaults(), () -> { throw original; })) == original);
    }

    @Test
    void failFastStepPreservesOriginalNoSuchElementException() {
        UiStepScope scope = new UiStepScope(null, null, null);
        org.openqa.selenium.NoSuchElementException original = new org.openqa.selenium.NoSuchElementException("missing");
        assertTrue(assertThrows(org.openqa.selenium.NoSuchElementException.class,
                () -> scope.run("locate", UiStepOptions.defaults(), () -> { throw original; })) == original);
    }

    @Test
    void failFastStepPreservesSameGenericRuntimeException() {
        UiStepScope scope = new UiStepScope(null, null, null);
        RuntimeException original = new RuntimeException("business failure");
        assertTrue(assertThrows(RuntimeException.class,
                () -> scope.run("business step", UiStepOptions.defaults(), () -> { throw original; })) == original);
    }

    @Test
    void nonFailFastStepReturnsFailedResult() {
        UiStepScope scope = new UiStepScope(null, null, null);
        UiStepOptions options = UiStepOptions.builder()
                .failFast(false)
                .build();

        UiStepResult result = scope.run("Verify order summary", options, () -> {
            throw new IllegalStateException("bad summary");
        });

        assertEquals(UiStepStatus.FAILED, result.status());
        assertEquals("bad summary", result.failure().message());
    }

    @Test
    void rejectsBlankStepName() {
        UiStepScope scope = new UiStepScope(null, null, null);

        assertThrows(IllegalArgumentException.class, () -> scope.run(" ", UiStepOptions.defaults(), () -> {
        }));
    }
}

