package io.github.testlens;

/** Visual state shown around an element without changing the element or test outcome. @since 0.3.1 */
public enum HighlightState {
    ACTION,
    WAITING,
    RETRY,
    SUCCESS,
    FAILURE
}
