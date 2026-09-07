package io.github.testlens.selenium.network;

import io.github.testlens.core.redaction.RedactionPolicy;

/** Safe diagnostic copy of a throwable crossing the public network boundary. */
final class NetworkDiagnosticThrowable extends RuntimeException {
    private static final int MAX_DEPTH = 16;
    private final String originalType;

    private NetworkDiagnosticThrowable(String originalType, String message, Throwable cause) {
        super(message, cause, true, true);
        this.originalType = originalType;
    }

    static Throwable copy(Throwable throwable, RedactionPolicy policy) {
        return copy(throwable, policy == null ? RedactionPolicy.defaults() : policy, 0);
    }

    private static Throwable copy(Throwable throwable, RedactionPolicy policy, int depth) {
        if (throwable == null || depth >= MAX_DEPTH) return null;
        if (!policy.enabled()) return throwable;
        Throwable cause = copy(throwable.getCause(), policy, depth + 1);
        NetworkDiagnosticThrowable safe = new NetworkDiagnosticThrowable(throwable.getClass().getName(),
                redact(policy, throwable.getMessage()), cause);
        safe.setStackTrace(throwable.getStackTrace());
        for (Throwable suppressed : throwable.getSuppressed()) {
            Throwable copy = copy(suppressed, policy, depth + 1);
            if (copy != null) safe.addSuppressed(copy);
        }
        return safe;
    }

    private static String redact(RedactionPolicy policy, String value) {
        try {
            return policy.redact(value);
        } catch (RuntimeException failure) {
            return "[REDACTION_FAILED]";
        }
    }

    @Override
    public String toString() {
        return originalType + (getMessage() == null ? "" : ": " + getMessage());
    }
}
