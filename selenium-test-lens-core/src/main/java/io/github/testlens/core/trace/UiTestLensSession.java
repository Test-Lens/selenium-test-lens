package io.github.testlens.core.trace;

import io.github.testlens.core.redaction.RedactionPolicy;
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
    private final RedactionPolicy redactionPolicy;
    private TraceMetadata metadata;
    private boolean retryDecisionRecorded;
    private boolean retryPolicyTriggered;

    private UiTestLensSession(String name, RetryOutcomePolicy retryOutcomePolicy, int allowedRetries,
                              RedactionPolicy redactionPolicy) {
        if (allowedRetries < 0) throw new IllegalArgumentException("allowedRetries must not be negative");
        this.retryOutcomePolicy = retryOutcomePolicy == null ? RetryOutcomePolicy.REPORT_ONLY : retryOutcomePolicy;
        this.allowedRetries = allowedRetries;
        this.redactionPolicy = redactionPolicy == null ? RedactionPolicy.defaults() : redactionPolicy;
        String id = UUID.randomUUID().toString();
        String safeName = name == null || name.isBlank() ? "Selenium Test Lens session" : this.redactionPolicy.redact(name.trim());
        this.metadata = TraceMetadata.builder(id, safeName)
                .status(TraceStatus.STARTED)
                .build();
        addEvent(TraceEvent.started(TraceEventType.SESSION_STARTED, this.metadata.name())
                .toBuilder()
                .attribute("sessionId", id)
                .build());
    }

    public static UiTestLensSession start(String name) {
        return new UiTestLensSession(name, RetryOutcomePolicy.REPORT_ONLY, 0, RedactionPolicy.defaults());
    }

    public static UiTestLensSession start(String name, RetryOutcomePolicy policy, int allowedRetries) {
        return new UiTestLensSession(name, policy, allowedRetries, RedactionPolicy.defaults());
    }

    public static UiTestLensSession start(String name, RetryOutcomePolicy policy, int allowedRetries,
                                          RedactionPolicy redactionPolicy) {
        return new UiTestLensSession(name, policy, allowedRetries, redactionPolicy);
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
        TraceEvent safe = redactEvent(event);
        events.add(safe);
        return safe;
    }

    public synchronized TraceArtifact attachArtifact(TraceArtifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("artifact must not be null");
        }
        TraceArtifact safe = redactArtifact(artifact);
        artifacts.add(safe);
        addEvent(TraceEvent.builder(TraceEventType.ARTIFACT_ATTACHED, TraceStatus.INFO, safe.name())
                .message("Artifact attached")
                .attribute("artifactType", safe.type().name())
                .attribute("path", safe.path())
                .attribute("url", safe.url())
                .artifact(safe)
                .build());
        return safe;
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

    private TraceEvent redactEvent(TraceEvent event) {
        Map<String, String> attributes = new java.util.LinkedHashMap<>();
        event.attributes().forEach((key, value) -> attributes.put(key, redactionPolicy.redact(key, value)));
        TraceEvent.Builder builder = event.toBuilder()
                .name(redactionPolicy.redact(event.name()))
                .message(redactionPolicy.redact(event.message()))
                .attributes(attributes)
                .artifacts(event.artifacts().stream().map(this::redactArtifact).toList());
        if (event.failure() != null) builder.failure(redactFailure(event.failure()));
        return builder.build();
    }

    private TraceFailure redactFailure(TraceFailure failure) {
        Map<String, String> details = new java.util.LinkedHashMap<>();
        failure.details().forEach((key, value) -> details.put(key, redactionPolicy.redact(key, value)));
        return new TraceFailure(redactionPolicy.redact(failure.message()), failure.exceptionType(),
                redactionPolicy.redact(failure.stackTrace()), details);
    }

    private TraceArtifact redactArtifact(TraceArtifact artifact) {
        Map<String, String> metadata = new java.util.LinkedHashMap<>();
        artifact.metadata().forEach((key, value) -> metadata.put(key, redactionPolicy.redact(key, value)));
        return TraceArtifact.of(redactionPolicy.redact(artifact.name()), artifact.type(),
                redactionPolicy.redact(artifact.path()), redactionPolicy.redactUrl(artifact.url()),
                redactionPolicy.redact(artifact.mediaType()), artifact.createdAt(), metadata);
    }

    private static final class DurationAccumulator {
        private java.time.Duration value = java.time.Duration.ZERO;
        void add(java.time.Duration duration) {
            if (duration != null && !duration.isNegative()) value = value.plus(duration);
        }
        java.time.Duration value() { return value; }
    }
}

