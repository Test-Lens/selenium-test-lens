package io.github.testlens;

import io.github.testlens.hud.SourceIde;
import io.github.testlens.hud.SourceNavigationOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SourceNavigationCompatibilityProbeTest {
    @TempDir Path temp;

    @Test void idea2026WithHandlerIsReady() throws Exception {
        assertState(environment("2026.1", ProbeResult.PRESENT, ProbeResult.PRESENT),
                SourceNavigationCompatibility.State.READY, true);
    }

    @Test void idea2026WithoutHandlerRequiresAction() throws Exception {
        assertState(environment("2026.1", ProbeResult.ABSENT, ProbeResult.PRESENT),
                SourceNavigationCompatibility.State.PROTOCOL_HANDLER_MISSING, false);
    }

    @Test void olderIdeaWithDaemonAndHandlerIsReady() throws Exception {
        assertState(environment("2025.2.5", ProbeResult.PRESENT, ProbeResult.PRESENT),
                SourceNavigationCompatibility.State.READY, true);
    }

    @Test void olderIdeaWithoutDaemonOrHandlerRequiresToolboxOrNewIde() throws Exception {
        SourceNavigationCompatibility result = assess(environment("2025.2.5", ProbeResult.ABSENT, ProbeResult.ABSENT));
        assertEquals(SourceNavigationCompatibility.State.TOOLBOX_OR_NEW_IDE_REQUIRED, result.state());
        assertFalse(result.navigationAllowed());
        assertTrue(result.recommendedAction().contains("Toolbox 3.3+"));
        assertTrue(result.recommendedAction().contains("2026.1+"));
    }

    @Test void unknownIdeaWithHandlerIsReady() throws Exception {
        assertState(environment(null, ProbeResult.PRESENT, ProbeResult.UNKNOWN),
                SourceNavigationCompatibility.State.READY, true);
    }

    @Test void unavailableDetectionIsUnknownAndAllowsAttempt() throws Exception {
        assertState(JetBrainsEnvironment.unknown("blocked"),
                SourceNavigationCompatibility.State.UNKNOWN, true);
    }

    @Test void missingAndInvalidProjectMappingsAreDistinct() throws Exception {
        SourceNavigationOptions missing = SourceNavigationOptions.builder().enabled(true).ide(SourceIde.INTELLIJ).build();
        SourceNavigationCompatibilityProbe probe = probe(environment("2026.1", ProbeResult.PRESENT, ProbeResult.PRESENT));
        assertEquals(SourceNavigationCompatibility.State.PROJECT_MAPPING_MISSING,
                probe.assess(missing, temp).state());

        SourceNavigationOptions invalid = SourceNavigationOptions.builder().enabled(true).ide(SourceIde.INTELLIJ)
                .intellijProject("Missing", temp.resolve("does-not-exist")).build();
        assertEquals(SourceNavigationCompatibility.State.PROJECT_MAPPING_INVALID,
                probe.assess(invalid, temp).state());
    }

    @Test void invalidSourceRootAndUnresolvedSourceAreActionable() throws Exception {
        SourceNavigationOptions invalidRoot = SourceNavigationOptions.builder().enabled(true).ide(SourceIde.INTELLIJ)
                .intellijProject("Test Project", temp).sourceRoots(temp.resolve("missing-source")).build();
        SourceNavigationCompatibility base = probe(environment("2026.1", ProbeResult.PRESENT, ProbeResult.PRESENT))
                .assess(invalidRoot, temp);
        assertEquals(SourceNavigationCompatibility.State.SOURCE_ROOT_INVALID, base.state());

        SourceNavigationCompatibility ready = assess(environment("2026.1", ProbeResult.PRESENT, ProbeResult.PRESENT));
        SourceNavigationCompatibility unresolved = ready.sourceUnresolved("Missing.java:9");
        assertEquals(SourceNavigationCompatibility.State.SOURCE_FILE_UNRESOLVED, unresolved.state());
        assertFalse(unresolved.navigationAllowed());
    }

    @Test void semanticVersionComparisonIsNumeric() {
        assertTrue(SemanticVersion.parse("2026.10").compareTo(SemanticVersion.parse("2026.2")) > 0);
        assertTrue(SemanticVersion.parse("2026.1").compareTo(SemanticVersion.parse("2025.10.9")) > 0);
        assertFalse(SemanticVersion.parse("unknown").known());
    }

    @Test void windowsProbeUsesFixedRegistryCommandsAndProductMetadata() {
        Path executable = Path.of("D:/JetBrains/IntelliJ IDEA 2025.2.5/bin/idea64.exe");
        FakeSystem system = new FakeSystem(executable);
        JetBrainsEnvironment result = new WindowsJetBrainsEnvironmentProbe(system).probe();

        assertEquals("2025.2.5", result.ide().orElseThrow().version().display());
        assertEquals("252.28238.7", result.ide().orElseThrow().build());
        assertEquals(ProbeResult.ABSENT, result.protocolHandler());
        assertEquals(ProbeResult.ABSENT, result.jetbrainsDaemon());
        assertEquals(List.of(
                List.of("reg.exe", "query", "HKCU\\Software\\Classes\\jetbrains", "/v", "URL Protocol"),
                List.of("reg.exe", "query", "HKCR\\jetbrains", "/v", "URL Protocol")), system.commands);
    }

    @Test void unavailableWindowsCapabilitiesRemainUnknown() {
        SystemAccess unavailable = new SystemAccess() {
            @Override public String osName() { return "Windows 11"; }
            @Override public String property(String name) { return null; }
            @Override public Map<String, String> environment() { return Map.of("LOCALAPPDATA", "D:/Local"); }
            @Override public ProcessSnapshot processes() { return new ProcessSnapshot(List.of(), false); }
            @Override public Optional<String> readString(Path path) { return Optional.empty(); }
            @Override public ProbeResult directoryStatus(Path path) { return ProbeResult.UNKNOWN; }
            @Override public CommandResult run(List<String> command, Duration timeout) {
                return new CommandResult(false, -1);
            }
        };
        JetBrainsEnvironment result = new WindowsJetBrainsEnvironmentProbe(unavailable).probe();
        assertEquals(ProbeResult.UNKNOWN, result.protocolHandler());
        assertEquals(ProbeResult.UNKNOWN, result.jetbrainsDaemon());
        assertEquals(ProbeResult.UNKNOWN, result.toolbox());
    }

    private void assertState(JetBrainsEnvironment environment, SourceNavigationCompatibility.State state,
                             boolean allowed) throws Exception {
        SourceNavigationCompatibility result = assess(environment);
        assertEquals(state, result.state());
        assertEquals(allowed, result.navigationAllowed());
    }

    private SourceNavigationCompatibility assess(JetBrainsEnvironment environment) throws Exception {
        return probe(environment).assess(options(), temp);
    }

    private SourceNavigationCompatibilityProbe probe(JetBrainsEnvironment environment) {
        return new SourceNavigationCompatibilityProbe(() -> environment);
    }

    private SourceNavigationOptions options() throws Exception {
        Files.createDirectories(temp);
        return SourceNavigationOptions.builder().enabled(true).ide(SourceIde.INTELLIJ)
                .intellijProject("Test Project", temp).build();
    }

    private JetBrainsEnvironment environment(String version, ProbeResult protocol, ProbeResult daemon) {
        Optional<JetBrainsIdeInstallation> ide = version == null ? Optional.empty()
                : Optional.of(new JetBrainsIdeInstallation("IntelliJ IDEA", SemanticVersion.parse(version),
                        "build", Path.of("D:/JetBrains/idea64.exe")));
        return new JetBrainsEnvironment(ide, protocol, daemon, ProbeResult.ABSENT, "test");
    }

    private final class FakeSystem implements SystemAccess {
        private final Path executable;
        private final List<List<String>> commands = new java.util.ArrayList<>();
        private FakeSystem(Path executable) { this.executable = executable; }
        @Override public String osName() { return "Windows 11"; }
        @Override public String property(String name) { return null; }
        @Override public Map<String, String> environment() { return Map.of("LOCALAPPDATA", "D:/Local"); }
        @Override public ProcessSnapshot processes() {
            return new ProcessSnapshot(List.of(
                    new SystemProcess("idea64.exe", Optional.of(executable.toString()))), true);
        }
        @Override public Optional<String> readString(Path path) {
            return Optional.of("{\"name\":\"IntelliJ IDEA\",\"version\":\"2025.2.5\","
                    + "\"buildNumber\":\"252.28238.7\"}");
        }
        @Override public ProbeResult directoryStatus(Path path) { return ProbeResult.ABSENT; }
        @Override public CommandResult run(List<String> command, Duration timeout) {
            commands.add(List.copyOf(command));
            return new CommandResult(true, 1);
        }
    }
}
