package io.github.testlens.studio;

import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.FailureClassification;
import io.github.testlens.application.tooling.ai.TestExecutionResult;
import io.github.testlens.studio.browser.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ExternalRepairPipelineTest {
    @TempDir Path project;

    @Test void productMismatchNeverStartsSelectorAnalysisOrOffersRepair(){
        AtomicBoolean browserOpened=new AtomicBoolean();
        BrowserSessionProvider browsers=new BrowserSessionProvider(){
            @Override public BrowserAvailability preflight(BrowserRequest request){return BrowserAvailability.AVAILABLE;}
            @Override public BrowserSession open(BrowserRequest request){browserOpened.set(true);throw new AssertionError("selector analysis must not run");}
        };
        BrowserRequest mapping=BrowserRequest.studioOwned(Purpose.MAPPING,Browser.CHROME);
        ExternalRepairPipeline pipeline=new ExternalRepairPipeline(project,List.of(project),List.of(),browsers,mapping,null);
        TestExecutionResult execution=failure(List.of("AssertionFailedError: expected true but was false"));

        FailureClassification classification=pipeline.classifier().classify(execution);

        assertEquals(FailureClassification.Category.PRODUCT_DEFECT,classification.category());
        assertFalse(browserOpened.get());
        assertTrue(pipeline.stabilizer().propose(classification,execution).isEmpty());
    }

    @Test void missingElementWithoutCorrelatedLiveEvidenceRemainsUnknown(){
        BrowserSessionProvider unavailable=new BrowserSessionProvider(){
            @Override public BrowserAvailability preflight(BrowserRequest request){return BrowserAvailability.NOT_AVAILABLE;}
            @Override public BrowserSession open(BrowserRequest request){throw new IllegalStateException("not available");}
        };
        ExternalRepairPipeline pipeline=new ExternalRepairPipeline(project,List.of(project),List.of(),unavailable,
                BrowserRequest.studioOwned(Purpose.MAPPING,Browser.CHROME),null);
        TestExecutionResult execution=failure(List.of("NoSuchElementException: old selector"));

        FailureClassification classification=pipeline.classifier().classify(execution);

        assertEquals(FailureClassification.Category.UNKNOWN,classification.category(),
                "exception type alone must not imply selector instability");
        assertTrue(pipeline.stabilizer().propose(classification,execution).isEmpty());
    }

    private static TestExecutionResult failure(List<String> evidence){
        ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.FAILED,
                evidence,List.of(),ContractHeader.Confidence.OBSERVED);
        return new TestExecutionResult(header,"scenario",TestExecutionResult.Outcome.PASS,
                TestExecutionResult.Outcome.FAIL,"tests=1",1,List.of("compile:pass"),evidence,List.of(),
                evidence,List.of(),evidence,"Targeted test failed");
    }
}
