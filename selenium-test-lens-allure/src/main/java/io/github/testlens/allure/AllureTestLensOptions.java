package io.github.testlens.allure;

/**
 * Selects finalized Test Lens artifacts for Allure. Defaults attach the two screenshots, HTML report, and failure ZIP
 * for failed sessions only; trace JSON and non-failed sessions are opt-in.
 *
 * @since 0.3.0
 */
public record AllureTestLensOptions(boolean diagnosticScreenshot, boolean cleanScreenshot, boolean htmlReport,
                                    boolean trace, boolean failureBundle, boolean nonFailedSessions) {
    /** @return failure-focused default attachment policy */
    public static AllureTestLensOptions defaults() { return builder().build(); }
    /** @return a new builder initialized with the defaults */
    public static Builder builder() { return new Builder(); }

    /** Builder for the small artifact-level policy. @since 0.3.0 */
    public static final class Builder {
        private boolean diagnosticScreenshot = true;
        private boolean cleanScreenshot = true;
        private boolean htmlReport = true;
        private boolean trace;
        private boolean failureBundle = true;
        private boolean nonFailedSessions;

        /** Enables the diagnostic PNG. @param value enabled state @return this builder */
        public Builder attachDiagnosticScreenshot(boolean value) { diagnosticScreenshot = value; return this; }
        /** Enables the clean PNG. @param value enabled state @return this builder */
        public Builder attachCleanScreenshot(boolean value) { cleanScreenshot = value; return this; }
        /** Enables the HTML report. @param value enabled state @return this builder */
        public Builder attachHtmlReport(boolean value) { htmlReport = value; return this; }
        /** Enables trace JSON. @param value enabled state @return this builder */
        public Builder attachTrace(boolean value) { trace = value; return this; }
        /** Enables the completed failure ZIP. @param value enabled state @return this builder */
        public Builder attachFailureBundle(boolean value) { failureBundle = value; return this; }
        /** Allows the policy to run for passed and skipped sessions. @param value enabled state @return this builder */
        public Builder attachNonFailedSessions(boolean value) { nonFailedSessions = value; return this; }
        /** @return immutable options */
        public AllureTestLensOptions build() {
            return new AllureTestLensOptions(diagnosticScreenshot, cleanScreenshot, htmlReport, trace,
                    failureBundle, nonFailedSessions);
        }
    }
}
