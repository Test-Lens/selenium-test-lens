package benchmark;

import io.github.testlens.studio.browser.BrowserSessionContext;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public final class LoginPage {
    private final WebDriver driver;
    private final By username=By.id("username"),password=By.id("password"),loginButton=By.id("old-login-button"),error=By.id("error"),session=By.id("session-state");
    public LoginPage(){this(BrowserSessionContext.currentDriver());}
    public LoginPage(WebDriver driver){this.driver=driver;}
    public boolean invalidPasswordShowsExpectedError(String user,String invalidValue){
        driver.findElement(username).sendKeys(user);
        driver.findElement(password).sendKeys(invalidValue);
        driver.findElement(loginButton).click();
        return driver.findElement(error).isDisplayed()
                && "Invalid password".equals(driver.findElement(error).getText())
                && "false".equals(driver.findElement(session).getDomAttribute("data-authenticated"));
    }
}
