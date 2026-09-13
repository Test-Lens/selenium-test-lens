package io.github.testlens.allure;

/** Outcome of publishing one finalized Test Lens result into the current Allure executable. @since 0.3.0 */
public enum AllureAttachStatus {
    /** Every selected artifact was attached. */
    ATTACHED,
    /** At least one selected artifact was attached and at least one was unavailable or failed. */
    PARTIALLY_ATTACHED,
    /** Allure exposed no current test, fixture, or step. */
    SKIPPED_NO_ACTIVE_CONTEXT,
    /** The same finalization result was already handled in this Allure context. */
    SKIPPED_ALREADY_ATTACHED,
    /** Session outcome or artifact policy selected nothing for publication. */
    SKIPPED_BY_POLICY,
    /** The policy selected artifacts, but none existed as readable regular files. */
    SKIPPED_NO_AVAILABLE_ARTIFACTS,
    /** No selected artifact could be attached because publication failed. */
    FAILED
}
