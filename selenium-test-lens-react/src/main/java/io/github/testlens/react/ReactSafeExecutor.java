package io.github.testlens.react;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.function.Function;

/**
 * Retries locator-based actions across common React/SPA re-render windows.
 */
public class ReactSafeExecutor {

    private final WebDriver driver;
    private final ReactOverlaySupport overlay;
    private final ReactSelectHelper reactSelect;
    private final int maxRetries;
    private final Duration retryDelay;
    private final Duration waitPerAttempt;
    private final RetryTraceSink retryTraceSink;

    public ReactSafeExecutor(WebDriver driver,
                             ReactOverlaySupport overlay,
                             int maxRetries,
                             Duration retryDelay,
                             Duration waitPerAttempt) {
        this(driver, overlay, maxRetries, retryDelay, waitPerAttempt,
                overlay instanceof RetryTraceSink sink ? sink : RetryTraceSink.noop());
    }

    ReactSafeExecutor(WebDriver driver,
                      ReactOverlaySupport overlay,
                      int maxRetries,
                      Duration retryDelay,
                      Duration waitPerAttempt,
                      RetryTraceSink retryTraceSink) {
        this.driver = driver;
        this.overlay = overlay;
        this.reactSelect = new ReactSelectHelper(driver);
        this.maxRetries = maxRetries <= 0 ? 3 : maxRetries;
        this.retryDelay = retryDelay != null ? retryDelay : Duration.ofMillis(200);
        this.waitPerAttempt = waitPerAttempt != null ? waitPerAttempt : Duration.ofSeconds(15);
        this.retryTraceSink = retryTraceSink == null ? RetryTraceSink.noop() : retryTraceSink;
    }

    public ReactSafeExecutor(WebDriver driver, ReactOverlaySupport overlay) {
        this(driver, overlay, 3, Duration.ofMillis(200), Duration.ofSeconds(15));
    }

    public <T> T doWithRetry(By locator,
                             String actionDescription,
                             Function<WebElement, T> op) {

        StaleElementReferenceException lastStale = null;
        NoSuchElementException lastNse = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            boolean operationStarted = false;
            long operationStartedAt = 0;
            try {
                if (overlay != null && overlay.getConfig().isShowHudPanel()) {
                    overlay.setStep(String.format(
                            "React-safe [%s]: attempt %d/%d",
                            actionDescription, attempt, maxRetries
                    ));
                }

                WebDriverWait wait = new WebDriverWait(driver, waitPerAttempt);
                WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));

                if (overlay != null) {
                    overlay.highlightElement(element, actionDescription);
                }

                operationStartedAt = System.nanoTime();
                operationStarted = true;
                return op.apply(element);

            } catch (StaleElementReferenceException e) {
                lastStale = e;
                recordRecoveryRetry(operationStarted, locator, actionDescription, attempt, operationStartedAt, e);
                sleep(retryDelay);
            } catch (NoSuchElementException e) {
                lastNse = e;
                recordRecoveryRetry(operationStarted, locator, actionDescription, attempt, operationStartedAt, e);
                sleep(retryDelay);
            } catch (ElementClickInterceptedException e) {
                // React overlays can appear between presence and interaction; retry with a fresh element.
                recordRecoveryRetry(operationStarted, locator, actionDescription, attempt, operationStartedAt, e);
                sleep(retryDelay);
            }
        }

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

    private void recordRecoveryRetry(boolean operationStarted, By locator, String action, int attempt,
                                     long operationStartedAt, RuntimeException failure) {
        if (!operationStarted || attempt >= maxRetries) return;
        try {
            retryTraceSink.record(action, locator.toString(), attempt, attempt + 1, failure,
                    Math.max(0, System.nanoTime() - operationStartedAt));
        } catch (RuntimeException ignored) {
            // Retry diagnostics must not change React operation behavior.
        }
    }

    interface RetryTraceSink {
        void record(String action, String locator, int attempt, int nextAttempt,
                    RuntimeException failure, long durationNanos);

        static RetryTraceSink noop() {
            return (action, locator, attempt, nextAttempt, failure, durationNanos) -> { };
        }
    }

    public void click(By locator, String label) {
        doWithRetry(locator, "CLICK: " + label, el -> {
            el.click();
            return null;
        });
    }

    public void clearAndType(By locator, String text, String label) {
        doWithRetry(locator, "TYPE: " + label, el -> {
            el.clear();
            el.sendKeys(text);
            return null;
        });
    }

    public String getText(By locator, String label) {
        return doWithRetry(locator, "GET_TEXT: " + label, WebElement::getText);
    }

    public String getAttribute(By locator, String attr, String label) {
        return doWithRetry(locator, "GET_ATTR(" + attr + "): " + label,
                el -> el.getAttribute(attr));
    }

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

