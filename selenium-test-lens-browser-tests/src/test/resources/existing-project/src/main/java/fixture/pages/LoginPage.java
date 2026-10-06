package fixture.pages;

import io.github.testlens.JsOverlayDebug;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public final class LoginPage extends BasePage {
    private final By username = By.id("username");
    private final By password = By.id("password");
    private final By loginButton = By.cssSelector("[data-testid='login-submit']");

    public LoginPage(WebDriver driver, JsOverlayDebug lens) {
        super(driver, lens);
    }

    public LoginPage enterUsername(String value) {
        lens.locator(username, "Username").fill(value);
        return this;
    }

    public LoginPage enterPassword(String value) {
        lens.locator(password, "Password").fill(value);
        return this;
    }

    public DashboardPage loginAs(String usernameValue, String passwordValue) {
        enterUsername(usernameValue);
        enterPassword(passwordValue);
        lens.locator(loginButton, "Log in").click();
        return new DashboardPage(driver, lens);
    }
}
