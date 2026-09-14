package io.github.testlens.browser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.testlens.TestLens;
import io.github.testlens.selenium.auth.AuthCookie;
import io.github.testlens.selenium.auth.AuthState;
import io.github.testlens.selenium.auth.AuthStateEnsureOutcome;
import io.github.testlens.selenium.auth.AuthStateMetadata;
import io.github.testlens.selenium.auth.AuthStateRequest;
import io.github.testlens.selenium.auth.AuthStateValidation;
import io.github.testlens.selenium.auth.AuthStorageEntry;
import io.github.testlens.selenium.auth.AuthStorageType;
import io.github.testlens.selenium.auth.ManagedAuthStateException;
import io.github.testlens.selenium.auth.ManagedAuthStateFailureReason;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManagedAuthStateIT {
    @TempDir Path temp;
    private WebDriver driver;

    @AfterEach
    void closeDriver() { closeCurrentDriver(); }

    @Test
    void managedLifecycleCreatesRestoresInvalidatesRefreshesAndFailsSafe() throws Exception {
        try (LocalApplication application = LocalApplication.start()) {
            Path path = temp.resolve("primary.json");
            AtomicInteger logins = new AtomicInteger();

            driver = createDriver();
            driver.get(application.origin());
            TestLens firstLens = TestLens.attach(driver);
            AuthStateRequest first = request("primary-user", path, logins);
            assertEquals(AuthStateEnsureOutcome.CREATED, firstLens.authState().ensure(first).outcome());
            assertEquals(1, logins.get());
            closeCurrentDriver();

            driver = createDriver();
            driver.get(application.origin());
            TestLens secondLens = TestLens.attach(driver);
            AtomicInteger freshBrowserLogins = new AtomicInteger();
            AuthStateRequest second = request("primary-user", path, freshBrowserLogins);
            assertEquals(AuthStateEnsureOutcome.RESTORED, secondLens.authState().ensure(second).outcome());
            assertEquals(0, freshBrowserLogins.get());

            secondLens.authState().invalidate("primary-user");
            assertEquals(AuthStateEnsureOutcome.CREATED, secondLens.authState().ensure(second).outcome());
            assertEquals(1, freshBrowserLogins.get());

            invalidState(application.origin()).save(path);
            assertEquals(AuthStateEnsureOutcome.REFRESHED, secondLens.authState().ensure(second).outcome());
            assertEquals(2, freshBrowserLogins.get());

            byte[] before = Files.readAllBytes(path);
            AtomicInteger forbiddenLogin = new AtomicInteger();
            AuthStateRequest inconclusive = AuthStateRequest.builder().key("primary-user").path(path)
                    .login(webDriver -> forbiddenLogin.incrementAndGet())
                    .validate(webDriver -> AuthStateValidation.INCONCLUSIVE).build();
            ManagedAuthStateException failure = assertThrows(ManagedAuthStateException.class,
                    () -> secondLens.authState().ensure(inconclusive));
            assertEquals(ManagedAuthStateFailureReason.VALIDATION_INCONCLUSIVE, failure.reason());
            assertEquals(0, forbiddenLogin.get());
            assertArrayEquals(before, Files.readAllBytes(path));
        }
    }

    private AuthStateRequest request(String key, Path path, AtomicInteger logins) {
        return AuthStateRequest.builder().key(key).path(path)
                .login(webDriver -> {
                    logins.incrementAndGet();
                    webDriver.manage().addCookie(new Cookie("session", "authenticated"));
                    ((JavascriptExecutor) webDriver).executeScript(
                            "localStorage.setItem('auth-token','browser-token');"
                                    + "sessionStorage.setItem('auth-tab','authenticated');");
                })
                .validate(webDriver -> {
                    Object token = ((JavascriptExecutor) webDriver).executeScript(
                            "return localStorage.getItem('auth-token');");
                    Cookie cookie = webDriver.manage().getCookieNamed("session");
                    return cookie != null && "authenticated".equals(cookie.getValue())
                            && "browser-token".equals(token)
                            ? AuthStateValidation.AUTHENTICATED : AuthStateValidation.UNAUTHENTICATED;
                }).build();
    }

    private static AuthState invalidState(String origin) {
        return new AuthState(AuthStateMetadata.builder().origin(origin).build(),
                List.of(new AuthCookie("session", "invalid", "", "/", null, false, false, "Lax")),
                List.of(new AuthStorageEntry(origin, "auth-token", "invalid", AuthStorageType.LOCAL_STORAGE)),
                List.of(new AuthStorageEntry(origin, "auth-tab", "invalid", AuthStorageType.SESSION_STORAGE)));
    }

    private void closeCurrentDriver() {
        if (driver != null) {
            try { driver.quit(); } finally { driver = null; }
        }
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

    private record LocalApplication(HttpServer server, String origin) implements AutoCloseable {
        static LocalApplication start() throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", LocalApplication::html);
            server.start();
            return new LocalApplication(server, "http://127.0.0.1:" + server.getAddress().getPort());
        }

        private static void html(HttpExchange exchange) throws IOException {
            byte[] body = "<!doctype html><title>Managed Auth State</title>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        }

        @Override public void close() { server.stop(0); }
    }
}
