package io.github.testlens.selenium.locator;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Instant;
import java.util.Objects;

public final class UiLocatorResolver {
    private final WebDriver driver;

    public UiLocatorResolver(WebDriver driver) {
        this.driver = Objects.requireNonNull(driver, "driver must not be null");
    }

    public WebElement resolve(By by, UiLocatorOptions options) {
        UiLocatorResult result = resolveResult(by, options);
        if (!result.passed()) {
            throw new UiLocatorException("resolve", by == null ? "" : by.toString(), result.message(), null, "");
        }
        return new WebDriverWait(driver, effectiveOptions(options).timeout())
                .pollingEvery(effectiveOptions(options).pollInterval())
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .until(webDriver -> webDriver.findElement(by));
    }

    public UiLocatorResult resolveResult(By by, UiLocatorOptions options) {
        UiLocatorOptions effectiveOptions = effectiveOptions(options);
        Instant started = Instant.now();
        if (by == null) {
            return UiLocatorResult.builder(UiLocatorStatus.FAILED)
                    .failureReason(UiLocatorFailureReason.NOT_FOUND)
                    .action("resolve")
                    .description("")
                    .attempts(0)
                    .elapsed(java.time.Duration.between(started, Instant.now()))
                    .message("Locator must not be null")
                    .build();
        }
        try {
            new WebDriverWait(driver, effectiveOptions.timeout())
                    .pollingEvery(effectiveOptions.pollInterval())
                    .ignoring(NoSuchElementException.class)
                    .ignoring(StaleElementReferenceException.class)
                    .until(webDriver -> webDriver.findElement(by));
            return UiLocatorResult.builder(UiLocatorStatus.PASSED)
                    .action("resolve")
                    .description(by.toString())
                    .attempts(1)
                    .elapsed(java.time.Duration.between(started, Instant.now()))
                    .message("Locator resolved")
                    .build();
        } catch (RuntimeException e) {
            return UiLocatorResult.builder(UiLocatorStatus.FAILED)
                    .failureReason(UiLocatorFailureReason.TIMEOUT)
                    .action("resolve")
                    .description(by.toString())
                    .attempts(1)
                    .elapsed(java.time.Duration.between(started, Instant.now()))
                    .message("Locator was not resolved before timeout")
                    .build();
        }
    }

    private static UiLocatorOptions effectiveOptions(UiLocatorOptions options) {
        return options != null ? options : UiLocatorOptions.defaults();
    }
}
