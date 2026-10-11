package io.github.testlens.application.tooling.ai.security;

import io.github.testlens.core.redaction.RedactionPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentArtifactSecurityGateTest {
    private final AgentArtifactSecurityGate gate = new AgentArtifactSecurityGate();

    @Test
    void acceptsQualifiedJavaApiReferenceThatOnlyLooksLikeCompactJwt() {
        String output = "{\"payload\":{\"pageObjectApisUsed\":["
                + "\"benchmark.LoginPage.invalidPasswordShowsExpectedError(String,String)\"]}}";

        var result = gate.validateStructuredAgentOutput(output, RedactionPolicy.defaults(), List.of(),
                java.util.Set.of("$.payload.sourcePatch"),
                (path, value) -> path.equals("$.payload.pageObjectApisUsed[]")
                        && value.equals("benchmark.LoginPage.invalidPasswordShowsExpectedError(String,String)"));

        assertEquals(AgentArtifactSecurityGate.Status.PASS, result.status(), result.findings().toString());
    }

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

    @Test
    void structuredImplementerGatePreservesNestedJsonProtectionWithoutTreatingJavaAsDiagnostics() {
        var policy = RedactionPolicy.builder().secret("configured-secret-canary").build();
        String safe = "{\"payload\":{\"sourcePatch\":\"String password = \\\"fixture-invalid\\\";\"}}";
        assertEquals(AgentArtifactSecurityGate.Status.PASS,
                gate.validateStructuredAgentOutput(safe, policy, List.of(),
                        java.util.Set.of("$.payload.sourcePatch")).status());

        String nestedSensitiveKey =
                "{\"payload\":{\"sourcePatch\":\"safe\",\"nested\":{\"password\":\"fixture\"}}}";
        var nested = gate.validateStructuredAgentOutput(nestedSensitiveKey, policy, List.of(),
                java.util.Set.of("$.payload.sourcePatch"));
        assertEquals(AgentArtifactSecurityGate.Status.BLOCKED, nested.status());
        assertTrue(nested.findings().stream().anyMatch(finding -> finding.code().equals("SENSITIVE_KEY_VALUE")));

        String sourceSecret = "{\"payload\":{\"sourcePatch\":\"String value = \\\"configured-secret-canary\\\";\"}}";
        var secret = gate.validateStructuredAgentOutput(sourceSecret, policy, List.of(),
                java.util.Set.of("$.payload.sourcePatch"));
        assertEquals(AgentArtifactSecurityGate.Status.BLOCKED, secret.status());
        assertTrue(secret.findings().stream().anyMatch(finding -> finding.code().equals("LITERAL_SECRET")
                && finding.location().equals("$.payload.sourcePatch")));

        String embeddedJson = "{\"payload\":{\"sourcePatch\":"
                + "\"String nested = \\\"{\\\\\\\"password\\\\\\\":\\\\\\\"synthetic\\\\\\\"}\\\";\"}}";
        var embedded = gate.validateStructuredAgentOutput(embeddedJson, policy, List.of(),
                java.util.Set.of("$.payload.sourcePatch"));
        assertEquals(AgentArtifactSecurityGate.Status.BLOCKED, embedded.status());
        assertTrue(embedded.findings().stream().anyMatch(finding ->
                finding.code().equals("SENSITIVE_JSON_KEY_VALUE")
                        && finding.location().equals("$.payload.sourcePatch")));
    }

    @Test
    void generatedSourceGateKeepsCallerCanaryAndCredentialRules() {
        var canary = gate.validateGeneratedSource("String password = \"caller-canary\";",
                RedactionPolicy.defaults(), List.of("caller-canary"));
        assertTrue(canary.findings().stream().anyMatch(finding ->
                finding.code().equals("CALLER_CANARY_PRESENT")));

        var credential = gate.validateGeneratedSource(
                "String authorization = \"Authorization: Bearer synthetic-value\";",
                RedactionPolicy.defaults(), List.of());
        assertTrue(credential.findings().stream().anyMatch(finding ->
                finding.code().equals("AUTHORIZATION_CREDENTIAL")));
    }

    @Test
    void structuredGateFindsJsonEscapedCanaryWithoutExposingIt() {
        String canary = "caller\"canary\\line\nend";
        String json = "{\"payload\":{\"sourcePatch\":\"caller\\\"canary\\\\line\\nend\"}}";

        var result = gate.validateStructuredAgentOutput(json, RedactionPolicy.defaults(), List.of(canary),
                java.util.Set.of("$.payload.sourcePatch"));

        assertEquals(AgentArtifactSecurityGate.Status.BLOCKED, result.status());
        assertTrue(result.findings().stream().anyMatch(finding ->
                finding.code().equals("CALLER_CANARY_PRESENT")
                        && finding.location().equals("$.decodedString")));
        assertTrue(result.findings().stream().allMatch(finding -> finding.location().length() <= 160));
        assertFalse(result.findings().toString().contains(canary));
    }
}
