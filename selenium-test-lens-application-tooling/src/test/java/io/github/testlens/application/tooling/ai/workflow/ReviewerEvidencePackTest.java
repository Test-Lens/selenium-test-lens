package io.github.testlens.application.tooling.ai.workflow;

import io.github.testlens.application.tooling.ai.CodeReviewResult;
import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.TestImplementationProposal;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReviewerEvidencePackTest {
    @Test
    void acceptsOnlyCatalogBackedReferencesAndClosedRuleIdentifiers() {
        ReviewerEvidencePack.Evidence implementation = evidence("implementation-1",
                ReviewerEvidencePack.ArtifactType.IMPLEMENTATION,
                List.of(CodeReviewResult.Category.ASSERTION_INSUFFICIENT),
                List.of(CodeReviewResult.RuleId.ASSERTION_REQUIRED.name()));
        ReviewerEvidencePack.Evidence execution = evidence("execution-1",
                ReviewerEvidencePack.ArtifactType.EXECUTION,
                List.of(CodeReviewResult.Category.EXECUTION_EVIDENCE_INSUFFICIENT),
                List.of(CodeReviewResult.RuleId.EXECUTION_EVIDENCE_REQUIRED.name()));

        ReviewerEvidencePack pack = new ReviewerEvidencePack("run-1", 1, "implementation-1",
                "implementation-digest", "source-fingerprint", List.of(implementation, execution),
                List.of("implementation-1", "execution-1"),
                List.of("EXECUTION_EVIDENCE_REQUIRED", "ASSERTION_REQUIRED"));

        assertEquals(List.of("execution-1", "implementation-1"), pack.requiredReviewedArtifactRefs());
        assertEquals(List.of("ASSERTION_REQUIRED", "EXECUTION_EVIDENCE_REQUIRED"), pack.allowedRuleIds());
        for (String reference : pack.requiredReviewedArtifactRefs()) {
            assertEquals(1, pack.evidence().stream().filter(item -> item.artifactId().equals(reference)).count());
        }
        assertThrows(IllegalArgumentException.class, () -> new ReviewerEvidencePack("run-1", 1,
                "implementation-1", "implementation-digest", "source-fingerprint",
                List.of(implementation, execution), List.of("foreign-artifact"),
                List.of("ASSERTION_REQUIRED")));
        assertThrows(IllegalArgumentException.class, () -> new ReviewerEvidencePack("run-1", 1,
                "implementation-1", "implementation-digest", "source-fingerprint",
                List.of(implementation, execution), List.of("implementation-1"),
                List.of("UNKNOWN_RULE")));
    }

    @Test
    void rejectsDuplicateOrIncompleteCatalogEntries() {
        ReviewerEvidencePack.Evidence implementation = evidence("implementation-1",
                ReviewerEvidencePack.ArtifactType.IMPLEMENTATION, List.of(), List.of());

        assertThrows(IllegalArgumentException.class, () -> new ReviewerEvidencePack("run-1", 1,
                "implementation-1", "implementation-digest", "source-fingerprint",
                List.of(implementation, implementation), List.of("implementation-1"), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ReviewerEvidencePack.Evidence("",
                ReviewerEvidencePack.ArtifactType.IMPLEMENTATION, "run-1", 1, "digest", List.of(),
                ReviewerEvidencePack.Availability.AVAILABLE, true, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ReviewerEvidencePack.Evidence("implementation-1",
                ReviewerEvidencePack.ArtifactType.IMPLEMENTATION, "run-1", 1, "digest", List.of(),
                ReviewerEvidencePack.Availability.AVAILABLE, true, List.of(), List.of("UNKNOWN_RULE")));
    }

    @Test
    void verifiesDigestLineageAvailabilityAndFingerprintAgainstTrustedArtifacts() {
        ArtifactEnvelope<TestImplementationProposal> implementation = ArtifactEnvelope.create(
                "implementation-1", "run-1", List.of("plan-1"), 1, implementation());
        ArtifactEnvelope<String> execution = ArtifactEnvelope.create(
                "execution-1", "run-1", List.of("compile-1", "implementation-1"), 1, "execution-pass");
        List<ArtifactEnvelope<?>> artifacts = List.of(implementation, execution);
        ReviewerEvidencePack valid = pack(implementation, execution,
                sourceDigest(implementation.payload()), implementation.payloadDigest(), execution.payloadDigest(),
                implementation.parents(), execution.parents());

        assertEquals(ReviewerEvidencePack.IntegrityStatus.VALID, valid.verify(artifacts, "source-fingerprint"));
        assertEquals(ReviewerEvidencePack.IntegrityStatus.IMPLEMENTATION_FINGERPRINT_MISMATCH,
                valid.verify(artifacts, "stale-fingerprint"));
        assertEquals(ReviewerEvidencePack.IntegrityStatus.EVIDENCE_UNAVAILABLE,
                valid.verify(List.of(implementation), "source-fingerprint"));

        ReviewerEvidencePack wrongImplementationDigest = pack(implementation, execution,
                "wrong-source-digest", implementation.payloadDigest(), execution.payloadDigest(),
                implementation.parents(), execution.parents());
        assertEquals(ReviewerEvidencePack.IntegrityStatus.EVIDENCE_DIGEST_MISMATCH,
                wrongImplementationDigest.verify(artifacts, "source-fingerprint"));

        ReviewerEvidencePack wrongPayloadDigest = pack(implementation, execution,
                sourceDigest(implementation.payload()), implementation.payloadDigest(), "wrong-payload-digest",
                implementation.parents(), execution.parents());
        assertEquals(ReviewerEvidencePack.IntegrityStatus.EVIDENCE_DIGEST_MISMATCH,
                wrongPayloadDigest.verify(artifacts, "source-fingerprint"));

        ReviewerEvidencePack wrongLineage = pack(implementation, execution,
                sourceDigest(implementation.payload()), implementation.payloadDigest(), execution.payloadDigest(),
                implementation.parents(), List.of("foreign-parent"));
        assertEquals(ReviewerEvidencePack.IntegrityStatus.EVIDENCE_LINEAGE_INVALID,
                wrongLineage.verify(artifacts, "source-fingerprint"));
    }

    private static ReviewerEvidencePack.Evidence evidence(String artifactId,
            ReviewerEvidencePack.ArtifactType type, List<CodeReviewResult.Category> categories,
            List<String> rules) {
        return new ReviewerEvidencePack.Evidence(artifactId, type, "run-1", 1, artifactId + "-digest",
                List.of(), ReviewerEvidencePack.Availability.AVAILABLE, true, categories, rules);
    }

    private static ReviewerEvidencePack pack(ArtifactEnvelope<TestImplementationProposal> implementation,
            ArtifactEnvelope<String> execution, String implementationDigest, String implementationPayloadDigest,
            String executionPayloadDigest, List<String> implementationParents, List<String> executionParents) {
        List<ReviewerEvidencePack.Evidence> evidence = List.of(
                new ReviewerEvidencePack.Evidence(implementation.artifactId(),
                        ReviewerEvidencePack.ArtifactType.IMPLEMENTATION, implementation.runId(),
                        implementation.attempt(), implementationPayloadDigest, implementationParents,
                        ReviewerEvidencePack.Availability.AVAILABLE, true, List.of(), List.of()),
                new ReviewerEvidencePack.Evidence(execution.artifactId(),
                        ReviewerEvidencePack.ArtifactType.EXECUTION, execution.runId(), execution.attempt(),
                        executionPayloadDigest, executionParents, ReviewerEvidencePack.Availability.AVAILABLE,
                        true, List.of(), List.of()));
        return new ReviewerEvidencePack("run-1", 1, implementation.artifactId(), implementationDigest,
                "source-fingerprint", evidence, List.of(implementation.artifactId(), execution.artifactId()),
                List.of());
    }

    private static TestImplementationProposal implementation() {
        ContractHeader header = new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of(), List.of(), ContractHeader.Confidence.OBSERVED);
        return new TestImplementationProposal(header, "scenario-1", "class GeneratedTest {}",
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY, List.of(), List.of());
    }

    private static String sourceDigest(TestImplementationProposal implementation) {
        return ArtifactEnvelope.digest(implementation.sourcePatch().getBytes(StandardCharsets.UTF_8));
    }
}
