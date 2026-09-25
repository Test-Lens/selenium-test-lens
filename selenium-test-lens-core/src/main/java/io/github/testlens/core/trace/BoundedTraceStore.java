package io.github.testlens.core.trace;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Per-session bounded storage behind the existing trace pipeline. */
final class BoundedTraceStore {
    static final String WATERMARK = "testlens.recorder.failureWatermark";
    static final String FREEZE = "testlens.recorder.failureFreeze";
    private static final String OPERATION_ID = "metadata.operationId";
    private static final long EVENT_OVERHEAD = 96L;
    private static final Pattern GRAPHEME = Pattern.compile("\\X");
    private static final Set<TraceEventType> DIAGNOSTIC_TYPES = EnumSet.of(
            TraceEventType.CUSTOM, TraceEventType.LOCATOR_RESOLVE, TraceEventType.ACTIONABILITY_CHECK,
            TraceEventType.NETWORK_EVENT, TraceEventType.NETWORK_WAIT);

    private final TraceRetentionOptions options;
    private final List<RetainedEvent> events = new ArrayList<>();
    private final List<RetainedArtifact> artifacts = new ArrayList<>();
    private long retainedBytes;
    private long evictedEvents;
    private long evictedBytes;
    private long countEvictions;
    private long byteEvictions;
    private long oversizedEvents;
    private long truncatedEvents;
    private long oversizedDrops;
    private long lateDrops;
    private boolean partialArtifact;
    private State state = State.RECORDING;
    private int postFailureEvents;
    private long postFailureBytes;
    private Snapshot snapshot;

    BoundedTraceStore(TraceRetentionOptions options) {
        this.options = options == null ? TraceRetentionOptions.defaults() : options;
    }

    synchronized TraceEvent add(TraceEvent input) {
        if (input == null) return null;
        if (state == State.CLOSED || state == State.FINAL_SNAPSHOT) {
            lateDrops++;
            return null;
        }
        boolean watermark = "true".equals(input.attributes().get(WATERMARK));
        boolean freeze = "true".equals(input.attributes().get(FREEZE));
        if (watermark) protectRecentContext();
        if (freeze) {
            protectRecentContext();
            state = State.FAILURE_FROZEN;
        }
        TraceEvent event = normalize(input);
        if (event == null) return null;
        if (event.type() == TraceEventType.FAILURE_BUNDLE
                && Set.of("FAILED", "UNSUPPORTED", "TRUNCATED", "SKIPPED_TOO_LARGE")
                .contains(event.attributes().getOrDefault("componentStatus", ""))) {
            partialArtifact = true;
        }
        long size = TraceSizeEstimator.eventBytes(event);
        Kind kind = classify(event, watermark, freeze);
        if (state == State.FAILURE_FROZEN && !kind.alwaysAccepted) {
            int eventBudget = Math.max(1, Math.min(64, options.maxEvents() / 8));
            long byteBudget = Math.max(options.maxEventBytes(), options.maxBytes() / 8);
            if (postFailureEvents >= eventBudget || postFailureBytes + size > byteBudget) {
                lateDrops++;
                return null;
            }
            postFailureEvents++;
            postFailureBytes += size;
        }
        RetainedEvent retained = new RetainedEvent(event, size, kind,
                operationId(event), kind.protectedEvent || watermark || freeze);
        events.add(retained);
        retainedBytes += size;
        enforceBounds(retained);
        return retained.retained ? retained.event : null;
    }

    synchronized TraceArtifact addArtifact(TraceArtifact artifact) {
        if (artifact == null) return null;
        if (state == State.CLOSED || state == State.FINAL_SNAPSHOT) {
            lateDrops++;
            return null;
        }
        long size = TraceSizeEstimator.artifactBytes(artifact);
        if (size > options.maxEventBytes()) {
            oversizedEvents++;
            TraceArtifact compact = compactArtifact(artifact, options.maxEventBytes());
            size = TraceSizeEstimator.artifactBytes(compact);
            if (size > options.maxEventBytes()) {
                oversizedDrops++;
                return null;
            }
            artifact = compact;
            truncatedEvents++;
        }
        RetainedArtifact retained = new RetainedArtifact(artifact, size);
        artifacts.add(retained);
        retainedBytes += size;
        enforceBounds(null);
        return retained.retained ? retained.artifact : null;
    }

    synchronized List<TraceEvent> events() {
        if (snapshot != null) return snapshot.events;
        return List.copyOf(events.stream().map(value -> value.event).toList());
    }

    synchronized List<TraceArtifact> artifacts() {
        if (snapshot != null) return snapshot.artifacts;
        return List.copyOf(artifacts.stream().map(value -> value.artifact).toList());
    }

    synchronized Snapshot close(TraceStatus terminalStatus) {
        if (snapshot != null) return snapshot;
        state = State.FINAL_SNAPSHOT;
        List<TraceEvent> retainedEvents;
        if (terminalStatus == TraceStatus.PASSED
                && options.passedSessionRetention() == PassedTraceRetention.SUMMARY_ONLY) {
            retainedEvents = events.stream()
                    .filter(value -> value.event.type() == TraceEventType.SESSION_STARTED
                            || value.event.type() == TraceEventType.SESSION_FINISHED
                            || value.event.type() == TraceEventType.RETRY_SUMMARY)
                    .map(value -> value.event)
                    .toList();
        } else {
            retainedEvents = events.stream().map(value -> value.event).toList();
        }
        Instant closedAt = Instant.now();
        long snapshotBytes = retainedEvents.stream().mapToLong(TraceSizeEstimator::eventBytes).sum()
                + artifacts.stream().mapToLong(value -> value.size).sum();
        Map<String, String> labels = retentionLabels(retainedEvents.size(), snapshotBytes, closedAt);
        snapshot = new Snapshot(List.copyOf(retainedEvents),
                List.copyOf(artifacts.stream().map(value -> value.artifact).toList()), labels, closedAt);
        events.clear();
        artifacts.clear();
        state = State.CLOSED;
        return snapshot;
    }

    private TraceEvent normalize(TraceEvent input) {
        long original = TraceSizeEstimator.eventBytes(input);
        if (original <= options.maxEventBytes()) return input;
        oversizedEvents++;
        TraceEvent.Builder builder = input.toBuilder();
        TraceFailure failure = input.failure();
        if (failure != null) {
            builder.failure(new TraceFailure(failure.message(), failure.exceptionType(), "", Map.of()));
        }
        builder.artifacts(List.of());
        Map<String, String> essential = new LinkedHashMap<>();
        for (String key : List.of(OPERATION_ID, "metadata.hudCategory", "metadata.hudStatus",
                "sessionId", WATERMARK, FREEZE)) {
            String value = input.attributes().get(key);
            if (value != null) essential.put(key, value);
        }
        builder.attributes(essential);
        TraceEvent reduced = builder.build();
        long fixed = TraceSizeEstimator.eventBytes(reduced.toBuilder().name("").message("").build());
        long available = Math.max(0, options.maxEventBytes() - fixed);
        String name = truncateUtf8(input.name(), available / 3);
        String message = truncateUtf8(input.message(), Math.max(0, available - utf8(name)));
        reduced = reduced.toBuilder().name(name).message(message).build();
        if (TraceSizeEstimator.eventBytes(reduced) <= options.maxEventBytes()) {
            truncatedEvents++;
            return reduced;
        }
        TraceEvent minimal = TraceEvent.builder(input.type(), input.status(), "")
                .id(input.id()).timestamp(input.timestamp()).duration(input.duration())
                .attributes(essential).build();
        if (TraceSizeEstimator.eventBytes(minimal) <= options.maxEventBytes()) {
            truncatedEvents++;
            return minimal;
        }
        oversizedDrops++;
        return null;
    }

    private void enforceBounds(RetainedEvent incoming) {
        while (events.size() + artifacts.size() > options.maxEvents() || retainedBytes > options.maxBytes()) {
            boolean countExceeded = events.size() + artifacts.size() > options.maxEvents();
            int diagnostic = oldestEvent(Kind.DIAGNOSTIC, false);
            if (diagnostic >= 0) {
                evictEventGroup(diagnostic, countExceeded);
                continue;
            }
            int operation = oldestCompletedOperation();
            if (operation >= 0) {
                evictEventGroup(operation, countExceeded);
                continue;
            }
            int ordinary = oldestUnprotectedEvent();
            if (ordinary >= 0) {
                evictEventGroup(ordinary, countExceeded);
                continue;
            }
            if (!artifacts.isEmpty()) {
                RetainedArtifact removed = artifacts.remove(0);
                removed.retained = false;
                retainedBytes -= removed.size;
                recordEviction(removed.size, countExceeded);
                continue;
            }
            int compactableProtected = oldestCompactableProtected(incoming);
            if (compactableProtected >= 0) {
                removeEvent(events.get(compactableProtected), countExceeded);
                continue;
            }
            if (incoming != null && events.remove(incoming)) {
                incoming.retained = false;
                retainedBytes -= incoming.size;
                recordEviction(incoming.size, countExceeded);
            }
            break;
        }
    }

    private void evictEventGroup(int index, boolean countExceeded) {
        RetainedEvent candidate = events.get(index);
        if (!candidate.operationId.isBlank()) {
            List<RetainedEvent> group = events.stream()
                    .filter(value -> candidate.operationId.equals(value.operationId) && !value.protectedEvent)
                    .toList();
            if (!group.isEmpty()) {
                for (RetainedEvent value : group) removeEvent(value, countExceeded);
                return;
            }
        }
        removeEvent(candidate, countExceeded);
    }

    private void removeEvent(RetainedEvent value, boolean countExceeded) {
        if (events.remove(value)) {
            value.retained = false;
            retainedBytes -= value.size;
            recordEviction(value.size, countExceeded);
        }
    }

    private void recordEviction(long size, boolean countExceeded) {
        evictedEvents++;
        evictedBytes += size;
        if (countExceeded) countEvictions++; else byteEvictions++;
    }

    private int oldestEvent(Kind kind, boolean includeProtected) {
        for (int i = 0; i < events.size(); i++) {
            RetainedEvent value = events.get(i);
            if (value.kind == kind && (includeProtected || !value.protectedEvent)) return i;
        }
        return -1;
    }

    private int oldestCompletedOperation() {
        for (int i = 0; i < events.size(); i++) {
            RetainedEvent value = events.get(i);
            if (value.protectedEvent || value.operationId.isBlank()) continue;
            boolean terminal = events.stream().anyMatch(other -> value.operationId.equals(other.operationId)
                    && Set.of(TraceStatus.PASSED, TraceStatus.FAILED, TraceStatus.ERROR,
                    TraceStatus.SKIPPED, TraceStatus.WARNING).contains(other.event.status()));
            if (terminal) return i;
        }
        return -1;
    }

    private int oldestUnprotectedEvent() {
        for (int i = 0; i < events.size(); i++) if (!events.get(i).protectedEvent) return i;
        return -1;
    }

    private int oldestCompactableProtected(RetainedEvent incoming) {
        for (int i = 0; i < events.size(); i++) {
            RetainedEvent value = events.get(i);
            if (value == incoming) continue;
            if (value.event.type() != TraceEventType.SESSION_FINISHED) return i;
        }
        return -1;
    }

    private void protectRecentContext() {
        int allowance = Math.max(1, Math.min(64, options.maxEvents() / 4));
        int start = Math.max(0, events.size() - allowance);
        for (int i = start; i < events.size(); i++) events.get(i).protectedEvent = true;
    }

    private Kind classify(TraceEvent event, boolean watermark, boolean freeze) {
        if (event.type() == TraceEventType.SESSION_FINISHED || freeze) return Kind.TERMINAL_ROOT;
        if (watermark || event.failure() != null || event.status() == TraceStatus.FAILED
                || event.status() == TraceStatus.ERROR) return Kind.FAILURE_CONTEXT;
        if (!operationId(event).isBlank() || event.type().name().startsWith("ACTION")
                || event.type().name().startsWith("ASSERTION") || event.type().name().startsWith("STEP")) {
            return Kind.OPERATION;
        }
        return DIAGNOSTIC_TYPES.contains(event.type()) ? Kind.DIAGNOSTIC : Kind.OPERATION;
    }

    private Map<String, String> retentionLabels(int retainedEventCount, long snapshotBytes, Instant closedAt) {
        List<String> issues = new ArrayList<>();
        if (countEvictions > 0) issues.add("COUNT_EVICTION");
        if (byteEvictions > 0) issues.add("BYTE_EVICTION");
        if (truncatedEvents > 0) issues.add("OVERSIZED_TRUNCATION");
        if (oversizedDrops > 0) issues.add("OVERSIZED_DROP");
        if (lateDrops > 0) issues.add("LATE_EVENT_DROPPED");
        if (partialArtifact) issues.add("PARTIAL_ARTIFACT");
        Map<String, String> out = new LinkedHashMap<>();
        out.put("testlens.retention.schemaVersion", "1");
        out.put("testlens.retention.complete", String.valueOf(issues.isEmpty()));
        out.put("testlens.retention.retainedEvents", String.valueOf(retainedEventCount));
        out.put("testlens.retention.retainedEstimatedBytes", String.valueOf(snapshotBytes));
        out.put("testlens.retention.evictedEvents", String.valueOf(evictedEvents));
        out.put("testlens.retention.evictedEstimatedBytes", String.valueOf(evictedBytes));
        out.put("testlens.retention.countEvictions", String.valueOf(countEvictions));
        out.put("testlens.retention.byteEvictions", String.valueOf(byteEvictions));
        out.put("testlens.retention.oversizedEvents", String.valueOf(oversizedEvents));
        out.put("testlens.retention.truncatedEvents", String.valueOf(truncatedEvents));
        out.put("testlens.retention.lateEventsDropped", String.valueOf(lateDrops));
        out.put("testlens.retention.snapshotClosedAt", closedAt.toString());
        out.put("testlens.retention.issues", String.join(",", issues));
        out.put("testlens.retention.passedSessionRetention", options.passedSessionRetention().name());
        return Map.copyOf(out);
    }

    private static String operationId(TraceEvent event) {
        return event.attributes().getOrDefault(OPERATION_ID,
                event.attributes().getOrDefault("operationId", ""));
    }

    private static TraceArtifact compactArtifact(TraceArtifact artifact, long maxBytes) {
        long share = Math.max(0, maxBytes / 5);
        return TraceArtifact.of(truncateUtf8(artifact.name(), share), artifact.type(),
                truncateUtf8(artifact.path(), share), truncateUtf8(artifact.url(), share),
                truncateUtf8(artifact.mediaType(), share), artifact.createdAt(), Map.of());
    }

    static String truncateUtf8(String value, long maxBytes) {
        if (value == null || value.isEmpty() || maxBytes <= 0) return "";
        if (utf8(value) <= maxBytes) return value;
        Matcher matcher = GRAPHEME.matcher(value);
        StringBuilder out = new StringBuilder();
        long bytes = 0;
        while (matcher.find()) {
            String cluster = matcher.group();
            long next = utf8(cluster);
            if (bytes + next > maxBytes) break;
            out.append(cluster);
            bytes += next;
        }
        return out.toString();
    }

    private static long utf8(String value) {
        return value == null ? 0L : value.getBytes(StandardCharsets.UTF_8).length;
    }

    record Snapshot(List<TraceEvent> events, List<TraceArtifact> artifacts,
                    Map<String, String> labels, Instant closedAt) { }

    private enum State { RECORDING, FAILURE_FROZEN, FINAL_SNAPSHOT, CLOSED }
    private enum Kind {
        DIAGNOSTIC(false, false), OPERATION(false, false), FAILURE_CONTEXT(true, true), TERMINAL_ROOT(true, true);
        private final boolean protectedEvent;
        private final boolean alwaysAccepted;
        Kind(boolean protectedEvent, boolean alwaysAccepted) {
            this.protectedEvent = protectedEvent;
            this.alwaysAccepted = alwaysAccepted;
        }
    }
    private static final class RetainedEvent {
        private final TraceEvent event;
        private final long size;
        private final Kind kind;
        private final String operationId;
        private boolean protectedEvent;
        private boolean retained = true;
        private RetainedEvent(TraceEvent event, long size, Kind kind, String operationId, boolean protectedEvent) {
            this.event = event; this.size = size; this.kind = kind; this.operationId = operationId;
            this.protectedEvent = protectedEvent;
        }
    }
    private static final class RetainedArtifact {
        private final TraceArtifact artifact;
        private final long size;
        private boolean retained = true;
        private RetainedArtifact(TraceArtifact artifact, long size) {
            this.artifact = artifact;
            this.size = size;
        }
    }

    /** Deterministic retained-safe UTF-8 estimator; never serializes JSON or touches a driver/filesystem. */
    static final class TraceSizeEstimator {
        private TraceSizeEstimator() { }
        static long eventBytes(TraceEvent event) {
            long size = EVENT_OVERHEAD + utf8(event.id()) + utf8(event.name()) + utf8(event.message())
                    + utf8(event.parentId()) + 32L;
            for (Map.Entry<String, String> entry : event.attributes().entrySet()) {
                size += 16L + utf8(entry.getKey()) + utf8(entry.getValue());
            }
            if (event.failure() != null) {
                size += 48L + utf8(event.failure().message()) + utf8(event.failure().exceptionType())
                        + utf8(event.failure().stackTrace());
                for (Map.Entry<String, String> entry : event.failure().details().entrySet()) {
                    size += 16L + utf8(entry.getKey()) + utf8(entry.getValue());
                }
            }
            for (TraceArtifact artifact : event.artifacts()) size += artifactBytes(artifact);
            return size;
        }
        static long artifactBytes(TraceArtifact artifact) {
            long size = 64L + utf8(artifact.name()) + utf8(artifact.path()) + utf8(artifact.url())
                    + utf8(artifact.mediaType());
            for (Map.Entry<String, String> entry : artifact.metadata().entrySet()) {
                size += 16L + utf8(entry.getKey()) + utf8(entry.getValue());
            }
            return size;
        }
    }
}
