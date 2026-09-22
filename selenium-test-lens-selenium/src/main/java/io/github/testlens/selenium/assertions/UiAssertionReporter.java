package io.github.testlens.selenium.assertions;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

final class UiAssertionReporter {
    private final OverlayLogger logger;
    private final boolean sourceNavigationEnabled;
    private final String operationSession = UUID.randomUUID().toString();
    private final AtomicLong operationSequence = new AtomicLong();
    private final ThreadLocal<OperationState> activeOperation = new ThreadLocal<>();

    public UiAssertionReporter(OverlayLogger logger) {
        this(logger, false);
    }

    UiAssertionReporter(OverlayLogger logger, boolean sourceNavigationEnabled) {
        this.logger = logger != null ? logger : OverlayLogger.noop();
        this.sourceNavigationEnabled = sourceNavigationEnabled;
    }

    public static UiAssertionReporter noop() {
        return new UiAssertionReporter(OverlayLogger.noop());
    }

    public void started(String assertionName, String locatorDescription) {
        emit(UiTestLensEventType.ASSERTION_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Waiting for assertion condition", assertionName, locatorDescription, 0, "", "");
    }

    public void retry(String assertionName, String locatorDescription, int attempt, String expectedPreview, String actualPreview) {
        emit(UiTestLensEventType.ASSERTION_RETRY, UiTestLensStatus.WARN, UiTestLensLogLevel.WARN,
                "Condition not yet satisfied", assertionName, locatorDescription, attempt, expectedPreview, actualPreview);
    }

    public void passed(UiAssertionResult result) {
        emit(UiTestLensEventType.ASSERTION_PASSED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                result.message(), result.assertionName(), result.locatorDescription(), result.attempts(),
                result.expectedPreview(), result.actualPreview());
    }

    public void failed(UiAssertionResult result) {
        UiTestLensEventType eventType = result.status() == UiAssertionStatus.TIMED_OUT
                ? UiTestLensEventType.ASSERTION_TIMED_OUT
                : UiTestLensEventType.ASSERTION_FAILED;
        emit(eventType, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR,
                result.message(), result.assertionName(), result.locatorDescription(), result.attempts(),
                result.expectedPreview(), result.actualPreview());
    }

    private void emit(UiTestLensEventType eventType,
                      UiTestLensStatus status,
                      UiTestLensLogLevel level,
                      String message,
                      String assertionName,
                      String locatorDescription,
                      int attempt,
                      String expectedPreview,
                      String actualPreview) {
        try {
            OperationState operation = operationFor(status);
            UiTestLensLogEntry.Builder builder = UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(eventType)
                    .status(status)
                    .message(message == null || message.isBlank() ? "Retryable assertion event" : message)
                    .action("assert." + safe(assertionName))
                    .metadata("assertion", safe(assertionName))
                    .metadata("locator", safe(locatorDescription))
                    .metadata("attempt", String.valueOf(attempt))
                    .metadata("expectedPreview", safe(expectedPreview))
                    .metadata("actualPreview", safe(actualPreview));
            builder.metadata("operationId", operation.id());
            if (status == UiTestLensStatus.PASSED || status == UiTestLensStatus.FAILED) {
                builder.metadata("durationMs", String.valueOf(Math.max(0L,
                        (System.nanoTime() - operation.startedNanos()) / 1_000_000L)));
            }
            if (eventType == UiTestLensEventType.ASSERTION_RETRY) {
                builder.metadata("retryKind", "poll");
            }
            if (sourceNavigationEnabled) builder.metadata("testlens.internal.captureSourceLocation", "true");
            logger.emit(builder.build());
            if (status == UiTestLensStatus.PASSED || status == UiTestLensStatus.FAILED) activeOperation.remove();
        } catch (Exception ignored) {
        }
    }

    private OperationState operationFor(UiTestLensStatus status) {
        OperationState current = activeOperation.get();
        if (status == UiTestLensStatus.STARTED || current == null) {
            current = new OperationState(operationSession + ":" + operationSequence.incrementAndGet(), System.nanoTime());
            activeOperation.set(current);
        }
        return current;
    }

    private record OperationState(String id, long startedNanos) {}

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

