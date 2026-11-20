import io.github.testlens.selenium.network.NetworkCaptureMode;
import io.github.testlens.selenium.network.NetworkDiagnostics;
import io.github.testlens.selenium.network.NetworkDiagnosticsOptions;
import io.github.testlens.selenium.network.NetworkEvent;
import io.github.testlens.selenium.network.NetworkWaitCondition;
import io.github.testlens.selenium.network.NetworkWaitStatus;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Optional public-API adapter, compiled and loaded only on the Lens classpath. */
public final class LensProbeBridge implements LayerProbe.LensCapture {
    private NetworkDiagnostics network;
    private final Map<String, String> observed = new HashMap<>();

    public void start(WebDriver driver) {
        network = new NetworkDiagnostics(driver);
        network.start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.BIDI)
                .includeHeaders(true).maxCapturedEvents(2000).build());
        if (!network.isStarted() || !network.activeCaptureMode().filter(mode -> mode == NetworkCaptureMode.BIDI).isPresent()) {
            throw new IllegalStateException("LENS_BIDI_START_FAILED");
        }
    }

    public void awaitResponse(String url, int status, long deadlineNanos) {
        long remaining = deadlineNanos - System.nanoTime();
        if (remaining <= 0) throw new IllegalStateException("LENS_SHARED_EVENT_DEADLINE_EXPIRED");
        var result = network.waitForResponse(NetworkWaitCondition.builder().exactUrl(url).method("GET").status(status)
                .timeout(Duration.ofNanos(remaining)).build());
        if (result.status() != NetworkWaitStatus.MATCHED) throw new IllegalStateException("LENS_RESPONSE_MISMATCH");
    }

    public List<Map<String, Object>> snapshot() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (NetworkEvent event : network.events()) {
            String url = LayerProbe.safeUrl(event.url());
            if (url.isEmpty()) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("eventId", event.id());
            row.put("type", event.type().name());
            row.put("url", url);
            row.put("bidiTimestamp", event.timestamp().toString());
            row.put("snapshotObservedAt", observed.computeIfAbsent(event.id(), ignored -> Instant.now().toString()));
            row.put("hop", event.attributes().getOrDefault("redirectCount", "UNAVAILABLE"));
            if (event.request() != null) {
                row.put("requestId", event.request().id());
                row.put("method", event.request().method().equals("GET") ? "GET" : "<other-method>");
            }
            if (event.response() != null) {
                row.put("requestId", event.response().requestId());
                row.put("status", event.response().status());
                String location = event.response().headers().entrySet().stream()
                        .filter(entry -> entry.getKey().equalsIgnoreCase("location")).map(Map.Entry::getValue).findFirst().orElse("");
                row.put("location", LayerProbe.location(location, url));
                if (event.attributes().containsKey("bodySize")) row.put("bodySize", Long.parseLong(event.attributes().get("bodySize")));
            }
            rows.add(row);
        }
        return rows;
    }

    public Map<String, Object> state() {
        if (network == null) return Map.of("started", false);
        var summary = network.summary();
        return Map.of("started", network.isStarted(), "activeMode", network.activeCaptureMode().map(Enum::name).orElse(""),
                "status", summary.status().name(), "requests", summary.totalRequests(), "responses", summary.totalResponses(),
                "failedRequests", summary.failedRequests(), "dropped", summary.droppedEvents(), "ignored", summary.ignoredEvents());
    }

    public String exportJson() { return network.exportJson(); }
    public void close() { if (network != null) network.stop(); }
}
