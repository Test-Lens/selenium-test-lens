package io.github.testlens.application.model;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Selenium-free, provider-neutral, versioned description of an observed application.
 * All collections are canonicalized so serialization and generation are deterministic.
 * @since 0.5.0
 */
public record ApplicationModel(int schemaVersion, String applicationId, String applicationName,
                               String generatorVersion, Instant generatedAt, List<PageModel> pages,
                               List<SharedComponent> sharedComponents, List<Transition> transitions,
                               Coverage coverage, List<Limitation> limitations, Provenance provenance) {
    public static final int SCHEMA_VERSION = 1;

    public ApplicationModel {
        if (schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported schemaVersion: " + schemaVersion);
        require(applicationId, "applicationId"); require(applicationName, "applicationName");
        require(generatorVersion, "generatorVersion"); Objects.requireNonNull(generatedAt, "generatedAt");
        pages = sorted(pages, Comparator.comparing(PageModel::pageId));
        sharedComponents = sorted(sharedComponents, Comparator.comparing(SharedComponent::componentId));
        transitions = sorted(transitions, Comparator.comparing(Transition::transitionId));
        coverage = Objects.requireNonNull(coverage, "coverage");
        limitations = sorted(limitations, Comparator.comparing(Limitation::code).thenComparing(Limitation::detail));
        provenance = Objects.requireNonNull(provenance, "provenance");
    }

    public record PageModel(String pageId, String canonicalName, PageIdentity identity,
                            List<String> titles, List<String> fingerprints, List<PageState> states,
                            List<Region> regions, List<ElementModel> elements,
                            List<String> sharedComponentRefs, List<Transition> transitions,
                            Provenance provenance, List<Limitation> limitations) {
        public PageModel {
            require(pageId,"pageId"); require(canonicalName,"canonicalName"); Objects.requireNonNull(identity,"identity");
            titles=sortedStrings(titles); fingerprints=sortedStrings(fingerprints);
            states=sorted(states,Comparator.comparing(PageState::stateId));
            regions=sorted(regions,Comparator.comparing(Region::regionId));
            elements=sorted(elements,Comparator.comparing(ElementModel::elementId));
            sharedComponentRefs=sortedStrings(sharedComponentRefs);
            transitions=sorted(transitions,Comparator.comparing(Transition::transitionId));
            provenance=Objects.requireNonNull(provenance,"provenance");
            limitations=sorted(limitations,Comparator.comparing(Limitation::code).thenComparing(Limitation::detail));
        }
    }

    public record PageIdentity(String normalizedUrlPattern, String route, String titlePattern,
                               List<String> landmarkElementIds, String structuralFingerprint,
                               EvidenceSource source) {
        public PageIdentity {
            require(normalizedUrlPattern,"normalizedUrlPattern");
            landmarkElementIds=sortedStrings(landmarkElementIds); Objects.requireNonNull(source,"source");
        }
    }

    public record PageState(String stateId, String semanticName, List<String> distinguishingSignals,
                            List<String> elementIds, Provenance provenance) {
        public PageState {
            require(stateId,"stateId"); require(semanticName,"semanticName");
            distinguishingSignals=sortedStrings(distinguishingSignals); elementIds=sortedStrings(elementIds);
            provenance=Objects.requireNonNull(provenance,"provenance");
        }
    }

    public record Region(String regionId, String semanticName, String role, String fingerprint,
                         Provenance provenance) {
        public Region { require(regionId,"regionId");require(semanticName,"semanticName");require(fingerprint,"fingerprint");Objects.requireNonNull(provenance,"provenance"); }
    }

    public record ElementModel(String elementId, String semanticName, ElementType type, String semanticRole,
                               String label, String accessibleName, List<Action> actions,
                               SelectorProjection preferredSelector, List<SelectorProjection> alternativeSelectors,
                               SelectorQuality selectorQuality, String regionId, String fingerprint,
                               Provenance provenance, List<Limitation> limitations) {
        public ElementModel {
            require(elementId,"elementId");require(semanticName,"semanticName");Objects.requireNonNull(type,"type");
            actions=sorted(actions,Comparator.comparing(Enum::name));
            alternativeSelectors=sorted(alternativeSelectors,Comparator.comparing(SelectorProjection::rankingPosition)
                    .thenComparing(SelectorProjection::candidateId));
            Objects.requireNonNull(selectorQuality,"selectorQuality");require(fingerprint,"fingerprint");
            provenance=Objects.requireNonNull(provenance,"provenance");
            limitations=sorted(limitations,Comparator.comparing(Limitation::code).thenComparing(Limitation::detail));
        }
    }

    public record SelectorProjection(String strategy, String value, String candidateId, String validation,
                                     String sameTarget, boolean unique, List<String> stability,
                                     int rankingPosition, List<String> reasons, List<String> limitations,
                                     EvidenceSource source) {
        public SelectorProjection {
            require(strategy,"strategy");require(value,"value");require(candidateId,"candidateId");
            stability=sortedStrings(stability);reasons=sortedStrings(reasons);limitations=sortedStrings(limitations);
            if(rankingPosition<1)throw new IllegalArgumentException("rankingPosition must be positive");
            Objects.requireNonNull(source,"source");
        }
    }

    public record SharedComponent(String componentId, String canonicalName, List<String> pageIds,
                                  List<String> elementIds, List<String> fingerprints,
                                  EvidenceSource source, Provenance provenance) {
        public SharedComponent {
            require(componentId,"componentId");require(canonicalName,"canonicalName");
            pageIds=sortedStrings(pageIds);elementIds=sortedStrings(elementIds);fingerprints=sortedStrings(fingerprints);
            Objects.requireNonNull(source,"source");Objects.requireNonNull(provenance,"provenance");
        }
    }

    public record Transition(String transitionId, String sourcePageId, String sourceStateId,
                             String elementId, Action action, String targetPageId, String targetStateId,
                             TransitionConfidence confidence, Provenance provenance) {
        public Transition {
            require(transitionId,"transitionId");require(sourcePageId,"sourcePageId");
            Objects.requireNonNull(action,"action");Objects.requireNonNull(confidence,"confidence");
            provenance=Objects.requireNonNull(provenance,"provenance");
        }
    }

    public record Provenance(EvidenceSource source, String observationId, String contextDescriptor,
                             List<String> evidence, Map<String,String> attributes) {
        public Provenance {
            Objects.requireNonNull(source,"source"); evidence=sortedStrings(evidence);
            attributes=Map.copyOf(new TreeMap<>(attributes==null?Map.of():attributes));
        }
        public static Provenance observed(String observationId,String context,List<String> evidence){
            return new Provenance(EvidenceSource.OBSERVED,observationId,context,evidence,Map.of());
        }
    }

    public record Coverage(Completeness completeness, int observations, int pages, int states,
                           int discoveredNodes, int analyzedElements, int skippedElements,
                           int selectorAnalyses, int transitions, boolean truncated) {
        public Coverage { Objects.requireNonNull(completeness,"completeness"); }
    }
    public record Limitation(String code,String detail){public Limitation{require(code,"code");require(detail,"detail");}}

    public enum Completeness { COMPLETE_FOR_OBSERVED_CONTEXT, PARTIAL, FAILED }
    public enum EvidenceSource { OBSERVED, LIVE_CANDIDATE_ANALYSIS, USER_DECLARED, INFERRED, STATIC_AUDIT, AI_PROPOSED, UNKNOWN }
    public enum ElementType { BUTTON, INPUT, TEXTAREA, SELECT, CHECKBOX, RADIO, LINK, FORM, TABLE, TAB, MENU, MENU_ITEM, DIALOG, COMBOBOX, LISTBOX, OPTION, NAVIGATION, SEARCH, CUSTOM, UNKNOWN }
    public enum Action { CLICK, TYPE, CLEAR, SELECT, CHECK, UNCHECK, UPLOAD, FOCUS, OPEN, ASSERT, UNKNOWN }
    public enum SelectorQuality { VERIFIED, REVIEW_REQUIRED, UNAVAILABLE }
    public enum TransitionConfidence { OBSERVED, USER_DECLARED, INFERRED, UNKNOWN }

    private static void require(String value,String name){if(value==null||value.isBlank())throw new IllegalArgumentException(name+" is required");}
    private static List<String> sortedStrings(List<String> values){return (values==null?List.<String>of():values).stream().filter(Objects::nonNull).distinct().sorted().toList();}
    private static <T> List<T> sorted(List<T> values,Comparator<? super T> comparator){return (values==null?List.<T>of():values).stream().filter(Objects::nonNull).sorted(comparator).toList();}
}
