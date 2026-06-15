package io.github.testlens.core.trace.export;

public final class TraceHtmlExportOptions {
    private final String title;
    private final boolean includeJsonPayload;
    private final boolean includeArtifacts;
    private final boolean includeStackTraces;
    private final boolean includeAttributes;
    private final boolean collapsePassedEvents;
    private final boolean groupTimelineByCategory;
    private final boolean includeEventTypeSummary;
    private final boolean includeFailureSummary;
    private final boolean includeArtifactPreview;
    private final boolean includeDurationSummary;
    private final boolean compactTimeline;
    private final HtmlReportTheme theme;
    private final int maxMessageLength;

    private TraceHtmlExportOptions(Builder builder) {
        this.title = builder.title == null || builder.title.isBlank() ? "Selenium Test Lens Trace" : builder.title.trim();
        this.includeJsonPayload = builder.includeJsonPayload;
        this.includeArtifacts = builder.includeArtifacts;
        this.includeStackTraces = builder.includeStackTraces;
        this.includeAttributes = builder.includeAttributes;
        this.collapsePassedEvents = builder.collapsePassedEvents;
        this.groupTimelineByCategory = builder.groupTimelineByCategory;
        this.includeEventTypeSummary = builder.includeEventTypeSummary;
        this.includeFailureSummary = builder.includeFailureSummary;
        this.includeArtifactPreview = builder.includeArtifactPreview;
        this.includeDurationSummary = builder.includeDurationSummary;
        this.compactTimeline = builder.compactTimeline;
        this.theme = builder.theme == null ? HtmlReportTheme.AUTO : builder.theme;
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

    public boolean groupTimelineByCategory() {
        return groupTimelineByCategory;
    }

    public boolean includeEventTypeSummary() {
        return includeEventTypeSummary;
    }

    public boolean includeFailureSummary() {
        return includeFailureSummary;
    }

    public boolean includeArtifactPreview() {
        return includeArtifactPreview;
    }

    public boolean includeDurationSummary() {
        return includeDurationSummary;
    }

    public boolean compactTimeline() {
        return compactTimeline;
    }

    public HtmlReportTheme theme() {
        return theme;
    }

    public int maxMessageLength() {
        return maxMessageLength;
    }

    public static final class Builder {
        private String title = "Selenium Test Lens Trace";
        private boolean includeJsonPayload = true;
        private boolean includeArtifacts = true;
        private boolean includeStackTraces = false;
        private boolean includeAttributes = true;
        private boolean collapsePassedEvents = false;
        private boolean groupTimelineByCategory = true;
        private boolean includeEventTypeSummary = true;
        private boolean includeFailureSummary = true;
        private boolean includeArtifactPreview = true;
        private boolean includeDurationSummary = true;
        private boolean compactTimeline = false;
        private HtmlReportTheme theme = HtmlReportTheme.AUTO;
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

        public Builder groupTimelineByCategory(boolean groupTimelineByCategory) {
            this.groupTimelineByCategory = groupTimelineByCategory;
            return this;
        }

        public Builder includeEventTypeSummary(boolean includeEventTypeSummary) {
            this.includeEventTypeSummary = includeEventTypeSummary;
            return this;
        }

        public Builder includeFailureSummary(boolean includeFailureSummary) {
            this.includeFailureSummary = includeFailureSummary;
            return this;
        }

        public Builder includeArtifactPreview(boolean includeArtifactPreview) {
            this.includeArtifactPreview = includeArtifactPreview;
            return this;
        }

        public Builder includeDurationSummary(boolean includeDurationSummary) {
            this.includeDurationSummary = includeDurationSummary;
            return this;
        }

        public Builder compactTimeline(boolean compactTimeline) {
            this.compactTimeline = compactTimeline;
            return this;
        }

        public Builder theme(HtmlReportTheme theme) {
            this.theme = theme;
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
