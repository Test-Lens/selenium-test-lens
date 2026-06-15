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
    void failFastStepThrowsUiStepError() {
        UiStepScope scope = new UiStepScope(null, null, null);

        UiStepError error = assertThrows(UiStepError.class, () -> scope.run(
                "Verify order summary",
                UiStepOptions.defaults(),
                () -> {
                    throw new AssertionError("Business assertions failed for: Order summary");
                }));

        assertEquals(UiStepStatus.FAILED, error.result().status());
        assertTrue(error.getMessage().contains("Verify order summary"));
        assertTrue(error.getMessage().contains("Business assertions failed for: Order summary"));
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
