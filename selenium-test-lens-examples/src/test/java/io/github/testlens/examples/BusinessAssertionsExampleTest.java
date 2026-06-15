package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

@Disabled("Documentation-only example; requires a real WebDriver.")
class BusinessAssertionsExampleTest {

    @Test
    void businessAssertionsUsage() {
        WebDriver driver = null; // replace with a real driver in a test project

        JsOverlayDebug overlay = new JsOverlayDebug(driver);

        overlay.business("Order summary")
                .check("shows total amount", () -> {
                    overlay.getByTestId("order-total").expect().toHaveText("123.00 PLN");
                })
                .check("contains premium product", () -> {
                    overlay.getByTestId("product-name").expect().toContainText("Premium");
                })
                .check("save button remains enabled", () -> {
                    overlay.getByTestId("save-button").expect().toBeEnabled();
                })
                .verify();
    }
}

