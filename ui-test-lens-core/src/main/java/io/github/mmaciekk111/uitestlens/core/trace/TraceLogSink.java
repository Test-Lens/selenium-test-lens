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
                .attribute("uiEventType", entry.eventType() == null ? "" : entry.eventType().name())
                .attribute("logEventType", entry.eventType() == null ? "" : entry.eventType().name())
                .attribute("logLevel", entry.level() == null ? "" : entry.level().name())
                .attribute("logStatus", entry.status() == null ? "" : entry.status().name())
                .attribute("action", entry.action() == null ? "" : entry.action())
                .attribute("step", entry.step() == null ? "" : entry.step());
        addTargetAttributes(builder, entry);
        for (Map.Entry<String, String> metadata : entry.metadata().entrySet()) {
            builder.attribute("metadata." + metadata.getKey(), metadata.getValue());
        }
        if (entry.throwable() != null) {
            builder.failure(TraceFailure.from(entry.throwable(), false));
        }
        session.addEvent(builder.build());
    }

    private static void addTargetAttributes(TraceEvent.Builder builder, UiTestLensLogEntry entry) {
        if (entry.target() == null) {
            return;
        }
        builder.attribute("target.selector", entry.target().selector() == null ? "" : entry.target().selector())
                .attribute("target.label", entry.target().label() == null ? "" : entry.target().label())
                .attribute("target.tagName", entry.target().tagName() == null ? "" : entry.target().tagName())
                .attribute("target.text", entry.target().text() == null ? "" : entry.target().text());
        for (Map.Entry<String, String> targetMetadata : entry.target().metadata().entrySet()) {
            builder.attribute("target.metadata." + targetMetadata.getKey(), targetMetadata.getValue());
        }
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
            case LOCATOR_RESOLVE_STARTED, LOCATOR_RESOLVE_PASSED, LOCATOR_RESOLVE_FAILED -> TraceEventType.LOCATOR_RESOLVE;
            case ACTION, LOCATOR_RETRY -> TraceEventType.LOCATOR_ACTION;
            case LOCATOR_ACTION_STARTED -> TraceEventType.ACTION_STARTED;
            case LOCATOR_ACTION_PASSED -> TraceEventType.ACTION_PASSED;
            case LOCATOR_ACTION_FAILED -> TraceEventType.ACTION_FAILED;
            case ASSERTION, ASSERTION_STARTED, ASSERTION_RETRY -> TraceEventType.ASSERTION_STARTED;
            case ASSERTION_PASSED -> TraceEventType.ASSERTION_PASSED;
            case ASSERTION_FAILED, ASSERTION_TIMED_OUT -> TraceEventType.ASSERTION_FAILED;
            case BUSINESS_ASSERTION_STARTED, BUSINESS_ASSERTION_GROUP_STARTED -> TraceEventType.BUSINESS_ASSERTION_STARTED;
            case BUSINESS_ASSERTION_PASSED, BUSINESS_ASSERTION_GROUP_PASSED -> TraceEventType.BUSINESS_ASSERTION_PASSED;
            case BUSINESS_ASSERTION_FAILED, BUSINESS_ASSERTION_GROUP_FAILED -> TraceEventType.BUSINESS_ASSERTION_FAILED;
            case OVERLAY_POLICY_STARTED, OVERLAY_DETECTED, OVERLAY_ACTION_STARTED -> TraceEventType.OVERLAY_DETECTED;
            case OVERLAY_ACTION_PASSED, OVERLAY_ACTION_FAILED, OVERLAY_HANDLED, OVERLAY_STILL_VISIBLE -> TraceEventType.OVERLAY_HANDLED;
            case ACTIONABILITY_CHECK_STARTED, ACTIONABILITY_CHECK_PASSED, ACTIONABILITY_CHECK_FAILED, ACTIONABILITY_READY, ACTIONABILITY_NOT_READY -> TraceEventType.ACTIONABILITY_CHECK;
            case SCREENSHOT_CAPTURE_STARTED, SCREENSHOT_CAPTURE_PASSED, SCREENSHOT_CAPTURE_FAILED -> TraceEventType.SCREENSHOT;
            case VIDEO_ATTACHED, VIDEO_ATTACH_FAILED, VIDEO_ATTACH_SKIPPED -> TraceEventType.VIDEO;
            case NETWORK_DIAGNOSTICS_STARTED, NETWORK_DIAGNOSTICS_STOPPED, NETWORK_REQUEST_RECORDED, NETWORK_RESPONSE_RECORDED, NETWORK_FAILURE_RECORDED, NETWORK_ASSERTION_PASSED, NETWORK_ASSERTION_FAILED, NETWORK_LOG_ATTACHED -> TraceEventType.NETWORK_EVENT;
            case NETWORK_WAIT_STARTED, NETWORK_WAIT_PASSED, NETWORK_WAIT_FAILED, NETWORK_WAIT_TIMED_OUT -> TraceEventType.NETWORK_WAIT;
            default -> TraceEventType.CUSTOM;
        };
    }
}
