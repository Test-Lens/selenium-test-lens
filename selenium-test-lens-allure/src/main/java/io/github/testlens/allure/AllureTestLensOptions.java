package io.github.testlens.allure;

/**
 * Selects finalized Test Lens artifacts for Allure. Defaults attach the two screenshots, HTML report, and failure ZIP
 * for failed sessions only; trace JSON and non-failed sessions are opt-in.
 *
 * @param diagnosticScreenshot attach the finalized diagnostic screenshot when available
 * @param cleanScreenshot attach the finalized clean screenshot when available
 * @param htmlReport attach the finalized HTML report when available
 * @param trace attach the finalized trace JSON when available
 * @param failureBundle attach the completed failure ZIP when available
 * @param nonFailedSessions allow attachment attempts for passed and skipped sessions
 * @since 0.3.0
 */
public record AllureTestLensOptions(boolean diagnosticScreenshot, boolean cleanScreenshot, boolean htmlReport,
                                    boolean trace, boolean failureBundle, boolean nonFailedSessions) {
    /**
     * Returns the failure-focused default attachment policy.
     *
     * @return default policy
     * @since 0.3.0
     */
    public static AllureTestLensOptions defaults() { return builder().build(); }
    /**
     * Creates a builder initialized with the defaults.
     *
     * @return new builder
     * @since 0.3.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Builder for the artifact-level attachment policy.
     *
     * @since 0.3.0
     */
    public static final class Builder {
        private boolean diagnosticScreenshot = true;
        private boolean cleanScreenshot = true;
        private boolean htmlReport = true;
        private boolean trace;
        private boolean failureBundle = true;
        private boolean nonFailedSessions;

        /**
         * Creates a builder initialized with the default attachment policy.
         *
         * @since 0.3.0
         */
        public Builder() { }

        /**
         * Controls attachment of the diagnostic PNG.
         *
         * @param value enabled state
         * @return this builder
         * @since 0.3.0
         */
        public Builder attachDiagnosticScreenshot(boolean value) { diagnosticScreenshot = value; return this; }
        /**
         * Controls attachment of the clean PNG.
         *
         * @param value enabled state
         * @return this builder
         * @since 0.3.0
         */
        public Builder attachCleanScreenshot(boolean value) { cleanScreenshot = value; return this; }
        /**
         * Controls attachment of the HTML report.
         *
         * @param value enabled state
         * @return this builder
         * @since 0.3.0
         */
        public Builder attachHtmlReport(boolean value) { htmlReport = value; return this; }
        /**
         * Controls attachment of trace JSON.
         *
         * @param value enabled state
         * @return this builder
         * @since 0.3.0
         */
        public Builder attachTrace(boolean value) { trace = value; return this; }
        /**
         * Controls attachment of the completed failure ZIP.
         *
         * @param value enabled state
         * @return this builder
         * @since 0.3.0
         */
        public Builder attachFailureBundle(boolean value) { failureBundle = value; return this; }
        /**
         * Allows the policy to run for passed and skipped sessions.
         *
         * @param value enabled state
         * @return this builder
         * @since 0.3.0
         */
        public Builder attachNonFailedSessions(boolean value) { nonFailedSessions = value; return this; }
        /**
         * Builds immutable attachment options.
         *
         * @return immutable options
         * @since 0.3.0
         */
        public AllureTestLensOptions build() {
            return new AllureTestLensOptions(diagnosticScreenshot, cleanScreenshot, htmlReport, trace,
                    failureBundle, nonFailedSessions);
        }
    }
}
