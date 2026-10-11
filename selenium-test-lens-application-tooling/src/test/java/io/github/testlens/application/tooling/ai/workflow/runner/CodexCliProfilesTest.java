package io.github.testlens.application.tooling.ai.workflow.runner;

import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.json.StrictJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.List;
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
        assertEquals(Set.of("schemaVersion", "result"),
                Set.copyOf(assertInstanceOf(List.class, schema.get("required"))));
        assertFalse(schema.containsKey("anyOf"));
        assertFalse(schema.containsKey("oneOf"));
        Map<?, ?> rootProperties = assertInstanceOf(Map.class, schema.get("properties"));
        Map<?, ?> result = assertInstanceOf(Map.class, rootProperties.get("result"));
        List<?> alternatives = assertInstanceOf(List.class, result.get("anyOf"));
        assertEquals(3, alternatives.size());
        Map<String, Set<String>> payloadFields = Map.of(
                "TEST_IMPLEMENTATION_PROPOSAL", Set.of("header", "scenarioId", "sourcePatch",
                        "selectorAccessPolicy", "pageObjectApisUsed", "missingCapabilities"),
                "PAGE_OBJECT_CAPABILITY_MISSING", Set.of("pageId", "capability", "reason"),
                "PAGE_OBJECT_EXTENSION_PROPOSAL", Set.of("missing", "extensionClass", "proposedMethod", "evidence"));
        for (Object value : alternatives) {
            Map<?, ?> branch = assertInstanceOf(Map.class, value);
            assertEquals("object", branch.get("type"));
            assertEquals(false, branch.get("additionalProperties"));
            assertEquals(Set.of("resultType", "payload"),
                    Set.copyOf(assertInstanceOf(List.class, branch.get("required"))));
            Map<?, ?> properties = assertInstanceOf(Map.class, branch.get("properties"));
            Map<?, ?> resultType = assertInstanceOf(Map.class, properties.get("resultType"));
            String discriminator = assertInstanceOf(String.class, resultType.get("const"));
            Map<?, ?> payload = assertInstanceOf(Map.class, properties.get("payload"));
            assertEquals("object", payload.get("type"));
            assertEquals(false, payload.get("additionalProperties"));
            Set<String> expectedFields = payloadFields.get(discriminator);
            assertNotNull(expectedFields, "unexpected Implementer result discriminator");
            assertEquals(expectedFields, Set.copyOf(assertInstanceOf(List.class, payload.get("required"))));
            assertEquals(expectedFields,
                    assertInstanceOf(Map.class, payload.get("properties")).keySet());
        }
        assertEquals(payloadFields.keySet(), alternatives.stream()
                .map(Map.class::cast)
                .map(branch -> Map.class.cast(branch.get("properties")))
                .map(properties -> Map.class.cast(properties.get("resultType")))
                .map(resultType -> String.class.cast(resultType.get("const")))
                .collect(java.util.stream.Collectors.toSet()));

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
