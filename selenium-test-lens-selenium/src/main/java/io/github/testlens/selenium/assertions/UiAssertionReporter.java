package io.github.testlens.selenium.assertions;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;

final class UiAssertionReporter {
    private final OverlayLogger logger;

    public UiAssertionReporter(OverlayLogger logger) {
        this.logger = logger != null ? logger : OverlayLogger.noop();
    }

    public static UiAssertionReporter noop() {
        return new UiAssertionReporter(OverlayLogger.noop());
    }

    public void started(String assertionName, String locatorDescription) {
        emit(UiTestLensEventType.ASSERTION_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Retryable assertion started", assertionName, locatorDescription, 0, "", "");
    }

    public void retry(String assertionName, String locatorDescription, int attempt, String expectedPreview, String actualPreview) {
        emit(UiTestLensEventType.ASSERTION_RETRY, UiTestLensStatus.WARN, UiTestLensLogLevel.WARN,
                "Retryable assertion retry", assertionName, locatorDescription, attempt, expectedPreview, actualPreview);
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
            logger.emit(UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(eventType)
                    .status(status)
                    .message(message == null || message.isBlank() ? "Retryable assertion event" : message)
                    .action("assert." + safe(assertionName))
                    .metadata("assertion", safe(assertionName))
                    .metadata("locator", safe(locatorDescription))
                    .metadata("attempt", String.valueOf(attempt))
                    .metadata("expectedPreview", safe(expectedPreview))
                    .metadata("actualPreview", safe(actualPreview))
                    .build());
        } catch (Exception ignored) {
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

