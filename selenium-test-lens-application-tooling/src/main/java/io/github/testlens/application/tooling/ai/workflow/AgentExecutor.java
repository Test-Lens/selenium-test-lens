package io.github.testlens.application.tooling.ai.workflow;

import io.github.testlens.application.tooling.ai.CodeReviewResult;
import io.github.testlens.application.tooling.ai.FailureClassification;
import io.github.testlens.application.tooling.ai.RepairProposal;
import io.github.testlens.application.tooling.ai.TestImplementationProposal;
import io.github.testlens.application.tooling.ai.TestExecutionResult;
import io.github.testlens.application.tooling.ai.TestPlan;

import java.util.List;
import java.util.Map;

/** Boundary for an agent provider; workflow code has no SDK or network dependency. @since 0.5.0 */
@FunctionalInterface
public interface AgentExecutor {
    AgentResult execute(AgentCommand command) throws AgentExecutionException;

    record AgentCommand(String runId, Role role, List<ArtifactEnvelope<?>> inputs,
                        Map<String, String> instructions) {
        public AgentCommand {
            if (runId == null || runId.isBlank() || role == null) throw new IllegalArgumentException("runId and role are required");
            inputs = List.copyOf(inputs == null ? List.of() : inputs);
            instructions = Map.copyOf(instructions == null ? Map.of() : instructions);
        }
    }

    record AgentResult(String providerRequestId, ArtifactEnvelope<?> artifact) {
        public AgentResult {
            if (artifact == null || !structured(artifact.payload())) throw new IllegalArgumentException("A versioned structured agent artifact is required");
        }
        private static boolean structured(Object value){return value instanceof TestPlan||value instanceof TestImplementationProposal||value instanceof TestExecutionResult||value instanceof FailureClassification||value instanceof RepairProposal||value instanceof CodeReviewResult||value instanceof PageObjectCapabilityMissing||value instanceof PageObjectExtensionProposal;}
    }

    enum Role { PLANNER, IMPLEMENTER, FAILURE_CLASSIFIER, REPAIRER, REVIEWER,
        TEST_ARCHITECT, SCENARIO_DESIGNER, TEST_IMPLEMENTER, TEST_VERIFIER, STABILIZER, CODE_REVIEWER, UNIT_TEST_AGENT }

    final class AgentExecutionException extends Exception {
        public AgentExecutionException(String message) { super(message); }
        public AgentExecutionException(String message, Throwable cause) { super(message, cause); }
    }
}
