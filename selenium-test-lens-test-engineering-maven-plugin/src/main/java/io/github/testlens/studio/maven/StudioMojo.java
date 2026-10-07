package io.github.testlens.studio.maven;

import java.util.Objects;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Launches Test Engineering Studio with the current Maven project's source roots and test classpath.
 *
 * @since 0.5.0
 */
@Mojo(
        name = "studio",
        defaultPhase = LifecyclePhase.NONE,
        requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = false)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public final class StudioMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "testLens.studio.openBrowser", defaultValue = "true")
    private boolean openBrowser;

    private final StudioLauncher launcher;

    public StudioMojo() {
        this(new PublicStudioLauncher());
    }

    StudioMojo(StudioLauncher launcher) {
        this.launcher = Objects.requireNonNull(launcher, "launcher");
    }

    @Override
    public void execute() throws MojoExecutionException {
        StudioProjectContext context;
        try {
            context = MavenProjectContextFactory.from(project);
        } catch (DependencyResolutionRequiredException exception) {
            throw new MojoExecutionException("Test classpath is not resolved for Test Engineering Studio", exception);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new MojoExecutionException("Maven project context is invalid for Test Engineering Studio", exception);
        }

        try (StudioLaunch launch = launcher.launch(context, openBrowser)) {
            getLog().info("Test Engineering Studio: " + launch.uri() + " (project " + launch.projectId() + ")");
            launch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Interrupted while waiting for Test Engineering Studio", exception);
        } catch (Exception exception) {
            throw new MojoExecutionException("Unable to launch Test Engineering Studio", exception);
        }
    }
}
