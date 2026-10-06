package io.github.testlens.application.mapper;

import io.github.testlens.application.model.ApplicationModel;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationAggregatorTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-02T03:04:05Z"), ZoneOffset.UTC);

    @Test
    void repeatObservationDoesNotDuplicatePageStateOrElement() {
        ApplicationAggregator aggregator = aggregator(10);
        ApplicationModel.PageModel observation = page("page-a", "state-a", element("element-a", "saveButton"));

        aggregator.merge(scan(observation));
        aggregator.merge(scan(observation));
        ApplicationModel model = aggregator.model();

        assertEquals(1, model.pages().size());
        assertEquals(1, model.pages().get(0).states().size());
        assertEquals(1, model.pages().get(0).elements().size());
        assertEquals(2, model.coverage().observations());
    }

    @Test
    void samePageAddsNewStateWhileIndependentPageKeepsItsOwnElements() {
        ApplicationAggregator aggregator = aggregator(10);
        aggregator.merge(scan(page("page-a", "state-empty", element("element-search", "searchInput"))));
        aggregator.merge(scan(page("page-a", "state-populated", element("element-row", "customerLink"))));
        aggregator.merge(scan(page("page-b", "state-details", element("element-save", "saveButton"))));

        ApplicationModel model = aggregator.model();
        ApplicationModel.PageModel first = model.pages().stream().filter(page -> page.pageId().equals("page-a")).findFirst().orElseThrow();
        ApplicationModel.PageModel second = model.pages().stream().filter(page -> page.pageId().equals("page-b")).findFirst().orElseThrow();

        assertEquals(2, first.states().size());
        assertEquals(List.of("element-row", "element-search"), first.elements().stream().map(ApplicationModel.ElementModel::elementId).toList());
        assertEquals(List.of("element-save"), second.elements().stream().map(ApplicationModel.ElementModel::elementId).toList());
        assertEquals(2, model.coverage().pages());
        assertEquals(3, model.coverage().states());
    }

    @Test
    void stateLimitReportsPartialCoverageInsteadOfPretendingCompleteness() {
        ApplicationAggregator aggregator = aggregator(1);
        aggregator.merge(scan(page("page-a", "state-a", element("element-a", "saveButton"))));
        aggregator.merge(scan(page("page-a", "state-b", element("element-b", "cancelButton"))));

        ApplicationModel model = aggregator.model();

        assertEquals(1, model.pages().get(0).states().size());
        assertEquals(ApplicationModel.Completeness.PARTIAL, model.coverage().completeness());
        assertTrue(model.coverage().truncated());
        assertTrue(model.limitations().stream().anyMatch(limit -> limit.code().equals("MAX_PAGE_STATES")));
    }

    @Test
    void generatedAtIsStableForTheLifetimeOfAnAggregatorSnapshotSource() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-02T03:04:05Z"));
        ApplicationAggregator aggregator = aggregator(ApplicationMapperOptions.builder("Example").clock(clock).build());
        aggregator.merge(scan(page("page-a", "state-a", element("element-a", "saveButton"))));

        Instant generatedAt = aggregator.model().generatedAt();
        clock.set(Instant.parse("2026-02-03T04:05:06Z"));

        assertEquals(generatedAt, aggregator.model().generatedAt(),
                "reading an unchanged model must not manufacture a new generation timestamp");
    }

    @Test
    void pageAndTransitionLimitsProducePartialModelWithExplicitLimitations() {
        ApplicationMapperOptions options = ApplicationMapperOptions.builder("Example")
                .maxPages(1)
                .maxTransitions(1)
                .clock(FIXED_CLOCK)
                .build();
        ApplicationAggregator aggregator = aggregator(options);
        aggregator.merge(scan(page("page-a", "state-a", element("element-a", "openButton"))));
        aggregator.merge(scan(page("page-b", "state-b", element("element-b", "saveButton"))));
        aggregator.transition(transition("transition-a"));
        aggregator.transition(transition("transition-b"));

        ApplicationModel model = aggregator.model();

        assertEquals(1, model.pages().size());
        assertEquals(1, model.transitions().size());
        assertEquals(ApplicationModel.Completeness.PARTIAL, model.coverage().completeness());
        assertTrue(model.limitations().stream().anyMatch(limit -> limit.code().equals("MAX_PAGES")));
        assertTrue(model.limitations().stream().anyMatch(limit -> limit.code().equals("MAX_TRANSITIONS")));
    }

    @Test
    void selectorRecommendationChangeKeepsIdentityAndDistinctTargetsWithSameFingerprint() {
        ApplicationAggregator aggregator = aggregator(10);
        ApplicationModel.ElementModel review = element(
                "element-stable", "saveButton", "shared-fingerprint", ApplicationModel.SelectorQuality.REVIEW_REQUIRED, null);
        ApplicationModel.SelectorProjection verifiedSelector = new ApplicationModel.SelectorProjection(
                "css selector", "[data-testid='save']", "candidate-new", "VERIFIED_IN_SCOPE", "SAME_TARGET",
                true, List.of("DECLARED_STABLE"), 1, List.of(), List.of(), ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS);
        ApplicationModel.ElementModel verified = element(
                "element-stable", "saveButton", "shared-fingerprint", ApplicationModel.SelectorQuality.VERIFIED, verifiedSelector);
        ApplicationModel.ElementModel distinct = element(
                "element-distinct", "saveButton", "shared-fingerprint", ApplicationModel.SelectorQuality.REVIEW_REQUIRED, null);

        aggregator.merge(scan(page("page-a", "state-a", List.of(review, distinct))));
        aggregator.merge(scan(page("page-a", "state-a", List.of(verified, distinct))));
        ApplicationModel.PageModel page = aggregator.model().pages().get(0);

        assertEquals(List.of("element-distinct", "element-stable"),
                page.elements().stream().map(ApplicationModel.ElementModel::elementId).toList());
        ApplicationModel.ElementModel retained = page.elements().stream()
                .filter(element -> element.elementId().equals("element-stable"))
                .findFirst().orElseThrow();
        assertEquals(ApplicationModel.SelectorQuality.VERIFIED, retained.selectorQuality());
        assertEquals("candidate-new", retained.preferredSelector().candidateId());
    }

    @Test
    void newerVerifiedRecommendationReplacesOlderVerifiedSelectorWithoutChangingIdentity() {
        ApplicationAggregator aggregator = aggregator(10);
        ApplicationModel.SelectorProjection oldSelector = new ApplicationModel.SelectorProjection(
                "id", "legacy-save", "candidate-old", "VERIFIED_IN_SCOPE", "SAME_TARGET",
                true, List.of("NO_APPEARANCE_SIGNAL"), 1, List.of(), List.of(), ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS);
        ApplicationModel.SelectorProjection newSelector = new ApplicationModel.SelectorProjection(
                "css selector", "[data-testid='save']", "candidate-new", "VERIFIED_IN_SCOPE", "SAME_TARGET",
                true, List.of("DECLARED_STABLE"), 1, List.of(), List.of(), ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS);
        ApplicationModel.ElementModel oldElement = element(
                "element-stable", "saveButton", "stable-fingerprint", ApplicationModel.SelectorQuality.VERIFIED, oldSelector);
        ApplicationModel.ElementModel newElement = element(
                "element-stable", "saveButton", "stable-fingerprint", ApplicationModel.SelectorQuality.VERIFIED, newSelector);

        aggregator.merge(scan(page("page-a", "state-a", oldElement)));
        aggregator.merge(scan(page("page-a", "state-a", newElement)));
        ApplicationModel.ElementModel retained = aggregator.model().pages().get(0).elements().get(0);

        assertEquals("element-stable", retained.elementId());
        assertEquals("stable-fingerprint", retained.fingerprint());
        assertEquals("candidate-new", retained.preferredSelector().candidateId());
        assertEquals("[data-testid='save']", retained.preferredSelector().value());
    }

    @Test
    void currentReviewRequiredObservationReplacesStaleVerifiedRecommendation() {
        assertSelectorDegradationIsVisible(ApplicationModel.SelectorQuality.REVIEW_REQUIRED);
    }

    @Test
    void currentUnavailableObservationRemovesStaleVerifiedRecommendation() {
        assertSelectorDegradationIsVisible(ApplicationModel.SelectorQuality.UNAVAILABLE);
    }

    private static void assertSelectorDegradationIsVisible(ApplicationModel.SelectorQuality currentQuality) {
        ApplicationAggregator aggregator = aggregator(10);
        ApplicationModel.SelectorProjection oldSelector = new ApplicationModel.SelectorProjection(
                "css selector", "[data-testid='save']", "candidate-old", "VERIFIED_IN_SCOPE", "SAME_TARGET",
                true, List.of("DECLARED_STABLE"), 1, List.of(), List.of(), ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS);
        ApplicationModel.ElementModel oldElement = element(
                "element-stable", "saveButton", "stable-fingerprint", ApplicationModel.SelectorQuality.VERIFIED, oldSelector);
        ApplicationModel.ElementModel currentElement = element(
                "element-stable", "saveButton", "stable-fingerprint", currentQuality, null);

        aggregator.merge(scan(page("page-a", "state-a", oldElement)));
        aggregator.merge(scan(page("page-a", "state-a", currentElement)));
        ApplicationModel.ElementModel retained = aggregator.model().pages().get(0).elements().get(0);

        assertEquals("element-stable", retained.elementId());
        assertEquals(currentQuality, retained.selectorQuality(),
                "the current live result must not be hidden by a stale verified selector");
        assertNull(retained.preferredSelector(),
                "selector absence in the current observation must remain visible to review and generation");
    }

    private static ApplicationAggregator aggregator(int maxPageStates) {
        ApplicationMapperOptions options = ApplicationMapperOptions.builder("Example")
                .maxPageStates(maxPageStates)
                .clock(FIXED_CLOCK)
                .build();
        return aggregator(options);
    }

    private static ApplicationAggregator aggregator(ApplicationMapperOptions options) {
        return new ApplicationAggregator("application-1", "Example", options);
    }

    private static ApplicationAggregator.PageScanResult scan(ApplicationModel.PageModel page) {
        return new ApplicationAggregator.PageScanResult(page, 1, 0, 1, false, List.of(), Map.of(), 10, 20);
    }

    private static ApplicationModel.PageModel page(String pageId, String stateId, ApplicationModel.ElementModel element) {
        return page(pageId, stateId, List.of(element));
    }

    private static ApplicationModel.PageModel page(String pageId, String stateId, List<ApplicationModel.ElementModel> elements) {
        ApplicationModel.Provenance provenance = ApplicationModel.Provenance.observed("observation", "current", List.of("TEST"));
        ApplicationModel.PageIdentity identity = new ApplicationModel.PageIdentity(
                "/" + pageId,
                "/" + pageId,
                null,
                List.of(),
                "structure-" + stateId,
                ApplicationModel.EvidenceSource.OBSERVED);
        ApplicationModel.PageState state = new ApplicationModel.PageState(
                stateId,
                stateId,
                List.of("structure=" + stateId),
                elements.stream().map(ApplicationModel.ElementModel::elementId).toList(),
                provenance);
        return new ApplicationModel.PageModel(
                pageId,
                pageId,
                identity,
                List.of(),
                List.of("structure-" + stateId),
                List.of(state),
                List.of(),
                elements,
                List.of(),
                List.of(),
                provenance,
                List.of());
    }

    private static ApplicationModel.ElementModel element(String elementId, String name) {
        return element(elementId, name, "fingerprint-" + elementId, ApplicationModel.SelectorQuality.UNAVAILABLE, null);
    }

    private static ApplicationModel.ElementModel element(
            String elementId,
            String name,
            String fingerprint,
            ApplicationModel.SelectorQuality quality,
            ApplicationModel.SelectorProjection selector) {
        ApplicationModel.Provenance provenance = ApplicationModel.Provenance.observed("observation", "current", List.of("TEST"));
        return new ApplicationModel.ElementModel(
                elementId,
                name,
                ApplicationModel.ElementType.BUTTON,
                "button",
                name,
                name,
                List.of(ApplicationModel.Action.CLICK),
                selector,
                List.of(),
                quality,
                null,
                fingerprint,
                provenance,
                List.of());
    }

    private static ApplicationModel.Transition transition(String id) {
        return new ApplicationModel.Transition(
                id, "page-a", "state-a", "element-a", ApplicationModel.Action.CLICK,
                "page-b", "state-b", ApplicationModel.TransitionConfidence.OBSERVED,
                ApplicationModel.Provenance.observed("observation", "current", List.of("TEST")));
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;

        private MutableClock(Instant initial) {
            instant = new AtomicReference<>(initial);
        }

        void set(Instant value) {
            instant.set(value);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
