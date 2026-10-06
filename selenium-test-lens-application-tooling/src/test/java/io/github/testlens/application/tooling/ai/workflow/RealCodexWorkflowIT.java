package io.github.testlens.application.tooling.ai.workflow;

import io.github.testlens.application.tooling.ai.AgentContextPack;
import io.github.testlens.application.tooling.ai.AgentTask;
import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.FailureClassification;
import io.github.testlens.application.tooling.ai.workflow.runner.CodexCliDetector;
import io.github.testlens.application.tooling.ai.workflow.runner.CodexCliProfiles;
import io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner;
import io.github.testlens.application.tooling.ai.workflow.runner.RoleDispatchingAgentExecutor;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Opt-in local dogfood: this class is not selected by the default Surefire patterns. */
class RealCodexWorkflowIT {
    @TempDir Path temporaryDirectory;

    @Test void realCodexProducesPlanImplementationAndReviewForBoundedPageObjectContext() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("testlens.codex.dogfood"),
                "Set -Dtestlens.codex.dogfood=true for an authenticated local Codex run");
        Path executable = CodexCliDetector.detect().orElseThrow(() ->
                new AssertionError("Opt-in dogfood requested, but a native Codex executable was not detected"));

        EnumMap<AgentExecutor.Role, AgentExecutor> roles = new EnumMap<>(AgentExecutor.Role.class);
        for (AgentExecutor.Role role : List.of(AgentExecutor.Role.TEST_ARCHITECT,
                AgentExecutor.Role.TEST_IMPLEMENTER, AgentExecutor.Role.CODE_REVIEWER)) {
            var profile = CodexCliProfiles.readOnly(executable, "codex-" + role.name().toLowerCase(), role,
                    Duration.ofMinutes(3), CodexCliProfiles.defaultEnvironmentAllowlist());
            roles.put(role, new ExternalAgentRunner(profile, temporaryDirectory.resolve("staging-" + role.name())));
        }

        AgentWorkflowCoordinator coordinator = new AgentWorkflowCoordinator(
                new TestEngineeringWorkflow(WorkflowPolicy.defaults()),
                new RoleDispatchingAgentExecutor(roles), new TargetedJavaCompiler(),
                (executionRequest, compiledOutput) -> {
                    Class<?> generated = compiledOutput.loadClass(executionRequest.testClass(),
                            RealCodexWorkflowIT.class.getClassLoader());
                    Object result = generated.getMethod("execute").invoke(null);
                    boolean passed = Boolean.TRUE.equals(result);
                    return new TargetedTestExecutor.ExecutionResult(passed, 1,
                            List.of("agent-produced bytecode execute()=" + result));
                },
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                execution -> new FailureClassification(header(), FailureClassification.Category.UNKNOWN,
                        "No failure", List.of(), List.of(), List.of()),
                AgentWorkflowCoordinator.Stabilizer.none(),
                new AgentWorkflowCoordinator.Configuration(Path.of("src/test/java/example/InvalidPasswordTest.java"),
                        "example.InvalidPasswordTest", null, 17, "", 12, Duration.ofSeconds(10),
                        List.of("example.InvalidPasswordTest"), 2));

        AgentWorkflowCoordinator.Result result = coordinator.run("real-codex-invalid-password",
                request(), boundedContext());
        assertEquals(TestEngineeringRun.State.SUCCESS, result.run().state());
        assertEquals(3, result.metrics().agentAttempts());
    }

    private static TestEngineeringRequest request() {
        return new TestEngineeringRequest(new TestEngineeringRequest.Requirement(
                "Add a test that an invalid password displays the login error", List.of("error is visible")),
                new TestEngineeringRequest.Scope(List.of("login-page"), List.of()),
                new TestEngineeringRequest.Framework("plain Java compile fixture", "17", "none"),
                new TestEngineeringRequest.Target("fixture", "InvalidPasswordTest", "invalid-password"),
                List.of("src/test/java"), TestEngineeringRequest.ExecutionPolicy.TARGETED_AFTER_COMPILE);
    }

    private static AgentContextPack boundedContext() {
        AgentTask task = new AgentTask(header(), AgentTask.TaskType.CREATE_TEST,
                "Add a test that an invalid password displays the login error", List.of("login-page"),
                List.of("login-error"), List.of("login-submit"), List.of(
                "Return a complete compilable Java source file in sourcePatch",
                "Use package example and class InvalidPasswordTest",
                "The class must expose public static boolean execute() and return true after modelling the successful Page Object assertion",
                "Use no imports or external libraries",
                "Use Page Object APIs only; never create selectors or call WebDriver directly"));
        return new AgentContextPack(header(), task, List.of(), Map.of("LoginPage", List.of(
                "LoginPage login(String username, String password)", "boolean errorVisible()")),
                List.of("Plain Java fixture; model the Page Object interaction without external dependencies"),
                List.of(new AgentContextPack.ScopeDecision("login-page", "PAGE", "REQUIRED_PAGE")),
                List.of(new AgentContextPack.ScopeDecision("customers-page", "PAGE", "OUTSIDE_TASK_SUBGRAPH")),
                AgentContextPack.Completeness.COMPLETE_FOR_REQUESTED_SCOPE);
    }

    private static ContractHeader header() {
        return new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of("bounded dogfood fixture"), List.of(), ContractHeader.Confidence.OBSERVED);
    }
}
