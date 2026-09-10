package io.github.testlens.selenium.evidence;

import java.nio.file.Path;

/** Configures naming, publication, attachment, mode, and resource limits for screenshot evidence. */
public final class ScreenshotCaptureOptions {
    /** Default maximum number of pixels allocated for a stitched full-page image. */
    public static final long DEFAULT_MAX_PIXEL_COUNT = 40_000_000L;
    /** Default maximum number of viewport PNG tiles used by one full-page capture. */
    public static final int DEFAULT_MAX_TILE_COUNT = 200;
    private final Path outputDirectory;
    private final String fileNamePrefix;
    private final boolean includeTimestamp;
    private final boolean overwriteExisting;
    private final boolean attachToSession;
    private final ScreenshotCaptureMode captureMode;
    private final long maxPixelCount;
    private final int maxTileCount;

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
        this.captureMode = builder.captureMode;
        this.maxPixelCount = builder.maxPixelCount;
        this.maxTileCount = builder.maxTileCount;
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

    /** Returns the requested screenshot area; the default is {@link ScreenshotCaptureMode#VIEWPORT}. */
    public ScreenshotCaptureMode captureMode() { return captureMode; }

    /** Returns the maximum number of pixels allowed in a stitched image. */
    public long maxPixelCount() { return maxPixelCount; }

    /** Returns the maximum number of viewport tiles allowed for one capture. */
    public int maxTileCount() { return maxTileCount; }

    public static final class Builder {
        private Path outputDirectory = Path.of("target/ui-test-lens/screenshots");
        private String fileNamePrefix = "screenshot";
        private boolean includeTimestamp = true;
        private boolean overwriteExisting = false;
        private boolean attachToSession = true;
        private ScreenshotCaptureMode captureMode = ScreenshotCaptureMode.VIEWPORT;
        private long maxPixelCount = DEFAULT_MAX_PIXEL_COUNT;
        private int maxTileCount = DEFAULT_MAX_TILE_COUNT;

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

        /**
         * Selects viewport or portable full-page capture.
         * Screenshot pixels are not transformed by central text redaction.
         *
         * @param captureMode non-null capture mode
         * @return this builder
         */
        public Builder captureMode(ScreenshotCaptureMode captureMode) {
            if (captureMode == null) throw new IllegalArgumentException("captureMode must not be null");
            this.captureMode = captureMode;
            return this;
        }

        /**
         * Sets the maximum number of pixels allocated for a stitched result.
         *
         * @param maxPixelCount positive pixel limit
         * @return this builder
         */
        public Builder maxPixelCount(long maxPixelCount) {
            if (maxPixelCount < 1) throw new IllegalArgumentException("maxPixelCount must be positive");
            this.maxPixelCount = maxPixelCount;
            return this;
        }

        /**
         * Sets the maximum number of viewport tiles captured for one full-page result.
         *
         * @param maxTileCount positive tile limit
         * @return this builder
         */
        public Builder maxTileCount(int maxTileCount) {
            if (maxTileCount < 1) throw new IllegalArgumentException("maxTileCount must be positive");
            this.maxTileCount = maxTileCount;
            return this;
        }

        public ScreenshotCaptureOptions build() {
            return new ScreenshotCaptureOptions(this);
        }
    }
}

