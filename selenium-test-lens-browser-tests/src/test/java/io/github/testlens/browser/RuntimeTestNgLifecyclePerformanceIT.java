package io.github.testlens.browser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.testlens.TestLensOptions;
import io.github.testlens.hud.HudOptions;
import io.github.testlens.hud.HudPreset;
import io.github.testlens.testng.DriverScope;
import io.github.testlens.testng.TestLensTestNg;
import io.github.testlens.testng.TestLensTestNgContext;
import io.github.testlens.testng.TestLensTestNgFactory;
import io.github.testlens.testng.TestLensTestNgListener;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.TestNG;
import org.testng.annotations.Listeners;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opt-in timing matrix for the real TestNG adapter, not a mock listener benchmark. */
class RuntimeTestNgLifecyclePerformanceIT {
    private static final ThreadLocal<Configuration> CONFIG = new ThreadLocal<>();
    private static final CopyOnWriteArrayList<String> SESSION_IDS = new CopyOnWriteArrayList<>();
    private static final AtomicInteger CREATES = new AtomicInteger();
    private static final AtomicInteger QUITS = new AtomicInteger();
    private static final AtomicLong CREATE_NANOS = new AtomicLong();
    private static final AtomicLong ACTION_NANOS = new AtomicLong();
    private static String baseUrl;

    @Test
    void recordsPerMethodAndPerClassStandardDebugMatrix() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("perf.audit"), "opt-in performance audit");
        Path output = Path.of(System.getProperty("perf.outputDir", "target/performance-audit"))
                .toAbsolutePath().normalize();
        Files.createDirectories(output);
        HttpServer server = server();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
        List<Row> rows = new ArrayList<>();
        try {
            for (DriverScope scope : DriverScope.values()) {
                for (HudPreset preset : List.of(HudPreset.STANDARD, HudPreset.DEBUG)) {
                    reset(new Configuration(preset, output.resolve("testng-" + scope + "-" + preset)));
                    TestNG runner = new TestNG(false);
                    runner.setUseDefaultListeners(false);
                    runner.setTestClasses(new Class<?>[]{scope == DriverScope.PER_METHOD
                            ? PerMethodFixture.class : PerClassFixture.class});
                    Collector collector = new Collector();
                    runner.addListener(collector);
                    long started = System.nanoTime();
                    runner.run();
                    long wall = System.nanoTime() - started;
                    assertEquals(2, collector.results.size());
                    assertTrue(collector.results.stream().allMatch(result -> result.getStatus() == ITestResult.SUCCESS));
                    assertEquals(2, SESSION_IDS.stream().distinct().count());
                    int expectedDrivers = scope == DriverScope.PER_METHOD ? 2 : 1;
                    assertEquals(expectedDrivers, CREATES.get());
                    assertEquals(expectedDrivers, QUITS.get());
                    long reports = Files.walk(CONFIG.get().outputRoot()).filter(path -> path.getFileName().toString().equals("trace.json")).count();
                    assertEquals(2L, reports);
                    rows.add(new Row(scope.name(), preset.name(), wall, CREATE_NANOS.get(), ACTION_NANOS.get(),
                            wall - CREATE_NANOS.get() - ACTION_NANOS.get(), CREATES.get(), QUITS.get(),
                            SESSION_IDS.size(), reports));
                }
            }
        } finally {
            CONFIG.remove();
            server.stop(0);
        }
        List<String> csv = new ArrayList<>();
        csv.add("sourceSha,browser,headed,scope,preset,suiteWallNanos,createDriverNanos,actionNanos,adapterSetupFinishQuitNanos,createCount,quitCount,lensSessions,reports");
        for (Row row : rows) csv.add(String.join(",", System.getProperty("perf.sourceSha", "unknown"),
                BrowserTestHarness.browserName(), System.getProperty("headed", "false"), row.scope(), row.preset(),
                String.valueOf(row.wallNanos()), String.valueOf(row.createNanos()), String.valueOf(row.actionNanos()),
                String.valueOf(row.adapterRemainderNanos()), String.valueOf(row.createCount()),
                String.valueOf(row.quitCount()), String.valueOf(row.sessions()), String.valueOf(row.reports())));
        Files.write(output.resolve("testng-lifecycle.csv"), csv, StandardCharsets.UTF_8);
    }

    private static void reset(Configuration configuration) {
        CONFIG.set(configuration);
        SESSION_IDS.clear();
        CREATES.set(0);
        QUITS.set(0);
        CREATE_NANOS.set(0);
        ACTION_NANOS.set(0);
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = AuditFactory.class, driverScope = DriverScope.PER_METHOD)
    public static class PerMethodFixture extends Fixture { }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = AuditFactory.class, driverScope = DriverScope.PER_CLASS)
    public static class PerClassFixture extends Fixture { }

    public abstract static class Fixture {
        @org.testng.annotations.Test(priority = 1)
        public void first() { workload(); }
        @org.testng.annotations.Test(priority = 2)
        public void second() { workload(); }

        private void workload() {
            long started = System.nanoTime();
            TestLensTestNgContext context = TestLensTestNgContext.current();
            SESSION_IDS.add(context.session().id());
            context.driver().get(baseUrl);
            context.lens().getByTestId("button").click();
            context.lens().getByTestId("input").fill("value");
            context.lens().expect(org.openqa.selenium.By.cssSelector("[data-testid=input]")).toHaveValue("value");
            assertEquals(1L, ((Number) ((JavascriptExecutor) context.driver())
                    .executeScript("return window.clicks")).longValue());
            ACTION_NANOS.addAndGet(System.nanoTime() - started);
        }
    }

    public static final class AuditFactory implements TestLensTestNgFactory {
        public AuditFactory() { }
        @Override public WebDriver createDriver() {
            long started = System.nanoTime();
            WebDriver delegate = BrowserTestHarness.createDriver();
            CREATE_NANOS.addAndGet(System.nanoTime() - started);
            CREATES.incrementAndGet();
            return (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (ignored, method, args) -> {
                        if (method.getName().equals("quit")) QUITS.incrementAndGet();
                        try { return method.invoke(delegate, args); }
                        catch (InvocationTargetException failure) { throw failure.getCause(); }
                    });
        }
        @Override public TestLensOptions lensOptions() {
            Configuration value = CONFIG.get();
            return TestLensOptions.builder().hud(HudOptions.builder().preset(value.preset()).build())
                    .outputRoot(value.outputRoot()).screenshotOnFailure(false).build();
        }
        @Override public String sessionName(ITestResult result) {
            return "performance " + result.getMethod().getMethodName() + " " + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    private static HttpServer server() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", RuntimeTestNgLifecyclePerformanceIT::serve);
        server.start();
        return server;
    }

    private static void serve(HttpExchange exchange) throws IOException {
        byte[] body = """
                <!doctype html><html><body><button data-testid='button'>click</button>
                <input data-testid='input'><script>window.clicks=0;
                document.querySelector('button').onclick=()=>window.clicks++;</script></body></html>
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        try (var output = exchange.getResponseBody()) { output.write(body); }
    }

    private static final class Collector implements org.testng.ITestListener {
        private final CopyOnWriteArrayList<ITestResult> results = new CopyOnWriteArrayList<>();
        @Override public void onTestSuccess(ITestResult result) { results.add(result); }
        @Override public void onTestFailure(ITestResult result) { results.add(result); }
        @Override public void onTestSkipped(ITestResult result) { results.add(result); }
    }

    private record Configuration(HudPreset preset, Path outputRoot) { }
    private record Row(String scope, String preset, long wallNanos, long createNanos, long actionNanos,
                       long adapterRemainderNanos, int createCount, int quitCount, int sessions, long reports) { }
}
