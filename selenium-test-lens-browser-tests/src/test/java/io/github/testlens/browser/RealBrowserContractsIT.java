package io.github.testlens.browser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.TestLens;
import io.github.testlens.TestLensFinalizationResult;
import io.github.testlens.TestLensOptions;
import io.github.testlens.core.trace.TraceEventType;
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
import io.github.testlens.selenium.evidence.ScreenshotCaptureMode;
import io.github.testlens.selenium.evidence.ScreenshotCaptureOptions;
import io.github.testlens.selenium.evidence.ScreenshotCaptureResult;
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
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealBrowserContractsIT {
    private static final Duration WAIT = Duration.ofSeconds(5);
    private static HttpServer server;
    private static String baseUrl;
    private static final ConcurrentHashMap<String, ControlledRequest> CONTROLLED_REQUESTS =
            new ConcurrentHashMap<>();

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

    @ParameterizedTest(name = "composite card locators (overlay enabled={0})")
    @ValueSource(booleans = {true, false})
    void compositeLocatorsScopeFilterAndClickExactlyOneCardButton(boolean enabled) {
        open("/composite-locators");
        TestLens lens = configuredLens(enabled, true);
        lens.startSession("composite-cards-" + enabled + "-" + UUID.randomUUID());
        var buy = lens.getByRole("button", "Buy");
        var emptyCard = lens.locator(By.id("laptop-sold"), "Empty card");
        var targetCard = lens.locator(By.id("laptop-available"), "Target card");
        var xpathCard = lens.locator(By.id("phone-available"), "XPath target card");
        var cards = lens.locator(By.cssSelector(".product-card"), "Product cards")
                .filterByTextContaining("Laptop")
                .filterByAttribute("data-status", "available")
                .filterHas(buy);

        assertEquals(0, emptyCard.filterHas(buy).count());
        cards.waitUntilCountAtLeast(1);
        targetCard.locator(buy).click();
        xpathCard.locator(By.xpath("//button[@id='phone-available-buy']")).click();

        assertEquals(1L, number("return (window.buttonClicks || {})['laptop-available-buy'] || 0"));
        assertEquals(1L, number("return (window.buttonClicks || {})['phone-available-buy'] || 0"));
        assertEquals(0L, number("return (window.buttonClicks || {})['global-before-buy'] || 0"));
        assertEquals(0L, number("return (window.buttonClicks || {})['global-after-buy'] || 0"));
        assertEquals(0L, number("return window.globalBuyClicks || 0"));
        assertEquals(1, cards.count());
        lens.finishPassed();
    }

    @ParameterizedTest(name = "composite count waits (overlay enabled={0})")
    @ValueSource(booleans = {true, false})
    void compositeCountWaitsObserveDynamicRerenders(boolean enabled) {
        open("/composite-locators");
        TestLens lens = configuredLens(enabled, true);
        lens.startSession("composite-count-" + enabled + "-" + UUID.randomUUID());
        var dynamicItems = lens.locator(By.cssSelector("#dynamic-list .dynamic-item"), "Dynamic items");
        assertEquals(1, dynamicItems.count());

        lens.locator(By.id("start-dynamic"), "Start dynamic changes").click();
        dynamicItems.waitUntilCountAtLeast(3).waitUntilCountAtMost(1).waitUntilCount(1);

        assertEquals(1, dynamicItems.count());
        assertEquals("final", dynamicItems.first().attribute("data-phase"));
        assertEquals("Composite locators", driver.getTitle());
        lens.finishPassed();
    }

    @ParameterizedTest(name = "collection and value assertions (overlay enabled={0})")
    @ValueSource(booleans = {true, false})
    void locatorStateAssertionsPollCollectionsAttributesClassesAndCss(boolean enabled) {
        open("/assertion-states");
        TestLens lens = configuredLens(enabled, true);
        lens.startSession("state-values-" + enabled + "-" + UUID.randomUUID());
        lens.locator(By.id("start-state-changes")).click();

        UiAssertionResult count = lens.locator(By.cssSelector("#state-list .state-item"))
                .filterByAttribute("data-status", "available")
                .expect(assertionOptions(false)).toHaveCount(3);
        UiAssertionResult attribute = lens.locator(By.id("busy-control"))
                .expect(assertionOptions(false)).toHaveAttribute("aria-busy", "false");
        UiAssertionResult classResult = lens.locator(By.id("class-control"))
                .expect(assertionOptions(false)).toHaveClass("ready");
        UiAssertionResult css = lens.locator(By.id("css-control"))
                .expect(assertionOptions(false)).toHaveCss("display", "block");

        for (UiAssertionResult result : List.of(count, attribute, classResult, css)) {
            assertEquals(UiAssertionStatus.PASSED, result.status());
            assertTrue(result.attempts() >= 1);
            assertFalse(result.assertionName().isBlank());
            assertNotNull(result.elapsed());
        }
        assertTrue(count.attempts() >= 1);
        lens.finishPassed();
    }

    @ParameterizedTest(name = "selected and checked assertions (overlay enabled={0})")
    @ValueSource(booleans = {true, false})
    void locatorStateAssertionsReadSelectedAndCheckedWithoutActivation(boolean enabled) {
        open("/assertion-states");
        TestLens lens = configuredLens(enabled, true);
        lens.startSession("state-controls-" + enabled + "-" + UUID.randomUUID());

        assertTrue(lens.locator(By.id("native-option")).expect().toBeSelected().isPassed());
        assertTrue(lens.locator(By.id("native-checked")).expect().toBeChecked().isPassed());
        assertTrue(lens.locator(By.cssSelector("#styled-label .decoration")).expect().toBeChecked().isPassed());
        assertTrue(lens.locator(By.id("aria-checkbox")).expect().toBeChecked().isPassed());
        assertTrue(lens.locator(By.id("aria-switch")).expect().toBeUnchecked().isPassed());
        assertTrue(lens.locator(By.id("aria-option")).expect().toBeSelected().isPassed());

        assertEquals(0L, number("return window.assertionClicks || 0"));
        assertEquals(0L, number("return window.assertionChanges || 0"));
        lens.finishPassed();
    }

    @ParameterizedTest(name = "attachment assertions (overlay enabled={0})")
    @ValueSource(booleans = {true, false})
    void locatorStateAssertionsTrackAttachmentDetachmentAndReplacement(boolean enabled) {
        open("/assertion-states");
        TestLens lens = configuredLens(enabled, true);
        lens.startSession("state-attachment-" + enabled + "-" + UUID.randomUUID());
        var added = lens.locator(By.id("late-attached"));
        var removed = lens.locator(By.id("will-detach"));
        var replacement = lens.locator(By.id("replacement-target"))
                .filterByAttribute("data-version", "new");

        lens.locator(By.id("start-attachment-changes")).click();
        UiAssertionResult attached = added.expect(assertionOptions(false)).toBeAttached();
        UiAssertionResult detached = removed.expect(assertionOptions(false)).toBeDetached();
        UiAssertionResult replaced = replacement.expect(assertionOptions(false)).toBeAttached();

        assertEquals(UiAssertionStatus.PASSED, attached.status());
        assertEquals(UiAssertionStatus.PASSED, detached.status());
        assertEquals(UiAssertionStatus.PASSED, replaced.status());
        assertTrue(attached.attempts() >= 1);
        assertEquals("new", replacement.attribute("data-version"));
        lens.finishPassed();
    }

    @ParameterizedTest(name = "page URL and title assertions poll safely (overlay enabled={0})")
    @ValueSource(booleans = {true, false})
    void pageAssertionsPollUrlAndTitleWithoutLeakingUrlSecrets(boolean enabled) throws Exception {
        open("/page-expectations");
        TestLens lens = configuredLens(enabled, true);
        UiTestLensSession session = lens.startSession("page-expectations-" + enabled + "-" + UUID.randomUUID());
        UiAssertionOptions options = assertionOptions(false);
        String expectedUrl = baseUrl + "/page-expectations/dashboard?token=browser-secret#private-fragment";

        UiAssertionResult url = lens.expectPage(options).toHaveUrl(expectedUrl);
        UiAssertionResult title = lens.expectPage(options).toHaveTitle("Ready Dashboard");
        assertEquals(UiAssertionStatus.PASSED, url.status());
        assertEquals(UiAssertionStatus.PASSED, title.status());
        assertTrue(url.attempts() >= 1);
        assertTrue(title.attempts() >= 1);

        TestLensFinalizationResult finalized = lens.finishPassed();
        String diagnostics = session.events() + Files.readString(finalized.jsonReport())
                + Files.readString(finalized.htmlReport());
        assertFalse(diagnostics.contains("browser-secret"));
        assertFalse(diagnostics.contains("private-fragment"));
        assertFalse(diagnostics.contains("token="));
    }

    @Test
    void pageAssertionsUseTheExplicitlySelectedPopupWindow() {
        open("/contexts");
        TestLens lens = configuredLens(true, true);
        lens.startSession("page-popup-" + UUID.randomUUID());
        Set<String> before = lens.windowHandles();
        driver.findElement(By.id("popup-button")).click();
        lens.switchToWindow(lens.waitForNewWindow(before), "Page assertion popup");

        assertEquals(UiAssertionStatus.PASSED,
                lens.expectPage(assertionOptions(false)).toContainUrl("/popup-target").status());
        assertEquals(UiAssertionStatus.PASSED,
                lens.expectPage(assertionOptions(false)).toContainTitle("Pop").status());
        assertEquals("Popup", driver.getTitle());
        lens.finishPassed();
    }

    @Test
    void pageWaitFacadeObservesDocumentReadinessWithPageLoadStrategyNone() {
        driver = createDriver(PageLoadStrategy.NONE);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(15));
        driver.get(baseUrl + "/page-waits");
        TestLens lens = configuredLens(true, true);
        UiTestLensSession session = lens.startSession("page-ready-wait-" + UUID.randomUUID());

        lens.waitForInteractiveOrComplete(Duration.ofSeconds(3));
        lens.waitForPageReady(Duration.ofSeconds(3));

        assertEquals("complete", ((JavascriptExecutor) driver).executeScript("return document.readyState"));
        assertEquals(4, session.events().stream()
                .filter(event -> "page.wait".equals(event.attributes().get("action"))).count());
        lens.finishPassed();
    }

    @Test
    void xhrFetchTrackerWaitsForCompletionAndIsReinstalledAfterNavigation() {
        open("/page-waits");
        TestLens lens = configuredLens(true, true);
        lens.startSession("network-idle-wait-" + UUID.randomUUID());

        lens.waitForNetworkIdle(Duration.ZERO, Duration.ofSeconds(1));
        ((JavascriptExecutor) driver).executeScript(
                "void fetch(arguments[0]); return null;", baseUrl + "/wait-delayed");
        long firstStarted = System.nanoTime();
        lens.waitForNetworkIdle(Duration.ofMillis(100), Duration.ofSeconds(3));
        long firstElapsed = Duration.ofNanos(System.nanoTime() - firstStarted).toMillis();
        assertTrue(firstElapsed >= 250, "wait returned before the observed fetch and idle window completed");

        driver.navigate().to(baseUrl + "/page-waits-next");
        lens.waitForNetworkIdle(Duration.ZERO, Duration.ofSeconds(1));
        ((JavascriptExecutor) driver).executeScript(
                "void fetch(arguments[0]); return null;", baseUrl + "/wait-delayed");
        lens.waitForNetworkIdle(Duration.ofMillis(100), Duration.ofSeconds(3));
        assertEquals(0L, number("return window.__seleniumActiveRequests || 0"));
        lens.finishPassed();
    }

    @Test
    void xhrFetchTrackerTimesOutWhileObservedFetchRemainsActive() throws Exception {
        open("/page-waits");
        TestLens lens = configuredLens(false, true);
        UiTestLensSession session = lens.startSession("network-idle-timeout-" + UUID.randomUUID());
        lens.waitForNetworkIdle(Duration.ZERO, Duration.ofSeconds(1));

        String gateId = UUID.randomUUID().toString();
        ControlledRequest gate = new ControlledRequest();
        assertNull(CONTROLLED_REQUESTS.putIfAbsent(gateId, gate));
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> launchState = (Map<String, Object>) ((JavascriptExecutor) driver).executeScript("""
                    const xhr = new XMLHttpRequest();
                    xhr.open('GET', arguments[0], true);
                    window.__testLensControlledXhr = xhr;
                    xhr.send();

                    const network = window.__uiTestLens
                        && window.__uiTestLens.state
                        && window.__uiTestLens.state.network;
                    return {
                        trackerInstalled: Boolean(network && network.trackerInstalled),
                        activeRequests: Number(network && network.activeRequests)
                    };
                    """, baseUrl + "/wait-controlled/" + gateId);
            assertEquals(Boolean.TRUE, launchState.get("trackerInstalled"),
                    "XHR/fetch tracker was not installed when the request was launched");
            assertEquals(1L, ((Number) launchState.get("activeRequests")).longValue(),
                    "tracker must observe exactly the one controlled active request");

            assertThrows(TimeoutException.class,
                    () -> lens.waitForNetworkIdle(Duration.ofMillis(100), Duration.ofMillis(350)));

            assertEquals(1, session.events().stream()
                    .filter(event -> "page.wait".equals(event.attributes().get("action")))
                    .filter(event -> "350".equals(event.attributes().get("metadata.timeoutMs")))
                    .filter(event -> event.status() == TraceStatus.STARTED).count());
            assertEquals(0, session.events().stream()
                    .filter(event -> "page.wait".equals(event.attributes().get("action")))
                    .filter(event -> "350".equals(event.attributes().get("metadata.timeoutMs")))
                    .filter(event -> event.status() == TraceStatus.PASSED).count());
            assertEquals(1, session.events().stream()
                    .filter(event -> "page.wait".equals(event.attributes().get("action")))
                    .filter(event -> "350".equals(event.attributes().get("metadata.timeoutMs")))
                    .filter(event -> event.status() == TraceStatus.FAILED)
                    .filter(event -> "TIMEOUT".equals(event.attributes().get("metadata.reason"))).count());
            assertEquals(0, session.events().stream()
                    .filter(event -> event.type() == TraceEventType.RETRY).count());
            assertEquals(0, session.retrySummary().totalRetries());
        } finally {
            try {
                ((JavascriptExecutor) driver).executeScript("""
                        if (window.__testLensControlledXhr) {
                            window.__testLensControlledXhr.abort();
                            window.__testLensControlledXhr = null;
                        }
                        return null;
                        """);
            } finally {
                gate.release.countDown();
                try {
                    new WebDriverWait(driver, WAIT).until(ignored -> number("""
                            return window.__uiTestLens.state.network.activeRequests;
                            """) == 0L);
                } finally {
                    try {
                        if (gate.arrived.getCount() == 0L) {
                            assertTrue(gate.completed.await(WAIT.toMillis(), TimeUnit.MILLISECONDS),
                                    "controlled HTTP handler did not finish after release");
                        }
                    } finally {
                        CONTROLLED_REQUESTS.remove(gateId, gate);
                    }
                }
            }
        }
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

    @Test
    void portableFullPageScreenshotsPreserveLayoutContextAndFailureEvidence() throws Exception {
        open("/full-page");
        Path output = Path.of("target", "ui-test-lens", browserName(), "full-page-" + UUID.randomUUID());
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
                .overlayConfig(OverlayConfig.builder().enabled(true).decorationDurationMs(60_000).build())
                .cleanupHudOnFinish(false)
                .failureBundleOptions(FailureBundleOptions.builder()
                        .screenshotCaptureMode(ScreenshotCaptureMode.FULL_PAGE).build())
                .outputRoot(output)
                .build());
        lens.startSession("portable-full-page-" + UUID.randomUUID());
        overlay(true).hudLog("info", "Full-page evidence", "browser-it");
        assertTrue(await(hudPresent()));
        ((JavascriptExecutor) driver).executeScript("""
                const host = document.getElementById('selenium-overlay-host');
                host.style.setProperty('border', '8px solid rgb(255, 0, 255)', 'important');
                """);

        @SuppressWarnings("unchecked")
        Map<String, Number> geometry = (Map<String, Number>) ((JavascriptExecutor) driver).executeScript("""
                return {documentWidth: document.documentElement.scrollWidth,
                  documentHeight: document.documentElement.scrollHeight,
                  viewportWidth: innerWidth, viewportHeight: innerHeight};
                """);
        long initialX = 137;
        long initialY = 311;
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(arguments[0], arguments[1])", initialX, initialY);

        ScreenshotCaptureResult viewport = lens.captureScreenshot("viewport-contract",
                ScreenshotCaptureOptions.builder().outputDirectory(output).includeTimestamp(false).build());
        assertTrue(viewport.isCaptured(), viewport.message());
        assertEquals(ScreenshotCaptureMode.VIEWPORT, viewport.capturedMode());
        assertEquals(1, viewport.tileCount());
        assertTrue(viewport.height() < geometry.get("documentHeight").longValue());

        ScreenshotCaptureOptions fullPage = ScreenshotCaptureOptions.builder()
                .outputDirectory(output)
                .includeTimestamp(false)
                .captureMode(ScreenshotCaptureMode.FULL_PAGE)
                .build();
        ScreenshotCaptureResult captured = lens.captureScreenshot("full-page-contract", fullPage);
        assertTrue(captured.isCaptured(), captured.message());
        assertEquals(ScreenshotCaptureMode.FULL_PAGE, captured.capturedMode());
        assertTrue(captured.tileCount() > 2);
        double scaleX = viewport.width() / geometry.get("viewportWidth").doubleValue();
        double scaleY = viewport.height() / geometry.get("viewportHeight").doubleValue();
        assertEquals(Math.round(geometry.get("documentWidth").doubleValue() * scaleX), captured.width());
        assertEquals(Math.round(geometry.get("documentHeight").doubleValue() * scaleY), captured.height());
        BufferedImage image = ImageIO.read(captured.path().toFile());
        assertTrue(containsRgb(image, 220, 40, 40), "top marker must be present");
        assertTrue(containsRgb(image, 40, 180, 70), "middle marker must be present");
        assertTrue(containsRgb(image, 35, 80, 220), "bottom marker must be present");
        assertTrue(containsRgb(image, 255, 165, 0), "right-side marker must be present");
        assertTrue(containsRgb(image, 0, 220, 220), "sticky marker must remain present");
        assertTrue(countRgb(image, 255, 0, 255) > 0, "HUD must appear in diagnostic capture");
        assertNoTransparentRow(image);
        assertEquals(initialX, number("return Math.round(window.scrollX)"));
        assertEquals(initialY, number("return Math.round(window.scrollY)"));

        String window = driver.getWindowHandle();
        driver.switchTo().frame(driver.findElement(By.id("full-page-frame")));
        ScreenshotCaptureResult framed = lens.captureScreenshot("frame-full-page", fullPage);
        assertEquals(io.github.testlens.selenium.evidence.ScreenshotCaptureStatus.SKIPPED, framed.status());
        assertEquals(window, driver.getWindowHandle());
        assertTrue(driver.findElement(By.id("frame-value")).isDisplayed(), "capture must preserve frame context");
        driver.switchTo().defaultContent();

        ScreenshotCaptureResult limited = lens.captureScreenshot("limited-full-page",
                ScreenshotCaptureOptions.builder().outputDirectory(output).includeTimestamp(false)
                        .captureMode(ScreenshotCaptureMode.FULL_PAGE).maxPixelCount(1).build());
        assertEquals(io.github.testlens.selenium.evidence.ScreenshotCaptureStatus.FAILED, limited.status());
        assertFalse(Files.exists(output.resolve("screenshot_limited-full-page.png")));

        TestLensFinalizationResult result = lens.finishFailed(new AssertionError("controlled failure"));
        BufferedImage diagnostic = ImageIO.read(result.failureScreenshot().toFile());
        BufferedImage clean = ImageIO.read(result.failureBundleDirectory().orElseThrow()
                .resolve("failure-clean.png").toFile());
        assertTrue(diagnostic.getHeight() > viewport.height());
        assertEquals(diagnostic.getWidth(), clean.getWidth());
        assertEquals(diagnostic.getHeight(), clean.getHeight());
        assertTrue(countRgb(diagnostic, 255, 0, 255) > 0);
        assertEquals(0, countRgb(clean, 255, 0, 255));
        assertTrue(Files.readString(result.failureBundleManifest().orElseThrow()).contains("FULL_PAGE"));
        assertTrue(hudPresent().apply(driver), "clean capture must restore the HUD");
        assertFalse(driver.getTitle().isBlank(), "capture and finalization must leave the driver active");
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
        return createDriver(PageLoadStrategy.NORMAL);
    }

    private static WebDriver createDriver(PageLoadStrategy pageLoadStrategy) {
        boolean headed = Boolean.parseBoolean(System.getProperty("headed", "false"));
        return switch (browserName()) {
            case "chrome" -> {
                ChromeOptions options = new ChromeOptions();
                options.setPageLoadStrategy(pageLoadStrategy);
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
                options.setPageLoadStrategy(pageLoadStrategy);
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
        if (path.startsWith("/wait-controlled/")) {
            serveControlledRequest(exchange, path.substring("/wait-controlled/".length()));
            return;
        }
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
            case "/composite-locators" -> html(exchange, page("Composite locators", """
                    <button id='global-before-buy' class='global-buy'>Buy</button>
                    <section class='product-card' id='laptop-sold' data-status='sold'>
                      <h2>Laptop Basic</h2><span class='price'>49</span><span class='status'>Sold</span>
                    </section>
                    <section class='product-card' id='laptop-available' data-status='available'>
                      <h2>Laptop Pro</h2><span class='price'>99</span><span class='status'>Available</span>
                      <button id='laptop-available-buy'>Buy</button>
                    </section>
                    <section class='product-card' id='phone-available' data-status='available'>
                      <h2>Phone</h2><span class='price'>59</span><span class='status'>Available</span>
                      <button id='phone-available-buy'>Buy</button>
                    </section>
                    <button id='global-after-buy' class='global-buy'>Buy</button>
                    <button id='start-dynamic'>Start changes</button>
                    <div id='dynamic-list'><div class='dynamic-item' data-phase='initial'>Initial</div></div>
                    """), false);
            case "/assertion-states" -> html(exchange, page("Locator state assertions", """
                    <button id='start-state-changes'>Start state changes</button>
                    <div id='state-list'><div class='state-item' data-status='pending'>Pending</div></div>
                    <div id='busy-control' aria-busy='true'></div>
                    <div id='class-control' class='control waiting'></div>
                    <div id='css-control' style='display: block'></div>
                    <select><option id='native-option' selected>Chosen</option></select>
                    <input id='native-checked' type='checkbox' checked>
                    <label id='styled-label'><input id='styled-checked' type='checkbox' checked hidden><span class='decoration'>Styled</span></label>
                    <button id='aria-checkbox' role='checkbox' aria-checked='true'>ARIA checkbox</button>
                    <button id='aria-switch' role='switch' aria-checked='false'>ARIA switch</button>
                    <div id='aria-option' role='option' aria-selected='true'>ARIA option</div>
                    <button id='start-attachment-changes'>Start attachment changes</button>
                    <div id='will-detach'>Remove me</div>
                    <div id='replacement-target' data-version='old'>Old</div>
                    """), false);
            case "/page-expectations" -> html(exchange, page("Loading Dashboard", """
                    <p id='page-state'>Loading</p>
                    """), false);
            case "/page-waits", "/page-waits-next" -> html(exchange, page("Page waits", """
                    <p id='wait-state'>Ready for observed requests</p>
                    """), false);
            case "/full-page" -> html(exchange, page("Full-page screenshot", """
                    <div id='full-page-document'>
                      <header id='full-page-fixed'>Fixed header</header>
                      <section id='full-page-top'><span class='marker'>Top</span></section>
                      <section id='full-page-middle'><aside id='full-page-sticky'>Sticky</aside><span class='marker'>Middle</span></section>
                      <section id='full-page-bottom'><span class='marker'>Bottom</span></section>
                      <div id='full-page-right'>Right</div>
                      <iframe id='full-page-frame' src='/frame'></iframe>
                    </div>
                    """), true);
            case "/wait-delayed" -> {
                java.util.concurrent.CompletableFuture.runAsync(
                        () -> { }, java.util.concurrent.CompletableFuture.delayedExecutor(250,
                                java.util.concurrent.TimeUnit.MILLISECONDS)).join();
                response(exchange, "text/plain; charset=utf-8", "done", false);
            }
            case "/app.js" -> response(exchange, "application/javascript; charset=utf-8", APP_JS, false);
            case "/app.css" -> response(exchange, "text/css; charset=utf-8", APP_CSS, false);
            default -> response(exchange, "text/plain; charset=utf-8", "not found", false, 404);
        }
    }

    private static void serveControlledRequest(HttpExchange exchange, String gateId) throws IOException {
        ControlledRequest gate = CONTROLLED_REQUESTS.get(gateId);
        if (gate == null) {
            response(exchange, "text/plain; charset=utf-8", "unknown gate", false, 404);
            return;
        }
        gate.arrived.countDown();
        try {
            boolean released = gate.release.await(10, TimeUnit.SECONDS);
            response(exchange, "text/plain; charset=utf-8", released ? "released" : "gate timeout",
                    false, released ? 200 : 504);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            response(exchange, "text/plain; charset=utf-8", "interrupted", false, 503);
        } finally {
            gate.completed.countDown();
        }
    }

    private static final class ControlledRequest {
        private final CountDownLatch arrived = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final CountDownLatch completed = new CountDownLatch(1);
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
            document.querySelectorAll('.product-card button').forEach(button => button.addEventListener('click', () => {
              window.buttonClicks = window.buttonClicks || {};
              window.buttonClicks[button.id] = (window.buttonClicks[button.id] || 0) + 1;
              window.cardBuyClicks = (window.cardBuyClicks || 0) + 1;
              window.lastCardBuyId = button.id;
            }));
            document.querySelectorAll('.global-buy').forEach(button => button.addEventListener('click', () => {
              window.buttonClicks = window.buttonClicks || {};
              window.buttonClicks[button.id] = (window.buttonClicks[button.id] || 0) + 1;
              window.globalBuyClicks = (window.globalBuyClicks || 0) + 1;
            }));
            const startDynamic = document.getElementById('start-dynamic');
            if (startDynamic) startDynamic.addEventListener('click', () => {
              const list = document.getElementById('dynamic-list');
              setTimeout(() => {
                list.innerHTML = '<div class="dynamic-item" data-phase="grown">One</div>'
                  + '<div class="dynamic-item" data-phase="grown">Two</div>'
                  + '<div class="dynamic-item" data-phase="grown">Three</div>';
              }, 100);
              setTimeout(() => {
                list.innerHTML = '<div class="dynamic-item" data-phase="final">Final</div>';
              }, 800);
            });
            window.assertionClicks = 0;
            window.assertionChanges = 0;
            document.querySelectorAll('#native-checked, #styled-checked, #aria-checkbox, #aria-switch').forEach(control => {
              control.addEventListener('click', () => window.assertionClicks += 1);
              control.addEventListener('change', () => window.assertionChanges += 1);
            });
            const startStateChanges = document.getElementById('start-state-changes');
            if (startStateChanges) startStateChanges.addEventListener('click', () => {
              setTimeout(() => {
                document.getElementById('state-list').innerHTML =
                  '<div class="state-item" data-status="available">One</div>'
                  + '<div class="state-item" data-status="available">Two</div>'
                  + '<div class="state-item" data-status="available">Three</div>';
                document.getElementById('busy-control').setAttribute('aria-busy', 'false');
                document.getElementById('class-control').className = 'control ready';
              }, 150);
            });
            const startAttachmentChanges = document.getElementById('start-attachment-changes');
            if (startAttachmentChanges) startAttachmentChanges.addEventListener('click', () => {
              setTimeout(() => {
                const added = document.createElement('div');
                added.id = 'late-attached';
                added.textContent = 'Added';
                document.body.appendChild(added);
              }, 100);
              setTimeout(() => document.getElementById('will-detach')?.remove(), 250);
              setTimeout(() => {
                const old = document.getElementById('replacement-target');
                const replacement = old.cloneNode(true);
                replacement.setAttribute('data-version', 'new');
                replacement.textContent = 'New';
                old.replaceWith(replacement);
              }, 350);
            });
            const pageState = document.getElementById('page-state');
            if (pageState) setTimeout(() => {
              history.pushState({}, '', '/page-expectations/dashboard?token=browser-secret#private-fragment');
              document.title = 'Ready Dashboard';
              pageState.textContent = 'Ready';
            }, 150);
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
            #full-page-document { position: relative; width: 1800px; }
            #full-page-document section { height: 720px; }
            #full-page-top { background: rgb(220, 40, 40); }
            #full-page-middle { background: rgb(40, 180, 70); }
            #full-page-bottom { background: rgb(35, 80, 220); }
            #full-page-fixed { position: fixed; left: 10px; top: 10px; width: 220px; height: 44px;
              z-index: 8; background: rgb(230, 230, 0); }
            #full-page-sticky { position: sticky; top: 80px; width: 180px; height: 48px; background: rgb(0, 220, 220); }
            #full-page-right { position: absolute; left: 1680px; top: 980px; width: 100px; height: 100px;
              background: rgb(255, 165, 0); }
            """;

    private static boolean containsRgb(BufferedImage image, int red, int green, int blue) {
        return countRgb(image, red, green, blue) > 0;
    }

    private static long countRgb(BufferedImage image, int red, int green, int blue) {
        long count = 0;
        int expected = (red << 16) | (green << 8) | blue;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0x00ffffff) == expected) count++;
            }
        }
        return count;
    }

    private static void assertNoTransparentRow(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            boolean opaque = false;
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xff) != 0) { opaque = true; break; }
            }
            assertTrue(opaque, "stitched PNG contains an empty row at y=" + y);
        }
    }
}
