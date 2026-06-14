package io.github.mmaciekk111.uitestlens.core.trace.export;

public final class TraceHtmlExportOptions {
    private final String title;
    private final boolean includeJsonPayload;
    private final boolean includeArtifacts;
    private final boolean includeStackTraces;
    private final boolean includeAttributes;
    private final boolean collapsePassedEvents;
    private final int maxMessageLength;

    private TraceHtmlExportOptions(Builder builder) {
        this.title = builder.title == null || builder.title.isBlank() ? "UI Test Lens Trace" : builder.title.trim();
        this.includeJsonPayload = builder.includeJsonPayload;
        this.includeArtifacts = builder.includeArtifacts;
        this.includeStackTraces = builder.includeStackTraces;
        this.includeAttributes = builder.includeAttributes;
        this.collapsePassedEvents = builder.collapsePassedEvents;
        if (builder.maxMessageLength < 0) {
            throw new IllegalArgumentException("maxMessageLength must not be negative");
        }
        this.maxMessageLength = builder.maxMessageLength;
    }

    public static TraceHtmlExportOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String title() {
        return title;
    }

    public boolean includeJsonPayload() {
        return includeJsonPayload;
    }

    public boolean includeArtifacts() {
        return includeArtifacts;
    }

    public boolean includeStackTraces() {
        return includeStackTraces;
    }

    public boolean includeAttributes() {
        return includeAttributes;
    }

    public boolean collapsePassedEvents() {
        return collapsePassedEvents;
    }

    public int maxMessageLength() {
        return maxMessageLength;
    }

    public static final class Builder {
        private String title = "UI Test Lens Trace";
        private boolean includeJsonPayload = true;
        private boolean includeArtifacts = true;
        private boolean includeStackTraces = false;
        private boolean includeAttributes = true;
        private boolean collapsePassedEvents = false;
        private int maxMessageLength = 1000;

        private Builder() {
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder includeJsonPayload(boolean includeJsonPayload) {
            this.includeJsonPayload = includeJsonPayload;
            return this;
        }

        public Builder includeArtifacts(boolean includeArtifacts) {
            this.includeArtifacts = includeArtifacts;
            return this;
        }

        public Builder includeStackTraces(boolean includeStackTraces) {
            this.includeStackTraces = includeStackTraces;
            return this;
        }

        public Builder includeAttributes(boolean includeAttributes) {
            this.includeAttributes = includeAttributes;
            return this;
        }

        public Builder collapsePassedEvents(boolean collapsePassedEvents) {
            this.collapsePassedEvents = collapsePassedEvents;
            return this;
        }

        public Builder maxMessageLength(int maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
            return this;
        }

        public TraceHtmlExportOptions build() {
            return new TraceHtmlExportOptions(this);
        }
    }
}
