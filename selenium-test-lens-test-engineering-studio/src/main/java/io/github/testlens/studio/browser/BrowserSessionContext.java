package io.github.testlens.studio.browser;

import org.openqa.selenium.WebDriver;

import java.util.Objects;

/**
 * Browser made available to a Studio-targeted generated test on its execution thread.
 * The context is absent outside an active targeted run and never owns the driver.
 * @since 0.5.0
 */
public final class BrowserSessionContext {
    private static final ThreadLocal<WebDriver> CURRENT = new ThreadLocal<>();

    private BrowserSessionContext() { }

    public static WebDriver currentDriver() {
        WebDriver driver = CURRENT.get();
        if (driver == null) throw new IllegalStateException("No Test Engineering Studio browser session is active");
        return driver;
    }

    public static Scope bind(WebDriver driver) {
        if (CURRENT.get() != null) throw new IllegalStateException("A Studio browser session is already bound");
        CURRENT.set(Objects.requireNonNull(driver, "driver"));
        return new Scope();
    }

    public static final class Scope implements AutoCloseable {
        private boolean closed;
        private Scope() { }
        @Override public void close() { if (!closed) { closed=true; CURRENT.remove(); } }
    }
}
