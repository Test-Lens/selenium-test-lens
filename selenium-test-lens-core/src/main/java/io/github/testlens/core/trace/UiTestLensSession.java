package io.github.testlens.core.trace;

import io.github.testlens.core.trace.export.TraceHtmlExportOptions;
import io.github.testlens.core.trace.export.TraceHtmlExporter;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * In-memory trace/evidence session for a single UI automation flow.
 *
 * <p>The session collects timeline events and artifact references and can export JSON or HTML reports.
 */
public final class UiTestLensSession {
    private final List<TraceEvent> events = new ArrayList<>();
    private final List<TraceArtifact> artifacts = new ArrayList<>();
    private final RetryOutcomePolicy retryOutcomePolicy;
    private final int allowedRetries;
    private TraceMetadata metadata;
    private boolean retryDecisionRecorded;
    private boolean retryPolicyTriggered;

    private UiTestLensSession(String name, RetryOutcomePolicy retryOutcomePolicy, int allowedRetries) {
        if (allowedRetries < 0) throw new IllegalArgumentException("allowedRetries must not be negative");
        this.retryOutcomePolicy = retryOutcomePolicy == null ? RetryOutcomePolicy.REPORT_ONLY : retryOutcomePolicy;
        this.allowedRetries = allowedRetries;
        String id = UUID.randomUUID().toString();
        this.metadata = TraceMetadata.builder(id, name == null || name.isBlank() ? "Selenium Test Lens session" : name.trim())
                .status(TraceStatus.STARTED)
                .build();
        addEvent(TraceEvent.started(TraceEventType.SESSION_STARTED, this.metadata.name())
                .toBuilder()
                .attribute("sessionId", id)
                .build());
    }

    public static UiTestLensSession start(String name) {
        return new UiTestLensSession(name, RetryOutcomePolicy.REPORT_ONLY, 0);
    }

    public static UiTestLensSession start(String name, RetryOutcomePolicy policy, int allowedRetries) {
        return new UiTestLensSession(name, policy, allowedRetries);
    }

    public String id() {
        return metadata.sessionId();
    }

    public synchronized TraceMetadata metadata() {
        return metadata;
    }

    public synchronized List<TraceEvent> events() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public synchronized List<TraceArtifact> artifacts() {
        return Collections.unmodifiableList(new ArrayList<>(artifacts));
    }

    public synchronized RetrySummary retrySummary() {
        Map<String, Long> byAction = new TreeMap<>();
        Map<String, Long> byLocator = new TreeMap<>();
        Map<String, Long> byException = new TreeMap<>();
        long total = 0;
        DurationAccumulator timeLost = new DurationAccumulator();
        for (TraceEvent event : events) {
            if (event.type() != TraceEventType.RETRY) continue;
            total++;
            timeLost.add(event.duration());
            increment(byAction, event.attributes().get("retry.action"));
            increment(byLocator, event.attributes().get("retry.locator"));
            increment(byException, event.attributes().get("retry.exceptionType"));
        }
        return new RetrySummary(total, timeLost.value(), total > 0, retryOutcomePolicy,
                retryPolicyTriggered, byAction, byLocator, byException);
    }

    public synchronized TraceEvent addEvent(TraceEvent event) {
        if (event == null) {
            return null;
        }
        events.add(event);
        return event;
    }

    public synchronized TraceArtifact attachArtifact(TraceArtifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("artifact must not be null");
        }
        artifacts.add(artifact);
        addEvent(TraceEvent.builder(TraceEventType.ARTIFACT_ATTACHED, TraceStatus.INFO, artifact.name())
                .message("Artifact attached")
                .attribute("artifactType", artifact.type().name())
                .attribute("path", artifact.path())
                .attribute("url", artifact.url())
                .artifact(artifact)
                .build());
        return artifact;
    }

    public TraceArtifact attachScreenshot(String name, Path path) {
        return attachArtifact(TraceArtifact.screenshot(name, path));
    }

    public TraceArtifact attachVideo(String name, Path path) {
        return attachArtifact(TraceArtifact.video(name, path));
    }

    public TraceArtifact attachUrl(String name, TraceArtifactType type, String url) {
        return attachArtifact(TraceArtifact.url(name, type, url));
    }

    public synchronized void finishPassed() {
        RetrySummary beforeDecision = retrySummary();
        retryPolicyTriggered = triggersPolicy(beforeDecision.totalRetries());
        RetrySummary decided = retrySummary();
        if (retryPolicyTriggered) {
            RetryPolicyViolationException retryPolicyViolation = new RetryPolicyViolationException(retryOutcomePolicy, decided);
            recordRetryDecision(decided);
            finish(TraceStatus.FAILED, retryPolicyViolation, retryPolicyViolation.getMessage());
            throw retryPolicyViolation;
        } else {
            recordRetryDecision(decided);
            finish(TraceStatus.PASSED, null, "");
        }
    }

    public synchronized void finishFailed(Throwable throwable) {
        recordRetryDecision(retrySummary());
        finish(TraceStatus.FAILED, throwable, "");
    }

    public synchronized void finishSkipped(String reason) {
        recordRetryDecision(retrySummary());
        finish(TraceStatus.SKIPPED, null, reason);
    }

    public String exportJson() {
        return new TraceJsonExporter().export(this);
    }

    public String exportJson(TraceJsonExportOptions options) {
        return new TraceJsonExporter().export(this, options);
    }

    public Path exportJson(Path outputPath) {
        return new TraceJsonExporter().exportTo(this, outputPath);
    }

    public Path exportJson(Path outputPath, TraceJsonExportOptions options) {
        return new TraceJsonExporter().exportTo(this, outputPath, options);
    }

    public Path exportJsonReport() {
        return new TraceJsonExporter().exportToDefault(this);
    }

    public Path exportJsonReport(TraceJsonExportOptions options) {
        return new TraceJsonExporter().exportToDefault(this, options);
    }

    public String exportHtml() {
        return new TraceHtmlExporter().export(this);
    }

    public String exportHtml(TraceHtmlExportOptions options) {
        return new TraceHtmlExporter().export(this, options);
    }

    public Path exportHtml(Path outputPath) {
        return new TraceHtmlExporter().exportTo(this, outputPath);
    }

    public Path exportHtml(Path outputPath, TraceHtmlExportOptions options) {
        return new TraceHtmlExporter().exportTo(this, outputPath, options);
    }

    public Path exportHtmlReport() {
        return new TraceHtmlExporter().exportToDefault(this);
    }

    public Path exportHtmlReport(TraceHtmlExportOptions options) {
        return new TraceHtmlExporter().exportToDefault(this, options);
    }

    private void finish(TraceStatus status, Throwable throwable, String message) {
        Instant finishedAt = Instant.now();
        metadata = metadata.toBuilder()
                .status(status)
                .finishedAt(finishedAt)
                .build();
        TraceEvent.Builder event = TraceEvent.builder(TraceEventType.SESSION_FINISHED, status, metadata.name())
                .timestamp(finishedAt)
                .message(message == null ? "" : message)
                .attribute("sessionId", id());
        if (throwable != null) {
            event.failure(TraceFailure.from(throwable, false)).message(throwable.getMessage());
        }
        addEvent(event.build());
    }

    private boolean triggersPolicy(long totalRetries) {
        return switch (retryOutcomePolicy) {
            case REPORT_ONLY, WARN -> false;
            case FAIL_ON_ANY_RETRY -> totalRetries >= 1;
            case FAIL_AFTER_N -> totalRetries > allowedRetries;
        };
    }

    private void recordRetryDecision(RetrySummary summary) {
        if (retryDecisionRecorded) return;
        retryDecisionRecorded = true;
        TraceStatus status = summary.policyTriggered() ? TraceStatus.FAILED
                : summary.policy() == RetryOutcomePolicy.WARN && summary.flakyCandidate()
                ? TraceStatus.WARNING : TraceStatus.INFO;
        addEvent(TraceEvent.builder(TraceEventType.RETRY_SUMMARY, status, "Retry summary")
                .message(summary.flakyCandidate() ? "Recovery retry summary" : "No recovery retries recorded")
                .attribute("flakyCandidate", String.valueOf(summary.flakyCandidate()))
                .attribute("totalRetries", String.valueOf(summary.totalRetries()))
                .attribute("timeLostMs", String.valueOf(summary.timeLost().toMillis()))
                .attribute("policy", summary.policy().name())
                .attribute("policyTriggered", String.valueOf(summary.policyTriggered()))
                .attribute("allowedRetries", String.valueOf(allowedRetries))
                .build());
    }

    private static void increment(Map<String, Long> target, String key) {
        if (key != null && !key.isBlank()) target.merge(key, 1L, Long::sum);
    }

    private static final class DurationAccumulator {
        private java.time.Duration value = java.time.Duration.ZERO;
        void add(java.time.Duration duration) {
            if (duration != null && !duration.isNegative()) value = value.plus(duration);
        }
        java.time.Duration value() { return value; }
    }
}

