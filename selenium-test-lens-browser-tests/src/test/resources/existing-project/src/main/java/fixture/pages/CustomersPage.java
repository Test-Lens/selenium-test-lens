package fixture.pages;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.selenium.locator.UiLocator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public final class CustomersPage extends BasePage {
    private final UiLocator searchInput =
            lens.locator(By.cssSelector("[data-testid='customer-search']"), "Customer search");
    private final By openCustomer = By.cssSelector("[data-testid='open-customer']");

    public CustomersPage(WebDriver driver, JsOverlayDebug lens) {
        super(driver, lens);
    }

    public CustomersPage search(String text) {
        searchInput.fill(text);
        return this;
    }

    public CustomerDetailsPage openFirstCustomer() {
        lens.locator(openCustomer, "Open customer").click();
        return new CustomerDetailsPage(driver, lens);
    }
}
