package io.github.testlens.core.trace;

/** Controls how recovery retries affect an otherwise passed session. */
public enum RetryOutcomePolicy {
    REPORT_ONLY,
    WARN,
    FAIL_AFTER_N,
    FAIL_ON_ANY_RETRY
}
