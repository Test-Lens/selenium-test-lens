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
    record ExecutionResult(Status status, int tests, List<String> boundedEvidence) {
        public ExecutionResult(boolean successful, int tests, List<String> boundedEvidence) {
            this(successful ? Status.PASS : Status.FAIL, tests, boundedEvidence);
        }
        public ExecutionResult {
            if (status == null) throw new IllegalArgumentException("status is required");
            if (tests < 0) throw new IllegalArgumentException("tests must not be negative");
            boundedEvidence = List.copyOf(boundedEvidence == null ? List.of() : boundedEvidence);
        }
        public boolean successful() { return status == Status.PASS; }
    }
    enum Status { PASS, FAIL, TIMED_OUT }
}
