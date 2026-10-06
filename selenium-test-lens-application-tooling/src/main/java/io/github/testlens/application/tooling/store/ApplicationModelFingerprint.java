package io.github.testlens.application.tooling.store;

import io.github.testlens.application.model.ApplicationIds;
import io.github.testlens.application.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;

/** Stable semantic fingerprint that intentionally excludes timestamps and observation identifiers. @since 0.5.0 */
public final class ApplicationModelFingerprint {
    private ApplicationModelFingerprint() {}

    public static String semantic(ApplicationModel model) {
        List<String> values = new ArrayList<>();
        values.add(Integer.toString(model.schemaVersion())); values.add(model.applicationId()); values.add(model.applicationName());
        for (ApplicationModel.PageModel page : model.pages()) {
            values.add("page"); values.add(page.pageId()); values.add(page.canonicalName());
            values.add(page.identity().normalizedUrlPattern()); values.add(nullToEmpty(page.identity().route()));
            values.add(nullToEmpty(page.identity().titlePattern())); values.add(nullToEmpty(page.identity().structuralFingerprint()));
            values.addAll(page.identity().landmarkElementIds()); values.addAll(page.fingerprints());
            for (ApplicationModel.PageState state : page.states()) { values.add("state"); values.add(state.stateId()); values.add(state.semanticName()); values.addAll(state.distinguishingSignals()); values.addAll(state.elementIds()); }
            for (ApplicationModel.Region region : page.regions()) { values.add("region"); values.add(region.regionId()); values.add(region.semanticName()); values.add(nullToEmpty(region.role())); values.add(region.fingerprint()); }
            for (ApplicationModel.ElementModel element : page.elements()) {
                values.add("element"); values.add(element.elementId()); values.add(element.semanticName()); values.add(element.type().name());
                values.add(nullToEmpty(element.semanticRole())); values.add(nullToEmpty(element.label())); values.add(nullToEmpty(element.accessibleName()));
                values.add(element.fingerprint()); values.add(element.selectorQuality().name());
                element.actions().forEach(action -> values.add(action.name())); selector(values, element.preferredSelector());
                element.alternativeSelectors().forEach(selector -> selector(values, selector));
            }
            page.transitions().forEach(transition -> transition(values, transition));
        }
        model.sharedComponents().forEach(component -> { values.add("component"); values.add(component.componentId()); values.add(component.canonicalName()); values.addAll(component.pageIds()); values.addAll(component.elementIds()); values.addAll(component.fingerprints()); });
        model.transitions().forEach(transition -> transition(values, transition));
        model.limitations().forEach(limitation -> { values.add(limitation.code()); values.add(limitation.detail()); });
        return ApplicationIds.id("application-model-semantic-v1", values.toArray(String[]::new));
    }

    private static void selector(List<String> values, ApplicationModel.SelectorProjection selector) {
        if (selector == null) { values.add("no-selector"); return; }
        values.add("selector"); values.add(selector.candidateId()); values.add(selector.strategy()); values.add(selector.value());
        values.add(nullToEmpty(selector.validation())); values.add(nullToEmpty(selector.sameTarget())); values.add(Boolean.toString(selector.unique()));
        values.addAll(selector.stability()); values.addAll(selector.reasons()); values.addAll(selector.limitations());
    }
    private static void transition(List<String> values, ApplicationModel.Transition transition) {
        values.add("transition"); values.add(transition.transitionId()); values.add(transition.sourcePageId()); values.add(nullToEmpty(transition.sourceStateId()));
        values.add(nullToEmpty(transition.elementId())); values.add(transition.action().name()); values.add(nullToEmpty(transition.targetPageId()));
        values.add(nullToEmpty(transition.targetStateId())); values.add(transition.confidence().name());
    }
    private static String nullToEmpty(String value) { return value == null ? "" : value; }
}
