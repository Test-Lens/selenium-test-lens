package io.github.testlens.browser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class HudStudioDocumentationLayoutIT {
    private WebDriver driver;

    @AfterEach
    void closeDriver() {
        if (driver != null) driver.quit();
    }

    @Test
    void versionedStudioUsesAvailableWidthWithoutDocumentOverflow() {
        String root = System.getProperty("hudStudioDocsUrl");
        assumeTrue(root != null && !root.isBlank(), "-DhudStudioDocsUrl points to a built versioned docs root");
        driver = BrowserTestHarness.createDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        double desktopMain = 0;

        for (Dimension viewport : List.of(new Dimension(1920, 1000), new Dimension(1366, 900), new Dimension(500, 844))) {
            driver.manage().window().setSize(viewport);
            driver.get(root + "observability/hud-studio/");
            System.out.printf("HUD_STUDIO_PAGE url=%s title=%s body=%s%n", driver.getCurrentUrl(), driver.getTitle(),
                    js.executeScript("return document.body && document.body.className"));
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(ignored -> Boolean.TRUE.equals(js.executeScript("""
                    const frame=document.querySelector('[data-studio-frame]');
                    return document.body.classList.contains('tl-hud-studio-page') && Boolean(frame);
                    """)));
            WebElement frame = driver.findElement(By.cssSelector("[data-studio-frame]"));
            @SuppressWarnings("unchecked")
            Map<String, Number> metrics = (Map<String, Number>) js.executeScript("""
                    const frame=document.querySelector('[data-studio-frame]');
                    const viewport=document.documentElement.clientWidth;
                    const main=document.querySelector('.md-main__inner').getBoundingClientRect().width;
                    const article=document.querySelector('.md-content').getBoundingClientRect().width;
                    const host=document.querySelector('[data-studio-host]').getBoundingClientRect().width;
                    return {viewport,main,article,studio:host,utilization:host/article*100,
                      documentOverflow:document.documentElement.scrollWidth-viewport,
                      frameWidth:frame.getBoundingClientRect().width};
                    """);
            driver.switchTo().frame(frame);
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(ignored -> !driver.findElements(By.cssSelector(".studio")).isEmpty());
            @SuppressWarnings("unchecked")
            Map<String, Number> frameMetrics = (Map<String, Number>) js.executeScript("""
                    const studio=document.querySelector('.studio');
                    return {frameOverflow:document.documentElement.scrollWidth-document.documentElement.clientWidth,
                      innerStudio:studio.getBoundingClientRect().width, frameWidth:document.documentElement.clientWidth};
                    """);
            driver.switchTo().defaultContent();
            System.out.printf("HUD_STUDIO_LAYOUT viewport=%.0f main=%.1f article=%.1f studio=%.1f utilization=%.1f%% documentOverflow=%.1f frameOverflow=%.1f%n",
                    number(metrics, "viewport"), number(metrics, "main"), number(metrics, "article"), number(metrics, "studio"),
                    number(metrics, "utilization"), number(metrics, "documentOverflow"), number(frameMetrics, "frameOverflow"));
            assertTrue(number(metrics, "utilization") > (viewport.getWidth() >= 1366 ? 95 : 92),
                    "Studio must use the available article width");
            assertTrue(Math.abs(number(frameMetrics, "innerStudio") - number(frameMetrics, "frameWidth")) < 2,
                    "Configurator root must fill its iframe");
            assertTrue(number(metrics, "documentOverflow") <= 1, "Documentation must not overflow horizontally");
            assertTrue(number(frameMetrics, "frameOverflow") <= 1, "Studio iframe must not overflow horizontally");
            if (viewport.getWidth() >= 1366) {
                assertTrue(number(metrics, "main") > 1200, "Studio page must not retain the normal article max-width");
            }
            if (viewport.getWidth() == 1920) desktopMain = number(metrics, "main");
        }

        driver.manage().window().setSize(new Dimension(1920, 1000));
        driver.get(root + "observability/visual-diagnostics/");
        assertFalse(Boolean.TRUE.equals(js.executeScript("return document.body.classList.contains('tl-hud-studio-page')")));
        double ordinaryMain = ((Number) js.executeScript("return document.querySelector('.md-main__inner').getBoundingClientRect().width")).doubleValue();
        System.out.printf("HUD_STUDIO_LAYOUT ordinaryPageMain=%.1f%n", ordinaryMain);
        assertTrue(ordinaryMain < desktopMain * 0.8,
                "Ordinary documentation pages must keep the Material max-width");
    }

    @Test
    void versionedStudioReplayAdvancesSemanticRowsWithSourceNavigationOffAndOn() {
        String root = System.getProperty("hudStudioDocsUrl");
        assumeTrue(root != null && !root.isBlank(), "-DhudStudioDocsUrl points to a built versioned docs root");
        driver = BrowserTestHarness.createDriver();
        driver.manage().window().setSize(new Dimension(1440, 1000));

        driver.get(root + "demo/hud-studio/");
        awaitStudioReady();
        exerciseReplay(false);

        driver.get(root + "observability/hud-studio/");
        WebDriverWait wait = waitForStudio();
        wait.until(ignored -> !driver.findElements(By.cssSelector("[data-studio-frame]")).isEmpty());
        driver.switchTo().frame(driver.findElement(By.cssSelector("[data-studio-frame]")));
        awaitStudioReady();
        exerciseReplay(true);
    }

    private void exerciseReplay(boolean sourceNavigation) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebDriverWait wait = waitForStudio();
        WebElement standard = driver.findElement(By.cssSelector("[data-preset='STANDARD']"));
        if (!"true".equals(standard.getAttribute("aria-pressed"))) standard.click();
        WebElement enabled = driver.findElement(By.id("source-navigation-enabled"));
        if (enabled.isSelected() != sourceNavigation) enabled.click();
        if (sourceNavigation) {
            WebElement active = driver.findElement(By.id("source-navigation-preview-active"));
            if (!active.isSelected()) active.click();
        }

        driver.findElement(By.id("replay")).click();
        driver.switchTo().frame(driver.findElement(By.id("preview-frame")));
        Map<String, Object> started = awaitReplay(snapshot -> "RUNNING".equals(snapshot.get("action")));
        String firstRun = started.get("runId").toString();
        System.out.printf("HUD_STUDIO_REPLAY sourceNavigation=%s state=started snapshot=%s%n", sourceNavigation, started);
        assertTrue(started.get("step").toString().endsWith("ACTION Place order"), started.toString());
        assertTrue(started.get("actionId").toString().startsWith(firstRun));

        Map<String, Object> actionPassed = awaitReplay(snapshot -> "PASSED".equals(snapshot.get("action")));
        assertEquals(firstRun, actionPassed.get("runId"));
        if (sourceNavigation) {
            WebElement focusTarget = driver.findElement(By.id("preview-order"));
            focusTarget.click();
            focusTarget.sendKeys(Keys.F8);
            awaitReplay(snapshot -> "none".equals(snapshot.get("sourceDisplay")));
            focusTarget.sendKeys(Keys.F8);
            awaitReplay(snapshot -> "block".equals(snapshot.get("sourceDisplay")));
        }

        awaitReplay(snapshot -> "RUNNING".equals(snapshot.get("confirmation")));
        awaitReplay(snapshot -> "RETRYING".equals(snapshot.get("confirmation")));
        Map<String, Object> confirmationPassed = awaitReplay(snapshot -> "PASSED".equals(snapshot.get("confirmation")));
        System.out.printf("HUD_STUDIO_REPLAY sourceNavigation=%s state=confirmation-passed snapshot=%s%n", sourceNavigation, confirmationPassed);
        if (sourceNavigation) assertEquals("CheckoutTest.java:42", confirmationPassed.get("confirmationSource"));
        Map<String, Object> nextOperation = awaitReplay(snapshot -> "RUNNING".equals(snapshot.get("orderNumber")));
        assertNotEquals(nextOperation.get("confirmationId"), nextOperation.get("orderNumberId"));
        Map<String, Object> failed = awaitReplay(snapshot -> "FAILED".equals(snapshot.get("orderNumber")));
        if (sourceNavigation) assertEquals("OrderAssertions.java:116", failed.get("orderNumberSource"));
        Map<String, Object> complete = awaitReplay(snapshot -> "complete".equals(snapshot.get("replayState")));
        System.out.printf("HUD_STUDIO_REPLAY sourceNavigation=%s state=complete snapshot=%s%n", sourceNavigation, complete);
        assertTrue(complete.get("step").toString().endsWith("Replay complete"), complete.toString());
        assertEquals("PASSED", complete.get("action"));
        assertEquals("PASSED", complete.get("confirmation"));
        assertEquals("FAILED", complete.get("orderNumber"));
        if (sourceNavigation) assertEquals("CheckoutPage.java:87", complete.get("actionSource"));
        else assertEquals(null, complete.get("sourceDisplay"));

        driver.switchTo().parentFrame();
        WebElement eventLog = driver.findElement(By.cssSelector("[data-key='showEventLog']"));
        eventLog.click();
        driver.switchTo().frame(driver.findElement(By.id("preview-frame")));
        wait.until(ignored -> Boolean.FALSE.equals(js.executeScript("return !!window.__seleniumOverlayRoot.querySelector('#selenium-hud-logs')")));
        assertEquals(firstRun, replaySnapshot().get("runId"));
        driver.switchTo().parentFrame();
        driver.findElement(By.cssSelector("[data-key='showEventLog']")).click();
        js.executeScript("""
                const width=document.getElementById('width');
                width.value=String(Number(width.value)-10);width.dispatchEvent(new Event('input',{bubbles:true}));
                const accent=document.querySelector('[data-color-picker="accent"]');
                accent.value='#7c3aed';accent.dispatchEvent(new Event('input',{bubbles:true}));
                """);
        driver.switchTo().frame(driver.findElement(By.id("preview-frame")));
        Map<String, Object> restoredLog = awaitReplay(snapshot -> "complete".equals(snapshot.get("replayState"))
                && "PASSED".equals(snapshot.get("action")) && "FAILED".equals(snapshot.get("orderNumber")));
        assertEquals(firstRun, restoredLog.get("runId"));

        for (String preset : List.of("COMPACT", "DEBUG")) {
            driver.switchTo().parentFrame();
            driver.findElement(By.cssSelector("[data-preset='" + preset + "']")).click();
            driver.switchTo().frame(driver.findElement(By.id("preview-frame")));
            Map<String, Object> preserved = awaitReplay(snapshot -> "complete".equals(snapshot.get("replayState"))
                    && "PASSED".equals(snapshot.get("action")) && "FAILED".equals(snapshot.get("orderNumber")));
            assertEquals(firstRun, preserved.get("runId"));
        }
        driver.switchTo().parentFrame();
        driver.findElement(By.cssSelector("[data-preset='MINIMAL']")).click();
        driver.switchTo().frame(driver.findElement(By.id("preview-frame")));
        wait.until(ignored -> Boolean.FALSE.equals(js.executeScript("return !!window.__seleniumOverlayRoot.querySelector('#selenium-hud-logs')")));
        assertEquals(firstRun, replaySnapshot().get("runId"));
        driver.switchTo().parentFrame();
        driver.findElement(By.cssSelector("[data-preset='STANDARD']")).click();
        driver.switchTo().frame(driver.findElement(By.id("preview-frame")));
        Map<String, Object> restoredPreset = awaitReplay(snapshot -> "complete".equals(snapshot.get("replayState"))
                && "PASSED".equals(snapshot.get("action")) && "FAILED".equals(snapshot.get("orderNumber")));
        assertEquals(firstRun, restoredPreset.get("runId"));

        if (sourceNavigation) {
            js.executeScript("window.__seleniumOverlayRoot.querySelector('[data-category=\"ACTION\"] .stl-hud-source-location').click()");
            assertTrue(driver.findElement(By.id("source-navigation-demo-status")).getText().contains("No external application was launched"));
        }

        driver.switchTo().parentFrame();
        driver.findElement(By.id("replay")).click();
        driver.switchTo().frame(driver.findElement(By.id("preview-frame")));
        Map<String, Object> secondStarted = awaitReplay(snapshot -> "RUNNING".equals(snapshot.get("action")));
        String interruptedRun = secondStarted.get("runId").toString();
        assertNotEquals(firstRun, interruptedRun);
        driver.switchTo().parentFrame();
        driver.findElement(By.id("replay")).click();
        driver.switchTo().frame(driver.findElement(By.id("preview-frame")));
        Map<String, Object> replacement = awaitReplay(snapshot -> "RUNNING".equals(snapshot.get("action"))
                && !interruptedRun.equals(snapshot.get("runId")));
        String replacementRun = replacement.get("runId").toString();
        Map<String, Object> replacementComplete = awaitReplay(snapshot -> "complete".equals(snapshot.get("replayState")));
        System.out.printf("HUD_STUDIO_REPLAY sourceNavigation=%s state=replacement-complete snapshot=%s%n", sourceNavigation, replacementComplete);
        assertEquals(replacementRun, replacementComplete.get("runId"));
        assertTrue(((List<?>) replacementComplete.get("operationIds")).stream()
                .allMatch(id -> id.toString().startsWith(replacementRun)), replacementComplete.toString());
        driver.switchTo().parentFrame();
    }

    private void awaitStudioReady() {
        waitForStudio().until(ignored -> Boolean.TRUE.equals(((JavascriptExecutor) driver)
                .executeScript("return document.body.dataset.hudReady==='true'")));
    }

    private Map<String, Object> awaitReplay(java.util.function.Predicate<Map<String, Object>> condition) {
        return waitForStudio().until(ignored -> {
            Map<String, Object> snapshot = replaySnapshot();
            return condition.test(snapshot) ? snapshot : null;
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> replaySnapshot() {
        return (Map<String, Object>) ((JavascriptExecutor) driver).executeScript("""
                const root=window.__seleniumOverlayRoot;
                const panel=root.querySelector('#selenium-hud-panel');
                const row=suffix=>Array.from(root.querySelectorAll('.stl-hud-event')).find(item=>(item.dataset.operationId||'').endsWith(suffix));
                const action=row('action-place-order'),confirmation=row('assert-confirmation'),order=row('assert-order-number');
                const source=item=>item&&item.querySelector('.stl-hud-source-location');
                const firstSource=root.querySelector('.stl-hud-source-location');
                return {runId:panel.dataset.studioReplayId,replayState:panel.dataset.studioReplayState,
                  step:(root.querySelector('#selenium-hud-step')||{}).textContent||'',
                  action:action&&action.dataset.phase,actionId:action&&action.dataset.operationId,actionSource:source(action)&&source(action).textContent,
                  confirmation:confirmation&&confirmation.dataset.phase,confirmationId:confirmation&&confirmation.dataset.operationId,confirmationSource:source(confirmation)&&source(confirmation).textContent,
                  orderNumber:order&&order.dataset.phase,orderNumberId:order&&order.dataset.operationId,orderNumberSource:source(order)&&source(order).textContent,
                  sourceDisplay:firstSource&&getComputedStyle(firstSource).display,
                  operationIds:Array.from(root.querySelectorAll('.stl-hud-event[data-operation-id]')).map(item=>item.dataset.operationId)};
                """);
    }

    private WebDriverWait waitForStudio() {
        return new WebDriverWait(driver, Duration.ofSeconds(12), Duration.ofMillis(25));
    }

    private static double number(Map<String, Number> values, String key) {
        return values.get(key).doubleValue();
    }
}
