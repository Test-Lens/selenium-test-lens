package fixture.pages;

import io.github.testlens.JsOverlayDebug;
import org.openqa.selenium.WebDriver;

public abstract class BasePage {
    protected final WebDriver driver;
    protected final JsOverlayDebug lens;

    protected BasePage(WebDriver driver, JsOverlayDebug lens) {
        this.driver = driver;
        this.lens = lens;
    }
}
