package io.github.testlens;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Internal, injectable environment probe. URI generation intentionally lives elsewhere. */
interface JetBrainsEnvironmentProbe {
    JetBrainsEnvironment probe();

    static JetBrainsEnvironmentProbe system() {
        SystemAccess access = new RealSystemAccess();
        return access.osName().toLowerCase(Locale.ROOT).contains("windows")
                ? new WindowsJetBrainsEnvironmentProbe(access)
                : () -> JetBrainsEnvironment.unknown("Automatic JetBrains capability probing is unavailable on this OS");
    }
}

final class WindowsJetBrainsEnvironmentProbe implements JetBrainsEnvironmentProbe {
    private static final String EXPLICIT_HOME = "testlens.intellij.home";
    private static final String EXPLICIT_EXECUTABLE = "testlens.intellij.executable";
    private final SystemAccess system;

    WindowsJetBrainsEnvironmentProbe(SystemAccess system) { this.system = system; }

    @Override public JetBrainsEnvironment probe() {
        try {
            Optional<Path> executable = explicitExecutable().or(this::runningIdeaExecutable);
            Optional<JetBrainsIdeInstallation> ide = executable.flatMap(this::installation);
            ProbeResult daemon = daemonStatus();
            ProbeResult toolbox = processNamed("jetbrains-toolbox", "toolbox");
            ProbeResult protocol = protocolStatus();
            return new JetBrainsEnvironment(ide, protocol, daemon, toolbox, "Windows local capability probe");
        } catch (RuntimeException ignored) {
            return JetBrainsEnvironment.unknown("Windows capability probe could not complete safely");
        }
    }

    private Optional<Path> explicitExecutable() {
        String executable = system.property(EXPLICIT_EXECUTABLE);
        if (executable != null && !executable.isBlank()) return Optional.of(Path.of(executable));
        String home = system.property(EXPLICIT_HOME);
        if (home != null && !home.isBlank()) return Optional.of(Path.of(home).resolve("bin").resolve("idea64.exe"));
        return Optional.empty();
    }

    private Optional<Path> runningIdeaExecutable() {
        return system.processes().values().stream()
                .filter(process -> process.name().equalsIgnoreCase("idea64") || process.name().equalsIgnoreCase("idea64.exe"))
                .map(SystemProcess::command).flatMap(Optional::stream).map(Path::of).findFirst();
    }

    private Optional<JetBrainsIdeInstallation> installation(Path executable) {
        Path normalized = executable.toAbsolutePath().normalize();
        Path home = normalized.getParent() == null ? null : normalized.getParent().getParent();
        if (home == null) return Optional.empty();
        Path productInfo = home.resolve("product-info.json");
        Optional<String> json = system.readString(productInfo);
        if (json.isEmpty()) return Optional.empty();
        String name = jsonValue(json.get(), "name").orElse("IntelliJ IDEA");
        SemanticVersion version = SemanticVersion.parse(jsonValue(json.get(), "version").orElse(null));
        String build = jsonValue(json.get(), "buildNumber").orElse("unknown");
        return Optional.of(new JetBrainsIdeInstallation(name, version, build, normalized));
    }

    private ProbeResult daemonStatus() {
        ProbeResult running = processNamed("jetbrainsd");
        if (running == ProbeResult.PRESENT) return ProbeResult.PRESENT;
        String localAppData = system.environment().get("LOCALAPPDATA");
        if (localAppData == null || localAppData.isBlank()) return ProbeResult.UNKNOWN;
        Path current = Path.of(localAppData, "JetBrains", "Daemon", "bundles", "current");
        ProbeResult installed = system.directoryStatus(current);
        if (installed == ProbeResult.PRESENT) return ProbeResult.PRESENT;
        return running == ProbeResult.UNKNOWN || installed == ProbeResult.UNKNOWN
                ? ProbeResult.UNKNOWN : ProbeResult.ABSENT;
    }

    private ProbeResult protocolStatus() {
        CommandResult user = system.run(List.of("reg.exe", "query", "HKCU\\Software\\Classes\\jetbrains", "/v", "URL Protocol"),
                Duration.ofSeconds(2));
        if (user.success()) return ProbeResult.PRESENT;
        CommandResult classes = system.run(List.of("reg.exe", "query", "HKCR\\jetbrains", "/v", "URL Protocol"),
                Duration.ofSeconds(2));
        if (classes.success()) return ProbeResult.PRESENT;
        return user.executed() && classes.executed() ? ProbeResult.ABSENT : ProbeResult.UNKNOWN;
    }

    private ProbeResult processNamed(String... names) {
        ProcessSnapshot snapshot = system.processes();
        for (SystemProcess process : snapshot.values()) {
            String normalized = process.name().toLowerCase(Locale.ROOT).replace(".exe", "");
            for (String name : names) if (normalized.equals(name)) return ProbeResult.PRESENT;
        }
        return snapshot.available() ? ProbeResult.ABSENT : ProbeResult.UNKNOWN;
    }

    private static Optional<String> jsonValue(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                .matcher(json);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}

interface SystemAccess {
    String osName();
    String property(String name);
    Map<String, String> environment();
    ProcessSnapshot processes();
    Optional<String> readString(Path path);
    ProbeResult directoryStatus(Path path);
    CommandResult run(List<String> command, Duration timeout);
}

record SystemProcess(String name, Optional<String> command) {}
record ProcessSnapshot(List<SystemProcess> values, boolean available) {}
record CommandResult(boolean executed, int exitCode) { boolean success() { return executed && exitCode == 0; } }

final class RealSystemAccess implements SystemAccess {
    @Override public String osName() { return System.getProperty("os.name", ""); }
    @Override public String property(String name) { return System.getProperty(name); }
    @Override public Map<String, String> environment() { return System.getenv(); }
    @Override public ProcessSnapshot processes() {
        List<SystemProcess> result = new ArrayList<>();
        try {
            ProcessHandle.allProcesses().forEach(process -> {
                ProcessHandle.Info info = process.info();
                String command = info.command().orElse("");
                String name = command.isBlank() ? "" : Path.of(command).getFileName().toString();
                if (!name.isBlank()) result.add(new SystemProcess(name, info.command()));
            });
            return new ProcessSnapshot(List.copyOf(result), true);
        } catch (RuntimeException ignored) {
            return new ProcessSnapshot(List.of(), false);
        }
    }
    @Override public Optional<String> readString(Path path) {
        try { return Files.isRegularFile(path) ? Optional.of(Files.readString(path)) : Optional.empty(); }
        catch (IOException | RuntimeException ignored) { return Optional.empty(); }
    }
    @Override public ProbeResult directoryStatus(Path path) {
        if (!Files.exists(path)) return ProbeResult.ABSENT;
        try (var entries = Files.list(path)) {
            return entries.findAny().isPresent() ? ProbeResult.PRESENT : ProbeResult.ABSENT;
        } catch (IOException | RuntimeException ignored) { return ProbeResult.UNKNOWN; }
    }
    @Override public CommandResult run(List<String> command, Duration timeout) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) process.destroyForcibly();
            return new CommandResult(true, finished ? process.exitValue() : -1);
        } catch (IOException | InterruptedException | RuntimeException ignored) {
            if (ignored instanceof InterruptedException) Thread.currentThread().interrupt();
            return new CommandResult(false, -1);
        }
    }
}
