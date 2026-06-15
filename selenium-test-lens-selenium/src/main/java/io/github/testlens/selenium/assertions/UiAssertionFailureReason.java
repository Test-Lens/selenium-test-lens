package io.github.testlens.selenium.assertions;

public enum UiAssertionFailureReason {
    ELEMENT_NOT_FOUND,
    ELEMENT_NOT_VISIBLE,
    ELEMENT_STILL_VISIBLE,
    ELEMENT_NOT_ENABLED,
    ELEMENT_NOT_DISABLED,
    TEXT_MISMATCH,
    VALUE_MISMATCH,
    STALE_ELEMENT,
    TIMEOUT,
    UNKNOWN
}

