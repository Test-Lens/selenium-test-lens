package io.github.testlens.studio.browser;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.manager.SeleniumManager;

import java.util.List;
import java.util.Objects;

/**
 * Lazy local provider. Selenium Manager is consulted only by an explicit preflight/open call,
 * and a real browser is constructed only by {@link #open(BrowserRequest)}.
 * @since 0.5.0
 */
public final class DefaultLocalBrowserSessionProvider implements BrowserSessionProvider {
    private final BrowserPreflightProbe probe;
    private final LocalDriverFactory driverFactory;

    public DefaultLocalBrowserSessionProvider() {
        this(DefaultLocalBrowserSessionProvider::probeLocalBrowser,
                DefaultLocalBrowserSessionProvider::createLocalDriver);
    }

    DefaultLocalBrowserSessionProvider(BrowserPreflightProbe probe, LocalDriverFactory driverFactory) {
        this.probe = Objects.requireNonNull(probe, "probe");
        this.driverFactory = Objects.requireNonNull(driverFactory, "driverFactory");
    }

    @Override
    public BrowserAvailability preflight(BrowserRequest request) {
        if (request == null) return BrowserAvailability.CONFIGURATION_INVALID;
        try {
            BrowserAvailability result = probe.check(request);
            return result == null ? BrowserAvailability.CONFIGURATION_INVALID : result;
        } catch (IllegalArgumentException failure) {
            return BrowserAvailability.CONFIGURATION_INVALID;
        } catch (UnsupportedOperationException failure) {
            return BrowserAvailability.UNSUPPORTED;
        } catch (RuntimeException failure) {
            return BrowserAvailability.NOT_AVAILABLE;
        }
    }

    @Override
    public BrowserSession open(BrowserRequest request) {
        BrowserAvailability availability = preflight(request);
        if (availability != BrowserAvailability.AVAILABLE) {
            throw new IllegalStateException("Local browser is not available: " + availability);
        }
        try {
            return new BrowserSession(driverFactory.create(request), request.ownership());
        } catch (IllegalArgumentException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new IllegalStateException("Unable to open local " + request.browser() + " session", failure);
        }
    }

    private static BrowserAvailability probeLocalBrowser(BrowserRequest request) {
        String browserName = switch (request.browser()) {
            case CHROME -> "chrome";
            case FIREFOX -> "firefox";
        };
        var paths = SeleniumManager.getInstance().getBinaryPaths(List.of("--browser", browserName));
        return paths.getDriverPath() == null || paths.getDriverPath().isBlank()
                ? BrowserAvailability.NOT_AVAILABLE
                : BrowserAvailability.AVAILABLE;
    }

    private static WebDriver createLocalDriver(BrowserRequest request) {
        return switch (request.browser()) {
            case CHROME -> {
                ChromeOptions options = new ChromeOptions();
                if (request.headless()) options.addArguments("--headless=new");
                yield new ChromeDriver(options);
            }
            case FIREFOX -> {
                FirefoxOptions options = new FirefoxOptions();
                if (request.headless()) options.addArguments("-headless");
                yield new FirefoxDriver(options);
            }
        };
    }

    @FunctionalInterface
    interface BrowserPreflightProbe {
        BrowserAvailability check(BrowserRequest request);
    }

    @FunctionalInterface
    interface LocalDriverFactory {
        WebDriver create(BrowserRequest request);
    }
}
