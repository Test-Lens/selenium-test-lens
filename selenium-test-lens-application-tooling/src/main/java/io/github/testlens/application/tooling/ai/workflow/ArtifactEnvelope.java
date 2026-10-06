package io.github.testlens.application.tooling.ai.workflow;

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
        Objects.requireNonNull(payload, "payload");
        String actual = digest(payload);
        if (!actual.equals(payloadDigest)) throw new IllegalArgumentException("payload digest does not match payload");
    }

    public static <T> ArtifactEnvelope<T> create(String artifactId, String runId, List<String> parents,
                                                  int attempt, T payload) {
        return new ArtifactEnvelope<>(artifactId, runId, parents, attempt, digest(payload), payload);
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

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
