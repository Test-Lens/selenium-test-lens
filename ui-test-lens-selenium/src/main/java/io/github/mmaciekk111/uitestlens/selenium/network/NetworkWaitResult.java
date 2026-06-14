package io.github.mmaciekk111.uitestlens.selenium.network;

import java.time.Duration;

public final class NetworkWaitResult {
    private final NetworkWaitStatus status;
    private final String conditionSummary;
    private final NetworkEvent matchedEvent;
    private final NetworkRequest matchedRequest;
    private final NetworkResponse matchedResponse;
    private final int attempts;
    private final Duration elapsed;
    private final String message;
    private final NetworkWaitFailureReason failureReason;
    private final Throwable exception;

    private NetworkWaitResult(NetworkWaitStatus status,
                              String conditionSummary,
                              NetworkEvent matchedEvent,
                              NetworkRequest matchedRequest,
                              NetworkResponse matchedResponse,
                              int attempts,
                              Duration elapsed,
                              String message,
                              NetworkWaitFailureReason failureReason,
                              Throwable exception) {
        this.status = status == null ? NetworkWaitStatus.FAILED : status;
        this.conditionSummary = conditionSummary == null ? "" : conditionSummary;
        this.matchedEvent = matchedEvent;
        this.matchedRequest = matchedRequest;
        this.matchedResponse = matchedResponse;
        this.attempts = Math.max(0, attempts);
        this.elapsed = elapsed == null ? Duration.ZERO : elapsed;
        this.message = message == null ? "" : message;
        this.failureReason = failureReason;
        this.exception = exception;
    }

    public static NetworkWaitResult matched(NetworkWaitCondition condition,
                                            NetworkEvent event,
                                            NetworkRequest request,
                                            int attempts,
                                            Duration elapsed) {
        NetworkResponse response = event == null ? null : event.response();
        String target = response != null ? response.url() : request == null ? "" : request.url();
        return new NetworkWaitResult(NetworkWaitStatus.MATCHED, summaryOf(condition), event, request, response,
                attempts, elapsed, "Matched network event: " + target, null, null);
    }

    public static NetworkWaitResult timedOut(NetworkWaitCondition condition,
                                             int attempts,
                                             Duration elapsed,
                                             NetworkSummary summary) {
        NetworkWaitFailureReason reason = condition != null && condition.matchRequestOnly()
                ? NetworkWaitFailureReason.NO_MATCHING_REQUEST
                : NetworkWaitFailureReason.NO_MATCHING_RESPONSE;
        return new NetworkWaitResult(NetworkWaitStatus.TIMED_OUT, summaryOf(condition), null, null, null,
                attempts, elapsed, timeoutMessage(condition, elapsed, summary), reason, null);
    }

    public static NetworkWaitResult failed(NetworkWaitCondition condition,
                                           String message,
                                           NetworkWaitFailureReason reason,
                                           Throwable exception,
                                           int attempts,
                                           Duration elapsed) {
        return new NetworkWaitResult(NetworkWaitStatus.FAILED, summaryOf(condition), null, null, null,
                attempts, elapsed, message, reason == null ? NetworkWaitFailureReason.UNKNOWN : reason, exception);
    }

    public static NetworkWaitResult skipped(NetworkWaitCondition condition,
                                            String message,
                                            NetworkWaitFailureReason reason,
                                            int attempts,
                                            Duration elapsed) {
        return new NetworkWaitResult(NetworkWaitStatus.SKIPPED, summaryOf(condition), null, null, null,
                attempts, elapsed, message, reason == null ? NetworkWaitFailureReason.UNKNOWN : reason, null);
    }

    public NetworkWaitStatus status() { return status; }
    public String conditionSummary() { return conditionSummary; }
    public NetworkEvent matchedEvent() { return matchedEvent; }
    public NetworkRequest matchedRequest() { return matchedRequest; }
    public NetworkResponse matchedResponse() { return matchedResponse; }
    public int attempts() { return attempts; }
    public Duration elapsed() { return elapsed; }
    public String message() { return message; }
    public NetworkWaitFailureReason failureReason() { return failureReason; }
    public Throwable exception() { return exception; }

    private static String timeoutMessage(NetworkWaitCondition condition, Duration elapsed, NetworkSummary summary) {
        String unit = condition != null && condition.matchRequestOnly() ? "request" : "response";
        StringBuilder builder = new StringBuilder("Timed out waiting for ")
                .append(unit)
                .append(": ")
                .append(summaryOf(condition))
                .append(" after ")
                .append(elapsed.toMillis())
                .append(" ms.");
        if (summary != null) {
            builder.append(" Seen responses: ")
                    .append(summary.totalResponses())
                    .append(". Failed responses: ")
                    .append(summary.failedResponses())
                    .append('.');
        }
        return builder.toString();
    }

    private static String summaryOf(NetworkWaitCondition condition) {
        return condition == null ? "any network response" : condition.summary();
    }
}
