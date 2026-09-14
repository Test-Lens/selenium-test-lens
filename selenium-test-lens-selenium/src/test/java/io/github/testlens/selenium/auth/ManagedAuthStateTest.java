package io.github.testlens.selenium.auth;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.testlens.selenium.auth.AuthStateValidation.AUTHENTICATED;
import static io.github.testlens.selenium.auth.AuthStateValidation.INCONCLUSIVE;
import static io.github.testlens.selenium.auth.AuthStateValidation.UNAUTHENTICATED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedAuthStateTest {
    @TempDir Path temp;

    @Test
    void missingStateLogsInOnceValidatesCapturesAndCreates() {
        FakeBrowser browser = new FakeBrowser();
        AtomicInteger logins = new AtomicInteger();
        Path path = temp.resolve("state.json");

        AuthStateEnsureResult result = manager(browser).ensure(request("primary", path,
                driver -> { logins.incrementAndGet(); browser.login(); },
                driver -> browser.authenticated() ? AUTHENTICATED : UNAUTHENTICATED));

        assertEquals(AuthStateEnsureOutcome.CREATED, result.outcome());
        assertEquals(1, logins.get());
        assertTrue(Files.isRegularFile(path));
        assertEquals("fresh-token", AuthState.load(path).localStorage().get(0).value());
        assertNoTempFiles();
    }

    @Test
    void validExistingStateRestoresWithoutLoginOrWrite() throws Exception {
        Path path = temp.resolve("state.json");
        existingState("valid").save(path);
        byte[] before = Files.readAllBytes(path);
        FakeBrowser browser = new FakeBrowser();
        AtomicInteger logins = new AtomicInteger();

        AuthStateEnsureResult result = manager(browser).ensure(request("primary", path,
                driver -> logins.incrementAndGet(), driver -> browser.authenticated() ? AUTHENTICATED : UNAUTHENTICATED));

        assertEquals(AuthStateEnsureOutcome.RESTORED, result.outcome());
        assertEquals(0, logins.get());
        assertArrayEquals(before, Files.readAllBytes(path));
    }

    @Test
    void unauthenticatedExistingStateClearsLogsInOnceAndRefreshes() throws Exception {
        Path path = temp.resolve("state.json");
        existingState("expired").save(path);
        FakeBrowser browser = new FakeBrowser();
        AtomicInteger logins = new AtomicInteger();

        AuthStateEnsureResult result = manager(browser).ensure(request("primary", path,
                driver -> { logins.incrementAndGet(); browser.login(); },
                driver -> browser.authenticated() ? AUTHENTICATED : UNAUTHENTICATED));

        assertEquals(AuthStateEnsureOutcome.REFRESHED, result.outcome());
        assertEquals(1, logins.get());
        assertTrue(browser.deleteAllCookiesCalls >= 2); // restore clear plus managed pre-login clear
        assertTrue(browser.clearStorageCalls >= 2);
    }

    @Test
    void inconclusiveAfterRestoreNeverLogsInAndPreservesOldBytes() throws Exception {
        assertRestoreValidationFailure(INCONCLUSIVE, ManagedAuthStateFailureReason.VALIDATION_INCONCLUSIVE);
    }

    @Test
    void validatorExceptionAfterRestoreNeverLogsInAndPreservesOldBytes() throws Exception {
        Path path = temp.resolve("state.json");
        existingState("valid").save(path);
        byte[] before = Files.readAllBytes(path);
        AtomicInteger logins = new AtomicInteger();
        RuntimeException cause = new RuntimeException("validator bug");

        ManagedAuthStateException failure = assertThrows(ManagedAuthStateException.class, () ->
                manager(new FakeBrowser()).ensure(request("primary", path, driver -> logins.incrementAndGet(),
                        driver -> { throw cause; })));

        assertEquals(ManagedAuthStateFailureReason.VALIDATION_FAILED, failure.reason());
        assertEquals(cause, failure.getCause());
        assertEquals(0, logins.get());
        assertArrayEquals(before, Files.readAllBytes(path));
    }

    @Test
    void corruptStateIsRecreatedOnceAndFailurePreservesCorruptBytes() throws Exception {
        Path successPath = temp.resolve("corrupt-success.json");
        Files.writeString(successPath, "{corrupt");
        FakeBrowser successBrowser = new FakeBrowser();
        AtomicInteger logins = new AtomicInteger();
        assertEquals(AuthStateEnsureOutcome.REFRESHED, manager(successBrowser).ensure(request("success", successPath,
                driver -> { logins.incrementAndGet(); successBrowser.login(); }, driver -> AUTHENTICATED)).outcome());
        assertEquals(1, logins.get());
        assertNotNull(AuthState.load(successPath));

        Path failurePath = temp.resolve("corrupt-failure.json");
        byte[] corrupt = "{AUTH_TOKEN_MUST_NOT_APPEAR_8C2B".getBytes(StandardCharsets.UTF_8);
        Files.write(failurePath, corrupt);
        ManagedAuthStateException failure = assertThrows(ManagedAuthStateException.class, () ->
                manager(new FakeBrowser()).ensure(request("failure", failurePath,
                        driver -> { throw new RuntimeException("login broke"); }, driver -> AUTHENTICATED)));
        assertEquals(ManagedAuthStateFailureReason.LOGIN_FAILED, failure.reason());
        assertArrayEquals(corrupt, Files.readAllBytes(failurePath));
    }

    @Test
    void wrongOriginIsRecreatedOnce() {
        Path path = temp.resolve("wrong-origin.json");
        existingState("valid", "https://wrong.example").save(path);
        FakeBrowser browser = new FakeBrowser();
        browser.navigationOverride = FakeBrowser.ORIGIN;
        AtomicInteger logins = new AtomicInteger();

        AuthStateEnsureResult result = manager(browser).ensure(request("primary", path,
                driver -> { logins.incrementAndGet(); browser.login(); }, driver -> AUTHENTICATED));

        assertEquals(AuthStateEnsureOutcome.REFRESHED, result.outcome());
        assertEquals(1, logins.get());
    }

    @Test
    void failuresAfterLoginNeverPublishOrReplaceState() throws Exception {
        for (AuthStateValidation validation : List.of(UNAUTHENTICATED, INCONCLUSIVE)) {
            Path path = temp.resolve("state-" + validation + ".json");
            FakeBrowser browser = new FakeBrowser();
            AuthStateManager manager = manager(browser);
            java.util.concurrent.atomic.AtomicReference<AuthStateValidation> current =
                    new java.util.concurrent.atomic.AtomicReference<>(AUTHENTICATED);
            manager.ensure(request("key-" + validation, path, driver -> browser.login(), driver -> current.get()));
            byte[] before = Files.readAllBytes(path);
            current.set(validation);
            ManagedAuthStateException failure = assertThrows(ManagedAuthStateException.class,
                    () -> manager.refresh("key-" + validation));
            assertTrue(failure.reason() == ManagedAuthStateFailureReason.LOGIN_DID_NOT_AUTHENTICATE
                    || failure.reason() == ManagedAuthStateFailureReason.VALIDATION_INCONCLUSIVE);
            assertArrayEquals(before, Files.readAllBytes(path));
        }
    }

    @Test
    void loginAndCaptureFailuresDoNotPublishState() {
        Path loginPath = temp.resolve("login.json");
        ManagedAuthStateException loginFailure = assertThrows(ManagedAuthStateException.class, () ->
                manager(new FakeBrowser()).ensure(request("login", loginPath,
                        driver -> { throw new RuntimeException("AUTH_PASSWORD_MUST_NOT_APPEAR_7F3A"); },
                        driver -> AUTHENTICATED)));
        assertEquals(ManagedAuthStateFailureReason.LOGIN_FAILED, loginFailure.reason());
        assertFalse(Files.exists(loginPath));
        assertFalse(loginFailure.getMessage().contains("AUTH_PASSWORD_MUST_NOT_APPEAR_7F3A"));

        Path capturePath = temp.resolve("capture.json");
        FakeBrowser browser = new FakeBrowser();
        browser.captureFailure = new RuntimeException("capture token");
        ManagedAuthStateException captureFailure = assertThrows(ManagedAuthStateException.class, () ->
                manager(browser).ensure(request("capture", capturePath, driver -> browser.login(), driver -> AUTHENTICATED)));
        assertEquals(ManagedAuthStateFailureReason.CAPTURE_FAILED, captureFailure.reason());
        assertFalse(Files.exists(capturePath));
    }

    @Test
    void validationAndCaptureFailuresDuringRefreshPreserveOldBytes() throws Exception {
        Path validationPath = temp.resolve("validation-refresh.json");
        FakeBrowser validationBrowser = new FakeBrowser();
        AtomicInteger validations = new AtomicInteger();
        AuthStateManager validationManager = manager(validationBrowser);
        validationManager.ensure(request("validation", validationPath, driver -> validationBrowser.login(), driver -> {
            if (validations.incrementAndGet() > 1) throw new RuntimeException("validator callback bug");
            return AUTHENTICATED;
        }));
        byte[] validationBefore = Files.readAllBytes(validationPath);
        ManagedAuthStateException validationFailure = assertThrows(ManagedAuthStateException.class,
                () -> validationManager.refresh("validation"));
        assertEquals(ManagedAuthStateFailureReason.VALIDATION_FAILED, validationFailure.reason());
        assertArrayEquals(validationBefore, Files.readAllBytes(validationPath));

        Path capturePath = temp.resolve("capture-refresh.json");
        FakeBrowser captureBrowser = new FakeBrowser();
        AuthStateManager captureManager = manager(captureBrowser);
        captureManager.ensure(request("capture", capturePath, driver -> captureBrowser.login(), driver -> AUTHENTICATED));
        byte[] captureBefore = Files.readAllBytes(capturePath);
        captureBrowser.captureFailure = new RuntimeException("capture callback bug");
        ManagedAuthStateException captureFailure = assertThrows(ManagedAuthStateException.class,
                () -> captureManager.refresh("capture"));
        assertEquals(ManagedAuthStateFailureReason.CAPTURE_FAILED, captureFailure.reason());
        assertArrayEquals(captureBefore, Files.readAllBytes(capturePath));
    }

    @Test
    void browserCleanupFailureStopsBeforeLoginAndPreservesOldState() throws Exception {
        Path path = temp.resolve("cleanup.json");
        existingState("expired").save(path);
        byte[] before = Files.readAllBytes(path);
        FakeBrowser browser = new FakeBrowser();
        AtomicInteger logins = new AtomicInteger();

        ManagedAuthStateException failure = assertThrows(ManagedAuthStateException.class, () ->
                manager(browser).ensure(request("cleanup", path, driver -> logins.incrementAndGet(),
                        driver -> {
                            browser.clearFailure = new RuntimeException("storage unavailable");
                            return UNAUTHENTICATED;
                        })));

        assertEquals(ManagedAuthStateFailureReason.BROWSER_STATE_CLEAR_FAILED, failure.reason());
        assertEquals(0, logins.get());
        assertArrayEquals(before, Files.readAllBytes(path));
    }

    @Test
    void persistenceFailurePreservesOldBytesAndCleansTemporaryFile() throws Exception {
        Path path = temp.resolve("state.json");
        existingState("valid").save(path);
        byte[] before = Files.readAllBytes(path);
        FakeBrowser browser = new FakeBrowser();
        AuthStateStore failing = new AuthStateStore() {
            @Override void atomicReplace(Path target, AuthState state) {
                Path leftover = target.resolveSibling(".testlens-auth-test.tmp");
                try { Files.writeString(leftover, state.exportJson()); Files.deleteIfExists(leftover); }
                catch (Exception e) { throw new AssertionError(e); }
                throw new ManagedAuthStateException(ManagedAuthStateFailureReason.PERSIST_FAILED,
                        "Managed auth state persistence failed");
            }
        };
        AuthStateManager manager = manager(browser, failing);
        manager.ensure(request("primary", path, driver -> browser.login(),
                driver -> browser.authenticated() ? AUTHENTICATED : UNAUTHENTICATED));
        browser.cookies.clear();
        ManagedAuthStateException failure = assertThrows(ManagedAuthStateException.class, () -> manager.refresh("primary"));
        assertEquals(ManagedAuthStateFailureReason.PERSIST_FAILED, failure.reason());
        assertArrayEquals(before, Files.readAllBytes(path));
        assertNoTempFiles();
    }

    @Test
    void refreshAndInvalidateUseRegistryWithoutLogoutOrBrowserClearOnInvalidate() throws Exception {
        FakeBrowser browser = new FakeBrowser();
        AuthStateManager manager = manager(browser);
        Path path = temp.resolve("state.json");
        AtomicInteger logins = new AtomicInteger();
        manager.ensure(request("primary", path, driver -> { logins.incrementAndGet(); browser.login(); },
                driver -> AUTHENTICATED));
        byte[] old = Files.readAllBytes(path);

        browser.cookies.clear();
        assertEquals(AuthStateEnsureOutcome.REFRESHED, manager.refresh("primary").outcome());
        assertEquals(2, logins.get());
        assertFalse(java.util.Arrays.equals(old, Files.readAllBytes(path)));
        int clears = browser.clearStorageCalls;
        int cookieDeletes = browser.deleteAllCookiesCalls;
        manager.invalidate("primary");
        assertFalse(Files.exists(path));
        assertEquals(clears, browser.clearStorageCalls);
        assertEquals(cookieDeletes, browser.deleteAllCookiesCalls);
        assertEquals(2, logins.get());
        assertEquals(AuthStateEnsureOutcome.REFRESHED, manager.refresh("primary").outcome());
    }

    @Test
    void unknownKeysAndPathCollisionFailFast() {
        AuthStateManager manager = manager(new FakeBrowser());
        assertEquals(ManagedAuthStateFailureReason.UNKNOWN_KEY,
                assertThrows(ManagedAuthStateException.class, () -> manager.refresh("missing")).reason());
        assertEquals(ManagedAuthStateFailureReason.UNKNOWN_KEY,
                assertThrows(ManagedAuthStateException.class, () -> manager.invalidate("missing")).reason());
        manager.ensure(request("same", temp.resolve("one.json"), driver -> {}, driver -> AUTHENTICATED));
        assertEquals(ManagedAuthStateFailureReason.REGISTRATION_CONFLICT,
                assertThrows(ManagedAuthStateException.class, () -> manager.ensure(
                        request("same", temp.resolve("two.json"), driver -> {}, driver -> AUTHENTICATED))).reason());
    }

    @Test
    void requestValidationAndToStringsNeverExposeSensitiveConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> AuthStateRequest.builder().path(temp.resolve("x")).build());
        assertThrows(IllegalArgumentException.class, () -> AuthStateRequest.builder().key("x").build());
        String marker = "john.smith@example.invalid";
        AuthStateRequest request = request(marker, temp.resolve("AUTH_TOKEN_MUST_NOT_APPEAR_8C2B.json"),
                driver -> {}, driver -> AUTHENTICATED);
        String requestText = request.toString();
        String resultText = manager(new FakeBrowser()).ensure(request).toString();
        assertFalse(requestText.contains(marker));
        assertFalse(requestText.contains("AUTH_TOKEN_MUST_NOT_APPEAR_8C2B"));
        assertFalse(resultText.contains(marker));
    }

    @Test
    void lifecycleEventsContainNoKeyPathCallbacksOrStateContents() {
        List<UiTestLensLogEntry> events = new ArrayList<>();
        OverlayLogger logger = OverlayLogger.from(UiTestLensLogger.builder().sink(events::add).build());
        FakeBrowser browser = new FakeBrowser();
        String key = "john.smith@example.invalid";
        Path path = temp.resolve("AUTH_TOKEN_MUST_NOT_APPEAR_8C2B.json");

        new AuthStateManager(browser.driver(), logger).ensure(request(key, path,
                driver -> browser.login("AUTH_PASSWORD_MUST_NOT_APPEAR_7F3A"), driver -> AUTHENTICATED));

        String emitted = events.toString();
        assertTrue(events.stream().anyMatch(event -> event.eventType() == UiTestLensEventType.AUTH_STATE_CREATED));
        assertFalse(emitted.contains(key));
        assertFalse(emitted.contains(path.toString()));
        assertFalse(emitted.contains("AUTH_PASSWORD_MUST_NOT_APPEAR_7F3A"));
        assertFalse(emitted.contains("AUTH_TOKEN_MUST_NOT_APPEAR_8C2B"));
        assertFalse(emitted.contains("fresh-token"));
    }

    @Test
    void sameCanonicalPathSerializesConcurrentEnsureAndCreatesOnce() throws Exception {
        Path path = temp.resolve("nested").resolve("..").resolve("state.json");
        AtomicInteger logins = new AtomicInteger();
        int workers = 8;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<java.util.concurrent.Future<AuthStateEnsureResult>> futures = new ArrayList<>();
            for (int index = 0; index < workers; index++) {
                futures.add(executor.submit(() -> {
                    FakeBrowser browser = new FakeBrowser();
                    AuthStateManager manager = manager(browser);
                    start.await();
                    return manager.ensure(request("key", path, driver -> {
                        logins.incrementAndGet(); browser.login();
                    }, driver -> browser.authenticated() ? AUTHENTICATED : UNAUTHENTICATED));
                }));
            }
            start.countDown();
            int created = 0;
            for (var future : futures) {
                if (future.get(10, TimeUnit.SECONDS).outcome() == AuthStateEnsureOutcome.CREATED) created++;
            }
            assertEquals(1, created);
            assertEquals(1, logins.get());
        } finally {
            executor.shutdownNow();
        }
        assertNotNull(AuthState.load(temp.resolve("state.json")));
    }

    private void assertRestoreValidationFailure(AuthStateValidation validation,
                                                ManagedAuthStateFailureReason reason) throws Exception {
        Path path = temp.resolve("state.json");
        existingState("valid").save(path);
        byte[] before = Files.readAllBytes(path);
        AtomicInteger logins = new AtomicInteger();
        ManagedAuthStateException failure = assertThrows(ManagedAuthStateException.class, () ->
                manager(new FakeBrowser()).ensure(request("primary", path,
                        driver -> logins.incrementAndGet(), driver -> validation)));
        assertEquals(reason, failure.reason());
        assertEquals(0, logins.get());
        assertArrayEquals(before, Files.readAllBytes(path));
    }

    private AuthStateManager manager(FakeBrowser browser) { return manager(browser, new AuthStateStore()); }
    private AuthStateManager manager(FakeBrowser browser, AuthStateStore store) {
        return new AuthStateManager(browser.driver(), OverlayLogger.noop(), store,
                new AuthStateLockManager(), new AuthStateRegistrationRegistry());
    }

    private static AuthStateRequest request(String key, Path path, AuthStateLogin login, AuthStateValidator validator) {
        return AuthStateRequest.builder().key(key).path(path).login(login).validate(validator).build();
    }

    private static AuthState existingState(String cookieValue) { return existingState(cookieValue, FakeBrowser.ORIGIN); }
    private static AuthState existingState(String cookieValue, String origin) {
        return new AuthState(AuthStateMetadata.builder().origin(origin).build(),
                List.of(new AuthCookie("session", cookieValue, "app.example", "/", null, false, true, "Lax")),
                List.of(new AuthStorageEntry(origin, "token", cookieValue, AuthStorageType.LOCAL_STORAGE)),
                List.of(new AuthStorageEntry(origin, "tab", "authenticated", AuthStorageType.SESSION_STORAGE)));
    }

    private void assertNoTempFiles() {
        try (var files = Files.walk(temp)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith(".testlens-auth-")
                    && path.getFileName().toString().endsWith(".tmp")));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static final class FakeBrowser {
        static final String ORIGIN = "https://app.example";
        String currentUrl = ORIGIN + "/login";
        String navigationOverride;
        RuntimeException captureFailure;
        RuntimeException clearFailure;
        int deleteAllCookiesCalls;
        int clearStorageCalls;
        final Map<String, Cookie> cookies = new LinkedHashMap<>();
        final Map<String, String> localStorage = new LinkedHashMap<>();
        final Map<String, String> sessionStorage = new LinkedHashMap<>();

        void login() { login("fresh-token"); }
        void login(String token) {
            cookies.put("session", new Cookie.Builder("session", "valid").domain("app.example").path("/").build());
            localStorage.put("token", token);
            sessionStorage.put("tab", "authenticated");
        }
        boolean authenticated() {
            Cookie cookie = cookies.get("session");
            return cookie != null && "valid".equals(cookie.getValue());
        }

        WebDriver driver() {
            return (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getCurrentUrl" -> currentUrl;
                        case "get" -> { currentUrl = navigationOverride == null ? String.valueOf(args[0]) : navigationOverride; yield null; }
                        case "manage" -> options();
                        case "executeScript" -> script(String.valueOf(args[0]), args);
                        case "executeAsyncScript" -> null;
                        case "toString" -> "managed-auth-fake";
                        default -> defaultValue(method.getReturnType());
                    });
        }

        WebDriver.Options options() {
            return (WebDriver.Options) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.Options.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getCookies" -> {
                            if (captureFailure != null) throw captureFailure;
                            yield Set.copyOf(cookies.values());
                        }
                        case "addCookie" -> { Cookie cookie = (Cookie) args[0]; cookies.put(cookie.getName(), cookie); yield null; }
                        case "deleteAllCookies" -> { deleteAllCookiesCalls++; cookies.clear(); yield null; }
                        default -> defaultValue(method.getReturnType());
                    });
        }

        Object script(String script, Object[] args) {
            Object[] values = args.length > 1 && args[1] instanceof Object[] nested ? nested : new Object[0];
            if (script.contains("storage.length") && !script.contains("setItem")) {
                if (captureFailure != null) throw captureFailure;
                return new LinkedHashMap<>("session".equals(String.valueOf(values[0])) ? sessionStorage : localStorage);
            }
            boolean guarded = script.contains("window.location.origin");
            if (guarded && !AuthStateManager.originOf(currentUrl).equals(String.valueOf(values[0]))) return false;
            if (script.contains("localStorage.clear")) {
                if (clearFailure != null) throw clearFailure;
                clearStorageCalls++; localStorage.clear(); sessionStorage.clear(); return guarded ? true : null;
            }
            if (script.contains("setItem")) {
                int offset = guarded ? 1 : 0;
                Map<String, String> target = "session".equals(String.valueOf(values[offset])) ? sessionStorage : localStorage;
                target.put(String.valueOf(values[offset + 1]), String.valueOf(values[offset + 2]));
                return guarded ? true : null;
            }
            return null;
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class || type == long.class || type == short.class || type == byte.class) return 0;
        return null;
    }
}
