# AI test engineering foundation

Test Lens prepares deterministic, provider-neutral inputs for test-engineering agents. It does not embed an OpenAI, Anthropic, Gemini, or other model SDK, and it does not run an autonomous agent scheduler.

!!! warning "Development availability"
    These contracts are source-only 0.5.0 development work in `selenium-test-lens-application-tooling`. They are not part of Maven 0.4.0. The existing [AI-assisted integration builder](../ai-assisted-integration.md) remains a separate documentation tool that generates integration instructions.

The AI boundary begins only after deterministic observation and generation:

```text
ApplicationModel + correlated existing/generated Page Object API + requirement + conventions
    -> ContextSlicer
    -> AgentContextPack
    -> PromptPackRenderer
    -> AgentExecutor (scripted or bounded external process)
    -> structured result contract
```

## Agent Context Packs

`ContextSlicer` selects pages from explicit stable IDs and follows bounded transition dependencies. It includes only the corresponding states, elements, transitions, generated Page Object API summaries, and test conventions. It records every included and excluded scope decision and reports `COMPLETE_FOR_REQUESTED_SCOPE`, `PARTIAL`, or `FAILED`.

The source-aware overload additionally consumes an `ExistingProjectIndex` and `PageObjectCorrelation`. It adds only evidence-backed Page Object classes, methods, declaration fingerprints, and relevant JUnit 5 or TestNG tests. Raw selector values and full source files are not part of that projection. See [Existing Page Object correlation](../advanced/application-mapping/existing-page-objects.md).

Budgets cover pages, elements, transitions, transition depth, states, state-element references, Page Object APIs, conventions, individual strings, total characters, and scope decisions. If a budget is reached, the pack is partial; contracts are never silently truncated by `PromptPackRenderer`. Redaction is applied before context leaves this boundary.

This makes token efficiency an observable property. A login requirement should include the login page, relevant error state, required transition closure, and Page Object API, not every page, raw DOM node, repository file, and historical test.

## Specialized roles

`PromptPackRenderer.Role` defines six narrowly scoped prompt packs:

| Role | Structured output | Non-negotiable boundary |
|---|---|---|
| `TEST_ARCHITECT` | `TestPlan` | Does not invent pages, permissions, states, or capabilities absent from context. |
| `TEST_SCENARIO_DESIGNER` | `TestPlan.TestScenario[]` | Covers justified happy, negative, boundary, and transition cases; unknown remains unknown. |
| `TEST_IMPLEMENTER` | `TestImplementationProposal` | Page Objects only by default; no invented CSS, XPath, `By`, or `findElement`. Missing behavior becomes `PageObjectCapabilityMissing`. |
| `TEST_VERIFIER` | `TestExecutionResult` | Keeps compile, targeted execution, trace, screenshots, assertions, runtime events, and selector diagnostics as distinct evidence. |
| `TEST_STABILIZER` | `FailureClassification` and optional `RepairProposal` | Does not assume selector failure, add blind sleeps/retries, or apply silent healing. Selector repair requires Selector Intelligence evidence. |
| `TEST_REVIEWER` | `CodeReviewResult` | Reviews Page Object reuse, raw selectors, sleeps, retries, assertions, Test Lens API use, maintainability, and isolation. |

All result models carry a `ContractHeader` with schema version, status, evidence, limitations, and structured confidence. `TestPlan` additionally records scenarios, ordered steps, expected results, required pages/states/elements, test data, and risk areas. These are machine-readable contracts, not free-form essays.

## Implementer safety rule

The default selector access policy is `PAGE_OBJECTS_ONLY`. If an Application Model contains an element but its generated Page Object lacks an operation, the implementer reports the missing capability rather than bypassing the model:

```text
PageObjectCapabilityMissing
  pageId: ...
  elementId: ...
  requiredCapability: clickLoginButton
  reason: required Page Object API is absent
```

Raw Selenium is possible only when a caller deliberately chooses `RAW_SELECTORS_EXPLICITLY_ALLOWED`; it is not the default and does not relax selector validation elsewhere.

## Verification and stabilization loop

```text
TestPlan -> implementation proposal -> compile -> targeted execution
    -> PASS: review
    -> FAIL: classify -> inspect bounded evidence -> propose minimal repair
             -> explicit review/apply -> targeted rerun
```

The caller bounds attempts. The contracts do not implement an infinite self-healing loop.

`FailureClassification.Category` separates product defects, test logic defects, selector instability, timing/synchronization, assertion expectations, test data, environment, authentication, page-model drift, and unknown causes. A stabilizer must use Test Lens runtime evidence and the relevant model slice instead of receiving the entire application or repository.

When selector instability is proven, replacement follows Selector Intelligence: candidate generation, live validation, same-target comparison, stability evidence, and ranking. `RepairProposal` is always `PROPOSE_ONLY` and names affected tests. It never silently changes a selector because a test happened to pass afterward.

## Security and transport

An `AgentContextPack` is safer than raw DOM, but it is still an artifact that may contain application names, accessible labels, requirements, conventions, and URLs. `ContextSlicer` applies the configured `RedactionPolicy`, including URL redaction, before producing the pack. Review the pack before passing it to an external provider.

The development tooling now includes an opt-in `ExternalAgentRunner` implementation of the provider-neutral `AgentExecutor` boundary. Invoking that runner explicitly starts a configured local process; normal Test Lens runtime and context construction still perform no provider or network call. The runner does not contain a vendor SDK and does not choose a provider automatically.

Before starting a process, the runner serializes a bounded, versioned command, applies the central redaction and canary gate, and stages only explicitly supplied, fingerprint-checked source excerpts. It clears the child environment and restores only non-secret names from an explicit allowlist. The child receives input through standard input, not shell interpolation, and must return exactly one JSON object matching the role-specific schema. Invalid, oversized, timed-out, or unsafe results fail closed.

Storage, provider selection, provider authentication, network transport, retention, and approval remain trusted-host responsibilities. Never place credentials, cookies, reusable auth state, authorization headers, or unreviewed secrets in a requirement or convention string. The local Codex profile relies on the CLI's own authentication state outside serialized workflow artifacts; it does not copy credentials into an `AgentContextPack`.

See the [guided mapping tutorial](../advanced/application-mapping/tutorial.md) for the complete model-to-context flow.

For the auditable state machine, compile and targeted-run boundaries, deterministic generated-test policy, trusted repair apply, workflow artifacts, and auth-state gates, continue with [AI workflow orchestration](workflow-orchestration.md).
