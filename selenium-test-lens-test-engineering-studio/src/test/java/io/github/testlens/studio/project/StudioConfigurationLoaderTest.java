package io.github.testlens.studio.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StudioConfigurationLoaderTest {
    @TempDir Path project;

    @Test void localMachineConfigOverridesCommitSafeProjectConfig() throws Exception {
        Files.createDirectories(project.resolve(".test-lens"));
        Files.writeString(project.resolve(".test-lens/project.json"),"""
                {"schemaVersion":1,"browser":{"name":"chrome","headless":true},"agentProfiles":{"TEST_ARCHITECT":"project-high"}}
                """);
        Files.writeString(project.resolve(".test-lens/local.json"),"""
                {"schemaVersion":1,"browser":{"name":"firefox","headless":false},"agents":{"provider":"CODEX","profiles":{"TEST_ARCHITECT":"local-high"}}}
                """);
        var actual=new StudioConfigurationLoader().load(project);
        assertEquals("FIREFOX",actual.browser());assertFalse(actual.headless());
        assertEquals("local-high",actual.agentProfiles().get("TEST_ARCHITECT"));
    }

    @Test void futureSchemaUnknownFieldAndSecretsFailClosed() throws Exception {
        Files.createDirectories(project.resolve(".test-lens"));
        Path config=project.resolve(".test-lens/local.json");
        Files.writeString(config,"{\"schemaVersion\":2}");
        assertThrows(IllegalArgumentException.class,()->new StudioConfigurationLoader().load(project));
        Files.writeString(config,"{\"schemaVersion\":1,\"unexpected\":true}");
        assertThrows(IllegalArgumentException.class,()->new StudioConfigurationLoader().load(project));
        Files.writeString(config,"{\"schemaVersion\":1,\"agents\":{\"token\":\"secret\"}}");
        assertTrue(assertThrows(IllegalArgumentException.class,()->new StudioConfigurationLoader().load(project)).getMessage().contains("CONFIG_REJECTED"));
    }

    @Test void executableMustBeAbsolute() throws Exception {
        Files.createDirectories(project.resolve(".test-lens"));
        Files.writeString(project.resolve(".test-lens/local.json"),"{\"schemaVersion\":1,\"codexExecutable\":\"codex\"}");
        assertThrows(IllegalArgumentException.class,()->new StudioConfigurationLoader().load(project));
    }
}
