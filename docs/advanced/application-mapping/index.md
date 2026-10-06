# Application mapping

Application Mapper turns explicit browser observations into a deterministic, redacted model of an application. It is a tooling workflow built on the existing Selector Intelligence pipeline; it is not a background crawler and it does not change normal Test Lens execution.

!!! warning "Development availability"
    The application model, mapper, and tooling modules are source-only development work for 0.5.0. They are not included in the published `selenium-test-lens:0.4.0` artifact and are not available as supported Maven Central dependencies. Build and test these APIs from the current source reactor only. Do not present this page as an installation guide for 0.4.0.

The workflow has three deliberately separate layers:

```text
Browser/current browsing context
    -> bounded discovery
    -> existing Selector Intelligence
    -> ApplicationModel
    -> deterministic Page Objects
    -> bounded, provider-neutral agent context
```

The mapper never implements a second selector ranker. Each discovered live element is passed to `LiveCandidateAnalysisService`, which performs candidate generation, policy and stability analysis, live resolution, same-target comparison, and ranking. The mapper stores only the neutral projection of that result.

## Page Model and Application Model

`ApplicationModel` is a Selenium-free, schema-versioned record. Its top-level data includes application identity, generator metadata, pages, shared components, transitions, coverage, limitations, and provenance. A page contains:

- a stable `pageId` and a `PageIdentity` rather than a Selenium remote element ID;
- normalized URL pattern, route, title pattern, stable landmarks, and structural fingerprint;
- one or more `PageState` records for meaningful states such as an open dialog;
- regions and test-relevant elements;
- observed transitions and component references;
- provenance and limitations.

Page identity is not raw URL equality. Dynamic path segments such as `/customer/101` and `/customer/202` can resolve to the same page identity, while structurally different states at one route remain separate page states. Declarative `ApplicationOverrides` can assign page names, group observation fingerprints, name elements, mark shared regions, and deny URL patterns. Overrides use stable model fingerprints, never ephemeral Selenium IDs.

An `ElementModel` records semantic name and type, safe label/accessibility data, supported actions, region, selector quality, preferred and alternative selector projections, provenance, and limitations. Selector evidence distinguishes live validation, same-target comparison, uniqueness, and stability. A generated-looking ID is only evidence: a project policy can declare an exact value stable, and the existing selector engine remains authoritative.

## What is discovered

The scanner starts with one bounded discovery script for meaningful controls and landmarks, not `querySelectorAll("*")`. It considers native controls, supported ARIA roles, accessible names and labels, configured test attributes, visibility, disabled state, interaction capability, and the nearest meaningful region. Expensive selector analysis is limited to retained actionable elements.

It does not retain input, password, or textarea values; cookies; authorization headers; session identifiers; Selenium remote element IDs; or raw DOM. URL and text projections use the configured [`RedactionPolicy`](../../security/redaction.md).

Open shadow roots are traversed to `maxShadowDepth`. Closed roots cannot be inspected and are reported as a limitation. Frames are not switched implicitly: an observation covers the caller-selected current browsing context. Windows are not scanned opportunistically.

## Mapping modes

### Current Page

`CURRENT_PAGE` is the default. Starting a mapper issues no browser commands; only `observe()` scans the current context.

```java
ApplicationMapper mapper = ApplicationMapper.start(
        driver,
        ApplicationMapperOptions.builder("Customer portal").build()
);

MappingObservation observation = mapper.observe();
ApplicationModel model = mapper.model();
```

### Guided

`GUIDED` is the primary multi-page mode. The caller keeps ownership of WebDriver and performs the application flow. Before an action that represents a graph edge, call `beginTransition(...)`; after the destination or new state is ready, call `observe()`.

```java
ApplicationMapper mapper = ApplicationMapper.start(
        driver,
        ApplicationMapperOptions.builder("Customer portal")
                .mode(ApplicationMapperOptions.Mode.GUIDED)
                .build()
);

MappingObservation login = mapper.observe();
String loginButtonId = mapper.model().pages().stream()
        .filter(page -> page.pageId().equals(login.pageId()))
        .flatMap(page -> page.elements().stream())
        .filter(element -> "Log in".equals(element.accessibleName()))
        .map(ApplicationModel.ElementModel::elementId)
        .findFirst()
        .orElseThrow();

mapper.beginTransition(loginButtonId, ApplicationModel.Action.CLICK);
existingLoginPage.login(); // caller-owned Selenium or Page Object flow
mapper.observe();
```

The caller must wait for the intended destination with the application's normal synchronization before observing it. The mapper does not add background WebDriver threads.

### Safe Explore

`SAFE_EXPLORE` is opt-in and conservative. V1 considers same-origin links in the current page, executes only actions for which `ActionPolicy` returns `ALLOW`, restores the source after each action, and remains a one-hop explorer even when a larger crawl depth is configured. `DENY` and `REQUIRE_EXPLICIT_APPROVAL` are not executed by `safeExplore()`.

```java
ApplicationMapperOptions options = ApplicationMapperOptions.builder("Customer portal")
        .mode(ApplicationMapperOptions.Mode.SAFE_EXPLORE)
        .maxCrawlDepth(1)
        .maxExploreActions(5)
        .actionPolicy(action -> approvedElementIds.contains(action.elementId())
                ? ApplicationMapperOptions.ActionDecision.ALLOW
                : ApplicationMapperOptions.ActionDecision.REQUIRE_EXPLICIT_APPROVAL)
        .build();

ApplicationMapper mapper = ApplicationMapper.start(driver, options);
mapper.observe();
int executed = mapper.safeExplore();
```

There is no autonomous "click everything" mode. Mutating forms, payments, deletes, logout, unknown controls, cross-origin links, and new-window targets are not automatically explored. Authentication remains caller-owned: prepare the browser session before starting the mapper, and never place credentials in `ApplicationOverrides` or the model.

## Bounds, coverage, and provenance

Options bound discovered nodes, actionable elements, selector analyses, regions, open-shadow depth, pages, states, transitions, crawl depth, and exploration actions. Defaults are conservative and every configured limit is itself bounded. When observation or aggregation is truncated, the model reports `Completeness.PARTIAL` and specific `Limitation` entries. Partial coverage must not be interpreted as a complete map.

Provenance distinguishes `OBSERVED`, `LIVE_CANDIDATE_ANALYSIS`, `USER_DECLARED`, `INFERRED`, `STATIC_AUDIT`, `AI_PROPOSED`, and `UNKNOWN`. Structured evidence is authoritative; the model does not reduce trust to one unexplained floating-point score.

## Next steps

- Follow the [end-to-end guided tutorial](tutorial.md).
- Generate and safely regenerate [Page Objects](page-object-generation.md).
- Review [application drift, security, and troubleshooting](drift-security-troubleshooting.md).
- Prepare bounded [AI test-engineering context](../../ai/test-engineering.md).

