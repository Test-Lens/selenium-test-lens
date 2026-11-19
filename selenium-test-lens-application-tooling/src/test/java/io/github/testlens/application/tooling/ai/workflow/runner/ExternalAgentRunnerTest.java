package io.github.testlens.application.tooling.ai.workflow.runner;

import io.github.testlens.application.tooling.ai.*;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope;
import io.github.testlens.core.redaction.RedactionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExternalAgentRunnerTest {
    @TempDir Path temporary;

    @Test void executesStructuredAgentInIsolatedDirectoryAndKeepsProvenance() throws Exception {
        java.util.concurrent.atomic.AtomicReference<ExternalAgentRunner.ExecutionMetrics> metrics =
                new java.util.concurrent.atomic.AtomicReference<>();
        ExternalAgentRunner runner = new ExternalAgentRunner(
                profile("valid", AgentProfile.OutputTransport.STDOUT, Set.of()), temporary.toAbsolutePath(),
                new AgentProtocolCodec(), RedactionPolicy.defaults(), List.of(), List.of(), Map.of(), metrics::set);
        AgentExecutor.AgentResult result = runner.execute(command(Map.of("task", "plan")));

        assertInstanceOf(TestPlan.class, result.artifact().payload());
        assertEquals("run-1", result.artifact().runId());
        assertEquals(List.of("context"), result.artifact().parents());
        assertEquals(2, result.artifact().attempt());
        assertTrue(result.providerRequestId().startsWith("fixture:"));
        assertEquals(ExternalAgentRunner.ExecutionStatus.SUCCESS, metrics.get().status());
        assertTrue(metrics.get().inputBytes() > 0);
        assertTrue(metrics.get().outputBytes() > 0);
        assertTrue(metrics.get().durationMillis() >= 0);
        try (var children = Files.list(temporary)) { assertEquals(0, children.count()); }
    }

    @Test void supportsSafeOutputFileTransportAndSchemaPlaceholder() throws Exception {
        AgentProfile profile = profile("file-valid", AgentProfile.OutputTransport.FILE, Set.of());
        AgentExecutor.AgentResult result = runner(profile, Map.of()).execute(command(Map.of()));
        assertInstanceOf(TestPlan.class, result.artifact().payload());
    }

    @Test void passesOnlyAllowlistedEnvironmentAndUsesIsolatedWorkingDirectory() throws Exception {
        AgentProfile profile = profile("environment", AgentProfile.OutputTransport.STDOUT,
                Set.of("TEST_LENS_SAFE_ENV"));
        ExternalAgentRunner runner = runner(profile,
                Map.of("TEST_LENS_SAFE_ENV", "visible", "TEST_LENS_BLOCKED_ENV", "must-not-pass"));
        assertInstanceOf(TestPlan.class, runner.execute(command(Map.of())).artifact().payload());
    }

    @Test void rejectsMalformedWrongSchemaAndWrongRoleResult() {
        assertFailure("malformed", AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID, "MALFORMED_JSON", "$");
        assertFailure("wrong-schema", AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID,
                "SCHEMA_VERSION_UNSUPPORTED", "$.schemaVersion");
        assertFailure("wrong-type", AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID,
                "ROLE_TYPE_MISMATCH", "$.resultType");
    }

    @Test void distinguishesEmptyTruncatedStructuralAndDomainInvalidOutput() {
        assertFailure("empty", AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID, "OUTPUT_EMPTY", "result");
        assertFailure("truncated", AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID, "TRUNCATED_JSON", "$");
        assertFailure("truncated-string", AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID, "TRUNCATED_JSON", "$");
        assertFailure("missing-field", AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID,
                "REQUIRED_FIELD_MISSING", "$.payload.scenarios");
        assertFailure("domain-invalid", AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID,
                "DOMAIN_MAPPING_FAILED", "$.payload");
    }

    @Test void reportsTimeoutNonZeroAndBoundedOutputFailures() {
        assertFailure("sleep", AgentExecutor.AgentFailureCode.AGENT_TIMEOUT, "PROCESS_TIMEOUT", "process.wait");
        assertFailure("exit", AgentExecutor.AgentFailureCode.AGENT_PROCESS_FAILED,
                "PROCESS_NONZERO_EXIT", "process.exit");
        assertFailure("oversize", AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID, "OUTPUT_OVERSIZE", "result");
        assertFailure("stderr-oversize", AgentExecutor.AgentFailureCode.AGENT_PROCESS_FAILED,
                "DIAGNOSTIC_OVERFLOW", "process.diagnostics");
    }

    @Test void rejectsOversizedOrSecretBearingContextBeforeStartingProcess() {
        AgentProfile small = profile("valid", AgentProfile.OutputTransport.STDOUT, Set.of());
        ExternalAgentRunner runner = runner(small, Map.of());
        AgentExecutor.AgentExecutionException oversized = assertThrows(AgentExecutor.AgentExecutionException.class,
                () -> runner.execute(command(Map.of("large", "x".repeat(4_000)))));
        assertEquals(AgentExecutor.AgentFailureCode.AGENT_CONTEXT_REJECTED, oversized.code());

        ExternalAgentRunner secured = new ExternalAgentRunner(small, temporary.toAbsolutePath(),
                new AgentProtocolCodec(), RedactionPolicy.defaults(), List.of("password-canary-value"),
                List.of(), Map.of());
        AgentExecutor.AgentExecutionException canary = assertThrows(AgentExecutor.AgentExecutionException.class,
                () -> secured.execute(command(Map.of("credential", "password-canary-value"))));
        assertEquals(AgentExecutor.AgentFailureCode.AGENT_CONTEXT_REJECTED, canary.code());
        assertEquals("CALLER_CANARY_PRESENT", canary.diagnostic().reasonCode());
        assertEquals("command", canary.diagnostic().location());

        assertFailure("unsafe-output", AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID,
                "UNREDACTED_TEXT", "result");
    }

    @Test void rejectsUnavailableExecutableSecretEnvironmentAndBadExcerptFingerprint() {
        AgentProfile unavailable = new AgentProfile("missing", temporary.resolve("missing.exe").toAbsolutePath(),
                List.of(), AgentExecutor.Role.TEST_ARCHITECT, Duration.ofSeconds(1), 2_048, 2_048, 512,
                Set.of(), AgentProfile.OutputTransport.STDOUT);
        AgentExecutor.AgentExecutionException missing = assertThrows(AgentExecutor.AgentExecutionException.class,
                () -> runner(unavailable, Map.of()).execute(command(Map.of())));
        assertEquals(AgentExecutor.AgentFailureCode.AGENT_NOT_AVAILABLE, missing.code());
        assertEquals("PROCESS_START_FAILED", missing.diagnostic().reasonCode());
        assertEquals("process.executable", missing.diagnostic().location());

        assertThrows(IllegalArgumentException.class, () -> new AgentProfile("unsafe", javaExecutable(), List.of(),
                AgentExecutor.Role.TEST_ARCHITECT, Duration.ofSeconds(1), 2_048, 2_048, 512,
                Set.of("GITHUB_TOKEN"), AgentProfile.OutputTransport.STDOUT));

        AgentProfile valid = profile("valid", AgentProfile.OutputTransport.STDOUT, Set.of());
        ExternalAgentRunner badExcerpt = new ExternalAgentRunner(valid, temporary.toAbsolutePath(),
                new AgentProtocolCodec(), RedactionPolicy.defaults(), List.of(),
                List.of(new ExternalAgentRunner.SourceExcerpt(Path.of("LoginPage.java"), "class LoginPage {}", "wrong")),
                Map.of());
        AgentExecutor.AgentExecutionException mismatch = assertThrows(AgentExecutor.AgentExecutionException.class,
                () -> badExcerpt.execute(command(Map.of())));
        assertEquals(AgentExecutor.AgentFailureCode.AGENT_CONTEXT_REJECTED, mismatch.code());
        assertThrows(IllegalArgumentException.class, () -> new ExternalAgentRunner.SourceExcerpt(
                Path.of("..", "outside.java"), "class Outside {}", ArtifactEnvelope.digest("class Outside {}")));
    }

    @Test void stagesOnlyExplicitFingerprintCheckedSourceExcerpts() throws Exception {
        String source = "class LoginPage {}";
        AgentProfile valid = profile("excerpt", AgentProfile.OutputTransport.STDOUT, Set.of());
        ExternalAgentRunner runner = new ExternalAgentRunner(valid, temporary.toAbsolutePath(),
                new AgentProtocolCodec(), RedactionPolicy.defaults(), List.of(),
                List.of(new ExternalAgentRunner.SourceExcerpt(Path.of("LoginPage.java"), source,
                        ArtifactEnvelope.digest(source))), Map.of());
        assertInstanceOf(TestPlan.class, runner.execute(command(Map.of())).artifact().payload());
    }

    @Test void testPlanSchemaRequiresPositiveStepOrder() {
        String schema = new String(new AgentProtocolCodec().outputSchema(AgentExecutor.Role.TEST_ARCHITECT),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(schema.contains("\"order\":{\"minimum\":1,\"type\":\"integer\"}"),
                "the positive constraint must apply specifically to TestPlan steps[].order");
    }

    @Test void implementationSchemaRejectsEmptySourcePatchAtTheProviderBoundary() {
        String schema = new String(new AgentProtocolCodec().outputSchema(AgentExecutor.Role.TEST_IMPLEMENTER),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(schema.contains("\"sourcePatch\":{\"minLength\":1,\"type\":\"string\"}"),
                "the provider schema must match the decoder's non-empty sourcePatch contract");
    }

    @Test void implementationPromptRequiresSyntheticInlineCredentialTestData() {
        String command = new String(new AgentProtocolCodec().encodeCommand(new AgentExecutor.AgentCommand(
                "run-1", AgentExecutor.Role.TEST_IMPLEMENTER, List.of(), Map.of())),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(command.contains("synthetic test placeholders inline"));
        assertTrue(command.contains("do not embed or assign real credentials"));
        assertTrue(command.contains("must be unchanged by credential redaction"));
        assertTrue(command.contains("including in declarations or comments"));
    }

    @Test void acceptsSafeJavaPasswordIdentifierInStructuredImplementerOutput() throws Exception {
        AgentProfile implementer = profile("implementer-safe-password", AgentProfile.OutputTransport.STDOUT,
                Set.of(), AgentExecutor.Role.TEST_IMPLEMENTER);

        AgentExecutor.AgentResult result = runner(implementer, Map.of()).execute(
                implementerCommand("fp"));

        TestImplementationProposal proposal = assertInstanceOf(TestImplementationProposal.class,
                result.artifact().payload());
        assertTrue(proposal.sourcePatch().contains("String password"));
        assertTrue(proposal.sourcePatch().contains("fixture-invalid"));
        assertEquals(List.of("benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)"),
                proposal.pageObjectApisUsed());
    }

    @Test void acceptsAndCanonicalizesAnUnambiguousEquivalentReferenceThroughTheRealBoundary() throws Exception {
        AgentProfile implementer = profile("implementer-equivalent-reference", AgentProfile.OutputTransport.STDOUT,
                Set.of(), AgentExecutor.Role.TEST_IMPLEMENTER);

        AgentExecutor.AgentResult result = runner(implementer, Map.of()).execute(implementerCommand("fp"));

        TestImplementationProposal proposal = assertInstanceOf(TestImplementationProposal.class,
                result.artifact().payload());
        assertEquals(List.of("benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)"),
                proposal.pageObjectApisUsed());
    }

    @Test void acceptsCanonicalAndLegacyReferencesButAlwaysReturnsCanonicalMetadata() throws Exception {
        for (String mode : List.of("implementer-safe-password", "implementer-legacy-reference")) {
            AgentExecutor.AgentResult result = runner(profile(mode, AgentProfile.OutputTransport.STDOUT,
                    Set.of(), AgentExecutor.Role.TEST_IMPLEMENTER), Map.of()).execute(implementerCommand("fp"));
            TestImplementationProposal proposal = assertInstanceOf(TestImplementationProposal.class,
                    result.artifact().payload());
            assertEquals(List.of("benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)"),
                    proposal.pageObjectApisUsed());
        }
    }

    @Test void acceptsEveryManifestReferenceIncludingOverloadsAndInheritedMethods() throws Exception {
        AgentProfile implementer = profile("implementer-all-manifest-references",
                AgentProfile.OutputTransport.STDOUT, Set.of(), AgentExecutor.Role.TEST_IMPLEMENTER);

        TestImplementationProposal proposal = assertInstanceOf(TestImplementationProposal.class,
                runner(implementer, Map.of()).execute(implementerCommandWithAllReferences("fp")).artifact().payload());

        assertEquals(List.of(
                "benchmark.LoginPage#checkStatus(String)",
                "benchmark.LoginPage#checkStatus(int)",
                "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)",
                "benchmark.LoginPage#recordAttempt()",
                "benchmark.LoginPage#waitUntilReady()"), proposal.pageObjectApisUsed());
    }

    @Test void acceptsCapabilityAlternativesWithoutProposalOnlyFields() throws Exception {
        AgentExecutor.AgentResult missing = runner(profile("implementer-capability-missing",
                AgentProfile.OutputTransport.STDOUT, Set.of(), AgentExecutor.Role.TEST_IMPLEMENTER), Map.of())
                .execute(implementerCommand("fp"));
        AgentExecutor.AgentResult extension = runner(profile("implementer-extension-proposal",
                AgentProfile.OutputTransport.STDOUT, Set.of(), AgentExecutor.Role.TEST_IMPLEMENTER), Map.of())
                .execute(implementerCommand("fp"));

        assertInstanceOf(io.github.testlens.application.tooling.ai.workflow.PageObjectCapabilityMissing.class,
                missing.artifact().payload());
        assertInstanceOf(io.github.testlens.application.tooling.ai.workflow.PageObjectExtensionProposal.class,
                extension.artifact().payload());
    }

    @Test void rejectsStructuralImplementerVariantsAtPreciseSafeLocations() {
        assertImplementerDecodeFailure("implementer-missing-references", "REQUIRED_FIELD_MISSING",
                "$.result.payload.pageObjectApisUsed");
        assertImplementerDecodeFailure("implementer-reference-string", "FIELD_TYPE_INVALID",
                "$.result.payload.pageObjectApisUsed");
        assertImplementerDecodeFailure("implementer-reference-number", "ARRAY_ELEMENT_INVALID",
                "$.result.payload.pageObjectApisUsed[0]");
        assertImplementerDecodeFailure("implementer-result-payload-mismatch", "RESULT_VARIANT_INVALID",
                "$.result.payload");
        assertImplementerDecodeFailure("implementer-capability-with-proposal-field", "RESULT_VARIANT_INVALID",
                "$.result.payload");
        assertImplementerDecodeFailure("implementer-capability-number", "FIELD_TYPE_INVALID",
                "$.result.payload.capability");
        assertImplementerDecodeFailure("implementer-extension-evidence-number", "ARRAY_ELEMENT_INVALID",
                "$.result.payload.evidence[0]");
        assertImplementerDecodeFailure("implementer-mixed-envelope", "ROOT_STRUCTURE_INVALID", "$");
    }

    @Test void rejectsBlankButAcceptsEmptyReferencesAndReturnsVerifiedCanonicalUsage() throws Exception {
        assertImplementerDecodeFailure("implementer-blank-reference", "FIELD_TYPE_INVALID",
                "$.result.payload.pageObjectApisUsed[0]");
        AgentExecutor.AgentResult result = runner(profile("implementer-empty-references",
                AgentProfile.OutputTransport.STDOUT, Set.of(), AgentExecutor.Role.TEST_IMPLEMENTER), Map.of())
                .execute(implementerCommand("fp"));
        TestImplementationProposal proposal = assertInstanceOf(TestImplementationProposal.class,
                result.artifact().payload());
        assertEquals(List.of("benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)"),
                proposal.pageObjectApisUsed());
    }

    @Test void structuredImplementerOutputStillBlocksRealSensitiveCategories() {
        assertImplementerSecurityFailure("implementer-literal-secret",
                RedactionPolicy.builder().secret("configured-secret-canary").build(), List.of(),
                "LITERAL_SECRET", "$.result.payload.sourcePatch");
        assertImplementerSecurityFailure("implementer-bearer", RedactionPolicy.defaults(), List.of(),
                "AUTHORIZATION_CREDENTIAL", "$.result.payload.sourcePatch");
        assertImplementerSecurityFailure("implementer-jwt", RedactionPolicy.defaults(), List.of(),
                "JWT", "$.result.payload.sourcePatch");
        assertImplementerSecurityFailure("implementer-jwt-comment", RedactionPolicy.defaults(), List.of(),
                "JWT", "$.result.payload.sourcePatch");
        assertImplementerSecurityFailure("implementer-jwt-metadata", RedactionPolicy.defaults(), List.of(),
                "JWT", "$.result.payload.pageObjectApisUsed[item]");
        assertImplementerSecurityFailure("implementer-canary", RedactionPolicy.defaults(), List.of("caller-canary"),
                "CALLER_CANARY_PRESENT", "$.decodedString");
        AgentProfile appended = profile("implementer-appended-secret", AgentProfile.OutputTransport.STDOUT,
                Set.of(), AgentExecutor.Role.TEST_IMPLEMENTER);
        AgentExecutor.AgentExecutionException appendedFailure = assertThrows(AgentExecutor.AgentExecutionException.class,
                () -> new ExternalAgentRunner(appended, temporary.toAbsolutePath(), new AgentProtocolCodec(),
                        RedactionPolicy.builder().secret("configured-secret-canary").build(), List.of(), List.of(),
                        Map.of()).execute(implementerCommand("fp")));
        assertEquals(AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID, appendedFailure.code());
        assertEquals("LITERAL_SECRET", appendedFailure.diagnostic().reasonCode());
        assertEquals("$.result.payload.pageObjectApisUsed[item]", appendedFailure.diagnostic().location());
        assertFalse(appendedFailure.getMessage().contains("configured-secret-canary"));
    }

    @Test void derivesTrustedMetadataFromSourceInsteadOfAgentDeclarations() throws Exception {
        for (String mode : List.of("implementer-missing-method", "implementer-wrong-arity",
                "implementer-invalid-reference")) {
            TestImplementationProposal proposal = assertInstanceOf(TestImplementationProposal.class,
                    runner(profile(mode, AgentProfile.OutputTransport.STDOUT, Set.of(),
                            AgentExecutor.Role.TEST_IMPLEMENTER), Map.of()).execute(implementerCommand("fp"))
                            .artifact().payload());
            assertEquals(List.of("benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)"),
                    proposal.pageObjectApisUsed());
        }
        assertImplementerContractFailure("implementer-simple-reference", "stale-fingerprint",
                "PAGE_OBJECT_MANIFEST_STALE");
    }

    @Test void sourceVerificationFailuresUseNestedSourcePatchLocation() {
        assertImplementerContractFailure("implementer-source-missing-method", "fp",
                PageObjectApiReferenceValidator.METHOD_NOT_IN_MANIFEST, "$.result.payload.sourcePatch");
        assertImplementerContractFailure("implementer-source-wrong-signature", "fp",
                PageObjectApiReferenceValidator.METHOD_SIGNATURE_MISMATCH, "$.result.payload.sourcePatch");
        assertImplementerContractFailure("implementer-source-unresolved-receiver", "fp",
                PageObjectApiReferenceValidator.RECEIVER_UNRESOLVED, "$.result.payload.sourcePatch");
        assertImplementerContractFailure("implementer-source-parse-failure", "fp",
                PageObjectApiReferenceValidator.SOURCE_PARSE_FAILED, "$.result.payload.sourcePatch");
    }

    @Test void codecReportsMissingAndUnexpectedFieldsWithoutValues() {
        byte[] document = """
                {"schemaVersion":1,"resultType":"TEST_PLAN","payload":{"header":{"schemaVersion":1,"status":"READY","evidence":[],"limitations":[],"confidence":"AI_PROPOSED"},"unexpected":[]}}
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        io.github.testlens.application.tooling.json.StrictJson.JsonFormatException failure = assertThrows(
                io.github.testlens.application.tooling.json.StrictJson.JsonFormatException.class,
                () -> new AgentProtocolCodec().decode(AgentExecutor.Role.TEST_ARCHITECT, document));
        assertEquals(AgentProtocolCodec.DecodeFailure.REQUIRED_FIELD_MISSING,
                AgentProtocolCodec.decodeFailure(failure));
        assertEquals("$.payload.scenarios", AgentProtocolCodec.decodeLocation(failure));
        assertEquals("A required external agent field is missing", AgentProtocolCodec.decodeDetail(failure));
        assertTrue(failure.getMessage().contains("REQUIRED_FIELD_MISSING"));
        assertFalse(failure.getMessage().contains("unexpected]"));
        assertFalse(failure.getMessage().contains("AI_PROPOSED"));
    }

    @Test void diagnosticContractIsBoundedNonSensitiveAndOldConstructorsRemainUsable() {
        AgentExecutor.AgentExecutionException legacy = new AgentExecutor.AgentExecutionException(
                AgentExecutor.AgentFailureCode.AGENT_PROCESS_FAILED, "legacy", (Throwable) null);
        assertNull(legacy.diagnostic());
        assertThrows(IllegalArgumentException.class, () -> new AgentExecutor.AgentFailureDiagnostic(
                "PROCESS_FAILED", "process", Map.of("accessToken", "not-recorded")));
        assertThrows(IllegalArgumentException.class, () -> new AgentExecutor.AgentFailureDiagnostic(
                "PROCESS_FAILED", "process", Map.of("status", "x".repeat(161))));
        assertThrows(IllegalArgumentException.class, () -> new AgentExecutor.AgentFailureDiagnostic(
                "PROCESS_FAILED", "process", Map.of("detail", "password-secret")));
        assertDoesNotThrow(() -> new AgentExecutor.AgentFailureDiagnostic(
                "SCHEMA_INVALID", "$.payload.missingCapabilities[]", Map.of()));
        assertDoesNotThrow(() -> new AgentExecutor.AgentFailureDiagnostic(
                "SCHEMA_INVALID", "$.payload.findings[]", Map.of()));
    }

    private void assertFailure(String mode, AgentExecutor.AgentFailureCode expected,
                               String reasonCode, String location) {
        AgentExecutor.AgentExecutionException failure = assertThrows(AgentExecutor.AgentExecutionException.class,
                () -> runner(profile(mode, AgentProfile.OutputTransport.STDOUT, Set.of()), Map.of())
                        .execute(command(Map.of())));
        assertEquals(expected, failure.code());
        assertNotNull(failure.diagnostic());
        assertEquals(reasonCode, failure.diagnostic().reasonCode());
        assertEquals(location, failure.diagnostic().location());
        assertEquals("TEST_ARCHITECT", failure.diagnostic().metadata().get("role"));
        assertEquals("fixture", failure.diagnostic().metadata().get("profileId"));
        assertNotNull(failure.diagnostic().metadata().get("inputBytes"));
        assertNotNull(failure.diagnostic().metadata().get("outputBytes"));
        assertNotNull(failure.diagnostic().metadata().get("durationMillis"));
        assertNotNull(failure.diagnostic().metadata().get("processStatus"));
        if ("exit".equals(mode)) assertEquals("7", failure.diagnostic().metadata().get("exitCode"));
    }

    private void assertImplementerSecurityFailure(String mode, RedactionPolicy policy, List<String> canaries,
                                                   String reason, String location) {
        AgentProfile profile = profile(mode, AgentProfile.OutputTransport.STDOUT, Set.of(),
                AgentExecutor.Role.TEST_IMPLEMENTER);
        ExternalAgentRunner secured = new ExternalAgentRunner(profile, temporary.toAbsolutePath(),
                new AgentProtocolCodec(), policy, canaries, List.of(), Map.of());
        AgentExecutor.AgentExecutionException failure = assertThrows(AgentExecutor.AgentExecutionException.class,
                () -> secured.execute(implementerCommand("fp")));
        assertEquals(AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID, failure.code());
        assertEquals(reason, failure.diagnostic().reasonCode());
        assertEquals(location, failure.diagnostic().location());
        assertFalse(failure.getMessage().contains("configured-secret-canary"));
        assertFalse(failure.getMessage().contains("caller-canary"));
        assertFalse(failure.diagnostic().metadata().toString().contains("configured-secret-canary"));
        assertFalse(failure.diagnostic().metadata().toString().contains("caller-canary"));
    }

    private void assertImplementerContractFailure(String mode, String fingerprint, String reason) {
        assertImplementerContractFailure(mode, fingerprint, reason, "$.result.payload.sourcePatch");
    }

    private void assertImplementerContractFailure(String mode, String fingerprint, String reason, String location) {
        AgentProfile profile = profile(mode, AgentProfile.OutputTransport.STDOUT, Set.of(),
                AgentExecutor.Role.TEST_IMPLEMENTER);
        AgentExecutor.AgentExecutionException failure = assertThrows(AgentExecutor.AgentExecutionException.class,
                () -> runner(profile, Map.of()).execute(implementerCommand(fingerprint)));
        assertEquals(AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID, failure.code());
        assertEquals(reason, failure.diagnostic().reasonCode());
        assertEquals(location, failure.diagnostic().location());
        Map<String,String> metadata=failure.diagnostic().metadata();
        assertEquals("SOURCE_API_VERIFICATION",metadata.get("validationStage"));
        for(String key:List.of("ruleId","problemCategory","astNodeKind","receiverCategory","manifestMatch",
                "fingerprintStatus","signatureStatus")){
            assertNotNull(metadata.get(key),key+" must survive the external-agent boundary");
            assertNotEquals("UNKNOWN",metadata.get(key),key+" must remain a precise static value");
        }
        assertFalse(metadata.toString().contains("benchmark.LoginPage"));
        assertFalse(metadata.toString().contains("invalidPasswordShowsExpectedError"));
    }

    private void assertImplementerDecodeFailure(String mode, String reason, String location) {
        AgentProfile profile = profile(mode, AgentProfile.OutputTransport.STDOUT, Set.of(),
                AgentExecutor.Role.TEST_IMPLEMENTER);
        AgentExecutor.AgentExecutionException failure = assertThrows(AgentExecutor.AgentExecutionException.class,
                () -> runner(profile, Map.of()).execute(implementerCommand("fp")));
        assertEquals(AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID, failure.code());
        assertEquals(reason, failure.diagnostic().reasonCode());
        assertEquals(location, failure.diagnostic().location());
        assertFalse(failure.getMessage().contains("benchmark.LoginPage"));
        assertFalse(failure.getMessage().contains("GeneratedTest"));
    }

    private ExternalAgentRunner runner(AgentProfile profile, Map<String, String> environment) {
        return new ExternalAgentRunner(profile, temporary.toAbsolutePath(), new AgentProtocolCodec(),
                RedactionPolicy.defaults(), List.of(), List.of(), environment);
    }

    private AgentProfile profile(String mode, AgentProfile.OutputTransport transport, Set<String> environment) {
        return profile(mode, transport, environment, AgentExecutor.Role.TEST_ARCHITECT);
    }

    private AgentProfile profile(String mode, AgentProfile.OutputTransport transport, Set<String> environment,
                                 AgentExecutor.Role role) {
        List<String> arguments = new java.util.ArrayList<>(List.of("-cp", absoluteClasspath(),
                ExternalAgentFixtureMain.class.getName(), mode));
        if (transport == AgentProfile.OutputTransport.FILE) {
            arguments.add(AgentProfile.OUTPUT_FILE);
            arguments.add(AgentProfile.SCHEMA_FILE);
        }
        return new AgentProfile("fixture", javaExecutable(), arguments, role,
                Duration.ofSeconds(1), role == AgentExecutor.Role.TEST_IMPLEMENTER ? 8_192 : 2_048,
                1_024, 512, environment, transport);
    }

    private static AgentExecutor.AgentCommand command(Map<String, String> instructions) {
        return command(instructions, AgentExecutor.Role.TEST_ARCHITECT);
    }

    private static AgentExecutor.AgentCommand command(Map<String, String> instructions, AgentExecutor.Role role) {
        ArtifactEnvelope<String> context = ArtifactEnvelope.create("context", "run-1", List.of(), 1, "bounded-context");
        return new AgentExecutor.AgentCommand("run-1", role,
                List.of(context), instructions);
    }

    private static AgentExecutor.AgentCommand implementerCommand(String currentFingerprint) {
        AgentContextPack.MethodCapability method = new AgentContextPack.MethodCapability(
                "method-1", "invalidPasswordShowsExpectedError",
                "invalidPasswordShowsExpectedError(String,String)", List.of(
                new AgentContextPack.CapabilityParameter("username", "String"),
                new AgentContextPack.CapabilityParameter("password", "String")),
                "boolean", false, "ASSERTION", List.of());
        return implementerCommand(currentFingerprint, List.of(method));
    }

    private static AgentExecutor.AgentCommand implementerCommandWithAllReferences(String currentFingerprint) {
        AgentContextPack.MethodCapability invalid = new AgentContextPack.MethodCapability(
                "method-1", "invalidPasswordShowsExpectedError",
                "invalidPasswordShowsExpectedError(String,String)", List.of(
                new AgentContextPack.CapabilityParameter("username", "String"),
                new AgentContextPack.CapabilityParameter("password", "String")),
                "boolean", false, "ASSERTION", List.of());
        AgentContextPack.MethodCapability record = new AgentContextPack.MethodCapability(
                "method-2", "recordAttempt", "recordAttempt()", List.of(),
                "void", false, "ACTION", List.of());
        AgentContextPack.MethodCapability stringStatus = new AgentContextPack.MethodCapability(
                "method-3", "checkStatus", "checkStatus(String)",
                List.of(new AgentContextPack.CapabilityParameter("status", "String")),
                "boolean", false, "ASSERTION", List.of());
        AgentContextPack.MethodCapability intStatus = new AgentContextPack.MethodCapability(
                "method-4", "checkStatus", "checkStatus(int)",
                List.of(new AgentContextPack.CapabilityParameter("status", "int")),
                "boolean", false, "ASSERTION", List.of());
        AgentContextPack.MethodCapability inherited = new AgentContextPack.MethodCapability(
                "method-5", "waitUntilReady", "waitUntilReady()", List.of(),
                "void", false, "benchmark.BasePage", "WAIT", List.of());
        return implementerCommand(currentFingerprint,
                List.of(invalid, record, stringStatus, intStatus, inherited));
    }

    private static AgentExecutor.AgentCommand implementerCommand(String currentFingerprint,
                                                                  List<AgentContextPack.MethodCapability> methods) {
        AgentContextPack.PageObjectCapability owner = new AgentContextPack.PageObjectCapability(
                "class-1", "login", "benchmark.LoginPage", "src/test/java/benchmark/LoginPage.java",
                "PUBLIC", List.of(new AgentContextPack.ConstructorCapability(
                        "LoginPage()", List.of(), "PUBLIC", "DIRECT_CONSTRUCTION", true)), methods);
        AgentContextPack.PageObjectCapabilityManifest manifest =
                new AgentContextPack.PageObjectCapabilityManifest("fp",
                        AgentContextPack.ManifestCompleteness.COMPLETE, List.of(owner), List.of());
        ContractHeader header = new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of("fixture"), List.of(), ContractHeader.Confidence.OBSERVED);
        AgentContextPack context = new AgentContextPack(header,
                new AgentTask(header, AgentTask.TaskType.CREATE_TEST, "invalid login", List.of("login"),
                        List.of(), List.of(), List.of("Page Objects only")),
                List.of(), Map.of(), List.of(), AgentContextPack.SourceContext.empty(), manifest,
                List.of(), List.of(), AgentContextPack.Completeness.COMPLETE_FOR_REQUESTED_SCOPE);
        ArtifactEnvelope<AgentContextPack> artifact = ArtifactEnvelope.create(
                "context", "run-1", List.of(), 1, context);
        return new AgentExecutor.AgentCommand("run-1", AgentExecutor.Role.TEST_IMPLEMENTER,
                List.of(artifact), Map.of("testLens.pageObjectSourceFingerprint", currentFingerprint));
    }

    private static Path javaExecutable() {
        String name = System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", name).toAbsolutePath();
    }

    private static String absoluteClasspath() {
        Path base = Path.of("").toAbsolutePath();
        return Arrays.stream(System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator)))
                .map(Path::of).map(path -> path.isAbsolute() ? path : base.resolve(path).normalize())
                .map(Path::toString).collect(java.util.stream.Collectors.joining(File.pathSeparator));
    }
}
