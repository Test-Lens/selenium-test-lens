package io.github.testlens.selenium.network;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.testlens.OverlayConfig;
import io.github.testlens.ObservabilityMode;
import io.github.testlens.TestLens;
import io.github.testlens.TestLensFinalizationResult;
import io.github.testlens.TestLensOptions;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.selenium.evidence.FailureBundleOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Consumer regression: expected traffic comes from the HTTP peer, never Lens internals. */
@EnabledIfSystemProperty(named = "bidi.browser", matches = "true")
class NetworkReportCorrectnessTest {
    private final Path output = Path.of(System.getProperty("bidi.report.output", "target/network-report-evidence"))
            .resolve(UUID.randomUUID().toString()).toAbsolutePath();

    @Test void localChromeReportDataMatchesServerJournal() throws Exception { verify(false); }

    @Test void rawRemoteReportDataMatchesServerJournal() throws Exception {
        assumeTrue(!System.getProperty("bidi.grid", "").isBlank(), "NOT RUN: no test Grid configured");
        verify(true);
    }

    private void verify(boolean remote) throws Exception {
        Files.createDirectories(output);
        try (Peer peer = new Peer()) {
            ChromeOptions options = new ChromeOptions().enableBiDi();
            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
            String binary = System.getProperty("test.chrome.binary", "");
            if (!binary.isBlank()) options.setBinary(binary);
            WebDriver driver = remote
                    ? new RemoteWebDriver(URI.create(System.getProperty("bidi.grid")).toURL(), options)
                    : new ChromeDriver(options);
            try {
                driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(20));
                driver.get(peer.origin + "/page");
                verifySession(driver, peer, false);
                verifySession(driver, peer, true);
            } finally {
                driver.quit();
            }
        }
    }

    private void verifySession(WebDriver driver, Peer peer, boolean failed) throws Exception {
        // Both sessions start on the same origin, including after the first HTML report is opened.
        driver.get(peer.origin + "/page");
        peer.journal.clear();
        peer.completed = new CountDownLatch(failed ? 20 : 18);
        peer.credentialsObserved = false;
        String secret = UUID.randomUUID().toString();
        peer.secret = secret;
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
                .overlayConfig(OverlayConfig.builder().enabled(false).build())
                .observabilityMode(ObservabilityMode.DEFAULT)
                .screenshotOnFailure(false).outputRoot(output)
                .redactionPolicy(RedactionPolicy.builder().secret(secret).build())
                .failureBundleOptions(FailureBundleOptions.builder()
                        .diagnosticScreenshot(false).cleanScreenshot(false).build()).build());
        var session = lens.startSession(failed ? "network-report-failure" : "network-report-success");
        NetworkDiagnostics network = lens.network();
        try {
            if (failed) {
                assertThrows(NetworkAssertionError.class, network::assertNoFailedRequests,
                        "An unstarted capture must not report a passing assertion");
            }
            network.start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.BIDI)
                    .includeHeaders(true).build());
            assertTrue(network.isStarted(), "Real BiDi capture must start");
            assertEquals(NetworkCaptureMode.BIDI, network.activeCaptureMode().orElseThrow());

            fetch(driver, "/case/ok", "GET", null, null);
            fetch(driver, "/case/post", "POST", "{\"message\":\"journal-proof\"}", null);
            fetch(driver, "/case/redirect", "GET", null, null);
            fetch(driver, "/case/no-body", "GET", null, null);
            for (int index = 0; index < 3; index++) fetch(driver, "/case/repeated", "GET", null, null);
            burst(driver);
            fetch(driver, "/case/secret", "GET", null, secret);
            if (failed) {
                fetch(driver, "/case/not-found", "GET", null, null);
                fetch(driver, "/case/server-error", "GET", null, null);
            }
            fetch(driver, "/case/barrier", "GET", null, null);
            assertTrue(peer.completed.await(5, TimeUnit.SECONDS), "HTTP peer must finish journaling every transmitted response");
            List<Observed> journal = List.copyOf(peer.journal);
            assertEquals(failed ? 20 : 18, journal.size(), "Fixture must issue its complete traffic matrix");
            assertTrue(peer.credentialsObserved, "Peer must receive the original credential");
            assertTrue(journal.stream().anyMatch(item -> item.path.equals("/case/post")
                    && item.method.equals("POST") && item.body.equals("{\"message\":\"journal-proof\"}")),
                    "Peer must receive the JSON POST body");

            new WebDriverWait(driver, Duration.ofSeconds(10)).until(ignored ->
                    network.events().stream().filter(event -> event.response() != null
                            && event.url().startsWith(peer.origin + "/case/")).count() >= journal.size());
            // Correlation is verified through the public wait result, including each redirect hop.
            for (Observed item : journal.stream().distinct().toList()) {
                NetworkWaitResult result = network.waitForResponse(NetworkWaitCondition.builder()
                        .exactUrl(peer.origin + item.path).method(item.method).status(item.status).build());
                assertEquals(NetworkWaitStatus.MATCHED, result.status());
                assertNotNull(result.matchedRequest());
                assertEquals(result.matchedRequest().id(), result.matchedResponse().requestId());
                assertEquals(peer.origin + item.path, result.matchedRequest().url());
                assertEquals(item.method, result.matchedRequest().method());
            }
            String exported = network.exportJson();
            assertSafe(exported, secret);
            assertExport(journal, object(exported), peer.origin, "BIDI");
            network.stop();
            List<NetworkEvent> events = network.events();
            assertTraffic(journal, events, peer.origin);
            assertExportedBodySizeAvailability(object(exported), events);
            NetworkRequest post = events.stream().filter(event -> event.request() != null
                    && event.url().endsWith("/case/post")).findFirst().orElseThrow().request();
            assertEquals("application/json", header(post.headers(), "Content-Type"));
            NetworkRequest sensitive = events.stream().filter(event -> event.request() != null
                    && event.url().endsWith("/case/secret")).findFirst().orElseThrow().request();
            String credential = header(sensitive.headers(), "X-Api-Key");
            assertTrue(credential != null && !credential.isBlank() && !credential.contains(secret),
                    "Captured request must retain the header with its value redacted");
            NetworkResponse sensitiveResponse = events.stream().filter(event -> event.response() != null
                    && event.url().endsWith("/case/secret")).findFirst().orElseThrow().response();
            String cookie = header(sensitiveResponse.headers(), "Set-Cookie");
            assertTrue(cookie != null && !cookie.isBlank() && !cookie.contains(secret),
                    "Captured response must redact the session cookie");
            assertEquals(journal.size(), network.summary().totalRequests());
            assertEquals(journal.size(), network.summary().totalResponses());
            assertEquals(failed ? 2 : 0, network.summary().failedResponses());
            assertEquals(0, network.summary().failedRequests(), "HTTP errors are responses, not transport errors");
            assertEquals(0, network.summary().droppedEvents());
            assertEquals(0, network.summary().ignoredEvents());
            if (failed) assertThrows(NetworkAssertionError.class, network::assertNoFailedRequests);
            else assertEquals(NetworkDiagnosticsStatus.ASSERTION_PASSED, network.assertNoFailedRequests().status());

            Path attached = output.resolve(failed ? "failure-network.json" : "success-network.json");
            assertEquals(NetworkDiagnosticsStatus.ATTACHED, network.attachToSession(session, attached).status());
            TestLensFinalizationResult result = failed
                    ? lens.finishFailed(new AssertionError("Expected network failure token=" + secret))
                    : lens.finishPassed();
            assertEquals(failed ? TraceStatus.FAILED : TraceStatus.PASSED, result.session().metadata().status());
            assertTrue(result.fullySuccessful(), "Report and evidence finalization must succeed");
            assertTrue(result.session().events().stream().anyMatch(event ->
                            (failed ? "NETWORK_ASSERTION_FAILED" : "NETWORK_ASSERTION_PASSED")
                                    .equals(event.attributes().get("logEventType"))
                                    && event.status() == (failed ? TraceStatus.FAILED : TraceStatus.PASSED)),
                    "The finalized trace must retain the actual assertion outcome");
            assertFalse(network.isStarted());
            assertSafe(Files.readString(result.jsonReport()), secret);
            assertTraceReport(journal, object(Files.readString(result.jsonReport())));
            assertSafe(Files.readString(result.htmlReport()), secret);
            Map<String, Object> attachedExport = object(Files.readString(attached));
            assertExport(journal, attachedExport, peer.origin, "");
            assertExportedBodySizeAvailability(attachedExport, events);
            if (failed) assertBundle(result, journal, secret);
            else assertTrue(result.failureBundleArchive().isEmpty());

            // Serve and open the actual generated report, including with a raw Grid driver.
            peer.report = Files.readString(result.htmlReport());
            driver.get(peer.origin + "/report");
            String rendered = String.valueOf(((JavascriptExecutor) driver)
                    .executeScript("return document.body.textContent"));
            assertSafe(rendered, secret);
            assertTrue(rendered.contains(failed ? "FAILED" : "PASSED"));
            @SuppressWarnings("unchecked")
            List<String> rows = (List<String>) ((JavascriptExecutor) driver).executeScript(
                    "return Array.from(document.querySelectorAll('tr'), row => row.textContent)");
            // The documented report/log preview uses a sanitized path; the network export retains the full URL.
            Map<String, Long> expectedRows = multiset(journal.stream()
                    .map(item -> item.method + " " + item.path + " \u2192 " + item.status).toList());
            expectedRows.forEach((preview, count) -> assertEquals(count.longValue(),
                    rows.stream().filter(row -> row.contains(preview)).count(),
                    "HTML must show every response method, path and status exactly once"));
            for (NetworkEvent event : events) {
                if (event.request() != null) assertTrue(rendered.contains(event.request().id()),
                        "HTML must retain request correlation identifiers");
            }
        } finally {
            network.stop();
            peer.secret = "";
        }
    }

    private static void assertTraffic(List<Observed> journal, List<NetworkEvent> events, String origin) {
        List<NetworkEvent> requests = events.stream().filter(event -> event.request() != null).toList();
        List<NetworkEvent> responses = events.stream().filter(event -> event.response() != null).toList();
        assertEquals(multiset(journal.stream().map(item -> item.method + " " + origin + item.path).toList()),
                multiset(requests.stream().map(event -> event.request().method() + " " + event.url()).toList()));
        assertEquals(multiset(journal.stream().map(item -> item.status + " " + origin + item.path).toList()),
                multiset(responses.stream().map(event -> event.response().status() + " " + event.url()).toList()));
        assertEquals(events.size(), events.stream().map(NetworkEvent::id).distinct().count(), "No duplicate event IDs");
        assertEquals(requests.size(), requests.stream().map(event -> event.request().id() + "#"
                + event.attributes().get("redirectCount")).distinct().count(), "One request per ID and hop");
        assertEquals(responses.size(), responses.stream().map(event -> event.response().requestId() + "#"
                + event.attributes().get("redirectCount")).distinct().count(), "One response per ID and hop");
        for (NetworkEvent response : responses) {
            assertFalse(response.response().requestId().isBlank());
            assertResponseBodySize(journal, response.url(), response.response().status(), response.attributes(), origin);
            assertEquals(1, requests.stream().filter(request -> request.request().id().equals(response.response().requestId())
                    && request.url().equals(response.url())
                    && request.attributes().get("redirectCount").equals(response.attributes().get("redirectCount"))).count(),
                    "Every response must have exactly one matching request and redirect hop");
        }
        List<NetworkEvent> repeated = requests.stream().filter(event -> event.url().endsWith("/case/repeated")).toList();
        assertEquals(3, new HashSet<>(repeated.stream().map(event -> event.request().id()).toList()).size());
        assertEquals("0", responses.stream().filter(event -> event.url().endsWith("/case/redirect"))
                .findFirst().orElseThrow().attributes().get("redirectCount"));
        assertEquals("1", responses.stream().filter(event -> event.url().endsWith("/case/final"))
                .findFirst().orElseThrow().attributes().get("redirectCount"));
    }

    private static void assertExport(List<Observed> journal, Map<String, Object> json, String origin, String activeMode) {
        assertEquals("BIDI", json.get("requestedCaptureMode"));
        assertEquals(activeMode, json.get("activeCaptureMode"));
        List<Map<String, Object>> events = objects(json.get("events"));
        assertEquals(multiset(journal.stream().map(item -> item.method + " " + origin + item.path).toList()),
                multiset(events.stream().filter(event -> event.get("request") instanceof Map)
                        .map(event -> object(event.get("request"))).map(request -> request.get("method") + " " + request.get("url")).toList()));
        assertEquals(multiset(journal.stream().map(item -> item.status + " " + origin + item.path).toList()),
                multiset(events.stream().filter(event -> event.get("response") instanceof Map)
                        .map(event -> object(event.get("response"))).map(response -> number(response.get("status")) + " " + response.get("url")).toList()));
        assertEquals(events.size(), events.stream().map(event -> event.get("id")).distinct().count());
        List<Map<String, Object>> requests = events.stream().filter(event -> event.get("request") instanceof Map).toList();
        for (Map<String, Object> responseEvent : events.stream().filter(event -> event.get("response") instanceof Map).toList()) {
            Map<String, Object> response = object(responseEvent.get("response"));
            assertResponseBodySize(journal, (String) response.get("url"), (int) number(response.get("status")),
                    object(responseEvent.get("attributes")), origin);
            assertEquals(1, requests.stream().filter(requestEvent -> {
                Map<String, Object> request = object(requestEvent.get("request"));
                return request.get("id").equals(response.get("requestId"))
                        && request.get("url").equals(response.get("url"))
                        && object(requestEvent.get("attributes")).get("redirectCount")
                        .equals(object(responseEvent.get("attributes")).get("redirectCount"));
            }).count(), "Exported response IDs and hops must correlate to exactly one exported request");
        }
    }

    private static void assertResponseBodySize(List<Observed> journal, String url, int status,
                                               Map<String, ?> attributes, String origin) {
        if (!attributes.containsKey("bodySize")) return; // Unavailable is distinct from a measured zero.
        List<Long> transmittedSizes = journal.stream()
                .filter(item -> (origin + item.path).equals(url) && item.status == status)
                .map(Observed::responseBodyBytes).distinct().toList();
        assertEquals(1, transmittedSizes.size(), "The peer must supply an unambiguous transmitted body size");
        assertEquals(transmittedSizes.get(0).longValue(), Long.parseLong(String.valueOf(attributes.get("bodySize"))),
                "Available BiDi bodySize must equal bytes actually sent by the uncompressed HTTP peer: " + url);
    }

    private static void assertExportedBodySizeAvailability(Map<String, Object> exported, List<NetworkEvent> captured) {
        Map<String, NetworkEvent> byId = captured.stream().collect(Collectors.toMap(NetworkEvent::id, event -> event));
        for (Map<String, Object> event : objects(exported.get("events"))) {
            if (!(event.get("response") instanceof Map)) continue;
            NetworkEvent source = byId.get(event.get("id"));
            assertNotNull(source, "Every exported response must come from the captured snapshot");
            Map<String, Object> attributes = object(event.get("attributes"));
            assertEquals(source.attributes().containsKey("bodySize"), attributes.containsKey("bodySize"),
                    "An unavailable response body size must remain absent in JSON, never become zero");
            assertEquals(source.attributes().get("bodySize"), attributes.get("bodySize"),
                    "JSON must preserve the captured response body size");
        }
    }

    private static void assertTraceReport(List<Observed> journal, Map<String, Object> report) {
        List<Map<String, Object>> attributes = objects(report.get("events")).stream()
                .filter(event -> event.get("attributes") instanceof Map)
                .map(event -> object(event.get("attributes")))
                .filter(item -> "RESPONSE".equals(item.get("metadata.networkEventType"))).toList();
        assertEquals(multiset(journal.stream().map(item -> item.method + " " + item.path + " " + item.status).toList()),
                multiset(attributes.stream().map(item -> item.get("metadata.method") + " " + item.get("metadata.url") + " " + item.get("metadata.status")).toList()),
                "Trace JSON must preserve every response, method and URL exactly once");
        assertTrue(attributes.stream().allMatch(item -> item.get("metadata.requestId") instanceof String id && !id.isBlank()));
    }

    private static void assertBundle(TestLensFinalizationResult result, List<Observed> journal, String secret) throws Exception {
        Path bundle = result.failureBundleDirectory().orElseThrow();
        Map<String, Object> summary = object(Files.readString(bundle.resolve("network-summary.json")));
        assertEquals(journal.size(), number(summary.get("totalRequests")));
        assertEquals(journal.size(), number(summary.get("totalResponses")));
        assertEquals(2, number(summary.get("failedResponses")));
        assertEquals(0, number(summary.get("failedRequests")));
        assertEquals(0, number(summary.get("pending")));
        Map<String, Long> statuses = journal.stream().collect(Collectors.groupingBy(item -> String.valueOf(item.status), Collectors.counting()));
        Map<String, Object> exportedStatuses = object(summary.get("responseStatuses"));
        assertEquals(statuses.keySet(), exportedStatuses.keySet());
        statuses.forEach((status, count) -> assertEquals(count.longValue(), number(exportedStatuses.get(status))));
        try (var files = Files.walk(bundle)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) assertSafe(Files.readString(file), secret);
        }
        boolean summaryInArchive = false;
        try (ZipFile zip = new ZipFile(result.failureBundleArchive().orElseThrow().toFile())) {
            for (var entry : zip.stream().filter(entry -> !entry.isDirectory()).toList()) {
                String content = new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
                assertSafe(content, secret);
                if (entry.getName().endsWith("network-summary.json")) {
                    assertEquals(summary, object(content));
                    summaryInArchive = true;
                }
            }
        }
        assertTrue(summaryInArchive, "ZIP must contain the matching network summary");
    }

    private static void assertSafe(String content, String secret) {
        // Do not let a failing assertion print the credential or unredacted artifact.
        assertFalse(content.contains(secret), "Generated diagnostic content contains an unredacted credential");
    }

    private static void fetch(WebDriver driver, String path, String method, String body, String secret) {
        Object status = ((JavascriptExecutor) driver).executeAsyncScript("""
                const [path, method, body, secret] = arguments, done = arguments[arguments.length - 1];
                const headers = body === null ? {} : {'Content-Type':'application/json'};
                if (secret !== null) headers['X-Api-Key'] = secret;
                fetch(path, {method, body, headers, cache:'no-store'})
                  .then(async response => { await response.text(); done(response.status); })
                  .catch(() => done(-1));
                """, path, method, body, secret);
        assertTrue(status instanceof Number && ((Number) status).intValue() >= 200,
                "Browser fetch must complete without a transport error");
    }

    private static void burst(WebDriver driver) {
        Object count = ((JavascriptExecutor) driver).executeAsyncScript("""
                const done = arguments[arguments.length - 1];
                Promise.all(Array.from({length:8}, (_, index) => fetch('/case/burst/' + index, {cache:'no-store'})
                  .then(async response => { await response.text(); return response.status; })))
                  .then(statuses => done(statuses.filter(status => status === 200).length)).catch(() => done(-1));
                """);
        assertEquals(8, number(count));
    }

    private static Map<String, Long> multiset(List<String> values) {
        return values.stream().collect(Collectors.groupingBy(value -> value, Collectors.counting()));
    }
    private static String header(Map<String, String> headers, String name) {
        return headers.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue).findFirst().orElse(null);
    }
    private static long number(Object value) { return ((Number) value).longValue(); }
    private static Map<String, Object> object(String json) {
        Map<?, ?> parsed = new Json().toType(json, Map.class);
        return object(parsed);
    }
    @SuppressWarnings("unchecked") private static Map<String, Object> object(Object value) { return (Map<String, Object>) value; }
    @SuppressWarnings("unchecked") private static List<Map<String, Object>> objects(Object value) { return (List<Map<String, Object>>) value; }

    private record Observed(String method, String path, int status, String body, long responseBodyBytes) {}

    private static final class Peer implements AutoCloseable {
        final HttpServer server;
        volatile CountDownLatch completed = new CountDownLatch(0);
        final ExecutorService executor = Executors.newCachedThreadPool();
        final List<Observed> journal = new CopyOnWriteArrayList<>();
        final String origin;
        volatile String secret = "";
        volatile boolean credentialsObserved;
        volatile String report = "";

        Peer() throws IOException {
            server = HttpServer.create(new InetSocketAddress(System.getProperty("test.network.bind", "127.0.0.1"), 0), 0);
            server.setExecutor(executor);
            server.createContext("/", this::serve);
            server.start();
            origin = "http://" + System.getProperty("test.network.host", "127.0.0.1") + ":" + server.getAddress().getPort();
        }

        private void serve(HttpExchange exchange) throws IOException {
            try (exchange) {
                String path = exchange.getRequestURI().getPath();
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                int status = switch (path) {
                    case "/case/redirect" -> 302;
                    case "/case/no-body", "/favicon.ico" -> 204;
                    case "/case/not-found" -> 404;
                    case "/case/server-error" -> 500;
                    default -> 200;
                };
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                exchange.getResponseHeaders().set("Content-Type", path.equals("/page") || path.equals("/report")
                        ? "text/html; charset=utf-8" : "application/json");
                if (path.equals("/case/redirect")) exchange.getResponseHeaders().set("Location", "/case/final");
                if (path.equals("/case/secret")) {
                    credentialsObserved = !secret.isBlank() && secret.equals(exchange.getRequestHeaders().getFirst("X-Api-Key"));
                    exchange.getResponseHeaders().set("Set-Cookie", "session=" + secret + "; HttpOnly");
                }
                byte[] bytes = (path.equals("/report") ? report : path.equals("/page")
                        ? "<!doctype html><title>Network peer</title><link rel=\"icon\" href=\"data:,\"><p>Ready</p>"
                        : "{\"ok\":true}").getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, status == 204 || status == 302 ? -1 : bytes.length);
                if (status != 204 && status != 302) exchange.getResponseBody().write(bytes);
                if (path.startsWith("/case/")) {
                    journal.add(new Observed(exchange.getRequestMethod(), path, status, body,
                            status == 204 || status == 302 ? 0L : bytes.length));
                    completed.countDown();
                }
            }
        }

        @Override public void close() {
            server.stop(0);
            executor.shutdownNow();
        }
    }
}
