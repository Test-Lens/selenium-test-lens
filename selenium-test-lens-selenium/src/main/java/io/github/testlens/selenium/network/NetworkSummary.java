package io.github.testlens.selenium.network;

import java.util.List;
import java.util.Optional;

public final class NetworkSummary {
    private final int totalRequests;
    private final int totalResponses;
    private final int failedResponses;
    private final int failedRequests;
    private final int ignoredEvents;
    private final NetworkEvent firstFailure;
    private final NetworkDiagnosticsStatus status;

    public NetworkSummary(int totalRequests, int totalResponses, int failedResponses, int failedRequests,
                          int ignoredEvents, NetworkEvent firstFailure, NetworkDiagnosticsStatus status) {
        this.totalRequests = totalRequests;
        this.totalResponses = totalResponses;
        this.failedResponses = failedResponses;
        this.failedRequests = failedRequests;
        this.ignoredEvents = ignoredEvents;
        this.firstFailure = firstFailure;
        this.status = status == null ? NetworkDiagnosticsStatus.STOPPED : status;
    }

    public static NetworkSummary from(List<NetworkEvent> events, int ignoredEvents, int failedStatusThreshold, NetworkDiagnosticsStatus status) {
        int requests = 0;
        int responses = 0;
        int failedResponses = 0;
        int failedRequests = 0;
        NetworkEvent firstFailure = null;
        for (NetworkEvent event : events == null ? List.<NetworkEvent>of() : events) {
            if (event.type() == NetworkEventType.REQUEST) {
                requests++;
            } else if (event.type() == NetworkEventType.RESPONSE) {
                responses++;
                if (event.response() != null && event.response().status() >= failedStatusThreshold) {
                    failedResponses++;
                    if (firstFailure == null) {
                        firstFailure = event;
                    }
                }
            } else if (event.type() == NetworkEventType.FAILED) {
                failedRequests++;
                if (firstFailure == null) {
                    firstFailure = event;
                }
            }
        }
        return new NetworkSummary(requests, responses, failedResponses, failedRequests, ignoredEvents, firstFailure, status);
    }

    public int totalRequests() { return totalRequests; }
    public int totalResponses() { return totalResponses; }
    public int failedResponses() { return failedResponses; }
    public int failedRequests() { return failedRequests; }
    public int ignoredEvents() { return ignoredEvents; }
    public Optional<NetworkEvent> firstFailure() { return Optional.ofNullable(firstFailure); }
    public NetworkDiagnosticsStatus status() { return status; }

    public boolean hasFailures() {
        return failedResponses > 0 || failedRequests > 0;
    }

    public String failureSummary() {
        if (!hasFailures()) {
            return "No failed network requests";
        }
        String first = firstFailure == null ? "" : " firstFailure=" + firstFailure.url();
        return "Failed network requests: responses=" + failedResponses + ", requests=" + failedRequests + first;
    }
}
