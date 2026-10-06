package io.github.testlens.application.tooling.ai.workflow.runner;

import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.json.StrictJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CodexCliProfilesTest {
    @TempDir Path temporary;

    @Test void codexProfileUsesVerifiedNonInteractiveReadOnlyFileContract() throws Exception {
        Path executable = Files.createFile(temporary.resolve("codex.exe")).toAbsolutePath();
        AgentProfile profile = CodexCliProfiles.readOnly(executable, "codex-architect",
                AgentExecutor.Role.TEST_ARCHITECT, Duration.ofMinutes(2), Set.of("CODEX_HOME"));

        assertEquals(AgentProfile.OutputTransport.FILE, profile.outputTransport());
        assertTrue(profile.arguments().containsAll(Set.of("exec", "--ephemeral", "--ignore-user-config",
                "--ignore-rules", "--sandbox", "read-only", "--output-schema", "--output-last-message", "-")));
        assertFalse(profile.arguments().stream().anyMatch(value -> value.contains("dangerously")));
        assertFalse(profile.arguments().stream().anyMatch(value -> value.contains("ask-for-approval")));
    }

    @Test void detectorAcceptsDirectExecutableAndNeverReturnsCommandScript() throws Exception {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        Path direct = Files.createFile(temporary.resolve(windows ? "codex.exe" : "codex"));
        try {
            Files.setPosixFilePermissions(direct, Set.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        } catch (UnsupportedOperationException ignored) { }
        Path commandScript = Files.createFile(temporary.resolve("codex.cmd"));

        Path detected = CodexCliDetector.detect(Map.of("PATH", temporary.toString()),
                System.getProperty("os.name", ""), System.getProperty("os.arch", "")).orElseThrow();
        assertEquals(direct.toAbsolutePath().normalize(), detected);
        assertNotEquals(commandScript, detected);
    }

    @Test void schemasAreBoundedJsonAndRejectUnknownResultFields() {
        AgentProtocolCodec codec = new AgentProtocolCodec();
        Map<String, Object> schema = StrictJson.readObject(codec.outputSchema(AgentExecutor.Role.TEST_IMPLEMENTER));
        assertEquals("object", schema.get("type"));
        assertEquals(false, schema.get("additionalProperties"));

        byte[] unknown = """
                {"schemaVersion":1,"resultType":"TEST_IMPLEMENTATION_PROPOSAL","payload":{
                  "header":{"schemaVersion":1,"status":"READY","evidence":[],"limitations":[],"confidence":"AI_PROPOSED"},
                  "scenarioId":"s","sourcePatch":"class T {}","selectorAccessPolicy":"PAGE_OBJECTS_ONLY",
                  "pageObjectApisUsed":[],"missingCapabilities":[],"unexpected":true}}
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertThrows(StrictJson.JsonFormatException.class,
                () -> codec.decode(AgentExecutor.Role.TEST_IMPLEMENTER, unknown));
    }
}
