package release.diagnostic;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.BuildInfo;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.BiDi;
import org.openqa.selenium.bidi.Event;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** A consumer of raw public Selenium BiDi events, independent of any capture adapter. */
class ProtocolRedirectIntegrityTest {
    private static final Duration EVENT_DEADLINE = Duration.ofSeconds(3);
    private static final int STIMULI = 50;
    private final Object arrived = new Object();
    private final List<Map<String, Object>> phases = new ArrayList<>();
    private final List<Map<String, Object>> journal = new ArrayList<>();
    private final Map<String, String> pseudonyms = new LinkedHashMap<>();
    private boolean evidenceOverflow;

    @Test
    void eachControlledRedirectHasBothRawCompletedResponses() throws Throwable {
        HttpServer server = HttpServer.create(new InetSocketAddress(
                System.getProperty("test.network.bind", "127.0.0.1"), 0), 0);
        server.createContext("/", this::serve);
        server.start();
        String base = "http://" + System.getProperty("test.network.host", "127.0.0.1")
                + ":" + server.getAddress().getPort();
        WebDriver driver = null;
        BiDi bidi = null;
        List<Object> listenerIds = new ArrayList<>();
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("seleniumVersion", new BuildInfo().getReleaseLabel());
        evidence.put("stimuli", STIMULI);
        evidence.put("eventDeadlineMs", EVENT_DEADLINE.toMillis());
        evidence.put("outcome", "INCOMPLETE");
        Throwable pending = null;
        try {
            ChromeOptions options = new ChromeOptions().enableBiDi();
            String binary = System.getProperty("test.chrome.binary", "").trim();
            if (!binary.isEmpty()) options.setBinary(binary);
            options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
            if (!Boolean.parseBoolean(System.getProperty("headed", "false"))) options.addArguments("--headless=new");
            String grid = System.getProperty("bidi.grid", System.getProperty("test.grid.url", "")).trim();
            driver = grid.isEmpty() ? new ChromeDriver(options)
                    : new RemoteWebDriver(URI.create(grid).toURL(), options);
            if (!grid.isEmpty()) driver = new Augmenter().augment(driver);
            evidence.put("mode", grid.isEmpty() ? "local" : "grid");
            evidence.put("browserVersion", ((org.openqa.selenium.HasCapabilities) driver)
                    .getCapabilities().getBrowserVersion());
            bidi = ((HasBiDi) driver).getBiDi();
            for (String type : List.of("network.beforeRequestSent", "network.responseStarted",
                    "network.responseCompleted", "network.fetchError")) {
                Event<Map<String, Object>> event = new Event<>(type, payload -> payload);
                listenerIds.add(bidi.addListener(event, payload -> record(type, payload)));
            }
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(10));
            driver.get(base + "/probe");
            for (int stimulus = 1; stimulus <= STIMULI; stimulus++) {
                String redirect = "/redirect/" + stimulus;
                String target = "/final/" + stimulus;
                Object fetchStatus = ((JavascriptExecutor) driver).executeAsyncScript("""
                        const url = arguments[0], done = arguments[arguments.length - 1];
                        fetch(url, {cache: 'no-store'}).then(async response => {
                          await response.text(); done(response.status);
                        }).catch(() => done(-1));
                        """, base + redirect);
                assertEquals(200L, ((Number) fetchStatus).longValue(), "Native fetch failed at stimulus " + stimulus);
                boolean complete = awaitCompletions(redirect, target);
                synchronized (arrived) {
                    assertEquals(1, serverCount(redirect, 302), "Controlled server did not journal one redirect");
                    assertEquals(1, serverCount(target, 200), "Controlled server did not journal one final response");
                    if (!complete) {
                        evidence.put("outcome", "PROTOCOL_MISMATCH");
                        evidence.put("failedStimulus", stimulus);
                        fail("Raw BiDi responses disagree with controlled server at stimulus " + stimulus
                                + "; expected responseCompleted 302 " + redirect + " and 200 " + target
                                + ". Safe journal: " + safeSnapshot());
                    }
                }
            }
            evidence.put("outcome", "PASS");
        } catch (Throwable failure) {
            pending = failure;
            if ("INCOMPLETE".equals(evidence.get("outcome"))) evidence.put("outcome", "FAILURE");
        } finally {
            BiDi ownedBiDi = bidi;
            WebDriver ownedDriver = driver;
            if (ownedBiDi != null) {
                for (Object id : listenerIds) pending = attempt(pending, () -> removeListener(ownedBiDi, id));
                pending = attempt(pending, ownedBiDi::close);
            }
            if (ownedDriver != null) pending = attempt(pending, ownedDriver::quit);
            pending = attempt(pending, () -> server.stop(0));
            synchronized (arrived) {
                if (evidenceOverflow) {
                    evidence.put("outcome", "EVIDENCE_OVERFLOW");
                    pending = attempt(pending, () -> fail("Protocol diagnostic evidence exceeded its bounds"));
                }
            }
            if (pending != null && "PASS".equals(evidence.get("outcome"))) evidence.put("outcome", "CLEANUP_FAILED");
            pending = attempt(pending, () -> writeEvidence(evidence));
        }
        if (pending != null) throw pending;
    }

    private void writeEvidence(Map<String, Object> evidence) throws IOException {
        synchronized (arrived) {
            evidence.put("phases", List.copyOf(phases));
            evidence.put("serverJournal", List.copyOf(journal));
            evidence.put("evidenceOverflow", evidenceOverflow);
        }
        Path output = Path.of(System.getProperty("probe.output", "target"))
                .resolve("protocol-redirect-evidence.json").toAbsolutePath();
        Files.createDirectories(output.getParent());
        String json = new Json().toJson(evidence);
        Files.writeString(output, json, StandardCharsets.UTF_8);
        System.out.println("Raw BiDi protocol evidence: " + output);
        System.out.println(json);
    }

    private static Throwable attempt(Throwable pending, CleanupAction action) {
        try { action.run(); }
        catch (Throwable failure) {
            if (pending == null) return failure;
            if (pending != failure) pending.addSuppressed(failure);
        }
        return pending;
    }

    @FunctionalInterface
    private interface CleanupAction { void run() throws Throwable; }

    private boolean awaitCompletions(String redirect, String target) throws InterruptedException {
        long deadline = System.nanoTime() + EVENT_DEADLINE.toNanos();
        synchronized (arrived) {
            while (true) {
                if (evidenceOverflow) return false;
                if (completionCount(redirect, 302) == 1 && completionCount(target, 200) == 1) return true;
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) return false;
                TimeUnit.NANOSECONDS.timedWait(arrived, remaining);
            }
        }
    }

    private long completionCount(String path, int status) {
        return phases.stream().filter(row -> "network.responseCompleted".equals(row.get("type"))
                && path.equals(row.get("requestPath")) && Integer.valueOf(status).equals(row.get("status"))).count();
    }

    private long serverCount(String path, int status) {
        return journal.stream().filter(row -> path.equals(row.get("path"))
                && Integer.valueOf(status).equals(row.get("status"))).count();
    }

    private void record(String type, Map<String, Object> payload) {
        Map<?, ?> request = map(payload.get("request"));
        Map<?, ?> response = map(payload.get("response"));
        String requestPath = safePath(request.get("url"));
        String responsePath = safePath(response.get("url"));
        if (!requestPath.matches("/(redirect|final)/[0-9]+")) return;
        synchronized (arrived) {
            if (phases.size() >= STIMULI * 12) {
                evidenceOverflow = true;
                arrived.notifyAll();
                return;
            }
            String id = String.valueOf(request.get("request"));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type", type);
            row.put("timestamp", number(payload.get("timestamp")));
            row.put("requestId", pseudonyms.computeIfAbsent(id, ignored -> "request-" + (pseudonyms.size() + 1)));
            row.put("requestPath", requestPath);
            row.put("responsePath", responsePath);
            row.put("hop", number(payload.get("redirectCount")));
            row.put("status", number(response.get("status")).intValue());
            phases.add(row);
            arrived.notifyAll();
        }
    }

    private void serve(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        int status = path.matches("/redirect/[0-9]+") ? 302
                : path.matches("/final/[0-9]+") || path.equals("/probe") ? 200 : 404;
        if (status == 302) exchange.getResponseHeaders().set("Location", path.replace("/redirect/", "/final/"));
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        byte[] body = (path.equals("/probe") ? "<!doctype html><title>Protocol probe</title>" : "controlled response")
                .getBytes(StandardCharsets.UTF_8);
        try {
            exchange.sendResponseHeaders(status, status == 302 ? -1 : body.length);
            synchronized (arrived) {
                if (path.matches("/(redirect|final)/[0-9]+")) {
                    if (journal.size() < STIMULI * 4) {
                        journal.add(Map.of("path", path, "status", status, "timestamp", Instant.now().toEpochMilli()));
                    } else {
                        evidenceOverflow = true;
                        arrived.notifyAll();
                    }
                }
            }
            if (status != 302) exchange.getResponseBody().write(body);
        } finally { exchange.close(); }
    }

    private String safeSnapshot() {
        return new Json().toJson(Map.of("phases", List.copyOf(phases), "serverJournal", List.copyOf(journal)));
    }

    private static Map<?, ?> map(Object value) { return value instanceof Map<?, ?> result ? result : Map.of(); }
    private static Number number(Object value) { return value instanceof Number result ? result : -1L; }
    private static String safePath(Object value) {
        if (value == null) return "";
        try { return java.util.Objects.toString(URI.create(String.valueOf(value)).getPath(), ""); }
        catch (IllegalArgumentException invalid) { return ""; }
    }

    /** Listener IDs changed from long to String; invoke only the matching public removal API. */
    private static void removeListener(BiDi bidi, Object id) throws ReflectiveOperationException {
        for (Method method : BiDi.class.getMethods()) {
            if (!method.getName().equals("removeListener") || method.getParameterCount() != 1) continue;
            Class<?> parameter = method.getParameterTypes()[0];
            if (parameter.isInstance(id) || parameter == long.class && id instanceof Long) {
                method.invoke(bidi, id);
                return;
            }
        }
        throw new NoSuchMethodException("Public BiDi.removeListener does not accept the returned ID type");
    }
}
