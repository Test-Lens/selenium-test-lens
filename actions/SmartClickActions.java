package utils.jsExecHelper.actions;

import utils.jsExecHelper.OverlayConfig;
import utils.jsExecHelper.core.BlockingOverlayHelper;
import utils.jsExecHelper.core.OverlayRootManager;
import utils.jsExecHelper.react.ReactSafeExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

public class SmartClickActions {

    private final WebDriver driver;
    private final JavascriptExecutor js;
    private final OverlayConfig config;
    private final HighlightActions highlightActions;
    private final BlockingOverlayHelper blockingHelper;

    public SmartClickActions(WebDriver driver,
                             OverlayConfig config,
                             OverlayRootManager rootManager,
                             HighlightActions highlightActions) {
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.driver = driver;
        this.js = (JavascriptExecutor) driver;
        this.config = config;
        this.highlightActions = highlightActions;
        this.blockingHelper = new BlockingOverlayHelper(driver, config, rootManager, highlightActions);
    }

    /**
     * Główna implementacja klika z obsługą overlayów/popupów.
     * (wcześniej to było w smartClick).
     */
    public void clickWithOverlayHandling(WebElement target, String label) {
        if (target == null) {
            return;
        }

        // ✨ NAJPIERW spróbuj globalnie zamknąć znany overlay (np. cookies)
        blockingHelper.handleGlobalOverlayIfPresent("OVERLAY", "CLOSE");

        try {
            if (config.isEnabled()) {
                highlightActions.highlightClick(target, label);
            } else {
                target.click();
            }
            return;
        } catch (WebDriverException e) {
            if (!isClickInterceptError(e)) {
                throw e;
            }
        }

        // dopiero jeśli mimo wszystko jest błąd, lecimy w logikę "overlay blokuje target"
        boolean handled = blockingHelper.handleBlockingOverlayFor(
                target,
                "BLOCKING OVERLAY",
                "CLOSE"
        );

        if (handled) {
            if (config.isEnabled()) {
                highlightActions.highlightClick(target, label);
            } else {
                target.click();
            }
        } else {
            target.click();
        }
    }

    /**
     * Stara nazwa – zostawiamy dla kompatybilności.
     */
    @Deprecated
    public void smartClick(WebElement target, String label) {
        clickWithOverlayHandling(target, label);
    }

    /**
     * React-safe klik:
     * - pracuje na By (fresh element per attempt),
     * - retry przez ReactSafeExecutor (stale, NoSuchElement, intercept),
     * - wewnątrz korzysta z clickWithOverlayHandling (czyli dalej
     *   obsługa popupów/overlayów + highlight).
     */
    public void clickReactSafe(By locator,
                               String label,
                               ReactSafeExecutor reactSafeExecutor) {
        if (reactSafeExecutor == null) {
            throw new IllegalArgumentException("ReactSafeExecutor must not be null");
        }

        reactSafeExecutor.doWithRetry(
                locator,
                "SMART_CLICK_REACT_SAFE: " + label,
                element -> {
                    clickWithOverlayHandling(element, label);
                    return null;
                }
        );
    }

    private boolean isClickInterceptError(Throwable e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        msg = msg.toLowerCase();
        return msg.contains("other element would receive the click")
                || msg.contains("is not clickable at point")
                || msg.contains("intercepted");
    }
}
