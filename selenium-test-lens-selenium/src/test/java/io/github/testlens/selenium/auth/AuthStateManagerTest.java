package io.github.testlens.selenium.auth;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthStateManagerTest {

    @Test
    void capturesCookiesLocalStorageAndSessionStorage() {
        FakeBrowser browser = new FakeBrowser();
        browser.currentUrl = "https://app.example.com/dashboard";
        browser.cookies.put("session", new Cookie.Builder("session", "abc").domain("app.example.com").path("/").build());
        browser.localStorage.put("theme", "dark");
        browser.sessionStorage.put("tab", "checkout");

        AuthState state = new AuthStateManager(browser.driver()).captureState(AuthStateOptions.builder()
                .label("standard-customer")
                .role("customer")
                .build());

        assertEquals("standard-customer", state.metadata().label());
        assertEquals("customer", state.metadata().role());
        assertEquals("https://app.example.com", state.metadata().origin());
        assertEquals("app.example.com", state.metadata().domain());
        assertEquals(1, state.cookies().size());
        assertEquals("theme", state.localStorage().get(0).key());
        assertEquals("tab", state.sessionStorage().get(0).key());
    }

    @Test
    void restoresCookiesLocalStorageAndSessionStorage() {
        FakeBrowser browser = new FakeBrowser();
        browser.currentUrl = "https://app.example.com/login";
        AuthState state = new AuthState(
                AuthStateMetadata.builder().origin("https://app.example.com").build(),
                java.util.List.of(new AuthCookie("session", "abc", "app.example.com", "/", null, true, true, "Lax")),
                java.util.List.of(new AuthStorageEntry("https://app.example.com", "theme", "dark", AuthStorageType.LOCAL_STORAGE)),
                java.util.List.of(new AuthStorageEntry("https://app.example.com", "tab", "checkout", AuthStorageType.SESSION_STORAGE))
        );

        AuthRestoreResult result = new AuthStateManager(browser.driver()).restoreState(state, AuthRestoreOptions.defaults());

        assertEquals(AuthRestoreStatus.RESTORED, result.status(), result.message());
        assertEquals("https://app.example.com", browser.currentUrl);
        assertTrue(browser.cookies.containsKey("session"));
        assertEquals("dark", browser.localStorage.get("theme"));
        assertEquals("checkout", browser.sessionStorage.get("tab"));
        assertEquals(1, result.cookiesRestored());
    }

    @Test
    void expiredStateReturnsExpired() {
        FakeBrowser browser = new FakeBrowser();
        AuthState state = new AuthState(
                AuthStateMetadata.builder()
                        .origin("https://app.example.com")
                        .expiresAt(Instant.now().minusSeconds(1))
                        .build(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of()
        );

        AuthRestoreResult result = new AuthStateManager(browser.driver()).restoreState(state, AuthRestoreOptions.defaults());

        assertEquals(AuthRestoreStatus.EXPIRED, result.status());
    }

    @Test
    void originMismatchWithoutNavigationReturnsOriginMismatch() {
        FakeBrowser browser = new FakeBrowser();
        browser.currentUrl = "https://other.example.com";
        AuthState state = new AuthState(
                AuthStateMetadata.builder().origin("https://app.example.com").build(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of()
        );

        AuthRestoreResult result = new AuthStateManager(browser.driver()).restoreState(state, AuthRestoreOptions.builder()
                .navigateToOrigin(false)
                .build());

        assertEquals(AuthRestoreStatus.ORIGIN_MISMATCH, result.status());
    }

    @Test
    void unsupportedJavascriptExecutorFailsStorageCapture() {
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(
                AuthStateManagerTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getCurrentUrl" -> "https://app.example.com";
                    case "manage" -> noOpOptions();
                    case "toString" -> "driver-without-js";
                    default -> defaultValue(method.getReturnType());
                }
        );

        try {
            new AuthStateManager(driver).captureState(AuthStateOptions.defaults());
        } catch (AuthStateException e) {
            assertTrue(e.getMessage().contains("JavascriptExecutor"));
            return;
        }
        assertFalse(true, "Expected AuthStateException");
    }

    private static WebDriver.Options noOpOptions() {
        return (WebDriver.Options) Proxy.newProxyInstance(
                AuthStateManagerTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.Options.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getCookies" -> Set.of();
                    case "toString" -> "options";
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class || returnType == long.class || returnType == short.class || returnType == byte.class) {
            return 0;
        }
        if (returnType == double.class || returnType == float.class) {
            return 0.0;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class FakeBrowser {
        private String currentUrl = "https://app.example.com";
        private final Map<String, Cookie> cookies = new LinkedHashMap<>();
        private final Map<String, String> localStorage = new LinkedHashMap<>();
        private final Map<String, String> sessionStorage = new LinkedHashMap<>();

        WebDriver driver() {
            return (WebDriver) Proxy.newProxyInstance(
                    AuthStateManagerTest.class.getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getCurrentUrl" -> currentUrl;
                        case "get" -> {
                            currentUrl = String.valueOf(args[0]);
                            yield null;
                        }
                        case "manage" -> options();
                        case "executeScript" -> executeScript(String.valueOf(args[0]), args);
                        case "executeAsyncScript" -> null;
                        case "toString" -> "fake-auth-driver";
                        default -> defaultValue(method.getReturnType());
                    }
            );
        }

        private WebDriver.Options options() {
            return (WebDriver.Options) Proxy.newProxyInstance(
                    AuthStateManagerTest.class.getClassLoader(),
                    new Class<?>[]{WebDriver.Options.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getCookies" -> Set.copyOf(cookies.values());
                        case "addCookie" -> {
                            Cookie cookie = (Cookie) args[0];
                            cookies.put(cookie.getName(), cookie);
                            yield null;
                        }
                        case "deleteAllCookies" -> {
                            cookies.clear();
                            yield null;
                        }
                        case "toString" -> "fake-options";
                        default -> defaultValue(method.getReturnType());
                    }
            );
        }

        private Object executeScript(String script, Object[] args) {
            Object[] scriptArgs = args.length > 1 && args[1] instanceof Object[] nested ? nested : new Object[0];
            if (script.contains("storage.length") && !script.contains("storage.setItem")) {
                String kind = scriptArgs.length > 0 ? String.valueOf(scriptArgs[0]) : "";
                return new LinkedHashMap<>("session".equals(kind) ? sessionStorage : localStorage);
            }
            if (script.contains("window.localStorage.clear")) {
                localStorage.clear();
                sessionStorage.clear();
                return null;
            }
            if (script.contains("storage.setItem")) {
                String kind = scriptArgs.length > 0 ? String.valueOf(scriptArgs[0]) : "";
                String key = scriptArgs.length > 1 ? String.valueOf(scriptArgs[1]) : "";
                String value = scriptArgs.length > 2 ? String.valueOf(scriptArgs[2]) : "";
                ("session".equals(kind) ? sessionStorage : localStorage).put(key, value);
                return null;
            }
            return null;
        }
    }
}

