package io.github.testlens.core.trace;

import java.util.Objects;

/** Failure raised after reports are written when retry outcome policy rejects an otherwise passed session. */
public final class RetryPolicyViolationException extends RuntimeException {
    private final RetryOutcomePolicy policy;
    private final RetrySummary retrySummary;

    public RetryPolicyViolationException(RetryOutcomePolicy policy, RetrySummary retrySummary) {
        super(message(policy, retrySummary));
        this.policy = Objects.requireNonNull(policy, "policy");
        this.retrySummary = Objects.requireNonNull(retrySummary, "retrySummary");
    }

    public RetryOutcomePolicy policy() {
        return policy;
    }

    public RetrySummary retrySummary() {
        return retrySummary;
    }

    private static String message(RetryOutcomePolicy policy, RetrySummary summary) {
        Objects.requireNonNull(summary, "retrySummary");
        return "Retry outcome policy " + Objects.requireNonNull(policy, "policy")
                + " rejected a passed session: retries=" + summary.totalRetries()
                + ", timeLost=" + summary.timeLost().toMillis() + "ms";
    }
}
