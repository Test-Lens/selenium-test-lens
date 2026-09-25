package io.github.testlens.core.trace;

import java.util.Objects;

/** Immutable bounds for the per-session trace recorder. @since 0.4.0 */
public final class TraceRetentionOptions {
    private static final int DEFAULT_MAX_EVENTS = 4_096;
    private static final long DEFAULT_MAX_BYTES = 8L * 1024L * 1024L;
    private static final long DEFAULT_MAX_EVENT_BYTES = 256L * 1024L;

    private final int maxEvents;
    private final long maxBytes;
    private final long maxEventBytes;
    private final PassedTraceRetention passedSessionRetention;

    private TraceRetentionOptions(Builder builder) {
        if (builder.maxEvents <= 0) throw new IllegalArgumentException("maxEvents must be greater than zero");
        if (builder.maxBytes <= 0) throw new IllegalArgumentException("maxBytes must be greater than zero");
        if (builder.maxEventBytes <= 0) throw new IllegalArgumentException("maxEventBytes must be greater than zero");
        if (builder.maxEventBytes > builder.maxBytes) {
            throw new IllegalArgumentException("maxEventBytes must not exceed maxBytes");
        }
        this.maxEvents = builder.maxEvents;
        this.maxBytes = builder.maxBytes;
        this.maxEventBytes = builder.maxEventBytes;
        this.passedSessionRetention = Objects.requireNonNull(
                builder.passedSessionRetention, "passedSessionRetention must not be null");
    }

    public static TraceRetentionOptions defaults() { return builder().build(); }
    public static Builder builder() { return new Builder(); }
    public int maxEvents() { return maxEvents; }
    public long maxBytes() { return maxBytes; }
    public long maxEventBytes() { return maxEventBytes; }
    public PassedTraceRetention passedSessionRetention() { return passedSessionRetention; }

    /** Builder for immutable trace-retention options. @since 0.4.0 */
    public static final class Builder {
        private int maxEvents = DEFAULT_MAX_EVENTS;
        private long maxBytes = DEFAULT_MAX_BYTES;
        private long maxEventBytes = DEFAULT_MAX_EVENT_BYTES;
        private PassedTraceRetention passedSessionRetention = PassedTraceRetention.RETAIN_TRACE;

        private Builder() {}

        public Builder maxEvents(int value) { maxEvents = value; return this; }
        public Builder maxBytes(long value) { maxBytes = value; return this; }
        public Builder maxEventBytes(long value) { maxEventBytes = value; return this; }
        public Builder passedSessionRetention(PassedTraceRetention value) {
            passedSessionRetention = Objects.requireNonNull(value, "passedSessionRetention must not be null");
            return this;
        }
        public TraceRetentionOptions build() { return new TraceRetentionOptions(this); }
    }
}
