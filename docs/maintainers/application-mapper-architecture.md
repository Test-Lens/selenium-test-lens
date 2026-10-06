# Application Mapper architecture

## Decision

Application mapping is an explicit tooling workflow layered on the existing Selector Intelligence implementation. It does not run from `TestLens.attach`, does not add background work, and does not change normal Selenium, FAST, HUD, retry, or report behavior.

The dependency direction is:

```text
application-model (JDK only)
        ↑
application-mapper → selector-live → selector-engine
        ↑
application-tooling (persistence, code generation, drift, AI contracts)
```

The three application modules remain unpublished while the selector modules they consume are internal. The vertical slice is compile- and browser-tested from the source reactor; publication is a separate API/product decision for a later release.

## Why a neutral model

The browser is an observation source, not the persistence format. A mapper observation becomes a bounded page snapshot, each meaningful live element is analyzed by `LiveCandidateAnalysisService`, and only the neutral projection is merged into `ApplicationModel`. The model stores neither `WebDriver`, `WebElement`, Selenium remote IDs, raw DOM, form values, cookies, nor credentials.

`ApplicationModel` starts at schema version 1. IDs use domain-separated SHA-256 over canonical semantic inputs. Lists and maps are sorted before publication. Provenance distinguishes observed, live-validated, inferred, user-declared, static-audit, and AI-proposed information.

## Selector Intelligence reuse

There is deliberately no mapper selector generator or mapper score. `LiveCandidateAnalysisService` already owns bounded target metadata capture, calls `CandidateGenerator`, applies stability policy/evidence, resolves each candidate once, performs same-target comparison, and delegates order/recommendation to `CandidateRanker`. The mapper projects the ranked result and rejects candidates that are not valid for the intended usage.

## Mapping modes

- `CURRENT_PAGE`: one synchronous observation, no navigation.
- `GUIDED`: the caller performs application actions and invokes `observe()` after meaningful states.
- `SAFE_EXPLORE`: bounded navigation only through an explicit action policy. It is opt-in; unknown or mutating actions are denied or require caller approval.

V1 keeps the current browsing context. Open shadow roots are discovered to a configured depth. Closed roots are reported as unsupported. Frames and windows are never switched implicitly.

## Code generation and regeneration

The generator owns only `Generated*Page` files. A one-time user extension class is created only when absent and is never overwritten. Generated members include stable model IDs in comments/metadata so drift and later repair can correlate model element, source member, and runtime evidence.

## AI boundary

The AI layer is provider-neutral and produces bounded context packs and structured output contracts. Context slicing includes only relevant pages and transition closure and records exclusions and completeness. The implementer contract defaults to Page Object APIs only; missing behavior produces `PAGE_OBJECT_CAPABILITY_MISSING`, not an invented raw selector.

No AI SDK, network client, or autonomous scheduler is part of these modules.

## User documentation

The source-only development contract is documented under [Application mapping](../advanced/application-mapping/index.md), with a separate [guided tutorial](../advanced/application-mapping/tutorial.md), [Page Object generation](../advanced/application-mapping/page-object-generation.md), [drift and security guidance](../advanced/application-mapping/drift-security-troubleshooting.md), and [AI test engineering](../ai/test-engineering.md). Every page states that these 0.5.0 development modules are not Maven 0.4.0 artifacts.
