package io.github.testlens.studio.projection;

import io.github.testlens.application.tooling.ai.CodeReviewResult;
import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.source.PageObjectCorrelation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StudioProjectionFactoryTest {
    @Test void correlationProjectionIsBoundedAndPreservesDomainStates() {
        var entries=java.util.stream.IntStream.range(0,600).mapToObj(i->new PageObjectCorrelation.ElementCorrelation(
                "page","app-"+i,i%2==0?"source-"+i:null,i%2==0?"LoginPage.java#field"+i:null,
                i%2==0?PageObjectCorrelation.State.EXACT:PageObjectCorrelation.State.NO_MATCH,
                i%2==0?List.of(PageObjectCorrelation.Evidence.NORMALIZED_SELECTOR_MATCH):List.of(),List.of(),List.of())).toList();
        var domain=new PageObjectCorrelation(PageObjectCorrelation.SCHEMA_VERSION,List.of(),entries,
                PageObjectCorrelation.Completeness.PARTIAL,List.of("unmatched"),new PageObjectCorrelation.Metrics(1,600,300,300,0,0,0,300,0));
        var projection=new StudioProjectionFactory().correlations(domain,10,1_000);
        assertEquals(500,projection.items().size());
        assertEquals(600,projection.total());
        assertEquals(StageStatus.NEEDS_REVIEW,projection.stage().status());
        // Domain correlation canonicalizes by applicationElementId before pagination.
        assertEquals("app-107",projection.items().get(0).applicationElementId());
        assertEquals("NO_MATCH",projection.items().get(0).state());
        assertEquals("app-108",projection.items().get(1).applicationElementId());
        assertEquals("EXACT",projection.items().get(1).state());
    }

    @Test void commonStageCanonicalizesAndBoundsPresentationData() {
        var stage=new StageProjection(StageStatus.PARTIAL,
                java.util.stream.IntStream.range(0,700).mapToObj(i->new StageProjection.Item("E"+i,"detail")).toList(),
                List.of(new StageProjection.Item("L","limit")),List.of(),List.of());
        assertEquals(500,stage.evidence().size());
        assertEquals("limit",stage.limitations().get(0).detail());
    }

    @Test void reviewProjectionExposesOnlyBoundedTypedFeedback() {
        ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.READY,
                List.of("fixture"),List.of(),ContractHeader.Confidence.OBSERVED);
        CodeReviewResult.Finding finding=new CodeReviewResult.Finding(
                CodeReviewResult.Category.ASSERTION_INSUFFICIENT,CodeReviewResult.Severity.ERROR,
                "ASSERTION_REQUIRED","implementation-1",CodeReviewResult.ProblemCategory.ASSERTION,
                CodeReviewResult.RequiredAction.ADD_ASSERTION,CodeReviewResult.SafeLocation.ASSERTION,
                CodeReviewResult.EvidenceAvailability.VERIFIED);
        CodeReviewResult review=new CodeReviewResult(header,CodeReviewResult.Verdict.REQUEST_CHANGES,
                List.of("implementation-1","quality-1","compile-success-1","execution-1"),List.of(finding));

        var projection=new StudioProjectionFactory().review(review);

        assertEquals("REQUEST_CHANGES",projection.verdict());
        assertEquals(List.of("compile-success-1","execution-1","implementation-1","quality-1"),
                projection.reviewedArtifactRefs());
        var projected=projection.findings().get(0);
        assertEquals("ASSERTION_INSUFFICIENT",projected.category());
        assertEquals("ASSERTION_REQUIRED",projected.ruleId());
        assertEquals("implementation-1",projected.artifactRef());
        assertEquals("ADD_ASSERTION",projected.requiredAction());
        assertEquals("VERIFIED",projected.evidenceAvailability());
        assertNull(projection.contractDiagnostic());
    }

    @Test void blockedReviewProjectionPreservesExactStaticContractReason() {
        ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.BLOCKED,
                List.of("discarded prose"),List.of("discarded limitation"),ContractHeader.Confidence.OBSERVED);
        CodeReviewResult.Finding finding=new CodeReviewResult.Finding(
                CodeReviewResult.Category.REVIEW_UNSUBSTANTIATED,CodeReviewResult.Severity.ERROR,
                CodeReviewResult.RuleId.ARTIFACT_REFERENCE_INVALID.name(),"implementation-1",
                CodeReviewResult.ProblemCategory.REVIEW_CONTRACT,CodeReviewResult.RequiredAction.HUMAN_REVIEW,
                CodeReviewResult.SafeLocation.EXECUTION_EVIDENCE,CodeReviewResult.EvidenceAvailability.VERIFIED);
        CodeReviewResult review=new CodeReviewResult(header,CodeReviewResult.Verdict.BLOCK,
                List.of("implementation-1"),List.of(finding));

        var diagnostic=new StudioProjectionFactory().review(review).contractDiagnostic();

        assertNotNull(diagnostic);
        assertEquals("REVIEW_CONTRACT",diagnostic.validationStage());
        assertEquals("ARTIFACT_REFERENCE_INVALID",diagnostic.reasonCode());
        assertEquals("$.result.payload",diagnostic.location());
        assertEquals("DOMAIN_VALIDATION",diagnostic.category());
        assertEquals(ContractHeader.SCHEMA_VERSION,diagnostic.contractVersion());
        assertEquals("implementation-1",diagnostic.artifactRef());
        assertEquals("VERIFIED",diagnostic.evidenceAvailability());
    }
}
