package io.github.testlens.selenium.business;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;

public final class BusinessAssertionReporter {
    private final OverlayLogger logger;

    public BusinessAssertionReporter(OverlayLogger logger) {
        this.logger = logger != null ? logger : OverlayLogger.noop();
    }

    public void groupStarted(String subject, int checks) {
        emit(UiTestLensEventType.BUSINESS_ASSERTION_GROUP_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Business assertion group started", subject, "", checks, "");
    }

    public void checkStarted(String subject, String description) {
        emit(UiTestLensEventType.BUSINESS_ASSERTION_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Business assertion started", subject, description, 0, "");
    }

    public void checkFinished(BusinessAssertionResult result) {
        boolean passed = result.status() == BusinessAssertionStatus.PASSED;
        emit(passed ? UiTestLensEventType.BUSINESS_ASSERTION_PASSED : UiTestLensEventType.BUSINESS_ASSERTION_FAILED,
                passed ? UiTestLensStatus.PASSED : UiTestLensStatus.FAILED,
                passed ? UiTestLensLogLevel.INFO : UiTestLensLogLevel.ERROR,
                passed ? "Business assertion passed" : "Business assertion failed",
                result.subject(), result.description(), 0, result.message());
    }

    public void groupFinished(String subject, boolean passed, int total, int failed) {
        emit(passed ? UiTestLensEventType.BUSINESS_ASSERTION_GROUP_PASSED : UiTestLensEventType.BUSINESS_ASSERTION_GROUP_FAILED,
                passed ? UiTestLensStatus.PASSED : UiTestLensStatus.FAILED,
                passed ? UiTestLensLogLevel.INFO : UiTestLensLogLevel.ERROR,
                passed ? "Business assertion group passed" : "Business assertion group failed",
                subject, "", total, "failed=" + failed);
    }

    private void emit(UiTestLensEventType eventType,
                      UiTestLensStatus status,
                      UiTestLensLogLevel level,
                      String message,
                      String subject,
                      String description,
                      int count,
                      String summary) {
        try {
            logger.emit(UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(eventType)
                    .status(status)
                    .message(message)
                    .action("business.assert")
                    .metadata("subject", safe(subject))
                    .metadata("description", safe(description))
                    .metadata("count", String.valueOf(count))
                    .metadata("summary", safe(summary))
                    .build());
        } catch (Exception ignored) {
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
