package io.github.testlens.studio;

import io.github.testlens.application.tooling.ai.*;
import io.github.testlens.application.tooling.ai.workflow.*;
import io.github.testlens.application.tooling.ai.workflow.runner.AgentProfile;
import io.github.testlens.application.tooling.ai.workflow.runner.AgentProtocolCodec;
import io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner;
import io.github.testlens.application.tooling.ai.workflow.runner.PageObjectApiReferenceValidator;
import io.github.testlens.application.tooling.ai.workflow.runner.RoleDispatchingAgentExecutor;
import io.github.testlens.core.redaction.RedactionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ReviewableCoordinatorWorkflowGatewayTest {
    @Test void legacyConstructorUsesTrustedContextFingerprintThroughApproval() throws Exception {
        AtomicInteger executions = new AtomicInteger();
        TestImplementationProposal implementation = new TestImplementationProposal(ready(), "invalid-password",
                VALID_PAGE_OBJECT_SOURCE, TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,
                List.of(API_REFERENCE), List.of());
        var gateway = new ReviewableCoordinatorWorkflowGateway(
                scripted(new TestPlan(ready(), List.of()), implementation, review()),
                replay -> coordinator(replay, executions));
        gateway.prepare("run-legacy-constructor", request(), capabilityContext());

        gateway.generatePlan("run-legacy-constructor", "requirement");
        gateway.generateImplementation("run-legacy-constructor");

        assertEquals(TestEngineeringRun.State.SUCCESS, gateway.run("run-legacy-constructor").finalState());
        assertEquals(1, executions.get());
    }
    private static final String API_REFERENCE =
            "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)";
    private static final String VALID_PAGE_OBJECT_SOURCE = "package example; public final class GeneratedTest { "
            + "@org.junit.jupiter.api.Test void test() { org.junit.jupiter.api.Assertions.assertTrue("
            + "new benchmark.LoginPage().invalidPasswordShowsExpectedError(\"fixture-user\", \"fixture-invalid\")); } }";
    @TempDir Path temporary;

    @Test void implementerReceivesTheConcreteFullSourceContract() throws Exception {
        TestPlan plan=new TestPlan(ready(),List.of());
        TestImplementationProposal implementation=new TestImplementationProposal(ready(),"invalid-password",
                VALID_PAGE_OBJECT_SOURCE,
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,List.of(API_REFERENCE),List.of());
        ScriptedAgentExecutor delegate=scripted(plan,implementation);
        AtomicReference<AgentExecutor.AgentCommand> implementationCommand=new AtomicReference<>();
        AgentExecutor capturing=command->{if(command.role()==AgentExecutor.Role.TEST_IMPLEMENTER)implementationCommand.set(command);return delegate.execute(command);};
        var gateway=new ReviewableCoordinatorWorkflowGateway(capturing,
                (replay,ignored)->coordinator(replay,new AtomicInteger()),
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ignored->Path.of("src/test/java/GeneratedTest.java"),proposal->{},()->"fp");
        gateway.prepare("run-contract",request(),capabilityContext());gateway.generatePlan("run-contract","requirement");gateway.generateImplementation("run-contract");

        Map<String,String> instructions=implementationCommand.get().instructions();
        assertEquals("GeneratedTest",instructions.get("targetTestClass"));
        assertEquals("src/test/java/GeneratedTest.java",instructions.get("targetSourcePath"));
        assertEquals("JUnit 5",instructions.get("framework"));
        assertTrue(instructions.get("sourcePatchContract").contains("Complete non-empty compilable JUnit 5 Java source file"));
        assertTrue(instructions.get("sourcePatchContract").contains("at least one @Test method"));
        assertTrue(instructions.get("sourcePatchContract").contains("never return an empty TEST_IMPLEMENTATION_PROPOSAL"));
    }

    @Test void implementerReceivesVerifiedStudioRuntimeAcquisitionAndReturnSemantics() throws Exception {
        TestPlan plan=new TestPlan(ready(),List.of());
        TestImplementationProposal implementation=new TestImplementationProposal(ready(),"invalid-password",
                VALID_PAGE_OBJECT_SOURCE,
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,List.of(API_REFERENCE),List.of());
        ScriptedAgentExecutor delegate=scripted(plan,implementation);
        AtomicReference<AgentExecutor.AgentCommand> implementationCommand=new AtomicReference<>();
        AgentExecutor capturing=command->{if(command.role()==AgentExecutor.Role.TEST_IMPLEMENTER)implementationCommand.set(command);return delegate.execute(command);};
        var gateway=new ReviewableCoordinatorWorkflowGateway(capturing,
                (replay,ignored)->coordinator(replay,new AtomicInteger()),
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ignored->Path.of("src/test/java/example/GeneratedTest.java"),proposal->{},()->"fp");
        gateway.prepare("run-runtime",request(),capabilityContext());gateway.generatePlan("run-runtime","requirement");

        gateway.generateImplementation("run-runtime");

        String contract=implementationCommand.get().instructions().get("pageObjectRuntimeContract");
        assertNotNull(contract);
        assertTrue(contract.contains("new benchmark.LoginPage()"));
        assertTrue(contract.contains("STUDIO_BOUND_BROWSER_SESSION"));
        assertTrue(contract.contains("invalidPasswordShowsExpectedError"));
        assertTrue(contract.contains("BOOLEAN_ASSERTION_COMPATIBLE"));
        assertTrue(contract.contains("recordAttempt"));
        assertTrue(contract.contains("VOID_NOT_VALUE_ASSERTABLE"));
    }

    @Test void implementerCapabilityResultKeepsItsOriginalDiagnosticType() throws Exception {
        PageObjectCapabilityMissing missing=new PageObjectCapabilityMissing("login","construct page object",
                "No supported constructor was present in the bounded context");
        var gateway=new ReviewableCoordinatorWorkflowGateway(scripted(new TestPlan(ready(),List.of()),missing),
                replay->coordinator(replay,new AtomicInteger()));
        gateway.prepare("run-missing",request(),context());gateway.generatePlan("run-missing","requirement");

        PageObjectCapabilityException failure=assertThrows(PageObjectCapabilityException.class,
                ()->gateway.generateImplementation("run-missing"));
        assertEquals("PAGE_OBJECT_CAPABILITY_MISSING",failure.reasonCode());
        assertEquals("$.payload.capability",failure.location());
        assertEquals("REVISE_REQUIREMENT_OR_PAGE_OBJECT",failure.nextAction());
        assertFalse(failure.getMessage().contains("construct page object"));
    }

    @Test void verifiedCapabilityGetsOneControlledImplementationRetryInsteadOfFalseRefusal() throws Exception {
        TestPlan plan=plan("assertTrue(loginPage.invalidPasswordShowsExpectedError(\"user\", \"bad\"))");
        PageObjectCapabilityMissing missing=new PageObjectCapabilityMissing("login","construct page object",
                "The response did not use the verified construction contract");
        TestImplementationProposal implementation=new TestImplementationProposal(ready(),"invalid-password",
                VALID_PAGE_OBJECT_SOURCE,
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,List.of(API_REFERENCE),List.of());
        AtomicInteger implementerCalls=new AtomicInteger();
        ScriptedAgentExecutor delegate=scripted(plan,missing,implementation);
        AgentExecutor capturing=command->{
            if(command.role()==AgentExecutor.Role.TEST_IMPLEMENTER)implementerCalls.incrementAndGet();
            return delegate.execute(command);
        };
        var gateway=gateway(capturing,capabilityContext(),"fp",new AtomicInteger());
        gateway.prepare("run-verified-retry",request(),capabilityContext());
        gateway.generatePlan("run-verified-retry","requirement");

        TestImplementationProposal result=gateway.generateImplementation("run-verified-retry");

        assertEquals(implementation,result);
        assertEquals(2,implementerCalls.get(),"only one controlled retry is allowed");
    }

    @Test void unknownNaturalLanguagePlanRetriesOnceWhenRefusedMethodIsVerified() throws Exception {
        TestPlan plan=plan("Submit invalid credentials and verify that the error is visible");
        PageObjectCapabilityMissing refusal=new PageObjectCapabilityMissing("login",
                "benchmark.LoginPage.invalidPasswordShowsExpectedError(String,String)",
                "untrusted refusal detail");
        TestImplementationProposal implementation=new TestImplementationProposal(ready(),"invalid-password",
                VALID_PAGE_OBJECT_SOURCE,
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,List.of(API_REFERENCE),List.of());
        AtomicInteger implementerCalls=new AtomicInteger();
        ScriptedAgentExecutor delegate=scripted(plan,refusal,implementation);
        AgentExecutor capturing=command->{
            if(command.role()==AgentExecutor.Role.TEST_IMPLEMENTER)implementerCalls.incrementAndGet();
            return delegate.execute(command);
        };
        var gateway=gateway(capturing,capabilityContext(),"fp",new AtomicInteger());
        gateway.generatePlan("run-natural-method","requirement");

        assertEquals(implementation,gateway.generateImplementation("run-natural-method"));
        assertEquals(2,implementerCalls.get(),"a verified false refusal permits exactly one retry");
    }

    @Test void unknownNaturalLanguagePlanRetriesOnceWhenRefusedConstructionIsVerified() throws Exception {
        TestPlan plan=plan("Submit invalid credentials and verify that the error is visible");
        PageObjectCapabilityMissing refusal=new PageObjectCapabilityMissing("login",
                "construct benchmark.LoginPage with its public constructor", "untrusted refusal detail");
        TestImplementationProposal implementation=new TestImplementationProposal(ready(),"invalid-password",
                VALID_PAGE_OBJECT_SOURCE,
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,List.of(API_REFERENCE),List.of());
        AtomicInteger implementerCalls=new AtomicInteger();
        ScriptedAgentExecutor delegate=scripted(plan,refusal,implementation);
        AgentExecutor capturing=command->{
            if(command.role()==AgentExecutor.Role.TEST_IMPLEMENTER)implementerCalls.incrementAndGet();
            return delegate.execute(command);
        };
        var gateway=gateway(capturing,capabilityContext(),"fp",new AtomicInteger());
        gateway.generatePlan("run-natural-constructor","requirement");

        assertEquals(implementation,gateway.generateImplementation("run-natural-constructor"));
        assertEquals(2,implementerCalls.get(),"a verified construction refusal permits exactly one retry");
    }

    @Test void genuinelyMissingMethodIsRefusedWithoutRetryOrPipelineCreation() throws Exception {
        TestPlan plan=plan("Submit invalid credentials and verify that the error is visible");
        PageObjectCapabilityMissing refusal=new PageObjectCapabilityMissing("login",
                "benchmark.LoginPage.submitMissingOperation(String)", "untrusted missing-method reason");
        AtomicInteger implementerCalls=new AtomicInteger(),coordinatorCreations=new AtomicInteger();
        ScriptedAgentExecutor delegate=scripted(plan,refusal);
        AgentExecutor capturing=command->{
            if(command.role()==AgentExecutor.Role.TEST_IMPLEMENTER)implementerCalls.incrementAndGet();
            return delegate.execute(command);
        };
        var gateway=gateway(capturing,capabilityContext(),"fp",coordinatorCreations);
        gateway.generatePlan("run-real-missing","requirement");

        PageObjectCapabilityException failure=assertThrows(PageObjectCapabilityException.class,
                ()->gateway.generateImplementation("run-real-missing"));

        assertEquals(PageObjectCapabilityException.RefusalCategory.PAGE_OBJECT_METHOD_MISSING,
                failure.refusalCategory());
        assertEquals("login",failure.verifiedPageId());
        assertTrue(failure.sourceFingerprintMatched());
        assertEquals(1,implementerCalls.get(),"a real missing capability must not be retried");
        assertThrows(IllegalStateException.class,()->gateway.run("run-real-missing"));
        assertEquals(0,coordinatorCreations.get(),"a capability refusal must stop before compiler pipeline creation");
    }

    @Test void verifiedPlanDoesNotRetryAnUnverifiedRefusal() throws Exception {
        assertVerifiedPlanRefusalIsNotRetried("unknown-page",
                "benchmark.LoginPage.invalidPasswordShowsExpectedError(String,String)",capabilityContext(),"fp",
                PageObjectCapabilityException.RefusalCategory.UNKNOWN);
        assertVerifiedPlanRefusalIsNotRetried("login",
                "benchmark.LoginPage.invalidPasswordShowsExpectedError(String,String)",capabilityContext(),"changed-fingerprint",
                PageObjectCapabilityException.RefusalCategory.CONTEXT_INCOMPLETE);
        assertVerifiedPlanRefusalIsNotRetried("login",
                "benchmark.LoginPage.submitMissingOperation(String)",capabilityContext(),"fp",
                PageObjectCapabilityException.RefusalCategory.PAGE_OBJECT_METHOD_MISSING);
    }

    @Test void explicitWrongMethodSignatureDoesNotFallBackToNameOnlyMatching() throws Exception {
        TestPlan plan=plan("Submit invalid credentials and verify that the error is visible");
        PageObjectCapabilityMissing refusal=new PageObjectCapabilityMissing("login",
                "benchmark.LoginPage.invalidPasswordShowsExpectedError(String)","untrusted reason");
        AtomicInteger implementerCalls=new AtomicInteger();
        AgentExecutor capturing=counting(scripted(plan,refusal),implementerCalls);
        var gateway=gateway(capturing,capabilityContext(),"fp",new AtomicInteger());
        gateway.prepare("run-wrong-signature",request(),capabilityContext());
        gateway.generatePlan("run-wrong-signature","requirement");

        PageObjectCapabilityException failure=assertThrows(PageObjectCapabilityException.class,
                ()->gateway.generateImplementation("run-wrong-signature"));

        assertEquals(PageObjectCapabilityException.RefusalCategory.PAGE_OBJECT_METHOD_MISSING,
                failure.refusalCategory());
        assertEquals(1,implementerCalls.get());
    }

    @Test void refusalCategoriesDistinguishConstructionBindingAssertionIncompleteAndStaleContext() throws Exception {
        assertRefusalCategory("constructor",capabilityContextWithoutSupportedConstructor(),"fp",
                "construct benchmark.LoginPage",PageObjectCapabilityException.RefusalCategory.PAGE_OBJECT_CONSTRUCTION_MISSING,
                true,"login");
        assertRefusalCategory("binding",capabilityContext(),"fp",
                "browser session binding for benchmark.LoginPage",PageObjectCapabilityException.RefusalCategory.BROWSER_SESSION_BINDING_MISSING,
                true,"login");
        assertRefusalCategory("assertion",capabilityContext(),"fp",
                "assert recordAttempt",PageObjectCapabilityException.RefusalCategory.ASSERTION_INCOMPATIBLE,
                true,"login");
        assertRefusalCategory("incomplete",capabilityContext(AgentContextPack.ManifestCompleteness.PARTIAL),"fp",
                "benchmark.LoginPage.submitMissingOperation(String)",PageObjectCapabilityException.RefusalCategory.CONTEXT_INCOMPLETE,
                true,null);
        assertRefusalCategory("stale",capabilityContext(),"new-fingerprint",
                "benchmark.LoginPage.submitMissingOperation(String)",PageObjectCapabilityException.RefusalCategory.CONTEXT_INCOMPLETE,
                false,null);
    }

    @Test void voidMethodUsedAsAssertTrueIsRejectedBeforeImplementer() throws Exception {
        TestPlan plan=plan(List.of("loginPage.recordAttempt()","assertTrue(loginPage.recordAttempt())"));
        AtomicInteger implementerCalls=new AtomicInteger();
        ScriptedAgentExecutor delegate=scripted(plan);
        AgentExecutor capturing=command->{
            if(command.role()==AgentExecutor.Role.TEST_IMPLEMENTER)implementerCalls.incrementAndGet();
            return delegate.execute(command);
        };
        var gateway=new ReviewableCoordinatorWorkflowGateway(capturing,replay->coordinator(replay,new AtomicInteger()));
        gateway.prepare("run-invalid-plan",request(),capabilityContext());
        gateway.generatePlan("run-invalid-plan","requirement");

        PageObjectCapabilityException failure=assertThrows(PageObjectCapabilityException.class,
                ()->gateway.generateImplementation("run-invalid-plan"));

        assertEquals("PLAN_PAGE_OBJECT_INCOMPATIBLE",failure.reasonCode());
        assertEquals("$.plan.scenarios",failure.location());
        assertEquals(0,implementerCalls.get());
    }

    @Test void aKnownMethodDoesNotVerifyAPlanThatAlsoCallsInventedApi() throws Exception {
        TestPlan plan=plan(List.of("loginPage.invalidPasswordShowsExpectedError(\"user\", \"bad\")",
                "loginPage.inventedSuccess()"));
        AtomicInteger implementerCalls=new AtomicInteger();
        ScriptedAgentExecutor delegate=scripted(plan);
        AgentExecutor capturing=command->{
            if(command.role()==AgentExecutor.Role.TEST_IMPLEMENTER)implementerCalls.incrementAndGet();
            return delegate.execute(command);
        };
        var gateway=new ReviewableCoordinatorWorkflowGateway(capturing,replay->coordinator(replay,new AtomicInteger()));
        gateway.prepare("run-invented-plan",request(),capabilityContext());
        gateway.generatePlan("run-invented-plan","requirement");

        PageObjectCapabilityException failure=assertThrows(PageObjectCapabilityException.class,
                ()->gateway.generateImplementation("run-invented-plan"));

        assertEquals("PLAN_PAGE_OBJECT_INCOMPATIBLE",failure.reasonCode());
        assertEquals(0,implementerCalls.get());
    }

    @Test void wrongArgumentCountIsRejectedBeforeImplementerAndCannotTriggerRetry() throws Exception {
        TestPlan plan=plan("loginPage.invalidPasswordShowsExpectedError(\"only-one\")");
        AtomicInteger implementerCalls=new AtomicInteger();
        ScriptedAgentExecutor delegate=scripted(plan);
        AgentExecutor capturing=command->{
            if(command.role()==AgentExecutor.Role.TEST_IMPLEMENTER)implementerCalls.incrementAndGet();
            return delegate.execute(command);
        };
        var gateway=new ReviewableCoordinatorWorkflowGateway(capturing,replay->coordinator(replay,new AtomicInteger()));
        gateway.prepare("run-wrong-arity",request(),capabilityContext());
        gateway.generatePlan("run-wrong-arity","requirement");

        PageObjectCapabilityException failure=assertThrows(PageObjectCapabilityException.class,
                ()->gateway.generateImplementation("run-wrong-arity"));

        assertEquals("PLAN_PAGE_OBJECT_INCOMPATIBLE",failure.reasonCode());
        assertEquals(0,implementerCalls.get());
    }

    @Test void implementerExtensionProposalKeepsItsOriginalDiagnosticTypeAndCannotRun() throws Exception {
        AtomicInteger coordinatorCreations=new AtomicInteger();
        PageObjectCapabilityMissing missing=new PageObjectCapabilityMissing("login","submit login",
                "No public submit operation is present in the verified manifest");
        PageObjectExtensionProposal extension=new PageObjectExtensionProposal(missing,"LoginPageExtension",
                "public void submitLogin()",List.of("verified manifest"));
        var gateway=new ReviewableCoordinatorWorkflowGateway(scripted(new TestPlan(ready(),List.of()),extension),
                replay->{coordinatorCreations.incrementAndGet();return coordinator(replay,new AtomicInteger());});
        gateway.prepare("run-extension",request(),context());gateway.generatePlan("run-extension","requirement");

        PageObjectCapabilityException failure=assertThrows(PageObjectCapabilityException.class,
                ()->gateway.generateImplementation("run-extension"));

        assertEquals("PAGE_OBJECT_EXTENSION_PROPOSAL",failure.reasonCode());
        assertEquals("$.payload.missing.capability",failure.location());
        assertThrows(IllegalStateException.class,()->gateway.run("run-extension"));
        assertEquals(0,coordinatorCreations.get(),"an unapproved extension proposal must not compile or execute");
    }


    @Test void reviewStagesDoNotCompileOrExecuteAndRunReusesReviewedArtifacts() throws Exception {
        AtomicInteger executions = new AtomicInteger();
        TestPlan plan = new TestPlan(ready(), List.of());
        TestImplementationProposal implementation = new TestImplementationProposal(ready(), "invalid-password",
                VALID_PAGE_OBJECT_SOURCE,
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY, List.of(API_REFERENCE), List.of());
        ScriptedAgentExecutor agents = scripted(plan, implementation, review());
        var input = new CoordinatorWorkflowGateway.Input(request(), capabilityContext());
        ReviewableCoordinatorWorkflowGateway gateway = new ReviewableCoordinatorWorkflowGateway(agents,
                (replay,ignored) -> coordinator(replay, executions),
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ignored->Path.of("src/test/java/example/GeneratedTest.java"),proposal->{},()->"fp");
        gateway.prepare("run-1", input.request(), input.context());

        assertSame(plan, gateway.generatePlan("run-1", "Invalid password shows an error"));
        assertEquals(0, executions.get(), "plan review must not execute a test");
        assertEquals(implementation, gateway.generateImplementation("run-1"));
        assertFalse(gateway.implementationPolicy("run-1").isEmpty());
        assertTrue(gateway.implementationPolicy("run-1").stream().allMatch(StudioWorkflowGateway.PolicyCheck::passed));
        assertEquals(0, executions.get(), "implementation review must not execute a test");

        WorkflowReport report = gateway.run("run-1");

        assertEquals(TestEngineeringRun.State.SUCCESS, report.finalState());
        assertEquals(1, executions.get());
        assertEquals(0, agents.remaining(), "reviewed plan and implementation are replayed, not regenerated");
        assertNotNull(gateway.execution("run-1"));
    }

    @Test void runIsBlockedUntilImplementationWasReviewed() throws Exception {
        var gateway = new ReviewableCoordinatorWorkflowGateway(scripted(new TestPlan(ready(), List.of())),
                replay -> coordinator(replay, new AtomicInteger()));
        gateway.prepare("run-2", request(), context());
        gateway.generatePlan("run-2", "requirement");
        assertThrows(IllegalStateException.class, () -> gateway.run("run-2"));
    }

    @Test void deterministicPolicyBlocksExecutionBeforeCompilerOrBrowser() throws Exception {
        AtomicInteger executions=new AtomicInteger();
        TestImplementationProposal unsafe=new TestImplementationProposal(ready(),"unsafe",
                "class Unsafe { Object x = org.openqa.selenium.By.id(\"raw\"); void test() { "
                        + "new benchmark.LoginPage().invalidPasswordShowsExpectedError(\"u\", \"p\"); } }",
                TestImplementationProposal.SelectorAccessPolicy.RAW_SELECTORS_EXPLICITLY_ALLOWED,List.of(API_REFERENCE),List.of());
        var gateway=new ReviewableCoordinatorWorkflowGateway(scripted(new TestPlan(ready(),List.of()),unsafe),
                (replay,ignored)->coordinator(replay,executions),
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ignored->Path.of("src/test/java/example/GeneratedTest.java"),proposal->{},()->"fp");
        gateway.prepare("run-policy",request(),capabilityContext());gateway.generatePlan("run-policy","requirement");gateway.generateImplementation("run-policy");
        assertTrue(gateway.implementationPolicy("run-policy").stream().anyMatch(check->!check.passed()));
        assertThrows(IllegalStateException.class,()->gateway.run("run-policy"));
        assertEquals(0,executions.get());
    }

    @Test void repairRerunCompilesHandwrittenSourceThenUsesFreshCoordinatorAndArtifacts() throws Exception {
        AtomicInteger executions=new AtomicInteger(),repairCompiles=new AtomicInteger();
        TestPlan plan=new TestPlan(ready(),List.of());
        TestImplementationProposal implementation=new TestImplementationProposal(ready(),"invalid-password",
                VALID_PAGE_OBJECT_SOURCE,
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,List.of(API_REFERENCE),List.of());
        ScriptedAgentExecutor agents=scripted(plan,implementation,review(),review());
        var gateway=new ReviewableCoordinatorWorkflowGateway(agents,(replay,ignored)->coordinator(replay,executions),
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),ignored->Path.of("src/test/java/example/GeneratedTest.java"),
                proposal->{assertEquals("repair-1",proposal.proposalId());repairCompiles.incrementAndGet();},()->"fp");
        gateway.prepare("run-rerun",request(),capabilityContext());gateway.generatePlan("run-rerun","requirement");gateway.generateImplementation("run-rerun");gateway.run("run-rerun");
        WorkflowReport rerun=gateway.rerun("run-rerun",repair());
        assertEquals(TestEngineeringRun.State.SUCCESS,rerun.finalState());
        assertEquals(1,repairCompiles.get());assertEquals(2,executions.get());assertNotNull(gateway.execution("run-rerun"));
    }

    @Test void restoredReviewedArtifactsContinueWithoutCallingArchitectOrImplementer() throws Exception {
        AtomicInteger calls=new AtomicInteger(),executions=new AtomicInteger();TestPlan plan=new TestPlan(ready(),List.of());
        TestImplementationProposal implementation=new TestImplementationProposal(ready(),"restored",VALID_PAGE_OBJECT_SOURCE,TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,List.of(API_REFERENCE),List.of());
        AgentExecutor noGeneration=command->{if(command.role()==AgentExecutor.Role.TEST_ARCHITECT||command.role()==AgentExecutor.Role.TEST_IMPLEMENTER)calls.incrementAndGet();return scripted(review()).execute(command);};
        var gateway=new ReviewableCoordinatorWorkflowGateway(noGeneration,(replay,ignored)->coordinator(replay,executions),
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ignored->Path.of("src/test/java/example/GeneratedTest.java"),proposal->{},()->"fp");
        gateway.prepare("run-restored",request(),capabilityContext());gateway.restoreReviewedArtifacts("run-restored",plan,implementation);
        WorkflowReport report=gateway.run("run-restored");
        assertEquals(TestEngineeringRun.State.SUCCESS,report.finalState());assertEquals(0,calls.get());assertEquals(1,executions.get());
    }

    @Test void rejectedImplementationCannotCreateCoordinatorCompileOrExecute() throws Exception {
        AtomicInteger coordinatorCreations = new AtomicInteger();
        AtomicInteger executions = new AtomicInteger();
        String swallowed = "package example; public final class GeneratedTest { public static void execute() { "
                + "new benchmark.LoginPage().invalidPasswordShowsExpectedError(\"u\", \"p\"); "
                + "try { throw new AssertionError(); } catch (AssertionError ignored) { } } }";
        TestImplementationProposal implementation = new TestImplementationProposal(ready(), "unsafe", swallowed,
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY, List.of(API_REFERENCE), List.of());
        var gateway = new ReviewableCoordinatorWorkflowGateway(scripted(new TestPlan(ready(), List.of()), implementation),
                (replay,ignored) -> { coordinatorCreations.incrementAndGet(); return coordinator(replay, executions); },
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ignored->Path.of("src/test/java/example/GeneratedTest.java"),proposal->{},()->"fp");
        gateway.prepare("run-swallow", request(), capabilityContext());
        gateway.generatePlan("run-swallow", "requirement");
        gateway.generateImplementation("run-swallow");

        assertTrue(gateway.implementationPolicy("run-swallow").stream().anyMatch(check -> !check.passed()));
        assertThrows(IllegalStateException.class, () -> gateway.run("run-swallow"));
        assertEquals(0, coordinatorCreations.get(), "policy rejection must happen before compiler pipeline construction");
        assertEquals(0, executions.get());
    }

    @Test void externalImplementerSecurityBoundaryAllowsSafeSourceThroughAstCompilerAndExecutorBoundaries() throws Exception {
        AtomicInteger coordinatorCreations = new AtomicInteger();
        AtomicInteger executions = new AtomicInteger();
        AgentExecutor agents = externalImplementer("safe", RedactionPolicy.defaults());
        var gateway = new ReviewableCoordinatorWorkflowGateway(agents,
                (replay,ignored) -> { coordinatorCreations.incrementAndGet(); return coordinator(replay, executions); },
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ignored->Path.of("src/test/java/example/GeneratedTest.java"),proposal->{},()->"fp");
        gateway.prepare("run-external-safe", request(), capabilityContext());

        gateway.generatePlan("run-external-safe", "Invalid password shows an error");
        TestImplementationProposal implementation = gateway.generateImplementation("run-external-safe");

        assertTrue(implementation.sourcePatch().contains("String inputValue"));
        assertEquals(List.of("benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)"),
                implementation.pageObjectApisUsed());
        assertTrue(gateway.implementationPolicy("run-external-safe").stream()
                .allMatch(StudioWorkflowGateway.PolicyCheck::passed), "the decoded source must pass the real AST gate");
        WorkflowReport report = gateway.run("run-external-safe");
        assertEquals(TestEngineeringRun.State.SUCCESS, report.finalState());
        assertEquals(1, coordinatorCreations.get(), "a safe reviewed artifact reaches compilation");
        assertEquals(1, executions.get(), "a safe compiled artifact reaches the executor boundary");
    }

    @Test void externalLegacyEquivalentReferenceIsCanonicalizedBeforeTheAstBoundary() throws Exception {
        AtomicInteger coordinatorCreations = new AtomicInteger();
        AtomicInteger executions = new AtomicInteger();
        AgentExecutor agents = externalImplementer("legacy-equivalent", RedactionPolicy.defaults());
        var gateway = new ReviewableCoordinatorWorkflowGateway(agents,
                (replay,ignored) -> { coordinatorCreations.incrementAndGet(); return coordinator(replay, executions); },
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ignored -> Path.of("src/test/java/example/GeneratedTest.java"), proposal -> { }, () -> "fp");
        gateway.prepare("run-external-legacy", request(), capabilityContext());
        gateway.generatePlan("run-external-legacy", "Invalid password shows an error");

        TestImplementationProposal implementation = gateway.generateImplementation("run-external-legacy");

        assertEquals(List.of(API_REFERENCE), implementation.pageObjectApisUsed());
        assertFalse(gateway.implementationPolicy("run-external-legacy").isEmpty(),
                "accepted metadata must reach the deterministic AST policy boundary");
        assertTrue(gateway.implementationPolicy("run-external-legacy").stream()
                .allMatch(StudioWorkflowGateway.PolicyCheck::passed));
        assertEquals(0, coordinatorCreations.get(), "generation alone must not construct the compiler pipeline");
        assertEquals(0, executions.get());
    }

    @Test void emptyAndInvalidAgentDeclarationsAreReplacedByVerifiedSourceUsageBeforeAst() throws Exception {
        for (String mode : List.of("empty-references", "invalid-reference")) {
            AtomicInteger coordinatorCreations = new AtomicInteger();
            AtomicInteger executions = new AtomicInteger();
            var gateway = new ReviewableCoordinatorWorkflowGateway(
                    externalImplementer(mode, RedactionPolicy.defaults()),
                    (replay,ignored) -> { coordinatorCreations.incrementAndGet(); return coordinator(replay, executions); },
                    new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                    ignored -> Path.of("src/test/java/example/GeneratedTest.java"), proposal -> { }, () -> "fp");
            String runId = "run-source-authoritative-" + mode;
            gateway.prepare(runId, request(), capabilityContext());
            gateway.generatePlan(runId, "Invalid password shows an error");

            TestImplementationProposal implementation = gateway.generateImplementation(runId);

            assertEquals(List.of(API_REFERENCE), implementation.pageObjectApisUsed(), mode);
            assertFalse(gateway.implementationPolicy(runId).isEmpty(), mode);
            assertTrue(gateway.implementationPolicy(runId).stream()
                    .allMatch(StudioWorkflowGateway.PolicyCheck::passed), mode);
            assertEquals(0, coordinatorCreations.get(), mode);
            assertEquals(0, executions.get(), mode);
        }
    }

    @Test void malformedImplementerReferenceShapesStopBeforeAstCompilerExecutionAndWrite() throws Exception {
        record Case(String mode, String reason, String location) { }
        List<Case> cases = List.of(
                new Case("missing-references", "REQUIRED_FIELD_MISSING", "$.result.payload.pageObjectApisUsed"),
                new Case("reference-string", "FIELD_TYPE_INVALID", "$.result.payload.pageObjectApisUsed"),
                new Case("reference-number", "ARRAY_ELEMENT_INVALID", "$.result.payload.pageObjectApisUsed[0]"),
                new Case("result-payload-mismatch", "RESULT_VARIANT_INVALID", "$.result.payload"),
                new Case("mixed-envelope", "ROOT_STRUCTURE_INVALID", "$"),
                new Case("capability-number", "FIELD_TYPE_INVALID", "$.result.payload.capability"),
                new Case("extension-evidence-number", "ARRAY_ELEMENT_INVALID", "$.result.payload.evidence[0]"),
                new Case("blank-reference", "FIELD_TYPE_INVALID",
                        "$.result.payload.pageObjectApisUsed[0]"),
                new Case("source-missing-method", PageObjectApiReferenceValidator.METHOD_NOT_IN_MANIFEST,
                        "$.result.payload.sourcePatch"),
                new Case("source-wrong-signature", PageObjectApiReferenceValidator.METHOD_SIGNATURE_MISMATCH,
                        "$.result.payload.sourcePatch"),
                new Case("source-unresolved-receiver", PageObjectApiReferenceValidator.RECEIVER_UNRESOLVED,
                        "$.result.payload.sourcePatch"),
                new Case("source-parse-failure", PageObjectApiReferenceValidator.SOURCE_PARSE_FAILED,
                        "$.result.payload.sourcePatch"));
        for (Case value : cases) {
            AtomicInteger coordinatorCreations = new AtomicInteger();
            AtomicInteger executions = new AtomicInteger();
            var gateway = new ReviewableCoordinatorWorkflowGateway(
                    externalImplementer(value.mode(), RedactionPolicy.defaults()),
                    (replay,ignored) -> { coordinatorCreations.incrementAndGet(); return coordinator(replay, executions); },
                    new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                    ignored -> Path.of("src/test/java/example/GeneratedTest.java"), proposal -> { }, () -> "fp");
            String runId = "run-external-rejected-" + value.mode();
            gateway.prepare(runId, request(), capabilityContext());
            gateway.generatePlan(runId, "Invalid password shows an error");

            AgentExecutor.AgentExecutionException failure = assertThrows(AgentExecutor.AgentExecutionException.class,
                    () -> gateway.generateImplementation(runId), value.mode());

            assertEquals(AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID, failure.code(), value.mode());
            assertEquals(value.reason(), failure.diagnostic().reasonCode(), value.mode());
            assertEquals(value.location(), failure.diagnostic().location(), value.mode());
            if(PageObjectApiReferenceValidator.SOURCE_REJECTION_CODES.contains(value.reason())){
                assertEquals("SOURCE_API_VERIFICATION",
                        failure.diagnostic().metadata().get("validationStage"),value.mode());
                assertNotEquals("UNKNOWN",failure.diagnostic().metadata().get("receiverCategory"),value.mode());
                assertNotEquals("UNKNOWN",failure.diagnostic().metadata().get("signatureStatus"),value.mode());
            }
            assertTrue(gateway.implementationPolicy(runId).isEmpty(),
                    "rejected output must not reach the AST policy boundary: " + value.mode());
            assertThrows(IllegalStateException.class, () -> gateway.run(runId));
            assertEquals(0, coordinatorCreations.get(), value.mode());
            assertEquals(0, executions.get(), value.mode());
            try (var files = Files.walk(temporary)) {
                assertTrue(files.noneMatch(path -> path.getFileName().toString().equals("GeneratedTest.java")),
                        "rejected output must not be written: " + value.mode());
            }
        }
    }

    @Test void externalImplementerJwtInPageObjectMetadataStopsBeforeAstCompileAndExecute() throws Exception {
        AtomicInteger coordinatorCreations = new AtomicInteger();
        AtomicInteger executions = new AtomicInteger();
        AgentExecutor agents = externalImplementer("jwt-metadata", RedactionPolicy.defaults());
        var gateway = new ReviewableCoordinatorWorkflowGateway(agents,
                replay -> { coordinatorCreations.incrementAndGet(); return coordinator(replay, executions); });
        gateway.prepare("run-external-jwt", request(), capabilityContext());
        gateway.generatePlan("run-external-jwt", "Invalid password shows an error");

        AgentExecutor.AgentExecutionException failure = assertThrows(AgentExecutor.AgentExecutionException.class,
                () -> gateway.generateImplementation("run-external-jwt"));

        assertEquals(AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID, failure.code());
        assertEquals("JWT", failure.diagnostic().reasonCode());
        assertEquals("$.result.payload.pageObjectApisUsed[item]", failure.diagnostic().location());
        assertTrue(gateway.implementationPolicy("run-external-jwt").isEmpty(),
                "JWT rejection must happen before the AST policy checks");
        assertThrows(IllegalStateException.class, () -> gateway.run("run-external-jwt"));
        assertEquals(0, coordinatorCreations.get());
        assertEquals(0, executions.get());
    }

    @Test void externalImplementerSecretIsRejectedBeforeAstCompileOrExecuteWithOriginalDiagnostic() throws Exception {
        AtomicInteger coordinatorCreations = new AtomicInteger();
        AtomicInteger executions = new AtomicInteger();
        AgentExecutor agents = externalImplementer("secret",
                RedactionPolicy.builder().secret("configured-secret-canary").build());
        var gateway = new ReviewableCoordinatorWorkflowGateway(agents,
                replay -> { coordinatorCreations.incrementAndGet(); return coordinator(replay, executions); });
        gateway.prepare("run-external-secret", request(), context());
        gateway.generatePlan("run-external-secret", "Invalid password shows an error");

        AgentExecutor.AgentExecutionException failure = assertThrows(AgentExecutor.AgentExecutionException.class,
                () -> gateway.generateImplementation("run-external-secret"));

        assertEquals(AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID, failure.code());
        assertEquals("LITERAL_SECRET", failure.diagnostic().reasonCode());
        assertEquals("$.result.payload.sourcePatch", failure.diagnostic().location());
        assertFalse(failure.getMessage().contains("configured-secret-canary"));
        assertTrue(gateway.implementationPolicy("run-external-secret").isEmpty(),
                "security rejection must happen before AST policy checks");
        assertThrows(IllegalStateException.class, () -> gateway.run("run-external-secret"));
        assertEquals(0, coordinatorCreations.get(), "security rejection must happen before compiler construction");
        assertEquals(0, executions.get(), "security rejection must happen before execution");
    }

    @Test void anyProviderInvalidMetadataIsReplacedByVerifiedSourceUsageBeforeAst() throws Exception {
        AtomicInteger coordinatorCreations=new AtomicInteger(),executions=new AtomicInteger();
        TestImplementationProposal invalid=new TestImplementationProposal(ready(),"invalid-api",
                VALID_PAGE_OBJECT_SOURCE,
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,
                List.of("benchmark.LoginPage.methodThatDoesNotExist(String,String)"),List.of());
        var gateway=new ReviewableCoordinatorWorkflowGateway(scripted(new TestPlan(ready(),List.of()),invalid),
                (replay,ignored)->{coordinatorCreations.incrementAndGet();return coordinator(replay,executions);},
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ignored->Path.of("src/test/java/example/GeneratedTest.java"),proposal->{},()->"fp");
        gateway.prepare("run-provider-invalid",request(),capabilityContext());
        gateway.generatePlan("run-provider-invalid","requirement");

        TestImplementationProposal result=gateway.generateImplementation("run-provider-invalid");

        assertEquals(List.of(API_REFERENCE),result.pageObjectApisUsed());
        assertFalse(gateway.implementationPolicy("run-provider-invalid").isEmpty());
        assertTrue(gateway.implementationPolicy("run-provider-invalid").stream()
                .allMatch(StudioWorkflowGateway.PolicyCheck::passed));
        assertEquals(0,coordinatorCreations.get());
        assertEquals(0,executions.get());
    }

    @Test void sourceFingerprintIsRevalidatedImmediatelyBeforeRun() throws Exception {
        AtomicInteger coordinatorCreations=new AtomicInteger(),executions=new AtomicInteger();
        AtomicReference<String> currentFingerprint=new AtomicReference<>("fp");
        TestImplementationProposal implementation=new TestImplementationProposal(ready(),"toctou",
                VALID_PAGE_OBJECT_SOURCE,
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,
                List.of(API_REFERENCE),List.of());
        var gateway=new ReviewableCoordinatorWorkflowGateway(
                scripted(new TestPlan(ready(),List.of()),implementation,review()),
                (replay,ignored)->{coordinatorCreations.incrementAndGet();return coordinator(replay,executions);},
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ignored->Path.of("src/test/java/example/GeneratedTest.java"),proposal->{},currentFingerprint::get);
        gateway.prepare("run-toctou",request(),capabilityContext());
        gateway.generatePlan("run-toctou","requirement");
        gateway.generateImplementation("run-toctou");
        currentFingerprint.set("changed-after-human-review");

        PageObjectCapabilityException failure=assertThrows(PageObjectCapabilityException.class,
                ()->gateway.run("run-toctou"));

        assertEquals("PAGE_OBJECT_MANIFEST_STALE",failure.reasonCode());
        assertEquals(0,coordinatorCreations.get());
        assertEquals(0,executions.get());
    }

    @Test void restoredImplementationIsRevalidatedAndBlockedBeforeRerunRepairCompiler() throws Exception {
        AtomicInteger coordinatorCreations = new AtomicInteger();
        AtomicInteger repairCompiles = new AtomicInteger();
        AtomicInteger executions = new AtomicInteger();
        TestPlan plan = new TestPlan(ready(), List.of());
        TestImplementationProposal unsafe = new TestImplementationProposal(ready(), "restored-unsafe",
                "package example; public final class GeneratedTest { void test() { "
                        + "new benchmark.LoginPage().invalidPasswordShowsExpectedError(\"fixture-user\", \"fixture-invalid\"); "
                        + "try { throw new AssertionError(); } catch (Throwable ignored) { } } }",
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,
                List.of(API_REFERENCE), List.of());
        var gateway = new ReviewableCoordinatorWorkflowGateway(scripted(review()),
                (replay,ignored) -> { coordinatorCreations.incrementAndGet(); return coordinator(replay, executions); },
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ignored -> Path.of("src/test/java/example/GeneratedTest.java"),
                proposal -> repairCompiles.incrementAndGet(),()->"fp");
        gateway.prepare("run-restored-unsafe", request(), capabilityContext());
        gateway.restoreReviewedArtifacts("run-restored-unsafe", plan, unsafe);

        assertTrue(gateway.implementationPolicy("run-restored-unsafe").stream().anyMatch(check -> !check.passed()));
        assertThrows(IllegalStateException.class, () -> gateway.run("run-restored-unsafe"));
        assertThrows(IllegalStateException.class, () -> gateway.rerun("run-restored-unsafe", repair()));
        assertEquals(0, coordinatorCreations.get());
        assertEquals(0, repairCompiles.get(), "a blocked reviewed implementation cannot reach repair compilation");
        assertEquals(0, executions.get());
    }

    @Test void restoreUsesVerifiedSourceInsteadOfDeclarationsButStillRejectsStaleManifest() {
        record Case(String id,String reference) { }
        List<Case> cases=List.of(
                new Case("syntax","benchmark.LoginPage::invalidPasswordShowsExpectedError(String,String)"),
                new Case("missing","benchmark.LoginPage.notPresent(String,String)"),
                new Case("arity","benchmark.LoginPage.invalidPasswordShowsExpectedError(String)"));
        for(Case value:cases){
            AtomicInteger coordinatorCreations=new AtomicInteger(),executions=new AtomicInteger();
            TestImplementationProposal implementation=new TestImplementationProposal(ready(),value.id(),
                    "package example; public final class GeneratedTest { public static void execute() {} }",
                    TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,
                    List.of(value.reference()),List.of());
            var gateway=new ReviewableCoordinatorWorkflowGateway(scripted(review()),
                    (replay,ignored)->{coordinatorCreations.incrementAndGet();return coordinator(replay,executions);},
                    new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                    ignored->Path.of("src/test/java/example/GeneratedTest.java"),proposal->{},()->"fp");
            gateway.prepare("restore-"+value.id(),request(),capabilityContext());

            gateway.restoreReviewedArtifacts("restore-"+value.id(),new TestPlan(ready(),List.of()),implementation);

            assertEquals(0,coordinatorCreations.get());
            assertEquals(0,executions.get());
        }

        AtomicInteger coordinatorCreations=new AtomicInteger(),executions=new AtomicInteger();
        TestImplementationProposal implementation=new TestImplementationProposal(ready(),"stale",
                VALID_PAGE_OBJECT_SOURCE,TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,
                List.of(API_REFERENCE),List.of());
        var staleGateway=new ReviewableCoordinatorWorkflowGateway(scripted(review()),
                (replay,ignored)->{coordinatorCreations.incrementAndGet();return coordinator(replay,executions);},
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ignored->Path.of("src/test/java/example/GeneratedTest.java"),proposal->{},()->"stale");
        staleGateway.prepare("restore-stale",request(),capabilityContext());
        PageObjectCapabilityException failure=assertThrows(PageObjectCapabilityException.class,
                ()->staleGateway.restoreReviewedArtifacts("restore-stale",new TestPlan(ready(),List.of()),implementation));
        assertEquals(PageObjectApiReferenceValidator.MANIFEST_STALE,failure.reasonCode());
        assertEquals("$.payload.sourcePatch",failure.location());
        assertEquals(0,coordinatorCreations.get());
        assertEquals(0,executions.get());
    }

    private static AgentWorkflowCoordinator coordinator(AgentExecutor agents, AtomicInteger executions) {
        return new AgentWorkflowCoordinator(new TestEngineeringWorkflow(WorkflowPolicy.defaults()), agents,
                new TargetedJavaCompiler(), (request, output) -> {
                    executions.incrementAndGet();
                    return new TargetedTestExecutor.ExecutionResult(true, 1, List.of("Lens PASS"));
                }, new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                execution -> new FailureClassification(ready(), FailureClassification.Category.UNKNOWN,
                        "unclassified", execution.traceEvidenceRefs(), List.of(), List.of()),
                AgentWorkflowCoordinator.Stabilizer.none(),
                new AgentWorkflowCoordinator.Configuration(Path.of("src/test/java/example/GeneratedTest.java"),
                        "example.GeneratedTest", null, 17, System.getProperty("java.class.path"), 10, Duration.ofSeconds(5),
                        List.of("example.GeneratedTest"), 2), () -> "fp");
    }

    private static ReviewableCoordinatorWorkflowGateway gateway(AgentExecutor agents,AgentContextPack context,
            String currentFingerprint,AtomicInteger coordinatorCreations) {
        var gateway=new ReviewableCoordinatorWorkflowGateway(agents,
                (replay,ignored)->{coordinatorCreations.incrementAndGet();return coordinator(replay,new AtomicInteger());},
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ignored->Path.of("src/test/java/GeneratedTest.java"),proposal->{},()->currentFingerprint);
        gateway.prepare("run-natural-method",request(),context);
        gateway.prepare("run-natural-constructor",request(),context);
        gateway.prepare("run-real-missing",request(),context);
        return gateway;
    }

    private static void assertRefusalCategory(String id,AgentContextPack context,String currentFingerprint,
            String capability,PageObjectCapabilityException.RefusalCategory expected,boolean fingerprintMatched,
            String verifiedPageId) throws Exception {
        String runId="run-category-"+id;
        PageObjectCapabilityMissing refusal=new PageObjectCapabilityMissing("login",capability,
                "untrusted classifier reason");
        AtomicInteger coordinatorCreations=new AtomicInteger();
        var gateway=new ReviewableCoordinatorWorkflowGateway(scripted(plan("Describe the expected login outcome"),refusal),
                (replay,ignored)->{coordinatorCreations.incrementAndGet();return coordinator(replay,new AtomicInteger());},
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ignored->Path.of("src/test/java/GeneratedTest.java"),proposal->{},()->currentFingerprint);
        gateway.prepare(runId,request(),context);gateway.generatePlan(runId,"requirement");

        PageObjectCapabilityException failure=assertThrows(PageObjectCapabilityException.class,
                ()->gateway.generateImplementation(runId));

        assertEquals(expected,failure.refusalCategory(),id);
        assertEquals(verifiedPageId,failure.verifiedPageId(),id);
        assertEquals(fingerprintMatched,failure.sourceFingerprintMatched(),id);
        assertEquals(0,coordinatorCreations.get(),id+" refusal must not create the compiler pipeline");
    }

    private static void assertVerifiedPlanRefusalIsNotRetried(String pageId,String capability,
            AgentContextPack context,String currentFingerprint,
            PageObjectCapabilityException.RefusalCategory expected) throws Exception {
        String runId="run-unverified-refusal-"+expected.name().toLowerCase(java.util.Locale.ROOT)+"-"+pageId;
        TestPlan plan=plan("assertTrue(loginPage.invalidPasswordShowsExpectedError(\"user\", \"bad\"))");
        PageObjectCapabilityMissing refusal=new PageObjectCapabilityMissing(pageId,capability,"untrusted reason");
        AtomicInteger implementerCalls=new AtomicInteger();
        AgentExecutor agents=counting(scripted(plan,refusal),implementerCalls);
        var gateway=new ReviewableCoordinatorWorkflowGateway(agents,
                (replay,ignored)->coordinator(replay,new AtomicInteger()),
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ignored->Path.of("src/test/java/GeneratedTest.java"),proposal->{},()->currentFingerprint);
        gateway.prepare(runId,request(),context);gateway.generatePlan(runId,"requirement");

        PageObjectCapabilityException failure=assertThrows(PageObjectCapabilityException.class,
                ()->gateway.generateImplementation(runId));

        assertEquals(expected,failure.refusalCategory());
        assertEquals(1,implementerCalls.get(),"a VERIFIED plan must not validate an unrelated refusal");
    }

    private static AgentExecutor counting(AgentExecutor delegate,AtomicInteger implementerCalls) {
        return command->{
            if(command.role()==AgentExecutor.Role.TEST_IMPLEMENTER)implementerCalls.incrementAndGet();
            return delegate.execute(command);
        };
    }

    private static ScriptedAgentExecutor scripted(Object... payloads) {
        List<AgentExecutor.AgentResult> results = new ArrayList<>();
        for (int i = 0; i < payloads.length; i++) results.add(new AgentExecutor.AgentResult("scripted-" + i,
                ArtifactEnvelope.create("artifact-" + i, "fixture", List.of(), 1, payloads[i])));
        return new ScriptedAgentExecutor(results);
    }
    private AgentExecutor externalImplementer(String mode, RedactionPolicy redaction) {
        AgentProfile profile = new AgentProfile("studio-security-fixture", javaExecutable(),
                List.of("-cp", absoluteClasspath(), StudioExternalAgentFixtureMain.class.getName(), mode),
                AgentExecutor.Role.TEST_IMPLEMENTER, Duration.ofSeconds(10), 8_192, 4_096, 512,
                Set.of(), AgentProfile.OutputTransport.STDOUT);
        ExternalAgentRunner runner = new ExternalAgentRunner(profile, temporary.toAbsolutePath(),
                new AgentProtocolCodec(), redaction, List.of(), List.of(), Map.of());
        ScriptedAgentExecutor architectAndReviewer = scripted(new TestPlan(ready(), List.of()), review());
        return new RoleDispatchingAgentExecutor(Map.of(
                AgentExecutor.Role.TEST_ARCHITECT, architectAndReviewer,
                AgentExecutor.Role.TEST_IMPLEMENTER, runner,
                AgentExecutor.Role.CODE_REVIEWER, architectAndReviewer));
    }
    private static Path javaExecutable() {
        String name = System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", name).toAbsolutePath();
    }
    private static String absoluteClasspath() {
        Path base = Path.of("").toAbsolutePath();
        return Arrays.stream(System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator)))
                .map(Path::of).map(path -> path.isAbsolute() ? path : base.resolve(path).normalize())
                .map(Path::toString).collect(java.util.stream.Collectors.joining(File.pathSeparator));
    }
    private static TestEngineeringRequest request() {
        return new TestEngineeringRequest(new TestEngineeringRequest.Requirement("Invalid password shows an error", List.of("error visible")),
                new TestEngineeringRequest.Scope(List.of("login"), List.of()),
                new TestEngineeringRequest.Framework("JUnit", "5", "junit-jupiter"),
                new TestEngineeringRequest.Target("app", "GeneratedTest", "invalid-password"),
                List.of("src/test/java"), TestEngineeringRequest.ExecutionPolicy.TARGETED_AFTER_COMPILE);
    }
    private static AgentContextPack context() {
        return new AgentContextPack(ready(), new AgentTask(ready(), AgentTask.TaskType.CREATE_TEST,
                "Invalid password shows an error", List.of("login"), List.of("login-error"),
                List.of("login-submit"), List.of("Page Objects only")), List.of(),
                Map.of("LoginPage", List.of("login(String,String)", "errorVisible()")),
                List.of("JUnit 5"), List.of(), List.of(), AgentContextPack.Completeness.COMPLETE_FOR_REQUESTED_SCOPE);
    }
    private static AgentContextPack capabilityContext() {
        return capabilityContext(AgentContextPack.ManifestCompleteness.COMPLETE);
    }
    private static AgentContextPack capabilityContext(AgentContextPack.ManifestCompleteness completeness) {
        AgentContextPack.PageObjectCapability capability=new AgentContextPack.PageObjectCapability(
                "login-page","login","benchmark.LoginPage","src/test/java/benchmark/LoginPage.java","PUBLIC",
                List.of(new AgentContextPack.ConstructorCapability("LoginPage()",List.of(),"PUBLIC","DIRECT_CONSTRUCTION",false)),
                List.of(
                        new AgentContextPack.MethodCapability("invalid","invalidPasswordShowsExpectedError",
                                "invalidPasswordShowsExpectedError(String,String)",List.of(
                                new AgentContextPack.CapabilityParameter("user","String"),
                                new AgentContextPack.CapabilityParameter("value","String")),"boolean",false,"ACTION",List.of()),
                        new AgentContextPack.MethodCapability("record","recordAttempt","recordAttempt()",List.of(),
                                "void",false,"ACTION",List.of())));
        return new AgentContextPack(ready(), new AgentTask(ready(), AgentTask.TaskType.CREATE_TEST,
                "Invalid password shows an error", List.of("login"), List.of(), List.of(), List.of("Page Objects only")),
                List.of(),Map.of(),List.of("JUnit 5"),AgentContextPack.SourceContext.empty(),
                new AgentContextPack.PageObjectCapabilityManifest("fp",completeness,
                        List.of(capability),List.of()),List.of(),List.of(),AgentContextPack.Completeness.PARTIAL);
    }
    private static AgentContextPack capabilityContextWithoutSupportedConstructor() {
        AgentContextPack.PageObjectCapability capability=new AgentContextPack.PageObjectCapability(
                "login-page","login","benchmark.LoginPage","src/test/java/benchmark/LoginPage.java","PUBLIC",
                List.of(new AgentContextPack.ConstructorCapability("LoginPage(String)",
                        List.of(new AgentContextPack.CapabilityParameter("value","String")),"PRIVATE","UNSUPPORTED",false)),
                List.of(new AgentContextPack.MethodCapability("invalid","invalidPasswordShowsExpectedError",
                        "invalidPasswordShowsExpectedError(String,String)",List.of(
                        new AgentContextPack.CapabilityParameter("user","String"),
                        new AgentContextPack.CapabilityParameter("value","String")),"boolean",false,"ACTION",List.of())));
        return new AgentContextPack(ready(),new AgentTask(ready(),AgentTask.TaskType.CREATE_TEST,
                "Invalid password shows an error",List.of("login"),List.of(),List.of(),List.of("Page Objects only")),
                List.of(),Map.of(),List.of("JUnit 5"),AgentContextPack.SourceContext.empty(),
                new AgentContextPack.PageObjectCapabilityManifest("fp",AgentContextPack.ManifestCompleteness.COMPLETE,
                        List.of(capability),List.of()),List.of(),List.of(),AgentContextPack.Completeness.PARTIAL);
    }
    private static TestPlan plan(String assertion) {
        return plan(List.of(assertion));
    }
    private static TestPlan plan(List<String> actions) {
        List<TestPlan.TestStep> steps=new ArrayList<>();
        for(int index=0;index<actions.size();index++)steps.add(new TestPlan.TestStep(index+1,actions.get(index),"login",null,null));
        TestPlan.TestScenario scenario=new TestPlan.TestScenario("invalid-password","Invalid password",
                TestPlan.Priority.HIGH,List.of(),steps,
                List.of(new TestPlan.ExpectedResult("login",null,actions.get(actions.size()-1),null)),List.of("login"),List.of(),
                List.of(),List.of(),List.of());
        return new TestPlan(ready(),List.of(scenario));
    }
    private static CodeReviewResult review(){return new CodeReviewResult(ready(),CodeReviewResult.Verdict.APPROVE,
            List.of("implementation-1","quality-1","compile-success-1","execution-1"),List.of());}
    private static RepairProposal repair(){return new RepairProposal(ready(),"repair-1",RepairProposal.ApplicationPolicy.PROPOSE_ONLY,
            "selector","validated","id:old","css:[data-testid='new']",List.of("SAME_TARGET"),List.of("STABLE"),List.of("InvalidLoginTest"));}
    private static ContractHeader ready() {
        return new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of("fixture"), List.of(), ContractHeader.Confidence.OBSERVED);
    }
}
