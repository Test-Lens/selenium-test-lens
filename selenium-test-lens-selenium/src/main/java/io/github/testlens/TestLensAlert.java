package io.github.testlens;

import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;
import io.github.testlens.selenium.locator.UiLocatorOptions;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/** Small, driver-bound facade for native JavaScript alert, confirm and prompt dialogs. */
public final class TestLensAlert {
    private final WebDriver driver;
    private final UiLocatorOptions options;
    private final JsOverlayDebug lens;

    TestLensAlert(WebDriver driver, UiLocatorOptions options, JsOverlayDebug lens) {
        this.driver = Objects.requireNonNull(driver);
        this.options = Objects.requireNonNull(options);
        this.lens = Objects.requireNonNull(lens);
    }

    public TestLensAlert waitUntilPresent() {
        operation("alert.wait", "Wait for browser alert", () -> {
            AtomicInteger attempts = new AtomicInteger();
            new WebDriverWait(driver, options.timeout()).pollingEvery(options.pollInterval())
                    .until(webDriver -> {
                        int attempt = attempts.incrementAndGet();
                        try {
                            return webDriver.switchTo().alert();
                        } catch (NoAlertPresentException missing) {
                            lens.emitConsumerOperation("alert.wait.retry", "Browser alert retry " + attempt,
                                    UiTestLensStatus.WARN, UiTestLensLogLevel.INFO, null);
                            return null;
                        }
                    });
            return null;
        });
        return this;
    }

    public String text() { return operation("alert.text", "Read browser alert text", () -> driver.switchTo().alert().getText()); }
    public void accept() { operation("alert.accept", "Accept browser alert", () -> { driver.switchTo().alert().accept(); return null; }); }
    public void dismiss() { operation("alert.dismiss", "Dismiss browser alert", () -> { driver.switchTo().alert().dismiss(); return null; }); }
    public void fill(String value) {
        operation("alert.fill", "Fill browser prompt (length=" + (value == null ? 0 : value.length()) + ")",
                () -> { driver.switchTo().alert().sendKeys(value == null ? "" : value); return null; });
    }

    private <T> T operation(String action, String description, java.util.concurrent.Callable<T> body) {
        lens.emitConsumerOperation(action, description, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO, null);
        try {
            T result = body.call();
            lens.emitConsumerOperation(action, description, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null);
            return result;
        } catch (RuntimeException failure) {
            lens.emitConsumerOperation(action, description, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR, failure);
            throw failure;
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
