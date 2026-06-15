package io.github.testlens.selenium.steps;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;

public final class UiStepReporter {
    private final OverlayLogger logger;

    public UiStepReporter(OverlayLogger logger) {
        this.logger = logger != null ? logger : OverlayLogger.noop();
    }

    public void started(String name, UiStepOptions options) {
        emit(UiTestLensEventType.STEP_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Step started: " + name, name, UiStepStatus.RUNNING, 0, "", options);
    }

    public void finished(UiStepResult result, UiStepOptions options) {
        UiTestLensEventType eventType = switch (result.status()) {
            case PASSED -> UiTestLensEventType.STEP_PASSED;
            case SKIPPED -> UiTestLensEventType.STEP_SKIPPED;
            case FAILED -> UiTestLensEventType.STEP_FAILED;
            case RUNNING -> UiTestLensEventType.STEP_STARTED;
        };
        UiTestLensStatus status = switch (result.status()) {
            case PASSED -> UiTestLensStatus.PASSED;
            case SKIPPED -> UiTestLensStatus.SKIPPED;
            case FAILED -> UiTestLensStatus.FAILED;
            case RUNNING -> UiTestLensStatus.STARTED;
        };
        UiTestLensLogLevel level = result.status() == UiStepStatus.FAILED ? UiTestLensLogLevel.ERROR : UiTestLensLogLevel.INFO;
        String failure = result.failure() == null ? "" : result.failure().message();
        emit(eventType, status, level, "Step " + result.status() + ": " + result.name(),
                result.name(), result.status(), result.elapsed().toMillis(), failure, options);
    }

    public static String formatFailure(UiStepResult result) {
        if (result == null) {
            return "Step failed";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Step failed: ").append(result.name()).append("\n");
        if (result.failure() != null && !result.failure().message().isBlank()) {
            sb.append("Cause: ").append(result.failure().message()).append("\n");
        }
        sb.append("Elapsed: ").append(result.elapsed().toMillis()).append(" ms");
        return sb.toString();
    }

    private void emit(UiTestLensEventType eventType,
                      UiTestLensStatus status,
                      UiTestLensLogLevel level,
                      String message,
                      String name,
                      UiStepStatus stepStatus,
                      long elapsedMs,
                      String failure,
                      UiStepOptions options) {
        try {
            logger.emit(UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(eventType)
                    .status(status)
                    .message(message)
                    .step(name)
                    .action("step")
                    .metadata("stepName", safe(name))
                    .metadata("stepStatus", stepStatus == null ? "" : stepStatus.name())
                    .metadata("elapsedMs", String.valueOf(elapsedMs))
                    .metadata("failure", safe(failure))
                    .metadata("captureNestedEvents", String.valueOf(options == null || options.captureNestedEvents()))
                    .build());
        } catch (Exception ignored) {
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
