package io.github.testlens.selenium.network;

import java.util.Map;

/** Internal boundary around Selenium's beta BiDi API. */
interface NetworkCaptureSource extends AutoCloseable {
    default Map<String, String> diagnostics() { return Map.of(); }

    @Override
    void close();
}
