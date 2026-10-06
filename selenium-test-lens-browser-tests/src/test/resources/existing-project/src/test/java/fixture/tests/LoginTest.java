package fixture.tests;

import fixture.pages.LoginPage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public final class LoginTest {
    private LoginPage loginPage;

    @Test
    @Tag("login")
    void invalidPassword() {
        loginPage.loginAs("test-user", "invalid-password");
    }
}
