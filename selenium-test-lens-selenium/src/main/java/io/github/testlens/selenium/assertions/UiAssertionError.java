package io.github.testlens.selenium.assertions;

public final class UiAssertionError extends AssertionError {
    private final UiAssertionResult result;

    public UiAssertionError(UiAssertionResult result) {
        super(result == null ? "UI assertion failed" : result.summary());
        this.result = result;
    }

    public UiAssertionResult result() {
        return result;
    }
}

