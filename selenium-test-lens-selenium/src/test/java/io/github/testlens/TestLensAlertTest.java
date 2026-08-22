package io.github.testlens;

import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.selenium.locator.UiLocatorOptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TestLensAlertTest {
    @Test void readsAcceptsDismissesAndFillsWithoutTracingPromptValue() {
        AlertBrowser browser = new AlertBrowser();
        TestLens lens = lens(browser);
        UiTestLensSession session = lens.startSession("alerts");

        assertEquals("Are you sure?", lens.alert().waitUntilPresent().text());
        lens.alert().fill("secret-token");
        lens.alert().accept();
        browser.present.set(true);
        lens.alert().dismiss();

        assertEquals("secret-token", browser.promptValue.get());
        assertTrue(browser.accepted.get());
        assertTrue(browser.dismissed.get());
        assertTrue(session.events().stream().anyMatch(e -> "alert.accept".equals(e.attributes().get("action"))));
        assertTrue(session.events().stream().noneMatch(e -> e.toString().contains("secret-token")));
    }

    @Test void missingAlertKeepsOriginalSeleniumFailure() {
        AlertBrowser browser = new AlertBrowser();
        browser.present.set(false);
        TestLens lens = lens(browser);
        lens.startSession("missing alert");
        assertThrows(NoAlertPresentException.class, () -> lens.alert().text());
    }

    private static TestLens lens(AlertBrowser browser) {
        return TestLens.attach(browser.driver(), TestLensOptions.builder()
                .overlayConfig(OverlayConfig.builder().enabled(false).build())
                .locatorOptions(UiLocatorOptions.builder().timeout(Duration.ofMillis(60)).pollInterval(Duration.ofMillis(5)).build())
                .build());
    }

    private static final class AlertBrowser {
        final AtomicBoolean present = new AtomicBoolean(true);
        final AtomicBoolean accepted = new AtomicBoolean();
        final AtomicBoolean dismissed = new AtomicBoolean();
        final AtomicReference<String> promptValue = new AtomicReference<>();

        WebDriver driver() {
            Alert alert = (Alert) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Alert.class}, (p, m, a) -> switch (m.getName()) {
                case "getText" -> "Are you sure?";
                case "accept" -> { accepted.set(true); present.set(false); yield null; }
                case "dismiss" -> { dismissed.set(true); present.set(false); yield null; }
                case "sendKeys" -> { promptValue.set((String) a[0]); yield null; }
                default -> null;
            });
            WebDriver.TargetLocator target = (WebDriver.TargetLocator) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{WebDriver.TargetLocator.class}, (p, m, a) -> {
                        if (m.getName().equals("alert")) {
                            if (!present.get()) throw new NoAlertPresentException();
                            return alert;
                        }
                        return null;
                    });
            return (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{WebDriver.class, JavascriptExecutor.class}, (p, m, a) -> switch (m.getName()) {
                        case "switchTo" -> target;
                        case "executeScript", "executeAsyncScript" -> null;
                        default -> m.getReturnType() == boolean.class ? false : null;
                    });
        }
    }
}
