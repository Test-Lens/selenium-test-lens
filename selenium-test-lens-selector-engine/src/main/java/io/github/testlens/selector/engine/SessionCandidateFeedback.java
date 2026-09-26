package io.github.testlens.selector.engine;

import java.util.Objects;

/** Session-local user feedback; never changes selector facts or persisted policy. */
public record SessionCandidateFeedback(String analysisId, String candidateId, State state, boolean ambiguityWarning) {
    public SessionCandidateFeedback {
        if (analysisId == null || analysisId.isBlank()) throw new IllegalArgumentException("analysisId is required");
        if (candidateId == null || candidateId.isBlank()) throw new IllegalArgumentException("candidateId is required");
        Objects.requireNonNull(state, "state");
    }

    public enum State { USER_ACCEPTED_ONCE }
}
