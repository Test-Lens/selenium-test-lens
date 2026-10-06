package fixture.components;

import io.github.testlens.JsOverlayDebug;
import org.openqa.selenium.By;

public final class TopNavigation {
    private final JsOverlayDebug lens;
    private final By customersLink = By.cssSelector("[data-testid='customers-link']");

    public TopNavigation(JsOverlayDebug lens) {
        this.lens = lens;
    }

    public void openCustomers() {
        lens.locator(customersLink, "Customers").click();
    }
}
