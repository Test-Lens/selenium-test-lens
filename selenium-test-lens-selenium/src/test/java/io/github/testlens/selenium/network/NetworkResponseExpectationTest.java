package io.github.testlens.selenium.network;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NetworkResponseExpectationTest {

    @Test
    void withinReturnsMatchedResult() {
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver())
                .start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.MANUAL).build());
        diagnostics.addManualEvent(NetworkEvent.response(NetworkResponse.of("1", "/api/orders", 201)));

        NetworkWaitResult result = diagnostics.expectResponse()
                .urlContains("/api/orders")
                .status(201)
                .within(Duration.ofMillis(50));

        assertEquals(NetworkWaitStatus.MATCHED, result.status());
    }

    @Test
    void withinThrowsAssertionErrorOnTimeout() {
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver())
                .start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.MANUAL).build());

        NetworkAssertionError error = assertThrows(NetworkAssertionError.class, () -> diagnostics.expectResponse()
                .urlContains("/api/orders")
                .status(201)
                .within(Duration.ofMillis(10)));

        assertNotNull(error.waitResult());
        assertEquals(NetworkWaitStatus.TIMED_OUT, error.waitResult().status());
    }

    private static WebDriver fakeDriver() {
        return (WebDriver) Proxy.newProxyInstance(
                NetworkResponseExpectationTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "toString" -> "network-driver";
                    default -> null;
                }
        );
    }
}
