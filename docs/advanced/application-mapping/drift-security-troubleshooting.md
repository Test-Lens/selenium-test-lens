# Application drift, security, and troubleshooting

!!! warning "Development availability"
    Application drift, model history, and repair contracts are source-only 0.5.0 development tooling. They are not part of the published Maven 0.4.0 artifacts.

## Detect semantic drift

Persist bounded history so a later observation can be compared with a known model:

```java
Path root = Path.of(".test-lens").toAbsolutePath().normalize();
ApplicationModelStore store = new ApplicationModelStore();

store.write(root, Path.of("application/application-model.json"), model);
ApplicationModelStore.HistoryResult history = store.recordHistory(
        root.resolve("application/history"),
        model,
        10
);
```

History entries are content-addressed and retention is explicit. They do not grow without a configured bound.

```java
ApplicationDrift drift = new ApplicationModelDiffer().compare(before, after);
List<PageObjectDiff> sourceImpact =
        new PageObjectGenerator().diff(before, after, generationOptions);
```

Drift can report added or removed pages, states, elements, and transitions; renamed elements; changed transitions; changed selectors; and selectors that became less or more stable. Correlation uses model identity and semantics, not only locator text. When a source declaration cannot be correlated, the result is `NO_SOURCE_CORRELATION`, not a guessed match.

`similar` evidence from Selector Intelligence may support review or repair, but it never proves that two elements are the same target.

## Repair is reviewable

A selector-related failure is not automatically a flaky selector. Classify runtime evidence first. If the cause is `SELECTOR_INSTABILITY`, obtain a replacement through the existing candidate generation, live validation, same-target comparison, and ranking pipeline.

The tooling contract permits only `RepairProposal.ApplicationPolicy.PROPOSE_ONLY`. A proposal records what changed, why, old and new selector, same-target and stability evidence, and affected tests. It is not silent self-healing and is not automatically applied.

## Security boundary

The model is safer to share than raw DOM because the scanner deliberately excludes form values, password and textarea contents, cookies, authorization data, session identifiers, remote element IDs, and raw page source. Text and URL projections use the caller's central [`RedactionPolicy`](../../security/redaction.md), and `ContextSlicer` applies redaction again before writing agent context.

This is not a guarantee that arbitrary personal data can be inferred and removed. Avoid secrets in test labels, accessible names, URLs, custom attributes, application names, requirements, conventions, and user overrides. Review model and context artifacts before sending them outside the trusted environment. Screenshot and video boundaries remain separate from text redaction.

Authentication is caller-owned. Restore or perform login before mapping; do not store credentials, tokens, cookies, or reusable authentication state in `ApplicationModel`.

## Troubleshooting

### Coverage is PARTIAL

Read `MappingObservation.limitations()` and `ApplicationModel.coverage()`. Typical causes are discovery, actionable-element, selector-analysis, page, state, transition, or shadow-depth limits. Increase a specific limit only after reviewing cost and data exposure. Do not relabel partial coverage as complete.

### An element has no generated method

Inspect `ElementModel.selectorQuality()` and generator warnings. `UNAVAILABLE` and, by default, `REVIEW_REQUIRED` selectors are not generated. Fix the application test contract or selector policy/evidence, then remap; do not paste an unvalidated raw selector into generated code.

### The same route creates several states

That may be correct. Dialogs and structurally distinct SPA states share a page identity but retain separate `PageState` records. If observations that are truly the same page receive different identities, use reviewable `ApplicationOverrides.pageIdentityGroupsByObservationFingerprint` entries.

### Dynamic routes create duplicate pages

Inspect normalized URL patterns and page-identity evidence. Page identity includes more than the URL. Use an identity-group override only after confirming that landmarks and structure represent the same page.

### Shadow content is missing

Only open shadow roots are traversed, and traversal stops at `maxShadowDepth`. Closed roots are unsupported and reported. Expose a stable public testing surface or map the surrounding component; do not bypass a closed root with injected scripts.

### Frame content is missing

The mapper observes the current browsing context. Switch to the frame explicitly, call `observe()`, and restore the parent context yourself. There is no implicit random frame or window traversal.

### Safe exploration did nothing

Verify that mode is `SAFE_EXPLORE`, depth and action count are non-zero, the candidate is a same-origin link that does not open a new window, its URL is not denied, and the action policy returns `ALLOW`. `REQUIRE_EXPLICIT_APPROVAL` is intentionally not executed by `safeExplore()`.

### A mapper call failed

Use `MappingException.code()` rather than parsing message text. Codes distinguish stale targets, browser-script failures, blocked crawl actions, unretained page state, identity ambiguity, unsupported closed shadow roots, serialization or generation conflicts, and selector review requirements. Preserve the original failure and its evidence.
