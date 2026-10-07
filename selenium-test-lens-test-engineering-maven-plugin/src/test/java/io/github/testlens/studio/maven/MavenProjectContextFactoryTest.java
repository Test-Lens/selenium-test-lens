package io.github.testlens.studio.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MavenProjectContextFactoryTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void capturesNormalizedMavenProjectRootsAndTestClasspathInDeclaredOrder() throws Exception {
        Path projectRoot = temporaryDirectory.resolve("project");
        Path main = projectRoot.resolve("src/main/java");
        Path test = projectRoot.resolve("src/test/java");
        Path classes = projectRoot.resolve("target/test-classes");
        Path dependency = temporaryDirectory.resolve("repository/dependency.jar");
        MavenProject project = project(projectRoot, List.of("src/main/java", "src/main/java"),
                List.of(test.toString()), List.of(classes.toString(), dependency.toString(), classes.toString()));

        StudioProjectContext context = MavenProjectContextFactory.from(project);

        assertEquals(projectRoot.toAbsolutePath().normalize(), context.projectRoot());
        assertEquals(List.of(main.toAbsolutePath().normalize()), context.mainSourceRoots());
        assertEquals(List.of(test.toAbsolutePath().normalize()), context.testSourceRoots());
        assertEquals(List.of(classes.toAbsolutePath().normalize(), dependency.toAbsolutePath().normalize()),
                context.testClasspath());
    }

    @Test
    void rejectsProjectWithoutBasedir() {
        assertThrows(IllegalArgumentException.class, () -> MavenProjectContextFactory.from(new MavenProject()));
    }

    private static MavenProject project(
            Path root,
            List<String> mainRoots,
            List<String> testRoots,
            List<String> testClasspath) {
        return new MavenProject() {
            @Override
            public File getBasedir() {
                return root.toFile();
            }

            @Override
            public List<String> getCompileSourceRoots() {
                return mainRoots;
            }

            @Override
            public List<String> getTestCompileSourceRoots() {
                return testRoots;
            }

            @Override
            public List<String> getTestClasspathElements() throws DependencyResolutionRequiredException {
                return testClasspath;
            }
        };
    }
}
