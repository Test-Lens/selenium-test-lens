package io.github.testlens.browser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.TestLens;
import io.github.testlens.TestLensFinalizationResult;
import io.github.testlens.TestLensOptions;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.selenium.assertions.UiAssertionError;
import io.github.testlens.selenium.assertions.UiAssertionFailureReason;
import io.github.testlens.selenium.assertions.UiAssertionOptions;
import io.github.testlens.selenium.assertions.UiAssertionResult;
import io.github.testlens.selenium.assertions.UiAssertionStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealBrowserContractsIT {
    private static final Duration WAIT = Duration.ofSeconds(5);
    private static HttpServer server;
    private static String baseUrl;

    private WebDriver driver;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", RealBrowserContractsIT::serve);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @AfterEach
    void closeDriver() {
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                driver = null;
            }
        }
    }

    @ParameterizedTest(name = "highlightClick is decoration only (overlay enabled={0})")
    @ValueSource(booleans = {true, false})
    void highlightClickNeverClicks(boolean enabled) {
        open("/clicks");
        JsOverlayDebug lens = overlay(enabled);

        lens.highlightClick(driver.findElement(By.id("count-button")), "COUNT");

        assertClickCounts(0);
    }

    @ParameterizedTest(name = "highlightElement is decoration only (overlay enabled={0})")
    @ValueSource(booleans = {true, false})
    void highlightElementNeverClicks(boolean enabled) {
        open("/clicks");
        JsOverlayDebug lens = overlay(enabled);

        lens.highlightElement(driver.findElement(By.id("count-button")), "COUNT");

        assertClickCounts(0);
    }

    @ParameterizedTest(name = "highlightThenClick clicks once (overlay enabled={0})")
    @ValueSource(booleans = {true, false})
    void highlightThenClickClicksExactlyOnce(boolean enabled) {
        open("/clicks");
        JsOverlayDebug lens = overlay(enabled);

        lens.highlightThenClick(driver.findElement(By.id("count-button")), "COUNT");

        awaitClickCount(1);
        assertClickCounts(1);
    }

    @ParameterizedTest(name = "UiLocator.click clicks once (overlay enabled={0})")
    @ValueSource(booleans = {true, false})
    void uiLocatorClickClicksExactlyOnce(boolean enabled) {
        open("/clicks");
        TestLens lens = TestLens.attach(driver, overlayConfig(enabled));

        lens.locator(By.id("count-button"), "Count").click();

        awaitClickCount(1);
        assertClickCounts(1);
    }

    @Test
    void highlightLivesInShadowDomAndCannotReceivePointerEvents() {
        open("/clicks");
        overlay(true).highlightClick(driver.findElement(By.id("count-button")), "COUNT");

        assertTrue(await(scriptBoolean("""
                const host = document.getElementById('selenium-overlay-host');
                const mark = host && host.shadowRoot && host.shadowRoot.querySelector('[data-uitestlens-highlight="1"]');
                return Boolean(mark && getComputedStyle(mark).pointerEvents === 'none'
                    && getComputedStyle(host).pointerEvents === 'none');
                """)));
        assertFalse(driver.findElements(By.cssSelector("[data-uitestlens-highlight='1']")).size() > 0,
                "The decoration must not leak into the application DOM");
    }

    @Test
    void hudIsInitializedAndReinjectedAfterNavigation() {
        open("/clicks");
        TestLens lens = configuredLens(true, true);
        lens.startSession("hud-navigation");
        assertTrue(await(hudPresent()));

        driver.navigate().to(baseUrl + "/second");
        assertFalse(hudPresent().apply(driver));
        lens.step("after navigation", () -> { });

        assertTrue(await(hudPresent()));
    }

    @ParameterizedTest(name = "finish {0}, cleanup={1}")
    @MethodSource("finalizationCases")
    void finalizationCleansHudAccordingToConfiguration(TraceStatus status, boolean cleanup) {
        open("/clicks");
        TestLens lens = configuredLens(true, cleanup);
        lens.startSession("finish-" + status + "-cleanup-" + cleanup + "-" + UUID.randomUUID());
        assertTrue(await(hudPresent()));
        lens.locator(By.id("count-button"), "Finalization target").click();
        assertTrue(await(highlightPresent()));

        TestLensFinalizationResult result = switch (status) {
            case PASSED -> lens.finishPassed();
            case FAILED -> lens.finishFailed(new AssertionError("expected integration-test failure state"));
            case SKIPPED -> lens.finishSkipped("not applicable in this browser");
            default -> throw new IllegalArgumentException("Unsupported finalization status: " + status);
        };

        assertEquals(status, result.session().metadata().status());
        assertEquals(!cleanup, hudPresent().apply(driver));
        assertEquals(!cleanup, highlightPresent().apply(driver));
        if (status == TraceStatus.SKIPPED) {
            assertNull(result.failureScreenshot());
        }
    }

    @Test
    void coveredClickClosesPreparedOverlayAndClicksTargetOnce() {
        open("/covered");
        OverlayConfig config = OverlayConfig.builder()
                .enabled(true)
                .decorationDurationMs(5_000)
                .globalOverlayCloseButtonSelector("#blocker-close")
                .build();
        TestLens lens = TestLens.attach(driver, config);

        lens.locator(By.id("count-button"), "Covered count").click();

        awaitClickCount(1);
        assertClickCounts(1);
        assertEquals(1L, number("return window.overlayCloseClicks || 0"));
        assertTrue(await(scriptBoolean("return getComputedStyle(document.getElementById('blocker')).display === 'none'")));
    }

    @Test
    void frameWindowAndAlertFacadesWorkInARealBrowser() {
        open("/contexts");
        TestLens lens = configuredLens(true, true);
        lens.startSession("browser-contexts");

        lens.switchToFrame(By.id("test-frame"), "Test frame");
        assertEquals("inside frame", driver.findElement(By.id("frame-value")).getText());
        lens.switchToDefaultContent();

        Set<String> before = lens.windowHandles();
        driver.findElement(By.id("popup-button")).click();
        String popup = lens.waitForNewWindow(before);
        lens.switchToWindow(popup, "Popup");
        assertEquals("popup ready", driver.findElement(By.id("popup-value")).getText());
        driver.close();
        lens.switchToWindow(before.iterator().next(), "Main");

        driver.findElement(By.id("alert-button")).click();
        lens.alert().waitUntilPresent();
        assertEquals("Test Lens alert", lens.alert().text());
        lens.alert().accept();
        assertTrue(await(scriptBoolean("return document.getElementById('alert-result').textContent === 'accepted'")));
    }

    @Test
    void strictCspDoesNotChangeTheSeleniumOperationOutcome() {
        open("/csp");
        JsOverlayDebug lens = overlay(true);

        lens.highlightThenClick(driver.findElement(By.id("count-button")), "CSP count");

        awaitClickCount(1);
        assertClickCounts(1);
    }

    @Test
    void missingElementFailsOnFirstRealBrowserObservationWhenFailFastIsEnabled() {
        open("/assertions");
        TestLens lens = TestLens.attach(driver, overlayConfig(false));

        UiAssertionError error = assertThrows(UiAssertionError.class, () -> lens
                .locator(By.id("never-present"), "Never present")
                .expect(assertionOptions(true))
                .toBeVisible());

        assertEquals(UiAssertionStatus.FAILED, error.result().status());
        assertEquals(UiAssertionFailureReason.ELEMENT_NOT_FOUND, error.result().failureReason());
        assertEquals(1, error.result().attempts());
    }

    @Test
    void defaultRetryPolicyFindsElementAddedAsynchronouslyToRealDom() {
        open("/assertions");
        TestLens lens = TestLens.attach(driver, overlayConfig(false));

        UiAssertionResult result = lens.locator(By.id("async-element"), "Async element")
                .expect(assertionOptions(false))
                .toBeVisible();

        assertEquals(UiAssertionStatus.PASSED, result.status());
        assertTrue(result.attempts() >= 2);
    }

    private static Stream<Arguments> finalizationCases() {
        return Stream.of(
                Arguments.of(TraceStatus.PASSED, true),
                Arguments.of(TraceStatus.FAILED, true),
                Arguments.of(TraceStatus.SKIPPED, true),
                Arguments.of(TraceStatus.PASSED, false),
                Arguments.of(TraceStatus.FAILED, false),
                Arguments.of(TraceStatus.SKIPPED, false));
    }

    private void open(String path) {
        driver = createDriver();
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(15));
        driver.get(baseUrl + path);
        await(webDriver -> "complete".equals(((JavascriptExecutor) webDriver)
                .executeScript("return document.readyState")));
    }

    private JsOverlayDebug overlay(boolean enabled) {
        return new JsOverlayDebug(driver, overlayConfig(enabled));
    }

    private OverlayConfig overlayConfig(boolean enabled) {
        return OverlayConfig.builder()
                .enabled(enabled)
                .decorationDurationMs(5_000)
                .build();
    }

    private UiAssertionOptions assertionOptions(boolean failFast) {
        return UiAssertionOptions.builder()
                .timeout(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(50))
                .failFastOnMissingElement(failFast)
                .build();
    }

    private TestLens configuredLens(boolean overlayEnabled, boolean cleanupHud) {
        return TestLens.attach(driver, TestLensOptions.builder()
                .overlayConfig(OverlayConfig.builder()
                        .enabled(overlayEnabled)
                        .decorationDurationMs(60_000)
                        .build())
                .cleanupHudOnFinish(cleanupHud)
                .screenshotOnFailure(false)
                .outputRoot(Path.of("target", "ui-test-lens", browserName()))
                .build());
    }

    private void awaitClickCount(long expected) {
        assertTrue(await(scriptBoolean("return (window.applicationClicks || 0) === arguments[0]", expected)));
    }

    private void assertClickCounts(long expected) {
        assertEquals(expected, number("return window.applicationClicks || 0"));
        assertEquals(expected, number("return window.trustedApplicationClicks || 0"));
    }

    private long number(String script) {
        return ((Number) ((JavascriptExecutor) driver).executeScript(script)).longValue();
    }

    private java.util.function.Function<WebDriver, Boolean> hudPresent() {
        return scriptBoolean("""
                const host = document.getElementById('selenium-overlay-host');
                return Boolean(host && host.shadowRoot && host.shadowRoot.querySelector('#selenium-hud-panel'));
                """);
    }

    private java.util.function.Function<WebDriver, Boolean> highlightPresent() {
        return scriptBoolean("""
                const host = document.getElementById('selenium-overlay-host');
                return Boolean(host && host.shadowRoot && host.shadowRoot.querySelector('[data-uitestlens-highlight="1"]'));
                """);
    }

    private java.util.function.Function<WebDriver, Boolean> scriptBoolean(String script, Object... arguments) {
        return webDriver -> Boolean.TRUE.equals(((JavascriptExecutor) webDriver).executeScript(script, arguments));
    }

    private boolean await(java.util.function.Function<WebDriver, Boolean> condition) {
        return Boolean.TRUE.equals(new WebDriverWait(driver, WAIT).until(condition));
    }

    private static WebDriver createDriver() {
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
        return System.getProperty("browser", "chrome").trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static void serve(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        switch (path) {
            case "/clicks" -> html(exchange, page("Clicks", "<button id='count-button'>Count</button><span id='click-count'>0</span>"), false);
            case "/covered" -> html(exchange, page("Covered", """
                    <button id='count-button'>Covered count</button><span id='click-count'>0</span>
                    <div id='blocker'><button id='blocker-close'>Close overlay</button></div>
                    """), false);
            case "/contexts" -> html(exchange, page("Contexts", """
                    <iframe id='test-frame' src='/frame'></iframe>
                    <button id='popup-button'>Open popup</button>
                    <button id='alert-button'>Open alert</button>
                    <span id='alert-result'>pending</span>
                    """), false);
            case "/frame" -> html(exchange, page("Frame", "<p id='frame-value'>inside frame</p>"), false);
            case "/popup-target" -> html(exchange, page("Popup", "<p id='popup-value'>popup ready</p>"), false);
            case "/second" -> html(exchange, page("Second", "<button id='count-button'>Count after navigation</button><span id='click-count'>0</span>"), false);
            case "/csp" -> html(exchange, page("CSP", "<button id='count-button'>CSP count</button><span id='click-count'>0</span>"), true);
            case "/assertions" -> html(exchange, page("Assertions", "<div id='async-container'></div>"), false);
            case "/app.js" -> response(exchange, "application/javascript; charset=utf-8", APP_JS, false);
            case "/app.css" -> response(exchange, "text/css; charset=utf-8", APP_CSS, false);
            default -> response(exchange, "text/plain; charset=utf-8", "not found", false, 404);
        }
    }

    private static String page(String title, String body) {
        return "<!doctype html><html><head><meta charset='utf-8'><title>" + title
                + "</title><link rel='stylesheet' href='/app.css'></head><body><main>" + body
                + "</main><script src='/app.js'></script></body></html>";
    }

    private static void html(HttpExchange exchange, String body, boolean strictCsp) throws IOException {
        response(exchange, "text/html; charset=utf-8", body, strictCsp);
    }

    private static void response(HttpExchange exchange, String contentType, String body, boolean strictCsp) throws IOException {
        response(exchange, contentType, body, strictCsp, 200);
    }

    private static void response(HttpExchange exchange, String contentType, String body, boolean strictCsp, int status) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        if (strictCsp) {
            exchange.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'");
        }
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static final String APP_JS = """
            window.applicationClicks = 0;
            window.trustedApplicationClicks = 0;
            const countButton = document.getElementById('count-button');
            if (countButton) countButton.addEventListener('click', event => {
              window.applicationClicks += 1;
              if (event.isTrusted) window.trustedApplicationClicks += 1;
              const count = document.getElementById('click-count');
              if (count) count.textContent = String(window.applicationClicks);
            });
            const close = document.getElementById('blocker-close');
            if (close) close.addEventListener('click', () => {
              window.overlayCloseClicks = (window.overlayCloseClicks || 0) + 1;
              document.getElementById('blocker').style.display = 'none';
            });
            const popup = document.getElementById('popup-button');
            if (popup) popup.addEventListener('click', () => window.open('/popup-target', '_blank'));
            const alertButton = document.getElementById('alert-button');
            if (alertButton) alertButton.addEventListener('click', () => {
              alert('Test Lens alert');
              document.getElementById('alert-result').textContent = 'accepted';
            });
            const asyncContainer = document.getElementById('async-container');
            if (asyncContainer) setTimeout(() => {
              const element = document.createElement('div');
              element.id = 'async-element';
              element.textContent = 'ready';
              asyncContainer.appendChild(element);
            }, 250);
            """;

    private static final String APP_CSS = """
            body { font-family: sans-serif; margin: 40px; }
            button { min-width: 140px; min-height: 44px; margin: 12px; }
            iframe { width: 360px; height: 140px; display: block; }
            #blocker { position: fixed; inset: 0; z-index: 1000; background: rgba(10,20,30,.75); display: grid; place-items: center; }
            """;
}
