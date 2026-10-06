package io.github.testlens.application.tooling;

import io.github.testlens.application.model.ApplicationModel;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ToolingFixtures {
    private ToolingFixtures() { }

    public static ApplicationModel model() {
        return model(Instant.parse("2026-01-02T03:04:05Z"), "observation-a", "[data-testid='login']",
                ApplicationModel.SelectorQuality.VERIFIED, true);
    }

    public static ApplicationModel model(Instant generatedAt, String observationId, String selectorValue,
                                         ApplicationModel.SelectorQuality quality, boolean sourceCorrelation) {
        ApplicationModel.Provenance provenance = provenance(observationId, sourceCorrelation);
        ApplicationModel.ElementModel username = element("username", "username", ApplicationModel.ElementType.INPUT,
                List.of(ApplicationModel.Action.TYPE, ApplicationModel.Action.CLEAR), "id", "username", quality, provenance);
        ApplicationModel.ElementModel login = element("login", "loginButton", ApplicationModel.ElementType.BUTTON,
                List.of(ApplicationModel.Action.CLICK), "css selector", selectorValue, quality, provenance);
        ApplicationModel.ElementModel menu = element("menu", "userMenu", ApplicationModel.ElementType.BUTTON,
                List.of(ApplicationModel.Action.CLICK), "name", "user-menu", ApplicationModel.SelectorQuality.VERIFIED, provenance);
        ApplicationModel.Transition transition = new ApplicationModel.Transition(
                "login-transition", "login-page", "login-state", "login", ApplicationModel.Action.CLICK,
                "dashboard-page", "dashboard-state", ApplicationModel.TransitionConfidence.OBSERVED, provenance);
        ApplicationModel.PageModel loginPage = page("login-page", "Login Page", "/login", "login-state",
                List.of(username, login), List.of(transition), provenance);
        ApplicationModel.PageModel dashboard = page("dashboard-page", "Dashboard Page", "/dashboard", "dashboard-state",
                List.of(menu), List.of(), provenance);
        return new ApplicationModel(ApplicationModel.SCHEMA_VERSION, "application-1", "Example", "0.5.0", generatedAt,
                List.of(loginPage, dashboard), List.of(), List.of(transition),
                new ApplicationModel.Coverage(ApplicationModel.Completeness.COMPLETE_FOR_OBSERVED_CONTEXT,
                        2, 2, 2, 3, 3, 0, 3, 1, false), List.of(), provenance);
    }

    public static ApplicationModel.PageModel page(String id, String name, String url, String stateId,
                                                   List<ApplicationModel.ElementModel> elements,
                                                   List<ApplicationModel.Transition> transitions,
                                                   ApplicationModel.Provenance provenance) {
        ApplicationModel.PageIdentity identity = new ApplicationModel.PageIdentity(
                url, url, null, elements.stream().map(ApplicationModel.ElementModel::elementId).toList(),
                "structure-" + id, ApplicationModel.EvidenceSource.OBSERVED);
        ApplicationModel.PageState state = new ApplicationModel.PageState(
                stateId, stateId, List.of("structure=" + id),
                elements.stream().map(ApplicationModel.ElementModel::elementId).toList(), provenance);
        return new ApplicationModel.PageModel(id, name, identity, List.of(name), List.of("structure-" + id),
                List.of(state), List.of(), elements, List.of(), transitions, provenance, List.of());
    }

    public static ApplicationModel.ElementModel element(String id, String name, ApplicationModel.ElementType type,
                                                         List<ApplicationModel.Action> actions, String strategy,
                                                         String selectorValue, ApplicationModel.SelectorQuality quality,
                                                         ApplicationModel.Provenance provenance) {
        ApplicationModel.SelectorProjection selector = selectorValue == null ? null : new ApplicationModel.SelectorProjection(
                strategy, selectorValue, "candidate-" + id, "VERIFIED_IN_SCOPE", "SAME_TARGET", true,
                List.of("DECLARED_STABLE"), 1, List.of(), List.of(), ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS);
        return new ApplicationModel.ElementModel(id, name, type, type.name().toLowerCase(), name, name, actions,
                selector, List.of(), quality, null, "fingerprint-" + id, provenance, List.of());
    }

    public static ApplicationModel.Provenance provenance(String observationId, boolean sourceCorrelation) {
        return new ApplicationModel.Provenance(ApplicationModel.EvidenceSource.OBSERVED, observationId, "window=current",
                List.of("TEST"), sourceCorrelation ? Map.of("sourceDeclaration", "LoginPage.java:12") : Map.of());
    }
}
