package io.github.testlens.studio.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;

final class MavenProjectContextFactory {

    private MavenProjectContextFactory() {
    }

    static StudioProjectContext from(MavenProject project) throws DependencyResolutionRequiredException {
        Objects.requireNonNull(project, "project");
        File basedir = project.getBasedir();
        if (basedir == null) {
            throw new IllegalArgumentException("Maven project basedir is unavailable");
        }
        Path projectRoot = basedir.toPath().toAbsolutePath().normalize();
        return new StudioProjectContext(
                projectRoot,
                paths(project.getCompileSourceRoots(), projectRoot),
                paths(project.getTestCompileSourceRoots(), projectRoot),
                paths(project.getTestClasspathElements(), projectRoot));
    }

    private static List<Path> paths(List<String> values, Path projectRoot) {
        return values.stream()
                .map(Path::of)
                .map(path -> path.isAbsolute() ? path : projectRoot.resolve(path))
                .toList();
    }
}
