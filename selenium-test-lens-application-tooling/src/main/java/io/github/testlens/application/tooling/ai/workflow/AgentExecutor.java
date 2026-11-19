package io.github.testlens.application.tooling.ai.workflow;

import io.github.testlens.application.tooling.ai.CodeReviewResult;
import io.github.testlens.application.tooling.ai.FailureClassification;
import io.github.testlens.application.tooling.ai.RepairProposal;
import io.github.testlens.application.tooling.ai.TestImplementationProposal;
import io.github.testlens.application.tooling.ai.TestExecutionResult;
import io.github.testlens.application.tooling.ai.TestPlan;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Boundary for an agent provider; workflow code has no SDK or network dependency. @since 0.5.1 */
@FunctionalInterface
public interface AgentExecutor {
    AgentResult execute(AgentCommand command) throws AgentExecutionException;

    /** Structured provider request. @since 0.5.1 */
    record AgentCommand(String runId, Role role, List<ArtifactEnvelope<?>> inputs,
                        Map<String, String> instructions) {
        public AgentCommand {
            if (runId == null || runId.isBlank() || role == null) throw new IllegalArgumentException("runId and role are required");
            inputs = List.copyOf(inputs == null ? List.of() : inputs);
            instructions = Map.copyOf(instructions == null ? Map.of() : instructions);
        }
    }

    /** Structured provider response. @since 0.5.1 */
    record AgentResult(String providerRequestId, ArtifactEnvelope<?> artifact) {
        public AgentResult {
            if (artifact == null || !structured(artifact.payload())) throw new IllegalArgumentException("A versioned structured agent artifact is required");
        }
        private static boolean structured(Object value){return value instanceof TestPlan||value instanceof TestImplementationProposal||value instanceof TestExecutionResult||value instanceof FailureClassification||value instanceof RepairProposal||value instanceof CodeReviewResult||value instanceof PageObjectCapabilityMissing||value instanceof PageObjectExtensionProposal;}
    }

    /** Provider-neutral workflow role. @since 0.5.1 */
    enum Role { PLANNER, IMPLEMENTER, FAILURE_CLASSIFIER, REPAIRER, REVIEWER,
        TEST_ARCHITECT, SCENARIO_DESIGNER, TEST_IMPLEMENTER, TEST_VERIFIER, STABILIZER, CODE_REVIEWER, UNIT_TEST_AGENT }

    /** Bounded external-agent failure classification. @since 0.5.1 */
    enum AgentFailureCode {
        AGENT_NOT_AVAILABLE,
        AGENT_TIMEOUT,
        AGENT_PROCESS_FAILED,
        AGENT_OUTPUT_INVALID,
        AGENT_CONTEXT_REJECTED
    }

    /** Bounded, non-sensitive detail about an external-agent boundary failure. @since 0.5.1 */
    record AgentFailureDiagnostic(String reasonCode, String location, Map<String, String> metadata) {
        private static final int MAX_REASON_CODE = 64;
        private static final int MAX_LOCATION = 160;
        private static final int MAX_METADATA_ENTRIES = 32;
        private static final int MAX_METADATA_KEY = 48;
        private static final int MAX_METADATA_VALUE = 160;
        private static final java.util.Set<String> ALLOWED_METADATA_KEYS = java.util.Set.of(
                "role", "profileId", "resultType", "inputBytes", "outputBytes", "durationMillis", "processStatus",
                "exitCode", "validationStage", "ruleId", "problemCategory", "astNodeKind", "receiverCategory",
                "manifestMatch", "fingerprintStatus", "signatureStatus", "manifestOriginStage", "manifestReason",
                "sourceIndexCompleteness", "selectedPageObjectCount", "verifiedClassCount", "verifiedMethodCount",
                "unresolvedClassCount", "constructorStatus", "inheritedApiStatus", "correlationStatus",
                "manifestTruncated", "manifestMaxClasses", "manifestMaxMethods", "manifestMaxCharacters",
                "manifestSourcesChanged");

        public AgentFailureDiagnostic {
            requireToken(reasonCode, MAX_REASON_CODE, "reasonCode");
            requireLocation(location);
            if (metadata == null) metadata = Map.of();
            if (metadata.size() > MAX_METADATA_ENTRIES) {
                throw new IllegalArgumentException("diagnostic metadata exceeds its entry bound");
            }
            TreeMap<String, String> copy = new TreeMap<>();
            metadata.forEach((key, value) -> {
                requireToken(key, MAX_METADATA_KEY, "metadata key");
                requireSafeText(value, MAX_METADATA_VALUE, "metadata value");
                if (!ALLOWED_METADATA_KEYS.contains(key)) {
                    throw new IllegalArgumentException("diagnostic metadata key is not allowed");
                }
                String normalized = key.toLowerCase(java.util.Locale.ROOT);
                if (normalized.contains("secret") || normalized.contains("token") || normalized.contains("password")
                        || normalized.contains("cookie") || normalized.contains("authorization")
                        || normalized.contains("prompt") || normalized.contains("stdout")
                        || normalized.contains("stderr") || normalized.contains("outputcontent")) {
                    throw new IllegalArgumentException("diagnostic metadata keys must be non-sensitive");
                }
                copy.put(key, value);
            });
            metadata = Map.copyOf(copy);
        }

        private static void requireToken(String value, int max, String name) {
            if (value == null || value.isBlank() || value.length() > max
                    || !value.matches("[A-Za-z][A-Za-z0-9_.-]*")) {
                throw new IllegalArgumentException(name + " must be a bounded identifier");
            }
        }

        private static void requireLocation(String value) {
            if (value == null || value.isBlank() || value.length() > MAX_LOCATION
                    || value.chars().anyMatch(character -> !Character.isLetterOrDigit(character)
                    && "_$.[]-".indexOf(character) < 0)) {
                throw new IllegalArgumentException("location must be a bounded structural path");
            }
        }

        private static void requireSafeText(String value, int max, String name) {
            if (value == null || value.isBlank() || value.length() > max
                    || !value.matches("[A-Za-z0-9_.-]+")) {
                throw new IllegalArgumentException(name + " must be bounded single-line text");
            }
        }
    }

    /** Checked failure at the external-agent boundary. @since 0.5.1 */
    final class AgentExecutionException extends Exception {
        private final AgentFailureCode code;
        private final AgentFailureDiagnostic diagnostic;

        public AgentExecutionException(String message) {
            this(AgentFailureCode.AGENT_PROCESS_FAILED, message, null);
        }

        public AgentExecutionException(String message, Throwable cause) {
            this(AgentFailureCode.AGENT_PROCESS_FAILED, message, cause);
        }

        public AgentExecutionException(AgentFailureCode code, String message) {
            this(code, message, null);
        }

        public AgentExecutionException(AgentFailureCode code, String message, Throwable cause) {
            this(code, message, cause, null);
        }

        public AgentExecutionException(AgentFailureCode code, String message, Throwable cause,
                                       AgentFailureDiagnostic diagnostic) {
            super(message, cause);
            this.code = java.util.Objects.requireNonNull(code, "code");
            this.diagnostic = diagnostic;
        }

        public AgentFailureCode code() { return code; }
        public AgentFailureDiagnostic diagnostic() { return diagnostic; }
    }
}
