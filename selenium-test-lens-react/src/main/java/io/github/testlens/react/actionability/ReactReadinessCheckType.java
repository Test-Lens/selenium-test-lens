package io.github.testlens.react.actionability;

public enum ReactReadinessCheckType {
    ARIA_DISABLED,
    ARIA_BUSY,
    DATA_LOADING,
    DATA_PENDING,
    PROGRESSBAR_PRESENT,
    SPINNER_PRESENT,
    SKELETON_PRESENT,
    FOCUS_LOCK_ACTIVE,
    DIALOG_OR_MODAL_ACTIVE,
    STALE_AFTER_RESOLVE,
    BASE_ACTIONABILITY
}
