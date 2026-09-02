package io.github.testlens.browser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.testlens.OverlayConfig;
import io.github.testlens.TestLens;
import io.github.testlens.TestLensOptions;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.junit5.TestLensExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JUnit5ExtensionBrowserIT {
    private static final Duration WAIT = Duration.ofSeconds(5);
    private static final Path OUTPUT_ROOT = Path.of("target", "ui-test-lens", browserName(), "junit5-extension");
    private static final AtomicReference<TrackedBrowser> CURRENT_BROWSER = new AtomicReference<>();
    private static final AtomicReference<UiTestLensSession> CURRENT_SESSION = new AtomicReference<>();
    private static HttpServer server;
    private static String baseUrl;

    @Order(1)
    @RegisterExtension
    static final AfterEachCallback VERIFY_FINALIZATION = context -> {
        UiTestLensSession session = CURRENT_SESSION.getAndSet(null);
        TrackedBrowser browser = CURRENT_BROWSER.getAndSet(null);
        assertEquals(TraceStatus.PASSED, session.metadata().status());
        Path reportDirectory = OUTPUT_ROOT.resolve(sanitize(session.metadata().name())).resolve(session.id());
        assertTrue(Files.isRegularFile(reportDirectory.resolve("trace.json")));
        assertTrue(Files.isRegularFile(reportDirectory.resolve("report.html")));
        assertEquals(1, browser.quitCalls.get(), "extension must own and close the browser exactly once");
    };

    @Order(2)
    @RegisterExtension
    static final TestLensExtension TEST_LENS = TestLensExtension.builder(JUnit5ExtensionBrowserIT::createTrackedBrowser)
            .lensOptions(TestLensOptions.builder()
                    .overlayConfig(OverlayConfig.builder().enabled(true).build())
                    .screenshotOnFailure(false)
                    .outputRoot(OUTPUT_ROOT)
                    .build())
            .sessionName(context -> "junit5 extension " + context.getDisplayName())
            .build();

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", JUnit5ExtensionBrowserIT::serve);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void extensionInjectsConsumerDriverAndLens(WebDriver driver, TestLens lens) {
        assertSame(driver, lens.driver());
        CURRENT_SESSION.set(lens.session().orElseThrow());

        driver.get(baseUrl + "/");
        lens.getByTestId("save").click();

        new WebDriverWait(driver, WAIT).until(webDriver ->
                "1".equals(((JavascriptExecutor) webDriver).executeScript("return String(window.saveClicks || 0)")));
    }

    private static WebDriver createTrackedBrowser() {
        WebDriver delegate = createBrowser();
        TrackedBrowser tracked = new TrackedBrowser(delegate);
        if (!CURRENT_BROWSER.compareAndSet(null, tracked)) {
            delegate.quit();
            throw new IllegalStateException("A browser is already assigned to this invocation");
        }
        return tracked.proxy;
    }

    private static WebDriver createBrowser() {
        boolean headed = Boolean.parseBoolean(System.getProperty("headed", "false"));
        return switch (browserName()) {
            case "chrome" -> {
                ChromeOptions options = new ChromeOptions();
                options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
                String configuredBinary = System.getProperty("test.chrome.binary", "").trim();
                if (!configuredBinary.isEmpty()) {
                    options.setBinary(configuredBinary);
                }
                options.addArguments("--window-size=1280,900", "--disable-dev-shm-usage", "--no-sandbox");
                if (!headed) options.addArguments("--headless=new");
                yield new ChromeDriver(options);
            }
            case "firefox" -> {
                FirefoxOptions options = new FirefoxOptions();
                options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
                if (!headed) options.addArguments("-headless");
                WebDriver firefox = new FirefoxDriver(options);
                firefox.manage().window().setSize(new org.openqa.selenium.Dimension(1280, 900));
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
                <!doctype html><html><head><meta charset='utf-8'><title>JUnit 5 Extension</title></head>
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

    private static final class TrackedBrowser {
        private final AtomicInteger quitCalls = new AtomicInteger();
        private final WebDriver proxy;

        private TrackedBrowser(WebDriver delegate) {
            this.proxy = (WebDriver) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
                    (ignored, method, arguments) -> {
                        if (method.getName().equals("quit")) {
                            quitCalls.incrementAndGet();
                        }
                        try {
                            return method.invoke(delegate, arguments);
                        } catch (InvocationTargetException invocationFailure) {
                            throw invocationFailure.getCause();
                        }
                    });
        }
    }
}
