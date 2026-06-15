package io.github.mmaciekk111.uitestlens.core.logging.export;

import io.github.mmaciekk111.uitestlens.core.logging.TargetDescriptor;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensEventType;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensStatus;
import io.github.mmaciekk111.uitestlens.core.trace.TraceEvent;
import io.github.mmaciekk111.uitestlens.core.trace.TraceEventType;
import io.github.mmaciekk111.uitestlens.core.trace.TraceFailure;
import io.github.mmaciekk111.uitestlens.core.trace.TraceStatus;
import io.github.mmaciekk111.uitestlens.core.trace.UiTestLensSession;
import io.github.mmaciekk111.uitestlens.core.trace.export.TraceHtmlExportOptions;
import io.github.mmaciekk111.uitestlens.core.trace.export.TraceHtmlExporter;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class HtmlLogExporter implements UiTestLensLogExporter {
    private final LogExportOptions options;
    private final TraceHtmlExporter traceExporter = new TraceHtmlExporter();

    public HtmlLogExporter() {
        this(LogExportOptions.defaults());
    }

    public HtmlLogExporter(LogExportOptions options) {
        this.options = options != null ? options : LogExportOptions.defaults();
    }

    @Override
    public String export(List<UiTestLensLogEntry> entries) {
        return traceExporter.export(toSession(entries), reportOptions());
    }

    public String export(List<UiTestLensLogEntry> entries, TraceHtmlExportOptions reportOptions) {
        return traceExporter.export(toSession(entries), reportOptions == null ? reportOptions() : reportOptions);
    }

    public Path exportTo(List<UiTestLensLogEntry> entries, Path outputPath) {
        return traceExporter.exportTo(toSession(entries), outputPath, reportOptions());
    }

    public Path exportTo(List<UiTestLensLogEntry> entries, Path outputPath, TraceHtmlExportOptions reportOptions) {
        return traceExporter.exportTo(toSession(entries), outputPath, reportOptions == null ? reportOptions() : reportOptions);
    }

    public Path exportToDefault(List<UiTestLensLogEntry> entries) {
        return traceExporter.exportToDefault(toSession(entries), reportOptions());
    }

    public Path exportToDefault(List<UiTestLensLogEntry> entries, TraceHtmlExportOptions reportOptions) {
        return traceExporter.exportToDefault(toSession(entries), reportOptions == null ? reportOptions() : reportOptions);
    }

    private UiTestLensSession toSession(List<UiTestLensLogEntry> entries) {
        UiTestLensSession session = UiTestLensSession.start("Selenium Test Lens log report");
        if (entries != null) {
            for (UiTestLensLogEntry entry : entries) {
                if (entry != null) {
                    session.addEvent(toTraceEvent(entry));
                }
            }
        }

        boolean failed = session.events().stream()
                .anyMatch(event -> event.status() == TraceStatus.FAILED || event.status() == TraceStatus.ERROR);
        boolean skipped = session.events().stream().anyMatch(event -> event.status() == TraceStatus.SKIPPED);
        if (failed) {
            session.finishFailed(null);
        } else if (skipped) {
            session.finishSkipped("Log report contains skipped entries");
        } else {
            session.finishPassed();
        }
        return session;
    }

    private TraceEvent toTraceEvent(UiTestLensLogEntry entry) {
        TraceEvent.Builder builder = TraceEvent.builder(
                        traceType(entry.eventType()),
                        traceStatus(entry.status()),
                        firstNonBlank(entry.action(), entry.step(), entry.eventType().name()))
                .timestamp(entry.timestamp())
                .message(limit(entry.message()))
                .attribute("level", entry.level().name())
                .attribute("eventType", entry.eventType().name());

        if (entry.step() != null && !entry.step().isBlank()) {
            builder.attribute("step", limit(entry.step()));
        }
        if (entry.action() != null && !entry.action().isBlank()) {
            builder.attribute("action", limit(entry.action()));
        }
        appendTarget(builder, entry.target());
        if (options.includeMetadata()) {
            for (Map.Entry<String, String> metadata : entry.metadata().entrySet()) {
                builder.attribute("metadata." + metadata.getKey(), limit(metadata.getValue()));
            }
        }
        if (entry.throwable() != null) {
            builder.failure(TraceFailure.from(entry.throwable(), true));
        }
        return builder.build();
    }

    private void appendTarget(TraceEvent.Builder builder, TargetDescriptor target) {
        if (target == null) {
            return;
        }
        builder.attribute("target", limit(formatTarget(target)));
        if (target.selector() != null) {
            builder.attribute("target.selector", limit(target.selector()));
        }
        if (target.label() != null) {
            builder.attribute("target.label", limit(target.label()));
        }
        if (target.tagName() != null) {
            builder.attribute("target.tagName", limit(target.tagName()));
        }
        if (target.text() != null) {
            builder.attribute("target.text", limit(target.text()));
        }
        if (options.includeMetadata()) {
            for (Map.Entry<String, String> metadata : target.metadata().entrySet()) {
                builder.attribute("target.metadata." + metadata.getKey(), limit(metadata.getValue()));
            }
        }
    }

    private String formatTarget(TargetDescriptor target) {
        if (target.selector() != null) {
            return target.selector();
        }
        if (target.label() != null) {
            return target.label();
        }
        if (target.tagName() != null) {
            return target.tagName();
        }
        if (target.text() != null) {
            return target.text();
        }
        return "";
    }

    private TraceHtmlExportOptions reportOptions() {
        return TraceHtmlExportOptions.builder()
                .title("Selenium Test Lens Log Report")
                .includeJsonPayload(false)
                .includeArtifacts(false)
                .includeStackTraces(true)
                .maxMessageLength(options.maxFieldLength())
                .build();
    }

    private TraceStatus traceStatus(UiTestLensStatus status) {
        UiTestLensStatus effectiveStatus = status == null ? UiTestLensStatus.INFO : status;
        return switch (effectiveStatus) {
            case STARTED -> TraceStatus.STARTED;
            case PASSED -> TraceStatus.PASSED;
            case FAILED -> TraceStatus.FAILED;
            case SKIPPED -> TraceStatus.SKIPPED;
            case WARN -> TraceStatus.WARNING;
            case INFO -> TraceStatus.INFO;
        };
    }

    private TraceEventType traceType(UiTestLensEventType eventType) {
        UiTestLensEventType effectiveType = eventType == null ? UiTestLensEventType.GENERAL : eventType;
        return switch (effectiveType) {
            case STEP, STEP_STARTED -> TraceEventType.STEP_STARTED;
            case STEP_PASSED -> TraceEventType.STEP_PASSED;
            case STEP_FAILED -> TraceEventType.STEP_FAILED;
            case ACTION -> TraceEventType.ACTION_STARTED;
            case ACTIONABILITY_CHECK_STARTED, ACTIONABILITY_CHECK_PASSED, ACTIONABILITY_CHECK_FAILED,
                    ACTIONABILITY_READY, ACTIONABILITY_NOT_READY -> TraceEventType.ACTIONABILITY_CHECK;
            case ASSERTION, ASSERTION_STARTED -> TraceEventType.ASSERTION_STARTED;
            case ASSERTION_PASSED, NETWORK_ASSERTION_PASSED -> TraceEventType.ASSERTION_PASSED;
            case ASSERTION_FAILED, ASSERTION_TIMED_OUT, NETWORK_ASSERTION_FAILED -> TraceEventType.ASSERTION_FAILED;
            case BUSINESS_ASSERTION_STARTED, BUSINESS_ASSERTION_GROUP_STARTED -> TraceEventType.BUSINESS_ASSERTION_STARTED;
            case BUSINESS_ASSERTION_PASSED, BUSINESS_ASSERTION_GROUP_PASSED -> TraceEventType.BUSINESS_ASSERTION_PASSED;
            case BUSINESS_ASSERTION_FAILED, BUSINESS_ASSERTION_GROUP_FAILED -> TraceEventType.BUSINESS_ASSERTION_FAILED;
            case OVERLAY, OVERLAY_DETECTED, OVERLAY_POLICY_STARTED, OVERLAY_ACTION_STARTED, OVERLAY_STILL_VISIBLE -> TraceEventType.OVERLAY_DETECTED;
            case OVERLAY_ACTION_PASSED, OVERLAY_ACTION_FAILED, OVERLAY_HANDLED -> TraceEventType.OVERLAY_HANDLED;
            case LOCATOR_RESOLVE_STARTED, LOCATOR_RESOLVE_PASSED, LOCATOR_RESOLVE_FAILED -> TraceEventType.LOCATOR_RESOLVE;
            case LOCATOR_ACTION_STARTED, LOCATOR_ACTION_PASSED, LOCATOR_ACTION_FAILED, LOCATOR_RETRY -> TraceEventType.LOCATOR_ACTION;
            case SCREENSHOT_CAPTURE_STARTED, SCREENSHOT_CAPTURE_PASSED, SCREENSHOT_CAPTURE_FAILED -> TraceEventType.SCREENSHOT;
            case VIDEO_ATTACHED, VIDEO_ATTACH_FAILED, VIDEO_ATTACH_SKIPPED -> TraceEventType.VIDEO;
            case NETWORK_DIAGNOSTICS_STARTED, NETWORK_DIAGNOSTICS_STOPPED, NETWORK_REQUEST_RECORDED,
                    NETWORK_RESPONSE_RECORDED, NETWORK_FAILURE_RECORDED -> TraceEventType.NETWORK_EVENT;
            case NETWORK_LOG_ATTACHED -> TraceEventType.ARTIFACT_ATTACHED;
            case NETWORK_WAIT_STARTED, NETWORK_WAIT_PASSED, NETWORK_WAIT_FAILED, NETWORK_WAIT_TIMED_OUT -> TraceEventType.NETWORK_WAIT;
            case ERROR -> TraceEventType.ACTION_FAILED;
            default -> TraceEventType.CUSTOM;
        };
    }

    private String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback == null || fallback.isBlank() ? "Log entry" : fallback;
    }

    private String limit(String value) {
        if (value == null) {
            return "";
        }
        int max = options.maxFieldLength();
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
