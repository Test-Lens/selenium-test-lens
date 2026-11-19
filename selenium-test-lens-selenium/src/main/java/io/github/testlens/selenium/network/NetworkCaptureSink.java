package io.github.testlens.selenium.network;

interface NetworkCaptureSink {
    void recorded(NetworkEvent event);
    void ignored();
    default void failed(RuntimeException failure) {}
}
