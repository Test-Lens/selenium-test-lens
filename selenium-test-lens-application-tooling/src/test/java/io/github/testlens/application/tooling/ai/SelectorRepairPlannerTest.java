package io.github.testlens.application.tooling.ai;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.source.PageObjectCorrelation;
import io.github.testlens.application.tooling.source.SourceImpact;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SelectorRepairPlannerTest {
    @Test
    void proposesOnlyLiveSameTargetUniqueVerifiedReplacementForExactDeclaration() {
        RepairProposal proposal = new SelectorRepairPlanner().propose("repair-1", selectorFailure(),
                element("old", "#stale", ApplicationModel.EvidenceSource.OBSERVED, "UNKNOWN", false,
                        ApplicationModel.SelectorQuality.REVIEW_REQUIRED),
                element("new", "[data-testid='login']", ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS,
                        "SAME_TARGET", true, ApplicationModel.SelectorQuality.VERIFIED),
                correlation(PageObjectCorrelation.State.EXACT), impact(), index("css", "#stale"),
                "classification-1", "drift-1");

        assertEquals(RepairProposal.ApplicationPolicy.PROPOSE_ONLY, proposal.applicationPolicy());
        assertEquals("decl-login", proposal.sourceDeclarationRef());
        assertEquals("new", proposal.newCandidateId());
        assertEquals(List.of("LoginTest"), proposal.affectedTests());
        assertEquals(List.of("method-login"), proposal.affectedMethods());
        assertNotNull(proposal.sourceTarget());
        assertEquals("src/main/java/example/LoginPage.java", proposal.sourceTarget().logicalPath());
        assertEquals("file-fingerprint", proposal.sourceTarget().sourceFileFingerprint());
        assertEquals("declaration-fingerprint", proposal.sourceTarget().declarationFingerprint());
        assertEquals("VERIFIED_IN_SCOPE", proposal.replacementEvidence().validation());
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

    @Test
    void rejectsWrongTargetAmbiguousNotVerifiedAndKnownUnstableCandidates() {
        var planner = new SelectorRepairPlanner();
        var before = element("old", "#stale", ApplicationModel.EvidenceSource.OBSERVED, "UNKNOWN", false,
                ApplicationModel.SelectorQuality.REVIEW_REQUIRED);

        assertThrows(IllegalArgumentException.class, () -> planner.propose("repair", selectorFailure(), before,
                element("new", "#login", ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS,
                        "DIFFERENT_TARGET", true, ApplicationModel.SelectorQuality.VERIFIED),
                correlation(PageObjectCorrelation.State.EXACT), impact(), "classification", "drift"));
        assertThrows(IllegalArgumentException.class, () -> planner.propose("repair", selectorFailure(), before,
                element("new", "#login", ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS,
                        "SAME_TARGET", false, ApplicationModel.SelectorQuality.VERIFIED),
                correlation(PageObjectCorrelation.State.EXACT), impact(), "classification", "drift"));
        assertThrows(IllegalArgumentException.class, () -> planner.propose("repair", selectorFailure(), before,
                element("new", "#login", ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS,
                        "SAME_TARGET", true, ApplicationModel.SelectorQuality.VERIFIED,
                        "NOT_LIVE_VALIDATED", List.of("STABLE")),
                correlation(PageObjectCorrelation.State.EXACT), impact(), "classification", "drift"));
        for (String disposition : List.of("INSUFFICIENT_DATA", "DECLARED_UNSTABLE", "REVIEW_GENERATED_LOOKING",
                "OBSERVED_VARIABLE", "POLICY_POLICY_CONFLICT", "POLICY_EVIDENCE_CONFLICT")) {
            assertThrows(IllegalArgumentException.class, () -> planner.propose("repair", selectorFailure(), before,
                    element("new", "#login", ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS,
                            "SAME_TARGET", true, ApplicationModel.SelectorQuality.VERIFIED,
                            "VERIFIED_IN_SCOPE", List.of(disposition)),
                    correlation(PageObjectCorrelation.State.EXACT), impact(), "classification", "drift"), disposition);
        }
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
        return element(candidate, value, source, sameTarget, unique, quality, "VERIFIED_IN_SCOPE", List.of("STABLE"));
    }

    private static ApplicationModel.ElementModel element(String candidate, String value,
                                                          ApplicationModel.EvidenceSource source, String sameTarget,
                                                          boolean unique, ApplicationModel.SelectorQuality quality,
                                                          String validation, List<String> stability) {
        var selector = new ApplicationModel.SelectorProjection("css", value, candidate, validation,
                sameTarget, unique, stability, 1, List.of(), List.of(), source);
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

    private static ExistingProjectIndex index(String strategy, String value) {
        var source = new ExistingProjectIndex.SourceFile("src/main/java/example/LoginPage.java", "file-fingerprint");
        var owner = new ExistingProjectIndex.ClassEntry("class-login", source.logicalPath(), "example.LoginPage",
                "LoginPage", ExistingProjectIndex.ClassClassification.PAGE_OBJECT,
                ExistingProjectIndex.Origin.HAND_WRITTEN, List.of());
        var range = new ExistingProjectIndex.SourceRange(4, 36, 4, 50, 100, 115);
        var element = new ExistingProjectIndex.ElementEntry("source-login", owner.id(), "loginButton", "decl-login",
                strategy, new ExistingProjectIndex.ValueProjection("RESOLVED",
                ExistingProjectIndexer.selectorValueFingerprint(value)), range, "declaration-fingerprint");
        var metrics = new ExistingProjectIndex.Metrics(1, 1, 1, 1, 1, 0, 0, 0, 1);
        ExistingProjectIndex result = new ExistingProjectIndex(ExistingProjectIndex.SCHEMA_VERSION, "project",
                List.of(source), List.of(owner), List.of(element), List.of(), List.of(), List.of(),
                ExistingProjectIndex.Completeness.COMPLETE, List.of(), metrics);
        assertTrue(result.elements().get(0).declarationFingerprint().startsWith("declaration-"));
        return result;
    }
}
