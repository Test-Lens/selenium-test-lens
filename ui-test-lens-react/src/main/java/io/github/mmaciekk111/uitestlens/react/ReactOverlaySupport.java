package io.github.mmaciekk111.uitestlens.react;

import io.github.mmaciekk111.uitestlens.OverlayConfig;
import org.openqa.selenium.WebElement;

/**
 * Minimal overlay callbacks used by React-safe helpers.
 */
public interface ReactOverlaySupport {
    OverlayConfig getConfig();

    void setStep(String stepDescription);

    WebElement highlightElement(WebElement element, String label);
}
