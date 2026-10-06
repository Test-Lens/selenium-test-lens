package fixture.repair;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

public final class InvalidLoginTest {
    @Test
    public static boolean execute(WebDriver driver, String user, String secret) {
        return new ExistingLoginPage(driver).invalidPassword(user, secret).loginErrorVisible();
    }
}
