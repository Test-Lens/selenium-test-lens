package io.github.testlens.selenium.reporting;

/** Selects how the report uploader resolves an HTTP proxy. */
public enum ReportProxyMode {
    /** Bypasses every JVM or operating-system proxy selector. */
    DIRECT,
    /** Uses the current JVM {@link java.net.ProxySelector}. */
    SYSTEM,
    /** Uses one configured HTTP proxy, subject to no-proxy rules. */
    EXPLICIT
}
