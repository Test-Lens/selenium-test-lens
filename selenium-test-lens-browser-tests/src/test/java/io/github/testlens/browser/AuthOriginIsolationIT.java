package io.github.testlens.browser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.testlens.JsOverlayDebug;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.selenium.auth.AuthCookie;
import io.github.testlens.selenium.auth.AuthRestoreOptions;
import io.github.testlens.selenium.auth.AuthRestoreResult;
import io.github.testlens.selenium.auth.AuthRestoreStatus;
import io.github.testlens.selenium.auth.AuthState;
import io.github.testlens.selenium.auth.AuthStateMetadata;
import io.github.testlens.selenium.auth.AuthStorageEntry;
import io.github.testlens.selenium.auth.AuthStorageType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthOriginIsolationIT {
    private WebDriver driver;

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

    @Test
    void redirectToSsoIsRejectedBeforeForeignOriginStateIsMutated() throws Exception {
        try (OriginServers origins = OriginServers.crossOriginRedirect()) {
            driver = createDriver();
            driver.get(origins.ssoOrigin());
            driver.manage().addCookie(new Cookie("sso-existing", "keep-cookie"));
            execute("localStorage.setItem('sso-existing', 'keep-local');"
                    + "sessionStorage.setItem('sso-existing', 'keep-session');");

            JsOverlayDebug lens = new JsOverlayDebug(driver);
            UiTestLensSession session = lens.startSession("auth-cross-origin-redirect");
            AuthRestoreResult result = lens.auth().restoreState(stateFor(origins.applicationOrigin()),
                    AuthRestoreOptions.defaults());

            assertEquals(AuthRestoreStatus.ORIGIN_MISMATCH, result.status());
            assertNull(result.exception());
            assertEquals(0, result.cookiesRestored());
            assertEquals(0, result.localStorageEntriesRestored());
            assertEquals(0, result.sessionStorageEntriesRestored());
            assertEquals("keep-cookie", driver.manage().getCookieNamed("sso-existing").getValue());
            assertNull(driver.manage().getCookieNamed("app-canary"));
            assertEquals("keep-local", execute("return localStorage.getItem('sso-existing');"));
            assertEquals("keep-session", execute("return sessionStorage.getItem('sso-existing');"));
            assertNull(execute("return localStorage.getItem('app-local');"));
            assertNull(execute("return sessionStorage.getItem('app-session');"));
            assertTrue(session.events().stream().anyMatch(event ->
                    "AUTH_STATE_RESTORE_STARTED".equals(event.attributes().get("uiEventType"))));
            assertTrue(session.events().stream().anyMatch(event ->
                    "AUTH_STATE_RESTORE_SKIPPED".equals(event.attributes().get("uiEventType"))));
            assertFalse(session.events().stream().anyMatch(event ->
                    "AUTH_STATE_RESTORE_PASSED".equals(event.attributes().get("uiEventType"))));
        }
    }

    @Test
    void sameOriginRedirectRestoresState() throws Exception {
        try (OriginServers origins = OriginServers.sameOriginRedirect()) {
            driver = createDriver();
            driver.get(origins.applicationOrigin() + "/landing");

            JsOverlayDebug lens = new JsOverlayDebug(driver);
            UiTestLensSession session = lens.startSession("auth-same-origin-redirect");
            AuthRestoreResult result = lens.auth().restoreState(stateFor(origins.applicationOrigin()),
                    AuthRestoreOptions.defaults());

            assertEquals(AuthRestoreStatus.RESTORED, result.status(), result.message());
            assertEquals(1, result.cookiesRestored());
            assertEquals(1, result.localStorageEntriesRestored());
            assertEquals(1, result.sessionStorageEntriesRestored());
            assertEquals("app-cookie-value", driver.manage().getCookieNamed("app-canary").getValue());
            assertEquals("app-local-value", execute("return localStorage.getItem('app-local');"));
            assertEquals("app-session-value", execute("return sessionStorage.getItem('app-session');"));
            assertTrue(session.events().stream().anyMatch(event ->
                    "AUTH_STATE_RESTORE_PASSED".equals(event.attributes().get("uiEventType"))));
        }
    }

    private Object execute(String script) {
        return ((JavascriptExecutor) driver).executeScript(script);
    }

    private static AuthState stateFor(String origin) {
        return new AuthState(
                AuthStateMetadata.builder().origin(origin).build(),
                List.of(new AuthCookie("app-canary", "app-cookie-value", "", "/", null, false, false, "Lax")),
                List.of(new AuthStorageEntry(origin, "app-local", "app-local-value", AuthStorageType.LOCAL_STORAGE)),
                List.of(new AuthStorageEntry(origin, "app-session", "app-session-value", AuthStorageType.SESSION_STORAGE)));
    }

    private static WebDriver createDriver() {
        boolean headed = Boolean.parseBoolean(System.getProperty("headed", "false"));
        return switch (System.getProperty("browser", "chrome").trim().toLowerCase(Locale.ROOT)) {
            case "chrome" -> {
                ChromeOptions options = new ChromeOptions();
                options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
                String binary = System.getProperty("test.chrome.binary", "").trim();
                if (!binary.isEmpty()) options.setBinary(binary);
                options.addArguments("--window-size=1280,900", "--disable-dev-shm-usage", "--no-sandbox");
                if (!headed) options.addArguments("--headless=new");
                yield new ChromeDriver(options);
            }
            case "firefox" -> {
                FirefoxOptions options = new FirefoxOptions();
                options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
                String binary = System.getProperty("test.firefox.binary", "").trim();
                if (!binary.isEmpty()) options.setBinary(binary);
                if (!headed) options.addArguments("-headless");
                yield new FirefoxDriver(options);
            }
            default -> throw new IllegalArgumentException("Expected -Dbrowser=chrome or firefox");
        };
    }

    private record OriginServers(HttpServer application, HttpServer sso, String applicationOrigin, String ssoOrigin)
            implements AutoCloseable {
        static OriginServers crossOriginRedirect() throws IOException {
            HttpServer sso = server();
            String ssoOrigin = origin(sso);
            sso.createContext("/", exchange -> html(exchange, "SSO"));
            sso.start();
            HttpServer application = server();
            application.createContext("/", exchange -> redirect(exchange, ssoOrigin + "/login"));
            application.start();
            return new OriginServers(application, sso, origin(application), ssoOrigin);
        }

        static OriginServers sameOriginRedirect() throws IOException {
            HttpServer application = server();
            String applicationOrigin = origin(application);
            application.createContext("/", exchange -> redirect(exchange, applicationOrigin + "/landing"));
            application.createContext("/landing", exchange -> html(exchange, "Application"));
            application.start();
            return new OriginServers(application, null, applicationOrigin, "");
        }

        private static HttpServer server() throws IOException {
            return HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        }

        private static String origin(HttpServer server) {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private static void redirect(HttpExchange exchange, String location) throws IOException {
            exchange.getResponseHeaders().set("Location", location);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        }

        private static void html(HttpExchange exchange, String title) throws IOException {
            byte[] body = ("<!doctype html><title>" + title + "</title>").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        }

        @Override
        public void close() {
            application.stop(0);
            if (sso != null) sso.stop(0);
        }
    }
}
