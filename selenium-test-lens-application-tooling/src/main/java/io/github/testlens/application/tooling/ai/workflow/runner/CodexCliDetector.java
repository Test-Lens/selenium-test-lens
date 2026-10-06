package io.github.testlens.application.tooling.ai.workflow.runner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Locates a directly executable Codex binary without invoking a shell or accepting command text. @since 0.5.0 */
public final class CodexCliDetector {
    private CodexCliDetector() { }

    public static Optional<Path> detect() { return detect(System.getenv(), System.getProperty("os.name", ""),
            System.getProperty("os.arch", "")); }

    static Optional<Path> detect(Map<String, String> environment, String osName, String architecture) {
        boolean windows = osName.toLowerCase(Locale.ROOT).contains("win");
        String executable = windows ? "codex.exe" : "codex";
        List<Path> candidates = new ArrayList<>();
        String path = environment.get("PATH");
        if (path != null) {
            for (String entry : path.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))) {
                if (!entry.isBlank()) candidates.add(Path.of(entry).resolve(executable));
            }
        }
        if (windows) {
            String appData = environment.get("APPDATA");
            if (appData != null && !appData.isBlank()) {
                String platform = architecture.toLowerCase(Locale.ROOT).contains("arm")
                        ? "codex-win32-arm64/vendor/aarch64-pc-windows-msvc/bin/codex.exe"
                        : "codex-win32-x64/vendor/x86_64-pc-windows-msvc/bin/codex.exe";
                candidates.add(Path.of(appData, "npm", "node_modules", "@openai", "codex", "node_modules",
                        "@openai", platform));
            }
        }
        return candidates.stream().map(Path::toAbsolutePath).map(Path::normalize)
                .filter(candidate -> Files.isRegularFile(candidate) && Files.isExecutable(candidate))
                .findFirst();
    }
}
