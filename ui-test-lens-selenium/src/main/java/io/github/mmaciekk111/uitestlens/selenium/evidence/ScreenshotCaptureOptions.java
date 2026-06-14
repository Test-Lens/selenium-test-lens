package io.github.mmaciekk111.uitestlens.selenium.evidence;

import java.nio.file.Path;

public final class ScreenshotCaptureOptions {
    private final Path outputDirectory;
    private final String fileNamePrefix;
    private final boolean includeTimestamp;
    private final boolean overwriteExisting;
    private final boolean attachToSession;

    private ScreenshotCaptureOptions(Builder builder) {
        this.outputDirectory = builder.outputDirectory == null
                ? Path.of("target/ui-test-lens/screenshots")
                : builder.outputDirectory;
        this.fileNamePrefix = builder.fileNamePrefix == null || builder.fileNamePrefix.isBlank()
                ? "screenshot"
                : builder.fileNamePrefix.trim();
        this.includeTimestamp = builder.includeTimestamp;
        this.overwriteExisting = builder.overwriteExisting;
        this.attachToSession = builder.attachToSession;
    }

    public static ScreenshotCaptureOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Path outputDirectory() {
        return outputDirectory;
    }

    public String fileNamePrefix() {
        return fileNamePrefix;
    }

    public boolean includeTimestamp() {
        return includeTimestamp;
    }

    public boolean overwriteExisting() {
        return overwriteExisting;
    }

    public boolean attachToSession() {
        return attachToSession;
    }

    public static final class Builder {
        private Path outputDirectory = Path.of("target/ui-test-lens/screenshots");
        private String fileNamePrefix = "screenshot";
        private boolean includeTimestamp = true;
        private boolean overwriteExisting = false;
        private boolean attachToSession = true;

        private Builder() {
        }

        public Builder outputDirectory(Path outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        public Builder fileNamePrefix(String fileNamePrefix) {
            this.fileNamePrefix = fileNamePrefix;
            return this;
        }

        public Builder includeTimestamp(boolean includeTimestamp) {
            this.includeTimestamp = includeTimestamp;
            return this;
        }

        public Builder overwriteExisting(boolean overwriteExisting) {
            this.overwriteExisting = overwriteExisting;
            return this;
        }

        public Builder attachToSession(boolean attachToSession) {
            this.attachToSession = attachToSession;
            return this;
        }

        public ScreenshotCaptureOptions build() {
            return new ScreenshotCaptureOptions(this);
        }
    }
}
