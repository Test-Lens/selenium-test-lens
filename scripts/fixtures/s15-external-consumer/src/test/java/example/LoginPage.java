package example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public final class LoginPage {
    private final WebDriver driver;
    private final By username=By.id("username"),password=By.id("password"),loginButton=By.id("old-login-button"),error=By.id("error");
    public LoginPage(WebDriver driver){this.driver=driver;}
    public LoginPage loginExpectingFailure(String user,String secret){driver.findElement(username).sendKeys(user);driver.findElement(password).sendKeys(secret);driver.findElement(loginButton).click();return this;}
    public boolean errorVisible(){return driver.findElement(error).isDisplayed();}
}
