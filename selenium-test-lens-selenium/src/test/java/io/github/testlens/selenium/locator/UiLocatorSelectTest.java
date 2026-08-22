package io.github.testlens.selenium.locator;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class UiLocatorSelectTest {
    @Test void selectsByVisibleTextValueAndIndexAndReadsSelection() {
        Option poland = new Option("Poland", "PL", "0");
        Option germany = new Option("Germany", "DE", "1");
        Fixture fixture = fixture(List.of(poland, germany), true);
        UiTestLensSession session = fixture.overlay.startSession("select");
        UiLocator country = fixture.locator();

        country.selectByVisibleText("Poland");
        assertEquals("Poland", country.selectedText());
        assertEquals("PL", country.selectedValue());
        poland.selected.set(false);
        country.selectByValue("DE");
        assertTrue(germany.selected.get());
        germany.selected.set(false);
        country.selectByIndex(1);
        assertTrue(germany.selected.get());
        assertTrue(session.events().stream().anyMatch(e -> String.valueOf(e.attributes()).contains("select visible text")));
    }

    @Test void invalidSelectionAndNonSelectAreRealOperationFailures() {
        Fixture fixture = fixture(List.of(new Option("Poland", "PL", "0")), true);
        assertThrows(UiLocatorException.class, () -> fixture.locator().selectByVisibleText("Missing"));
        assertThrows(UiLocatorException.class, () -> fixture.locator().selectByValue("XX"));
        assertThrows(UiLocatorException.class, () -> fixture.locator().selectByIndex(8));
        Fixture notSelect = fixture(List.of(), false);
        assertThrows(UiLocatorException.class, () -> notSelect.locator().selectByIndex(0));
    }

    private static Fixture fixture(List<Option> optionModels, boolean selectTag) {
        List<WebElement> options = optionModels.stream().map(Option::element).toList();
        WebElement select = (WebElement) Proxy.newProxyInstance(UiLocatorSelectTest.class.getClassLoader(),
                new Class[]{WebElement.class}, (p, m, a) -> switch (m.getName()) {
                    case "getTagName" -> selectTag ? "select" : "div";
                    case "getDomAttribute", "getAttribute" -> null;
                    case "findElements" -> {
                        String query = String.valueOf(a[0]);
                        if (query.contains("By.tagName: option")) yield options;
                        yield optionModels.stream()
                                .filter(option -> query.contains(option.text) || query.contains(option.value))
                                .map(Option::element).toList();
                    }
                    case "isDisplayed", "isEnabled" -> true;
                    default -> null;
                });
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(UiLocatorSelectTest.class.getClassLoader(),
                new Class[]{WebDriver.class, JavascriptExecutor.class}, (p, m, a) -> switch (m.getName()) {
                    case "findElement" -> select;
                    case "findElements" -> List.of(select);
                    case "executeScript", "executeAsyncScript" -> null;
                    default -> m.getReturnType() == boolean.class ? false : null;
                });
        JsOverlayDebug overlay = new JsOverlayDebug(driver,
                OverlayConfig.builder().enabled(false).showHudPanel(false).build());
        return new Fixture(overlay);
    }

    private record Fixture(JsOverlayDebug overlay) {
        UiLocator locator() {
            return overlay.locator(By.id("country"), "Country", UiLocatorOptions.builder()
                    .timeout(Duration.ofMillis(60)).pollInterval(Duration.ofMillis(5)).maxRetries(1).build());
        }
    }

    private static final class Option {
        final String text; final String value; final String index; final AtomicBoolean selected = new AtomicBoolean();
        Option(String text, String value, String index) { this.text = text; this.value = value; this.index = index; }
        WebElement element() {
            return (WebElement) Proxy.newProxyInstance(UiLocatorSelectTest.class.getClassLoader(),
                    new Class[]{WebElement.class}, (p, m, a) -> switch (m.getName()) {
                        case "getTagName" -> "option";
                        case "getText" -> text;
                        case "isSelected" -> selected.get();
                        case "isEnabled", "isDisplayed" -> true;
                        case "click" -> { selected.set(true); yield null; }
                        case "getAttribute", "getDomAttribute" -> switch (String.valueOf(a[0])) {
                            case "value" -> value; case "index" -> index; default -> null;
                        };
                        default -> null;
                    });
        }
    }
}
