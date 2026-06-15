package io.github.testlens.selenium.evidence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

final class EvidencePathStrategy {
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH-mm-ss-SSS")
            .withZone(ZoneOffset.UTC);

    private EvidencePathStrategy() {
    }

    static Path screenshotPath(String name, ScreenshotCaptureOptions options) {
        ScreenshotCaptureOptions effectiveOptions = options == null ? ScreenshotCaptureOptions.defaults() : options;
        String prefix = sanitize(effectiveOptions.fileNamePrefix());
        String label = sanitize(name);
        StringBuilder fileName = new StringBuilder();
        if (effectiveOptions.includeTimestamp()) {
            fileName.append(FILE_TIMESTAMP.format(Instant.now())).append('_');
        }
        fileName.append(prefix);
        if (!label.isBlank()) {
            fileName.append('_').append(label);
        }
        fileName.append(".png");
        Path candidate = effectiveOptions.outputDirectory().resolve(fileName.toString());
        if (effectiveOptions.overwriteExisting() || !Files.exists(candidate)) {
            return candidate;
        }
        String base = fileName.substring(0, fileName.length() - 4);
        return effectiveOptions.outputDirectory().resolve(base + "_" + UUID.randomUUID() + ".png");
    }

    static String sanitize(String value) {
        String safe = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        safe = safe.replaceAll("[^a-z0-9._-]+", "-");
        safe = safe.replaceAll("-+", "-");
        safe = safe.replaceAll("(^[-.]+|[-.]+$)", "");
        return safe.isBlank() ? "screenshot" : safe;
    }
}

