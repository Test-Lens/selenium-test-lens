package io.github.testlens.application.tooling.ai.security;

import io.github.testlens.core.redaction.RedactionPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentArtifactSecurityGateTest {
    private final AgentArtifactSecurityGate gate = new AgentArtifactSecurityGate();

    @Test
    void acceptsAlreadyRedactedBoundedTextAndSafeReferences() {
        var policy = RedactionPolicy.defaults();
        var result = gate.validate("requirement=invalid login; token=[REDACTED]",
                List.of("trace:event-17", "selector:diagnostic-4"), policy, List.of("never-present"));
        assertEquals(AgentArtifactSecurityGate.Status.PASS, result.status());
    }

    @Test
    void blocksDisabledPolicyUnredactedSecretsCanariesAndUnsafeArtifacts() {
        var result = gate.validate("Authorization: Bearer secret-token CANARY",
                List.of("target/auth-state.json", "failure.png", "surefire-report.xml"),
                RedactionPolicy.disabled(), List.of("CANARY"));
        assertEquals(AgentArtifactSecurityGate.Status.BLOCKED, result.status());
        assertTrue(result.findings().stream().anyMatch(value -> value.code().equals("REDACTION_DISABLED")));
        assertTrue(result.findings().stream().anyMatch(value -> value.code().equals("CALLER_CANARY_PRESENT")));
        assertTrue(result.findings().stream().anyMatch(value -> value.code().equals("FORBIDDEN_ARTIFACT_REFERENCE")));
    }

    @Test
    void blocksTextThatStillRequiresConfiguredRedaction() {
        var policy = RedactionPolicy.builder().secret("top-secret").build();
        var result = gate.validate("password=top-secret", List.of(), policy, List.of());
        assertEquals(AgentArtifactSecurityGate.Status.BLOCKED, result.status());
        assertTrue(result.findings().stream().anyMatch(value -> value.code().equals("UNREDACTED_TEXT")));
    }
}
