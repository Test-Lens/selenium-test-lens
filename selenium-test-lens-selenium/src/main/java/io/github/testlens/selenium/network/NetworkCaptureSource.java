package io.github.testlens.selenium.network;

/** Internal boundary around Selenium's beta BiDi API. */
interface NetworkCaptureSource extends AutoCloseable {
    @Override
    void close();
}
