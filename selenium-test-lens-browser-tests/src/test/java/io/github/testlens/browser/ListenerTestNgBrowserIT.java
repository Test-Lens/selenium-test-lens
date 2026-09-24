package io.github.testlens.browser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.testlens.OverlayConfig;
import io.github.testlens.TestLensOptions;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.testng.TestLensTestNg;
import io.github.testlens.testng.TestLensTestNgContext;
import io.github.testlens.testng.TestLensTestNgFactory;
import io.github.testlens.testng.TestLensTestNgListener;
import io.github.testlens.testng.DriverScope;
import io.github.testlens.selenium.network.NetworkCaptureMode;
import io.github.testlens.selenium.network.NetworkDiagnostics;
import io.github.testlens.selenium.network.NetworkDiagnosticsOptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.support.ui.WebDriverWait;
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
import java.time.Duration;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListenerTestNgBrowserIT {
    private static final Duration WAIT = Duration.ofSeconds(5);
    private static final Path OUTPUT_ROOT = Path.of("target", "ui-test-lens", browserName(), "testng-listener");
    private static final AtomicReference<String> BASE_URL = new AtomicReference<>();
    private static final AtomicReference<TrackedBrowser> BROWSER = new AtomicReference<>();
    private static final AtomicReference<UiTestLensSession> SESSION = new AtomicReference<>();
    private static final CopyOnWriteArrayList<UiTestLensSession> SHARED_SESSIONS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<String> DOCUMENT_TOKENS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<NetworkDiagnostics> SHARED_NETWORK = new CopyOnWriteArrayList<>();

    @Test
    void testNgListenerOwnsARealBrowserInvocation() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ListenerTestNgBrowserIT::serve);
        server.start();
        BASE_URL.set("http://127.0.0.1:" + server.getAddress().getPort());
        try {
            ResultCollector collector = new ResultCollector();
            TestNG testng = new TestNG(false);
            testng.setUseDefaultListeners(false);
            testng.setTestClasses(new Class<?>[]{BrowserFixture.class});
            testng.addListener(collector);
            testng.run();

            assertEquals(1, collector.results.size());
            assertEquals(ITestResult.SUCCESS, collector.results.get(0).getStatus());
            UiTestLensSession session = SESSION.getAndSet(null);
            TrackedBrowser browser = BROWSER.getAndSet(null);
            assertEquals(TraceStatus.PASSED, session.metadata().status());
            Path reportDirectory = OUTPUT_ROOT.resolve(sanitize(session.metadata().name())).resolve(session.id());
            assertTrue(Files.isRegularFile(reportDirectory.resolve("trace.json")));
            assertTrue(Files.isRegularFile(reportDirectory.resolve("report.html")));
            assertEquals(1, browser.quitCalls.get());
            assertThrows(NoSuchSessionException.class, () -> browser.delegate.getWindowHandle());
            assertThrows(IllegalStateException.class, TestLensTestNgContext::current);
        } finally {
            server.stop(0);
            BASE_URL.set(null);
            TrackedBrowser remaining = BROWSER.getAndSet(null);
            if (remaining != null && remaining.quitCalls.get() == 0) {
                remaining.delegate.quit();
            }
        }
    }

    @Test
    void perClassScopeKeepsOneRealBrowserButProducesIndependentSessions() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ListenerTestNgBrowserIT::serve);
        server.start();
        BASE_URL.set("http://127.0.0.1:" + server.getAddress().getPort());
        SHARED_SESSIONS.clear();
        DOCUMENT_TOKENS.clear();
        SHARED_NETWORK.clear();
        try {
            ResultCollector collector = new ResultCollector();
            TestNG testng = new TestNG(false);
            testng.setUseDefaultListeners(false);
            testng.setTestClasses(new Class<?>[]{SharedBrowserFixture.class});
            testng.addListener(collector);
            testng.run();

            assertEquals(3, collector.results.size());
            assertTrue(collector.results.stream().allMatch(result -> result.getStatus() == ITestResult.SUCCESS));
            assertEquals(3, SHARED_SESSIONS.size());
            assertEquals(3, SHARED_SESSIONS.stream().map(UiTestLensSession::id).distinct().count());
            assertEquals(1, DOCUMENT_TOKENS.stream().distinct().count());
            assertEquals(3, SHARED_NETWORK.size());
            assertTrue(SHARED_NETWORK.stream().noneMatch(NetworkDiagnostics::isStarted));
            for (UiTestLensSession session : SHARED_SESSIONS) {
                Path reportDirectory = OUTPUT_ROOT.resolve(sanitize(session.metadata().name())).resolve(session.id());
                assertTrue(Files.isRegularFile(reportDirectory.resolve("trace.json")), session.id());
                assertTrue(Files.isRegularFile(reportDirectory.resolve("report.html")), session.id());
                assertTrue(Files.readString(reportDirectory.resolve("trace.json"))
                        .contains("SCREENSHOT_CAPTURE_PASSED"), session.id());
            }
            TrackedBrowser browser = BROWSER.getAndSet(null);
            assertEquals(1, browser.quitCalls.get());
            assertThrows(NoSuchSessionException.class, () -> browser.delegate.getWindowHandle());
        } finally {
            server.stop(0);
            BASE_URL.set(null);
            SHARED_SESSIONS.clear();
            DOCUMENT_TOKENS.clear();
            SHARED_NETWORK.clear();
            TrackedBrowser remaining = BROWSER.getAndSet(null);
            if (remaining != null && remaining.quitCalls.get() == 0) remaining.delegate.quit();
        }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = BrowserFactory.class)
    public static class BrowserFixture {
        @org.testng.annotations.Test
        public void usesRealDriverLensAndSession() {
            TestLensTestNgContext context = TestLensTestNgContext.current();
            assertSame(context.driver(), context.lens().driver());
            SESSION.set(context.session());
            context.driver().get(BASE_URL.get() + "/");
            context.lens().getByTestId("save").click();
            new WebDriverWait(context.driver(), WAIT).until(driver ->
                    "1".equals(((JavascriptExecutor) driver)
                            .executeScript("return String(window.saveClicks || 0)")));
        }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = SharedBrowserFactory.class, driverScope = DriverScope.PER_CLASS)
    public static class SharedBrowserFixture {
        @org.testng.annotations.BeforeMethod(alwaysRun = true)
        public void before() {
            TestLensTestNgContext context = TestLensTestNgContext.current();
            if (SHARED_SESSIONS.isEmpty()) context.driver().get(BASE_URL.get() + "/");
            SHARED_SESSIONS.add(context.session());
            DOCUMENT_TOKENS.add(String.valueOf(((JavascriptExecutor) context.driver())
                    .executeScript("return window.documentToken")));
            SHARED_NETWORK.add(context.lens().network().start(NetworkDiagnosticsOptions.builder()
                    .captureMode(NetworkCaptureMode.BIDI).build()));
        }

        @org.testng.annotations.Test(priority = 1)
        public void first() {
            TestLensTestNgContext.current().lens().getByTestId("save").click();
            fetch("/api/invocation-a");
            awaitRequest("/api/invocation-a");
        }

        @org.testng.annotations.Test(priority = 2)
        public void second() {
            assertEquals("1", String.valueOf(((JavascriptExecutor) TestLensTestNgContext.current().driver())
                    .executeScript("return String(window.saveClicks)")));
            assertTrue(currentNetwork().events().stream()
                    .noneMatch(event -> event.url().contains("/api/invocation-a")));
            fetch("/api/invocation-b");
            awaitRequest("/api/invocation-b");
            TestLensTestNgContext.current().lens().getByTestId("save").click();
        }

        @org.testng.annotations.Test(priority = 3)
        public void third() {
            assertEquals("2", String.valueOf(((JavascriptExecutor) TestLensTestNgContext.current().driver())
                    .executeScript("return String(window.saveClicks)")));
            assertTrue(currentNetwork().events().stream()
                    .noneMatch(event -> event.url().contains("/api/invocation-b")));
        }

        @org.testng.annotations.AfterMethod(alwaysRun = true)
        public void after() {
            TestLensTestNgContext.current().lens().captureScreenshot("after-method");
        }

        private static NetworkDiagnostics currentNetwork() {
            return SHARED_NETWORK.get(SHARED_NETWORK.size() - 1);
        }

        private static void fetch(String path) {
            ((JavascriptExecutor) TestLensTestNgContext.current().driver()).executeAsyncScript("""
                    const done = arguments[arguments.length - 1];
                    fetch(arguments[0]).then(response => done(response.status)).catch(error => done(String(error)));
                    """, BASE_URL.get() + path);
        }

        private static void awaitRequest(String path) {
            new WebDriverWait(TestLensTestNgContext.current().driver(), WAIT).until(ignored ->
                    currentNetwork().events().stream().anyMatch(event -> event.url().contains(path)));
        }
    }

    public static class BrowserFactory implements TestLensTestNgFactory {
        public BrowserFactory() {
        }

        @Override
        public WebDriver createDriver() {
            TrackedBrowser browser = new TrackedBrowser(browser());
            if (!BROWSER.compareAndSet(null, browser)) {
                browser.proxy.quit();
                throw new IllegalStateException("A browser is already assigned to this invocation");
            }
            return browser.proxy;
        }

        @Override
        public TestLensOptions lensOptions() {
            return TestLensOptions.builder()
                    .overlayConfig(OverlayConfig.builder().enabled(true).build())
                    .screenshotOnFailure(false)
                    .outputRoot(OUTPUT_ROOT)
                    .build();
        }

        @Override
        public String sessionName(ITestResult result) {
            return "testng real browser contract";
        }

        protected WebDriver browser() {
            return createBrowser();
        }
    }

    public static class SharedBrowserFactory extends BrowserFactory {
        @Override protected WebDriver browser() { return BrowserTestHarness.createBiDiDriver(); }
        @Override public String sessionName(ITestResult result) {
            return "testng shared browser " + result.getMethod().getMethodName() + " "
                    + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    private static WebDriver createBrowser() {
        return BrowserTestHarness.createDriver(PageLoadStrategy.NORMAL);
    }

    private static String browserName() {
        return BrowserTestHarness.browserName();
    }

    private static String sanitize(String value) {
        String safe = value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-").replaceAll("-+", "-")
                .replaceAll("(^[-.]+|[-.]+$)", "");
        return safe.isBlank() ? "session" : safe;
    }

    private static void serve(HttpExchange exchange) throws IOException {
        byte[] body = """
                <!doctype html><html><head><meta charset='utf-8'><title>TestNG Listener</title></head>
                <body><button data-testid='save'>Save</button><script>
                window.saveClicks = 0;
                window.documentToken = crypto.randomUUID();
                document.querySelector('[data-testid="save"]').addEventListener('click', () => window.saveClicks++);
                </script></body></html>
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static final class ResultCollector implements org.testng.ITestListener {
        private final CopyOnWriteArrayList<ITestResult> results = new CopyOnWriteArrayList<>();
        @Override public void onTestSuccess(ITestResult result) { results.add(result); }
        @Override public void onTestFailure(ITestResult result) { results.add(result); }
        @Override public void onTestSkipped(ITestResult result) { results.add(result); }
    }

    private static final class TrackedBrowser {
        private final WebDriver delegate;
        private final AtomicInteger quitCalls = new AtomicInteger();
        private final WebDriver proxy;

        private TrackedBrowser(WebDriver delegate) {
            this.delegate = delegate;
            Class<?>[] interfaces = delegate instanceof HasBiDi
                    ? new Class<?>[]{WebDriver.class, JavascriptExecutor.class, TakesScreenshot.class, HasBiDi.class}
                    : new Class<?>[]{WebDriver.class, JavascriptExecutor.class, TakesScreenshot.class};
            this.proxy = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    interfaces,
                    (ignored, method, arguments) -> {
                        if (method.getName().equals("quit")) quitCalls.incrementAndGet();
                        try {
                            return method.invoke(delegate, arguments);
                        } catch (InvocationTargetException failure) {
                            throw failure.getCause();
                        }
                    });
        }
    }
}
