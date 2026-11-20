import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.openqa.selenium.BuildInfo;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.BiDi;
import org.openqa.selenium.bidi.Event;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.HasCdp;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Isolated diagnostic. This class intentionally has no Lens imports. */
public final class LayerProbe {
    public interface LensCapture extends AutoCloseable {
        void start(WebDriver driver);
        void awaitResponse(String url, int status, long deadlineNanos);
        List<Map<String, Object>> snapshot();
        Map<String, Object> state();
        String exportJson();
        void close();
    }

    private static final List<String> TYPES = List.of("network.beforeRequestSent", "network.responseStarted",
            "network.responseCompleted", "network.fetchError");
    private final Object arrived = new Object();
    private final List<Map<String, Object>> phases = new ArrayList<>();
    private final List<Map<String, Object>> journal = new ArrayList<>();
    private final Map<String, String> ids = new LinkedHashMap<>();
    private final List<Map<String, Object>> expected = new ArrayList<>();
    private final List<Map<String, Object>> bodySizeMismatches = new ArrayList<>();
    private final Map<String, Object> evidence = new LinkedHashMap<>();
    private final String host = System.getProperty("probe.host", "127.0.0.1");
    private final String mode = System.getProperty("probe.mode", "A");
    private final int stimuli = Integer.getInteger("probe.stimuli", 50);
    private boolean overflow;
    private String origin;

    public static void main(String[] args) throws Exception {
        new LayerProbe().run();
    }

    private void run() throws Exception {
        Path output = Path.of(System.getProperty("probe.output", "target/controlled-http-bidi/evidence"));
        Files.createDirectories(output);
        WebDriver driver = null;
        BiDi bidi = null;
        LensCapture lens = null;
        HttpServer server = null;
        ExecutorService executor = null;
        List<Object> listenerIds = new ArrayList<>();
        Throwable failure = null;
        evidence.put("outcome", "NOTRUN");
        evidence.put("protocolOutcome", "NOTRUN");
        evidence.put("bodySizeOutcome", "NOTRUN");
        evidence.put("mode", mode);
        evidence.put("host", host);
        evidence.put("stimuli", stimuli);
        evidence.put("eventDeadlineMs", 3000);
        evidence.put("javaVersion", System.getProperty("java.version"));
        evidence.put("javaVendor", System.getProperty("java.vendor"));
        evidence.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        evidence.put("seleniumVersion", new BuildInfo().getReleaseLabel());
        evidence.put("httpsControl", Boolean.getBoolean("probe.httpsControl"));
        evidence.put("headless", "new");
        try {
            require(List.of("A", "B", "C").contains(mode), "INVALID_MODE");
            require(List.of("127.0.0.1", "localhost", "staging.example.org").contains(host), "INVALID_HOST");
            require(stimuli > 0 && stimuli <= 50, "INVALID_STIMULUS_COUNT");
            List<Map<String, Object>> runtimeClasspath = classpath();
            evidence.put("classpath", runtimeClasspath);
            require(new BuildInfo().getReleaseLabel().equals("4.53.0"), "SELENIUM_VERSION_NOT_453");
            require(!mode.equals("A") || runtimeClasspath.stream().noneMatch(row -> String.valueOf(row.get("file"))
                    .matches("(?i)(lens|core|overlay|legacy)\\.jar|selenium-test-lens-.*\\.jar")), "LENS_JAR_ON_MODE_A_CLASSPATH");
            boolean lensVisible;
            try { Class.forName("io.github.testlens.selenium.network.NetworkDiagnostics", false,
                    LayerProbe.class.getClassLoader()); lensVisible = true; }
            catch (ClassNotFoundException absent) { lensVisible = false; }
            evidence.put("lensRuntimeVisible", lensVisible);
            require(!mode.equals("A") || !lensVisible, "LENS_RUNTIME_PRESENT_IN_MODE_A");
            require(mode.equals("A") || lensVisible, "LENS_RUNTIME_MISSING");

            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            executor = Executors.newCachedThreadPool(r -> { Thread t = new Thread(r, "controlled-http-peer"); t.setDaemon(true); return t; });
            server.setExecutor(executor);
            server.createContext("/", this::serve);
            server.start();
            int port = server.getAddress().getPort();
            origin = "http://" + host + (host.equals("staging.example.org") ? "" : ":" + port);
            evidence.put("fixtureOrigin", origin);
            evidence.put("fixtureLoopbackPort", port);
            ChromeOptions options = new ChromeOptions().enableBiDi();
            String binary = System.getProperty("chrome.binary", "");
            require(!binary.isBlank(), "CHROME_BINARY_REQUIRED");
            options.setBinary(binary);
            List<String> chromeArguments = new ArrayList<>(List.of("--headless=new", "--no-sandbox", "--disable-dev-shm-usage"));
            if (host.equals("staging.example.org")) {
                chromeArguments.add("--host-resolver-rules=MAP staging.example.org:80 127.0.0.1:" + port
                        + ",MAP staging.example.org:443 127.0.0.1:9");
                chromeArguments.add("--no-proxy-server");
                evidence.put("proxyIsolation", "DIRECT for public-alias control only; not an HTTPS feature workaround");
            }
            if (Boolean.getBoolean("probe.httpsControl")) {
                require(System.getProperties().containsKey("probe.baseDisabledFeatures"), "PAIRED_DEFAULT_DISABLED_FEATURES_REQUIRED");
                String baseFeatures = System.getProperty("probe.baseDisabledFeatures");
                require(baseFeatures.length() <= 8192 && baseFeatures.matches("[A-Za-z0-9_.,<>:/()*+\\-]*"), "INVALID_BASE_DISABLED_FEATURES");
                Set<String> disabled = new LinkedHashSet<>();
                for (String feature : baseFeatures.split(",")) if (!feature.isBlank()) disabled.add(feature);
                disabled.add("HttpsUpgrades");
                disabled.add("HttpsFirstBalancedModeAutoEnable");
                chromeArguments.add("--disable-features=" + String.join(",", disabled));
                evidence.put("baseDisabledFeaturesFromPairedDefault", baseFeatures);
            }
            options.addArguments(chromeArguments);
            evidence.put("requestedChromeArguments", List.copyOf(chromeArguments));
            String grid = System.getProperty("probe.grid", "").trim();
            if (mode.equals("C")) {
                URI endpoint = URI.create(grid);
                require(List.of("http", "https").contains(endpoint.getScheme()) && endpoint.getUserInfo() == null
                        && endpoint.getQuery() == null && endpoint.getFragment() == null
                        && List.of("127.0.0.1", "localhost").contains(endpoint.getHost()), "GRID_MUST_BE_OWNED_LOOPBACK");
                evidence.put("gridEndpoint", endpoint.toString());
                driver = new Augmenter().augment(new RemoteWebDriver(endpoint.toURL(), options));
            } else {
                require(grid.isEmpty(), "GRID_ONLY_ALLOWED_IN_MODE_C");
                driver = new ChromeDriver(options);
            }
            var capabilities = ((HasCapabilities) driver).getCapabilities();
            evidence.put("browserName", capabilities.getBrowserName());
            evidence.put("browserVersion", capabilities.getBrowserVersion());
            evidence.put("platformName", String.valueOf(capabilities.getPlatformName()));
            Map<?, ?> chrome = map(capabilities.getCapability("chrome"));
            evidence.put("chromeDriverVersion", String.valueOf(chrome.getOrDefault("chromedriverVersion", null)).split(" ")[0]);
            require(capabilities.getBrowserVersion().equals(evidence.get("chromeDriverVersion")), "BROWSER_DRIVER_VERSION_MISMATCH");
            evidence.put("webSocketUrlExposed", capabilities.getCapability("webSocketUrl") instanceof String);
            recordFeatureArguments(driver);
            require(driver instanceof HasBiDi, "NO_PUBLIC_BIDI");
            bidi = ((HasBiDi) driver).getBiDi();
            for (String type : TYPES) {
                listenerIds.add(bidi.addListener(new Event<Map<String, Object>>(type, payload -> payload), payload -> record(type, payload)));
            }
            if (!mode.equals("A")) {
                lens = (LensCapture) Class.forName("LensProbeBridge").getDeclaredConstructor().newInstance();
                lens.start(driver);
            }
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(12));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(10));
            evidence.put("outcome", "INCOMPLETE");
            String navigation = origin + "/ok?caseId=page";
            expect(navigation, 200);
            driver.get(navigation);
            evidence.put("browserUrlAfterNavigation", browserUrl(driver));
            awaitCase(List.of(navigation), lens);

            String json = origin + "/json?caseId=json";
            expect(json, 200);
            fetch(driver, json);
            awaitCase(List.of(json), lens);
            for (int stimulus = 1; stimulus <= stimuli; stimulus++) {
                evidence.put("currentStimulus", stimulus);
                String redirect = origin + "/redirect?caseId=" + stimulus;
                String target = origin + "/ok?caseId=" + stimulus;
                expect(redirect, 302);
                expect(target, 200);
                fetch(driver, redirect);
                awaitCase(List.of(redirect, target), lens);
                evidence.put("completedStimuli", stimulus);
            }
            verifyRaw();
            if (lens != null) {
                verifyLens(lens.snapshot());
                writeSafeLensExport(output, lens.exportJson());
            }
            evidence.put("protocolOutcome", "PASS");
            evidence.put("bodySizeOutcome", bodySizeMismatches.isEmpty() ? "PASS_OR_UNAVAILABLE" : "FAIL");
            require(bodySizeMismatches.isEmpty(), "PROVIDED_BODY_SIZE_DISAGREES_WITH_PEER");
            evidence.put("outcome", "PASS");
        } catch (Throwable problem) {
            failure = problem;
            evidence.put("outcome", "FAIL");
            if (!"PASS".equals(evidence.get("protocolOutcome"))) evidence.put("protocolOutcome", "FAIL");
            if (!bodySizeMismatches.isEmpty()) evidence.put("bodySizeOutcome", "FAIL");
            evidence.put("failureType", problem.getClass().getName());
            evidence.put("failureCode", problem instanceof IllegalStateException && problem.getMessage() != null
                    && problem.getMessage().matches("[A-Z0-9_]+") ? problem.getMessage() : "WEBDRIVER_OR_DIAGNOSTIC_OPERATION_FAILED");
        } finally {
            if (driver != null) {
                try { evidence.put("browserUrlAtEnd", browserUrl(driver)); }
                catch (Throwable ignored) { evidence.put("browserUrlAtEnd", "UNAVAILABLE"); }
            }
            if (lens != null) {
                try { evidence.put("lensStateBeforeStop", lens.state()); evidence.put("lensEvents", lens.snapshot());
                    writeSafeLensExport(output, lens.exportJson()); }
                catch (Throwable cleanup) { failure = cleanupFailure(failure, cleanup); }
                try { lens.close(); evidence.put("lensStateAfterStop", lens.state()); }
                catch (Throwable cleanup) { failure = cleanupFailure(failure, cleanup); }
            }
            if (bidi != null) {
                for (Object id : listenerIds) {
                    try { removeListener(bidi, id); }
                    catch (Throwable cleanup) { failure = cleanupFailure(failure, cleanup); }
                }
                try { bidi.close(); }
                catch (Throwable cleanup) { failure = cleanupFailure(failure, cleanup); }
            }
            if (driver != null) try { driver.quit(); } catch (Throwable cleanup) { failure = cleanupFailure(failure, cleanup); }
            if (server != null) server.stop(0);
            if (executor != null) executor.shutdownNow();
            synchronized (arrived) {
                evidence.put("phases", List.copyOf(phases));
                evidence.put("serverJournal", journal.stream().map(LinkedHashMap::new).toList());
                evidence.put("expected", List.copyOf(expected));
                evidence.put("evidenceOverflow", overflow);
            }
            evidence.put("bodySizeMismatches", List.copyOf(bodySizeMismatches));
            if (overflow || failure != null && "PASS".equals(evidence.get("outcome"))) evidence.put("outcome", "FAIL");
            Files.writeString(output.resolve("evidence.json"), new Json().toJson(evidence), StandardCharsets.UTF_8);
            System.out.println(new Json().toJson(Map.of("mode", mode, "host", host, "outcome", evidence.get("outcome"),
                    "protocolOutcome", evidence.get("protocolOutcome"), "bodySizeOutcome", evidence.get("bodySizeOutcome"),
                    "completedStimuli", evidence.getOrDefault("completedStimuli", 0), "evidence", output.resolve("evidence.json").toString())));
        }
        if (!"PASS".equals(evidence.get("outcome"))) System.exit(1);
    }

    private Throwable cleanupFailure(Throwable failure, Throwable cleanup) {
        evidence.put("cleanupFailed", true);
        if (failure == null) return cleanup;
        failure.addSuppressed(cleanup);
        return failure;
    }

    private void expect(String url, int status) { expected.add(Map.of("url", url, "status", status)); }

    private void fetch(WebDriver driver, String url) {
        Object result = ((JavascriptExecutor) driver).executeAsyncScript("""
            const url = arguments[0], done = arguments[arguments.length - 1];
            fetch(url, {cache:'no-store'}).then(async response => {
                await response.text(); done({status:response.status,url:response.url});
            }).catch(() => done({status:-1,url:''}));
            """, url);
        require(number(map(result).get("status")).intValue() == 200, "NATIVE_FETCH_DID_NOT_FINISH_WITH_200");
    }

    private void awaitCase(List<String> urls, LensCapture lens) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        synchronized (arrived) {
            while (!caseReady(urls)) {
                long remaining = deadline - System.nanoTime();
                require(remaining > 0 && !overflow, "RAW_OR_SERVER_COMPLETION_MISMATCH");
                TimeUnit.NANOSECONDS.timedWait(arrived, remaining);
            }
        }
        if (lens != null) for (String url : urls) {
            int status = expected.stream().filter(row -> url.equals(row.get("url"))).findFirst().map(row -> number(row.get("status")).intValue()).orElseThrow();
            lens.awaitResponse(url, status, deadline);
        }
    }

    private boolean caseReady(List<String> urls) {
        for (String url : urls) {
            int status = expected.stream().filter(row -> url.equals(row.get("url"))).findFirst().map(row -> number(row.get("status")).intValue()).orElseThrow();
            long completions = phases.stream().filter(row -> "network.responseCompleted".equals(row.get("type"))
                    && url.equals(row.get("requestUrl")) && url.equals(row.get("responseUrl")) && number(row.get("status")).intValue() == status).count();
            long server = journal.stream().filter(row -> url.equals(row.get("url")) && number(row.get("status")).intValue() == status && Boolean.TRUE.equals(row.get("completed"))).count();
            if (completions != 1 || server != 1) return false;
        }
        return true;
    }

    private void record(String type, Map<String, Object> payload) {
        Map<?, ?> request = map(payload.get("request")), response = map(payload.get("response"));
        String requestUrl = safeUrl(request.get("url")), responseUrl = safeUrl(response.get("url"));
        if (requestUrl.isEmpty() && responseUrl.isEmpty()) return;
        synchronized (arrived) {
            if (phases.size() >= 2000) { overflow = true; arrived.notifyAll(); return; }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type", type);
            row.put("requestUrl", requestUrl);
            row.put("responseUrl", responseUrl);
            row.put("method", "GET".equals(request.get("method")) ? "GET" : "<other-method>");
            String id = String.valueOf(request.get("request"));
            row.put("requestId", ids.computeIfAbsent(id, ignored -> "request-" + (ids.size() + 1)));
            row.put("requestIdAvailable", request.get("request") instanceof String s && !s.isBlank());
            row.put("hop", number(payload.get("redirectCount")));
            row.put("status", number(response.get("status")));
            row.put("bidiTimestamp", number(payload.get("timestamp")));
            row.put("observedAt", Instant.now().toString());
            row.put("location", safeLocation(response.get("headers"), requestUrl));
            if (response.get("bodySize") instanceof Number n) row.put("bodySize", n);
            phases.add(row);
            arrived.notifyAll();
        }
    }

    private void verifyRaw() {
        synchronized (arrived) {
            require(!overflow, "EVIDENCE_OVERFLOW");
            require(journal.size() == expected.size(), "SERVER_REQUEST_COUNT_MISMATCH");
            for (Map<String, Object> wanted : expected) {
                String url = String.valueOf(wanted.get("url"));
                int status = number(wanted.get("status")).intValue();
                List<Map<String, Object>> peerRows = journal.stream().filter(row -> url.equals(row.get("url"))).toList();
                require(peerRows.size() == 1, "SERVER_REQUEST_NOT_UNIQUE");
                Map<String, Object> peer = peerRows.get(0);
                require(Boolean.TRUE.equals(peer.get("completed")) && "GET".equals(peer.get("method"))
                        && number(peer.get("status")).intValue() == status, "SERVER_REQUEST_DID_NOT_FINISH");
                require(!Instant.parse(String.valueOf(peer.get("finishedAt"))).isBefore(Instant.parse(String.valueOf(peer.get("receivedAt")))), "SERVER_FINISH_PRECEDES_RECEIPT");
                List<Map<String, Object>> requestRows = phases.stream().filter(row -> "network.beforeRequestSent".equals(row.get("type")) && url.equals(row.get("requestUrl"))).toList();
                require(requestRows.size() == 1, "RAW_REQUEST_NOT_UNIQUE");
                Map<String, Object> request = requestRows.get(0);
                require(Boolean.TRUE.equals(request.get("requestIdAvailable")) && "GET".equals(request.get("method")), "RAW_REQUEST_ID_OR_METHOD_INVALID");
                long expectedHop = URI.create(url).getPath().equals("/ok") && URI.create(url).getQuery().matches("caseId=[0-9]+") ? 1 : 0;
                require(number(request.get("hop")).longValue() == expectedHop, "RAW_REDIRECT_HOP_INVALID");
                if (status == 302) {
                    String target = String.valueOf(peer.get("location"));
                    List<Map<String, Object>> finals = phases.stream().filter(row -> "network.beforeRequestSent".equals(row.get("type"))
                            && target.equals(row.get("requestUrl"))).toList();
                    require(finals.size() == 1 && request.get("requestId").equals(finals.get(0).get("requestId"))
                            && number(finals.get(0).get("hop")).longValue() == 1, "RAW_REDIRECT_CHAIN_ID_OR_HOP_MISMATCH");
                }
                for (String type : List.of("network.responseStarted", "network.responseCompleted")) {
                    List<Map<String, Object>> rows = phases.stream().filter(row -> type.equals(row.get("type")) && url.equals(row.get("requestUrl"))).toList();
                    require(rows.size() == 1, "RAW_RESPONSE_NOT_UNIQUE");
                    Map<String, Object> row = rows.get(0);
                    require(url.equals(row.get("responseUrl")) && number(row.get("status")).intValue() == status, "RAW_RESPONSE_URL_OR_STATUS_MISMATCH");
                    require(request.get("requestId").equals(row.get("requestId")) && number(request.get("hop")).longValue() == number(row.get("hop")).longValue(), "RAW_ID_HOP_CORRELATION_MISMATCH");
                    if (status == 302) require(peer.get("location").equals(row.get("location")), "RAW_LOCATION_MISMATCH");
                    if (type.equals("network.responseCompleted")) compareBody("RAW", row, peer);
                }
            }
            require(phases.stream().noneMatch(row -> "network.fetchError".equals(row.get("type"))), "RAW_FETCH_ERROR");
            require(phases.size() == expected.size() * 3, "UNEXPECTED_RAW_PHASES");
            require(journal.stream().map(row -> row.get("serverRequestId")).distinct().count() == journal.size(), "SERVER_IDS_NOT_UNIQUE");
        }
    }

    private void verifyLens(List<Map<String, Object>> rows) {
        for (Map<String, Object> wanted : expected) {
            String url = String.valueOf(wanted.get("url"));
            int status = number(wanted.get("status")).intValue();
            List<Map<String, Object>> requests = rows.stream().filter(row -> "REQUEST".equals(row.get("type")) && url.equals(row.get("url"))).toList();
            List<Map<String, Object>> responses = rows.stream().filter(row -> "RESPONSE".equals(row.get("type")) && url.equals(row.get("url"))).toList();
            require(requests.size() == 1 && responses.size() == 1, "LENS_REQUEST_OR_RESPONSE_NOT_UNIQUE");
            Map<String, Object> request = requests.get(0), response = responses.get(0);
            require("GET".equals(request.get("method")) && number(response.get("status")).intValue() == status, "LENS_METHOD_OR_STATUS_MISMATCH");
            String expectedHop = URI.create(url).getPath().equals("/ok") && URI.create(url).getQuery().matches("caseId=[0-9]+") ? "1" : "0";
            require(expectedHop.equals(request.get("hop")), "LENS_REDIRECT_HOP_INVALID");
            require(request.get("requestId").equals(response.get("requestId")) && request.get("hop").equals(response.get("hop")), "LENS_ID_HOP_CORRELATION_MISMATCH");
            Map<String, Object> peer = journal.stream().filter(row -> url.equals(row.get("url"))).findFirst().orElseThrow();
            if (status == 302) require(peer.get("location").equals(response.get("location")), "LENS_LOCATION_MISMATCH");
            if (status == 302) {
                List<Map<String, Object>> finals = rows.stream().filter(row -> "REQUEST".equals(row.get("type"))
                        && peer.get("location").equals(row.get("url"))).toList();
                require(finals.size() == 1 && request.get("requestId").equals(finals.get(0).get("requestId"))
                        && "1".equals(finals.get(0).get("hop")), "LENS_REDIRECT_CHAIN_ID_OR_HOP_MISMATCH");
            }
            compareBody("LENS", response, peer);
        }
        require(rows.size() == expected.size() * 2, "UNEXPECTED_LENS_EVENTS");
        require(rows.stream().map(row -> row.get("eventId")).distinct().count() == rows.size(), "LENS_EVENT_IDS_NOT_UNIQUE");
    }

    private void compareBody(String layer, Map<String, Object> row, Map<String, Object> peer) {
        if (!row.containsKey("bodySize")) return;
        if (number(row.get("bodySize")).longValue() != number(peer.get("bodyBytes")).longValue()) {
            bodySizeMismatches.add(Map.of("layer", layer, "url", peer.get("url"), "bodySize", row.get("bodySize"), "peerBodyBytes", peer.get("bodyBytes")));
        }
    }

    private void writeSafeLensExport(Path output, String json) throws Exception {
        Map<?, ?> exported = new Json().toType(json, Map.class);
        List<Map<String, Object>> safe = new ArrayList<>();
        Object all = exported.get("events");
        if (all instanceof List<?> events) for (Object item : events) {
            Map<?, ?> event = map(item), request = map(event.get("request")), response = map(event.get("response"));
            Map<?, ?> details = request.isEmpty() ? response : request;
            String url = safeUrl(details.get("url"));
            if (url.isEmpty()) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("eventId", String.valueOf(event.get("id")));
            row.put("type", String.valueOf(event.get("type")));
            row.put("url", url);
            row.put("timestamp", event.get("timestamp"));
            row.put("requestId", details.get(request.isEmpty() ? "requestId" : "id"));
            if (!request.isEmpty()) row.put("method", "GET".equals(request.get("method")) ? "GET" : "<other-method>");
            if (!response.isEmpty()) row.put("status", number(response.get("status")));
            Map<?, ?> attributes = map(event.get("attributes"));
            row.put("hop", String.valueOf(attributes.get("redirectCount")));
            if (attributes.containsKey("bodySize")) row.put("bodySize", attributes.get("bodySize"));
            safe.add(row);
        }
        Files.writeString(output.resolve("lens-export.json"), new Json().toJson(Map.of("events", safe,
                "requestedCaptureMode", String.valueOf(exported.get("requestedCaptureMode")),
                "activeCaptureMode", String.valueOf(exported.get("activeCaptureMode")))), StandardCharsets.UTF_8);
    }

    private void serve(HttpExchange exchange) throws java.io.IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/favicon.ico")) { exchange.sendResponseHeaders(204, -1); exchange.close(); return; }
        String query = exchange.getRequestURI().getRawQuery();
        String authority = exchange.getRequestHeaders().getFirst("Host");
        String url = safeUrl("http://" + authority + path + (query == null ? "" : "?" + query));
        if (url.isEmpty()) { exchange.sendResponseHeaders(404, -1); exchange.close(); return; }
        int status = path.equals("/redirect") ? 302 : 200;
        String location = status == 302 ? URI.create(url).resolve("/ok?" + query).toString() : "";
        byte[] bytes = (path.equals("/json") ? "{\"ok\":true}" : "<!doctype html><title>Controlled HTTP</title><link rel=\"icon\" href=\"data:,\"><p>OK</p>").getBytes(StandardCharsets.UTF_8);
        Map<String, Object> row = new LinkedHashMap<>();
        synchronized (arrived) {
            if (journal.size() >= 256) { overflow = true; exchange.sendResponseHeaders(503, -1); exchange.close(); arrived.notifyAll(); return; }
            row.put("serverRequestId", "server-" + (journal.size() + 1));
            row.put("caseId", query.substring("caseId=".length()));
            row.put("method", exchange.getRequestMethod().equals("GET") ? "GET" : "<other-method>");
            row.put("url", url);
            row.put("receivedAt", Instant.now().toString());
            row.put("finishedAt", "");
            row.put("location", location);
            row.put("status", status);
            row.put("bodyBytes", status == 302 ? 0 : bytes.length);
            row.put("completed", false);
            journal.add(row);
        }
        boolean completed = false;
        try {
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.getResponseHeaders().set("Content-Type", path.equals("/json") ? "application/json" : "text/html; charset=utf-8");
            if (status == 302) exchange.getResponseHeaders().set("Location", "/ok?" + query);
            exchange.sendResponseHeaders(status, status == 302 ? -1 : bytes.length);
            if (status != 302) exchange.getResponseBody().write(bytes);
            exchange.close();
            completed = true;
        } finally {
            exchange.close();
            synchronized (arrived) {
                row.put("finishedAt", Instant.now().toString());
                row.put("completed", completed);
                arrived.notifyAll();
            }
        }
    }

    public static String safeUrl(Object candidate) {
        if (!(candidate instanceof String value) || value.isBlank()) return "";
        try {
            URI uri = URI.create(value);
            if (uri.getScheme() == null || uri.getHost() == null || uri.getPath() == null
                    || !List.of("http", "https").contains(uri.getScheme()) || uri.getUserInfo() != null || uri.getFragment() != null
                    || !List.of("127.0.0.1", "localhost", "staging.example.org").contains(uri.getHost())
                    || !List.of("/ok", "/redirect", "/json").contains(uri.getPath())
                    || uri.getRawQuery() == null || !uri.getRawQuery().matches("caseId=(page|json|[1-9]|[1-4][0-9]|50)")) return "";
            return uri.toString();
        } catch (IllegalArgumentException invalid) { return ""; }
    }

    public static String location(String value, String base) {
        if (value == null || value.isBlank() || base.isBlank()) return "";
        try { return safeUrl(URI.create(base).resolve(value).toString()); }
        catch (IllegalArgumentException invalid) { return ""; }
    }

    private static String safeLocation(Object headers, String base) {
        if (headers instanceof List<?> list) for (Object raw : list) {
            Map<?, ?> header = map(raw);
            if ("location".equalsIgnoreCase(String.valueOf(header.get("name")))) {
                Object value = header.get("value");
                return location(value instanceof String s ? s : String.valueOf(map(value).get("value")), base);
            }
        }
        return "";
    }

    private static String browserUrl(WebDriver driver) {
        String url = driver.getCurrentUrl();
        String safe = safeUrl(url);
        return !safe.isEmpty() ? safe : List.of("about:blank", "chrome-error://chromewebdata/").contains(url) ? url : "<outside-controlled-fixture>";
    }

    private void recordFeatureArguments(WebDriver driver) {
        // Read only feature switches. Never retain the whole command line: it
        // contains profile paths and debugger endpoints unrelated to this probe.
        if (!(driver instanceof HasCdp cdp)) {
            evidence.put("featureCommandLineOutcome", "UNAVAILABLE_NO_PUBLIC_HAS_CDP");
            require(!Boolean.getBoolean("probe.httpsControl"), "CONTROL_FEATURE_COMMAND_LINE_NOT_VERIFIED");
            return;
        }
        try {
            Object arguments = cdp.executeCdpCommand("Browser.getBrowserCommandLine", Map.of()).get("arguments");
            List<String> features = new ArrayList<>();
            if (arguments instanceof List<?> list) for (Object argument : list) {
                if (argument instanceof String value && (value.startsWith("--disable-features=") || value.startsWith("--enable-features="))) {
                    features.add(value);
                }
            }
            evidence.put("actualFeatureArguments", List.copyOf(features));
            List<String> disabled = features.stream().filter(value -> value.startsWith("--disable-features=")).toList();
            evidence.put("actualDisabledFeatures", disabled.size() == 1 ? disabled.get(0).substring("--disable-features=".length()) : "");
            evidence.put("featureCommandLineOutcome", disabled.size() > 1 ? "AMBIGUOUS_MULTIPLE_DISABLE_FLAGS" : "CAPTURED");
            if (Boolean.getBoolean("probe.httpsControl")) {
                require(disabled.size() == 1, "CONTROL_DISABLE_FEATURES_AMBIGUOUS");
                Set<String> actual = new LinkedHashSet<>(List.of(disabled.get(0).substring("--disable-features=".length()).split(",")));
                for (String base : System.getProperty("probe.baseDisabledFeatures").split(",")) {
                    require(base.isBlank() || actual.contains(base), "CONTROL_DROPPED_DEFAULT_DISABLED_FEATURE");
                }
                require(actual.contains("HttpsUpgrades") && actual.contains("HttpsFirstBalancedModeAutoEnable"), "CONTROL_HTTPS_FEATURES_MISSING");
            }
        } catch (IllegalStateException failure) {
            throw failure;
        } catch (RuntimeException unavailable) {
            evidence.put("featureCommandLineOutcome", "UNAVAILABLE_CDP_COMMAND");
            require(!Boolean.getBoolean("probe.httpsControl"), "CONTROL_FEATURE_COMMAND_LINE_NOT_VERIFIED");
        }
    }

    private static List<Map<String, Object>> classpath() throws Exception {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (String part : System.getProperty("java.class.path").split(java.io.File.pathSeparator)) {
            Path path = Path.of(part);
            if (Files.isRegularFile(path)) entries.add(Map.of("file", path.getFileName().toString(), "sha256",
                    java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)))));
        }
        return entries;
    }

    private static Map<?, ?> map(Object value) { return value instanceof Map<?, ?> result ? result : Map.of(); }
    private static Number number(Object value) { return value instanceof Number n ? n : -1; }
    private static void require(boolean condition, String code) { if (!condition) throw new IllegalStateException(code); }
    private static void removeListener(BiDi bidi, Object id) throws Exception {
        for (Method method : BiDi.class.getMethods()) if (method.getName().equals("removeListener") && method.getParameterCount() == 1
                && (method.getParameterTypes()[0].isInstance(id) || method.getParameterTypes()[0] == long.class && id instanceof Long)) {
            method.invoke(bidi, id);
            return;
        }
        throw new NoSuchMethodException("Public BiDi.removeListener");
    }
}
