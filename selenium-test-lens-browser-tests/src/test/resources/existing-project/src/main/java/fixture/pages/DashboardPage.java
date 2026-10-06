package fixture.pages;

import fixture.components.TopNavigation;
import io.github.testlens.JsOverlayDebug;
import org.openqa.selenium.WebDriver;

public final class DashboardPage extends BasePage {
    private final TopNavigation navigation;

    public DashboardPage(WebDriver driver, JsOverlayDebug lens) {
        super(driver, lens);
        this.navigation = new TopNavigation(lens);
    }

    public CustomersPage openCustomers() {
        navigation.openCustomers();
        return new CustomersPage(driver, lens);
    }
}
