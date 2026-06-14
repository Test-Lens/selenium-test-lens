package io.github.mmaciekk111.uitestlens.selenium.steps;

public final class UiStepError extends RuntimeException {
    private final UiStepResult result;

    public UiStepError(UiStepResult result) {
        super(UiStepReporter.formatFailure(result), result == null || result.failure() == null ? null : result.failure().cause());
        this.result = result;
    }

    public UiStepResult result() {
        return result;
    }
}
