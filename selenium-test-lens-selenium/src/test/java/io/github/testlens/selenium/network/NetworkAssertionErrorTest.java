package io.github.testlens.selenium.network;

import io.github.testlens.core.redaction.RedactionPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkAssertionErrorTest {

    @Test
    void storesSummary() {
        NetworkSummary summary = NetworkSummary.from(List.of(
                NetworkEvent.response(NetworkResponse.of("1", "/api/orders", 500))
        ), 0, 400, NetworkDiagnosticsStatus.STARTED);

        NetworkAssertionError error = new NetworkAssertionError("failed", summary);

        assertSame(summary, error.summary());
    }

    @Test
    void storesWaitResult() {
        NetworkSummary summary = NetworkSummary.from(List.of(), 0, 400, NetworkDiagnosticsStatus.STARTED);
        NetworkWaitResult waitResult = NetworkWaitResult.timedOut(
                NetworkWaitCondition.builder().urlContains("/api/orders").build(),
                1,
                java.time.Duration.ofMillis(10),
                summary
        );

        NetworkAssertionError error = new NetworkAssertionError("timeout", summary, waitResult);

        assertSame(summary, error.summary());
        assertEquals(NetworkWaitStatus.TIMED_OUT, error.waitResult().status());
    }

    @Test
    void internalAssertionFactoryExposesOnlyRedactedThrowableGraph() {
        String secret = "TL_NETWORK_THROWABLE_SECRET";
        RedactionPolicy policy = RedactionPolicy.builder().secret(secret).build();
        IllegalStateException cause = new IllegalStateException("cause " + secret);
        RuntimeException failure = new RuntimeException("failure " + secret, cause);
        failure.addSuppressed(new IllegalArgumentException("suppressed " + secret));
        NetworkWaitResult raw = NetworkWaitResult.failed(
                NetworkWaitCondition.builder().exactUrl("https://user:pass@example.test/?token=" + secret
                        + "#fragment").build(),
                "wait failed " + secret,
                NetworkWaitFailureReason.UNKNOWN,
                failure,
                1,
                java.time.Duration.ofMillis(10));
        NetworkWaitResult safe = raw.redacted(policy, null, null,
                "exact url=https://example.test/?token=[REDACTED]");

        NetworkAssertionError error = NetworkAssertionError.redacted(
                "assertion " + secret,
                NetworkSummary.from(List.of(), 0, 400, NetworkDiagnosticsStatus.FAILED),
                safe,
                policy);

        String outward = error.getMessage() + error + error.waitResult().message()
                + error.waitResult().conditionSummary() + error.waitResult().exception()
                + error.getCause() + error.getCause().getCause()
                + error.getCause().getSuppressed()[0];
        assertFalse(outward.contains(secret));
        assertTrue(outward.contains("[REDACTED]"));
        assertFalse(error.waitResult().exception() == failure);
        assertTrue(failure.getMessage().contains(secret), "the runtime exception must remain untouched");
        assertSame(error.waitResult().exception(), error.getCause());
    }
}

