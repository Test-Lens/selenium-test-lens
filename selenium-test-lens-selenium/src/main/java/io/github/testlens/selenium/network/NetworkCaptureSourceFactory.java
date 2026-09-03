package io.github.testlens.selenium.network;

import org.openqa.selenium.WebDriver;

@FunctionalInterface
interface NetworkCaptureSourceFactory {
    NetworkCaptureSource open(WebDriver driver, NetworkDiagnosticsOptions options, NetworkCaptureSink sink);
}
