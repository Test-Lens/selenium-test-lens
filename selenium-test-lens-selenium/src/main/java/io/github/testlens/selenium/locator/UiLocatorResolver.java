package io.github.testlens.selenium.locator;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

final class UiLocatorResolver {
    private final WebDriver driver;

    public UiLocatorResolver(WebDriver driver) {
        this.driver = Objects.requireNonNull(driver, "driver must not be null");
    }

    public WebElement resolve(By by, UiLocatorOptions options) {
        Resolution resolution = resolveOnce(by, options);
        if (!resolution.result().passed()) {
            throw new UiLocatorException(
                    "resolve",
                    by == null ? "" : by.toString(),
                    resolution.result().message(),
                    resolution.failure(),
                    "");
        }
        return resolution.element();
    }

    public UiLocatorResult resolveResult(By by, UiLocatorOptions options) {
        return resolveOnce(by, options).result();
    }

    private Resolution resolveOnce(By by, UiLocatorOptions options) {
        UiLocatorOptions effectiveOptions = effectiveOptions(options);
        long startedNanos = System.nanoTime();
        AtomicInteger attempts = new AtomicInteger();

        if (by == null) {
            IllegalArgumentException failure = new IllegalArgumentException("Locator must not be null");
            return failedResolution(
                    "",
                    UiLocatorFailureReason.NOT_FOUND,
                    attempts.get(),
                    startedNanos,
                    failure.getMessage(),
                    failure);
        }

        try {
            WebElement element = new WebDriverWait(driver, effectiveOptions.timeout())
                    .pollingEvery(effectiveOptions.pollInterval())
                    .ignoring(NoSuchElementException.class)
                    .ignoring(StaleElementReferenceException.class)
                    .until(webDriver -> {
                        attempts.incrementAndGet();
                        if (by instanceof By.Remotable) return webDriver.findElement(by);
                        var elements = by.findElements(webDriver);
                        if (elements.isEmpty()) throw new NoSuchElementException("Composite locator matched no elements");
                        return elements.get(0);
                    });
            UiLocatorResult result = UiLocatorResult.builder(UiLocatorStatus.PASSED)
                    .action("resolve")
                    .description(by.toString())
                    .attempts(attempts.get())
                    .elapsed(elapsedSince(startedNanos))
                    .message("Locator resolved")
                    .build();
            return new Resolution(element, result, null);
        } catch (TimeoutException timeout) {
            return failedResolution(
                    by.toString(),
                    UiLocatorFailureReason.TIMEOUT,
                    attempts.get(),
                    startedNanos,
                    timeoutMessage(timeout),
                    timeout);
        } catch (RuntimeException failure) {
            return failedResolution(
                    by.toString(),
                    UiLocatorFailureReason.UNKNOWN,
                    attempts.get(),
                    startedNanos,
                    "Locator resolution failed: " + failure.getClass().getSimpleName(),
                    failure);
        }
    }

    private static Resolution failedResolution(String description,
                                               UiLocatorFailureReason reason,
                                               int attempts,
                                               long startedNanos,
                                               String message,
                                               RuntimeException failure) {
        UiLocatorResult result = UiLocatorResult.builder(UiLocatorStatus.FAILED)
                .failureReason(reason)
                .action("resolve")
                .description(description)
                .attempts(attempts)
                .elapsed(elapsedSince(startedNanos))
                .message(message)
                .build();
        return new Resolution(null, result, failure);
    }

    private static Duration elapsedSince(long startedNanos) {
        return Duration.ofNanos(System.nanoTime() - startedNanos);
    }

    private static String timeoutMessage(TimeoutException timeout) {
        Throwable cause = timeout.getCause();
        if (cause instanceof CollectionSelectionException) {
            return "Locator was not resolved before timeout | " + cause.getMessage().split("\n", 2)[0];
        }
        return "Locator was not resolved before timeout";
    }

    private static UiLocatorOptions effectiveOptions(UiLocatorOptions options) {
        return options != null ? options : UiLocatorOptions.defaults();
    }

    private record Resolution(WebElement element, UiLocatorResult result, RuntimeException failure) {}
}
