package io.github.testlens.selenium.steps;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class UiStepResult {
    private final String name;
    private final UiStepStatus status;
    private final Instant startedAt;
    private final Instant endedAt;
    private final Duration elapsed;
    private final UiStepFailure failure;
    private final List<UiStepResult> children;

    private UiStepResult(String name,
                         UiStepStatus status,
                         Instant startedAt,
                         Instant endedAt,
                         UiStepFailure failure,
                         List<UiStepResult> children) {
        this.name = validateName(name);
        this.status = status == null ? UiStepStatus.FAILED : status;
        this.startedAt = startedAt == null ? Instant.now() : startedAt;
        this.endedAt = endedAt == null ? this.startedAt : endedAt;
        Duration computed = Duration.between(this.startedAt, this.endedAt);
        this.elapsed = computed.isNegative() ? Duration.ZERO : computed;
        this.failure = failure;
        this.children = List.copyOf(children == null ? List.of() : children);
    }

    public static UiStepResult passed(String name, Instant startedAt, Instant endedAt) {
        return new UiStepResult(name, UiStepStatus.PASSED, startedAt, endedAt, null, List.of());
    }

    public static UiStepResult failed(String name, Instant startedAt, Instant endedAt, UiStepFailure failure) {
        return new UiStepResult(name, UiStepStatus.FAILED, startedAt, endedAt, failure, List.of());
    }

    public static UiStepResult skipped(String name, Instant startedAt, Instant endedAt, String message) {
        UiStepFailure failure = message == null || message.isBlank()
                ? null
                : new UiStepFailure(message, null, "", "");
        return new UiStepResult(name, UiStepStatus.SKIPPED, startedAt, endedAt, failure, List.of());
    }

    public String name() {
        return name;
    }

    public UiStepStatus status() {
        return status;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant endedAt() {
        return endedAt;
    }

    public Duration elapsed() {
        return elapsed;
    }

    public UiStepFailure failure() {
        return failure;
    }

    public List<UiStepResult> children() {
        return children;
    }

    public boolean isPassed() {
        return status == UiStepStatus.PASSED;
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Step ").append(status).append(": ").append(name)
                .append(" elapsedMs=").append(elapsed.toMillis());
        if (failure != null && !failure.message().isBlank()) {
            sb.append(" cause=").append(failure.message());
        }
        return sb.toString();
    }

    static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("step name must not be blank");
        }
        return name.trim();
    }
}
