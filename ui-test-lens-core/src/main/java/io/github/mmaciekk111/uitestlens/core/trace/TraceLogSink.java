package io.github.mmaciekk111.uitestlens.core.trace;

import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensEventType;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogLevel;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogSink;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensStatus;

import java.util.Map;
import java.util.Objects;

public final class TraceLogSink implements UiTestLensLogSink {
    private final UiTestLensSession session;

    public TraceLogSink(UiTestLensSession session) {
        this.session = Objects.requireNonNull(session, "session must not be null");
    }

    @Override
    public void accept(UiTestLensLogEntry entry) {
        if (entry == null) {
            return;
        }
        TraceEvent.Builder builder = TraceEvent.builder(typeFor(entry.eventType()), statusFor(entry.status(), entry.level()), nameFor(entry))
                .timestamp(entry.timestamp())
                .message(entry.message())
                .attribute("logEventType", entry.eventType() == null ? "" : entry.eventType().name())
                .attribute("logLevel", entry.level() == null ? "" : entry.level().name())
                .attribute("action", entry.action() == null ? "" : entry.action())
                .attribute("step", entry.step() == null ? "" : entry.step());
        for (Map.Entry<String, String> metadata : entry.metadata().entrySet()) {
            builder.attribute("metadata." + metadata.getKey(), metadata.getValue());
        }
        if (entry.throwable() != null) {
            builder.failure(TraceFailure.from(entry.throwable(), false));
        }
        session.addEvent(builder.build());
    }

    private static String nameFor(UiTestLensLogEntry entry) {
        if (entry.step() != null && !entry.step().isBlank()) {
            return entry.step();
        }
        if (entry.action() != null && !entry.action().isBlank()) {
            return entry.action();
        }
        return entry.eventType() == null ? "log" : entry.eventType().name();
    }

    private static TraceStatus statusFor(UiTestLensStatus status, UiTestLensLogLevel level) {
        if (status == UiTestLensStatus.STARTED) {
            return TraceStatus.STARTED;
        }
        if (status == UiTestLensStatus.PASSED) {
            return TraceStatus.PASSED;
        }
        if (status == UiTestLensStatus.FAILED) {
            return TraceStatus.FAILED;
        }
        if (status == UiTestLensStatus.SKIPPED) {
            return TraceStatus.SKIPPED;
        }
        if (status == UiTestLensStatus.WARN || level == UiTestLensLogLevel.WARN) {
            return TraceStatus.WARNING;
        }
        if (level == UiTestLensLogLevel.ERROR) {
            return TraceStatus.ERROR;
        }
        return TraceStatus.INFO;
    }

    private static TraceEventType typeFor(UiTestLensEventType type) {
        if (type == null) {
            return TraceEventType.CUSTOM;
        }
        return switch (type) {
            case STEP_STARTED -> TraceEventType.STEP_STARTED;
            case STEP_PASSED -> TraceEventType.STEP_PASSED;
            case STEP_FAILED -> TraceEventType.STEP_FAILED;
            case ACTION, LOCATOR_ACTION_STARTED, LOCATOR_ACTION_PASSED, LOCATOR_ACTION_FAILED, LOCATOR_RETRY -> TraceEventType.LOCATOR_ACTION;
            case ASSERTION, ASSERTION_STARTED, ASSERTION_RETRY -> TraceEventType.ASSERTION_STARTED;
            case ASSERTION_PASSED -> TraceEventType.ASSERTION_PASSED;
            case ASSERTION_FAILED, ASSERTION_TIMED_OUT -> TraceEventType.ASSERTION_FAILED;
            case BUSINESS_ASSERTION_STARTED, BUSINESS_ASSERTION_GROUP_STARTED -> TraceEventType.BUSINESS_ASSERTION_STARTED;
            case BUSINESS_ASSERTION_PASSED, BUSINESS_ASSERTION_GROUP_PASSED -> TraceEventType.BUSINESS_ASSERTION_PASSED;
            case BUSINESS_ASSERTION_FAILED, BUSINESS_ASSERTION_GROUP_FAILED -> TraceEventType.BUSINESS_ASSERTION_FAILED;
            case OVERLAY_DETECTED -> TraceEventType.OVERLAY_DETECTED;
            case OVERLAY_HANDLED, OVERLAY_STILL_VISIBLE -> TraceEventType.OVERLAY_HANDLED;
            case ACTIONABILITY_CHECK_STARTED, ACTIONABILITY_CHECK_PASSED, ACTIONABILITY_CHECK_FAILED, ACTIONABILITY_READY, ACTIONABILITY_NOT_READY -> TraceEventType.ACTIONABILITY_CHECK;
            default -> TraceEventType.CUSTOM;
        };
    }
}
