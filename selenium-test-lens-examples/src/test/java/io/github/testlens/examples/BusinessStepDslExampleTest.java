package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

@Disabled("Documentation-only example; requires a real WebDriver.")
class BusinessStepDslExampleTest {

    @Test
    void businessStepDslUsage() {
        WebDriver driver = null; // replace with a real driver in a test project

        JsOverlayDebug overlay = new JsOverlayDebug(driver);

        overlay.step("Fill checkout form", () -> {
            overlay.getByTestId("email").fill("test@example.com");
            overlay.getByTestId("save-button").click();
        });

        overlay.step("Verify order summary", () -> {
            overlay.business("Order summary")
                    .check("shows total amount", () -> {
                        overlay.getByTestId("order-total").expect().toHaveText("123.00 PLN");
                    })
                    .check("contains premium product", () -> {
                        overlay.getByTestId("product-name").expect().toContainText("Premium");
                    })
                    .verify();
        });
    }
}
