package io.github.testlens.application.tooling.ai.workflow.runner;

import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/** Safe non-interactive profiles for a locally authenticated Codex CLI. @since 0.5.0 */
public final class CodexCliProfiles {
    private CodexCliProfiles() { }

    public static AgentProfile readOnly(Path nativeExecutable, String id, AgentExecutor.Role role,
                                        Duration timeout, Set<String> environmentAllowlist) {
        return new AgentProfile(id, nativeExecutable, List.of(
                "exec",
                "--ephemeral",
                "--ignore-user-config",
                "--ignore-rules",
                "--sandbox", "read-only",
                "--color", "never",
                "--skip-git-repo-check",
                "-C", AgentProfile.WORKING_DIRECTORY,
                "--output-schema", AgentProfile.SCHEMA_FILE,
                "--output-last-message", AgentProfile.OUTPUT_FILE,
                "-"), role, timeout, 1024 * 1024, 1024 * 1024, 128 * 1024,
                environmentAllowlist, AgentProfile.OutputTransport.FILE);
    }

    /**
     * Minimal host integration needed by the native CLI. These variables are paths/platform settings, not
     * serialized credentials. The configured executable remains a trusted host boundary: ProcessBuilder does not
     * provide portable OS-level read isolation, so profiles must additionally use the provider's sandbox controls.
     */
    public static Set<String> defaultEnvironmentAllowlist() {
        return Set.of("CODEX_HOME", "SYSTEMROOT", "WINDIR", "TEMP", "TMP");
    }
}
