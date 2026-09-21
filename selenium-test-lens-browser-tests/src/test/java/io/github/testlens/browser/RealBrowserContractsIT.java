package io.github.testlens.browser;

import consumer.pages.ConsumerSourcePage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.testlens.JsOverlayDebug;
import io.github.testlens.HighlightOptions;
import io.github.testlens.HighlightState;
import io.github.testlens.OverlayConfig;
import io.github.testlens.TestLens;
import io.github.testlens.TestLensFinalizationResult;
import io.github.testlens.TestLensOptions;
import io.github.testlens.hud.HudOptions;
import io.github.testlens.hud.HudFontPreset;
import io.github.testlens.hud.HudHeaderLayout;
import io.github.testlens.hud.HudPosition;
import io.github.testlens.hud.HudPreset;
import io.github.testlens.hud.HudScrollbarStyle;
import io.github.testlens.hud.HudTypography;
import io.github.testlens.hud.HudTimestampFormat;
import io.github.testlens.hud.SourceIde;
import io.github.testlens.hud.SourceNavigationOptions;
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
import io.github.testlens.selenium.evidence.VisualMaskMode;
import io.github.testlens.selenium.evidence.VisualRedactionFailurePolicy;
import io.github.testlens.selenium.evidence.VisualRedactionOptions;
import io.github.testlens.selenium.locator.UiLocatorException;
import io.github.testlens.allure.AllureAttachStatus;
import io.github.testlens.allure.AllureTestLens;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.FileSystemResultsWriter;
import io.qameta.allure.model.TestResult;
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
import org.openqa.selenium.json.Json;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Duration;
import java.time.ZoneId;
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
import static org.junit.jupiter.api.Assertions.fail;

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

    @Test
    @SuppressWarnings("unchecked")
    void hudTimestampContractIsRenderedInTheLocalFixtureDom() {
        open("/clicks");
        HudOptions hudOptions = HudOptions.builder()
                .showTimestamps(true)
                .timestampPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS XXX")
                .timestampZone(ZoneId.of("Europe/Warsaw"))
                .typography(io.github.testlens.hud.HudTypography.builder().timestampFontSizePx(8).build())
                .build();
        JsOverlayDebug overlay = new JsOverlayDebug(driver, OverlayConfig.builder()
                .hudOptions(hudOptions).build());
        overlay.initHud("timestamp contract", "local");
        overlay.hudLog("info", "winter", "2026-01-15T22:59:59Z");
        overlay.hudLog("info", "summer midnight", "2026-07-15T22:00:00Z");
        overlay.hudLog("warn", "placeholder", "ui-test-lens");

        List<String> javaRows = (List<String>) ((JavascriptExecutor) driver).executeScript("""
                return Array.from(window.__seleniumOverlayRoot.querySelectorAll('#selenium-hud-logs > div'))
                  .map(row => row.textContent);
                """);
        assertEquals("[2026-01-15 23:59:59.000000000 +01:00][INFO] winter", javaRows.get(javaRows.size() - 3));
        assertEquals("[2026-07-16 00:00:00.000000000 +02:00][INFO] summer midnight", javaRows.get(javaRows.size() - 2));
        String fallback = javaRows.get(javaRows.size() - 1);
        assertTrue(fallback.matches("\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{9} [+-]\\d{2}:\\d{2}]\\[WARN] placeholder"), fallback);
        assertFalse(fallback.contains("ui-test-lens"));
        assertEquals("8px", ((JavascriptExecutor) driver).executeScript("""
                return getComputedStyle(document.getElementById('selenium-overlay-host').shadowRoot
                  .querySelector('.stl-hud-timestamp')).fontSize;
                """));

        Map<String, Object> direct = (Map<String, Object>) ((JavascriptExecutor) driver).executeScript("""
                const hud = window.__uiTestLens.modules.hud;
                function render(format, zone, shown, timestamp, message, presentation) {
                  hud.init({testName:'direct runtime',theme:{},hudOptions:{showEventLog:true,
                    showTimestamps:shown,timestampFormat:format,timestampZone:zone,branding:'NONE'}});
                  hud.clear();
                  hud.log(message,'info',timestamp,'GENERAL',null,null,presentation);
                  const row=window.__seleniumOverlayRoot.querySelector('#selenium-hud-logs > div');
                  return {text:row.textContent,stored:row.getAttribute('data-test-lens-timestamp')};
                }
                return {
                  iso:render('ISO_UTC','Europe/Warsaw',true,'2026-01-15T22:59:59Z','iso','2026-01-15T22:59:59.000Z'),
                  utc:render('TIME_ONLY','UTC',true,'2026-01-15T22:59:59Z','utc','22:59:59'),
                  hidden:render('DATE_TIME','Europe/Warsaw',false,null,'hidden')
                };
                """);
        assertEquals("[2026-01-15T22:59:59.000Z][INFO] iso", ((Map<?, ?>) direct.get("iso")).get("text"));
        assertEquals("[22:59:59][INFO] utc", ((Map<?, ?>) direct.get("utc")).get("text"));
        assertEquals("[INFO] hidden", ((Map<?, ?>) direct.get("hidden")).get("text"));
        assertTrue(String.valueOf(((Map<?, ?>) direct.get("hidden")).get("stored")).matches("\\d{4}-.*Z"));
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
    void xhrFetchTrackerWaitsForCompletionAndIsReinstalledAfterNavigation() throws Exception {
        open("/page-waits");
        TestLens lens = configuredLens(true, true);
        lens.startSession("network-idle-wait-" + UUID.randomUUID());

        ((JavascriptExecutor) driver).executeScript("""
                const delegate = window.fetch;
                window.__testLensApplicationFetchCalls = 0;
                window.__testLensApplicationFetchReceiverWasWindow = null;
                window.fetch = function() {
                    window.__testLensApplicationFetchCalls++;
                    window.__testLensApplicationFetchReceiverWasWindow = this === window;
                    return delegate.apply(window, arguments);
                };
                """);
        lens.waitForNetworkIdle(Duration.ZERO, Duration.ofSeconds(1));
        assertControlledFetchCompletesAfterRelease(lens);
        assertEquals(1L, number("return window.__testLensApplicationFetchCalls;"));
        assertEquals(Boolean.TRUE, ((JavascriptExecutor) driver).executeScript("""
                return window.__testLensApplicationFetchReceiverWasWindow;
                """));

        driver.navigate().to(baseUrl + "/page-waits-next");
        assertFalse(Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript("""
                const network = window.__uiTestLens
                    && window.__uiTestLens.state
                    && window.__uiTestLens.state.network;
                return Boolean(network && network.trackerInstalled);
                """)), "a new document must not inherit the previous document's tracker");
        lens.waitForNetworkIdle(Duration.ZERO, Duration.ofSeconds(1));
        assertControlledFetchCompletesAfterRelease(lens);
        lens.finishPassed();
    }

    @Test
    void xhrFetchTrackerPropagatesFetchAbortAndBalancesCounter() throws Exception {
        open("/page-waits");
        TestLens lens = configuredLens(false, true);
        lens.startSession("network-idle-fetch-abort-" + UUID.randomUUID());
        lens.waitForNetworkIdle(Duration.ZERO, Duration.ofSeconds(1));

        String gateId = UUID.randomUUID().toString();
        ControlledRequest gate = new ControlledRequest();
        assertNull(CONTROLLED_REQUESTS.putIfAbsent(gateId, gate));
        try {
            assertControlledFetchArmed(gateId);
            driver.findElement(By.id("controlled-fetch-trigger")).click();
            assertTrue(gate.arrived.await(WAIT.toMillis(), TimeUnit.MILLISECONDS),
                    () -> "controlled fetch did not reach the local HTTP handler; browser state="
                            + controlledFetchDiagnostic());
            assertControlledFetchActive();

            ((JavascriptExecutor) driver).executeScript("""
                    window.__testLensControlledFetch.abort();
                    return null;
                    """);
            new WebDriverWait(driver, WAIT).until(ignored -> {
                Map<String, Object> state = controlledFetchState();
                return "rejected".equals(state.get("fetchStatus"))
                        && ((Number) state.get("activeRequests")).longValue() == 0L;
            });

            Map<String, Object> rejectedState = controlledFetchState();
            assertEquals("rejected", rejectedState.get("fetchStatus"));
            assertEquals("AbortError", rejectedState.get("rejectionName"));
            assertEquals(0L, ((Number) rejectedState.get("activeRequests")).longValue());
            lens.waitForNetworkIdle(Duration.ZERO, Duration.ofSeconds(1));
        } finally {
            gate.release.countDown();
            if (gate.arrived.getCount() == 0L) {
                assertTrue(gate.completed.await(WAIT.toMillis(), TimeUnit.MILLISECONDS),
                        "controlled HTTP handler did not finish after fetch abort");
            }
            CONTROLLED_REQUESTS.remove(gateId, gate);
        }
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
    void typedHighlightUsesConfiguredStateAndOperationScopedLifecycle() {
        open("/clicks");
        HighlightOptions highlights = HighlightOptions.builder().actionColor("#123456")
                .waitingColor("#2468ac").retryColor("#c47a00").successColor("#16803a")
                .failureColor("#b91c1c").durationMs(10000).borderWidthPx(5).showLabels(true).build();
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder().highlights(highlights).build());
        WebElement button = driver.findElement(By.id("count-button"));

        lens.highlight(button, "Count control", HighlightState.SUCCESS);
        assertTrue(await(scriptBoolean("""
                const mark=document.getElementById('selenium-overlay-host').shadowRoot
                    .querySelector('[data-uitestlens-highlight="1"]');
                return mark && mark.dataset.uitestlensHighlightState==='success'
                    && getComputedStyle(mark).borderColor==='rgb(22, 128, 58)'
                    && getComputedStyle(mark).borderWidth==='5px'
                    && mark.textContent==='Count control';
                """)));

        lens.locator(By.id("count-button"), "UiExpect control").expect().toBeVisible();
        assertTrue(scriptBoolean("""
                const mark=document.getElementById('selenium-overlay-host').shadowRoot
                    .querySelector('[data-uitestlens-highlight="1"]');
                return mark && mark.dataset.uitestlensHighlightState==='success'
                    && getComputedStyle(mark).borderColor==='rgb(22, 128, 58)'
                    && mark.textContent.includes('UiExpect control');
                """).apply(driver), () -> "UiExpect highlight DOM: " + ((JavascriptExecutor) driver).executeScript("""
                return Array.from(document.getElementById('selenium-overlay-host').shadowRoot
                    .querySelectorAll('[data-uitestlens-highlight="1"]')).map(mark => ({
                        state: mark.dataset.uitestlensHighlightState,
                        color: getComputedStyle(mark).borderColor,
                        label: mark.textContent
                    }));
                """));

        JsOverlayDebug legacy = new JsOverlayDebug(driver, OverlayConfig.builder()
                .highlightOptions(highlights).build());
        assertTrue(legacy.assertVisible(button, true, "Legacy assertion control"));
        assertTrue(await(scriptBoolean("""
                const mark=document.getElementById('selenium-overlay-host').shadowRoot
                    .querySelector('[data-uitestlens-highlight="1"]');
                return mark && mark.dataset.uitestlensHighlightState==='success'
                    && getComputedStyle(mark).borderColor==='rgb(22, 128, 58)'
                    && mark.textContent.includes('Legacy assertion control');
                """)));

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.__highlightTimerCheck=performance.now()");
        js.executeScript("""
                window.__uiTestLens.modules.highlight.element(arguments[0], 'OLD',
                    {duration:80,color:'#ef4444',borderWidth:2,state:'failure'});
                """, button);
        js.executeScript("""
                window.__uiTestLens.modules.highlight.element(arguments[0], 'NEW',
                    {duration:1000,color:'#22c55e',borderWidth:3,state:'success'});
                """, button);
        assertTrue(await(scriptBoolean("""
                const mark=document.getElementById('selenium-overlay-host').shadowRoot
                    .querySelector('[data-uitestlens-highlight="1"]');
                return performance.now()-window.__highlightTimerCheck>180 && mark
                    && mark.textContent==='NEW' && mark.dataset.uitestlensHighlightState==='success';
                """)));
        js.executeScript("window.__uiTestLens.modules.highlight.clear()");

        WebElement shadowButton = (WebElement) js.executeScript("""
                const host=document.createElement('div');document.body.appendChild(host);
                const root=host.attachShadow({mode:'open'});const button=document.createElement('button');
                button.textContent='Shadow action';root.appendChild(button);return button;
                """);
        lens.highlight(shadowButton, "Shadow control", HighlightState.RETRY);
        assertTrue(await(scriptBoolean("""
                const mark=document.getElementById('selenium-overlay-host').shadowRoot
                    .querySelector('[data-uitestlens-highlight="1"]');
                return mark && mark.dataset.uitestlensHighlightState==='retry'
                    && getComputedStyle(mark).borderColor==='rgb(196, 122, 0)';
                """)));
        js.executeScript("arguments[0].remove()", shadowButton);
        js.executeScript("window.dispatchEvent(new Event('resize'))");
        assertTrue(await(scriptBoolean("""
                return !document.getElementById('selenium-overlay-host').shadowRoot
                    .querySelector('[data-uitestlens-highlight="1"]');
                """)));
    }

    @Test
    void highlightSwitchesStayIndependentFromHudAndOverlayMasterSwitch() {
        open("/clicks");
        HighlightOptions manualOnly = HighlightOptions.builder()
                .automaticFeedback(false).durationMs(1000).build();
        TestLens hudOff = TestLens.attach(driver, TestLensOptions.builder()
                .overlayConfig(OverlayConfig.builder().showHudPanel(false).build())
                .highlights(manualOnly).build());
        WebElement button = driver.findElement(By.id("count-button"));

        hudOff.highlight(button, "Manual while HUD is off", HighlightState.ACTION);
        assertTrue(await(scriptBoolean("""
                const host=document.getElementById('selenium-overlay-host');
                return host && host.shadowRoot.querySelector('[data-uitestlens-highlight="1"]')
                    && !host.shadowRoot.querySelector('#selenium-hud-panel');
                """)));
        ((JavascriptExecutor) driver).executeScript("window.__uiTestLens.modules.highlight.clear()");

        hudOff.locator(By.id("count-button"), "Automatic disabled").click();
        assertClickCounts(1);
        assertFalse(scriptBoolean("""
                const host=document.getElementById('selenium-overlay-host');
                return !!(host && host.shadowRoot.querySelector('[data-uitestlens-highlight="1"]'));
                """).apply(driver));

        TestLens highlightsOff = TestLens.attach(driver, TestLensOptions.builder()
                .highlights(HighlightOptions.builder().enabled(false).build()).build());
        highlightsOff.highlight(button, "Disabled highlight");
        assertFalse(scriptBoolean("""
                const host=document.getElementById('selenium-overlay-host');
                return !!(host && host.shadowRoot.querySelector('[data-uitestlens-highlight="1"]'));
                """).apply(driver));

        TestLens overlayOff = TestLens.attach(driver, TestLensOptions.builder()
                .overlayConfig(OverlayConfig.builder().enabled(false).build())
                .highlights(HighlightOptions.defaults()).build());
        overlayOff.highlight(button, "Master disabled");
        assertFalse(scriptBoolean("""
                const host=document.getElementById('selenium-overlay-host');
                return !!(host && host.shadowRoot.querySelector('[data-uitestlens-highlight="1"]'));
                """).apply(driver));
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

    @Test
    void hudSourceNavigationIsModifierScopedPassiveAndLifecycleSafe() {
        open("/clicks");
        HudOptions hud = HudOptions.builder().showTimestamps(true).timestampPattern("HH:mm:ss.SSS")
                .timestampZone(java.time.ZoneOffset.UTC).sourceNavigation(SourceNavigationOptions.builder()
                .enabled(true).ide(SourceIde.INTELLIJ).build()).build();
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder().hud(hud).build());
        lens.startSession("source-navigation");
        ConsumerSourcePage.clickCounter(lens);

        assertTrue(await(scriptBoolean("""
                const root=document.getElementById('selenium-overlay-host').shadowRoot;
                return !!root.querySelector('.stl-hud-source-location');
                """)));
        @SuppressWarnings("unchecked")
        Map<String, Object> passive = (Map<String, Object>) ((JavascriptExecutor) driver).executeScript("""
                const root=document.getElementById('selenium-overlay-host').shadowRoot;
                const panel=root.querySelector('#selenium-hud-panel');
                const link=root.querySelector('.stl-hud-source-location');
                const timestamp=link.closest('[data-test-lens-timestamp]').querySelector('.stl-hud-timestamp');
                return {label:link.textContent, panelPointer:getComputedStyle(panel).pointerEvents,
                  linkPointer:getComputedStyle(link).pointerEvents, display:getComputedStyle(link).display,
                  active:panel.dataset.sourceNavigationActive,timestamp:timestamp.textContent,
                  eventTime:timestamp.closest('[data-test-lens-timestamp]').dataset.testLensTimestamp,
                  timestampRole:timestamp.getAttribute('role'),timestampTabIndex:timestamp.getAttribute('tabindex')};
                """);
        assertTrue(passive.get("label").toString().matches("ConsumerSourcePage\\.java:\\d+"), passive.toString());
        assertEquals("none", passive.get("panelPointer"));
        assertEquals("none", passive.get("linkPointer"));
        assertEquals("none", passive.get("display"));
        assertEquals(null, passive.get("timestampRole"));
        assertEquals(null, passive.get("timestampTabIndex"));
        assertTrue(scriptBoolean("""
                const root=document.getElementById('selenium-overlay-host').shadowRoot;
                const link=root.querySelector('.stl-hud-source-location');
                const attributes=Array.from(link.attributes).map(attribute=>attribute.value).join(' ');
                return !root.innerHTML.includes('idea://') && !root.innerHTML.includes('vscode://')
                  && !root.innerHTML.includes('Java%20Projects') && !root.innerHTML.includes('Java Projects')
                  && !attributes.includes('idea://') && !attributes.includes('vscode://')
                  && !link.hasAttribute('href');
                """).apply(driver), "absolute source path or IDE URI leaked into Shadow DOM");

        ((JavascriptExecutor) driver).executeScript("""
                window.__uiTestLensSourceTargets=[];
                window.__uiTestLensSourceNavigation=target=>window.__uiTestLensSourceTargets.push(target);
                window.dispatchEvent(new KeyboardEvent('keydown',{key:'Control',ctrlKey:true}));
                window.dispatchEvent(new KeyboardEvent('keydown',{key:'Alt',ctrlKey:true,altKey:true}));
                window.dispatchEvent(new KeyboardEvent('keydown',{key:'Shift',ctrlKey:true,altKey:true,shiftKey:true}));
                window.dispatchEvent(new KeyboardEvent('keyup',{key:'Shift',ctrlKey:true,altKey:true,shiftKey:false}));
                """);
        @SuppressWarnings("unchecked")
        Map<String, Object> active = (Map<String, Object>) ((JavascriptExecutor) driver).executeScript("""
                const root=document.getElementById('selenium-overlay-host').shadowRoot;
                const panel=root.querySelector('#selenium-hud-panel');
                const link=root.querySelector('.stl-hud-source-location[data-navigable="true"]');
                link.click();
                return {panelPointer:getComputedStyle(panel).pointerEvents,
                  linkPointer:getComputedStyle(link).pointerEvents, display:getComputedStyle(link).display,
                  active:panel.dataset.sourceNavigationActive,status:getComputedStyle(root.querySelector('.stl-hud-source-status')).display,
                  timestamp:link.closest('[data-test-lens-timestamp]').querySelector('.stl-hud-timestamp').textContent,
                  eventTime:link.closest('[data-test-lens-timestamp]').dataset.testLensTimestamp,
                  target:window.__uiTestLensSourceTargets[0]};
                """);
        assertEquals("none", active.get("panelPointer"));
        assertEquals("auto", active.get("linkPointer"));
        assertEquals("block", active.get("display"));
        assertEquals("true", active.get("active"));
        assertEquals("block", active.get("status"));
        assertEquals(passive.get("timestamp"), active.get("timestamp"));
        assertEquals(passive.get("eventTime"), active.get("eventTime"));
        String navigationTarget = active.get("target").toString();
        assertTrue(navigationTarget.startsWith("idea://open?file="), navigationTarget);
        assertTrue(navigationTarget.contains("ConsumerSourcePage.java"), navigationTarget);

        WebElement besideHud = (WebElement) ((JavascriptExecutor) driver).executeScript("""
                const panel=document.getElementById('selenium-overlay-host').shadowRoot.querySelector('#selenium-hud-panel');
                const rect=panel.getBoundingClientRect();
                const button=document.createElement('button');
                button.id='hud-click-through'; button.textContent='under HUD';
                button.style.cssText=`position:fixed;left:${rect.left+4}px;top:${rect.top+4}px;width:24px;height:24px;z-index:2147483646`;
                button.addEventListener('click',()=>button.dataset.clicks=String(Number(button.dataset.clicks||0)+1));
                document.body.appendChild(button); return button;
                """);
        besideHud.click();
        assertEquals("1", besideHud.getAttribute("data-clicks"), "active HUD intercepted click beside source link");

        ((JavascriptExecutor) driver).executeScript("""
                const root=document.getElementById('selenium-overlay-host').shadowRoot;
                const rect=root.querySelector('.stl-hud-source-location[data-navigable="true"]').getBoundingClientRect();
                const button=document.createElement('button');
                button.id='under-source-link'; button.textContent='under source';
                button.style.cssText=`position:fixed;left:${rect.left}px;top:${rect.top}px;width:${Math.max(1,rect.width)}px;height:${Math.max(1,rect.height)}px;z-index:2147483646`;
                button.addEventListener('click',()=>button.dataset.clicks=String(Number(button.dataset.clicks||0)+1));
                document.body.appendChild(button);
                """);

        ((JavascriptExecutor) driver).executeScript(
                "window.dispatchEvent(new KeyboardEvent('keyup',{key:'Alt',ctrlKey:true,altKey:false}));");
        assertTrue(scriptBoolean("""
                const p=document.getElementById('selenium-overlay-host').shadowRoot.querySelector('#selenium-hud-panel');
                return p.dataset.sourceNavigationActive==='false' && getComputedStyle(p.querySelector('.stl-hud-source-location')).display==='none';
                """).apply(driver));
        WebElement underSource = driver.findElement(By.id("under-source-link"));
        underSource.click();
        assertEquals("1", underSource.getAttribute("data-clicks"), "released source link remained a click target");

        ((JavascriptExecutor) driver).executeScript("""
                const altGraph=new KeyboardEvent('keydown',{key:'AltGraph',code:'AltRight',ctrlKey:true,altKey:true});
                Object.defineProperty(altGraph,'getModifierState',{value:name=>name==='AltGraph'});
                window.dispatchEvent(altGraph);
                """);
        assertTrue(scriptBoolean("""
                const root=document.getElementById('selenium-overlay-host').shadowRoot;
                const panel=root.querySelector('#selenium-hud-panel');
                const link=root.querySelector('.stl-hud-source-location');
                return panel.dataset.sourceNavigationActive==='false' && getComputedStyle(link).pointerEvents==='none';
                """).apply(driver), "AltGraph activated source navigation");
        ((JavascriptExecutor) driver).executeScript("""
                window.dispatchEvent(new KeyboardEvent('keydown',{key:'Control',ctrlKey:true}));
                window.dispatchEvent(new KeyboardEvent('keydown',{key:'Alt',ctrlKey:true,altKey:true}));
                window.dispatchEvent(new Event('blur'));
                """);
        assertTrue(scriptBoolean("""
                return document.getElementById('selenium-overlay-host').shadowRoot
                  .querySelector('#selenium-hud-panel').dataset.sourceNavigationActive==='false';
                """).apply(driver));

        lens.finishPassed();
        TestLens next = TestLens.attach(driver, TestLensOptions.builder().hud(hud).build());
        next.startSession("source-navigation-next-session");
        ConsumerSourcePage.clickCounter(next);
        ((JavascriptExecutor) driver).executeScript("""
                window.__uiTestLensSourceTargets=[];
                window.__uiTestLensSourceNavigation=target=>window.__uiTestLensSourceTargets.push(target);
                window.dispatchEvent(new KeyboardEvent('keydown',{key:'Control',ctrlKey:true}));
                window.dispatchEvent(new KeyboardEvent('keydown',{key:'Alt',ctrlKey:true,altKey:true}));
                document.getElementById('selenium-overlay-host').shadowRoot
                  .querySelector('.stl-hud-source-location[data-navigable="true"]').click();
                """);
        assertEquals(1L, ((Number) ((JavascriptExecutor) driver).executeScript(
                "return window.__uiTestLensSourceTargets.length")).longValue());
        next.finishPassed();
    }

    @Test
    void disabledSourceNavigationInstallsNoKeyboardListeners() {
        open("/clicks");
        ((JavascriptExecutor) driver).executeScript("""
                window.__sourceNavigationListenerTypes=[];
                window.__originalAddEventListener=window.addEventListener;
                window.addEventListener=function(type,listener,options){
                  if(type==='keydown'||type==='keyup'||type==='blur') window.__sourceNavigationListenerTypes.push(type);
                  return window.__originalAddEventListener.call(window,type,listener,options);
                };
                """);
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder().hud(HudOptions.defaults()).build());
        lens.startSession("source-navigation-disabled");
        ConsumerSourcePage.clickCounter(lens);

        @SuppressWarnings("unchecked")
        List<String> listenerTypes = (List<String>) ((JavascriptExecutor) driver).executeScript("""
                window.addEventListener=window.__originalAddEventListener;
                return window.__sourceNavigationListenerTypes;
                """);
        assertEquals(List.of(), listenerTypes);
        lens.finishPassed();
    }

    @Test
    void configuredHudIsClampedInsideEveryViewportCorner() {
        for (org.openqa.selenium.Dimension size : List.of(
                new org.openqa.selenium.Dimension(1440, 900),
                new org.openqa.selenium.Dimension(1024, 768),
                new org.openqa.selenium.Dimension(768, 700),
                new org.openqa.selenium.Dimension(390, 844))) {
            driver = createDriver();
            try {
                driver.manage().window().setSize(size);
                for (HudPosition position : HudPosition.values()) {
                    open("/clicks");
                    HudOptions hud = HudOptions.builder().position(position).widthPx(620)
                            .maxHeightPx(500).maxLogHeightPx(180).offsetXPx(40).offsetYPx(40).build();
                    TestLens lens = TestLens.attach(driver, TestLensOptions.builder().hud(hud).build());
                    lens.startSession("responsive-" + size.getWidth() + "-" + position);
                    assertTrue(await(hudPresent()));
                    @SuppressWarnings("unchecked")
                    List<Number> bounds = (List<Number>) ((JavascriptExecutor) driver).executeScript("""
                            const panel = document.getElementById('selenium-overlay-host').shadowRoot
                                .querySelector('#selenium-hud-panel');
                            const rect = panel.getBoundingClientRect();
                            return [rect.left, rect.top, rect.right, rect.bottom, innerWidth, innerHeight];
                            """);
                    assertTrue(bounds.get(0).doubleValue() >= 9, position + " escaped the left edge at " + size);
                    assertTrue(bounds.get(1).doubleValue() >= 9, position + " escaped the top edge at " + size);
                    assertTrue(bounds.get(2).doubleValue() <= bounds.get(4).doubleValue() - 9,
                            position + " escaped the right edge at " + size);
                    assertTrue(bounds.get(3).doubleValue() <= bounds.get(5).doubleValue() - 9,
                            position + " escaped the bottom edge at " + size);
                    lens.finishPassed();
                }
            } finally {
                driver.quit();
                driver = null;
            }
        }
    }

    @Test
    void hudAppliesSectionTypographyAndKeepsCompactBranding() {
        open("/clicks");
        HudOptions hud = HudOptions.builder()
                .fontPreset(HudFontPreset.UI_SANS)
                .typography(HudTypography.builder()
                        .header(HudFontPreset.MONOSPACE)
                        .eventLog(HudFontPreset.SYSTEM)
                        .metadata(HudFontPreset.MONOSPACE)
                        .build())
                .build();
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder().hud(hud).build());
        lens.startSession("section-typography");
        lens.step("current step inherits global font", () -> { });
        assertTrue(await(hudPresent()));

        @SuppressWarnings("unchecked")
        Map<String, Object> layout = (Map<String, Object>) ((JavascriptExecutor) driver).executeScript("""
                const root = document.getElementById('selenium-overlay-host').shadowRoot;
                const panel = root.querySelector('#selenium-hud-panel');
                const rail = root.querySelector('.stl-hud-side-rail');
                const main = root.querySelector('.stl-hud-main');
                const testValue = root.querySelector('#selenium-hud-test .stl-hud-meta-value');
                const stepValue = root.querySelector('#selenium-hud-step .stl-hud-meta-value');
                const label = root.querySelector('#selenium-hud-test .stl-hud-meta-label');
                const log = root.querySelector('#selenium-hud-logs > div');
                return {
                  railWidth: rail.getBoundingClientRect().width,
                  contentInset: main.getBoundingClientRect().left - panel.getBoundingClientRect().left,
                  panelWidth: panel.getBoundingClientRect().width,
                  mainWidth: main.getBoundingClientRect().width,
                  headerFont: getComputedStyle(testValue).fontFamily,
                  stepFont: getComputedStyle(stepValue).fontFamily,
                  metaFont: getComputedStyle(label).fontFamily,
                  eventFont: getComputedStyle(log).fontFamily,
                  overflow: panel.scrollWidth > panel.clientWidth
                };
                """);

        assertTrue(layout.get("headerFont").toString().contains("ui-monospace"), layout.toString());
        assertTrue(layout.get("stepFont").toString().contains("Test Lens Sora"), layout.toString());
        assertTrue(layout.get("metaFont").toString().contains("ui-monospace"), layout.toString());
        assertTrue(layout.get("eventFont").toString().contains("system-ui"), layout.toString());
        assertTrue(((Number) layout.get("railWidth")).doubleValue() <= 16.5, layout.toString());
        assertTrue(((Number) layout.get("contentInset")).doubleValue() <= 21, layout.toString());
        assertTrue(((Number) layout.get("mainWidth")).doubleValue()
                >= ((Number) layout.get("panelWidth")).doubleValue() - 35, layout.toString());
        assertEquals(Boolean.FALSE, layout.get("overflow"), layout.toString());
    }

    @Test
    void hudHeaderLayoutKeepsAtomicItemsResponsiveAndTruncated() {
        assertHudHeaderLayout(HudHeaderLayout.AUTO, 620, false, false);
        assertHudHeaderLayout(HudHeaderLayout.AUTO, 520, false, false);
        assertHudHeaderLayout(HudHeaderLayout.AUTO, 420, false, false);
        assertHudHeaderLayout(HudHeaderLayout.AUTO, 320, false, false);
        assertHudHeaderLayout(HudHeaderLayout.AUTO, 320, true, false);
        assertHudHeaderLayout(HudHeaderLayout.INLINE, 320, true, false);
        assertHudHeaderLayout(HudHeaderLayout.STACKED, 620, false, false);
        assertHudHeaderLayout(HudHeaderLayout.AUTO, 620, false, true);

        open("/clicks");
        HudOptions responsive = HudOptions.builder().widthPx(620).headerLayout(HudHeaderLayout.AUTO).build();
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder().hud(responsive).build());
        lens.startSession("Checkout creates an order for a returning customer with saved delivery details");
        lens.step("Observe order request", () -> { });
        Map<String, Object> wideMetrics = hudHeaderMetrics(620);
        assertAutoHeaderMatchesAvailableWidth(wideMetrics);
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(390, 844));
        Map<String, Object> narrowResize = awaitHudResize(390, 844, 620, wideMetrics, true);
        Map<String, Object> narrowMetrics = hudHeaderMetrics(620);
        narrowMetrics.put("resizeSettle", narrowResize);
        assertAutoHeaderMatchesAvailableWidth(narrowMetrics);
        assertTrue(((Number) narrowMetrics.get("availableHeaderWidth")).doubleValue()
                < ((Number) wideMetrics.get("availableHeaderWidth")).doubleValue(), narrowMetrics.toString());
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(1024, 768));
        Map<String, Object> wideResize = awaitHudResize(1024, 768, 620, narrowMetrics, false);
        Map<String, Object> restoredMetrics = hudHeaderMetrics(620);
        restoredMetrics.put("resizeSettle", wideResize);
        assertAutoHeaderMatchesAvailableWidth(restoredMetrics);
        lens.finishPassed();
    }

    @Test
    void bundledUiFontLoadsOnceAndIsSharedByHudAndHighlightLabels() {
        open("/clicks");
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
                .hud(HudOptions.builder().fontPreset(HudFontPreset.UI_SANS).build()).build());
        lens.startSession("bundled-font-contract");
        lens.step("Observe order request", () -> { });
        JsOverlayDebug overlay = overlay(true);
        overlay.highlightClick(driver.findElement(By.id("count-button")), "SHARED TYPE");
        overlay.highlightClick(driver.findElement(By.id("count-button")), "SECOND LABEL");

        assertTrue(await(scriptBoolean("""
                return window.__uiTestLens.state.typography.status === 'loaded'
                  && document.fonts.check('400 10px "Test Lens Sora"');
                """)));
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) ((JavascriptExecutor) driver).executeScript("""
                const root=document.getElementById('selenium-overlay-host').shadowRoot;
                const panel=root.querySelector('#selenium-hud-panel');
                const label=root.querySelector('.selenium-overlay-highlight-badge');
                const canvas=document.createElement('canvas'), context=canvas.getContext('2d');
                const width=(text,font)=>{context.font=font;return context.measureText(text).width;};
                let faces=0;
                document.fonts.forEach(face=>{if(face.family.replaceAll('"','')==='Test Lens Sora')faces++;});
                return {hudFont:getComputedStyle(panel).fontFamily,
                  labelFont:getComputedStyle(label).fontFamily,
                  status:panel.dataset.testLensFontStatus,
                  typographyStyles:root.querySelectorAll('style[data-test-lens-visual-typography]').length,
                  faces:faces,
                  testSoraWidth:width('bundled-font-contract','400 10px "Test Lens Sora"'),
                  testSystemWidth:width('bundled-font-contract','400 10px system-ui'),
                  stepSoraWidth:width('Observe order request','400 10px "Test Lens Sora"'),
                  stepSystemWidth:width('Observe order request','400 10px system-ui')};
                """);
        assertTrue(metrics.get("hudFont").toString().contains("Test Lens Sora"), metrics.toString());
        assertTrue(metrics.get("labelFont").toString().contains("Test Lens Sora"), metrics.toString());
        assertEquals("loaded", metrics.get("status"));
        assertEquals(1L, ((Number) metrics.get("typographyStyles")).longValue());
        assertEquals(1L, ((Number) metrics.get("faces")).longValue());
        System.out.printf("HUD_FONT_METRICS test Sora=%.3f system=%.3f; step Sora=%.3f system=%.3f%n",
                metric(metrics, "testSoraWidth"), metric(metrics, "testSystemWidth"),
                metric(metrics, "stepSoraWidth"), metric(metrics, "stepSystemWidth"));
        lens.finishPassed();
    }

    @Test
    void autoHeaderReflowsAfterDelayedFontReadinessAndStillRespondsToResize() {
        open("/clicks");
        ((JavascriptExecutor) driver).executeScript("""
                window.__testLensNativeFontLoad = FontFace.prototype.load;
                FontFace.prototype.load = function() {
                  const face=this, nativeLoad=window.__testLensNativeFontLoad;
                  return new Promise((resolve,reject)=>setTimeout(()=>nativeLoad.call(face).then(resolve,reject),1500));
                };
                """);
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder().hud(HudOptions.builder()
                .widthPx(620).headerLayout(HudHeaderLayout.AUTO).fontPreset(HudFontPreset.UI_SANS).build()).build());
        lens.startSession("Checkout creates an order for a returning customer");
        lens.step("Observe order request", () -> { });
        assertTrue(await(hudPresent()));
        assertEquals("loading", ((JavascriptExecutor) driver).executeScript(
                "return window.__uiTestLens.state.typography.status"));

        assertTrue(await(scriptBoolean("""
                const root=document.getElementById('selenium-overlay-host').shadowRoot;
                const panel=root.querySelector('#selenium-hud-panel');
                return window.__uiTestLens.state.typography.status==='loaded'
                  && Number(panel.dataset.testLensFontReflow||0)>=1
                  && document.fonts.check('400 10px "Test Lens Sora"');
                """)));
        Map<String, Object> loadedMetrics = hudHeaderMetrics(620);
        assertAutoHeaderMatchesAvailableWidth(loadedMetrics);
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(390, 844));
        Map<String, Object> resized = awaitHudResize(390, 844, 620, loadedMetrics, true);
        assertTrue(metric(resized, "panelWidth") < metric(loadedMetrics, "panelWidth"), resized.toString());
        ((JavascriptExecutor) driver).executeScript(
                "FontFace.prototype.load=window.__testLensNativeFontLoad;delete window.__testLensNativeFontLoad");
        lens.finishPassed();
    }

    private void assertHudHeaderLayout(HudHeaderLayout layout, int width,
                                       boolean longValues, boolean pipeline) {
        open("/clicks");
        HudOptions options = HudOptions.builder().preset(pipeline ? HudPreset.DEBUG : HudPreset.COMPACT)
                .widthPx(width).headerLayout(layout)
                .showPipeline(pipeline).build();
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder().hud(options).build());
        String testName = longValues
                ? "Checkout creates an order and preserves all customer delivery details without losing context"
                : "Checkout creates an order for a returning customer";
        String stepName = longValues
                ? "Observe the outgoing order request and verify its completed diagnostic response"
                : "Observe order request";
        lens.startSession(testName);
        lens.step(stepName, () -> { });
        assertTrue(await(hudPresent()));
        Map<String, Object> metrics = hudHeaderMetrics(width);
        int expectedRows = switch (layout) {
            case AUTO -> expectedAutoHeaderRows(metrics);
            case INLINE -> 1;
            case STACKED -> 2;
        };
        assertEquals(expectedRows, ((Number) metrics.get("rows")).intValue(), metrics.toString());
        assertEquals(layout.name(), metrics.get("layout"), metrics.toString());
        assertEquals(Boolean.TRUE, metrics.get("pipeOutside"), metrics.toString());
        assertEquals(Boolean.TRUE, metrics.get("testSingleLine"), metrics.toString());
        assertEquals(Boolean.TRUE, metrics.get("stepSingleLine"), metrics.toString());
        assertEquals(Boolean.FALSE, metrics.get("headerOverflows"), metrics.toString());
        if (longValues) {
            assertTrue(Boolean.TRUE.equals(metrics.get("testEllipsis"))
                    || Boolean.TRUE.equals(metrics.get("stepEllipsis")), metrics.toString());
            assertEquals(testName, metrics.get("testTitle"));
            assertEquals(stepName, metrics.get("stepTitle"));
        }
        lens.finishPassed();
        driver.quit();
        driver = null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> hudHeaderMetrics(int configuredWidth) {
        return (Map<String, Object>) ((JavascriptExecutor) driver).executeScript("""
                const root=document.getElementById('selenium-overlay-host').shadowRoot;
                const panel=root.querySelector('#selenium-hud-panel');
                const header=root.querySelector('.stl-hud-context-header');
                const test=root.querySelector('#selenium-hud-test');
                const step=root.querySelector('#selenium-hud-step');
                const testValue=test.querySelector('.stl-hud-meta-value');
                const stepValue=step.querySelector('.stl-hud-meta-value');
                const pipe=root.querySelector('#selenium-hud-pipeline');
                const headerStyle=getComputedStyle(header);
                const panelRect=panel.getBoundingClientRect();
                const naturalWidth=item=>{
                  const clone=item.cloneNode(true);
                  clone.removeAttribute('id');
                  clone.style.cssText += ';position:fixed;visibility:hidden;pointer-events:none;'
                    + 'flex:none;width:max-content;min-width:0;max-width:none';
                  const row=clone.querySelector('.stl-hud-meta-row');
                  const value=clone.querySelector('.stl-hud-meta-value');
                  row.style.width='max-content';
                  row.style.maxWidth='none';
                  value.style.flex='0 0 auto';
                  value.style.maxWidth='none';
                  value.style.overflow='visible';
                  header.appendChild(clone);
                  const width=clone.getBoundingClientRect().width;
                  clone.remove();
                  return width;
                };
                return {rows:test.offsetTop===step.offsetTop?1:2,
                  configuredWidth:arguments[0],
                  innerWidth:window.innerWidth,
                  innerHeight:window.innerHeight,
                  panelWidth:panelRect.width,
                  panelLeft:panelRect.left,
                  panelRight:panelRect.right,
                  panelTop:panelRect.top,
                  panelBottom:panelRect.bottom,
                  anchor:(panel.style.top!=='auto'?'TOP':'BOTTOM')+'_'+(panel.style.left!=='auto'?'LEFT':'RIGHT'),
                  headerWidth:header.getBoundingClientRect().width,
                  availableHeaderWidth:header.clientWidth,
                  testWidth:test.getBoundingClientRect().width,
                  stepWidth:step.getBoundingClientRect().width,
                  testNaturalWidth:naturalWidth(test),
                  stepNaturalWidth:naturalWidth(step),
                  gap:parseFloat(headerStyle.columnGap)||0,
                  fontFamily:headerStyle.fontFamily,
                  testFontFamily:getComputedStyle(testValue).fontFamily,
                  stepFontFamily:getComputedStyle(stepValue).fontFamily,
                  testEllipsis:testValue.scrollWidth>testValue.clientWidth,
                  stepEllipsis:stepValue.scrollWidth>stepValue.clientWidth,
                  testTitle:testValue.title,stepTitle:stepValue.title,
                  testSingleLine:getComputedStyle(testValue).whiteSpace==='nowrap',
                  stepSingleLine:getComputedStyle(stepValue).whiteSpace==='nowrap',
                  headerOverflows:header.scrollWidth>header.clientWidth+1,
                  pipeOutside:!pipe||pipe.parentNode!==header,
                  layout:header.dataset.layout};
                """, configuredWidth);
    }

    private Map<String, Object> awaitHudResize(int requestedWidth, int requestedHeight, int configuredWidth,
                                               Map<String, Object> previousMetrics, boolean shrinking) {
        long startedAt = System.nanoTime();
        List<Map<String, Object>> polls = new java.util.ArrayList<>();
        try {
            Map<String, Object> settled = new WebDriverWait(driver, WAIT).until(webDriver -> {
                Map<String, Object> metrics = new java.util.LinkedHashMap<>(hudResizeMetrics(configuredWidth));
                metrics.put("poll", polls.size() + 1);
                metrics.put("elapsedMs", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
                polls.add(metrics);
                return hudResizeSettled(metrics, previousMetrics, shrinking) ? metrics : null;
            });
            settled.put("polls", polls.toString());
            return settled;
        } catch (TimeoutException timeout) {
            throw new AssertionError("HUD resize did not settle: requestedWindow=" + requestedWidth + "x"
                    + requestedHeight + ", configuredHudWidth=" + configuredWidth
                    + ", previousPanelWidth=" + previousMetrics.get("panelWidth")
                    + ", previousInnerWidth=" + previousMetrics.get("innerWidth")
                    + ", polls=" + polls, timeout);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> hudResizeMetrics(int configuredWidth) {
        return (Map<String, Object>) ((JavascriptExecutor) driver).executeScript("""
                const root=document.getElementById('selenium-overlay-host').shadowRoot;
                const panel=root.querySelector('#selenium-hud-panel');
                const header=root.querySelector('.stl-hud-context-header');
                const rect=panel.getBoundingClientRect();
                return {configuredWidth:arguments[0],innerWidth:window.innerWidth,innerHeight:window.innerHeight,
                  panelWidth:rect.width,panelLeft:rect.left,panelRight:rect.right,
                  panelTop:rect.top,panelBottom:rect.bottom,
                  availableHeaderWidth:header.clientWidth,
                  anchor:(panel.style.top!=='auto'?'TOP':'BOTTOM')+'_'+(panel.style.left!=='auto'?'LEFT':'RIGHT'),
                  fontFamily:getComputedStyle(header).fontFamily};
                """, configuredWidth);
    }

    private boolean hudResizeSettled(Map<String, Object> metrics, Map<String, Object> previousMetrics,
                                     boolean shrinking) {
        double innerWidth = metric(metrics, "innerWidth");
        double innerHeight = metric(metrics, "innerHeight");
        double panelWidth = metric(metrics, "panelWidth");
        boolean viewportChanged = shrinking
                ? innerWidth < metric(previousMetrics, "innerWidth")
                : innerWidth > metric(previousMetrics, "innerWidth");
        boolean panelWidthChanged = shrinking
                ? panelWidth < metric(previousMetrics, "panelWidth")
                : panelWidth > metric(previousMetrics, "panelWidth");
        double tolerance = 1.0;
        double safeMargin = 10.0;
        boolean withinSafeMargins = metric(metrics, "panelLeft") >= safeMargin - tolerance
                && metric(metrics, "panelRight") <= innerWidth - safeMargin + tolerance
                && metric(metrics, "panelTop") >= safeMargin - tolerance
                && metric(metrics, "panelBottom") <= innerHeight - safeMargin + tolerance;
        return viewportChanged && panelWidthChanged && withinSafeMargins;
    }

    private double metric(Map<String, Object> metrics, String name) {
        return ((Number) metrics.get(name)).doubleValue();
    }

    private void assertAutoHeaderMatchesAvailableWidth(Map<String, Object> metrics) {
        assertEquals(expectedAutoHeaderRows(metrics), ((Number) metrics.get("rows")).intValue(), metrics.toString());
        assertEquals(Boolean.TRUE, metrics.get("testSingleLine"), metrics.toString());
        assertEquals(Boolean.TRUE, metrics.get("stepSingleLine"), metrics.toString());
        assertEquals(Boolean.FALSE, metrics.get("headerOverflows"), metrics.toString());
    }

    private int expectedAutoHeaderRows(Map<String, Object> metrics) {
        double requiredWidth = ((Number) metrics.get("testNaturalWidth")).doubleValue()
                + ((Number) metrics.get("stepNaturalWidth")).doubleValue()
                + ((Number) metrics.get("gap")).doubleValue();
        double availableWidth = ((Number) metrics.get("availableHeaderWidth")).doubleValue();
        return requiredWidth <= availableWidth + 0.5 ? 1 : 2;
    }

    @Test
    void hudUsesConfiguredSubtleScrollbarAndCanRestoreNativeRendering() {
        open("/clicks");
        HudOptions custom = HudOptions.builder()
                .maxLogHeightPx(80)
                .scrollbarStyle(HudScrollbarStyle.SUBTLE)
                .scrollbarWidthPx(7)
                .scrollbarTrackColor("#020617")
                .scrollbarThumbColor("#526174")
                .scrollbarThumbHoverColor("#718096")
                .build();
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder().hud(custom).build());
        lens.startSession("scrollbar-custom");
        for (int index = 0; index < 18; index++) {
            int row = index;
            lens.step("event row " + index, () -> assertTrue(row >= 0));
        }
        assertTrue(await(hudPresent()));

        @SuppressWarnings("unchecked")
        Map<String, Object> customStyle = (Map<String, Object>) ((JavascriptExecutor) driver).executeScript("""
                const logs = document.getElementById('selenium-overlay-host').shadowRoot
                    .querySelector('#selenium-hud-logs');
                const computed = getComputedStyle(logs);
                const webkit = getComputedStyle(logs, '::-webkit-scrollbar');
                return {
                  className: logs.className,
                  widthVariable: logs.style.getPropertyValue('--ui-test-lens-scrollbar-width'),
                  track: logs.style.getPropertyValue('--ui-test-lens-scrollbar-track'),
                  thumb: logs.style.getPropertyValue('--ui-test-lens-scrollbar-thumb'),
                  hover: logs.style.getPropertyValue('--ui-test-lens-scrollbar-thumb-hover'),
                  renderedWidth: webkit.width,
                  overflows: logs.scrollHeight > logs.clientHeight,
                  scrollbarColor: computed.scrollbarColor,
                  scrollbarWidth: computed.scrollbarWidth
                };
                """);
        assertTrue(customStyle.get("className").toString().contains("stl-hud-custom-scrollbar"), customStyle.toString());
        assertTrue(customStyle.get("className").toString().contains("stl-hud-scrollbar-subtle"), customStyle.toString());
        assertEquals("7px", customStyle.get("widthVariable"), customStyle.toString());
        assertEquals("#020617", customStyle.get("track"), customStyle.toString());
        assertEquals("#526174", customStyle.get("thumb"), customStyle.toString());
        assertEquals("#718096", customStyle.get("hover"), customStyle.toString());
        if (browserName().equals("firefox")) {
            assertEquals("thin", customStyle.get("scrollbarWidth"), customStyle.toString());
            assertEquals("rgb(82, 97, 116) rgb(2, 6, 23)", customStyle.get("scrollbarColor"),
                    customStyle.toString());
        } else {
            assertEquals("7px", customStyle.get("renderedWidth"), customStyle.toString());
        }
        assertEquals(Boolean.TRUE, customStyle.get("overflows"), customStyle.toString());
        lens.finishPassed();
        driver.quit();
        driver = null;

        open("/second");
        HudOptions nativeScrollbar = HudOptions.builder().scrollbarStyle(HudScrollbarStyle.NATIVE).build();
        TestLens nativeLens = TestLens.attach(driver, TestLensOptions.builder().hud(nativeScrollbar).build());
        nativeLens.startSession("scrollbar-native");
        nativeLens.step("native scrollbar", () -> { });
        assertTrue(await(hudPresent()));
        @SuppressWarnings("unchecked")
        Map<String, Object> nativeStyle = (Map<String, Object>) ((JavascriptExecutor) driver).executeScript("""
                const logs=document.getElementById('selenium-overlay-host').shadowRoot
                    .querySelector('#selenium-hud-logs');
                const computed=getComputedStyle(logs);
                return {className:logs.className,
                  widthVariable:logs.style.getPropertyValue('--ui-test-lens-scrollbar-width'),
                  track:logs.style.getPropertyValue('--ui-test-lens-scrollbar-track'),
                  thumb:logs.style.getPropertyValue('--ui-test-lens-scrollbar-thumb'),
                  hover:logs.style.getPropertyValue('--ui-test-lens-scrollbar-thumb-hover'),
                  scrollbarColor:computed.scrollbarColor,
                  scrollbarWidth:computed.scrollbarWidth};
                """);
        assertFalse(nativeStyle.get("className").toString().contains("stl-hud-custom-scrollbar"),
                nativeStyle.toString());
        assertEquals("", nativeStyle.get("widthVariable"), nativeStyle.toString());
        assertEquals("", nativeStyle.get("track"), nativeStyle.toString());
        assertEquals("", nativeStyle.get("thumb"), nativeStyle.toString());
        assertEquals("", nativeStyle.get("hover"), nativeStyle.toString());
        if (browserName().equals("firefox")) {
            assertEquals("auto", nativeStyle.get("scrollbarWidth"), nativeStyle.toString());
            assertEquals("auto", nativeStyle.get("scrollbarColor"), nativeStyle.toString());
        }
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
        assertTrue(await(highlightPresent()));
        @SuppressWarnings("unchecked")
        Map<String, Object> typography = (Map<String, Object>) ((JavascriptExecutor) driver).executeScript("""
                const root=document.getElementById('selenium-overlay-host').shadowRoot;
                const badge=root.querySelector('.selenium-overlay-highlight-badge');
                return {font:getComputedStyle(badge).fontFamily,
                  status:window.__uiTestLens.state.typography.status};
                """);
        String cspFont = typography.get("font").toString();
        assertTrue(cspFont.contains("Test Lens Sora") || cspFont.contains("sans-serif"), typography.toString());
        assertTrue(Set.of("loading", "loaded", "timeout", "failed", "unsupported")
                .contains(typography.get("status").toString()), typography.toString());
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
        assertFalse(driver.getTitle().isBlank(), "finishFailed must leave the WebDriver alive");
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.startsWith(baseUrl),
                "finishFailed must leave the driver in the active local page; browser=" + browserName()
                        + ", page=" + page + ", currentUrl=" + currentUrl);
        assertTrue(hudPresent().apply(driver), "cleanup=false must restore HUD after clean capture");
        assertTrue(highlightPresent().apply(driver), "cleanup=false must restore highlight after clean capture");

        Path bundle = result.failureBundleDirectory().orElseThrow();
        assertTrue(Files.isRegularFile(result.failureScreenshot()));
        assertTrue(Files.isRegularFile(bundle.resolve("failure-clean.png")));
        String context = Files.readString(bundle.resolve("context.json"));
        assertTrue(context.contains("currentUrl"));
        assertTrue(context.contains("127.0.0.1"));
        assertTrue(context.contains("title"));
        assertTrue(context.contains("currentWindowHandle"));
        assertTrue(Files.readString(bundle.resolve("runtime.json")).contains(browserName()));
        assertTrue(Files.isRegularFile(result.jsonReport()));
        assertTrue(Files.isRegularFile(result.htmlReport()));
        assertTrue(result.failureBundleManifest().isPresent());
        assertTrue(result.failureBundleArchive().isPresent());

        ManifestComponent pageSource = manifestComponent(result.failureBundleManifest().orElseThrow(), "pageSource");
        String pageSourceDiagnostic = "browser=" + browserName() + ", page=" + page
                + ", collectorStatus=" + pageSource.status() + ", path=" + pageSource.path()
                + ", diagnostic=" + pageSource.message();
        if ("/clicks".equals(page)) {
            assertEquals("CAPTURED", pageSource.status(),
                    "Stable page must retain strong page-source coverage; " + pageSourceDiagnostic);
        } else if (!Set.of("CAPTURED", "FAILED", "UNSUPPORTED").contains(pageSource.status())) {
            fail("Complete page-source collection must be captured or explicitly report unavailability; "
                    + pageSourceDiagnostic);
        }

        Path pageSourceFile = bundle.resolve("page-source.html");
        if ("CAPTURED".equals(pageSource.status())) {
            assertEquals("failure-bundle/page-source.html", pageSource.path(), pageSourceDiagnostic);
            assertEquals("text/html", pageSource.mediaType(), pageSourceDiagnostic);
            assertNotNull(pageSource.sizeBytes(), pageSourceDiagnostic);
            assertTrue(pageSource.sizeBytes() > 0, pageSourceDiagnostic);
            assertTrue(Files.isRegularFile(pageSourceFile), pageSourceDiagnostic);
        } else {
            assertTrue(pageSource.message() != null && !pageSource.message().isBlank(), pageSourceDiagnostic);
            assertNull(pageSource.path(), pageSourceDiagnostic);
            assertNull(pageSource.sizeBytes(), pageSourceDiagnostic);
            assertFalse(Files.exists(pageSourceFile),
                    "An unavailable collector must not leave a false page-source artifact; " + pageSourceDiagnostic);
        }

        try (ZipFile zip = new ZipFile(result.failureBundleArchive().orElseThrow().toFile())) {
            assertTrue(zip.getEntry("manifest.json") != null);
            assertTrue(zip.getEntry("trace.json") != null);
            assertTrue(zip.getEntry("report.html") != null);
            assertTrue(zip.getEntry("failure-diagnostic.png") != null);
            assertTrue(zip.getEntry("failure-clean.png") != null);
            if ("CAPTURED".equals(pageSource.status())) {
                assertNotNull(zip.getEntry("page-source.html"), pageSourceDiagnostic);
            } else {
                assertNull(zip.getEntry("page-source.html"),
                        "An unavailable collector must not add a false ZIP entry; " + pageSourceDiagnostic);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static ManifestComponent manifestComponent(Path manifestPath, String componentName) throws IOException {
        Map<String, Object> manifest = new Json().toType(Files.readString(manifestPath), Map.class);
        Object componentsValue = manifest.get("components");
        if (!(componentsValue instanceof Map<?, ?> components)) {
            throw new AssertionError("Failure-bundle manifest has no components object: " + manifestPath);
        }
        Object componentValue = components.get(componentName);
        if (!(componentValue instanceof Map<?, ?> component)) {
            throw new AssertionError("Failure-bundle manifest has no " + componentName + " component: " + manifestPath);
        }
        Object size = component.get("sizeBytes");
        return new ManifestComponent(
                (String) component.get("status"),
                (String) component.get("path"),
                size instanceof Number number ? number.longValue() : null,
                (String) component.get("mediaType"),
                (String) component.get("message"));
    }

    private record ManifestComponent(String status, String path, Long sizeBytes, String mediaType, String message) {
    }

    @Test
    void portableFullPageScreenshotsPreserveLayoutContextAndFailureEvidence() throws Exception {
        open("/full-page");
        Path output = Path.of("target", "ui-test-lens", browserName(), "full-page-" + UUID.randomUUID());
        TestLens overlaysOff = TestLens.attach(driver, TestLensOptions.builder()
                .overlayConfig(OverlayConfig.builder().enabled(false).build())
                .outputRoot(output)
                .build());
        ScreenshotCaptureResult overlaysOffResult = overlaysOff.captureScreenshot("full-page-overlays-off",
                ScreenshotCaptureOptions.builder().outputDirectory(output).includeTimestamp(false)
                        .captureMode(ScreenshotCaptureMode.FULL_PAGE).build());
        assertTrue(overlaysOffResult.isCaptured(), overlaysOffResult.message());
        assertTrue(overlaysOffResult.tileCount() > 2);
        assertEquals(0L, number("return document.querySelectorAll('#selenium-overlay-host, "
                + "[data-test-lens-overlay-snapshot]').length"),
                "HUD/highlights OFF must remain off before and after capture");

        TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
                .overlayConfig(OverlayConfig.builder().enabled(true).decorationDurationMs(3_000).build())
                .cleanupHudOnFinish(false)
                .failureBundleOptions(FailureBundleOptions.builder()
                        .screenshotCaptureMode(ScreenshotCaptureMode.FULL_PAGE).build())
                .outputRoot(output)
                .build());
        lens.startSession("portable-full-page-" + UUID.randomUUID());
        overlay(true).hudLog("info", "click Login button", "browser-it");
        overlay(true).hudLog("warn", "retry Login button", "browser-it");
        overlay(true).hudLog("error", "failure Login button", "browser-it");
        assertTrue(await(hudPresent()));

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

        overlay(true).highlightElement(driver.findElement(By.id("full-page-sticky")), "Login button");
        ((JavascriptExecutor) driver).executeScript("""
                const host = document.getElementById('selenium-overlay-host');
                const root = host.shadowRoot;
                const shell = root.querySelector('.stl-hud-shell');
                const highlight = root.querySelector('[data-uitestlens-highlight]');
                const badge = highlight && highlight.querySelector('.selenium-overlay-highlight-badge');
                host.style.setProperty('visibility', 'visible', 'important');
                if (shell) shell.style.setProperty('box-shadow', '0 0 0 6px rgb(201, 17, 91)', 'important');
                highlight.style.setProperty('border-color', 'rgb(17, 201, 91)', 'important');
                highlight.style.setProperty('background', 'rgb(17, 201, 91)', 'important');
                badge.style.setProperty('background', 'rgb(91, 17, 201)', 'important');
                const hostRect = host.getBoundingClientRect();
                window.__fullPageSnapshotProbe = {
                  beforeWidth: document.documentElement.scrollWidth,
                  beforeHeight: document.documentElement.scrollHeight,
                  initialScrollX: scrollX, initialScrollY: scrollY,
                  hostLeft: hostRect.left, hostTop: hostRect.top, installed: null
                };
                window.__fullPageSnapshotObserver = new MutationObserver(() => {
                  const snapshot = document.querySelector('[data-test-lens-overlay-snapshot]');
                  if (!snapshot) return;
                  window.__fullPageSnapshotStyleChanges = 0;
                  window.__fullPageSnapshotStyleObserver = new MutationObserver(records => {
                    window.__fullPageSnapshotStyleChanges += records.length;
                  });
                  window.__fullPageSnapshotStyleObserver.observe(snapshot,
                    {attributes:true, attributeFilter:['style']});
                  const snapshotRect = snapshot.getBoundingClientRect();
                  const snapshotRoot = snapshot.shadowRoot;
                  const snapshotHud = snapshotRoot && snapshotRoot.querySelector('#selenium-hud-panel');
                  const snapshotHighlight = snapshotRoot && snapshotRoot.querySelector('[data-uitestlens-highlight]');
                  window.__fullPageSnapshotProbe.installed = {
                    width: document.documentElement.scrollWidth,
                    height: document.documentElement.scrollHeight,
                    left: parseFloat(snapshot.style.left), top: parseFloat(snapshot.style.top),
                    rectLeft: snapshotRect.left, rectTop: snapshotRect.top,
                    liveCount: document.querySelectorAll('#selenium-overlay-host').length,
                    snapshotCount: document.querySelectorAll('[data-test-lens-overlay-snapshot]').length,
                    duplicateHostId: snapshot.id === 'selenium-overlay-host',
                    pointerEvents: getComputedStyle(snapshot).pointerEvents,
                    inert: snapshot.hasAttribute('inert'), ariaHidden: snapshot.getAttribute('aria-hidden'),
                    shadowChildren: snapshotRoot ? snapshotRoot.childNodes.length : 0,
                    hudText: snapshotHud ? snapshotHud.textContent : '',
                    highlightCount: snapshotRoot ? snapshotRoot.querySelectorAll('[data-uitestlens-highlight]').length : 0,
                    label: snapshotHighlight && snapshotHighlight.querySelector('.selenium-overlay-highlight-badge')
                      ? snapshotHighlight.querySelector('.selenium-overlay-highlight-badge').textContent : ''
                  };
                  window.__fullPageSnapshotObserver.disconnect();
                });
                window.__fullPageSnapshotObserver.observe(document.documentElement, {childList:true, subtree:true});
                window.__fullPageGuardObservations = [];
                window.__fullPageHeightChanges = 0;
                window.__fullPageInitialSheets = document.adoptedStyleSheets ? document.adoptedStyleSheets.length : -1;
                addEventListener('scroll', () => {
                  if (scrollY > 0 && window.__fullPageHeightChanges === 0) {
                    window.__fullPageHeightChanges++;
                    document.getElementById('full-page-bottom').style.height = '760px';
                  }
                  const liveShell = host.shadowRoot.querySelector('.stl-hud-shell');
                  if (liveShell) liveShell.style.setProperty('box-shadow', '0 0 0 6px rgb(91, 201, 17)', 'important');
                  requestAnimationFrame(() => {
                    const animated = getComputedStyle(document.getElementById('full-page-animated'));
                    const transitioning = getComputedStyle(document.getElementById('full-page-transition'));
                    const caret = getComputedStyle(document.getElementById('full-page-caret'));
                    window.__fullPageGuardObservations.push({animation:animated.animationPlayState,
                      transition:transitioning.transitionDuration, caret:caret.caretColor,
                      liveOverlay:getComputedStyle(host).visibility,
                      snapshot:document.querySelectorAll('[data-test-lens-overlay-snapshot]').length});
                  });
                }, {passive:true});
                """);
        assertEquals(1L, number("return document.getElementById('selenium-overlay-host').shadowRoot"
                + ".querySelectorAll('[data-uitestlens-highlight]').length"));
        ScreenshotCaptureResult overlayBaselineResult = lens.captureScreenshot("full-page-overlay-baseline",
                ScreenshotCaptureOptions.builder().outputDirectory(output).includeTimestamp(false).build());
        assertTrue(overlayBaselineResult.isCaptured(), overlayBaselineResult.message());
        BufferedImage overlayBaseline = ImageIO.read(overlayBaselineResult.path().toFile());

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
        assertEquals(Math.round(number("return document.documentElement.scrollWidth") * scaleX), captured.width());
        assertEquals(Math.round(number("return document.documentElement.scrollHeight") * scaleY), captured.height());
        BufferedImage image = ImageIO.read(captured.path().toFile());
        assertTrue(containsRgb(image, 220, 40, 40), "top marker must be present");
        assertTrue(containsRgb(image, 40, 180, 70), "middle marker must be present");
        assertTrue(containsRgb(image, 35, 80, 220), "bottom marker must be present");
        assertTrue(containsRgb(image, 255, 165, 0), "right-side marker must be present");
        assertTrue(containsRgb(image, 0, 220, 220), "sticky marker must remain present");
        assertEquals(1, countColorClusters(overlayBaseline, 201, 17, 91, 100, 16));
        assertEquals(1, countColorClusters(overlayBaseline, 17, 201, 91, 20, 16));
        assertEquals(1, countColorClusters(overlayBaseline, 91, 17, 201, 20, 16));
        assertEquals(1, countColorClusters(image, 201, 17, 91, 100, 16),
                "the stitched image must contain exactly one HUD marker");
        assertEquals(1, countColorClusters(image, 17, 201, 91, 20, 16),
                "the stitched image must contain exactly one highlight marker");
        assertEquals(1, countColorClusters(image, 91, 17, 201, 20, 16),
                "the stitched image must contain exactly one label marker");
        assertEquals(0, countRgb(image, 91, 201, 17),
                "a live HUD rerender during stitching must not leak into the frozen evidence state");
        assertNoTransparentRow(image);
        assertEquals(1L, number("return window.__fullPageHeightChanges"),
                "the scroll-triggered application layout shift must be incorporated into the same capture attempt");
        assertEquals(0L, number("return window.__fullPageSnapshotStyleChanges"),
                "the frozen overlay snapshot must not be restyled between geometry observations");
        assertEquals(1L, number("return window.__fullPageGuardObservations.some(v => v.animation === 'paused') ? 1 : 0"));
        assertEquals(1L, number("return window.__fullPageGuardObservations.some(v => v.transition === '0s') ? 1 : 0"));
        assertEquals(1L, number("return window.__fullPageGuardObservations.some(v => v.caret === 'transparent' || v.caret === 'rgba(0, 0, 0, 0)') ? 1 : 0"));
        assertEquals(1L, number("return window.__fullPageGuardObservations.some(v => v.liveOverlay === 'hidden' && v.snapshot === 1) ? 1 : 0"));
        assertEquals(number("return window.__fullPageSnapshotProbe.beforeWidth"),
                number("return window.__fullPageSnapshotProbe.installed.width"),
                "installing the overlay snapshot must not change scrollWidth");
        assertEquals(number("return window.__fullPageSnapshotProbe.beforeHeight"),
                number("return window.__fullPageSnapshotProbe.installed.height"),
                "installing the overlay snapshot must not change scrollHeight");
        assertEquals(initialX, number("return Math.round(window.__fullPageSnapshotProbe.installed.left)"));
        assertEquals(initialY, number("return Math.round(window.__fullPageSnapshotProbe.installed.top)"));
        assertEquals(number("return Math.round(window.__fullPageSnapshotProbe.hostLeft)"),
                number("return Math.round(window.__fullPageSnapshotProbe.installed.rectLeft)"));
        assertEquals(number("return Math.round(window.__fullPageSnapshotProbe.hostTop)"),
                number("return Math.round(window.__fullPageSnapshotProbe.installed.rectTop)"));
        assertEquals(1L, number("return window.__fullPageSnapshotProbe.installed.liveCount"));
        assertEquals(1L, number("return window.__fullPageSnapshotProbe.installed.snapshotCount"));
        assertEquals(0L, number("return window.__fullPageSnapshotProbe.installed.duplicateHostId ? 1 : 0"));
        assertEquals(1L, number("return window.__fullPageSnapshotProbe.installed.pointerEvents === 'none' ? 1 : 0"));
        assertEquals(1L, number("return window.__fullPageSnapshotProbe.installed.inert"
                + " && window.__fullPageSnapshotProbe.installed.ariaHidden === 'true' ? 1 : 0"));
        assertEquals(1L, number("return window.__fullPageSnapshotProbe.installed.shadowChildren > 0"
                + " && window.__fullPageSnapshotProbe.installed.hudText.includes('click Login button')"
                + " && window.__fullPageSnapshotProbe.installed.hudText.includes('retry Login button')"
                + " && window.__fullPageSnapshotProbe.installed.hudText.includes('failure Login button')"
                + " && window.__fullPageSnapshotProbe.installed.highlightCount === 1"
                + " && window.__fullPageSnapshotProbe.installed.label === 'Login button' ? 1 : 0"),
                "the manually cloned shadow tree must contain the HUD, statuses, highlight and label");
        assertEquals(0L, number("return document.querySelectorAll('[data-test-lens-screenshot-guard]').length"));
        assertEquals(0L, number("return document.querySelectorAll('[data-test-lens-overlay-snapshot]').length"));
        assertEquals(number("return window.__fullPageInitialSheets"),
                number("return document.adoptedStyleSheets ? document.adoptedStyleSheets.length : -1"));
        assertEquals(1L, number("return getComputedStyle(document.getElementById('full-page-animated')).animationPlayState === 'running' ? 1 : 0"));
        assertEquals(1L, number("return getComputedStyle(document.getElementById('full-page-transition')).transitionDuration === '2s' ? 1 : 0"));
        assertEquals(1L, number("return getComputedStyle(document.getElementById('selenium-overlay-host')).visibility === 'visible' ? 1 : 0"));
        assertEquals(1L, number("return document.getElementById('selenium-overlay-host').style.visibility === 'visible'"
                + " && document.getElementById('selenium-overlay-host').style.getPropertyPriority('visibility') === 'important' ? 1 : 0"),
                "restore must preserve the prior inline visibility value and priority");
        assertTrue(hudPresent().apply(driver), "HUD must be restored without reinjection after capture");
        assertEquals(1L, number("return getComputedStyle(document.getElementById('selenium-overlay-host').shadowRoot"
                + ".querySelector('.stl-hud-shell')).boxShadow.includes('rgb(91, 201, 17)') ? 1 : 0"),
                "the live overlay must resume at its naturally updated post-capture state");
        assertTrue(await(d -> ((Number) ((JavascriptExecutor) d).executeScript(
                "return document.getElementById('selenium-overlay-host').shadowRoot"
                        + ".querySelectorAll('[data-uitestlens-highlight]').length")).longValue() == 0),
                "the live highlight cleanup timer must resume normally after capture");
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
        assertTrue(countRgb(diagnostic, 91, 201, 17) > 500,
                "diagnostic full-page failure evidence must contain the current HUD");
        assertEquals(0, countRgb(clean, 91, 201, 17),
                "the separately requested clean failure artifact keeps its overlay-free contract");
        assertTrue(Files.readString(result.failureBundleManifest().orElseThrow()).contains("FULL_PAGE"));
        assertTrue(hudPresent().apply(driver), "clean capture must restore the HUD");
        assertFalse(driver.getTitle().isBlank(), "capture and finalization must leave the driver active");
    }

    @Test
    void fullPageCaptureExtendsToScrollTriggeredContentWithoutRetryFallback() throws Exception {
        open("/full-page");
        Path output = Path.of("target", "ui-test-lens", browserName(),
                "dynamic-full-page-" + UUID.randomUUID());
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
                .overlayConfig(OverlayConfig.builder().enabled(false).build())
                .outputRoot(output)
                .build());

        @SuppressWarnings("unchecked")
        Map<String, Number> initial = (Map<String, Number>) ((JavascriptExecutor) driver).executeScript("""
                return {documentHeight: document.documentElement.scrollHeight,
                  viewportHeight: innerHeight};
                """);
        ScreenshotCaptureResult viewport = lens.captureScreenshot("dynamic-height-viewport",
                ScreenshotCaptureOptions.builder().outputDirectory(output).includeTimestamp(false).build());
        assertTrue(viewport.isCaptured(), viewport.message());
        long originalY = 123;
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, arguments[0])", originalY);
        ((JavascriptExecutor) driver).executeScript("""
                window.__dynamicFullPageGrowths = 0;
                addEventListener('scroll', function addDynamicScreenshotContent() {
                  if (scrollY <= 123 || window.__dynamicFullPageGrowths > 0) return;
                  window.__dynamicFullPageGrowths++;
                  const added = document.createElement('div');
                  added.id = 'dynamic-full-page-bottom';
                  added.style.cssText = 'height:240px;background:rgb(123,45,210)';
                  added.textContent = 'Dynamically added screenshot bottom';
                  document.body.appendChild(added);
                }, {passive:true});
                """);

        ScreenshotCaptureResult captured = lens.captureScreenshot("dynamic-height-full-page",
                ScreenshotCaptureOptions.builder().outputDirectory(output).includeTimestamp(false)
                        .captureMode(ScreenshotCaptureMode.FULL_PAGE).build());

        assertTrue(captured.isCaptured(), captured.message());
        long finalHeight = number("return document.documentElement.scrollHeight");
        assertTrue(finalHeight > initial.get("documentHeight").longValue());
        double scaleY = viewport.height() / initial.get("viewportHeight").doubleValue();
        assertEquals(Math.round(finalHeight * scaleY), captured.height());
        assertEquals(1L, number("return window.__dynamicFullPageGrowths"));
        BufferedImage image = ImageIO.read(captured.path().toFile());
        assertTrue(containsRgb(image, 123, 45, 210), "the dynamically appended bottom must be captured");
        assertEquals(1, countColorClusters(image, 123, 45, 210, 100, 16),
                "the dynamically appended bottom must not be duplicated");
        assertNoTransparentRow(image);
        assertEquals(originalY, number("return Math.round(window.scrollY)"));
    }

    @Test
    void visualRedactionProtectsViewportFullPageAndFailurePairWithoutMutatingPage() throws Exception {
        open("/visual-redaction");
        Path output = Path.of("target", "ui-test-lens", browserName(), "visual-redaction-" + UUID.randomUUID());
        VisualRedactionOptions visual = VisualRedactionOptions.builder()
                .mask(By.id("customer-number"), VisualMaskMode.SOLID)
                .mask(By.id("customer-email"), VisualMaskMode.BLUR)
                .mask(By.id("far-secret"), VisualMaskMode.SOLID)
                .solidColor("#13579B")
                .blurRadiusPx(12)
                .paddingPx(2)
                .failurePolicy(VisualRedactionFailurePolicy.STRICT)
                .build();
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
                .visualRedaction(visual)
                .failureBundleOptions(FailureBundleOptions.defaults())
                .outputRoot(output).build());
        lens.startSession("visual-redaction-" + UUID.randomUUID());

        @SuppressWarnings("unchecked")
        Map<String, Number> emailRect = (Map<String, Number>) ((JavascriptExecutor) driver).executeScript("""
                const r = document.getElementById('customer-email').getBoundingClientRect();
                return {left:r.left, top:r.top, width:r.width, height:r.height};
                """);
        @SuppressWarnings("unchecked")
        Map<String, Number> passwordRect = (Map<String, Number>) ((JavascriptExecutor) driver).executeScript("""
                const r = document.getElementById('password-secret').getBoundingClientRect();
                return {left:r.left, top:r.top, width:r.width, height:r.height};
                """);
        @SuppressWarnings("unchecked")
        Map<String, Number> customerRect = (Map<String, Number>) ((JavascriptExecutor) driver).executeScript("""
                const r = document.getElementById('customer-number').getBoundingClientRect();
                return {left:r.left, top:r.top, width:r.width, height:r.height};
                """);
        TestLens baselineLens = TestLens.attach(driver, TestLensOptions.builder()
                .visualRedaction(VisualRedactionOptions.disabled()).build());
        ScreenshotCaptureResult baseline = baselineLens.captureScreenshot("visual-baseline",
                ScreenshotCaptureOptions.builder().outputDirectory(output).includeTimestamp(false).build());
        ScreenshotCaptureResult viewport = lens.captureScreenshot("visual-masked",
                ScreenshotCaptureOptions.builder().outputDirectory(output).includeTimestamp(false).build());
        assertTrue(viewport.isCaptured(), viewport.message());
        BufferedImage baselineImage = ImageIO.read(baseline.path().toFile());
        BufferedImage maskedImage = ImageIO.read(viewport.path().toFile());
        assertTrue(countRgb(maskedImage, 19, 87, 155) > 2_000,
                "password and explicit SOLID masks must produce a substantial opaque region");
        assertTrue(regionColorRatio(maskedImage, passwordRect, 19, 87, 155) > 0.90,
                "automatic password masking must cover its pixel region with SOLID");
        assertTrue(regionColorRatio(maskedImage, customerRect, 19, 87, 155) > 0.90,
                "explicit SOLID must cover its pixel region with the custom color");
        assertTrue(regionDifference(baselineImage, maskedImage, emailRect) > 20,
                "BLUR must alter pixels in the configured email region");
        assertEquals("THIS_MUST_NOT_APPEAR_7F3A", driver.findElement(By.id("password-secret")).getAttribute("value"));
        assertEquals("customer-004291", driver.findElement(By.id("customer-number")).getAttribute("value"));
        assertEquals(0L, number("return document.querySelectorAll('[data-test-lens-visual-mask]').length"));

        ((JavascriptExecutor) driver).executeScript("""
                window.__visualBatchAttempts = [];
                window.__visualShiftDone = false;
                window.__visualBatchObserver = new MutationObserver(() => {
                  const batch = document.querySelector('[data-test-lens-visual-mask-batch]');
                  if (!batch) return;
                  const attempt = Number(batch.getAttribute('data-test-lens-visual-mask-attempt'));
                  if (!window.__visualBatchAttempts.includes(attempt)) window.__visualBatchAttempts.push(attempt);
                  if (attempt === 1 && !window.__visualShiftDone) {
                    window.__visualShiftDone = true;
                    document.getElementById('far-secret').style.transform = 'translateX(120px)';
                  }
                });
                window.__visualBatchObserver.observe(document.body, {childList:true, subtree:true});
                """);
        ScreenshotCaptureResult full = lens.captureScreenshot("visual-full-page", ScreenshotCaptureOptions.builder()
                .outputDirectory(output).includeTimestamp(false).captureMode(ScreenshotCaptureMode.FULL_PAGE).build());
        ((JavascriptExecutor) driver).executeScript("window.__visualBatchObserver.disconnect()");
        assertTrue(full.isCaptured(), full.message());
        assertTrue(full.tileCount() > 1);
        BufferedImage fullImage = ImageIO.read(full.path().toFile());
        assertTrue(countRgb(fullImage, 19, 87, 155) > 4_000,
                "off-screen secret must be SOLID-masked in stitched output");
        @SuppressWarnings("unchecked")
        Map<String, Number> shiftedFarRect = (Map<String, Number>) ((JavascriptExecutor) driver).executeScript("""
                const r = document.getElementById('far-secret').getBoundingClientRect();
                return {left:r.left + scrollX, top:r.top + scrollY, width:r.width, height:r.height};
                """);
        assertTrue(fullPageRegionColorRatio(fullImage, shiftedFarRect, 19, 87, 155) > 0.90,
                "full-page retry must mask the current post-layout-shift geometry");
        assertTrue(number("return window.__visualBatchAttempts.includes(2) ? 1 : 0") == 1,
                "layout shift must force a whole-batch retry");
        assertEquals(0L, number("return document.querySelectorAll('[data-test-lens-visual-mask]').length"));

        TestLensFinalizationResult failure = lens.finishFailed(new AssertionError("controlled visual failure"));
        BufferedImage diagnostic = ImageIO.read(failure.failureScreenshot().toFile());
        BufferedImage clean = ImageIO.read(failure.failureBundleDirectory().orElseThrow()
                .resolve("failure-clean.png").toFile());
        assertTrue(countRgb(diagnostic, 19, 87, 155) > 2_000, "diagnostic screenshot must remain masked");
        assertTrue(countRgb(clean, 19, 87, 155) > 2_000, "clean screenshot must remain masked");
        assertFalse(driver.getPageSource().isBlank());
        assertTrue(driver.getPageSource().contains("THIS_MUST_NOT_APPEAR_7F3A"),
                "visual redaction intentionally does not redact page source in memory");

        Path allureResults = output.resolve("allure-results");
        AllureLifecycle allure = new AllureLifecycle(new FileSystemResultsWriter(allureResults));
        Allure.setLifecycle(allure);
        String allureUuid = UUID.randomUUID().toString();
        allure.scheduleTestCase(allureUuid, new TestResult().setUuid(allureUuid).setName("visual redaction attachment"));
        allure.startTestCase(allureUuid);
        assertEquals(AllureAttachStatus.ATTACHED, AllureTestLens.attach(failure).status());
        allure.stopTestCase(allureUuid);
        allure.writeTestCase(allureUuid);
        byte[] diagnosticBytes = Files.readAllBytes(failure.failureScreenshot());
        byte[] cleanBytes = Files.readAllBytes(failure.cleanFailureScreenshot().orElseThrow());
        try (Stream<Path> attachments = Files.list(allureResults)) {
            List<byte[]> pngs = attachments.filter(path -> path.toString().endsWith("-attachment.png"))
                    .map(path -> { try { return Files.readAllBytes(path); } catch (IOException e) { throw new RuntimeException(e); } })
                    .toList();
            assertEquals(2, pngs.size());
            assertTrue(pngs.stream().anyMatch(bytes -> java.util.Arrays.equals(bytes, diagnosticBytes)));
            assertTrue(pngs.stream().anyMatch(bytes -> java.util.Arrays.equals(bytes, cleanBytes)));
        }
    }

    @Test
    void visualRedactionLabelUsesSharedTypographyWithoutOwningMaskGeometry() {
        open("/visual-redaction");
        ((JavascriptExecutor) driver).executeScript("""
                window.__visualMaskLabelFont = null;
                window.__visualMaskLabelObserver = new MutationObserver(() => {
                  const mask=document.querySelector('[data-test-lens-visual-mask]');
                  if(mask && mask.textContent==='REDACTED') {
                    window.__visualMaskLabelFont=getComputedStyle(mask).fontFamily;
                  }
                });
                window.__visualMaskLabelObserver.observe(document.body,{childList:true,subtree:true});
                """);
        Path output = Path.of("target", "ui-test-lens", browserName(), "visual-redaction-font-" + UUID.randomUUID());
        VisualRedactionOptions visual = VisualRedactionOptions.builder()
                .maskPasswordInputs(false)
                .mask(By.id("customer-number"), VisualMaskMode.SOLID)
                .maskLabel("REDACTED")
                .failurePolicy(VisualRedactionFailurePolicy.STRICT)
                .build();
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
                .visualRedaction(visual).outputRoot(output).build());
        ScreenshotCaptureResult capture = lens.captureScreenshot("redaction-label-font",
                ScreenshotCaptureOptions.builder().outputDirectory(output).includeTimestamp(false).build());
        assertTrue(capture.isCaptured(), capture.message());
        assertTrue(await(scriptBoolean("return !!window.__visualMaskLabelFont")));
        String font = String.valueOf(((JavascriptExecutor) driver)
                .executeScript("return window.__visualMaskLabelFont"));
        assertTrue(font.contains("Test Lens Sora"), font);
        assertEquals(0L, number("return document.querySelectorAll('[data-test-lens-visual-mask]').length"));
        ((JavascriptExecutor) driver).executeScript("window.__visualMaskLabelObserver.disconnect()");
    }

    @Test
    void visualRedactionReResolvesReactLikeReplacementBeforeStrictCapture() throws Exception {
        open("/visual-redaction");
        Path output = Path.of("target", "ui-test-lens", browserName(), "visual-replacement-" + UUID.randomUUID());
        ((JavascriptExecutor) driver).executeScript("""
                window.__visualBatchAttempts = [];
                window.__visualReplacementDone = false;
                window.__visualBatchObserver = new MutationObserver(() => {
                  const batch = document.querySelector('[data-test-lens-visual-mask-batch]');
                  if (!batch) return;
                  const attempt = Number(batch.getAttribute('data-test-lens-visual-mask-attempt'));
                  if (!window.__visualBatchAttempts.includes(attempt)) window.__visualBatchAttempts.push(attempt);
                  if (attempt === 1 && !window.__visualReplacementDone) {
                    window.__visualReplacementDone = true;
                    const oldTarget = document.getElementById('password-secret');
                    const replacement = oldTarget.cloneNode(true);
                    replacement.style.marginLeft = '180px';
                    oldTarget.replaceWith(replacement);
                  }
                });
                window.__visualBatchObserver.observe(document.body, {childList:true, subtree:true});
                """);
        TestLens lens = strictSolidLens(output, By.id("password-secret"));
        ScreenshotCaptureResult result = lens.captureScreenshot("replacement",
                ScreenshotCaptureOptions.builder().outputDirectory(output).includeTimestamp(false).build());
        ((JavascriptExecutor) driver).executeScript("window.__visualBatchObserver.disconnect()");

        assertTrue(result.isCaptured(), result.message());
        @SuppressWarnings("unchecked")
        Map<String, Number> currentRect = (Map<String, Number>) ((JavascriptExecutor) driver).executeScript("""
                const r = document.getElementById('password-secret').getBoundingClientRect();
                return {left:r.left, top:r.top, width:r.width, height:r.height};
                """);
        assertTrue(regionColorRatio(ImageIO.read(result.path().toFile()), currentRect, 19, 87, 155) > 0.90,
                "replacement node's current bounding box must be masked");
        assertEquals(1L, number("return window.__visualBatchAttempts.includes(2) ? 1 : 0"));
        assertEquals("THIS_MUST_NOT_APPEAR_7F3A", driver.findElement(By.id("password-secret")).getAttribute("value"));
        assertEquals(0L, number("return document.querySelectorAll('[data-test-lens-visual-mask]').length"));
    }

    @Test
    void visualRedactionRetriesSameNodeMoveAndResizeBeforeStrictCapture() throws Exception {
        open("/visual-redaction");
        Path output = Path.of("target", "ui-test-lens", browserName(), "visual-move-" + UUID.randomUUID());
        ((JavascriptExecutor) driver).executeScript("""
                window.__visualBatchAttempts = [];
                window.__visualMoveDone = false;
                window.__visualBatchObserver = new MutationObserver(() => {
                  const batch = document.querySelector('[data-test-lens-visual-mask-batch]');
                  if (!batch) return;
                  const attempt = Number(batch.getAttribute('data-test-lens-visual-mask-attempt'));
                  if (!window.__visualBatchAttempts.includes(attempt)) window.__visualBatchAttempts.push(attempt);
                  if (attempt === 1 && !window.__visualMoveDone) {
                    window.__visualMoveDone = true;
                    const target = document.getElementById('customer-number');
                    target.style.marginLeft = '160px';
                    target.style.width = '280px';
                    target.style.height = '72px';
                  }
                });
                window.__visualBatchObserver.observe(document.body, {childList:true, subtree:true});
                """);
        WebElement original = driver.findElement(By.id("customer-number"));
        TestLens lens = strictSolidLens(output, By.id("customer-number"));
        ScreenshotCaptureResult result = lens.captureScreenshot("move-resize",
                ScreenshotCaptureOptions.builder().outputDirectory(output).includeTimestamp(false).build());
        ((JavascriptExecutor) driver).executeScript("window.__visualBatchObserver.disconnect()");

        assertTrue(result.isCaptured(), result.message());
        assertEquals(original, driver.findElement(By.id("customer-number")), "the same DOM node must have moved");
        @SuppressWarnings("unchecked")
        Map<String, Number> currentRect = (Map<String, Number>) ((JavascriptExecutor) driver).executeScript("""
                const r = document.getElementById('customer-number').getBoundingClientRect();
                return {left:r.left, top:r.top, width:r.width, height:r.height};
                """);
        assertTrue(regionColorRatio(ImageIO.read(result.path().toFile()), currentRect, 19, 87, 155) > 0.90,
                "same-node moved and resized bounding box must be masked");
        assertEquals(1L, number("return window.__visualBatchAttempts.includes(2) ? 1 : 0"));
        assertEquals(0L, number("return document.querySelectorAll('[data-test-lens-visual-mask]').length"));
    }

    @Test
    void visualRedactionBatchPerformanceDiagnostic() {
        open("/visual-redaction");
        Path output = Path.of("target", "ui-test-lens", browserName(), "visual-perf-" + UUID.randomUUID());
        for (int maskCount : List.of(1, 5, 20, 50)) {
            ((JavascriptExecutor) driver).executeScript("""
                    document.getElementById('visual-redaction-fixture').innerHTML = '';
                    const count = arguments[0];
                    for (let i = 0; i < count; i++) {
                      const target = document.createElement('div');
                      target.className = 'performance-secret';
                      target.textContent = 'synthetic-' + i;
                      target.style.cssText = 'width:120px;height:20px;margin:1px;background:#ddd';
                      document.getElementById('visual-redaction-fixture').appendChild(target);
                    }
                    """, maskCount);
            TestLens lens = strictSolidLens(output, By.cssSelector(".performance-secret"));
            long started = System.nanoTime();
            ScreenshotCaptureResult result = lens.captureScreenshot("batch-" + maskCount,
                    ScreenshotCaptureOptions.builder().outputDirectory(output).includeTimestamp(false).build());
            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            System.out.println("VISUAL_REDACTION_BATCH masks=" + maskCount + " elapsedMs=" + elapsedMs);
            assertTrue(result.isCaptured(), result.message());
            assertTrue(elapsedMs < 10_000, "batch capture should not introduce an absurd delay");
            assertEquals(0L, number("return document.querySelectorAll('[data-test-lens-visual-mask]').length"));
        }
    }

    private TestLens strictSolidLens(Path output, By locator) {
        VisualRedactionOptions visual = VisualRedactionOptions.builder().maskPasswordInputs(false)
                .mask(locator, VisualMaskMode.SOLID).solidColor("#13579B")
                .failurePolicy(VisualRedactionFailurePolicy.STRICT).build();
        return TestLens.attach(driver, TestLensOptions.builder().visualRedaction(visual).outputRoot(output).build());
    }

    @Test
    void strictVisualRedactionDoesNotPublishWhenRequiredTargetIsMissing() {
        open("/visual-redaction");
        Path output = Path.of("target", "ui-test-lens", browserName(), "visual-strict-" + UUID.randomUUID());
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder().visualRedaction(
                VisualRedactionOptions.builder().maskPasswordInputs(false)
                        .mask(By.id("missing-sensitive-target"), VisualMaskMode.SOLID)
                        .failurePolicy(VisualRedactionFailurePolicy.STRICT).build())
                .outputRoot(output).build());
        lens.startSession("strict-redaction-" + UUID.randomUUID());
        ScreenshotCaptureResult result = lens.captureScreenshot("strict-missing",
                ScreenshotCaptureOptions.builder().outputDirectory(output).includeTimestamp(false).build());
        assertFalse(result.isCaptured());
        assertEquals(0L, number("return document.querySelectorAll('[data-test-lens-visual-mask]').length"));
        assertFalse(Files.exists(output.resolve("screenshot_strict-missing.png")));
        TestLensFinalizationResult finalized = lens.finishFailed(new AssertionError("original test failure"));
        assertEquals(TraceStatus.FAILED, finalized.session().metadata().status());
        assertNull(finalized.failureScreenshot());
        assertFalse(finalized.diagnosticFailures().isEmpty(),
                "mask failure must be aggregated without replacing the original failure outcome");
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
        if (driver != null) {
            driver.quit();
            driver = null;
        }
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

    private void assertControlledFetchCompletesAfterRelease(TestLens lens) throws Exception {
        String gateId = UUID.randomUUID().toString();
        ControlledRequest gate = new ControlledRequest();
        assertNull(CONTROLLED_REQUESTS.putIfAbsent(gateId, gate));
        try {
            assertControlledFetchArmed(gateId);

            driver.findElement(By.id("controlled-fetch-trigger")).click();
            boolean arrived = gate.arrived.await(WAIT.toMillis(), TimeUnit.MILLISECONDS);
            assertTrue(arrived, () -> "controlled fetch did not reach the local HTTP handler; browser state="
                    + controlledFetchDiagnostic());

            assertControlledFetchActive();
            assertEquals(1L, gate.completed.getCount(),
                    "the controlled response must not be complete before its release");

            gate.release.countDown();
            lens.waitForNetworkIdle(Duration.ofMillis(100), Duration.ofSeconds(3));

            assertTrue(gate.completed.await(WAIT.toMillis(), TimeUnit.MILLISECONDS),
                    "network-idle wait returned before the controlled HTTP response completed");
            assertEquals(0L, number("return window.__uiTestLens.state.network.activeRequests;"));
            assertEquals("fulfilled", ((JavascriptExecutor) driver).executeScript("""
                    return window.__testLensControlledFetchState.status;
                    """));
        } finally {
            try {
                ((JavascriptExecutor) driver).executeScript("""
                        if (window.__testLensControlledFetch) {
                            window.__testLensControlledFetch.abort();
                            window.__testLensControlledFetch = null;
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
                                    "controlled HTTP handler did not finish after fetch cleanup");
                        }
                    } finally {
                        CONTROLLED_REQUESTS.remove(gateId, gate);
                    }
                }
            }
        }
    }

    private void assertControlledFetchArmed(String gateId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> armedState = (Map<String, Object>) ((JavascriptExecutor) driver).executeScript("""
                const trigger = document.getElementById('controlled-fetch-trigger');
                const controlledUrl = arguments[0];
                window.__testLensControlledFetchState = {
                    status: 'armed', rejectionName: null, rejectionMessage: null
                };
                trigger.onclick = () => {
                    const controller = new AbortController();
                    const state = {status: 'pending', rejectionName: null, rejectionMessage: null};
                    window.__testLensControlledFetch = controller;
                    window.__testLensControlledFetchState = state;
                    void fetch(controlledUrl, {signal: controller.signal}).then(
                        () => { state.status = 'fulfilled'; },
                        failure => {
                            state.status = 'rejected';
                            state.rejectionName = String(failure && failure.name || 'Error');
                            state.rejectionMessage = String(failure && failure.message || '');
                        });
                };
                const network = window.__uiTestLens
                    && window.__uiTestLens.state
                    && window.__uiTestLens.state.network;
                return {
                    trackerInstalled: Boolean(network && network.trackerInstalled),
                    activeRequests: Number(network && network.activeRequests),
                    fetchStatus: window.__testLensControlledFetchState.status
                };
                """, baseUrl + "/wait-controlled/" + gateId);
        assertEquals(Boolean.TRUE, armedState.get("trackerInstalled"),
                "XHR/fetch tracker was not installed when the controlled trigger was armed");
        assertEquals(0L, ((Number) armedState.get("activeRequests")).longValue(),
                "arming the controlled trigger must not start a request");
        assertEquals("armed", armedState.get("fetchStatus"));
    }

    private void assertControlledFetchActive() {
        Map<String, Object> activeState = controlledFetchState();
        assertEquals(Boolean.TRUE, activeState.get("trackerInstalled"));
        assertEquals(1L, ((Number) activeState.get("activeRequests")).longValue(),
                "tracker must observe the controlled fetch while its response is blocked");
        assertEquals("pending", activeState.get("fetchStatus"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> controlledFetchState() {
        return (Map<String, Object>) ((JavascriptExecutor) driver).executeScript("""
                const network = window.__uiTestLens.state.network;
                const fetchState = window.__testLensControlledFetchState || {};
                return {
                    trackerInstalled: Boolean(network && network.trackerInstalled),
                    activeRequests: Number(network && network.activeRequests),
                    fetchStatus: fetchState.status,
                    rejectionName: fetchState.rejectionName,
                    rejectionMessage: fetchState.rejectionMessage
                };
                """);
    }

    private String controlledFetchDiagnostic() {
        Object diagnostic = ((JavascriptExecutor) driver).executeScript("""
                const state = window.__testLensControlledFetchState || {};
                const network = window.__uiTestLens
                    && window.__uiTestLens.state
                    && window.__uiTestLens.state.network;
                return 'status=' + String(state.status || 'missing')
                    + ', rejection=' + String(state.rejectionName || 'none')
                    + ', message=' + String(state.rejectionMessage || 'none')
                    + ', activeRequests=' + String(network && network.activeRequests);
                """);
        return String.valueOf(diagnostic);
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
        return BrowserTestHarness.createDriver(pageLoadStrategy);
    }

    private static String browserName() {
        return BrowserTestHarness.browserName();
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
                    <button id='controlled-fetch-trigger' type='button'>Start controlled fetch</button>
                    """), false);
            case "/full-page" -> html(exchange, page("Full-page screenshot", """
                    <div id='full-page-document'>
                      <header id='full-page-fixed'>Fixed header</header>
                      <section id='full-page-top'><span class='marker'>Top</span></section>
                      <section id='full-page-middle'><aside id='full-page-sticky'>Sticky</aside><span class='marker'>Middle</span></section>
                      <section id='full-page-bottom'><span class='marker'>Bottom</span></section>
                      <div id='full-page-animated'>Animated</div>
                      <div id='full-page-transition'>Transition</div>
                      <input id='full-page-caret' value='caret'>
                      <div id='full-page-right'>Right</div>
                      <iframe id='full-page-frame' src='/frame'></iframe>
                    </div>
                    """), true);
            case "/visual-redaction" -> html(exchange, page("Visual redaction", """
                    <div id='visual-redaction-fixture'>
                      <label>Password <input id='password-secret' type='password' value='THIS_MUST_NOT_APPEAR_7F3A'></label>
                      <label>Customer number <input id='customer-number' value='customer-004291'></label>
                      <div id='customer-email'>alice.private@example.test</div>
                      <div id='visual-redaction-spacer'></div>
                      <div id='far-secret'>OFFSCREEN_SECRET_7F3A</div>
                    </div>
                    """), false);
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
            #full-page-animated { animation: full-page-pulse 1s linear infinite; }
            #full-page-transition { width: 100px; transition: width 2s ease; }
            @keyframes full-page-pulse { from { opacity: .35; } to { opacity: 1; } }
            #full-page-fixed { position: fixed; left: 10px; top: 10px; width: 220px; height: 44px;
              z-index: 8; background: rgb(230, 230, 0); }
            #full-page-sticky { position: sticky; top: 80px; width: 180px; height: 48px; background: rgb(0, 220, 220); }
            #full-page-right { position: absolute; left: 1680px; top: 980px; width: 100px; height: 100px;
              background: rgb(255, 165, 0); }
            #visual-redaction-fixture { width: 640px; }
            #visual-redaction-fixture label, #customer-email { display: block; margin: 18px 0; }
            #visual-redaction-fixture input, #customer-email, #far-secret { width: 360px; height: 48px;
              box-sizing: border-box; padding: 12px; font: 20px monospace; color: rgb(15, 25, 35);
              background: repeating-linear-gradient(90deg, rgb(245,245,245) 0 5px, rgb(210,220,235) 5px 10px); }
            #visual-redaction-spacer { height: 1500px; }
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

    private static int countColorClusters(BufferedImage image, int red, int green, int blue,
                                          int minimumPixels, int mergeGap) {
        int width = image.getWidth(), height = image.getHeight();
        int expected = (red << 16) | (green << 8) | blue;
        boolean[] visited = new boolean[Math.multiplyExact(width, height)];
        int[] queue = new int[visited.length];
        List<int[]> bounds = new java.util.ArrayList<>();
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int start = y * width + x;
            if (visited[start] || (image.getRGB(x, y) & 0x00ffffff) != expected) continue;
            int head = 0, tail = 0, pixels = 0;
            int minX = x, minY = y, maxX = x, maxY = y;
            visited[start] = true;
            queue[tail++] = start;
            while (head < tail) {
                int current = queue[head++];
                int currentX = current % width, currentY = current / width;
                pixels++;
                minX = Math.min(minX, currentX);
                minY = Math.min(minY, currentY);
                maxX = Math.max(maxX, currentX);
                maxY = Math.max(maxY, currentY);
                if (currentX > 0) tail = enqueueMatching(image, expected, visited, queue, tail, current - 1);
                if (currentX + 1 < width) tail = enqueueMatching(image, expected, visited, queue, tail, current + 1);
                if (currentY > 0) tail = enqueueMatching(image, expected, visited, queue, tail, current - width);
                if (currentY + 1 < height) tail = enqueueMatching(image, expected, visited, queue, tail, current + width);
            }
            if (pixels >= minimumPixels) bounds.add(new int[]{minX, minY, maxX, maxY});
        }
        int[] parents = new int[bounds.size()];
        for (int i = 0; i < parents.length; i++) parents[i] = i;
        for (int i = 0; i < bounds.size(); i++) for (int j = i + 1; j < bounds.size(); j++) {
            int[] a = bounds.get(i), b = bounds.get(j);
            int horizontalGap = Math.max(0, Math.max(a[0] - b[2], b[0] - a[2]));
            int verticalGap = Math.max(0, Math.max(a[1] - b[3], b[1] - a[3]));
            if (horizontalGap <= mergeGap && verticalGap <= mergeGap) {
                parents[findRoot(parents, j)] = findRoot(parents, i);
            }
        }
        int clusters = 0;
        for (int i = 0; i < parents.length; i++) if (findRoot(parents, i) == i) clusters++;
        return clusters;
    }

    private static int findRoot(int[] parents, int index) {
        while (parents[index] != index) {
            parents[index] = parents[parents[index]];
            index = parents[index];
        }
        return index;
    }

    private static int enqueueMatching(BufferedImage image, int expected, boolean[] visited,
                                       int[] queue, int tail, int index) {
        if (visited[index]) return tail;
        visited[index] = true;
        int x = index % image.getWidth(), y = index / image.getWidth();
        if ((image.getRGB(x, y) & 0x00ffffff) == expected) queue[tail++] = index;
        return tail;
    }

    private long regionDifference(BufferedImage before, BufferedImage after, Map<String, Number> rect) {
        double scaleX = after.getWidth() / (double) number("return window.innerWidth");
        double scaleY = after.getHeight() / (double) number("return window.innerHeight");
        int left = (int) Math.max(0, Math.round(rect.get("left").doubleValue() * scaleX));
        int top = (int) Math.max(0, Math.round(rect.get("top").doubleValue() * scaleY));
        int right = (int) Math.min(after.getWidth(), Math.round((rect.get("left").doubleValue()
                + rect.get("width").doubleValue()) * scaleX));
        int bottom = (int) Math.min(after.getHeight(), Math.round((rect.get("top").doubleValue()
                + rect.get("height").doubleValue()) * scaleY));
        long changed = 0;
        for (int y = top; y < bottom; y++) for (int x = left; x < right; x++) {
            if (before.getRGB(x, y) != after.getRGB(x, y)) changed++;
        }
        return changed;
    }

    private double regionColorRatio(BufferedImage image, Map<String, Number> rect, int red, int green, int blue) {
        double scaleX = image.getWidth() / (double) number("return window.innerWidth");
        double scaleY = image.getHeight() / (double) number("return window.innerHeight");
        int left = (int) Math.max(0, Math.round(rect.get("left").doubleValue() * scaleX));
        int top = (int) Math.max(0, Math.round(rect.get("top").doubleValue() * scaleY));
        int right = (int) Math.min(image.getWidth(), Math.round((rect.get("left").doubleValue()
                + rect.get("width").doubleValue()) * scaleX));
        int bottom = (int) Math.min(image.getHeight(), Math.round((rect.get("top").doubleValue()
                + rect.get("height").doubleValue()) * scaleY));
        int expected = (red << 16) | (green << 8) | blue;
        long matching = 0, total = 0;
        for (int y = top; y < bottom; y++) for (int x = left; x < right; x++) {
            total++;
            if ((image.getRGB(x, y) & 0x00ffffff) == expected) matching++;
        }
        return total == 0 ? 0 : matching / (double) total;
    }

    private double fullPageRegionColorRatio(BufferedImage image, Map<String, Number> rect,
                                            int red, int green, int blue) {
        double scaleX = image.getWidth() / (double) number("return document.documentElement.scrollWidth");
        double scaleY = image.getHeight() / (double) number("return document.documentElement.scrollHeight");
        int left = (int) Math.max(0, Math.round(rect.get("left").doubleValue() * scaleX));
        int top = (int) Math.max(0, Math.round(rect.get("top").doubleValue() * scaleY));
        int right = (int) Math.min(image.getWidth(), Math.round((rect.get("left").doubleValue()
                + rect.get("width").doubleValue()) * scaleX));
        int bottom = (int) Math.min(image.getHeight(), Math.round((rect.get("top").doubleValue()
                + rect.get("height").doubleValue()) * scaleY));
        int expected = (red << 16) | (green << 8) | blue;
        long matching = 0, total = 0;
        for (int y = top; y < bottom; y++) for (int x = left; x < right; x++) {
            total++;
            if ((image.getRGB(x, y) & 0x00ffffff) == expected) matching++;
        }
        return total == 0 ? 0 : matching / (double) total;
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
