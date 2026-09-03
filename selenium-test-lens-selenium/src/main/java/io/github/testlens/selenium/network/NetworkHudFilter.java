package io.github.testlens.selenium.network;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Immutable presentation-only filter for raw network entries rendered in the HUD. */
public final class NetworkHudFilter {
    private final boolean showRequests;
    private final boolean showResponses;
    private final boolean showFailures;
    private final boolean showFailedResponses;
    private final List<Pattern> includePatterns;
    private final List<Pattern> excludePatterns;
    private final List<String> includePatternSources;
    private final List<String> excludePatternSources;

    private NetworkHudFilter(Builder builder) {
        this.showRequests = builder.showRequests;
        this.showResponses = builder.showResponses;
        this.showFailures = builder.showFailures;
        this.showFailedResponses = builder.showFailedResponses;
        this.includePatterns = List.copyOf(builder.includePatterns);
        this.excludePatterns = List.copyOf(builder.excludePatterns);
        this.includePatternSources = sources(includePatterns);
        this.excludePatternSources = sources(excludePatterns);
    }

    /** Hides duplicate request lines and shows responses, fetch errors, and failed HTTP responses. */
    public static NetworkHudFilter defaults() {
        return builder().build();
    }

    /** Shows every raw request, response, and failure entry. */
    public static NetworkHudFilter all() {
        return builder().showRequests(true).showResponses(true).showFailures(true)
                .showFailedResponses(true).build();
    }

    /** Hides every raw request, response, and failure entry; control entries remain visible. */
    public static NetworkHudFilter none() {
        return builder().showRequests(false).showResponses(false).showFailures(false)
                .showFailedResponses(false).build();
    }

    public static Builder builder() { return new Builder(); }

    public boolean showRequests() { return showRequests; }
    public boolean showResponses() { return showResponses; }
    public boolean showFailures() { return showFailures; }
    public boolean showFailedResponses() { return showFailedResponses; }
    public List<String> includeUrlPatterns() { return includePatternSources; }
    public List<String> excludeUrlPatterns() { return excludePatternSources; }

    boolean isVisible(NetworkEvent event, int failedStatusThreshold) {
        try {
            if (event == null) return false;
            String url = event.url() == null ? "" : event.url();
            if (matches(excludePatterns, url)) return false;
            if (event.type() == NetworkEventType.FAILED) return showFailures;
            if (event.type() == NetworkEventType.RESPONSE && event.response() != null
                    && event.response().status() >= failedStatusThreshold) {
                return showFailedResponses;
            }
            boolean typeVisible = event.type() == NetworkEventType.REQUEST ? showRequests
                    : event.type() == NetworkEventType.RESPONSE && showResponses;
            return typeVisible && (includePatterns.isEmpty() || matches(includePatterns, url));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean matches(List<Pattern> patterns, String value) {
        return patterns.stream().anyMatch(pattern -> pattern.matcher(value).find());
    }

    private static List<String> sources(List<Pattern> patterns) {
        return patterns.stream().map(Pattern::pattern).toList();
    }

    public static final class Builder {
        private boolean showRequests;
        private boolean showResponses = true;
        private boolean showFailures = true;
        private boolean showFailedResponses = true;
        private final List<Pattern> includePatterns = new ArrayList<>();
        private final List<Pattern> excludePatterns = new ArrayList<>();

        private Builder() {}

        public Builder showRequests(boolean value) { this.showRequests = value; return this; }
        public Builder showResponses(boolean value) { this.showResponses = value; return this; }
        public Builder showFailures(boolean value) { this.showFailures = value; return this; }
        public Builder showFailedResponses(boolean value) { this.showFailedResponses = value; return this; }

        public Builder includeUrlPattern(String regex) {
            add(includePatterns, regex, "includeUrlPattern");
            return this;
        }

        public Builder excludeUrlPattern(String regex) {
            add(excludePatterns, regex, "excludeUrlPattern");
            return this;
        }

        public NetworkHudFilter build() { return new NetworkHudFilter(this); }

        private static void add(List<Pattern> target, String regex, String option) {
            if (regex == null || regex.isBlank()) return;
            try {
                target.add(Pattern.compile(regex));
            } catch (PatternSyntaxException failure) {
                throw new IllegalArgumentException(option + " regex is invalid: " + regex, failure);
            }
        }
    }
}
