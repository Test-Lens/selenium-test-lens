package io.github.testlens;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.PageWaits;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

/** Package bridge that keeps the legacy PageWaits API while applying per-Lens options. */
final class ConfiguredPageWaits extends PageWaits {
    ConfiguredPageWaits(WebDriver driver,
                        OverlayConfig config,
                        Duration timeout,
                        Duration pollInterval,
                        OverlayLogger logger) {
        super(driver, config, timeout, pollInterval, logger);
    }
}
