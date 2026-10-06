package io.github.testlens.application.tooling.ai.workflow;

import java.time.Duration;
import java.util.List;

/** Injected boundary for targeted execution; implementations own the actual runner. @since 0.5.0 */
@FunctionalInterface
public interface TargetedTestExecutor {
    ExecutionResult execute(ExecutionRequest request) throws Exception;

    record ExecutionRequest(String runId, String testClass, List<String> selectors, Duration timeout) {
        public ExecutionRequest {
            if (runId == null || runId.isBlank() || testClass == null || testClass.isBlank()) throw new IllegalArgumentException("runId and testClass are required");
            selectors = List.copyOf(selectors == null ? List.of() : selectors);
            if (timeout == null || timeout.isNegative() || timeout.isZero()) throw new IllegalArgumentException("positive timeout is required");
        }
    }
    record ExecutionResult(boolean successful, int tests, List<String> boundedEvidence) {
        public ExecutionResult { boundedEvidence = List.copyOf(boundedEvidence == null ? List.of() : boundedEvidence); }
    }
}
