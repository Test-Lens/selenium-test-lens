package io.github.testlens.studio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class TestEngineeringStudioBrowserOptionsTest {
    @Test
    void usesConfiguredChromeBinary() {
        String previous = System.getProperty("test.chrome.binary");
        try {
            System.setProperty("test.chrome.binary", "/toolcache/chrome/stable/chrome");

            Map<?, ?> chrome = chromeOptionsCapability(TestEngineeringStudioBrowserIT.chromeOptions());

            assertEquals("/toolcache/chrome/stable/chrome", chrome.get("binary"));
        } finally {
            restore("test.chrome.binary", previous);
        }
    }

    @Test
    void preservesDefaultChromeDiscoveryWhenBinaryIsNotConfigured() {
        String previous = System.getProperty("test.chrome.binary");
        try {
            System.clearProperty("test.chrome.binary");

            Map<?, ?> chrome = chromeOptionsCapability(TestEngineeringStudioBrowserIT.chromeOptions());

            assertFalse(chrome.containsKey("binary"));
        } finally {
            restore("test.chrome.binary", previous);
        }
    }

    private static Map<?, ?> chromeOptionsCapability(ChromeOptions options) {
        return (Map<?, ?>) options.asMap().get(ChromeOptions.CAPABILITY);
    }

    private static void restore(String name, String value) {
        if (value == null) System.clearProperty(name);
        else System.setProperty(name, value);
    }
}
