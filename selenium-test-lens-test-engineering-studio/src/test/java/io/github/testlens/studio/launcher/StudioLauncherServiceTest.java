package io.github.testlens.studio.launcher;

import io.github.testlens.studio.StudioWorkflowGateway;
import io.github.testlens.studio.project.ProjectDescriptor;
import io.github.testlens.studio.project.ProjectDiscovery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StudioLauncherServiceTest {
    @TempDir Path root;

    @Test void launchesOnLoopbackFreePortWithoutRunningProjectActions() throws Exception {
        Files.createDirectories(root.resolve("src/test/java"));Files.writeString(root.resolve("pom.xml"),"<project><modelVersion>4.0.0</modelVersion><artifactId>fixture</artifactId></project>");
        ProjectDescriptor descriptor=new ProjectDiscovery().discover(root);
        AtomicInteger browserCalls=new AtomicInteger();
        StudioWorkflowGateway gateway=runId->{throw new AssertionError("launcher must not invoke workflow");};
        StudioLauncherService launcher=new StudioLauncherService(gateway,null,uri->{browserCalls.incrementAndGet();return true;});

        try(var handle=launcher.launch(descriptor,new StudioLauncherService.LaunchOptions(false))){
            assertTrue(handle.uri().getHost().equals("127.0.0.1")||handle.uri().getHost().equals("localhost"));
            assertTrue(handle.uri().getPort()>0);assertEquals(0,browserCalls.get());
            HttpURLConnection bootstrap=(HttpURLConnection)handle.uri().toURL().openConnection();
            assertEquals(200,bootstrap.getResponseCode());bootstrap.getInputStream().readAllBytes();
            String cookie=bootstrap.getHeaderField("Set-Cookie").split(";",2)[0];
            HttpURLConnection connection=(HttpURLConnection)handle.uri().resolve("api/config").toURL().openConnection();
            connection.setRequestProperty("Cookie",cookie);
            assertEquals(200,connection.getResponseCode());
            String body=new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(body.contains("AUTO_DETECTED"));assertTrue(body.contains("fixture"));
            assertTrue(handle.service().workflowHistory().isEmpty());
        }
    }

    @Test void refusesUnsupportedProjectBeforeBinding() throws Exception {
        Files.createDirectories(root.resolve("src/test/java"));Files.writeString(root.resolve("build.gradle"),"plugins { id 'java' }");
        ProjectDescriptor descriptor=new ProjectDiscovery().discover(root);
        var launcher=new StudioLauncherService(runId->{throw new AssertionError();},null,uri->false);
        assertThrows(IllegalArgumentException.class,()->launcher.launch(descriptor,new StudioLauncherService.LaunchOptions(false)));
    }

    @Test void configuredWorkspaceIsTheActualPersistenceRoot() throws Exception {
        Files.createDirectories(root.resolve("src/test/java"));Files.createDirectories(root.resolve(".test-lens"));
        Files.writeString(root.resolve("pom.xml"),"<project><modelVersion>4.0.0</modelVersion><artifactId>fixture</artifactId></project>");
        Files.writeString(root.resolve(".test-lens/project.json"),"{\"schemaVersion\":1,\"workspaceDirectory\":\"studio-state\"}");
        ProjectDescriptor descriptor=new ProjectDiscovery().discover(root);
        var launcher=new StudioLauncherService(runId->{throw new AssertionError();},null,uri->false);
        try(var handle=launcher.launch(descriptor,new StudioLauncherService.LaunchOptions(false))){
            assertEquals(root.resolve("studio-state").toAbsolutePath().normalize(),handle.service().workspace().workspaceRoot());
        }
    }
}
