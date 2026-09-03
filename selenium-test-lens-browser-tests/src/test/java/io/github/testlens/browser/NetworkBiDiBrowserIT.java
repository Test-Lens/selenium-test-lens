package io.github.testlens.browser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.testlens.OverlayConfig;
import io.github.testlens.TestLens;
import io.github.testlens.TestLensFinalizationResult;
import io.github.testlens.TestLensOptions;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.selenium.network.NetworkCaptureMode;
import io.github.testlens.selenium.network.NetworkDiagnostics;
import io.github.testlens.selenium.network.NetworkDiagnosticsOptions;
import io.github.testlens.selenium.network.NetworkEvent;
import io.github.testlens.selenium.network.NetworkEventType;
import io.github.testlens.selenium.network.NetworkWaitCondition;
import io.github.testlens.selenium.network.NetworkWaitResult;
import io.github.testlens.selenium.network.NetworkWaitStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class NetworkBiDiBrowserIT {
    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", NetworkBiDiBrowserIT::serve);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        System.out.println("Dedicated network integration tests: WebDriver BiDi enabled for " + browserName());
    }

    @AfterAll
    static void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void bidiCapturesRealTrafficWaitsRedirectsLimitsLifecycleAndFailureBundle() throws Exception {
        WebDriver driver = createBiDiDriver();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Path output = Path.of("target", "ui-test-lens", browserName(), "bidi-network");
            TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
                    .overlayConfig(OverlayConfig.builder().enabled(false).build())
                    .screenshotOnFailure(false)
                    .outputRoot(output)
                    .build());
            lens.startSession("bidi-network-real-browser");
            NetworkDiagnostics network = lens.network().start(NetworkDiagnosticsOptions.builder()
                    .captureMode(NetworkCaptureMode.BIDI)
                    .includeHeaders(true)
                    .ignoreUrlPattern(".*/ignored(?:\\?.*)?")
                    .build());
            assertTrue(network.isStarted());
            assertEquals(NetworkCaptureMode.BIDI, network.activeCaptureMode().orElseThrow());

            driver.get(baseUrl + "/network-page");
            Future<NetworkWaitResult> wait = executor.submit(() -> network.waitForResponse(
                    NetworkWaitCondition.builder().urlContains("/api/success").status(201)
                            .timeout(Duration.ofSeconds(5)).pollInterval(Duration.ofSeconds(2)).build()));
            fetch(driver, "/api/success", true);
            NetworkWaitResult matched = wait.get();
            assertEquals(NetworkWaitStatus.MATCHED, matched.status());
            assertEquals(201, matched.matchedResponse().status());
            assertEquals(matched.matchedRequest().id(), matched.matchedResponse().requestId());

            fetch(driver, "/api/failure", false);
            fetch(driver, "/redirect", false);
            fetch(driver, "/ignored", false);
            fetch(driver, "/api/fetch-error", false);

            awaitSummary(driver, network, 1, 1);
            NetworkEvent sensitive = network.events().stream()
                    .filter(event -> event.request() != null && event.request().url().contains("/api/success"))
                    .findFirst().orElseThrow();
            assertEquals("***", header(sensitive, "x-api-key"));
            assertFalse(network.events().stream().anyMatch(event -> event.url().contains("/ignored")));
            assertTrue(network.summary().ignoredEvents() >= 1);
            assertTrue(network.summary().failedResponses() >= 1);
            assertTrue(network.summary().failedRequests() >= 1);

            NetworkEvent finalResponse = network.events().stream()
                    .filter(event -> event.type() == NetworkEventType.RESPONSE)
                    .filter(event -> event.url().contains("/api/final"))
                    .findFirst().orElseThrow();
            assertTrue(Integer.parseInt(finalResponse.attributes().get("redirectCount")) >= 1);
            assertTrue(network.events().stream()
                    .filter(event -> event.request() != null)
                    .anyMatch(event -> event.request().id().equals(finalResponse.response().requestId())
                            && event.attributes().get("redirectCount").equals(finalResponse.attributes().get("redirectCount"))));

            String json = network.exportJson();
            assertTrue(json.contains("\"requestedCaptureMode\":\"BIDI\""));
            assertTrue(json.contains("\"activeCaptureMode\":\"BIDI\""));
            assertTrue(json.contains("redirectCount"));

            int beforeRestart = countResponses(network.events(), "/api/restart");
            network.start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.BIDI).build());
            fetch(driver, "/api/restart", false);
            network.waitForResponse("/api/restart", 200);
            assertEquals(beforeRestart + 1, countResponses(network.events(), "/api/restart"));

            int beforeStop = network.summary().totalResponses();
            network.stop();
            fetch(driver, "/api/after-stop", false);
            assertEquals(beforeStop, network.summary().totalResponses());
            assertFalse(driver.getTitle().isBlank(), "Network stop must not close the driver");

            network.start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.BIDI).build());
            fetch(driver, "/api/bundle", false);
            assertEquals(NetworkWaitStatus.MATCHED, network.waitForResponse("/api/bundle", 200).status());
            TestLensFinalizationResult result = lens.finishFailed(new AssertionError("expected BiDi bundle failure"));
            assertEquals(TraceStatus.FAILED, result.session().metadata().status());
            assertFalse(network.isStarted(), "Lens finalization must stop active capture");
            String summary = Files.readString(result.outputDirectory()
                    .resolve("failure-bundle/network-summary.json"));
            assertTrue(summary.contains("\"requestedCaptureMode\""));
            assertTrue(summary.contains("\"activeCaptureMode\""));
            assertTrue(summary.contains("\"BIDI\""));
            assertTrue(result.failureBundleArchive().isPresent());
            assertFalse(driver.getTitle().isBlank(), "Lens finalization must leave the driver alive");
        } finally {
            executor.shutdownNow();
            driver.quit();
        }
    }

    @Test
    void autoSelectsRealBiDiWithoutFallback() {
        WebDriver driver = createBiDiDriver();
        try {
            NetworkDiagnostics network = new NetworkDiagnostics(driver).start(NetworkDiagnosticsOptions.builder()
                    .captureMode(NetworkCaptureMode.AUTO).build());
            assertTrue(network.isStarted());
            assertEquals(NetworkCaptureMode.AUTO, network.captureMode());
            assertEquals(NetworkCaptureMode.BIDI, network.activeCaptureMode().orElseThrow());
            network.stop();
            assertFalse(driver.getWindowHandle().isBlank());
        } finally {
            driver.quit();
        }
    }

    private static void awaitSummary(WebDriver driver, NetworkDiagnostics network,
                                     int failedResponses, int failedRequests) {
        NetworkWaitResult failedStatus = network.waitForResponse(NetworkWaitCondition.builder()
                .urlContains("/api/failure").status(503).includeFailedResponses(true)
                .timeout(Duration.ofSeconds(5)).build());
        assertEquals(NetworkWaitStatus.MATCHED, failedStatus.status());
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(ignored ->
                network.summary().failedResponses() >= failedResponses
                        && network.summary().failedRequests() >= failedRequests);
    }

    private static Object fetch(WebDriver driver, String path, boolean sensitiveHeader) {
        return ((JavascriptExecutor) driver).executeAsyncScript("""
                const url = arguments[0], sensitive = arguments[1], done = arguments[arguments.length - 1];
                const headers = sensitive ? {'X-Api-Key':'browser-secret'} : {};
                fetch(url, {headers}).then(async response => {
                  try { await response.text(); } catch (ignored) {}
                  done({status: response.status});
                }).catch(error => done({error: String(error)}));
                """, baseUrl + path, sensitiveHeader);
    }

    private static String header(NetworkEvent event, String name) {
        return event.request().headers().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(java.util.Map.Entry::getValue).findFirst().orElse(null);
    }

    private static int countResponses(List<NetworkEvent> events, String urlPart) {
        return (int) events.stream().filter(event -> event.response() != null)
                .filter(event -> event.response().url().contains(urlPart)).count();
    }

    private static WebDriver createBiDiDriver() {
        boolean headed = Boolean.parseBoolean(System.getProperty("headed", "false"));
        return switch (browserName()) {
            case "chrome" -> {
                ChromeOptions options = new ChromeOptions().enableBiDi();
                options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
                String configuredBinary = System.getProperty("test.chrome.binary", "").trim();
                if (!configuredBinary.isEmpty()) options.setBinary(configuredBinary);
                options.addArguments("--window-size=1280,900", "--disable-dev-shm-usage", "--no-sandbox");
                if (!headed) options.addArguments("--headless=new");
                yield new ChromeDriver(options);
            }
            case "firefox" -> {
                FirefoxOptions options = new FirefoxOptions().enableBiDi();
                options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
                String configuredBinary = System.getProperty("test.firefox.binary", "").trim();
                if (!configuredBinary.isEmpty()) options.setBinary(configuredBinary);
                if (!headed) options.addArguments("-headless");
                WebDriver firefox = new FirefoxDriver(options);
                firefox.manage().window().setSize(new org.openqa.selenium.Dimension(1280, 900));
                yield firefox;
            }
            default -> throw new IllegalArgumentException("Unsupported browser: " + browserName());
        };
    }

    private static String browserName() {
        return System.getProperty("browser", "chrome").trim().toLowerCase(Locale.ROOT);
    }

    private static void serve(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        switch (path) {
            case "/network-page" -> response(exchange, 200,
                    "text/html; charset=utf-8", "<!doctype html><title>BiDi network</title><p>ready</p>");
            case "/api/success" -> response(exchange, 201, "application/json", "{\"ok\":true}");
            case "/api/failure" -> response(exchange, 503, "application/json", "{\"ok\":false}");
            case "/redirect" -> {
                exchange.getResponseHeaders().set("Location", "/api/final");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
            }
            case "/api/final", "/api/restart", "/api/after-stop", "/api/bundle" ->
                    response(exchange, 200, "application/json", "{\"ok\":true}");
            case "/ignored" -> response(exchange, 204, "text/plain", "");
            case "/api/fetch-error" -> {
                byte[] partial = "partial".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, partial.length + 100L);
                exchange.getResponseBody().write(partial);
                exchange.getResponseBody().close();
                exchange.close();
            }
            default -> response(exchange, 404, "text/plain", "not found");
        }
    }

    private static void response(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
        if (status != 204) exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
        exchange.close();
    }
}
