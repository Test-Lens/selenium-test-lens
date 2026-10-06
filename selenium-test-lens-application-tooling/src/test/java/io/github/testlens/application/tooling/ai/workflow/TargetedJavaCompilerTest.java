package io.github.testlens.application.tooling.ai.workflow;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TargetedJavaCompilerTest {
    @Test void compilesInMemoryWhenSourcePreconditionMatches() {
        Path path = Path.of("src/test/java/example/Generated.java");
        String current = "package example; class Generated {}";
        String proposed = "package example; public class Generated { public int answer() { return 42; } }";
        var request = new TargetedJavaCompiler.CompilationRequest(List.of(
                new TargetedJavaCompiler.SourceUnit(path, "example.Generated", proposed, ArtifactEnvelope.digest(current))),
                17, System.getProperty("java.class.path"), 10);
        var result = new TargetedJavaCompiler().compile(request, Map.of(path, current));
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        assertEquals(1, result.generatedClasses());
    }

    @Test void refusesStaleSourceWithoutInvokingCompilation() {
        Path path = Path.of("src/test/java/example/Generated.java");
        var request = new TargetedJavaCompiler.CompilationRequest(List.of(
                new TargetedJavaCompiler.SourceUnit(path, "example.Generated", "broken !!!", ArtifactEnvelope.digest("expected"))),
                17, "", 10);
        var result = new TargetedJavaCompiler().compile(request, Map.of(path, "changed"));
        assertFalse(result.successful());
        assertEquals("SOURCE_PRECONDITION_FAILED", result.diagnostics().get(0).code());
    }

    @Test void scriptedExecutorIsDeterministicAndBounded() throws Exception {
        var payload = new PageObjectCapabilityMissing("LOGIN", "clearUsername", "API is absent");
        var artifact = ArtifactEnvelope.create("missing", "run", List.of(), 1, payload);
        ScriptedAgentExecutor executor = new ScriptedAgentExecutor(List.of(new AgentExecutor.AgentResult("one", artifact)));
        var command = new AgentExecutor.AgentCommand("run", AgentExecutor.Role.IMPLEMENTER, List.of(), Map.of());
        assertEquals(payload, executor.execute(command).artifact().payload());
        assertEquals(0, executor.remaining());
        assertThrows(AgentExecutor.AgentExecutionException.class, () -> executor.execute(command));
    }

    @Test void targetedExecutionIsAnInjectedBoundary() throws Exception {
        TargetedTestExecutor executor = request -> new TargetedTestExecutor.ExecutionResult(true, 1, List.of("target passed"));
        var result = executor.execute(new TargetedTestExecutor.ExecutionRequest(
                "run", "example.GeneratedTest", List.of("scenario"), Duration.ofSeconds(5)));
        assertTrue(result.successful());
        assertEquals(1, result.tests());
    }
}
