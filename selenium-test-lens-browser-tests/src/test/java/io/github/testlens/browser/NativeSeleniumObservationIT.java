package io.github.testlens.browser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.testlens.TestLens;
import io.github.testlens.TestLensOptions;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.hud.HudOptions;
import io.github.testlens.hud.HudPreset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NativeSeleniumObservationIT {
    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", NativeSeleniumObservationIT::serve);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    @AfterAll static void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test void nativeSeleniumRemainsExactlyOnceAcrossDriverElementAndDerivedContexts() {
        WebDriver raw = BrowserTestHarness.createDriver();
        try {
            TestLens lens = TestLens.attach(raw, TestLensOptions.builder()
                    .hud(HudOptions.builder().preset(HudPreset.DEBUG).build()).build());
            WebDriver driver = lens.observeDriver();
            UiTestLensSession session = lens.startSession("native selenium observation");

            assertTrue(driver instanceof JavascriptExecutor);
            assertTrue(driver instanceof TakesScreenshot);
            assertTrue(driver instanceof Interactive);
            assertTrue(driver instanceof HasCapabilities);
            if (raw instanceof HasBiDi) assertTrue(driver instanceof HasBiDi);

            driver.get(baseUrl);
            Page page = new Page();
            PageFactory.initElements(driver, page);

            page.email.clear();
            page.email.sendKeys("person@example.test");
            assertEquals(page.email, driver.switchTo().activeElement());
            page.save.click();
            assertEquals(1L, number(driver, "return window.counts.save"));
            assertEquals("person@example.test", page.email.getAttribute("value"));

            driver.findElement(By.id("parent")).findElement(By.className("child")).click();
            driver.findElements(By.cssSelector(".item")).get(1).click();
            assertEquals(1L, number(driver, "return window.counts.child"));
            assertEquals(1L, number(driver, "return window.counts.item"));

            WebElement dynamic = new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.elementToBeClickable(By.id("dynamic")));
            dynamic.click();
            assertEquals(1L, number(driver, "return window.counts.dynamic"));

            new Actions(driver).moveToElement(page.save).click().perform();
            assertEquals(2L, number(driver, "return window.counts.save"));

            WebElement labelled = lens.observe(page.jsButton, "JavaScript Save");
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", labelled);
            assertEquals(1L, number(driver, "return window.counts.js"));

            Object async = ((JavascriptExecutor) driver).executeAsyncScript(
                    "const done=arguments[arguments.length-1]; setTimeout(()=>done('complete'),30);");
            assertEquals("complete", async);

            SearchContext shadow = driver.findElement(By.id("shadow-host")).getShadowRoot();
            shadow.findElement(By.cssSelector("button")).click();
            assertEquals(1L, number(driver, "return window.counts.shadow"));

            driver.switchTo().frame(driver.findElement(By.id("fixture-frame")));
            driver.findElement(By.id("frame-button")).click();
            assertEquals("1", driver.findElement(By.id("frame-count")).getText());
            driver.switchTo().defaultContent();

            driver.switchTo().frame(0);
            assertEquals("1", driver.findElement(By.id("frame-count")).getText());
            driver.switchTo().parentFrame();
            driver.switchTo().frame("checkout");
            assertEquals("1", driver.findElement(By.id("frame-count")).getText());
            driver.switchTo().defaultContent();
            assertThrows(NoSuchFrameException.class, () -> driver.switchTo().frame("missing-frame"));
            assertNotNull(driver.findElement(By.id("save")), "failed switch must leave context unchanged");

            String originalWindow = driver.getWindowHandle();
            driver.switchTo().newWindow(WindowType.TAB);
            driver.get(baseUrl);
            assertNotNull(driver.findElement(By.id("save")));
            driver.close();
            driver.switchTo().window(originalWindow);
            assertNotNull(driver.findElement(By.id("save")));

            ElementClickInterceptedException intercepted = assertThrows(ElementClickInterceptedException.class,
                    () -> driver.findElement(By.id("covered")).click());
            assertNotNull(intercepted);
            assertEquals(0L, number(driver, "return window.counts.covered"),
                    "native observation must not introduce Smart Click or JavaScript fallback");

            WebElement rawSave = raw.findElement(By.id("save"));
            ((JavascriptExecutor) raw).executeScript("arguments[0].click();", lens.observe(rawSave, "Raw executor target"));
            assertEquals(3L, number(driver, "return window.counts.save"));

            List<?> nativeActions = session.events().stream()
                    .filter(event -> event.attributes().getOrDefault("action", "").startsWith("selenium."))
                    .toList();
            assertFalse(nativeActions.isEmpty());
            assertTrue(session.events().stream().anyMatch(event -> "NativeSeleniumObservationIT.java".equals(
                    event.attributes().get("metadata.source.fileName"))),
                    "source navigation must resolve the consumer test rather than decorator internals");
            assertTrue(session.events().stream().noneMatch(event -> event.message().contains("person@example.test")));
            String structured = session.exportJson();
            assertTrue(structured.contains("\"locatorObservation\""));
            assertTrue(structured.contains("\"kind\":\"ELEMENT\""));
            assertTrue(structured.contains("\"kind\":\"SHADOW_ROOT\""));
            assertTrue(structured.contains("\"kind\":\"FRAME\""));
            assertTrue(structured.contains("\"kind\":\"WINDOW\""));
            assertTrue(structured.contains("\"kind\":\"INDEX\",\"value\":0"));
            assertTrue(structured.contains("\"kind\":\"NAME_OR_ID\",\"value\":\"checkout\""));
            assertTrue(structured.contains("\"kind\":\"SESSION_LOCAL_HANDLE\""));
            assertTrue(structured.contains("\"declarationSource\":{\"knowledge\":\"UNKNOWN\"}"));
            assertFalse(structured.contains("metadata.testlens.selector"));

            lens.finishPassed();
        } finally {
            raw.quit();
        }
    }

    private static long number(WebDriver driver, String script) {
        return ((Number) ((JavascriptExecutor) driver).executeScript(script)).longValue();
    }

    private static void serve(HttpExchange exchange) throws IOException {
        byte[] body = HTML.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static final class Page {
        @FindBy(id = "email") WebElement email;
        @FindBy(id = "save") WebElement save;
        @FindBy(id = "js-button") WebElement jsButton;
    }

    private static final String HTML = """
            <!doctype html><html><body>
            <input id='email'><button id='save'>Save</button><button id='js-button'>JS</button>
            <div id='parent'><button class='child'>Child</button></div>
            <button class='item'>One</button><button class='item'>Two</button>
            <div style='position:relative;width:140px;height:40px'>
              <button id='covered' style='position:absolute;inset:0'>Covered</button>
              <div id='blocker' style='position:absolute;inset:0;z-index:2;background:#ddd'>Blocker</div>
            </div>
            <div id='dynamic-slot'></div><div id='shadow-host'></div>
            <iframe id='fixture-frame' name='checkout' srcdoc="<button id='frame-button' style='margin:60px' onclick='document.getElementById(&quot;frame-count&quot;).textContent=1'>Frame</button><span id='frame-count'>0</span>"></iframe>
            <script>
              window.counts={save:0,child:0,item:0,dynamic:0,js:0,shadow:0,covered:0};
              save.onclick=()=>counts.save++; document.querySelector('.child').onclick=()=>counts.child++;
              document.querySelectorAll('.item').forEach(x=>x.onclick=()=>counts.item++);
              document.getElementById('js-button').onclick=()=>counts.js++;
              document.getElementById('covered').onclick=()=>counts.covered++;
              const root=document.getElementById('shadow-host').attachShadow({mode:'open'});
              root.innerHTML='<button id="shadow-button">Shadow</button>';
              root.querySelector('button').onclick=()=>counts.shadow++;
              setTimeout(()=>{const b=document.createElement('button');b.id='dynamic';b.textContent='Dynamic';
                b.onclick=()=>counts.dynamic++;document.getElementById('dynamic-slot').appendChild(b);},150);
            </script></body></html>
            """;
}
