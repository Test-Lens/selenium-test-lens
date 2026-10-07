package example;

import io.github.testlens.application.tooling.ai.*;
import io.github.testlens.application.tooling.ai.workflow.*;
import io.github.testlens.application.tooling.ai.workflow.runner.provider.*;

import java.util.List;
import java.util.Map;

public final class CertificationAgentProvider implements AgentExecutorProvider {
    private static final ContractHeader READY=new ContractHeader(1,ContractHeader.Status.READY,List.of("scripted external certification"),List.of(),ContractHeader.Confidence.OBSERVED);
    @Override public ProviderAvailability availability(){return new ProviderAvailability(Status.AVAILABLE,"External scripted certification provider","scripted");}
    @Override public ProviderAvailability preflight(){return availability();}
    @Override public AgentExecutor executorFor(AgentExecutor.Role role,String profile){return command->{Object payload=switch(role){
        case TEST_ARCHITECT->new TestPlan(READY,List.of(new TestPlan.TestScenario("INVALID_PASSWORD","Invalid password",TestPlan.Priority.HIGH,List.of("Login page"),List.of(new TestPlan.TestStep(1,"LOGIN","LOGIN",null,"invalid-password")),List.of(new TestPlan.ExpectedResult("LOGIN",null,"Error visible",null)),List.of("LOGIN"),List.of(),List.of(),List.of("VALID_USER","INVALID_PASSWORD"),List.of())));
        case TEST_IMPLEMENTER->implementation(command.runId());
        case CODE_REVIEWER->new CodeReviewResult(READY,CodeReviewResult.Verdict.APPROVE,List.of("implementation"),List.of());
        default->throw new AgentExecutor.AgentExecutionException(AgentExecutor.AgentFailureCode.AGENT_PROCESS_FAILED,"Unexpected role "+role);
    };return new AgentExecutor.AgentResult("external-scripted",ArtifactEnvelope.create(role.name().toLowerCase(),command.runId(),List.of(),1,payload));};}
    private static TestImplementationProposal implementation(String runId){String name="GeneratedStudioTest"+runId.replaceAll("[^A-Za-z0-9]","");String source="""
            import example.LoginPage;
            import io.github.testlens.studio.browser.BrowserSessionContext;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertTrue;
            public final class %s {
              @Test void invalidPasswordShowsError(){
                LoginPage page=new LoginPage(BrowserSessionContext.currentDriver());
                assertTrue(page.loginExpectingFailure("test-user","invalid-password").errorVisible());
              }
            }
            """.formatted(name);return new TestImplementationProposal(READY,"INVALID_PASSWORD",source,TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,List.of("LoginPage.loginExpectingFailure","LoginPage.errorVisible"),List.of());}
}
