package io.github.testlens;

import io.github.testlens.hud.SourceNavigationOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/** Resolves the local IntelliJ project identity without guessing from a worktree directory name. */
record IntellijProjectContext(String name, Path root) {
    static Optional<IntellijProjectContext> resolve(SourceNavigationOptions options, Path executionRoot) {
        if (options == null) return Optional.empty();
        Path base = (executionRoot == null ? Path.of("") : executionRoot).toAbsolutePath().normalize();
        if (!options.intellijProjectName().isBlank() && options.intellijProjectRoot() != null) {
            Path configuredRoot = options.intellijProjectRoot();
            Path root = (configuredRoot.isAbsolute() ? configuredRoot : base.resolve(configuredRoot)).normalize();
            return Files.isDirectory(root)
                    ? Optional.of(new IntellijProjectContext(options.intellijProjectName(), root))
                    : Optional.empty();
        }

        Set<Path> candidates = new LinkedHashSet<>();
        candidates.add(base);
        options.sourceRoots().stream().filter(path -> path != null)
                .map(path -> (path.isAbsolute() ? path : base.resolve(path)).normalize()).forEach(candidates::add);
        for (Path candidate : candidates) {
            Optional<IntellijProjectContext> inferred = inferFromIdeaName(candidate);
            if (inferred.isPresent()) return inferred;
        }
        return Optional.empty();
    }

    private static Optional<IntellijProjectContext> inferFromIdeaName(Path start) {
        Path current = Files.isDirectory(start) ? start : start.getParent();
        while (current != null) {
            Path nameFile = current.resolve(".idea").resolve(".name");
            if (Files.isRegularFile(nameFile)) {
                try {
                    String name = Files.readString(nameFile).trim();
                    if (!name.isBlank()) return Optional.of(new IntellijProjectContext(name, current));
                } catch (IOException | RuntimeException ignored) {
                    return Optional.empty();
                }
            }
            current = current.getParent();
        }
        return Optional.empty();
    }
}
