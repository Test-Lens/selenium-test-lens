package io.github.testlens.studio.maven;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StudioMojoTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void launchesAwaitsAndClosesStudioWithProjectContext() throws Exception {
        RecordingLaunch launch = new RecordingLaunch();
        RecordingLauncher launcher = new RecordingLauncher(launch);
        StudioMojo mojo = new StudioMojo(launcher);
        MavenProject project = project(temporaryDirectory);
        set(mojo, "project", project);
        set(mojo, "openBrowser", false);

        mojo.execute();

        assertSame(launch, launcher.result);
        assertFalse(launcher.openBrowser);
        assertTrue(launch.awaited);
        assertTrue(launch.closed);
    }

    private static MavenProject project(Path root) {
        return new MavenProject() {
            @Override
            public File getBasedir() {
                return root.toFile();
            }

            @Override
            public List<String> getCompileSourceRoots() {
                return List.of(root.resolve("src/main/java").toString());
            }

            @Override
            public List<String> getTestCompileSourceRoots() {
                return List.of(root.resolve("src/test/java").toString());
            }

            @Override
            public List<String> getTestClasspathElements() throws DependencyResolutionRequiredException {
                return List.of(root.resolve("target/test-classes").toString());
            }
        };
    }

    private static void set(StudioMojo mojo, String field, Object value) throws Exception {
        var declared = StudioMojo.class.getDeclaredField(field);
        declared.setAccessible(true);
        declared.set(mojo, value);
    }

    private static final class RecordingLauncher implements StudioLauncher {
        private final StudioLaunch result;
        private boolean openBrowser;

        private RecordingLauncher(StudioLaunch result) {
            this.result = result;
        }

        @Override
        public StudioLaunch launch(StudioProjectContext context, boolean openBrowser) {
            this.openBrowser = openBrowser;
            return result;
        }
    }

    private static final class RecordingLaunch implements StudioLaunch {
        private boolean awaited;
        private boolean closed;

        @Override
        public URI uri() {
            return URI.create("http://127.0.0.1:12345/");
        }

        @Override
        public String projectId() {
            return "project-1";
        }

        @Override
        public void await() {
            awaited = true;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
