package io.github.testlens.application.tooling.ai.workflow.runner;

import io.github.testlens.application.tooling.ai.TestPlan;
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
        assertFailure("malformed", AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID);
        assertFailure("wrong-schema", AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID);
        assertFailure("wrong-type", AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID);
    }

    @Test void reportsTimeoutNonZeroAndBoundedOutputFailures() {
        assertFailure("sleep", AgentExecutor.AgentFailureCode.AGENT_TIMEOUT);
        assertFailure("exit", AgentExecutor.AgentFailureCode.AGENT_PROCESS_FAILED);
        assertFailure("oversize", AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID);
        assertFailure("stderr-oversize", AgentExecutor.AgentFailureCode.AGENT_PROCESS_FAILED);
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
    }

    @Test void rejectsUnavailableExecutableSecretEnvironmentAndBadExcerptFingerprint() {
        AgentProfile unavailable = new AgentProfile("missing", temporary.resolve("missing.exe").toAbsolutePath(),
                List.of(), AgentExecutor.Role.TEST_ARCHITECT, Duration.ofSeconds(1), 2_048, 2_048, 512,
                Set.of(), AgentProfile.OutputTransport.STDOUT);
        AgentExecutor.AgentExecutionException missing = assertThrows(AgentExecutor.AgentExecutionException.class,
                () -> runner(unavailable, Map.of()).execute(command(Map.of())));
        assertEquals(AgentExecutor.AgentFailureCode.AGENT_NOT_AVAILABLE, missing.code());

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

    private void assertFailure(String mode, AgentExecutor.AgentFailureCode expected) {
        AgentExecutor.AgentExecutionException failure = assertThrows(AgentExecutor.AgentExecutionException.class,
                () -> runner(profile(mode, AgentProfile.OutputTransport.STDOUT, Set.of()), Map.of())
                        .execute(command(Map.of())));
        assertEquals(expected, failure.code());
    }

    private ExternalAgentRunner runner(AgentProfile profile, Map<String, String> environment) {
        return new ExternalAgentRunner(profile, temporary.toAbsolutePath(), new AgentProtocolCodec(),
                RedactionPolicy.defaults(), List.of(), List.of(), environment);
    }

    private AgentProfile profile(String mode, AgentProfile.OutputTransport transport, Set<String> environment) {
        List<String> arguments = new java.util.ArrayList<>(List.of("-cp", absoluteClasspath(),
                ExternalAgentFixtureMain.class.getName(), mode));
        if (transport == AgentProfile.OutputTransport.FILE) {
            arguments.add(AgentProfile.OUTPUT_FILE);
            arguments.add(AgentProfile.SCHEMA_FILE);
        }
        return new AgentProfile("fixture", javaExecutable(), arguments, AgentExecutor.Role.TEST_ARCHITECT,
                Duration.ofSeconds(1), 2_048, 1_024, 512, environment, transport);
    }

    private static AgentExecutor.AgentCommand command(Map<String, String> instructions) {
        ArtifactEnvelope<String> context = ArtifactEnvelope.create("context", "run-1", List.of(), 1, "bounded-context");
        return new AgentExecutor.AgentCommand("run-1", AgentExecutor.Role.TEST_ARCHITECT,
                List.of(context), instructions);
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
