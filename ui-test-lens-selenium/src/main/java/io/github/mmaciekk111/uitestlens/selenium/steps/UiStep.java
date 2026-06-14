package io.github.mmaciekk111.uitestlens.selenium.steps;

import java.util.Objects;

final class UiStep {
    private final String name;
    private final Runnable body;
    private final UiStepOptions options;

    UiStep(String name, Runnable body, UiStepOptions options) {
        this.name = UiStepResult.validateName(name);
        this.body = Objects.requireNonNull(body, "body must not be null");
        this.options = options == null ? UiStepOptions.defaults() : options;
    }

    String name() {
        return name;
    }

    Runnable body() {
        return body;
    }

    UiStepOptions options() {
        return options;
    }
}
