package io.github.testlens.studio.maven;

import java.net.URI;

interface StudioLaunch extends AutoCloseable {

    URI uri();

    String projectId();

    void await() throws InterruptedException;

    @Override
    void close();
}
