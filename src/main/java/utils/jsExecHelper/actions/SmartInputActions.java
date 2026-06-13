package utils.jsExecHelper.actions;

import utils.jsExecHelper.OverlayConfig;
import utils.jsExecHelper.core.BlockingOverlayHelper;
import utils.jsExecHelper.core.OverlayRootManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

/**
 * "Sprytny" input:
 * - próbuje wpisać tekst (typeWithHint),
 * - jeśli coś zasłania input (popup/overlay), próbuje to zamknąć,
 * - ponawia wpisanie.
 */
public class SmartInputActions {

    private final WebDriver driver;
    private final JavascriptExecutor js;
    private final OverlayConfig config;
    private final TypingActions typingActions;
    private final BlockingOverlayHelper blockingHelper;

    public SmartInputActions(WebDriver driver,
                             OverlayConfig config,
                             OverlayRootManager rootManager,
                             TypingActions typingActions) {
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.driver = driver;
        this.js = (JavascriptExecutor) driver;
        this.config = config;
        this.typingActions = typingActions;
        this.blockingHelper = new BlockingOverlayHelper(driver, config, rootManager, null);
        // Uwaga: tu możesz pominąć highlightActions w helperze,
        // jeśli nie chcesz highlightować overlaya przy wpisywaniu.
    }

    public void smartTypeWithHint(WebElement element, String value, String labelForOverlay) {
        if (element == null) return;

        // ✨ najpierw spróbuj globalnie zamknąć overlay (cookies itp.)
        blockingHelper.handleGlobalOverlayIfPresent(
                labelForOverlay != null ? labelForOverlay : "OVERLAY",
                "CLOSE"
        );

        try {
            typingActions.typeWithHint(element, value);
            return;
        } catch (WebDriverException e) {
            if (!isInputInterceptError(e)) {
                throw e;
            }
        }

        // fallback: coś blokuje konkretny input – próbujemy zamknąć overlay nad nim
        boolean handled = blockingHelper.handleBlockingOverlayFor(
                element,
                labelForOverlay != null ? labelForOverlay : "OVERLAY",
                "CLOSE"
        );

        // w obu przypadkach ponów wpisanie
        typingActions.typeWithHint(element, value);
    }


    private boolean isInputInterceptError(Throwable e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        msg = msg.toLowerCase();
        return msg.contains("other element would receive the click")
                || msg.contains("is not clickable at point")
                || msg.contains("not interactable")
                || msg.contains("is not clickable at point")
                || msg.contains("intercepted");
    }
}
