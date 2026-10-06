package io.github.testlens.application.tooling.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidenceFailureClassifierTest {
    @Test
    void productEvidenceWinsOverSimultaneousSelectorEvidence() {
        var signals = new EvidenceFailureClassifier.Signals(false, List.of(), false, List.of(),
                true, List.of("actual total differs from product contract"), true,
                List.of("selector diagnostic was also present"), List.of(), false, List.of(), List.of("LOGIN"));

        FailureClassification result = new EvidenceFailureClassifier().classify(signals);

        assertEquals(FailureClassification.Category.PRODUCT_DEFECT, result.category());
        assertEquals(List.of("actual total differs from product contract"), result.evidenceRefs());
        assertEquals(List.of("selector diagnostic was also present"), result.counterEvidenceRefs());
    }

    @Test
    void returnsUnknownWhenNoSupportedEvidenceExists() {
        var signals = new EvidenceFailureClassifier.Signals(false, List.of(), false, List.of(),
                false, List.of(), false, List.of(), List.of(), false, List.of(), List.of());

        FailureClassification result = new EvidenceFailureClassifier().classify(signals);

        assertEquals(FailureClassification.Category.UNKNOWN, result.category());
        assertEquals(ContractHeader.Status.PARTIAL, result.header().status());
        assertTrue(result.counterEvidenceRefs().get(0).contains("No decisive"));
    }
}
