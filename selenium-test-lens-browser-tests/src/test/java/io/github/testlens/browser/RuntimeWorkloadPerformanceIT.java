package io.github.testlens.browser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.testlens.TestLens;
import io.github.testlens.TestLensFinalizationResult;
import io.github.testlens.TestLensOptions;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.hud.HudOptions;
import io.github.testlens.hud.HudPreset;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opt-in end-to-end workload audit. Unlike {@link RuntimePerformanceAuditIT}, this executes public Lens actions. */
class RuntimeWorkloadPerformanceIT {
    private static final int WARMUPS = Integer.getInteger("perf.workloadWarmups", 1);
    private static final int REPETITIONS = Integer.getInteger("perf.workloadRepetitions", 5);

    @Test
    void recordsEquivalentStandardAndDebugWorkloads() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("perf.audit"), "opt-in performance audit");
        Path output = Path.of(System.getProperty("perf.outputDir", "target/performance-audit"))
                .toAbsolutePath().normalize();
        Files.createDirectories(output);
        HttpServer server = server();
        BrowserTestHarness.WireCommandMetrics wire = new BrowserTestHarness.WireCommandMetrics();
        long createStarted = System.nanoTime();
        WebDriver driver = BrowserTestHarness.createDriver(wire);
        long createNanos = System.nanoTime() - createStarted;
        List<OperationResult> operations = new ArrayList<>();
        List<LifecycleResult> lifecycle = new ArrayList<>();
        Map<String, Object> environment;
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
            for (int repetition = -WARMUPS; repetition < REPETITIONS; repetition++) {
                for (HudPreset preset : orderedPresets(repetition)) {
                    RunResult run = runSuccessfulWorkload(driver, wire, url, preset, repetition);
                    if (repetition >= 0) {
                        operations.addAll(run.operations());
                        lifecycle.add(run.lifecycle().withCreateNanos(createNanos));
                    }
                }
            }
            // Evidence/finalization is deliberately separate from the successful action comparison.
            for (HudPreset preset : List.of(HudPreset.STANDARD, HudPreset.DEBUG)) {
                lifecycle.add(runFailureWorkload(driver, wire, url, preset));
            }
            environment = driverMetadata(driver, operations, lifecycle);
        } finally {
            long quitStarted = System.nanoTime();
            try { driver.quit(); } finally {
                long quitNanos = System.nanoTime() - quitStarted;
                lifecycle.replaceAll(item -> item.quitNanos() == 0L ? item.withQuitNanos(quitNanos) : item);
                server.stop(0);
            }
        }
        writeOperations(output.resolve("workload-operations.csv"), operations);
        writeLifecycle(output.resolve("workload-lifecycle.csv"), lifecycle);
        writeMetadata(output.resolve("workload-metadata.json"), environment);
    }

    private static RunResult runSuccessfulWorkload(WebDriver driver,
                                                   BrowserTestHarness.WireCommandMetrics wire,
                                                   String url,
                                                   HudPreset preset,
                                                   int repetition) {
        driver.get(url);
        wire.reset();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        TestLensOptions options = options(preset, "success-" + preset.name().toLowerCase(Locale.ROOT));
        long attachStarted = System.nanoTime();
        TestLens lens = TestLens.attach(driver, options);
        long attachNanos = System.nanoTime() - attachStarted;
        long startStarted = System.nanoTime();
        UiTestLensSession session = lens.startSession("runtime workload " + preset + " " + repetition);
        long startNanos = System.nanoTime() - startStarted;
        installHudObserver(js);
        List<OperationResult> results = new ArrayList<>();

        results.add(measure("click", preset, repetition, session, wire, js,
                () -> lens.getByTestId("available").click()));
        results.add(measure("clear", preset, repetition, session, wire, js,
                () -> lens.getByTestId("name").clear()));
        results.add(measure("fill", preset, repetition, session, wire, js,
                () -> lens.getByTestId("name").fill("Test Lens")));
        results.add(measure("assertion", preset, repetition, session, wire, js,
                () -> lens.expect(By.cssSelector("[data-testid='name']")).toHaveValue("Test Lens")));
        js.executeScript("setTimeout(() => document.querySelector('[data-testid=delayed]').hidden=false, 80)");
        results.add(measure("wait", preset, repetition, session, wire, js,
                () -> lens.getByTestId("delayed").waitUntilVisible()));
        results.add(measure("smart-click-js-fallback", preset, repetition, session, wire, js,
                () -> lens.getByTestId("covered").click()));

        assertEquals(1L, number(js, "return window.availableClicks"));
        assertEquals(1L, number(js, "return window.coveredClicks"));
        long finishStarted = System.nanoTime();
        TestLensFinalizationResult finalized = lens.finishPassed();
        long finishNanos = System.nanoTime() - finishStarted;
        assertTrue(Files.isRegularFile(finalized.jsonReport()));
        assertTrue(Files.isRegularFile(finalized.htmlReport()));
        return new RunResult(results, new LifecycleResult(preset.name(), repetition, "PASSED", 0L,
                attachNanos, startNanos, finishNanos, 0L, session.events().size(), 1, 1,
                wire.total(), wire.count("executeScript")));
    }

    private static LifecycleResult runFailureWorkload(WebDriver driver,
                                                      BrowserTestHarness.WireCommandMetrics wire,
                                                      String url,
                                                      HudPreset preset) {
        driver.get(url);
        wire.reset();
        TestLens lens = TestLens.attach(driver, options(preset, "failure-" + preset.name().toLowerCase(Locale.ROOT)));
        long start = System.nanoTime();
        UiTestLensSession session = lens.startSession("runtime controlled failure " + preset);
        long startNanos = System.nanoTime() - start;
        AssertionError failure = assertThrows(AssertionError.class,
                () -> lens.expect(By.cssSelector("[data-testid='name']")).toHaveValue("never"));
        long finishStarted = System.nanoTime();
        TestLensFinalizationResult finalized = lens.finishFailed(failure);
        long finishNanos = System.nanoTime() - finishStarted;
        assertTrue(Files.isRegularFile(finalized.failureScreenshot()), "controlled failure must retain evidence");
        return new LifecycleResult(preset.name(), 0, "CONTROLLED_FAILURE", 0L, 0L, startNanos,
                finishNanos, 0L, session.events().size(), 1, 1, wire.total(), wire.count("executeScript"));
    }

    private static OperationResult measure(String operation,
                                           HudPreset preset,
                                           int repetition,
                                           UiTestLensSession session,
                                           BrowserTestHarness.WireCommandMetrics wire,
                                           JavascriptExecutor js,
                                           Runnable body) {
        resetHudObserver(js);
        int eventsBefore = session.events().size();
        int wireBefore = wire.total();
        int scriptsBefore = wire.count("executeScript");
        long started = System.nanoTime();
        String outcome = "PASSED";
        try { body.run(); }
        catch (RuntimeException | Error failure) { outcome = failure.getClass().getSimpleName(); throw failure; }
        finally {
            long elapsed = System.nanoTime() - started;
            int operationWire = wire.total() - wireBefore;
            int operationScripts = wire.count("executeScript") - scriptsBefore;
            HudMutations mutations = readHudObserver(js);
            int events = session.events().size() - eventsBefore;
            long clickCount = number(js, "return window.availableClicks + window.coveredClicks");
            RESULTS.get().add(new OperationResult(preset.name(), repetition, operation, elapsed, events,
                    mutations.inserts(), mutations.updates(), operationWire, operationScripts,
                    outcome, clickCount, session.exportJson().getBytes(StandardCharsets.UTF_8).length));
        }
        return RESULTS.get().remove(RESULTS.get().size() - 1);
    }

    private static final ThreadLocal<List<OperationResult>> RESULTS = ThreadLocal.withInitial(ArrayList::new);

    private static void installHudObserver(JavascriptExecutor js) {
        js.executeScript("""
                window.__perfHudMetrics={inserts:0,updates:0};
                const root=window.__seleniumOverlayRoot;
                const logs=root && root.querySelector('#selenium-hud-logs');
                if(window.__perfHudObserver) window.__perfHudObserver.disconnect();
                if(logs){ window.__perfHudObserver=new MutationObserver(ms=>ms.forEach(m=>{
                  if(m.type==='childList') window.__perfHudMetrics.inserts += m.addedNodes.length;
                  else window.__perfHudMetrics.updates++;
                })); window.__perfHudObserver.observe(logs,{childList:true,subtree:true,characterData:true,attributes:true}); }
                """);
    }

    private static void resetHudObserver(JavascriptExecutor js) {
        js.executeScript("if(window.__perfHudMetrics){window.__perfHudMetrics.inserts=0;window.__perfHudMetrics.updates=0}");
    }

    @SuppressWarnings("unchecked")
    private static HudMutations readHudObserver(JavascriptExecutor js) {
        Object value = js.executeScript("return window.__perfHudMetrics || {inserts:0,updates:0}");
        Map<String, Object> map = value instanceof Map<?, ?> found ? (Map<String, Object>) found : Map.of();
        return new HudMutations(asLong(map.get("inserts")), asLong(map.get("updates")));
    }

    private static long number(JavascriptExecutor js, String script) {
        return ((Number) js.executeScript(script)).longValue();
    }

    private static long asLong(Object value) { return value instanceof Number number ? number.longValue() : 0L; }

    private static TestLensOptions options(HudPreset preset, String directory) {
        return TestLensOptions.builder()
                .hud(HudOptions.builder().preset(preset).build())
                .outputRoot(Path.of(System.getProperty("perf.outputDir", "target/performance-audit"), directory))
                .screenshotOnFailure(true)
                .build();
    }

    private static List<HudPreset> orderedPresets(int repetition) {
        return Math.floorMod(repetition + WARMUPS, 2) == 0
                ? List.of(HudPreset.STANDARD, HudPreset.DEBUG)
                : List.of(HudPreset.DEBUG, HudPreset.STANDARD);
    }

    private static HttpServer server() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", RuntimeWorkloadPerformanceIT::serve);
        server.start();
        return server;
    }

    private static void serve(HttpExchange exchange) throws IOException {
        byte[] body = """
                <!doctype html><html><head><meta charset='utf-8'><title>Runtime workload</title></head><body>
                <button data-testid='available'>Available</button><input data-testid='name' value='seed'>
                <div data-testid='delayed' hidden>ready</div>
                <div style='position:relative;width:180px;height:40px'>
                  <button data-testid='covered' style='width:180px;height:40px'>Covered</button>
                  <div data-testid='blocker' style='position:absolute;inset:0;z-index:2;background:#ddd'>blocker</div>
                </div><script>
                window.availableClicks=0; window.coveredClicks=0;
                document.querySelector('[data-testid=available]').onclick=()=>window.availableClicks++;
                document.querySelector('[data-testid=covered]').onclick=()=>window.coveredClicks++;
                </script></body></html>
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, body.length);
        try (var output = exchange.getResponseBody()) { output.write(body); }
    }

    private static void writeOperations(Path path, List<OperationResult> values) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("sourceSha,browser,headed,preset,repetition,operation,elapsedNanos,lensEvents,hudInserts,hudUpdates,wireCommands,wireExecuteScript,outcome,totalBusinessClicks,traceJsonBytes");
        for (OperationResult value : values) lines.add(String.join(",",
                System.getProperty("perf.sourceSha", "unknown"), BrowserTestHarness.browserName(),
                System.getProperty("headed", "false"), value.preset(), String.valueOf(value.repetition()),
                value.operation(), String.valueOf(value.elapsedNanos()), String.valueOf(value.lensEvents()),
                String.valueOf(value.hudInserts()), String.valueOf(value.hudUpdates()),
                String.valueOf(value.wireCommands()), String.valueOf(value.wireExecuteScript()), value.outcome(),
                String.valueOf(value.totalBusinessClicks()), String.valueOf(value.traceJsonBytes())));
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static void writeLifecycle(Path path, List<LifecycleResult> values) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("sourceSha,browser,headed,preset,repetition,outcome,createDriverNanos,attachNanos,startSessionNanos,finishNanos,quitNanos,lensEvents,lensSessions,reports,wireCommands,wireExecuteScript");
        for (LifecycleResult value : values) lines.add(String.join(",",
                System.getProperty("perf.sourceSha", "unknown"), BrowserTestHarness.browserName(),
                System.getProperty("headed", "false"), value.preset(), String.valueOf(value.repetition()), value.outcome(),
                String.valueOf(value.createDriverNanos()), String.valueOf(value.attachNanos()),
                String.valueOf(value.startSessionNanos()), String.valueOf(value.finishNanos()),
                String.valueOf(value.quitNanos()), String.valueOf(value.lensEvents()),
                String.valueOf(value.lensSessions()), String.valueOf(value.reports()),
                String.valueOf(value.wireCommands()), String.valueOf(value.wireExecuteScript())));
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static Map<String, Object> driverMetadata(WebDriver driver, List<OperationResult> operations,
                                                      List<LifecycleResult> lifecycle) {
        var values = new java.util.LinkedHashMap<String, Object>();
        values.put("generatedAt", Instant.now().toString());
        values.put("sourceSha", System.getProperty("perf.sourceSha", "unknown"));
        values.put("artifactOrigin", "reactor test-classes");
        values.put("browser", BrowserTestHarness.browserName());
        values.put("headed", Boolean.getBoolean("headed"));
        values.put("workloadWarmups", WARMUPS);
        values.put("workloadRepetitions", REPETITIONS);
        values.put("operationRows", operations.size());
        values.put("lifecycleRows", lifecycle.size());
        if (driver instanceof HasCapabilities capable) {
            var capabilities = capable.getCapabilities();
            values.put("browserVersion", capabilities.getBrowserVersion());
            Object chrome = capabilities.getCapability("chrome");
            Object firefox = capabilities.getCapability("moz:geckodriverVersion");
            values.put("driverVersion", firefox == null ? driverVersion(chrome) : firefox);
            values.put("platform", String.valueOf(capabilities.getPlatformName()));
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private static Object driverVersion(Object browserCapability) {
        if (browserCapability instanceof Map<?, ?> map) {
            Object value = ((Map<String, Object>) map).get("chromedriverVersion");
            if (value != null) return String.valueOf(value).split(" ")[0];
        }
        return "unknown";
    }

    private static void writeMetadata(Path path, Map<String, Object> metadata) throws IOException {
        String json = "{\n" + metadata.entrySet().stream().map(entry -> "  \"" + entry.getKey() + "\": "
                + (entry.getValue() instanceof Number || entry.getValue() instanceof Boolean ? entry.getValue()
                : "\"" + String.valueOf(entry.getValue()).replace("\\", "\\\\").replace("\"", "\\\"") + "\""))
                .collect(java.util.stream.Collectors.joining(",\n")) + "\n}\n";
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private record HudMutations(long inserts, long updates) { }
    private record OperationResult(String preset, int repetition, String operation, long elapsedNanos,
                                   int lensEvents, long hudInserts, long hudUpdates, int wireCommands,
                                   int wireExecuteScript, String outcome, long totalBusinessClicks,
                                   int traceJsonBytes) { }
    private record RunResult(List<OperationResult> operations, LifecycleResult lifecycle) { }
    private record LifecycleResult(String preset, int repetition, String outcome, long createDriverNanos,
                                   long attachNanos, long startSessionNanos, long finishNanos, long quitNanos,
                                   int lensEvents, int lensSessions, int reports, int wireCommands,
                                   int wireExecuteScript) {
        LifecycleResult withCreateNanos(long value) { return new LifecycleResult(preset, repetition, outcome, value,
                attachNanos, startSessionNanos, finishNanos, quitNanos, lensEvents, lensSessions, reports,
                wireCommands, wireExecuteScript); }
        LifecycleResult withQuitNanos(long value) { return new LifecycleResult(preset, repetition, outcome,
                createDriverNanos, attachNanos, startSessionNanos, finishNanos, value, lensEvents, lensSessions,
                reports, wireCommands, wireExecuteScript); }
    }
}
