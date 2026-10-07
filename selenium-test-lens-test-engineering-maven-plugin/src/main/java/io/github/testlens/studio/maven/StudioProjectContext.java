package io.github.testlens.studio.maven;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

record StudioProjectContext(
        Path projectRoot,
        List<Path> mainSourceRoots,
        List<Path> testSourceRoots,
        List<Path> testClasspath) {

    StudioProjectContext {
        projectRoot = Objects.requireNonNull(projectRoot, "projectRoot").toAbsolutePath().normalize();
        mainSourceRoots = immutablePaths(mainSourceRoots, "mainSourceRoots");
        testSourceRoots = immutablePaths(testSourceRoots, "testSourceRoots");
        testClasspath = immutablePaths(testClasspath, "testClasspath");
    }

    private static List<Path> immutablePaths(List<Path> paths, String name) {
        Objects.requireNonNull(paths, name);
        return paths.stream()
                .map(path -> Objects.requireNonNull(path, name + " entry").toAbsolutePath().normalize())
                .distinct()
                .toList();
    }
}
