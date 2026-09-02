package io.github.testlens.selenium.evidence;

/** Controls best-effort evidence collected when a Test Lens session finishes as failed. */
public final class FailureBundleOptions {
    public static final long DEFAULT_MAX_TEXT_ARTIFACT_BYTES = 5L * 1024L * 1024L;
    public static final int DEFAULT_MAX_CONSOLE_ENTRIES = 1_000;

    private final boolean enabled;
    private final boolean diagnosticScreenshot;
    private final boolean cleanScreenshot;
    private final boolean context;
    private final boolean diagnostics;
    private final boolean pageSource;
    private final boolean browserConsole;
    private final boolean networkSummary;
    private final boolean runtimeMetadata;
    private final boolean configurationSnapshot;
    private final boolean zipArchive;
    private final long maxTextArtifactBytes;
    private final int maxConsoleEntries;

    private FailureBundleOptions(Builder builder) {
        enabled = builder.enabled;
        diagnosticScreenshot = builder.diagnosticScreenshot;
        cleanScreenshot = builder.cleanScreenshot;
        context = builder.context;
        diagnostics = builder.diagnostics;
        pageSource = builder.pageSource;
        browserConsole = builder.browserConsole;
        networkSummary = builder.networkSummary;
        runtimeMetadata = builder.runtimeMetadata;
        configurationSnapshot = builder.configurationSnapshot;
        zipArchive = builder.zipArchive;
        maxTextArtifactBytes = builder.maxTextArtifactBytes;
        maxConsoleEntries = builder.maxConsoleEntries;
    }

    public static FailureBundleOptions defaults() { return builder().build(); }

    /** Preset that explicitly enables potentially sensitive page source and browser console capture. */
    public static FailureBundleOptions complete() {
        return builder().pageSource(true).browserConsole(true).build();
    }

    public static Builder builder() { return new Builder(); }
    public boolean enabled() { return enabled; }
    public boolean diagnosticScreenshot() { return diagnosticScreenshot; }
    public boolean cleanScreenshot() { return cleanScreenshot; }
    public boolean context() { return context; }
    public boolean diagnostics() { return diagnostics; }
    public boolean pageSource() { return pageSource; }
    public boolean browserConsole() { return browserConsole; }
    public boolean networkSummary() { return networkSummary; }
    public boolean runtimeMetadata() { return runtimeMetadata; }
    public boolean configurationSnapshot() { return configurationSnapshot; }
    public boolean zipArchive() { return zipArchive; }
    public long maxTextArtifactBytes() { return maxTextArtifactBytes; }
    public int maxConsoleEntries() { return maxConsoleEntries; }

    public static final class Builder {
        private boolean enabled = true;
        private boolean diagnosticScreenshot = true;
        private boolean cleanScreenshot = true;
        private boolean context = true;
        private boolean diagnostics = true;
        private boolean pageSource;
        private boolean browserConsole;
        private boolean networkSummary = true;
        private boolean runtimeMetadata = true;
        private boolean configurationSnapshot = true;
        private boolean zipArchive = true;
        private long maxTextArtifactBytes = DEFAULT_MAX_TEXT_ARTIFACT_BYTES;
        private int maxConsoleEntries = DEFAULT_MAX_CONSOLE_ENTRIES;

        private Builder() {}
        public Builder enabled(boolean value) { enabled = value; return this; }
        public Builder diagnosticScreenshot(boolean value) { diagnosticScreenshot = value; return this; }
        public Builder cleanScreenshot(boolean value) { cleanScreenshot = value; return this; }
        public Builder context(boolean value) { context = value; return this; }
        public Builder diagnostics(boolean value) { diagnostics = value; return this; }
        public Builder pageSource(boolean value) { pageSource = value; return this; }
        public Builder browserConsole(boolean value) { browserConsole = value; return this; }
        public Builder networkSummary(boolean value) { networkSummary = value; return this; }
        public Builder runtimeMetadata(boolean value) { runtimeMetadata = value; return this; }
        public Builder configurationSnapshot(boolean value) { configurationSnapshot = value; return this; }
        public Builder zipArchive(boolean value) { zipArchive = value; return this; }
        public Builder maxTextArtifactBytes(long value) {
            if (value < 1) throw new IllegalArgumentException("maxTextArtifactBytes must be positive");
            maxTextArtifactBytes = value;
            return this;
        }
        public Builder maxConsoleEntries(int value) {
            if (value < 0) throw new IllegalArgumentException("maxConsoleEntries must not be negative");
            maxConsoleEntries = value;
            return this;
        }
        public FailureBundleOptions build() { return new FailureBundleOptions(this); }
    }
}
