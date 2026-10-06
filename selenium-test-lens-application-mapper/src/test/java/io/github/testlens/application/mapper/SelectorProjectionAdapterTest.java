package io.github.testlens.application.mapper;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.selector.engine.CandidateAnalysis;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.testlens.selector.engine.CandidateAnalysis.*;
import static org.junit.jupiter.api.Assertions.*;

class SelectorProjectionAdapterTest {
    private final SelectorProjectionAdapter adapter = new SelectorProjectionAdapter();

    @Test
    void reusesRankedLiveValidatedSameTargetCandidates() {
        Candidate wrongTarget = candidate("wrong", "id", "wrong", ValidationState.WRONG_TARGET, 1, TargetComparison.DIFFERENT_TARGET);
        Candidate preferred = candidate("preferred", "css selector", "[data-testid='save']", ValidationState.VERIFIED_IN_SCOPE, 1, TargetComparison.SAME_TARGET);
        Candidate alternative = candidate("alternative", "id", "save", ValidationState.VALID_FOR_INTENT, 1, TargetComparison.SAME_TARGET);

        SelectorProjectionAdapter.Projection projection = adapter.project(
                analysis(Recommendation.KEEP_CURRENT, wrongTarget, preferred, alternative),
                RedactionPolicy.defaults());

        assertEquals(ApplicationModel.SelectorQuality.VERIFIED, projection.quality());
        assertEquals("preferred", projection.preferred().candidateId());
        assertEquals(2, projection.preferred().rankingPosition(), "ranking position must remain the engine position");
        assertTrue(projection.preferred().unique());
        assertEquals(List.of("alternative"), projection.alternatives().stream().map(ApplicationModel.SelectorProjection::candidateId).toList());
        assertEquals(ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS, projection.preferred().source());
    }

    @Test
    void reviewRecommendationIsNotUpgradedToVerified() {
        SelectorProjectionAdapter.Projection projection = adapter.project(
                analysis(Recommendation.REVIEW_REQUIRED,
                        candidate("candidate", "id", "react-select-17-input", ValidationState.VERIFIED_IN_SCOPE, 1, TargetComparison.SAME_TARGET)),
                RedactionPolicy.defaults());

        assertEquals(ApplicationModel.SelectorQuality.REVIEW_REQUIRED, projection.quality());
        assertNotNull(projection.preferred());
    }

    @Test
    void noSameTargetCandidateProducesNoRecommendedSelector() {
        SelectorProjectionAdapter.Projection projection = adapter.project(
                analysis(Recommendation.NO_VALID_CANDIDATE,
                        candidate("ambiguous", "css selector", ".save", ValidationState.VALID_BUT_AMBIGUOUS, 2, TargetComparison.SAME_TARGET),
                        candidate("different", "id", "save", ValidationState.VERIFIED_IN_SCOPE, 1, TargetComparison.DIFFERENT_TARGET)),
                RedactionPolicy.defaults());

        assertEquals(ApplicationModel.SelectorQuality.UNAVAILABLE, projection.quality());
        assertNull(projection.preferred());
        assertTrue(projection.alternatives().isEmpty());
    }

    private static CandidateAnalysis analysis(Recommendation recommendation, Candidate... candidates) {
        return new CandidateAnalysis(
                CandidateAnalysis.SCHEMA_VERSION,
                UsageIntent.FIND_ONE,
                "context",
                null,
                null,
                null,
                List.of(candidates),
                recommendation,
                new Completeness(true, false, false, 0, candidates.length, candidates.length, candidates.length),
                List.of());
    }

    private static Candidate candidate(
            String id,
            String strategy,
            String value,
            ValidationState state,
            int matches,
            TargetComparison comparison) {
        return new Candidate(
                id,
                new Locator(strategy, value),
                false,
                List.of(Origin.TEST_ATTRIBUTE),
                List.of(),
                new Validation(state, matches, comparison, List.of()),
                new Complexity(ScopeFragility.DIRECT, SemanticPreference.PREFERRED_TEST_ATTRIBUTE, 0, 0, value.length(), 1),
                List.of(new Reason("LIVE_VALIDATED", "validated")),
                List.of(),
                false);
    }
}
