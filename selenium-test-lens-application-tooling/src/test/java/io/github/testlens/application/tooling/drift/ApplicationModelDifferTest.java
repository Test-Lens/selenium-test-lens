package io.github.testlens.application.tooling.drift;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ToolingFixtures;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationModelDifferTest {
    @Test
    void reportsSelectorChangeDegradationImprovementAndSourceCorrelation() {
        ApplicationModel verified = ToolingFixtures.model(Instant.EPOCH, "one", "#login",
                ApplicationModel.SelectorQuality.VERIFIED, true);
        ApplicationModel degraded = ToolingFixtures.model(Instant.EPOCH, "two", "[data-testid='login']",
                ApplicationModel.SelectorQuality.REVIEW_REQUIRED, true);
        ApplicationDrift down = new ApplicationModelDiffer().compare(verified, degraded);

        assertChange(down, ApplicationDrift.ChangeType.SELECTOR_CHANGED);
        ApplicationDrift.Change unstable = change(down, ApplicationDrift.ChangeType.SELECTOR_BECAME_UNSTABLE);
        assertEquals(ApplicationDrift.SourceCorrelation.CORRELATED, unstable.sourceCorrelation());

        ApplicationDrift up = new ApplicationModelDiffer().compare(degraded, verified);
        assertChange(up, ApplicationDrift.ChangeType.SELECTOR_IMPROVED);
    }

    @Test
    void reportsAddRemoveAndNoSourceCorrelationWithoutGuessing() {
        ApplicationModel before = ToolingFixtures.model(Instant.EPOCH, "one", "#login",
                ApplicationModel.SelectorQuality.VERIFIED, false);
        ApplicationModel after = withElements(before, List.of(before.pages().get(0).elements().get(0),
                ToolingFixtures.element("new-action", "newAction", ApplicationModel.ElementType.BUTTON,
                        List.of(ApplicationModel.Action.CLICK), "id", "new-action",
                        ApplicationModel.SelectorQuality.VERIFIED, ToolingFixtures.provenance("two", false))));

        ApplicationDrift added = new ApplicationModelDiffer().compare(before, after);
        assertEquals(ApplicationDrift.SourceCorrelation.NO_SOURCE_CORRELATION,
                change(added, ApplicationDrift.ChangeType.ELEMENT_ADDED).sourceCorrelation());
        assertChange(new ApplicationModelDiffer().compare(after, before), ApplicationDrift.ChangeType.ELEMENT_REMOVED);
    }

    @Test
    void sameQualitySelectorEvidenceChangesAreReportedAsSemanticDrift() {
        ApplicationModel.SelectorProjection baseline = selector("VERIFIED_IN_SCOPE", "SAME_TARGET", true,
                List.of("DECLARED_STABLE"));
        List<ApplicationModel.SelectorProjection> changedEvidence = List.of(
                selector("AMBIGUOUS", "SAME_TARGET", true, List.of("DECLARED_STABLE")),
                selector("VERIFIED_IN_SCOPE", "DIFFERENT_TARGET", true, List.of("DECLARED_STABLE")),
                selector("VERIFIED_IN_SCOPE", "SAME_TARGET", false, List.of("DECLARED_STABLE")),
                selector("VERIFIED_IN_SCOPE", "SAME_TARGET", true, List.of("GENERATED_LOOKING")));

        ApplicationModel before = withLoginSelector(baseline);
        for (ApplicationModel.SelectorProjection changed : changedEvidence) {
            ApplicationDrift drift = new ApplicationModelDiffer().compare(before, withLoginSelector(changed));
            ApplicationDrift.Change change = change(drift, ApplicationDrift.ChangeType.SELECTOR_CHANGED);
            assertEquals(ApplicationDrift.SourceCorrelation.CORRELATED, change.sourceCorrelation());
            assertEquals("VERIFIED", withLoginSelector(changed).pages().stream()
                    .filter(page -> page.pageId().equals("login-page")).findFirst().orElseThrow().elements().stream()
                    .filter(element -> element.elementId().equals("login")).findFirst().orElseThrow().selectorQuality().name());
        }
    }

    private static ApplicationModel withElements(ApplicationModel model, List<ApplicationModel.ElementModel> elements) {
        ApplicationModel.PageModel old = model.pages().get(0);
        ApplicationModel.PageModel changed = ToolingFixtures.page(old.pageId(), old.canonicalName(),
                old.identity().normalizedUrlPattern(), old.states().get(0).stateId(), elements, old.transitions(), old.provenance());
        List<ApplicationModel.PageModel> pages = new ArrayList<>(model.pages());
        pages.set(0, changed);
        return new ApplicationModel(model.schemaVersion(), model.applicationId(), model.applicationName(), model.generatorVersion(),
                model.generatedAt(), pages, model.sharedComponents(), model.transitions(), model.coverage(), model.limitations(), model.provenance());
    }

    private static ApplicationModel withLoginSelector(ApplicationModel.SelectorProjection selector) {
        ApplicationModel model = ToolingFixtures.model();
        ApplicationModel.PageModel loginPage = model.pages().stream()
                .filter(page -> page.pageId().equals("login-page")).findFirst().orElseThrow();
        List<ApplicationModel.ElementModel> elements = loginPage.elements().stream().map(element -> {
            if (!element.elementId().equals("login")) return element;
            return new ApplicationModel.ElementModel(element.elementId(), element.semanticName(), element.type(),
                    element.semanticRole(), element.label(), element.accessibleName(), element.actions(), selector,
                    element.alternativeSelectors(), ApplicationModel.SelectorQuality.VERIFIED, element.regionId(),
                    element.fingerprint(), element.provenance(), element.limitations());
        }).toList();
        ApplicationModel.PageModel changed = ToolingFixtures.page(loginPage.pageId(), loginPage.canonicalName(),
                loginPage.identity().normalizedUrlPattern(), loginPage.states().get(0).stateId(), elements,
                loginPage.transitions(), loginPage.provenance());
        List<ApplicationModel.PageModel> pages = model.pages().stream()
                .map(page -> page.pageId().equals(loginPage.pageId()) ? changed : page).toList();
        return new ApplicationModel(model.schemaVersion(), model.applicationId(), model.applicationName(), model.generatorVersion(),
                model.generatedAt(), pages, model.sharedComponents(), model.transitions(), model.coverage(), model.limitations(), model.provenance());
    }

    private static ApplicationModel.SelectorProjection selector(String validation, String sameTarget, boolean unique,
                                                                 List<String> stability) {
        return new ApplicationModel.SelectorProjection("css selector", "[data-testid='login']", "candidate-login",
                validation, sameTarget, unique, stability, 1, List.of(), List.of(),
                ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS);
    }

    private static void assertChange(ApplicationDrift drift, ApplicationDrift.ChangeType type) {
        assertNotNull(change(drift, type));
    }

    private static ApplicationDrift.Change change(ApplicationDrift drift, ApplicationDrift.ChangeType type) {
        return drift.changes().stream().filter(value -> value.type() == type).findFirst().orElseThrow();
    }
}
