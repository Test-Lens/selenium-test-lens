package io.github.testlens.application.tooling.ai.workflow;

import io.github.testlens.application.tooling.ai.AgentContextPack;
import io.github.testlens.application.tooling.ai.CodeReviewResult;
import io.github.testlens.application.tooling.ai.FailureClassification;
import io.github.testlens.application.tooling.ai.RepairProposal;
import io.github.testlens.application.tooling.ai.TestExecutionResult;
import io.github.testlens.application.tooling.ai.TestImplementationProposal;
import io.github.testlens.application.tooling.ai.TestPlan;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Immutable provenance envelope for an agent artifact. @since 0.5.0 */
public record ArtifactEnvelope<T>(String artifactId, String runId, List<String> parents, int attempt,
                                  String payloadDigest, T payload) {
    public ArtifactEnvelope {
        if (blank(artifactId) || blank(runId)) throw new IllegalArgumentException("artifactId and runId are required");
        parents = List.copyOf(parents == null ? List.of() : parents);
        if (attempt < 1) throw new IllegalArgumentException("attempt must be positive");
        payload = snapshotPayload(payload);
        String actual = digest(payload);
        if (!actual.equals(payloadDigest)) throw new IllegalArgumentException("payload digest does not match payload");
    }

    public static <T> ArtifactEnvelope<T> create(String artifactId, String runId, List<String> parents,
                                                  int attempt, T payload) {
        T snapshot = snapshotPayload(payload);
        return new ArtifactEnvelope<>(artifactId, runId, parents, attempt, digest(snapshot), snapshot);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T payload() {
        return payload instanceof byte[] value ? (T) value.clone() : payload;
    }

    public static String digest(Object payload) {
        Objects.requireNonNull(payload, "payload");
        byte[] bytes = payload instanceof byte[] value ? value : Objects.toString(payload).getBytes(StandardCharsets.UTF_8);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T snapshotPayload(T payload) {
        Objects.requireNonNull(payload, "payload");
        if (payload instanceof byte[] value) return (T) value.clone();
        if (isSupportedImmutablePayload(payload)) return payload;
        throw new IllegalArgumentException("unsupported payload type for immutable envelope: "
                + payload.getClass().getName());
    }

    private static boolean isSupportedImmutablePayload(Object payload) {
        return payload instanceof String
                || payload instanceof Boolean
                || payload instanceof Byte
                || payload instanceof Short
                || payload instanceof Integer
                || payload instanceof Long
                || payload instanceof Float
                || payload instanceof Double
                || payload instanceof Character
                || payload instanceof AgentContextPack
                || payload instanceof TestPlan
                || payload instanceof AgentWorkflowCoordinator.CompilationFeedback
                || payload instanceof PageObjectCapabilityMissing
                || payload instanceof PageObjectExtensionProposal
                || payload instanceof TestImplementationProposal
                || payload instanceof TargetedJavaCompiler.CompilationResult
                || payload instanceof TestExecutionResult
                || payload instanceof CodeReviewResult
                || payload instanceof FailureClassification
                || payload instanceof RepairProposal;
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
