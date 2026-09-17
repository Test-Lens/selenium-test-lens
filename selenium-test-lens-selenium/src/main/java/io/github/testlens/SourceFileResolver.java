package io.github.testlens;

import io.github.testlens.core.logging.SourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Lazy, cached source-file resolver for standard Maven/Gradle layouts and custom roots. */
final class SourceFileResolver {
    private static final java.util.concurrent.atomic.AtomicInteger CREATED = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger RESOLUTIONS = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger DISCOVERIES = new java.util.concurrent.atomic.AtomicInteger();
    private static final Set<String> STANDARD_SUFFIXES = Set.of(
            "src/test/java", "src/main/java", "src/test/kotlin", "src/main/kotlin");
    private final Path projectRoot;
    private final List<Path> customRoots;
    private final Map<String, Optional<Path>> cache = new ConcurrentHashMap<>();
    private volatile List<Path> roots;

    SourceFileResolver(Path projectRoot, List<Path> customRoots) {
        CREATED.incrementAndGet();
        this.projectRoot = (projectRoot == null ? Path.of("") : projectRoot).toAbsolutePath().normalize();
        this.customRoots = customRoots == null ? List.of() : customRoots.stream()
                .map(path -> path.isAbsolute() ? path : this.projectRoot.resolve(path))
                .map(path -> path.toAbsolutePath().normalize()).toList();
    }

    Optional<Path> resolve(SourceLocation location) {
        RESOLUTIONS.incrementAndGet();
        if (location == null || location.fileName().isBlank()) return Optional.empty();
        String key = location.className() + "|" + location.fileName();
        return cache.computeIfAbsent(key, ignored -> resolveUncached(location));
    }

    int cacheSize() { return cache.size(); }

    private Optional<Path> resolveUncached(SourceLocation location) {
        String topLevelClass = location.className();
        int nested = topLevelClass.indexOf('$');
        if (nested >= 0) topLevelClass = topLevelClass.substring(0, nested);
        int lastDot = topLevelClass.lastIndexOf('.');
        String packagePath = lastDot < 0 ? "" : topLevelClass.substring(0, lastDot).replace('.', '/');
        for (Path root : roots()) {
            Path candidate = packagePath.isEmpty()
                    ? root.resolve(location.fileName()) : root.resolve(packagePath).resolve(location.fileName());
            if (Files.isRegularFile(candidate)) return Optional.of(candidate.toAbsolutePath().normalize());
        }
        return Optional.empty();
    }

    private List<Path> roots() {
        List<Path> current = roots;
        if (current != null) return current;
        synchronized (this) {
            if (roots != null) return roots;
            DISCOVERIES.incrementAndGet();
            LinkedHashSet<Path> detected = new LinkedHashSet<>(customRoots);
            STANDARD_SUFFIXES.forEach(suffix -> {
                Path direct = projectRoot.resolve(suffix);
                if (Files.isDirectory(direct)) detected.add(direct);
            });
            try (var paths = Files.walk(projectRoot, 6)) {
                paths.filter(Files::isDirectory).forEach(path -> {
                    String normalized = projectRoot.relativize(path).toString().replace('\\', '/');
                    for (String suffix : STANDARD_SUFFIXES) {
                        if (normalized.equals(suffix) || normalized.endsWith('/' + suffix)) detected.add(path);
                    }
                });
            } catch (IOException | RuntimeException ignored) {
                // Source navigation is best-effort and must never affect a test.
            }
            roots = List.copyOf(new ArrayList<>(detected));
            return roots;
        }
    }

    static void resetMetrics() { CREATED.set(0); RESOLUTIONS.set(0); DISCOVERIES.set(0); }
    static int createdCount() { return CREATED.get(); }
    static int resolutionCount() { return RESOLUTIONS.get(); }
    static int discoveryCount() { return DISCOVERIES.get(); }
}
