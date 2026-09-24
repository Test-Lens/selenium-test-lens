package io.github.testlens.browser;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.hud.HudOptions;
import io.github.testlens.hud.HudPreset;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.decorators.Decorated;
import org.openqa.selenium.support.decorators.WebDriverDecorator;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Opt-in, non-CI runtime audit. Enable with {@code -Dperf.audit=true}. */
class RuntimePerformanceAuditIT {
    private static final int WARMUPS = Integer.getInteger("perf.warmups", 2);
    private static final int REPETITIONS = Integer.getInteger("perf.repetitions", 5);
    private static final int OPERATIONS = Integer.getInteger("perf.operations", 100);
    private static final int HUD_ROW_LIMIT = 250;

    @Test
    void recordsHudRuntimeMatrix() throws IOException {
        Assumptions.assumeTrue(Boolean.getBoolean("perf.audit"), "opt-in performance audit");
        Path output = Path.of(System.getProperty("perf.outputDir", "target/performance-audit"))
                .toAbsolutePath().normalize();
        Files.createDirectories(output);
        List<Result> results = new ArrayList<>();
        WebDriver raw = BrowserTestHarness.createDriver();
        CountingDecorator decorator = new CountingDecorator();
        WebDriver driver = decorator.decorate(raw);
        try {
            Profile[] profiles = Profile.values();
            for (int repetition = -WARMUPS; repetition < REPETITIONS; repetition++) {
                int offset = Math.floorMod(repetition + WARMUPS, profiles.length);
                for (int profileIndex = 0; profileIndex < profiles.length; profileIndex++) {
                    Profile profile = profiles[(offset + profileIndex) % profiles.length];
                    driver.get("data:text/html,<title>Test Lens performance audit</title><main id='fixture'>ready</main>");
                    decorator.reset();
                    long started = System.nanoTime();
                    if (profile == Profile.RAW_SELENIUM) {
                        JavascriptExecutor js = (JavascriptExecutor) driver;
                        for (int operation = 0; operation < OPERATIONS; operation++) {
                            js.executeScript("document.getElementById('fixture').dataset.last=arguments[0]", operation);
                        }
                    } else {
                        JsOverlayDebug lens = new JsOverlayDebug(driver, config(profile));
                        lens.initHud("runtime audit", profile.name());
                        for (int operation = 0; operation < OPERATIONS; operation++) {
                            lens.hudLog("info", "event " + operation + " 🧪", null);
                        }
                    }
                    long elapsed = System.nanoTime() - started;
                    long rows = profile == Profile.RAW_SELENIUM ? 0L : ((Number) ((JavascriptExecutor) driver)
                            .executeScript("return window.__seleniumOverlayRoot ? "
                                    + "window.__seleniumOverlayRoot.querySelectorAll('#selenium-hud-logs > div').length : 0"))
                            .longValue();
                    if (profile == Profile.LENS_STANDARD || profile == Profile.LENS_DEBUG) {
                        assertEquals(Math.min(OPERATIONS, HUD_ROW_LIMIT), rows,
                                "semantic HUD must retain the newest rows up to its documented limit");
                    }
                    if (repetition >= 0) {
                        long evictedRows = profile == Profile.LENS_STANDARD || profile == Profile.LENS_DEBUG
                                ? Math.max(0L, OPERATIONS - rows) : 0L;
                        results.add(new Result(profile.name(), repetition, OPERATIONS, elapsed,
                                rows, evictedRows, decorator.snapshot()));
                    }
                }
            }
            driver.get("data:text/html,<main>first document</main>");
            JsOverlayDebug navigationLens = new JsOverlayDebug(driver, config(Profile.LENS_DEBUG));
            navigationLens.initHud("runtime recovery", "navigation");
            driver.get("data:text/html,<main>replacement document</main>");
            navigationLens.hudLog("info", "after navigation", null);
            assertEquals(1L, ((Number) ((JavascriptExecutor) driver).executeScript(
                    "return window.__seleniumOverlayRoot.querySelectorAll('#selenium-hud-logs > div').length"))
                    .longValue(), "the cold path must reinstall the HUD in the replacement document");
        } finally {
            driver.quit();
        }
        writeCsv(output.resolve("runtime-audit.csv"), results);
        writeJson(output.resolve("runtime-audit.json"), results);
    }

    private static OverlayConfig config(Profile profile) {
        boolean visible = profile != Profile.LENS_LOW_DIAGNOSTICS;
        HudPreset preset = profile == Profile.LENS_DEBUG ? HudPreset.DEBUG : HudPreset.STANDARD;
        return OverlayConfig.builder()
                .enabled(true)
                .showHudPanel(visible)
                .hudOptions(HudOptions.builder().preset(preset).build())
                .build();
    }

    private static void writeCsv(Path path, List<Result> results) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("sourceSha,browser,headed,profile,repetition,operations,elapsedNanos,nanosPerOperation,rows,evictedRows,executeScript,alert,totalClientCalls");
        String sha = System.getProperty("perf.sourceSha", "unknown");
        for (Result result : results) {
            lines.add(String.join(",", sha, BrowserTestHarness.browserName(),
                    System.getProperty("headed", "false"), result.profile(), String.valueOf(result.repetition()),
                    String.valueOf(result.operations()), String.valueOf(result.elapsedNanos()),
                    String.valueOf(result.elapsedNanos() / result.operations()), String.valueOf(result.rows()),
                    String.valueOf(result.evictedRows()),
                    String.valueOf(result.calls().getOrDefault("executeScript", 0L)),
                    String.valueOf(result.calls().getOrDefault("alert", 0L)),
                    String.valueOf(result.calls().values().stream().mapToLong(Long::longValue).sum())));
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static void writeJson(Path path, List<Result> results) throws IOException {
        StringBuilder json = new StringBuilder("{\n  \"generatedAt\": \"").append(Instant.now())
                .append("\",\n  \"sourceSha\": \"").append(escape(System.getProperty("perf.sourceSha", "unknown")))
                .append("\",\n  \"browser\": \"").append(escape(BrowserTestHarness.browserName()))
                .append("\",\n  \"headed\": ").append(Boolean.getBoolean("headed"))
                .append(",\n  \"warmups\": ").append(WARMUPS).append(",\n  \"results\": [\n");
        for (int index = 0; index < results.size(); index++) {
            Result result = results.get(index);
            json.append("    {\"profile\":\"").append(result.profile()).append("\",\"repetition\":")
                    .append(result.repetition()).append(",\"operations\":").append(result.operations())
                    .append(",\"elapsedNanos\":").append(result.elapsedNanos())
                    .append(",\"rows\":").append(result.rows())
                    .append(",\"evictedRows\":").append(result.evictedRows()).append(",\"clientCalls\":{");
            int callIndex = 0;
            for (Map.Entry<String, Long> call : result.calls().entrySet()) {
                if (callIndex++ > 0) json.append(',');
                json.append('\"').append(escape(call.getKey())).append("\":").append(call.getValue());
            }
            json.append("}}").append(index + 1 == results.size() ? "\n" : ",\n");
        }
        json.append("  ]\n}\n");
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private enum Profile { RAW_SELENIUM, LENS_LOW_DIAGNOSTICS, LENS_STANDARD, LENS_DEBUG }

    private record Result(String profile, int repetition, int operations, long elapsedNanos,
                          long rows, long evictedRows, Map<String, Long> calls) { }

    private static final class CountingDecorator extends WebDriverDecorator<WebDriver> {
        private final ConcurrentHashMap<String, LongAdder> calls = new ConcurrentHashMap<>();

        @Override
        public void beforeCall(Decorated<?> target, Method method, Object[] args) {
            calls.computeIfAbsent(method.getName(), ignored -> new LongAdder()).increment();
        }

        void reset() { calls.clear(); }

        Map<String, Long> snapshot() {
            Map<String, Long> snapshot = new TreeMap<>();
            calls.forEach((name, count) -> snapshot.put(name, count.sum()));
            return new LinkedHashMap<>(snapshot);
        }
    }
}
