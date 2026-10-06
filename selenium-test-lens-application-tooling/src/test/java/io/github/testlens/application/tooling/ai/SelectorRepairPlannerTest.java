package io.github.testlens.application.tooling.ai;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.source.PageObjectCorrelation;
import io.github.testlens.application.tooling.source.SourceImpact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SelectorRepairPlannerTest {
    @Test
    void proposesOnlyLiveSameTargetUniqueVerifiedReplacementForExactDeclaration() {
        RepairProposal proposal = new SelectorRepairPlanner().propose("repair-1", selectorFailure(),
                element("old", "#stale", ApplicationModel.EvidenceSource.OBSERVED, "UNKNOWN", false,
                        ApplicationModel.SelectorQuality.REVIEW_REQUIRED),
                element("new", "[data-testid='login']", ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS,
                        "SAME_TARGET", true, ApplicationModel.SelectorQuality.VERIFIED),
                correlation(PageObjectCorrelation.State.EXACT), impact(), "classification-1", "drift-1");

        assertEquals(RepairProposal.ApplicationPolicy.PROPOSE_ONLY, proposal.applicationPolicy());
        assertEquals("decl-login", proposal.sourceDeclarationRef());
        assertEquals("new", proposal.newCandidateId());
        assertEquals(List.of("LoginTest"), proposal.affectedTests());
    }

    @Test
    void rejectsAmbiguousCorrelationAndNonLiveReplacement() {
        var planner = new SelectorRepairPlanner();
        var before = element("old", "#stale", ApplicationModel.EvidenceSource.OBSERVED, "UNKNOWN", false,
                ApplicationModel.SelectorQuality.REVIEW_REQUIRED);
        var live = element("new", "#login", ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS,
                "SAME_TARGET", true, ApplicationModel.SelectorQuality.VERIFIED);
        var inferred = element("new", "#login", ApplicationModel.EvidenceSource.INFERRED,
                "SAME_TARGET", true, ApplicationModel.SelectorQuality.VERIFIED);

        assertThrows(IllegalArgumentException.class, () -> planner.propose("repair", selectorFailure(), before, live,
                correlation(PageObjectCorrelation.State.AMBIGUOUS), impact(), "classification", "drift"));
        assertThrows(IllegalArgumentException.class, () -> planner.propose("repair", selectorFailure(), before, live,
                correlation(PageObjectCorrelation.State.PROBABLE), impact(), "classification", "drift"));
        assertThrows(IllegalArgumentException.class, () -> planner.propose("repair", selectorFailure(), before, inferred,
                correlation(PageObjectCorrelation.State.EXACT), impact(), "classification", "drift"));
    }

    private static FailureClassification selectorFailure() {
        ContractHeader header = new ContractHeader(1, ContractHeader.Status.READY, List.of("selector"), List.of(),
                ContractHeader.Confidence.OBSERVED);
        return new FailureClassification(header, FailureClassification.Category.SELECTOR_INSTABILITY,
                "stale selector", List.of("LOGIN_SUBMIT"), List.of("selector-live"), List.of());
    }

    private static ApplicationModel.ElementModel element(String candidate, String value,
                                                          ApplicationModel.EvidenceSource source, String sameTarget,
                                                          boolean unique, ApplicationModel.SelectorQuality quality) {
        var selector = new ApplicationModel.SelectorProjection("css", value, candidate, "VERIFIED_IN_SCOPE",
                sameTarget, unique, List.of("STABLE"), 1, List.of(), List.of(), source);
        return new ApplicationModel.ElementModel("LOGIN_SUBMIT", "loginButton", ApplicationModel.ElementType.BUTTON,
                "button", "Login", "Login", List.of(ApplicationModel.Action.CLICK), selector, List.of(), quality,
                null, "fp", ApplicationModel.Provenance.observed("obs", "default", List.of()), List.of());
    }

    private static PageObjectCorrelation.ElementCorrelation correlation(PageObjectCorrelation.State state) {
        return new PageObjectCorrelation.ElementCorrelation("LOGIN", "LOGIN_SUBMIT", "source-login", "decl-login",
                state, List.of(PageObjectCorrelation.Evidence.NORMALIZED_SELECTOR_MATCH), List.of(), List.of());
    }

    private static SourceImpact impact() {
        return new SourceImpact("LOGIN_SUBMIT", List.of("decl-login"), List.of("method-login"),
                List.of("LoginTest"), List.of());
    }
}
