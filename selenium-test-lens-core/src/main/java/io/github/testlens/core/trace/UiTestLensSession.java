package io.github.testlens.core.trace;

import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.core.trace.export.TraceHtmlExportOptions;
import io.github.testlens.core.trace.export.TraceHtmlExporter;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * In-memory trace/evidence session for a single UI automation flow.
 *
 * <p>The session collects timeline events and artifact references and can export JSON or HTML reports.
 * {@link TraceStatus#STARTED} identifies an unfinished session, not a successful one. The first call to a
 * terminal finish method wins; later finish calls are idempotent and do not alter status, finish time,
 * failure information, skip reason, or the recorded retry-policy decision.
 */
public final class UiTestLensSession {
    private final BoundedTraceStore traceStore;
    private final RetryOutcomePolicy retryOutcomePolicy;
    private final int allowedRetries;
    private final RedactionPolicy redactionPolicy;
    private TraceMetadata metadata;
    private boolean retryDecisionRecorded;
    private boolean retryPolicyTriggered;
    private long retryCount;
    private final DurationAccumulator retryTime = new DurationAccumulator();
    private final Map<String, Long> retriesByAction = new TreeMap<>();
    private final Map<String, Long> retriesByLocator = new TreeMap<>();
    private final Map<String, Long> retriesByException = new TreeMap<>();

    private UiTestLensSession(String name, RetryOutcomePolicy retryOutcomePolicy, int allowedRetries,
                              RedactionPolicy redactionPolicy, TraceRetentionOptions traceRetention) {
        if (allowedRetries < 0) throw new IllegalArgumentException("allowedRetries must not be negative");
        this.retryOutcomePolicy = retryOutcomePolicy == null ? RetryOutcomePolicy.REPORT_ONLY : retryOutcomePolicy;
        this.allowedRetries = allowedRetries;
        this.redactionPolicy = redactionPolicy == null ? RedactionPolicy.defaults() : redactionPolicy;
        this.traceStore = new BoundedTraceStore(traceRetention);
        String id = UUID.randomUUID().toString();
        String safeName = name == null || name.isBlank() ? "Test Lens session" : this.redactionPolicy.redact(name.trim());
        this.metadata = TraceMetadata.builder(id, safeName)
                .status(TraceStatus.STARTED)
                .build();
        addEvent(TraceEvent.started(TraceEventType.SESSION_STARTED, this.metadata.name())
                .toBuilder()
                .attribute("sessionId", id)
                .build());
    }

    public static UiTestLensSession start(String name) {
        return new UiTestLensSession(name, RetryOutcomePolicy.REPORT_ONLY, 0, RedactionPolicy.defaults(),
                TraceRetentionOptions.defaults());
    }

    public static UiTestLensSession start(String name, RetryOutcomePolicy policy, int allowedRetries) {
        return new UiTestLensSession(name, policy, allowedRetries, RedactionPolicy.defaults(),
                TraceRetentionOptions.defaults());
    }

    public static UiTestLensSession start(String name, RetryOutcomePolicy policy, int allowedRetries,
                                          RedactionPolicy redactionPolicy) {
        return new UiTestLensSession(name, policy, allowedRetries, redactionPolicy, TraceRetentionOptions.defaults());
    }

    /** Starts a session with explicit bounded trace retention. @since 0.4.0 */
    public static UiTestLensSession start(String name, RetryOutcomePolicy policy, int allowedRetries,
                                          RedactionPolicy redactionPolicy, TraceRetentionOptions traceRetention) {
        return new UiTestLensSession(name, policy, allowedRetries, redactionPolicy,
                traceRetention == null ? TraceRetentionOptions.defaults() : traceRetention);
    }

    public String id() {
        return metadata.sessionId();
    }

    public synchronized TraceMetadata metadata() {
        return metadata;
    }

    public synchronized List<TraceEvent> events() {
        return traceStore.events();
    }

    public synchronized List<TraceArtifact> artifacts() {
        return traceStore.artifacts();
    }

    public synchronized RetrySummary retrySummary() {
        return new RetrySummary(retryCount, retryTime.value(), retryCount > 0, retryOutcomePolicy,
                retryPolicyTriggered, retriesByAction, retriesByLocator, retriesByException);
    }

    public synchronized TraceEvent addEvent(TraceEvent event) {
        if (event == null) {
            return null;
        }
        TraceEvent safe = redactEvent(event);
        if (safe.type() == TraceEventType.RETRY) recordRetry(safe);
        return traceStore.add(safe);
    }

    public synchronized TraceArtifact attachArtifact(TraceArtifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("artifact must not be null");
        }
        TraceArtifact safe = redactArtifact(artifact);
        TraceArtifact retained = traceStore.addArtifact(safe);
        if (retained == null) return safe;
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

    /**
     * Finalizes the session as passed unless its recovery-retry outcome policy requires failure.
     * Only the first terminal finish call can change the session.
     */
    public synchronized void finishPassed() {
        if (isFinished()) return;
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

    /**
     * Finalizes the session as failed and records the supplied failure when this is the first terminal call.
     * A {@code null} failure still produces a failed session.
     */
    public synchronized void finishFailed(Throwable throwable) {
        if (isFinished()) return;
        recordRetryDecision(retrySummary());
        finish(TraceStatus.FAILED, throwable, "");
    }

    /** Finalizes the session as skipped with the supplied reason when this is the first terminal call. */
    public synchronized void finishSkipped(String reason) {
        if (isFinished()) return;
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
        if (status == TraceStatus.FAILED) {
            addEvent(TraceEvent.builder(TraceEventType.CUSTOM, TraceStatus.FAILED, "Failure freeze")
                    .message(throwable == null ? "Terminal failure" : throwable.getMessage())
                    .failure(throwable == null ? null : TraceFailure.from(throwable, false))
                    .attribute(BoundedTraceStore.FREEZE, "true")
                    .build());
        }
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
        BoundedTraceStore.Snapshot snapshot = traceStore.close(status);
        Map<String, String> labels = new LinkedHashMap<>(metadata.labels());
        labels.putAll(snapshot.labels());
        labels.put("testlens.retention.retryPolicy", retryOutcomePolicy.name());
        labels.put("testlens.retention.allowedRetries", String.valueOf(allowedRetries));
        metadata = metadata.toBuilder().labels(labels).build();
    }

    private boolean isFinished() {
        return metadata.status() != TraceStatus.STARTED;
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

    private void recordRetry(TraceEvent event) {
        retryCount++;
        retryTime.add(event.duration());
        increment(retriesByAction, event.attributes().get("retry.action"));
        increment(retriesByLocator, event.attributes().get("retry.locator"));
        increment(retriesByException, event.attributes().get("retry.exceptionType"));
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

