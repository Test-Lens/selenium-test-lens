package io.github.testlens.application.tooling.ai.workflow.runner.provider;

import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.runner.CodexCliDetector;
import io.github.testlens.application.tooling.ai.workflow.runner.CodexCliProfiles;
import io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Fail-closed adapter for a directly executable Codex CLI installation. @since 0.5.0 */
public final class CodexCliAgentExecutorProvider implements AgentExecutorProvider {
    private static final Duration DEFAULT_PREFLIGHT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_EXECUTION_TIMEOUT = Duration.ofMinutes(3);
    private static final int MAX_PREFLIGHT_BYTES = 64 * 1024;
    private static final Set<String> REQUIRED_EXEC_OPTIONS = Set.of(
            "--ephemeral", "--sandbox", "--output-schema", "--output-last-message");

    private final Optional<Path> executable;
    private final Path stagingRoot;
    private final Duration preflightTimeout;
    private final Duration executionTimeout;
    private final CommandProbe probe;

    public CodexCliAgentExecutorProvider(Path stagingRoot) {
        this(CodexCliDetector.detect(), stagingRoot, DEFAULT_PREFLIGHT_TIMEOUT,
                DEFAULT_EXECUTION_TIMEOUT, new ProcessCommandProbe());
    }

    public CodexCliAgentExecutorProvider(Path executable, Path stagingRoot) {
        this(Optional.of(requireAbsolute(executable, "executable")), stagingRoot, DEFAULT_PREFLIGHT_TIMEOUT,
                DEFAULT_EXECUTION_TIMEOUT, new ProcessCommandProbe());
    }

    CodexCliAgentExecutorProvider(Optional<Path> executable, Path stagingRoot, Duration preflightTimeout,
                                 Duration executionTimeout, CommandProbe probe) {
        this.executable = Objects.requireNonNull(executable, "executable")
                .map(value -> requireAbsolute(value, "executable").normalize());
        this.stagingRoot = requireAbsolute(stagingRoot, "stagingRoot").normalize();
        this.preflightTimeout = boundedDuration(preflightTimeout, "preflightTimeout", Duration.ofSeconds(1),
                Duration.ofSeconds(30));
        this.executionTimeout = boundedDuration(executionTimeout, "executionTimeout", Duration.ofSeconds(1),
                Duration.ofMinutes(30));
        this.probe = Objects.requireNonNull(probe, "probe");
    }

    @Override
    public ProviderAvailability availability() {
        if (executable.isEmpty()) {
            return new ProviderAvailability(Status.NOT_AVAILABLE, "Codex CLI executable was not found", null);
        }
        Path candidate = executable.orElseThrow();
        if (!Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS) || !Files.isExecutable(candidate)) {
            return new ProviderAvailability(Status.NOT_AVAILABLE,
                    "Configured Codex CLI executable is unavailable", null);
        }
        return new ProviderAvailability(Status.AVAILABLE, "Codex CLI executable is available", null);
    }

    @Override
    public ProviderAvailability preflight() {
        ProviderAvailability local = availability();
        if (!local.available()) return local;
        Path command = executable.orElseThrow();
        ProbeResult version = probe.run(List.of(command.toString(), "--version"), preflightTimeout,
                MAX_PREFLIGHT_BYTES);
        if (!version.started()) {
            return new ProviderAvailability(Status.NOT_AVAILABLE, "Codex CLI version preflight could not start", null);
        }
        String providerVersion = versionLine(version.output());
        if (version.timedOut() || version.overflow() || version.exitCode() != 0 || providerVersion == null) {
            return new ProviderAvailability(Status.VERSION_UNSUPPORTED,
                    "Codex CLI version preflight was not successful", providerVersion);
        }

        ProbeResult help = probe.run(List.of(command.toString(), "exec", "--help"), preflightTimeout,
                MAX_PREFLIGHT_BYTES);
        if (!help.started()) {
            return new ProviderAvailability(Status.NOT_AVAILABLE, "Codex CLI capability preflight could not start",
                    providerVersion);
        }
        if (help.timedOut() || help.overflow() || help.exitCode() != 0) {
            return new ProviderAvailability(Status.VERSION_UNSUPPORTED,
                    "Codex CLI exec capability preflight was not successful", providerVersion);
        }
        String normalizedHelp = help.output().toLowerCase(Locale.ROOT);
        List<String> missing = REQUIRED_EXEC_OPTIONS.stream().filter(option -> !normalizedHelp.contains(option))
                .sorted().toList();
        if (!missing.isEmpty()) {
            return new ProviderAvailability(Status.VERSION_UNSUPPORTED,
                    "Codex CLI exec is missing required capabilities: " + String.join(",", missing), providerVersion);
        }
        return new ProviderAvailability(Status.AVAILABLE, "Codex CLI preflight passed", providerVersion);
    }

    @Override
    public AgentExecutor executorFor(AgentExecutor.Role role, String logicalProfileId) {
        ProviderAvailability result = preflight(role, logicalProfileId);
        if (!result.available()) throw new ProviderUnavailableException(result);
        var profile = CodexCliProfiles.readOnly(executable.orElseThrow(), logicalProfileId, role,
                executionTimeout, CodexCliProfiles.defaultEnvironmentAllowlist());
        return new ExternalAgentRunner(profile, stagingRoot);
    }

    private static String versionLine(String output) {
        if (output == null) return null;
        String line = output.lines().map(String::trim).filter(value -> !value.isEmpty()).findFirst().orElse(null);
        if (line == null || line.length() > 128 || !line.toLowerCase(Locale.ROOT).contains("codex")
                || !line.matches(".*\\d+\\.\\d+.*")) return null;
        return line;
    }

    private static Path requireAbsolute(Path value, String name) {
        if (value == null || !value.isAbsolute()) throw new IllegalArgumentException(name + " must be absolute");
        return value;
    }

    private static Duration boundedDuration(Duration value, String name, Duration minimum, Duration maximum) {
        if (value == null || value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(name + " is outside the supported range");
        }
        return value;
    }

    interface CommandProbe {
        ProbeResult run(List<String> command, Duration timeout, int maxOutputBytes);
    }

    record ProbeResult(boolean started, int exitCode, boolean timedOut, boolean overflow, String output) {
        ProbeResult {
            output = output == null ? "" : output;
        }

        static ProbeResult unavailable() { return new ProbeResult(false, -1, false, false, ""); }
    }

    private static final class ProcessCommandProbe implements CommandProbe {
        @Override
        public ProbeResult run(List<String> command, Duration timeout, int maxOutputBytes) {
            Process process;
            try {
                ProcessBuilder builder = new ProcessBuilder(List.copyOf(command)).redirectErrorStream(true);
                Map<String, String> environment = builder.environment();
                environment.clear();
                copyPlatformVariable(environment, "SYSTEMROOT");
                copyPlatformVariable(environment, "WINDIR");
                process = builder.start();
            } catch (IOException failure) {
                return ProbeResult.unavailable();
            }
            ExecutorService reader = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "test-lens-codex-preflight");
                thread.setDaemon(true);
                return thread;
            });
            Future<BoundedOutput> output = reader.submit(() -> readBounded(process.getInputStream(), maxOutputBytes));
            try {
                boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
                if (!finished) terminate(process);
                BoundedOutput captured = output.get(2, TimeUnit.SECONDS);
                return new ProbeResult(true, finished ? process.exitValue() : -1, !finished, captured.overflow(),
                        new String(captured.bytes(), StandardCharsets.UTF_8));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                terminate(process);
                return new ProbeResult(true, -1, true, false, "");
            } catch (ExecutionException | TimeoutException failure) {
                terminate(process);
                return new ProbeResult(true, -1, false, true, "");
            } finally {
                reader.shutdownNow();
            }
        }

        private static void copyPlatformVariable(Map<String, String> target, String name) {
            String value = System.getenv(name);
            if (value != null) target.put(name, value);
        }

        private static BoundedOutput readBounded(InputStream input, int limit) throws IOException {
            try (input; ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(limit, 8_192))) {
                byte[] buffer = new byte[4_096];
                boolean overflow = false;
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    int remaining = limit - output.size();
                    if (remaining > 0) output.write(buffer, 0, Math.min(remaining, read));
                    if (read > remaining) overflow = true;
                }
                return new BoundedOutput(output.toByteArray(), overflow);
            }
        }

        private static void terminate(Process process) {
            process.destroy();
            try {
                if (!process.waitFor(500, TimeUnit.MILLISECONDS)) process.destroyForcibly();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }

    private record BoundedOutput(byte[] bytes, boolean overflow) { }
}
