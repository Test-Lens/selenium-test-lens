package io.github.testlens.studio.browser;

import org.openqa.selenium.WebDriver;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Browser plus its explicit lifecycle ownership. @since 0.5.0 */
public final class BrowserSession implements AutoCloseable {
    private final WebDriver driver;
    private final Ownership ownership;
    private final AtomicBoolean closed = new AtomicBoolean();

    public BrowserSession(WebDriver driver, Ownership ownership) {
        this.driver = Objects.requireNonNull(driver, "driver");
        this.ownership = Objects.requireNonNull(ownership, "ownership");
    }

    public WebDriver driver() {
        return driver;
    }

    public Ownership ownership() {
        return ownership;
    }

    public boolean closed() {
        return closed.get();
    }

    @Override
    public void close() {
        if (ownership != Ownership.STUDIO_OWNED) return;
        if (closed.compareAndSet(false, true)) driver.quit();
    }
}
