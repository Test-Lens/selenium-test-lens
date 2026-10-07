package io.github.testlens.studio.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectDiscoveryTest {
    @TempDir Path temporary;

    @Test void discoversConventionalMavenProjectWithoutConfiguration() throws Exception {
        Path root=mavenProject("first");
        ProjectDescriptor descriptor=new ProjectDiscovery().discover(root);

        assertEquals(ProjectDescriptor.BuildSystem.MAVEN,descriptor.buildSystem());
        assertEquals(ProjectDescriptor.Status.READY,descriptor.status());
        assertEquals(ProjectDescriptor.ConfigurationSource.AUTO_DETECTED,descriptor.configurationSource());
        assertEquals(List.of(root.resolve("src/main/java").toRealPath()),descriptor.mainSourceRoots());
        assertEquals(List.of(root.resolve("src/test/java").toRealPath()),descriptor.testSourceRoots());
        assertEquals(root.resolve(".test-lens"),descriptor.workspaceDirectory());
    }

    @Test void appliesCliOverridesAfterProjectConfiguration() throws Exception {
        Path root=mavenProject("configured"),custom=root.resolve("custom-tests");Files.createDirectories(custom);
        Files.createDirectories(root.resolve(".test-lens"));
        Files.writeString(root.resolve(".test-lens/project.json"),"""
                {"schemaVersion":1,"applicationName":"from-config","sourceRoots":["src/test/java"],"browser":"firefox"}
                """);
        var overrides=new ProjectDiscovery.Overrides(null,"from-cli",List.of(Path.of("custom-tests")),
                null,null,null,"chrome",false);
        ProjectDescriptor descriptor=new ProjectDiscovery().discover(new ProjectDiscovery.Request(root,overrides));

        assertEquals("from-cli",descriptor.applicationName());
        assertEquals(ProjectDescriptor.ConfigurationSource.CLI_OVERRIDE,descriptor.configurationSource());
        assertEquals(List.of(custom.toAbsolutePath().normalize()),descriptor.sourceRoots());
        assertEquals(ProjectDescriptor.Browser.CHROME,descriptor.browser().browser());
        assertFalse(descriptor.browser().headless());
    }

    @Test void rejectsUnsupportedSchemaPathEscapeAndSecretLikeValues() throws Exception {
        Path root=mavenProject("invalid");Files.createDirectories(root.resolve(".test-lens"));
        Path config=root.resolve(".test-lens/project.json");
        Files.writeString(config,"{\"schemaVersion\":2}");
        assertThrows(IllegalArgumentException.class,()->new ProjectDiscovery().discover(root));
        Files.writeString(config,"{\"schemaVersion\":1,\"sourceRoots\":[\"../outside\"]}");
        assertThrows(IllegalArgumentException.class,()->new ProjectDiscovery().discover(root));
        Files.writeString(config,"{\"schemaVersion\":1,\"startUrl\":\"https://example.test/reset?token=secret\"}");
        assertThrows(IllegalArgumentException.class,()->new ProjectDiscovery().discover(root));
        Files.writeString(config,"{\"schemaVersion\":1,\"notes\":\"Authorization: Bearer canary\"}");
        assertThrows(IllegalArgumentException.class,()->new ProjectDiscovery().discover(root));
    }

    @Test void projectIdentityDoesNotDependOnAbsoluteDirectory() throws Exception {
        ProjectDescriptor first=new ProjectDiscovery().discover(mavenProject("move-a"));
        ProjectDescriptor second=new ProjectDiscovery().discover(mavenProject("move-b"));
        assertEquals(first.projectId(),second.projectId());
    }

    @Test void reportsGradleAsUnsupportedInsteadOfPretendingItCanExecuteIt() throws Exception {
        Path root=temporary.resolve("gradle");Files.createDirectories(root.resolve("src/test/java"));Files.writeString(root.resolve("build.gradle"),"plugins { id 'java' }");
        ProjectDescriptor descriptor=new ProjectDiscovery().discover(root);
        assertEquals(ProjectDescriptor.BuildSystem.GRADLE,descriptor.buildSystem());
        assertEquals(ProjectDescriptor.Status.UNSUPPORTED_PROJECT,descriptor.status());
    }

    @Test void descriptorItselfRejectsWorkspaceOutsideProjectRoot() throws Exception {
        Path root=mavenProject("manual-descriptor");
        assertThrows(IllegalArgumentException.class,()->new ProjectDescriptor(1,"project-1","fixture",root,
                ProjectDescriptor.BuildSystem.MAVEN,ProjectDescriptor.Status.READY,ProjectDescriptor.ConfigurationSource.CLI_OVERRIDE,
                List.of(root.resolve("src/main/java")),List.of(root.resolve("src/test/java")),List.of(),temporary.resolve("outside"),null,
                ProjectDescriptor.BrowserFlags.defaults(),List.of(),List.of()));
    }

    private Path mavenProject(String directory) throws Exception {
        Path root=temporary.resolve(directory);Files.createDirectories(root.resolve("src/main/java"));Files.createDirectories(root.resolve("src/test/java"));
        Files.writeString(root.resolve("pom.xml"),"<project><modelVersion>4.0.0</modelVersion><groupId>x</groupId><artifactId>same-project</artifactId><version>1</version></project>");
        return root.toRealPath();
    }
}
