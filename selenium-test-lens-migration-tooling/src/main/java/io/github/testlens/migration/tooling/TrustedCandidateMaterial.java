package io.github.testlens.migration.tooling;

import java.util.List;
import java.util.Objects;

/** Explicit trusted local candidate input; it is never reconstructed from report display text. */
public record TrustedCandidateMaterial(String declarationRef, String candidateId, String strategy, String canonicalValue,
                                       String origin, ValidationState validationState, TargetComparison targetComparison,
                                       Intent intent, int matchCount, boolean contextCompatible,
                                       boolean policyConflict, boolean policyEvidenceConflict,
                                       List<String> limitations, String analysisDigest) {
    public TrustedCandidateMaterial {
        Objects.requireNonNull(declarationRef); Objects.requireNonNull(candidateId); Objects.requireNonNull(strategy);
        Objects.requireNonNull(canonicalValue); Objects.requireNonNull(origin); Objects.requireNonNull(validationState);
        Objects.requireNonNull(targetComparison); Objects.requireNonNull(intent);
        limitations = List.copyOf(limitations == null ? List.of() : limitations);
        if (!analysisDigest.matches("sha256:[0-9a-f]{64}")) throw new IllegalArgumentException("analysisDigest");
        if (canonicalValue.length() > 65_536 || canonicalValue.indexOf('\0') >= 0) throw new IllegalArgumentException("candidate value");
    }
    public enum ValidationState { VERIFIED_IN_SCOPE, VALID_FOR_INTENT, VALID_BUT_AMBIGUOUS, WRONG_TARGET, NO_MATCH, INVALID_SELECTOR, STALE_TARGET, TARGET_UNAVAILABLE, CONTEXT_UNAVAILABLE, UNSUPPORTED, SESSION_LOST, NOT_LIVE_VALIDATED }
    public enum TargetComparison { SAME_TARGET, DIFFERENT_TARGET, TARGET_UNAVAILABLE, STALE_TARGET, UNKNOWN }
    public enum Intent { FIND_ONE, FIND_MANY, UNKNOWN }
}
