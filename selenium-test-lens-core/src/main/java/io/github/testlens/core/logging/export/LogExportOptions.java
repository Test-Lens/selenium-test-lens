package io.github.testlens.core.logging.export;

public record LogExportOptions(
        boolean includeMetadata,
        boolean includeThrowable,
        boolean prettyPrint,
        int maxFieldLength
) {
    private static final int DEFAULT_MAX_FIELD_LENGTH = 500;

    public LogExportOptions {
        if (maxFieldLength <= 0) {
            maxFieldLength = DEFAULT_MAX_FIELD_LENGTH;
        }
    }

    public static LogExportOptions defaults() {
        return new LogExportOptions(true, true, true, DEFAULT_MAX_FIELD_LENGTH);
    }

    public static LogExportOptions compact() {
        return new LogExportOptions(false, false, false, DEFAULT_MAX_FIELD_LENGTH);
    }
}
