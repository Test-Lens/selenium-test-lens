package io.github.mmaciekk111.uitestlens.selenium.locator;

import io.github.mmaciekk111.uitestlens.JsOverlayDebug;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaywrightStyleLocatorHelpersTest {

    @Test
    void getByPlaceholderBuildsPlaceholderLocator() {
        UiLocator locator = overlay().getByPlaceholder("Search");

        assertTrue(locator.by().toString().contains("@placeholder = 'Search'"));
        assertTrue(locator.description().contains("placeholder: Search"));
    }

    @Test
    void getByTextBuildsExactTextLocator() {
        UiLocator locator = overlay().getByText("Save");

        assertTrue(locator.by().toString().contains("normalize-space(.) = 'Save'"));
    }

    @Test
    void getByTextContainingBuildsContainsLocator() {
        UiLocator locator = overlay().getByTextContaining("Saved");

        assertTrue(locator.by().toString().contains("contains(normalize-space(.), 'Saved')"));
    }

    @Test
    void getByLabelBuildsLabelAndAriaLocator() {
        UiLocator locator = overlay().getByLabel("Email");
        String by = locator.by().toString();

        assertTrue(by.contains("@aria-label = 'Email'"));
        assertTrue(by.contains("//label[normalize-space(.) = 'Email']/@for"));
        assertTrue(by.contains("@aria-labelledby"));
    }

    @Test
    void getByRoleBuildsExplicitAndImplicitRoleLocator() {
        UiLocator locator = overlay().getByRole("button", "Save");
        String by = locator.by().toString();

        assertTrue(by.contains("@role = 'button'"));
        assertTrue(by.contains("self::button"));
        assertTrue(by.contains("@aria-label = 'Save'"));
        assertTrue(by.contains("normalize-space(.) = 'Save'"));
    }

    private static JsOverlayDebug overlay() {
        return new JsOverlayDebug(fakeDriver());
    }

    private static WebDriver fakeDriver() {
        return (WebDriver) Proxy.newProxyInstance(
                PlaywrightStyleLocatorHelpersTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "toString" -> "locator-driver";
                    default -> null;
                }
        );
    }
}
