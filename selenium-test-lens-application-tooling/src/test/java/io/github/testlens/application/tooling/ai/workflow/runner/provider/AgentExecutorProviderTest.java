package io.github.testlens.application.tooling.ai.workflow.runner.provider;

import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.ScriptedAgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider.Status;
import static org.junit.jupiter.api.Assertions.*;

class AgentExecutorProviderTest {
    @TempDir Path temporary;

    @Test
    void codexProviderPreflightsTheDirectExecutableAndRequiredExecCapabilities() {
        CapturingProbe probe = new CapturingProbe();
        Path executable = javaExecutable();
        CodexCliAgentExecutorProvider provider = provider(Optional.of(executable), probe);

        AgentExecutorProvider.ProviderAvailability result = provider.preflight();

        assertEquals(Status.AVAILABLE, result.status());
        assertEquals("codex-cli 1.2.3", result.providerVersion());
        assertEquals(List.of(executable.toString(), "--version"), probe.commands.get(0));
        assertEquals(List.of(executable.toString(), "exec", "--help"), probe.commands.get(1));
        assertTrue(probe.commands.stream().allMatch(command -> Path.of(command.get(0)).isAbsolute()));
    }

    @Test
    void buildsExternalRunnerOnlyAfterSuccessfulProfilePreflight() {
        CapturingProbe probe = new CapturingProbe();
        CodexCliAgentExecutorProvider provider = provider(Optional.of(javaExecutable()), probe);

        AgentExecutor executor = provider.executorFor(AgentExecutor.Role.TEST_ARCHITECT, "local-test-architect");

        assertInstanceOf(ExternalAgentRunner.class, executor);
        assertEquals(2, probe.commands.size());
    }

    @Test
    void reportsMissingExecutableWithoutRunningAProbe() {
        CapturingProbe probe = new CapturingProbe();
        CodexCliAgentExecutorProvider provider = provider(Optional.empty(), probe);

        assertEquals(Status.NOT_AVAILABLE, provider.availability().status());
        assertEquals(Status.NOT_AVAILABLE, provider.preflight().status());
        assertTrue(probe.commands.isEmpty());
    }

    @Test
    void rejectsUnsupportedCliWhenARequiredCapabilityIsMissing() {
        CapturingProbe probe = new CapturingProbe();
        probe.help = "Usage: codex exec --ephemeral --sandbox --output-schema";
        CodexCliAgentExecutorProvider provider = provider(Optional.of(javaExecutable()), probe);

        AgentExecutorProvider.ProviderAvailability result = provider.preflight();

        assertEquals(Status.VERSION_UNSUPPORTED, result.status());
        assertTrue(result.reason().contains("--output-last-message"));
        assertThrows(AgentExecutorProvider.ProviderUnavailableException.class,
                () -> provider.executorFor(AgentExecutor.Role.PLANNER, "planner"));
    }

    @Test
    void rejectsInvalidVersionTimeoutOverflowAndLogicalProfileBeforeExecution() {
        CapturingProbe malformed = new CapturingProbe();
        malformed.version = "unknown";
        assertEquals(Status.VERSION_UNSUPPORTED,
                provider(Optional.of(javaExecutable()), malformed).preflight().status());

        CapturingProbe timedOut = new CapturingProbe();
        timedOut.versionResult = new CodexCliAgentExecutorProvider.ProbeResult(true, -1, true, false, "");
        assertEquals(Status.VERSION_UNSUPPORTED,
                provider(Optional.of(javaExecutable()), timedOut).preflight().status());

        CapturingProbe valid = new CapturingProbe();
        CodexCliAgentExecutorProvider provider = provider(Optional.of(javaExecutable()), valid);
        assertEquals(Status.PROFILE_INVALID,
                provider.preflight(AgentExecutor.Role.PLANNER, "unsafe profile").status());
        assertThrows(AgentExecutorProvider.ProviderUnavailableException.class,
                () -> provider.executorFor(AgentExecutor.Role.PLANNER, "unsafe profile"));
        assertTrue(valid.commands.isEmpty(), "invalid profile must fail before process preflight");
    }

    @Test
    void boundsAndSanitizesProviderReasons() {
        String unsafe = "x".repeat(700) + "\u0000secret-control";
        AgentExecutorProvider.ProviderAvailability availability =
                new AgentExecutorProvider.ProviderAvailability(Status.NOT_AVAILABLE, unsafe, "v".repeat(200));

        assertEquals(512, availability.reason().length());
        assertEquals(128, availability.providerVersion().length());
        assertFalse(availability.reason().contains("\u0000"));
    }

    @Test
    void scriptedProviderIsAvailableOnlyWhenExplicitlyConfiguredForTheRole() {
        ScriptedAgentExecutor scripted = new ScriptedAgentExecutor(List.of());
        ScriptedAgentExecutorProvider provider = new ScriptedAgentExecutorProvider(
                Map.of(AgentExecutor.Role.REVIEWER, scripted));

        assertSame(scripted, provider.executorFor(AgentExecutor.Role.REVIEWER, "ci-reviewer"));
        assertEquals(Status.PROFILE_INVALID,
                provider.preflight(AgentExecutor.Role.PLANNER, "ci-planner").status());
        assertThrows(AgentExecutorProvider.ProviderUnavailableException.class,
                () -> provider.executorFor(AgentExecutor.Role.PLANNER, "ci-planner"));
        assertEquals(Status.NOT_AVAILABLE, new ScriptedAgentExecutorProvider(Map.of()).availability().status());
    }

    private CodexCliAgentExecutorProvider provider(Optional<Path> executable, CapturingProbe probe) {
        return new CodexCliAgentExecutorProvider(executable, temporary.resolve("staging").toAbsolutePath(),
                Duration.ofSeconds(2), Duration.ofMinutes(1), probe);
    }

    private Path javaExecutable() {
        String name = System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", name).toAbsolutePath().normalize();
    }

    private static final class CapturingProbe implements CodexCliAgentExecutorProvider.CommandProbe {
        private final List<List<String>> commands = new ArrayList<>();
        private String version = "codex-cli 1.2.3";
        private String help = "Usage: codex exec --ephemeral --sandbox read-only --output-schema FILE "
                + "--output-last-message FILE";
        private CodexCliAgentExecutorProvider.ProbeResult versionResult;

        @Override
        public CodexCliAgentExecutorProvider.ProbeResult run(List<String> command, Duration timeout,
                                                             int maxOutputBytes) {
            commands.add(List.copyOf(command));
            if (command.contains("--version")) {
                return versionResult == null
                        ? new CodexCliAgentExecutorProvider.ProbeResult(true, 0, false, false, version)
                        : versionResult;
            }
            return new CodexCliAgentExecutorProvider.ProbeResult(true, 0, false, false, help);
        }
    }
}
