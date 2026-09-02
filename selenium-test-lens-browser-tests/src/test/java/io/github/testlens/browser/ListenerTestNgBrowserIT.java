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
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
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

    public static class BrowserFactory implements TestLensTestNgFactory {
        public BrowserFactory() {
        }

        @Override
        public WebDriver createDriver() {
            TrackedBrowser browser = new TrackedBrowser(createBrowser());
            assertTrue(BROWSER.compareAndSet(null, browser));
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
    }

    private static WebDriver createBrowser() {
        boolean headed = Boolean.parseBoolean(System.getProperty("headed", "false"));
        return switch (browserName()) {
            case "chrome" -> {
                ChromeOptions options = new ChromeOptions();
                options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
                String configuredBinary = System.getProperty("test.chrome.binary", "").trim();
                if (!configuredBinary.isEmpty()) options.setBinary(configuredBinary);
                options.addArguments("--window-size=1280,900", "--disable-dev-shm-usage", "--no-sandbox");
                if (!headed) options.addArguments("--headless=new");
                yield new ChromeDriver(options);
            }
            case "firefox" -> {
                FirefoxOptions options = new FirefoxOptions();
                options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
                if (!headed) options.addArguments("-headless");
                WebDriver firefox = new FirefoxDriver(options);
                firefox.manage().window().setSize(new Dimension(1280, 900));
                yield firefox;
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported -Dbrowser=" + browserName() + "; expected chrome or firefox");
        };
    }

    private static String browserName() {
        return System.getProperty("browser", "chrome").trim().toLowerCase(Locale.ROOT);
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
            this.proxy = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
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
