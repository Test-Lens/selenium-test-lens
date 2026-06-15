package io.github.testlens.selenium.business;

import io.github.testlens.selenium.assertions.UiAssertionResult;

import java.util.Objects;
import java.util.function.Supplier;

final class BusinessAssertion {
    private final String description;
    private final Supplier<UiAssertionResult> assertion;

    private BusinessAssertion(String description, Supplier<UiAssertionResult> assertion) {
        this.description = validateDescription(description);
        this.assertion = Objects.requireNonNull(assertion, "assertion must not be null");
    }

    static BusinessAssertion of(String description, Runnable assertion) {
        Objects.requireNonNull(assertion, "assertion must not be null");
        return new BusinessAssertion(description, () -> {
            assertion.run();
            return null;
        });
    }

    static BusinessAssertion of(String description, Supplier<UiAssertionResult> assertion) {
        return new BusinessAssertion(description, assertion);
    }

    String description() {
        return description;
    }

    UiAssertionResult run() {
        return assertion.get();
    }

    private static String validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        return description.trim();
    }
}

