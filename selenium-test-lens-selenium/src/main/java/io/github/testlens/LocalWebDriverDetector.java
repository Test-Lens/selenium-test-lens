package io.github.testlens;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;

/** Conservative local/remote classification for IDE protocol safety. */
final class LocalWebDriverDetector {
    private LocalWebDriverDetector() {}

    static boolean isLocal(WebDriver driver) {
        if (!(driver instanceof RemoteWebDriver remote)) return true;
        if (!(remote.getCommandExecutor() instanceof HttpCommandExecutor http)) return false;
        URL address = http.getAddressOfRemoteServer();
        return isLoopbackAddress(address);
    }

    static boolean isLoopbackAddress(URL address) {
        if (address == null) return false;
        String host = address.getHost();
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }
}
