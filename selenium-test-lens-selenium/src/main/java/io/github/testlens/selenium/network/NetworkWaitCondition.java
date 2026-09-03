package io.github.testlens.selenium.network;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class NetworkWaitCondition {
    private final String urlContains;
    private final String urlRegex;
    private final String exactUrl;
    private final String method;
    private final Integer status;
    private final Integer minStatus;
    private final Integer maxStatus;
    private final Duration timeout;
    private final Duration pollInterval;
    private final boolean includeFailedResponses;
    private final boolean matchRequestOnly;
    private final Pattern compiledUrlRegex;

    private NetworkWaitCondition(Builder builder) {
        this.urlContains = safe(builder.urlContains);
        this.urlRegex = safe(builder.urlRegex);
        this.exactUrl = safe(builder.exactUrl);
        this.method = normalizeMethod(builder.method);
        this.status = builder.status;
        this.minStatus = builder.minStatus;
        this.maxStatus = builder.maxStatus;
        this.timeout = positiveOrDefault(builder.timeout, Duration.ofSeconds(5));
        this.pollInterval = positiveOrDefault(builder.pollInterval, Duration.ofMillis(100));
        this.includeFailedResponses = builder.includeFailedResponses;
        this.matchRequestOnly = builder.matchRequestOnly;
        this.compiledUrlRegex = compile(urlRegex);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean matches(NetworkEvent event, List<NetworkEvent> allEvents) {
        if (event == null) {
            return false;
        }
        if (matchRequestOnly) {
            return event.request() != null && matchesRequest(event.request());
        }
        if (event.response() == null) {
            return false;
        }
        if (!includeFailedResponses && event.response().status() >= 400) {
            return false;
        }
        return matchesResponse(event, allEvents);
    }

    boolean matchesFailedResponse(NetworkEvent event, List<NetworkEvent> allEvents) {
        if (event == null || event.response() == null || includeFailedResponses || event.response().status() < 400) {
            return false;
        }
        return matchesUrl(event.response().url())
                && matchesStatus(event.response().status())
                && matchesMethod(event, allEvents);
    }

    public String summary() {
        StringBuilder builder = new StringBuilder();
        append(builder, "method", method);
        append(builder, "url contains", urlContains);
        append(builder, "url regex", urlRegex);
        append(builder, "exact url", exactUrl);
        if (status != null) append(builder, "status", String.valueOf(status));
        if (minStatus != null || maxStatus != null) {
            append(builder, "status range", (minStatus == null ? "*" : minStatus) + ".." + (maxStatus == null ? "*" : maxStatus));
        }
        if (matchRequestOnly) append(builder, "request only", "true");
        return builder.length() == 0 ? "any network response" : builder.toString();
    }

    public String urlContains() { return urlContains; }
    public String urlRegex() { return urlRegex; }
    public String exactUrl() { return exactUrl; }
    public String method() { return method; }
    public Integer status() { return status; }
    public Integer minStatus() { return minStatus; }
    public Integer maxStatus() { return maxStatus; }
    public Duration timeout() { return timeout; }
    public Duration pollInterval() { return pollInterval; }
    public boolean includeFailedResponses() { return includeFailedResponses; }
    public boolean matchRequestOnly() { return matchRequestOnly; }

    private boolean matchesRequest(NetworkRequest request) {
        return matchesUrl(request.url()) && (method.isBlank() || method.equalsIgnoreCase(request.method()));
    }

    private boolean matchesResponse(NetworkEvent event, List<NetworkEvent> allEvents) {
        NetworkResponse response = event.response();
        return matchesUrl(response.url())
                && matchesStatus(response.status())
                && matchesMethod(event, allEvents);
    }

    private boolean matchesUrl(String url) {
        String safeUrl = safe(url);
        if (!exactUrl.isBlank() && !exactUrl.equals(safeUrl)) {
            return false;
        }
        if (!urlContains.isBlank() && !safeUrl.contains(urlContains)) {
            return false;
        }
        return compiledUrlRegex == null || compiledUrlRegex.matcher(safeUrl).find();
    }

    private boolean matchesStatus(int responseStatus) {
        if (status != null) {
            return responseStatus == status;
        }
        if (minStatus != null && responseStatus < minStatus) {
            return false;
        }
        return maxStatus == null || responseStatus <= maxStatus;
    }

    private boolean matchesMethod(NetworkEvent responseEvent, List<NetworkEvent> allEvents) {
        if (method.isBlank()) {
            return true;
        }
        Optional<NetworkRequest> request = findRequest(responseEvent, allEvents);
        return request.map(value -> method.equalsIgnoreCase(value.method())).orElse(false);
    }

    private Optional<NetworkRequest> findRequest(NetworkEvent responseEvent, List<NetworkEvent> allEvents) {
        String requestId = responseEvent == null || responseEvent.response() == null
                ? "" : responseEvent.response().requestId();
        if (requestId == null || requestId.isBlank() || allEvents == null) {
            return Optional.empty();
        }
        String redirectCount = responseEvent.attributes().get("redirectCount");
        Optional<NetworkRequest> correlated = allEvents.stream()
                .filter(event -> event.request() != null && requestId.equals(event.request().id()))
                .filter(event -> redirectCount == null
                        || redirectCount.equals(event.attributes().get("redirectCount")))
                .map(NetworkEvent::request).findFirst();
        return correlated.isPresent() ? correlated : allEvents.stream()
                .map(NetworkEvent::request).filter(Objects::nonNull)
                .filter(request -> requestId.equals(request.id())).findFirst();
    }

    private static Pattern compile(String regex) {
        if (regex == null || regex.isBlank()) {
            return null;
        }
        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("urlRegex is invalid: " + regex, e);
        }
    }

    private static Duration positiveOrDefault(Duration value, Duration defaultValue) {
        return value == null || value.isZero() || value.isNegative() ? defaultValue : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalizeMethod(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static void append(StringBuilder builder, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(key).append('=').append(value);
    }

    public static final class Builder {
        private String urlContains;
        private String urlRegex;
        private String exactUrl;
        private String method;
        private Integer status;
        private Integer minStatus;
        private Integer maxStatus;
        private Duration timeout = Duration.ofSeconds(5);
        private Duration pollInterval = Duration.ofMillis(100);
        private boolean includeFailedResponses = true;
        private boolean matchRequestOnly;

        private Builder() {}

        public Builder urlContains(String urlContains) {
            this.urlContains = urlContains;
            return this;
        }

        public Builder urlRegex(String urlRegex) {
            this.urlRegex = urlRegex;
            return this;
        }

        public Builder exactUrl(String exactUrl) {
            this.exactUrl = exactUrl;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder minStatus(int minStatus) {
            this.minStatus = minStatus;
            return this;
        }

        public Builder maxStatus(int maxStatus) {
            this.maxStatus = maxStatus;
            return this;
        }

        public Builder statusBetween(int minStatus, int maxStatus) {
            this.minStatus = minStatus;
            this.maxStatus = maxStatus;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder pollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
            return this;
        }

        public Builder includeFailedResponses(boolean includeFailedResponses) {
            this.includeFailedResponses = includeFailedResponses;
            return this;
        }

        public Builder matchRequestOnly(boolean matchRequestOnly) {
            this.matchRequestOnly = matchRequestOnly;
            return this;
        }

        public NetworkWaitCondition build() {
            return new NetworkWaitCondition(this);
        }
    }
}

