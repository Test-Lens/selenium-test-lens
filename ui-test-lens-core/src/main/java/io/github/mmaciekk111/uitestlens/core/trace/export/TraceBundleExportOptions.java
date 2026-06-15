package io.github.mmaciekk111.uitestlens.core.trace.export;

import java.nio.file.Path;

/**
 * Options for portable Selenium Test Lens report ZIP bundles.
 */
public final class TraceBundleExportOptions {
    private final boolean includeStackTraces;
    private final boolean includeArtifactMetadata;
    private final boolean includeMissingArtifacts;
    private final boolean copyArtifacts;
    private final String bundleName;
    private final Path outputDirectory;
    private final HtmlReportTheme htmlTheme;

    private TraceBundleExportOptions(Builder builder) {
        this.includeStackTraces = builder.includeStackTraces;
        this.includeArtifactMetadata = builder.includeArtifactMetadata;
        this.includeMissingArtifacts = builder.includeMissingArtifacts;
        this.copyArtifacts = builder.copyArtifacts;
        this.bundleName = builder.bundleName == null || builder.bundleName.isBlank()
                ? "Selenium Test Lens Report"
                : builder.bundleName.trim();
        this.outputDirectory = builder.outputDirectory == null
                ? TraceReportSupport.DEFAULT_REPORT_DIRECTORY
                : builder.outputDirectory;
        this.htmlTheme = builder.htmlTheme == null ? HtmlReportTheme.AUTO : builder.htmlTheme;
    }

    public static TraceBundleExportOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean includeStackTraces() {
        return includeStackTraces;
    }

    public boolean includeArtifactMetadata() {
        return includeArtifactMetadata;
    }

    public boolean includeMissingArtifacts() {
        return includeMissingArtifacts;
    }

    public boolean copyArtifacts() {
        return copyArtifacts;
    }

    public String bundleName() {
        return bundleName;
    }

    public Path outputDirectory() {
        return outputDirectory;
    }

    public HtmlReportTheme htmlTheme() {
        return htmlTheme;
    }

    public static final class Builder {
        private boolean includeStackTraces = true;
        private boolean includeArtifactMetadata = true;
        private boolean includeMissingArtifacts = true;
        private boolean copyArtifacts = true;
        private String bundleName = "Selenium Test Lens Report";
        private Path outputDirectory = TraceReportSupport.DEFAULT_REPORT_DIRECTORY;
        private HtmlReportTheme htmlTheme = HtmlReportTheme.AUTO;

        private Builder() {
        }

        public Builder includeStackTraces(boolean includeStackTraces) {
            this.includeStackTraces = includeStackTraces;
            return this;
        }

        public Builder includeArtifactMetadata(boolean includeArtifactMetadata) {
            this.includeArtifactMetadata = includeArtifactMetadata;
            return this;
        }

        public Builder includeMissingArtifacts(boolean includeMissingArtifacts) {
            this.includeMissingArtifacts = includeMissingArtifacts;
            return this;
        }

        public Builder copyArtifacts(boolean copyArtifacts) {
            this.copyArtifacts = copyArtifacts;
            return this;
        }

        public Builder bundleName(String bundleName) {
            this.bundleName = bundleName;
            return this;
        }

        public Builder outputDirectory(Path outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        public Builder htmlTheme(HtmlReportTheme htmlTheme) {
            this.htmlTheme = htmlTheme;
            return this;
        }

        public TraceBundleExportOptions build() {
            return new TraceBundleExportOptions(this);
        }
    }
}
