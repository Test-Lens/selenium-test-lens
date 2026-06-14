package io.github.mmaciekk111.uitestlens.react;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.function.Function;

/**
 * Helper do "odpornych" akcji na React/SPA:
 * - pracuje na By locator (nie na WebElement),
 * - przy StaleElementReferenceException / chwilowym NoSuchElementException
 * próbuje ponownie znaleźć element i wykonać akcję,
 * - opcjonalnie loguje próby w HUD-zie JsOverlayDebug.
 */
public class ReactSafeExecutor {

    private final WebDriver driver;
    private final ReactOverlaySupport overlay; // opcjonalne – do HUD + highlight
    private final ReactSelectHelper reactSelect;
    private final int maxRetries;
    private final Duration retryDelay;
    private final Duration waitPerAttempt;

    public ReactSafeExecutor(WebDriver driver,
                             ReactOverlaySupport overlay,
                             int maxRetries,
                             Duration retryDelay,
                             Duration waitPerAttempt) {
        this.driver = driver;
        this.overlay = overlay;
        this.reactSelect = new ReactSelectHelper(driver);
        this.maxRetries = maxRetries <= 0 ? 3 : maxRetries;
        this.retryDelay = retryDelay != null ? retryDelay : Duration.ofMillis(200);
        this.waitPerAttempt = waitPerAttempt != null ? waitPerAttempt : Duration.ofSeconds(15);
    }

    public ReactSafeExecutor(WebDriver driver, ReactOverlaySupport overlay) {
        this(driver, overlay, 3, Duration.ofMillis(200), Duration.ofSeconds(15));
    }

    /**
     * Uniwersalny executor:
     * - bierze lokator,
     * - przy każdej próbie czeka na presenceOfElementLocated,
     * - przekazuje świeży WebElement do funkcji op,
     * - retry na stale / no-such.
     */
    public <T> T doWithRetry(By locator,
                             String actionDescription,
                             Function<WebElement, T> op) {

        StaleElementReferenceException lastStale = null;
        NoSuchElementException lastNse = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (overlay != null && overlay.getConfig().isShowHudPanel()) {
                    overlay.setStep(String.format(
                            "React-safe [%s]: attempt %d/%d",
                            actionDescription, attempt, maxRetries
                    ));
                }

                WebDriverWait wait = new WebDriverWait(driver, waitPerAttempt);
                WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));

                // Możesz tu dodać np. highlight przed akcją:
                if (overlay != null) {
                    overlay.highlightElement(element, actionDescription);
                }

                return op.apply(element);

            } catch (StaleElementReferenceException e) {
                lastStale = e;
                sleep(retryDelay);
            } catch (NoSuchElementException e) {
                lastNse = e;
                sleep(retryDelay);
            } catch (ElementClickInterceptedException e) {
                // React czasem coś jeszcze dorysowuje – mała pauza i retry
                sleep(retryDelay);
            }
        }

        // Jeśli tu doszliśmy, to wszystkie próby padły:
        String msg = String.format(
                "React-safe action FAILED after %d attempts: %s (locator: %s)",
                maxRetries, actionDescription, locator
        );
        if (overlay != null && overlay.getConfig().isShowHudPanel()) {
            overlay.setStep(msg);
        }

        if (lastStale != null) {
            throw new RuntimeException(msg, lastStale);
        } else if (lastNse != null) {
            throw new RuntimeException(msg, lastNse);
        } else {
            throw new RuntimeException(msg);
        }
    }

    private void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    // ===== Wygodne metody do najczęstszych akcji =====

    /**
     * Bezpieczny click – sam odświeży element przy stale.
     */
    public void click(By locator, String label) {
        doWithRetry(locator, "CLICK: " + label, el -> {
            el.click();
            return null;
        });
    }

    /**
     * Bezpieczne sendKeys – czyści pole i wpisuje tekst.
     */
    public void clearAndType(By locator, String text, String label) {
        doWithRetry(locator, "TYPE: " + label, el -> {
            el.clear();
            el.sendKeys(text);
            return null;
        });
    }

    /**
     * Bezpieczne readText – odczytuje aktualny tekst po re-renderach.
     */
    public String getText(By locator, String label) {
        return doWithRetry(locator, "GET_TEXT: " + label, WebElement::getText);
    }

    /**
     * Bezpieczne pobranie atrybutu.
     */
    public String getAttribute(By locator, String attr, String label) {
        return doWithRetry(locator, "GET_ATTR(" + attr + "): " + label,
                el -> el.getAttribute(attr));
    }

    /**
     * Bezpieczne sprawdzenie isDisplayed/isEnabled/isSelected.
     */
    public boolean isDisplayed(By locator, String label) {
        return doWithRetry(locator, "IS_DISPLAYED: " + label, WebElement::isDisplayed);
    }

    public boolean isEnabled(By locator, String label) {
        return doWithRetry(locator, "IS_ENABLED: " + label, WebElement::isEnabled);
    }

    public boolean isSelected(By locator, String label) {
        return doWithRetry(locator, "IS_SELECTED: " + label, WebElement::isSelected);
    }

    public ReactSelectHelper select() {
        return reactSelect;
    }


}


