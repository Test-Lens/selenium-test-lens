package fixture.repair;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public final class ExistingLoginPage {
    private final WebDriver driver;
    private final By username = By.id("username");
    private final By password = By.id("password");
    private final By loginButton = By.id("old-login-button");
    private final By loginError = By.id("login-error");

    public ExistingLoginPage(WebDriver driver) {
        this.driver = driver;
    }

    public ExistingLoginPage invalidPassword(String user, String secret) {
        driver.findElement(username).sendKeys(user);
        driver.findElement(password).sendKeys(secret);
        driver.findElement(loginButton).click();
        return this;
    }

    public boolean loginErrorVisible() {
        return driver.findElement(loginError).isDisplayed();
    }
}
