package io.github.testlens;

import java.nio.file.Path;

/** Internal, presentation-ready result of a local Source Navigation preflight. */
record SourceNavigationCompatibility(
        State state,
        Readiness readiness,
        boolean navigationAllowed,
        String summary,
        String detail,
        String recommendedAction,
        JetBrainsEnvironment environment,
        String projectStatus) {

    enum State {
        READY,
        PROTOCOL_HANDLER_MISSING,
        TOOLBOX_OR_NEW_IDE_REQUIRED,
        PROJECT_MAPPING_MISSING,
        PROJECT_MAPPING_INVALID,
        SOURCE_ROOT_INVALID,
        SOURCE_FILE_UNRESOLVED,
        JETBRAINS_DAEMON_MISSING,
        UNKNOWN
    }

    enum Readiness { VERIFIED, ACTION_REQUIRED, UNVERIFIED }

    String fingerprint() {
        return state + "|" + readiness + "|" + environment.protocolHandler() + "|"
                + environment.jetbrainsDaemon() + "|" + projectStatus;
    }

    String hudEventType() {
        return readiness == Readiness.VERIFIED
                ? "SOURCE_NAVIGATION_READY"
                : readiness == Readiness.UNVERIFIED
                        ? "SOURCE_NAVIGATION_UNVERIFIED"
                        : "SOURCE_NAVIGATION_ACTION_REQUIRED";
    }

    String javaSummary() {
        StringBuilder value = new StringBuilder(summary);
        environment.ide().ifPresent(ide -> value.append(System.lineSeparator())
                .append("IDE: ").append(ide.name()).append(' ').append(ide.version().display()));
        value.append(System.lineSeparator()).append("JetBrains protocol handler: ")
                .append(environment.protocolHandler().display())
                .append(System.lineSeparator()).append("jetbrainsd: ")
                .append(environment.jetbrainsDaemon().display())
                .append(System.lineSeparator()).append("Project mapping: ").append(projectStatus);
        if (!recommendedAction.isBlank()) {
            value.append(System.lineSeparator()).append("Recommended action: ").append(recommendedAction);
        }
        return value.toString();
    }

    SourceNavigationCompatibility sourceUnresolved(String sourceLabel) {
        if (readiness == Readiness.ACTION_REQUIRED) return this;
        return new SourceNavigationCompatibility(State.SOURCE_FILE_UNRESOLVED, Readiness.ACTION_REQUIRED, false,
                "Source Navigation unavailable", "Source file could not be resolved: " + sourceLabel,
                "Check intellijProject(...) and sourceRoots(...), then retry the compatibility check.",
                environment, projectStatus);
    }
}

record JetBrainsEnvironment(
        java.util.Optional<JetBrainsIdeInstallation> ide,
        ProbeResult protocolHandler,
        ProbeResult jetbrainsDaemon,
        ProbeResult toolbox,
        String probeNote) {
    static JetBrainsEnvironment unknown(String note) {
        return new JetBrainsEnvironment(java.util.Optional.empty(), ProbeResult.UNKNOWN,
                ProbeResult.UNKNOWN, ProbeResult.UNKNOWN, note == null ? "" : note);
    }
}

record JetBrainsIdeInstallation(String name, SemanticVersion version, String build, Path executable) {}

enum ProbeResult {
    PRESENT, ABSENT, UNKNOWN;
    String display() { return name().toLowerCase(java.util.Locale.ROOT); }
}

/** Minimal numeric product-version model; qualifiers are diagnostic-only. */
record SemanticVersion(int major, int minor, int patch, String display) implements Comparable<SemanticVersion> {
    static SemanticVersion parse(String value) {
        if (value == null) return new SemanticVersion(0, 0, 0, "unknown");
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?.*$").matcher(value.trim());
        if (!matcher.matches()) return new SemanticVersion(0, 0, 0, "unknown");
        return new SemanticVersion(integer(matcher.group(1)), integer(matcher.group(2)),
                integer(matcher.group(3)), value.trim());
    }

    boolean known() { return major > 0; }

    @Override public int compareTo(SemanticVersion other) {
        int result = Integer.compare(major, other.major);
        if (result == 0) result = Integer.compare(minor, other.minor);
        if (result == 0) result = Integer.compare(patch, other.patch);
        return result;
    }

    private static int integer(String value) { return value == null ? 0 : Integer.parseInt(value); }
}
