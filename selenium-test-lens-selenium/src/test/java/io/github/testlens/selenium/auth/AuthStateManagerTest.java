package io.github.testlens.selenium.auth;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogger;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthStateManagerTest {

    @Test
    void rejectsRedirectToForeignOriginBeforeMutatingCookiesOrStorage() {
        FakeBrowser browser = new FakeBrowser();
        browser.currentUrl = "https://app.example.com/login";
        browser.navigationResultUrl = "https://sso.example.com/login";
        browser.cookies.put("sso-session", new Cookie("sso-session", "keep-cookie"));
        browser.localStorage.put("sso-local", "keep-local");
        browser.sessionStorage.put("sso-session", "keep-session");
        AuthState state = new AuthState(
                AuthStateMetadata.builder().origin("https://app.example.com").build(),
                java.util.List.of(new AuthCookie("app-session", "application-cookie", "app.example.com", "/", null, true, true, "Lax")),
                java.util.List.of(new AuthStorageEntry("https://app.example.com", "app-local", "application-token", AuthStorageType.LOCAL_STORAGE)),
                java.util.List.of(new AuthStorageEntry("https://app.example.com", "app-session", "application-session", AuthStorageType.SESSION_STORAGE))
        );

        AuthRestoreResult result = new AuthStateManager(browser.driver()).restoreState(state, AuthRestoreOptions.defaults());

        assertEquals(AuthRestoreStatus.ORIGIN_MISMATCH, result.status());
        assertEquals(0, result.cookiesRestored());
        assertEquals(0, result.localStorageEntriesRestored());
        assertEquals(0, result.sessionStorageEntriesRestored());
        assertEquals(0, browser.deleteAllCookiesCalls);
        assertEquals(0, browser.addCookieCalls);
        assertEquals(0, browser.clearStorageCalls);
        assertEquals(0, browser.setStorageCalls);
        assertEquals("keep-cookie", browser.cookies.get("sso-session").getValue());
        assertEquals("keep-local", browser.localStorage.get("sso-local"));
        assertEquals("keep-session", browser.sessionStorage.get("sso-session"));
    }

    @Test
    void acceptsNavigationToAnotherPathOnTheSameOrigin() {
        FakeBrowser browser = new FakeBrowser();
        browser.navigationResultUrl = "https://app.example.com/dashboard?ready=true#main";

        AuthRestoreResult result = restore(browser, "https://app.example.com/login", AuthRestoreOptions.defaults());

        assertEquals(AuthRestoreStatus.RESTORED, result.status(), result.message());
        assertEquals(1, browser.addCookieCalls);
        assertEquals("application-token", browser.localStorage.get("app-local"));
    }

    @Test
    void rejectsRedirectToDifferentPortOrScheme() {
        FakeBrowser portBrowser = new FakeBrowser();
        portBrowser.navigationResultUrl = "http://app.example.com:8081/sso";
        AuthRestoreResult portResult = restore(portBrowser, "http://app.example.com:8080", AuthRestoreOptions.defaults());
        assertEquals(AuthRestoreStatus.ORIGIN_MISMATCH, portResult.status());
        assertNoMutations(portBrowser);

        FakeBrowser schemeBrowser = new FakeBrowser();
        schemeBrowser.navigationResultUrl = "https://app.example.com/login";
        AuthRestoreResult schemeResult = restore(schemeBrowser, "http://app.example.com", AuthRestoreOptions.defaults());
        assertEquals(AuthRestoreStatus.ORIGIN_MISMATCH, schemeResult.status());
        assertNoMutations(schemeBrowser);
    }

    @Test
    void canonicalizesCaseDefaultPortsAndIpv6Origins() {
        assertEquals("https://example.test", AuthStateManager.originOf("HTTPS://EXAMPLE.TEST:443/path"));
        assertEquals("http://example.test", AuthStateManager.originOf("http://EXAMPLE.TEST:80"));
        assertEquals("http://[::1]:8080", AuthStateManager.originOf("http://[::1]:8080/path"));
        assertEquals("", AuthStateManager.originOf("about:blank"));
        assertEquals("", AuthStateManager.originOf("example.test/path"));

        FakeBrowser browser = new FakeBrowser();
        browser.navigationResultUrl = "https://example.test/landing";
        AuthRestoreResult result = restore(browser, "HTTPS://EXAMPLE.TEST:443/path", AuthRestoreOptions.defaults());
        assertEquals(AuthRestoreStatus.RESTORED, result.status());
    }

    @Test
    void validatesCurrentOriginWhenNavigationIsDisabled() {
        AuthRestoreOptions options = AuthRestoreOptions.builder().navigateToOrigin(false).build();
        FakeBrowser same = new FakeBrowser();
        same.currentUrl = "https://app.example.com/current";
        assertEquals(AuthRestoreStatus.RESTORED,
                restore(same, "https://app.example.com", options).status());

        FakeBrowser foreign = new FakeBrowser();
        foreign.currentUrl = "https://sso.example.com/current";
        assertEquals(AuthRestoreStatus.ORIGIN_MISMATCH,
                restore(foreign, "https://app.example.com", options).status());
        assertNoMutations(foreign);
    }

    @Test
    void invalidOrUnreadableCurrentUrlCannotAuthorizeRestore() {
        AuthRestoreOptions options = AuthRestoreOptions.builder().navigateToOrigin(false).build();
        for (String url : List.of("", "not a url", "about:blank")) {
            FakeBrowser browser = new FakeBrowser();
            browser.currentUrl = url;
            assertEquals(AuthRestoreStatus.ORIGIN_MISMATCH,
                    restore(browser, "https://app.example.com", options).status());
            assertNoMutations(browser);
        }

        FakeBrowser failedRead = new FakeBrowser();
        WebDriverException failure = new WebDriverException("current URL unavailable");
        failedRead.currentUrlFailure = failure;
        AuthRestoreResult failed = restore(failedRead, "https://app.example.com", options);
        assertEquals(AuthRestoreStatus.FAILED, failed.status());
        assertSame(failure, failed.exception());
        assertNoMutations(failedRead);
    }

    @Test
    void rejectsMixedOriginStorageDuringPreflightBeforeNavigation() {
        FakeBrowser browser = new FakeBrowser();
        AuthState state = state("https://app.example.com",
                List.of(new AuthStorageEntry("https://sso.example.com", "foreign", "secret", AuthStorageType.LOCAL_STORAGE)),
                List.of());

        AuthRestoreResult result = new AuthStateManager(browser.driver()).restoreState(state, AuthRestoreOptions.defaults());

        assertEquals(AuthRestoreStatus.ORIGIN_MISMATCH, result.status());
        assertEquals(0, browser.navigationCalls);
        assertNoMutations(browser);
    }

    @Test
    void rejectsInvalidStateOriginDuringPreflightBeforeNavigation() {
        for (String origin : List.of("not an origin", "about:blank", "/relative")) {
            FakeBrowser browser = new FakeBrowser();

            AuthRestoreResult result = new AuthStateManager(browser.driver()).restoreState(
                    state(origin, List.of(), List.of()), AuthRestoreOptions.defaults());

            assertEquals(AuthRestoreStatus.ORIGIN_MISMATCH, result.status());
            assertEquals(0, browser.navigationCalls);
            assertNoMutations(browser);
        }
    }

    @Test
    void atomicStorageGuardStopsMutationWhenOriginChangesAfterJavaValidation() {
        FakeBrowser browser = new FakeBrowser();
        browser.currentUrl = "https://app.example.com/current";
        browser.changeOriginBeforeStorageMutation = "https://sso.example.com/login";
        browser.localStorage.put("sso-local", "keep-local");
        AuthRestoreOptions options = AuthRestoreOptions.builder()
                .navigateToOrigin(false)
                .clearExistingCookies(false)
                .restoreCookies(false)
                .build();

        AuthRestoreResult result = restore(browser, "https://app.example.com", options);

        assertEquals(AuthRestoreStatus.ORIGIN_MISMATCH, result.status());
        assertEquals(0, browser.clearStorageCalls);
        assertEquals(0, browser.setStorageCalls);
        assertEquals("keep-local", browser.localStorage.get("sso-local"));
        assertFalse(browser.localStorage.containsKey("app-local"));
    }

    @Test
    void rechecksOriginImmediatelyBeforeCookieMutation() {
        FakeBrowser browser = new FakeBrowser();
        browser.navigationResultUrl = "https://app.example.com/ready";
        browser.changeOriginOnCurrentUrlCall = 2;
        browser.changedCurrentUrl = "https://sso.example.com/login";

        AuthRestoreResult result = restore(browser, "https://app.example.com", AuthRestoreOptions.defaults());

        assertEquals(AuthRestoreStatus.ORIGIN_MISMATCH, result.status());
        assertNoMutations(browser);
    }

    @Test
    void cookieFailureIsTerminalAndStorageIsNotRestored() {
        FakeBrowser browser = new FakeBrowser();
        WebDriverException failure = new WebDriverException("cookie domain rejected");
        browser.addCookieFailure = failure;

        AuthRestoreResult result = restore(browser, "https://app.example.com", AuthRestoreOptions.defaults());

        assertEquals(AuthRestoreStatus.FAILED, result.status());
        assertSame(failure, result.exception());
        assertEquals(1, browser.deleteAllCookiesCalls);
        assertEquals(1, browser.addCookieCalls);
        assertEquals(0, browser.clearStorageCalls);
        assertEquals(0, browser.setStorageCalls);
    }

    @Test
    void explicitOriginValidationOptOutRetainsExistingMutationBehavior() {
        FakeBrowser browser = new FakeBrowser();
        browser.currentUrl = "https://sso.example.com/current";
        browser.cookies.put("sso", new Cookie("sso", "old"));
        AuthRestoreOptions options = AuthRestoreOptions.builder()
                .navigateToOrigin(false)
                .validateOrigin(false)
                .build();

        AuthRestoreResult result = restore(browser, "https://app.example.com", options);

        assertEquals(AuthRestoreStatus.RESTORED, result.status());
        assertEquals(1, browser.deleteAllCookiesCalls);
        assertEquals(1, browser.addCookieCalls);
        assertEquals("application-token", browser.localStorage.get("app-local"));
    }

    @Test
    void mismatchEmitsStartedAndSkippedWithoutPassedOrSensitiveValues() {
        FakeBrowser browser = new FakeBrowser();
        browser.navigationResultUrl = "https://sso.example.com/login";
        List<UiTestLensLogEntry> entries = new ArrayList<>();
        OverlayLogger logger = OverlayLogger.from(UiTestLensLogger.builder().sink(entries::add).build());
        AuthState state = state("https://app.example.com",
                List.of(new AuthStorageEntry("https://app.example.com", "storage-token", "canary-storage-value", AuthStorageType.LOCAL_STORAGE)),
                List.of());

        AuthRestoreResult result = new AuthStateManager(browser.driver(), logger)
                .restoreState(state, AuthRestoreOptions.defaults());

        assertEquals(AuthRestoreStatus.ORIGIN_MISMATCH, result.status());
        assertEquals(1, entries.stream().filter(entry -> entry.eventType() == UiTestLensEventType.AUTH_STATE_RESTORE_STARTED).count());
        assertEquals(1, entries.stream().filter(entry -> entry.eventType() == UiTestLensEventType.AUTH_STATE_RESTORE_SKIPPED).count());
        assertEquals(0, entries.stream().filter(entry -> entry.eventType() == UiTestLensEventType.AUTH_STATE_RESTORE_PASSED).count());
        assertTrue(entries.stream().noneMatch(entry -> entry.toString().contains("canary-storage-value")));
        assertTrue(entries.stream().noneMatch(entry -> entry.toString().contains("application-cookie")));
    }

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
        assertNull(result.exception());
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

    private static AuthRestoreResult restore(FakeBrowser browser, String origin, AuthRestoreOptions options) {
        return new AuthStateManager(browser.driver()).restoreState(
                state(origin,
                        List.of(new AuthStorageEntry(origin, "app-local", "application-token", AuthStorageType.LOCAL_STORAGE)),
                        List.of(new AuthStorageEntry(origin, "app-session", "application-session", AuthStorageType.SESSION_STORAGE))),
                options);
    }

    private static AuthState state(String origin,
                                   List<AuthStorageEntry> localStorage,
                                   List<AuthStorageEntry> sessionStorage) {
        return new AuthState(
                AuthStateMetadata.builder().origin(origin).build(),
                List.of(new AuthCookie("app-session", "application-cookie", "app.example.com", "/", null, true, true, "Lax")),
                localStorage,
                sessionStorage);
    }

    private static void assertNoMutations(FakeBrowser browser) {
        assertEquals(0, browser.deleteAllCookiesCalls);
        assertEquals(0, browser.addCookieCalls);
        assertEquals(0, browser.clearStorageCalls);
        assertEquals(0, browser.setStorageCalls);
    }

    private static final class FakeBrowser {
        private String currentUrl = "https://app.example.com";
        private String navigationResultUrl;
        private RuntimeException currentUrlFailure;
        private RuntimeException addCookieFailure;
        private String changeOriginBeforeStorageMutation;
        private int changeOriginOnCurrentUrlCall;
        private String changedCurrentUrl;
        private int getCurrentUrlCalls;
        private int navigationCalls;
        private int deleteAllCookiesCalls;
        private int addCookieCalls;
        private int clearStorageCalls;
        private int setStorageCalls;
        private final Map<String, Cookie> cookies = new LinkedHashMap<>();
        private final Map<String, String> localStorage = new LinkedHashMap<>();
        private final Map<String, String> sessionStorage = new LinkedHashMap<>();

        WebDriver driver() {
            return (WebDriver) Proxy.newProxyInstance(
                    AuthStateManagerTest.class.getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getCurrentUrl" -> {
                            if (currentUrlFailure != null) throw currentUrlFailure;
                            getCurrentUrlCalls++;
                            if (changeOriginOnCurrentUrlCall == getCurrentUrlCalls) currentUrl = changedCurrentUrl;
                            yield currentUrl;
                        }
                        case "get" -> {
                            navigationCalls++;
                            currentUrl = navigationResultUrl == null ? String.valueOf(args[0]) : navigationResultUrl;
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
                            addCookieCalls++;
                            if (addCookieFailure != null) throw addCookieFailure;
                            Cookie cookie = (Cookie) args[0];
                            cookies.put(cookie.getName(), cookie);
                            yield null;
                        }
                        case "deleteAllCookies" -> {
                            deleteAllCookiesCalls++;
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
            boolean guarded = script.contains("window.location.origin");
            if (guarded && changeOriginBeforeStorageMutation != null) {
                currentUrl = changeOriginBeforeStorageMutation;
                changeOriginBeforeStorageMutation = null;
            }
            if (guarded && (scriptArgs.length == 0 ||
                    !AuthStateManager.originOf(currentUrl).equals(String.valueOf(scriptArgs[0])))) {
                return false;
            }
            if (script.contains("window.localStorage.clear")) {
                clearStorageCalls++;
                localStorage.clear();
                sessionStorage.clear();
                return guarded ? true : null;
            }
            if (script.contains("storage.setItem")) {
                int offset = guarded ? 1 : 0;
                setStorageCalls++;
                String kind = scriptArgs.length > offset ? String.valueOf(scriptArgs[offset]) : "";
                String key = scriptArgs.length > offset + 1 ? String.valueOf(scriptArgs[offset + 1]) : "";
                String value = scriptArgs.length > offset + 2 ? String.valueOf(scriptArgs[offset + 2]) : "";
                ("session".equals(kind) ? sessionStorage : localStorage).put(key, value);
                return guarded ? true : null;
            }
            return null;
        }
    }
}

