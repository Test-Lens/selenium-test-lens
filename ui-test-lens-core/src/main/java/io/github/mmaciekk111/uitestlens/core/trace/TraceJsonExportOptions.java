package io.github.mmaciekk111.uitestlens.core.trace;

import java.nio.file.Path;

/**
 * Options for machine-readable JSON trace report export.
 */
public final class TraceJsonExportOptions {
    private final boolean includeStackTraces;
    private final boolean includeArtifactMetadata;
    private final boolean includeMissingArtifacts;
    private final Path artifactBaseDirectory;

    private TraceJsonExportOptions(Builder builder) {
        this.includeStackTraces = builder.includeStackTraces;
        this.includeArtifactMetadata = builder.includeArtifactMetadata;
        this.includeMissingArtifacts = builder.includeMissingArtifacts;
        this.artifactBaseDirectory = builder.artifactBaseDirectory;
    }

    public static TraceJsonExportOptions defaults() {
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

    public Path artifactBaseDirectory() {
        return artifactBaseDirectory;
    }

    public Builder toBuilder() {
        return builder()
                .includeStackTraces(includeStackTraces)
                .includeArtifactMetadata(includeArtifactMetadata)
                .includeMissingArtifacts(includeMissingArtifacts)
                .artifactBaseDirectory(artifactBaseDirectory);
    }

    public static final class Builder {
        private boolean includeStackTraces = true;
        private boolean includeArtifactMetadata = true;
        private boolean includeMissingArtifacts = true;
        private Path artifactBaseDirectory;

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

        public Builder artifactBaseDirectory(Path artifactBaseDirectory) {
            this.artifactBaseDirectory = artifactBaseDirectory;
            return this;
        }

        public TraceJsonExportOptions build() {
            return new TraceJsonExportOptions(this);
        }
    }
}
