package io.github.testlens;

import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class LocalWebDriverDetectorTest {
    @Test void distinguishesLocalDriverEndpointsFromRemoteGridEndpoints() throws Exception {
        assertTrue(LocalWebDriverDetector.isLoopbackAddress(new URL("http://127.0.0.1:9515")));
        assertTrue(LocalWebDriverDetector.isLoopbackAddress(new URL("http://localhost:4444/wd/hub")));
        assertFalse(LocalWebDriverDetector.isLoopbackAddress(new URL("https://grid.example.test/wd/hub")));
        assertFalse(LocalWebDriverDetector.isLoopbackAddress(null));
    }
}
