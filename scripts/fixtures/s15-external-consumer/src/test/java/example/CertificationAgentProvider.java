package example;

import io.github.testlens.application.tooling.ai.*;
import io.github.testlens.application.tooling.ai.workflow.*;
import io.github.testlens.application.tooling.ai.workflow.runner.*;
import io.github.testlens.application.tooling.ai.workflow.runner.provider.*;
import io.github.testlens.core.redaction.RedactionPolicy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;

public final class CertificationAgentProvider implements AgentExecutorProvider {
    private static final String REAL_MODE = "TEST_LENS_CERTIFICATION_REAL_CODEX";
    private static final String CODEX_EXECUTABLE = "TEST_LENS_CERTIFICATION_CODEX_EXECUTABLE";
    private static final String METRICS_FILE = "TEST_LENS_CERTIFICATION_METRICS_FILE";
    private static final String STAGING_ROOT = "TEST_LENS_CERTIFICATION_STAGING_ROOT";
    private static final ContractHeader READY=new ContractHeader(1,ContractHeader.Status.READY,List.of("scripted external certification"),List.of(),ContractHeader.Confidence.OBSERVED);
    private static final ContractHeader REVIEW_COMPLETED=new ContractHeader(1,ContractHeader.Status.COMPLETED,List.of("scripted external certification"),List.of(),ContractHeader.Confidence.OBSERVED);
    @Override public ProviderAvailability availability(){
        if(!realMode())return new ProviderAvailability(Status.AVAILABLE,"External scripted certification provider","scripted");
        try{requiredAbsolutePath(CODEX_EXECUTABLE,true);requiredAbsolutePath(METRICS_FILE,false);requiredAbsolutePath(STAGING_ROOT,false);return new ProviderAvailability(Status.AVAILABLE,"Real Codex certification provider","codex-cli");}
        catch(RuntimeException unavailable){return new ProviderAvailability(Status.NOT_AVAILABLE,"Real Codex certification configuration is incomplete",null);}
    }
    @Override public ProviderAvailability preflight(){return availability();}
    @Override public AgentExecutor executorFor(AgentExecutor.Role role,String profile){
        if(realMode()){
            ProviderAvailability availability=availability();
            if(!availability.available())throw new ProviderUnavailableException(availability);
            return realExecutor(role,profile);
        }
        return command->{Object payload=switch(role){
        case TEST_ARCHITECT->new TestPlan(READY,List.of(new TestPlan.TestScenario("INVALID_PASSWORD","Invalid password",TestPlan.Priority.HIGH,List.of("Login page"),List.of(new TestPlan.TestStep(1,"LOGIN","LOGIN",null,"invalid-password")),List.of(new TestPlan.ExpectedResult("LOGIN",null,"Error visible",null)),List.of("LOGIN"),List.of(),List.of(),List.of("VALID_USER","INVALID_PASSWORD"),List.of())));
        case TEST_IMPLEMENTER->implementation(command.runId());
        case CODE_REVIEWER->review(command);
        default->throw new AgentExecutor.AgentExecutionException(AgentExecutor.AgentFailureCode.AGENT_PROCESS_FAILED,"Unexpected role "+role);
        };return new AgentExecutor.AgentResult("external-scripted",ArtifactEnvelope.create(role.name().toLowerCase(),command.runId(),List.of(),1,payload));};
    }
    private static boolean realMode(){return "1".equals(System.getenv(REAL_MODE));}
    private static AgentExecutor realExecutor(AgentExecutor.Role role,String profile){
        Path executable=requiredAbsolutePath(CODEX_EXECUTABLE,true);
        Path metrics=requiredAbsolutePath(METRICS_FILE,false);
        Path staging=requiredAbsolutePath(STAGING_ROOT,false);
        var agentProfile=CodexCliProfiles.readOnly(executable,profile,role,Duration.ofMinutes(15),CodexCliProfiles.defaultEnvironmentAllowlist());
        return new ExternalAgentRunner(agentProfile,staging,new AgentProtocolCodec(),RedactionPolicy.defaults(),List.of(),List.of(),System.getenv(),value->appendMetric(metrics,value));
    }
    private static Path requiredAbsolutePath(String name,boolean mustExist){
        String value=System.getenv(name);
        if(value==null||value.isBlank())throw new IllegalStateException(name+" is required in real certification mode");
        Path path=Path.of(value).toAbsolutePath().normalize();
        if(!Path.of(value).isAbsolute()||(mustExist&&!Files.isRegularFile(path)))throw new IllegalStateException(name+" must name an absolute"+(mustExist?" existing file":" path"));
        return path;
    }
    private static synchronized void appendMetric(Path file,ExternalAgentRunner.ExecutionMetrics value){
        String line="{\"role\":\""+value.role().name()+"\",\"status\":\""+value.status().name()+"\",\"inputBytes\":"+value.inputBytes()+",\"outputBytes\":"+value.outputBytes()+",\"durationMillis\":"+value.durationMillis()+"}"+System.lineSeparator();
        try{Files.createDirectories(file.getParent());Files.writeString(file,line,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
        catch(Exception failure){throw new IllegalStateException("Unable to record sanitized certification metrics",failure);}
    }
    private static CodeReviewResult review(AgentExecutor.AgentCommand command){
        ReviewerEvidencePack evidence=command.inputs().stream().map(ArtifactEnvelope::payload)
                .filter(ReviewerEvidencePack.class::isInstance).map(ReviewerEvidencePack.class::cast)
                .findFirst().orElseThrow(()->new IllegalStateException("Reviewer evidence pack is required"));
        return new CodeReviewResult(REVIEW_COMPLETED,CodeReviewResult.Verdict.APPROVE,
                evidence.requiredReviewedArtifactRefs(),List.of());
    }
    private static TestImplementationProposal implementation(String runId){String name="GeneratedStudioTest"+runId.replaceAll("[^A-Za-z0-9]","");String source="""
            package benchmark;

            import benchmark.LoginPage;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertTrue;
            public final class %s {
              @Test void invalidPasswordShowsError(){
                assertTrue(new LoginPage().invalidPasswordShowsExpectedError("test-user","invalid-value"));
              }
            }
            """.formatted(name);return new TestImplementationProposal(READY,"INVALID_PASSWORD",source,TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,List.of("benchmark.LoginPage.invalidPasswordShowsExpectedError(String,String)"),List.of());}
}
