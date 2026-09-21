package io.github.testlens;

import io.github.testlens.hud.SourceIde;
import io.github.testlens.hud.SourceNavigationOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Evaluates project configuration separately from the injectable local-environment probe. */
final class SourceNavigationCompatibilityProbe {
    // JetBrains IDEA 2026.1 bundles jetbrainsd; Toolbox 3.3 introduced and auto-starts it.
    static final SemanticVersion MIN_IDEA_WITH_BUNDLED_JETBRAINSD = SemanticVersion.parse("2026.1");
    static final SemanticVersion MIN_TOOLBOX_WITH_JETBRAINSD = SemanticVersion.parse("3.3");

    private final JetBrainsEnvironmentProbe environmentProbe;

    SourceNavigationCompatibilityProbe(JetBrainsEnvironmentProbe environmentProbe) {
        this.environmentProbe = environmentProbe;
    }

    static SourceNavigationCompatibilityProbe system() {
        return new SourceNavigationCompatibilityProbe(JetBrainsEnvironmentProbe.system());
    }

    SourceNavigationCompatibility assess(SourceNavigationOptions options, Path executionRoot) {
        if (options == null || options.ide() != SourceIde.INTELLIJ) {
            return ready(JetBrainsEnvironment.unknown("JetBrains compatibility is not required"), "not required");
        }
        JetBrainsEnvironment environment = environmentProbe.probe();
        Path base = (executionRoot == null ? Path.of("") : executionRoot).toAbsolutePath().normalize();
        Optional<IntellijProjectContext> project = IntellijProjectContext.resolve(options, base);
        if (!options.intellijProjectName().isBlank() && options.intellijProjectRoot() != null) {
            Path configured = options.intellijProjectRoot();
            Path root = (configured.isAbsolute() ? configured : base.resolve(configured)).normalize();
            if (!Files.isDirectory(root)) return action(SourceNavigationCompatibility.State.PROJECT_MAPPING_INVALID,
                    environment, "invalid", "The configured IntelliJ project root does not exist.",
                    "Correct intellijProject(name, root) and retry the compatibility check.");
        } else if (project.isEmpty()) {
            return action(SourceNavigationCompatibility.State.PROJECT_MAPPING_MISSING, environment, "missing",
                    "No IntelliJ project mapping could be verified.",
                    "Configure intellijProject(name, root), or add a valid .idea/.name file.");
        }
        Path resolutionRoot = project.map(IntellijProjectContext::root).orElse(base);
        boolean invalidRoot = options.sourceRoots().stream().filter(path -> path != null)
                .map(path -> (path.isAbsolute() ? path : resolutionRoot.resolve(path)).normalize())
                .anyMatch(path -> !Files.isDirectory(path));
        if (invalidRoot) return action(SourceNavigationCompatibility.State.SOURCE_ROOT_INVALID, environment,
                "configured", "A configured Source Navigation source root does not exist.",
                "Correct sourceRoots(...) and retry the compatibility check.");

        String projectStatus = "configured";
        if (environment.protocolHandler() == ProbeResult.PRESENT) return ready(environment, projectStatus);
        if (environment.protocolHandler() == ProbeResult.UNKNOWN) {
            return new SourceNavigationCompatibility(SourceNavigationCompatibility.State.UNKNOWN,
                    SourceNavigationCompatibility.Readiness.UNVERIFIED, true,
                    "Source Navigation availability unverified",
                    "Local jetbrains:// protocol registration could not be verified.",
                    "A navigation attempt is allowed; verify Toolbox/IDE protocol registration if nothing opens.",
                    environment, projectStatus);
        }

        Optional<JetBrainsIdeInstallation> ide = environment.ide();
        if (ide.isPresent() && ide.get().version().known()
                && ide.get().version().compareTo(MIN_IDEA_WITH_BUNDLED_JETBRAINSD) < 0
                && environment.jetbrainsDaemon() == ProbeResult.ABSENT) {
            return action(SourceNavigationCompatibility.State.TOOLBOX_OR_NEW_IDE_REQUIRED, environment,
                    projectStatus, "This IntelliJ version does not bundle jetbrainsd, and the jetbrains:// "
                            + "handler was not detected.",
                    toolboxOrNewIdeAction());
        }
        if (ide.isPresent() && ide.get().version().compareTo(MIN_IDEA_WITH_BUNDLED_JETBRAINSD) >= 0
                && environment.jetbrainsDaemon() == ProbeResult.ABSENT) {
            return action(SourceNavigationCompatibility.State.JETBRAINS_DAEMON_MISSING, environment,
                    projectStatus, "IntelliJ supports Source Navigation, but jetbrainsd was not detected.",
                    "Start or restart IntelliJ IDEA once, then retry the compatibility check.");
        }
        return action(SourceNavigationCompatibility.State.PROTOCOL_HANDLER_MISSING, environment, projectStatus,
                "IntelliJ support was detected, but the jetbrains:// handler is not registered.",
                environment.jetbrainsDaemon() == ProbeResult.PRESENT
                        ? "Start or restart Toolbox/IntelliJ, then retry the compatibility check."
                        : toolboxOrNewIdeAction());
    }

    private static SourceNavigationCompatibility ready(JetBrainsEnvironment environment, String projectStatus) {
        return new SourceNavigationCompatibility(SourceNavigationCompatibility.State.READY,
                SourceNavigationCompatibility.Readiness.VERIFIED, true,
                "Source Navigation ready", "The local protocol and project mapping are available.", "",
                environment, projectStatus);
    }

    private static SourceNavigationCompatibility action(SourceNavigationCompatibility.State state,
                                                          JetBrainsEnvironment environment, String projectStatus,
                                                          String detail, String action) {
        return new SourceNavigationCompatibility(state, SourceNavigationCompatibility.Readiness.ACTION_REQUIRED,
                false, "Source Navigation unavailable", detail, action, environment, projectStatus);
    }

    private static String toolboxOrNewIdeAction() {
        return "Install/run JetBrains Toolbox " + MIN_TOOLBOX_WITH_JETBRAINSD.display()
                + "+ or update IntelliJ IDEA to " + MIN_IDEA_WITH_BUNDLED_JETBRAINSD.display() + "+.";
    }
}
