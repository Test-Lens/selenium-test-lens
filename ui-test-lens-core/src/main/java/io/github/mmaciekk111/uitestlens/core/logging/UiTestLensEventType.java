package io.github.mmaciekk111.uitestlens.core.logging;

public enum UiTestLensEventType {
    GENERAL,
    STEP,
    ACTION,
    WAIT,
    ASSERTION,
    HUD,
    OVERLAY,
    OVERLAY_POLICY_STARTED,
    OVERLAY_DETECTED,
    OVERLAY_ACTION_STARTED,
    OVERLAY_ACTION_PASSED,
    OVERLAY_ACTION_FAILED,
    OVERLAY_HANDLED,
    OVERLAY_STILL_VISIBLE,
    HIGHLIGHT,
    API,
    REACT,
    CLEANUP,
    ERROR
}
