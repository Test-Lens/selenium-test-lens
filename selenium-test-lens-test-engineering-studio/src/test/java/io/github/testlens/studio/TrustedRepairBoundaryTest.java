package io.github.testlens.studio;

import io.github.testlens.application.tooling.ai.*;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrustedRepairBoundaryTest {
    @TempDir Path root;

    @Test void proposalAndRejectNeverModifySource() throws Exception {
        Fixture fixture=fixture();String before=Files.readString(fixture.source());
        var registered=fixture.service().registerRepairProposal(fixture.runId(),fixture.proposal());
        assertNull(registered.decision());assertEquals(before,Files.readString(fixture.source()));
        var rejected=fixture.service().rejectRepair(fixture.runId(),fixture.proposal().proposalId());
        assertEquals("REJECTED",rejected.decision());assertEquals(before,Files.readString(fixture.source()));
    }

    @Test void forgedStaleApprovalIsRejectedByTrustedApplierPreconditions() throws Exception {
        Fixture fixture=fixture();fixture.service().registerRepairProposal(fixture.runId(),fixture.proposal());
        Files.writeString(fixture.source(),Files.readString(fixture.source()).replace("old-login","changed-outside-studio"));
        var result=fixture.service().approveRepair(fixture.runId(),fixture.proposal().proposalId());
        assertEquals("APPROVE_FAILED",result.decision());
        assertEquals("SOURCE_PRECONDITION_FAILED",result.applyStatus());
        assertTrue(Files.readString(fixture.source()).contains("changed-outside-studio"));
        assertFalse(Files.readString(fixture.source()).contains("login-submit"));
    }

    @Test void explicitApprovalAloneInvokesTrustedApply() throws Exception {
        Fixture fixture=fixture();fixture.service().registerRepairProposal(fixture.runId(),fixture.proposal());
        var result=fixture.service().approveRepair(fixture.runId(),fixture.proposal().proposalId());
        assertEquals("APPROVED",result.decision());assertEquals("APPLIED",result.applyStatus());
        assertTrue(Files.readString(fixture.source()).contains("By.cssSelector(\"[data-testid='login-submit']\")"));
    }

    @Test void identicalProposalIdsRemainScopedToTheirWorkflowRun() throws Exception {
        Fixture fixture=fixture();String second=fixture.service().createRequirement("Locked LoginPage selector");
        fixture.service().registerRepairProposal(fixture.runId(),fixture.proposal());
        fixture.service().registerRepairProposal(second,fixture.proposal());
        fixture.service().rejectRepair(fixture.runId(),fixture.proposal().proposalId());
        assertEquals("REJECTED",fixture.service().workflow(fixture.runId()).repair().decision());
        assertNull(fixture.service().workflow(second).repair().decision());
    }

    private Fixture fixture() throws Exception {
        Path sourceRoot=root.resolve("src/test/java"),source=sourceRoot.resolve("LoginPage.java");Files.createDirectories(sourceRoot);
        Files.writeString(source,"import org.openqa.selenium.By; class LoginPage { private final By loginButton = By.id(\"old-login\"); }");
        var service=new TestEngineeringStudioService(new TestEngineeringStudioService.Configuration(root,List.of(sourceRoot),List.of(),List.of("src/test/java"),"fixture"),null,null);
        service.scanProject();
        ExistingProjectIndex index=new ExistingProjectIndexer().index(new ExistingProjectIndexer.Request(root,List.of(sourceRoot),List.of()));
        var element=index.elements().get(0);var owner=index.classes().stream().filter(v->v.id().equals(element.ownerClassId())).findFirst().orElseThrow();
        var file=index.sourceFiles().stream().filter(v->v.logicalPath().equals(owner.logicalPath())).findFirst().orElseThrow();
        var range=element.range();
        var target=new RepairProposal.SourceTarget(element.id(),element.declarationRef(),owner.logicalPath(),
                new RepairProposal.SourceRange(range.startLine(),range.startColumn(),range.endLine(),range.endColumn(),range.startOffset(),range.endOffsetExclusive()),
                file.contentFingerprint(),element.declarationFingerprint(),"EXACT","id","old-login");
        var replacement=new RepairProposal.SelectorEvidence("cssSelector","[data-testid='login-submit']","candidate-new","VERIFIED_IN_SCOPE","SAME_TARGET",true,List.of("STABLE"),"LIVE_CANDIDATE_ANALYSIS");
        var header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.READY,List.of("same target"),List.of(),ContractHeader.Confidence.LIVE_VALIDATED);
        var proposal=new RepairProposal(header,"repair-1",RepairProposal.ApplicationPolicy.PROPOSE_ONLY,"selector","old selector absent","id:old-login","cssSelector:[data-testid='login-submit']",
                element.declarationRef(),"candidate-old","candidate-new","classification","drift",List.of("SAME_TARGET"),List.of("STABLE"),List.of("InvalidLoginTest"),List.of(),List.of("rerun"),target,replacement,List.of("login"));
        return new Fixture(service,source,proposal,service.createRequirement("Invalid password on LoginPage"));
    }
    private record Fixture(TestEngineeringStudioService service,Path source,RepairProposal proposal,String runId){}
}
