package io.github.testlens.selenium.reporting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Immutable proxy selection used only by the JVM report uploader, not by WebDriver. */
public final class ReportProxyOptions {
    private final ReportProxyMode mode;
    private final String host;
    private final int port;
    private final List<String> noProxy;

    private ReportProxyOptions(Builder builder) {
        mode = builder.mode;
        host = builder.host;
        port = builder.port;
        noProxy = List.copyOf(builder.noProxy);
        if (mode == ReportProxyMode.EXPLICIT && (host == null || host.isBlank())) {
            throw new IllegalArgumentException("An explicit proxy requires a host");
        }
        if (mode == ReportProxyMode.EXPLICIT && (port < 1 || port > 65_535)) {
            throw new IllegalArgumentException("An explicit proxy port must be between 1 and 65535");
        }
    }

    /** Returns system-proxy defaults. */
    public static ReportProxyOptions system() { return builder().mode(ReportProxyMode.SYSTEM).build(); }

    /** Returns a configuration that always bypasses proxies. */
    public static ReportProxyOptions direct() { return builder().mode(ReportProxyMode.DIRECT).build(); }

    /** Creates a proxy options builder. */
    public static Builder builder() { return new Builder(); }

    ReportProxyMode mode() { return mode; }
    String host() { return host; }
    int port() { return port; }
    List<String> noProxy() { return noProxy; }

    @Override public String toString() {
        return "ReportProxyOptions[mode=" + mode + ", configured=" + (host != null) + ", noProxyRules="
                + noProxy.size() + "]";
    }

    /** Builds immutable report proxy options. */
    public static final class Builder {
        private ReportProxyMode mode = ReportProxyMode.SYSTEM;
        private String host;
        private int port;
        private final List<String> noProxy = new ArrayList<>();

        private Builder() { }

        /** Sets the proxy selection mode. */
        public Builder mode(ReportProxyMode value) {
            if (value == null) throw new IllegalArgumentException("proxy mode must not be null");
            mode = value;
            return this;
        }

        /** Sets the explicit HTTP proxy host without credentials. */
        public Builder host(String value) {
            if (value == null || value.isBlank() || containsLineBreak(value)) {
                throw new IllegalArgumentException("proxy host must not be blank or contain line breaks");
            }
            host = normalizeHost(value);
            return this;
        }

        /** Sets the explicit HTTP proxy port. */
        public Builder port(int value) {
            if (value < 1 || value > 65_535) throw new IllegalArgumentException("proxy port must be between 1 and 65535");
            port = value;
            return this;
        }

        /** Adds an exact host, domain suffix, optional port, or {@code *} bypass rule. CIDR is not supported. */
        public Builder noProxy(String rule) {
            if (rule == null || rule.isBlank() || containsLineBreak(rule) || rule.contains("/")) {
                throw new IllegalArgumentException("no-proxy rule must be non-blank and must not use CIDR");
            }
            noProxy.add(rule.trim().toLowerCase(Locale.ROOT));
            return this;
        }

        /** Builds immutable proxy options. */
        public ReportProxyOptions build() { return new ReportProxyOptions(this); }

        private static boolean containsLineBreak(String value) { return value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0; }
        private static String normalizeHost(String value) {
            String result = value.trim();
            if (result.startsWith("[") && result.endsWith("]")) result = result.substring(1, result.length() - 1);
            return result.toLowerCase(Locale.ROOT);
        }
    }
}
