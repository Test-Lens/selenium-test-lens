package fixture.pages;

import io.github.testlens.JsOverlayDebug;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public final class CustomerDetailsPage extends BasePage {
    private final By editButton = By.cssSelector("[data-testid='edit-customer']");

    public CustomerDetailsPage(WebDriver driver, JsOverlayDebug lens) {
        super(driver, lens);
    }

    public void openEditDialog() {
        lens.locator(editButton, "Edit customer").click();
    }
}
