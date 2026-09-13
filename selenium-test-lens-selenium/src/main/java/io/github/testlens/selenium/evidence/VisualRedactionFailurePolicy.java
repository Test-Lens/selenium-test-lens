package io.github.testlens.selenium.evidence;

/** Controls whether a mask problem prevents screenshot publication. */
public enum VisualRedactionFailurePolicy {
    /** Captures available evidence and reports mask problems in the capture result. */
    BEST_EFFORT,
    /** Refuses to publish a screenshot when every required mask cannot be confirmed. */
    STRICT
}
