package io.github.testlens.application.tooling.ai.workflow.runner;

import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Trusted host configuration for one bounded external agent process. @since 0.5.0 */
public record AgentProfile(String id, Path executable, List<String> arguments, AgentExecutor.Role role,
                           Duration timeout, int maxInputBytes, int maxOutputBytes,
                           int maxDiagnosticsBytes, Set<String> environmentAllowlist,
                           OutputTransport outputTransport) {
    public static final String SCHEMA_FILE = "{{TEST_LENS_SCHEMA_FILE}}";
    public static final String OUTPUT_FILE = "{{TEST_LENS_OUTPUT_FILE}}";
    public static final String WORKING_DIRECTORY = "{{TEST_LENS_WORKING_DIRECTORY}}";

    public AgentProfile {
        if (id == null || !id.matches("[A-Za-z0-9._-]{1,96}")) {
            throw new IllegalArgumentException("profile id must be a safe identifier");
        }
        if (executable == null || !executable.isAbsolute()) {
            throw new IllegalArgumentException("agent executable must be an absolute path");
        }
        executable = executable.normalize();
        arguments = List.copyOf(arguments == null ? List.of() : arguments);
        if (arguments.size() > 128 || arguments.stream().anyMatch(AgentProfile::invalidArgument)) {
            throw new IllegalArgumentException("agent arguments must be bounded non-null values");
        }
        if (role == null || timeout == null || timeout.compareTo(Duration.ofSeconds(1)) < 0
                || timeout.compareTo(Duration.ofMinutes(30)) > 0) {
            throw new IllegalArgumentException("role and timeout in [1 second, 30 minutes] are required");
        }
        bounded("maxInputBytes", maxInputBytes, 1_024, 16 * 1024 * 1024);
        bounded("maxOutputBytes", maxOutputBytes, 1_024, 16 * 1024 * 1024);
        bounded("maxDiagnosticsBytes", maxDiagnosticsBytes, 256, 1024 * 1024);
        environmentAllowlist = Set.copyOf(environmentAllowlist == null ? Set.of() : environmentAllowlist);
        if (environmentAllowlist.stream().anyMatch(AgentProfile::secretLikeEnvironmentName)) {
            throw new IllegalArgumentException("secret-like environment variables cannot be allowlisted");
        }
        if (outputTransport == null) throw new IllegalArgumentException("outputTransport is required");
        if (outputTransport == OutputTransport.FILE && arguments.stream().noneMatch(OUTPUT_FILE::equals)) {
            throw new IllegalArgumentException("file output transport requires the output-file placeholder");
        }
    }

    private static boolean invalidArgument(String value) {
        return value == null || value.length() > 16_384 || value.indexOf('\0') >= 0;
    }

    private static boolean secretLikeEnvironmentName(String value) {
        if (value == null || !value.matches("[A-Za-z_][A-Za-z0-9_]{0,127}")) return true;
        String normalized = value.toUpperCase(Locale.ROOT);
        return normalized.contains("TOKEN") || normalized.contains("SECRET")
                || normalized.contains("PASSWORD") || normalized.contains("COOKIE")
                || normalized.contains("AUTHORIZATION") || normalized.contains("API_KEY")
                || normalized.contains("APIKEY") || normalized.contains("CREDENTIAL")
                || normalized.contains("SESSION") || normalized.contains("JWT")
                || normalized.contains("CSRF");
    }

    private static void bounded(String name, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be in [" + minimum + "," + maximum + "]");
        }
    }

    public enum OutputTransport { STDOUT, FILE }
}
