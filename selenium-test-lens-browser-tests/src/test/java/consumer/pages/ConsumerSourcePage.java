package consumer.pages;

import io.github.testlens.TestLens;
import org.openqa.selenium.By;

/** Consumer-package call site used by the real HUD source-navigation contract. */
public final class ConsumerSourcePage {
    private ConsumerSourcePage() {}

    public static void clickCounter(TestLens lens) {
        lens.locator(By.id("count-button"), "Counter button").click();
    }
}
