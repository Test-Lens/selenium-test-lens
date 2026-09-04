package io.github.testlens.browser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.TestLens;
import io.github.testlens.TestLensFinalizationResult;
import io.github.testlens.TestLensOptions;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.RetryOutcomePolicy;
import io.github.testlens.core.trace.RetryPolicyViolationException;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.selenium.assertions.UiAssertionError;
import io.github.testlens.selenium.assertions.UiAssertionFailureReason;
import io.github.testlens.selenium.assertions.UiAssertionOptions;
import io.github.testlens.selenium.assertions.UiAssertionResult;
import io.github.testlens.selenium.assertions.UiAssertionStatus;
import io.github.testlens.selenium.evidence.FailureBundleOptions;
import io.github.testlens.selenium.locator.UiLocatorException;
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
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

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

    @ParameterizedTest(name = "form and element actions (overlay enabled={0})")
    @ValueSource(booleans = {true, false})
    void formAndElementActionsWorkWithoutRawWebElementAccess(boolean enabled) throws Exception {
        Path first = Files.createTempFile("lens-upload-one-", ".txt");
        Path second = Files.createTempFile("lens-upload-two-", ".txt");
        try {
            Files.writeString(first, "one");
            Files.writeString(second, "two");
            open("/form-actions");
            TestLens lens = configuredLens(enabled, true);
            lens.startSession("form-actions-" + enabled + "-" + UUID.randomUUID());

            lens.locator(By.id("native-check"), "Native checkbox").check().check();
            assertTrue(driver.findElement(By.id("native-check")).isSelected());
            assertEquals(1L, number("return window.nativeClicks"));
            assertEquals(1L, number("return window.nativeChanges"));
            lens.locator(By.id("native-check"), "Native checkbox").uncheck().uncheck();
            assertFalse(driver.findElement(By.id("native-check")).isSelected());
            assertEquals(2L, number("return window.nativeClicks"));

            lens.locator(By.id("styled-mark"), "Styled checkbox").check();
            assertTrue(driver.findElement(By.id("styled-check")).isSelected());
            assertEquals(1L, number("return window.styledLabelClicks"));
            assertEquals(1L, number("return window.styledChanges"));
            lens.locator(By.id("nested-mark"), "Nested checkbox").check();
            assertTrue(driver.findElement(By.id("nested-check")).isSelected());

            lens.locator(By.id("native-radio"), "Radio").check();
            assertTrue(driver.findElement(By.id("native-radio")).isSelected());
            assertThrows(UiLocatorException.class,
                    () -> lens.locator(By.id("native-radio"), "Radio").uncheck());

            lens.locator(By.id("aria-check"), "ARIA checkbox").check();
            assertTrue(await(scriptBoolean("return document.getElementById('aria-check').getAttribute('aria-checked') === 'true'")));
            assertEquals(1L, number("return window.ariaClicks"));
            lens.locator(By.id("aria-switch"), "ARIA switch").check().uncheck();
            assertEquals("false", driver.findElement(By.id("aria-switch")).getDomAttribute("aria-checked"));
            assertEquals(2L, number("return window.switchClicks"));

            assertThrows(UiLocatorException.class,
                    () -> lens.locator(By.id("fake-checkbox"), "Fake checkbox").check());
            assertThrows(UiLocatorException.class,
                    () -> lens.locator(By.id("disabled-check"), "Disabled checkbox").check());
            assertEquals(0L, number("return window.disabledClicks || 0"));

            lens.locator(By.id("single-file"), "Single upload").upload(first);
            lens.locator(By.id("multi-file"), "Multiple upload").upload(first, second);
            assertEquals(1L, number("return document.getElementById('single-file').files.length"));
            assertEquals(2L, number("return document.getElementById('multi-file').files.length"));

            lens.locator(By.id("focus-field"), "Focus field").focus();
            assertEquals("focus-field", ((JavascriptExecutor) driver).executeScript("return document.activeElement.id"));
            assertEquals(1L, number("return window.focusEvents"));
            assertEquals(0L, number("return window.focusClicks"));
            lens.locator(By.id("far-target"), "Far target").scrollIntoView();
            assertTrue(await(scriptBoolean("""
                    const r=document.getElementById('far-target').getBoundingClientRect();
                    return r.top >= 0 && r.bottom <= window.innerHeight;
                    """)));
            assertFalse("far-target".equals(((JavascriptExecutor) driver).executeScript("return document.activeElement.id")));
            assertEquals(0L, number("return window.farClicks || 0"));

            ((JavascriptExecutor) driver).executeScript("document.getElementById('foreign-cover').style.display='block'");
            UiLocatorException intercepted = assertThrows(UiLocatorException.class,
                    () -> lens.locator(By.id("covered-check"), "Covered checkbox").check());
            assertTrue(hasCause(intercepted, org.openqa.selenium.ElementClickInterceptedException.class));
            assertFalse(driver.findElement(By.id("covered-check")).isSelected());

            if (enabled) {
                String hud = String.valueOf(((JavascriptExecutor) driver).executeScript("""
                        const h=document.getElementById('selenium-overlay-host');
                        return h && h.shadowRoot ? h.shadowRoot.textContent : '';
                        """));
                assertFalse(hud.contains(first.toString()));
                assertFalse(hud.contains(first.getFileName().toString()));
                assertFalse(hud.contains(second.getFileName().toString()));
            }
            TestLensFinalizationResult result = lens.finishPassed();
            String reports = Files.readString(result.jsonReport()) + Files.readString(result.htmlReport());
            assertFalse(reports.contains(first.toString()));
            assertFalse(reports.contains(first.getFileName().toString()));
            assertFalse(reports.contains(second.getFileName().toString()));
            assertEquals("Form actions", driver.getTitle());
        } finally {
            Files.deleteIfExists(first);
            Files.deleteIfExists(second);
        }
    }

    @Test
    void semanticRoleLocatorsUseBrowserComputedAccessibleNamesAndRemainLazy() {
        open("/semantic-locators");
        TestLens lens = configuredLens(true, true);
        lens.startSession("semantic-roles-" + UUID.randomUUID());

        assertEquals("labelled-button", lens.getByRole("button", "Save order").resolve().getDomAttribute("id"));
        assertEquals("multi-button", lens.getByRole("button", "Create invoice").resolve().getDomAttribute("id"));
        assertEquals("image-button", lens.getByRole("button", "Save image").resolve().getDomAttribute("id"));
        assertEquals("Save order", lens.getByRole("button", "Save order").accessibleName());

        var duplicates = lens.getByRole("button", "Duplicate action");
        assertEquals(2, duplicates.count());
        assertEquals("duplicate-one", duplicates.first().resolve().getDomAttribute("id"));
        assertEquals("duplicate-two", duplicates.nth(1).resolve().getDomAttribute("id"));
        assertEquals("duplicate-two", duplicates.last().resolve().getDomAttribute("id"));

        var late = lens.getByRole("button", "Added later");
        assertTrue(driver.findElements(By.id("late-semantic-button")).isEmpty());
        ((JavascriptExecutor) driver).executeScript("""
                const label = document.createElement('span');
                label.id = 'late-semantic-label';
                label.textContent = 'Added later';
                const button = document.createElement('button');
                button.id = 'late-semantic-button';
                button.setAttribute('aria-labelledby', label.id);
                document.getElementById('late-semantic-container').append(label, button);
                """);
        late.waitUntilVisible();
        assertEquals("late-semantic-button", late.resolve().getDomAttribute("id"));
        lens.finishPassed();
    }

    @Test
    void labelPlaceholderAndAltLocatorsKeepTheirDistinctSemantics() {
        open("/semantic-locators");
        TestLens lens = configuredLens(false, true);
        lens.startSession("semantic-sources-" + UUID.randomUUID());

        assertEquals("email", lens.getByLabel("Email address").resolve().getDomAttribute("id"));
        assertEquals("nested-input", lens.getByLabel("Nested field").resolve().getDomAttribute("id"));
        assertEquals("aria-label-input", lens.getByLabel("ARIA field").resolve().getDomAttribute("id"));
        assertEquals("aria-labelledby-input", lens.getByLabel("Referenced field").resolve().getDomAttribute("id"));
        assertEquals("multi-label-input", lens.getByLabel("First Second").resolve().getDomAttribute("id"));
        assertEquals(0, lens.getByLabel("Placeholder only").count());
        assertEquals(0, lens.getByLabel("Title only").count());
        assertEquals("placeholder-only", lens.getByPlaceholder("Placeholder only").resolve().getDomAttribute("id"));

        assertEquals("logo", lens.getByAltText("Company logo").resolve().getDomAttribute("id"));
        assertEquals("map-area", lens.getByAltText("Office map").resolve().getDomAttribute("id"));
        assertEquals("image-submit", lens.getByAltText("Submit image").resolve().getDomAttribute("id"));
        assertEquals("", lens.getByAltText("").accessibleName());
        lens.finishPassed();
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

    @Test
    void realStaleRecoveryIsReportedWithoutChangingPassedOutcome() throws Exception {
        open("/clicks");
        TestLens lens = retryLens(RetryOutcomePolicy.REPORT_ONLY);
        UiTestLensSession session = lens.startSession("real-stale-report-only-" + UUID.randomUUID());

        lens.locator(staleOnce(By.id("count-button")), "Stale once button").click();
        awaitClickCount(1);
        TestLensFinalizationResult result = lens.finishPassed();

        assertEquals(TraceStatus.PASSED, session.metadata().status());
        assertEquals(1, result.retrySummary().totalRetries());
        assertClickCounts(1);
        assertTrue(Files.readString(result.jsonReport()).contains("\"flakyCandidate\":true"));
        assertTrue(Files.readString(result.htmlReport()).contains("<h2>Flakiness</h2>"));
    }

    @Test
    void realStaleRecoveryCanFailPassedOutcomeAfterWritingReports() throws Exception {
        open("/clicks");
        TestLens lens = retryLens(RetryOutcomePolicy.FAIL_ON_ANY_RETRY);
        UiTestLensSession session = lens.startSession("real-stale-policy-failure-" + UUID.randomUUID());

        lens.locator(staleOnce(By.id("count-button")), "Stale once button").click();
        awaitClickCount(1);
        RetryPolicyViolationException failure = assertThrows(RetryPolicyViolationException.class, lens::finishPassed);

        Path directory = Path.of("target", "ui-test-lens", browserName())
                .resolve(session.metadata().name().toLowerCase(java.util.Locale.ROOT)).resolve(session.id());
        assertEquals(TraceStatus.FAILED, session.metadata().status());
        assertEquals(1, failure.retrySummary().totalRetries());
        assertClickCounts(1);
        assertTrue(Files.isRegularFile(directory.resolve("trace.json")));
        assertTrue(Files.readString(directory.resolve("report.html")).contains("flaky-failure"));
    }

    @ParameterizedTest(name = "failure bundle remains CSP-safe on {0}")
    @ValueSource(strings = {"/clicks", "/csp"})
    void realFailureBundleCapturesHudCleanViewAndKeepsDriverAlive(String page) throws Exception {
        open(page);
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
                .overlayConfig(OverlayConfig.builder().enabled(true).decorationDurationMs(60_000).build())
                .cleanupHudOnFinish(false)
                .failureBundleOptions(FailureBundleOptions.complete())
                .outputRoot(Path.of("target", "ui-test-lens", browserName()))
                .build());
        lens.startSession("real-failure-bundle-" + page.substring(1) + "-" + UUID.randomUUID());
        overlay(true).highlightClick(driver.findElement(By.id("count-button")), "Failure bundle target");
        assertTrue(await(hudPresent()));
        assertTrue(await(highlightPresent()));

        TestLensFinalizationResult result = lens.finishFailed(new AssertionError("expected browser IT failure"));

        assertEquals(TraceStatus.FAILED, result.session().metadata().status());
        assertTrue(Files.isRegularFile(result.failureScreenshot()));
        assertTrue(Files.isRegularFile(result.failureBundleDirectory().orElseThrow().resolve("failure-clean.png")));
        assertTrue(Files.isRegularFile(result.failureBundleDirectory().orElseThrow().resolve("page-source.html")));
        String context = Files.readString(result.failureBundleDirectory().orElseThrow().resolve("context.json"));
        assertTrue(context.contains("currentUrl"));
        assertTrue(context.contains("127.0.0.1"));
        assertTrue(context.contains("title"));
        assertTrue(context.contains("currentWindowHandle"));
        assertTrue(Files.readString(result.failureBundleDirectory().orElseThrow().resolve("runtime.json")).contains(browserName()));
        assertTrue(Files.isRegularFile(result.jsonReport()));
        assertTrue(Files.isRegularFile(result.htmlReport()));
        assertTrue(result.failureBundleManifest().isPresent());
        assertTrue(result.failureBundleArchive().isPresent());
        assertTrue(hudPresent().apply(driver), "cleanup=false must restore HUD after clean capture");
        assertTrue(highlightPresent().apply(driver), "cleanup=false must restore highlight after clean capture");
        assertFalse(driver.getTitle().isBlank(), "finishFailed must leave the WebDriver alive");
        try (ZipFile zip = new ZipFile(result.failureBundleArchive().orElseThrow().toFile())) {
            assertTrue(zip.getEntry("manifest.json") != null);
            assertTrue(zip.getEntry("trace.json") != null);
            assertTrue(zip.getEntry("report.html") != null);
            assertTrue(zip.getEntry("failure-diagnostic.png") != null);
            assertTrue(zip.getEntry("failure-clean.png") != null);
        }
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

    private TestLens retryLens(RetryOutcomePolicy policy) {
        return TestLens.attach(driver, TestLensOptions.builder()
                .overlayConfig(overlayConfig(false))
                .locatorOptions(io.github.testlens.selenium.locator.UiLocatorOptions.builder()
                        .timeout(Duration.ofSeconds(2)).pollInterval(Duration.ofMillis(25)).maxRetries(2).build())
                .retryOutcomePolicy(policy)
                .screenshotOnFailure(false)
                .outputRoot(Path.of("target", "ui-test-lens", browserName()))
                .build());
    }

    private By staleOnce(By delegate) {
        WebElement stale = driver.findElement(delegate);
        ((JavascriptExecutor) driver).executeScript("""
                const replacement = arguments[0].cloneNode(true);
                replacement.addEventListener('click', event => {
                  window.applicationClicks += 1;
                  if (event.isTrusted) window.trustedApplicationClicks += 1;
                  document.getElementById('click-count').textContent = String(window.applicationClicks);
                });
                arguments[0].replaceWith(replacement);
                """, stale);
        AtomicInteger calls = new AtomicInteger();
        return new By() {
            @Override
            public WebElement findElement(SearchContext context) {
                return calls.getAndIncrement() == 0 ? stale : context.findElement(delegate);
            }

            @Override
            public List<WebElement> findElements(SearchContext context) {
                return List.of(findElement(context));
            }

            @Override
            public String toString() {
                return delegate.toString();
            }
        };
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

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        java.util.Set<Throwable> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Throwable current = failure;
        while (current != null && seen.add(current)) {
            if (type.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
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
            case "/form-actions" -> html(exchange, page("Form actions", """
                    <input id='native-check' type='checkbox'>
                    <input id='native-radio' type='radio' name='choice'>
                    <input id='styled-check' type='checkbox' hidden>
                    <label id='styled-label' for='styled-check'><span id='styled-mark'>Styled</span></label>
                    <label id='nested-label'><input id='nested-check' type='checkbox' hidden><span id='nested-mark'>Nested</span></label>
                    <div id='aria-check' role='checkbox' aria-checked='false' tabindex='0'>ARIA check</div>
                    <button id='aria-switch' role='switch' aria-checked='false'>ARIA switch</button>
                    <input id='disabled-check' type='checkbox' disabled>
                    <div id='fake-checkbox' class='checkbox' data-state='unchecked'>Not semantic</div>
                    <input id='single-file' type='file' hidden>
                    <input id='multi-file' type='file' multiple hidden>
                    <input id='focus-field'>
                    <div id='covered-wrap'><input id='covered-check' type='checkbox'><div id='foreign-cover'></div></div>
                    <div id='spacer'></div><button id='far-target'>Far target</button>
                    """), false);
            case "/semantic-locators" -> html(exchange, page("Semantic locators", """
                    <span id='save-label'>Save order</span>
                    <button id='labelled-button' aria-labelledby='save-label'></button>
                    <span id='create-label'>Create</span><span id='invoice-label'>invoice</span>
                    <button id='multi-button' aria-labelledby='create-label invoice-label'></button>
                    <button id='image-button'><img alt='Save image'></button>
                    <label for='email'>Email address</label><input id='email'>
                    <label>Nested field <input id='nested-input'></label>
                    <input id='aria-label-input' aria-label='ARIA field'>
                    <span id='referenced-field-label'>Referenced field</span>
                    <input id='aria-labelledby-input' aria-labelledby='referenced-field-label'>
                    <label for='multi-label-input'>First</label><label for='multi-label-input'>Second</label>
                    <input id='multi-label-input'>
                    <input id='placeholder-only' placeholder='Placeholder only'>
                    <input id='title-only' title='Title only'>
                    <img id='logo' alt='Company logo'>
                    <map name='office'><area id='map-area' href='#office' alt='Office map'></map>
                    <input id='image-submit' type='image' alt='Submit image'>
                    <img id='decorative-image' alt=''>
                    <button id='duplicate-one'>Duplicate action</button>
                    <button id='duplicate-two'>Duplicate action</button>
                    <div id='late-semantic-container'></div>
                    """), false);
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
            const nativeCheck = document.getElementById('native-check');
            if (nativeCheck) {
              window.nativeClicks = 0; window.nativeChanges = 0;
              nativeCheck.addEventListener('click', () => window.nativeClicks++);
              nativeCheck.addEventListener('change', () => window.nativeChanges++);
            }
            const styledLabel = document.getElementById('styled-label');
            if (styledLabel) styledLabel.addEventListener('click', () => window.styledLabelClicks = (window.styledLabelClicks || 0) + 1);
            const styledCheck = document.getElementById('styled-check');
            if (styledCheck) styledCheck.addEventListener('change', () => window.styledChanges = (window.styledChanges || 0) + 1);
            const disabledCheck = document.getElementById('disabled-check');
            if (disabledCheck) disabledCheck.addEventListener('click', () => window.disabledClicks = (window.disabledClicks || 0) + 1);
            const ariaCheck = document.getElementById('aria-check');
            if (ariaCheck) ariaCheck.addEventListener('click', () => {
              window.ariaClicks = (window.ariaClicks || 0) + 1;
              const replacement = ariaCheck.cloneNode(true);
              replacement.setAttribute('aria-checked', 'false');
              ariaCheck.replaceWith(replacement);
              queueMicrotask(() => replacement.setAttribute('aria-checked', 'true'));
            });
            const ariaSwitch = document.getElementById('aria-switch');
            if (ariaSwitch) ariaSwitch.addEventListener('click', () => {
              window.switchClicks = (window.switchClicks || 0) + 1;
              ariaSwitch.setAttribute('aria-checked', ariaSwitch.getAttribute('aria-checked') === 'true' ? 'false' : 'true');
            });
            const focusField = document.getElementById('focus-field');
            if (focusField) {
              window.focusEvents = 0; window.focusClicks = 0;
              focusField.addEventListener('focus', () => window.focusEvents++);
              focusField.addEventListener('click', () => window.focusClicks++);
            }
            const farTarget = document.getElementById('far-target');
            if (farTarget) farTarget.addEventListener('click', () => window.farClicks = (window.farClicks || 0) + 1);
            """;

    private static final String APP_CSS = """
            body { font-family: sans-serif; margin: 40px; }
            button { min-width: 140px; min-height: 44px; margin: 12px; }
            iframe { width: 360px; height: 140px; display: block; }
            #blocker { position: fixed; inset: 0; z-index: 1000; background: rgba(10,20,30,.75); display: grid; place-items: center; }
            #styled-label, #nested-label, [role='checkbox'], [role='switch'] { display: inline-block; padding: 12px; margin: 8px; border: 1px solid #777; }
            #covered-wrap { position: relative; width: 80px; height: 40px; }
            #foreign-cover { display: none; position: absolute; inset: 0; z-index: 20; background: rgba(200,0,0,.5); }
            #spacer { height: 1800px; }
            """;
}
