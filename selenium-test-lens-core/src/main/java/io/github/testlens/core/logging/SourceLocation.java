package io.github.testlens.core.logging;

/**
 * Logical source call site for a user-facing Test Lens operation.
 *
 * <p>The model deliberately contains no absolute path or IDE URI, so it is safe to retain in
 * trace/log metadata. Local path resolution is a separate, lazy concern.
 *
 * @since 0.3.1
 */
public record SourceLocation(String className, String methodName, String fileName, int lineNumber) {
    public SourceLocation {
        className = safe(className);
        methodName = safe(methodName);
        fileName = safe(fileName);
        if (lineNumber < 0) lineNumber = 0;
    }

    /** Returns the compact label shown by the HUD. @since 0.3.1 */
    public String displayName() {
        return fileName + (lineNumber > 0 ? ":" + lineNumber : "");
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
