# ADR: deterministic source correlation before AI reasoning

## Status

Accepted for the unpublished 0.5.0 development vertical slice. This decision does not publish the application modules or change Maven 0.4.0.

## Context

An existing Selenium project already contains Page Objects, components, helpers, tests, base classes, locator declarations, and lifecycle conventions. Asking a model to rediscover those facts from an unbounded repository or raw DOM creates three avoidable risks:

- it can invent or bypass selectors;
- it can miss existing coverage and project conventions;
- it can receive far more source and authentication-sensitive data than the task needs.

The earlier Application Mapper stage established a deterministic browser-to-`ApplicationModel` and model-to-generated-Page-Object path. This stage must connect that model to existing source and execute a real test-engineering workflow without turning an AI provider into the source of truth.

## Decision

Keep deterministic observation, source analysis, validation, compilation, execution, and evidence projection separate from AI reasoning:

```text
Browser -> ApplicationModel
Java source -> ExistingProjectIndex + bounded usage edges
ApplicationModel + source index -> evidence-bearing correlation
correlation + requirement -> bounded AgentContextPack

AgentContextPack -> external role reasoning -> structured proposal
structured proposal -> deterministic policy validation -> compile -> targeted run
Test Lens evidence -> classification -> optional PROPOSE_ONLY repair
```

### Deterministic layer

The deterministic layer owns:

- Selector Audit-backed declaration discovery and stable declaration identity;
- source-safe `ExistingProjectIndex` projections and content-derived fingerprint;
- bounded test-to-method-to-declaration edges;
- application/source correlation states, evidence, conflicts, and overrides;
- context inclusion/exclusion decisions, redaction, and budgets;
- generated-test policy checks;
- source content preconditions and targeted compilation;
- targeted runner boundary and Test Lens evidence projection;
- failure categories supported by explicit signals;
- Selector Intelligence replacement evidence;
- workflow states, attempt bounds, artifact ancestry, retention, and trusted apply boundary.

This layer is canonical and conservative. `UNKNOWN`, `PARTIAL`, `AMBIGUOUS`, and `CONFLICT` are valid results. It does not need an LLM SDK or network access.

### AI reasoning layer

The reasoning layer may:

- turn a requirement and confirmed graph slice into a test plan;
- choose justified scenarios and existing Page Object methods;
- propose test source that satisfies the Page-Objects-only policy;
- interpret bounded evidence and suggest the smallest repair;
- review the proposed changes.

It may not claim new application capabilities, invent locators, silently apply source changes, weaken assertions to obtain a pass, or treat every failure as flaky Selenium.

## Why AI never owns selector generation

Selector choice already has a dedicated evidence pipeline:

```text
TargetMetadataCollector
  -> CandidateGenerator
  -> stability and project policy
  -> live resolution
  -> SameTargetComparator
  -> CandidateRanker
```

That pipeline can prove syntax, resolution, same target, uniqueness, stability, and ranking. A language model guessing `By.cssSelector(...)` cannot provide equivalent evidence and can accidentally target a different element. Therefore:

- tests use existing Page Object APIs by default;
- a missing method produces `PageObjectCapabilityMissing`;
- a method extension can reuse only an already-correlated declaration;
- a missing or stale selector returns to Selector Intelligence;
- a replacement is represented by a reviewable `RepairProposal`, never silent healing.

## Module and dependency placement

`selenium-test-lens-selector-tooling` owns JavaParser-backed source discovery because it already owns Selector Audit declaration identity. `selenium-test-lens-application-tooling` consumes its neutral index for correlation, context slicing, workflow, compilation, and repair projections. JavaParser does not move into application-model, application-mapper, selector-engine, core, overlay, or normal runtime.

There is no additional runtime work when these tooling APIs are unused: no scanner initialization, browser command, model allocation, source parse, or background WebDriver thread.

## Security decision

Authentication state is not AI context. The workflow fails closed unless central `RedactionPolicy` is enabled and the outgoing text is already redacted. Secret canaries and forbidden references are checked before persistence or provider transport. Auth/storage state, cookie values, bearer/JWT tokens, authorization data, environment and system properties, raw runner logs, page source, console output, screenshots, and videos remain outside the default pack.

Saved login state is a replayable credential, not redacted diagnostic evidence. It must stay outside the repository under trusted-host access and retention controls. Agents receive semantic preconditions such as `VALID_USER`, never the underlying password or token.

## Source mutation decision

Agent output is a proposal. Generated test source is restricted to request-approved paths and validated before compile. Page Object repair remains `PROPOSE_ONLY`; applying it requires a separate trusted-host call with an allowed path, explicit approval, and matching content fingerprint. A browser session or agent response cannot authorize source mutation.

## Consequences and limitations

- Context packs are smaller and traceable but can be `PARTIAL` when bounds or unresolved Java semantics are encountered.
- The usage projection handles common structural calls and inheritance facts, not every reflection or dynamic-dispatch case.
- Correlation may require a reviewed stable-ID override; names alone are insufficient.
- Provider adapters, remote services, autonomous browsing, IDE integration, deep compiler analysis, and silent repair remain out of scope.
- Workflow tests can use a scripted executor without proving the behavior of any real AI provider.

## Codex dogfooding record

Repository development used bounded delegation for the following roles: source analyzer/implementer for the same-parse-pass index, workflow implementer, unit-test agent, browser-test agent, documentation agent, and independent reviewer. Each role received task-local instructions, owned paths, acceptance criteria, and the minimum repository context needed for its assignment. No role received secrets, authentication state, or a provider credential.

This is evidence about the repository-development process, not a product claim. The product workflow did not invoke a Codex API. Deterministic workflow contracts are exercised in CI with `ScriptedAgentExecutor`; any real provider adapter remains external to these modules.

## Related documentation

- [Application Mapper architecture](application-mapper-architecture.md)
- [Existing Page Object correlation](../advanced/application-mapping/existing-page-objects.md)
- [AI workflow orchestration](../ai/workflow-orchestration.md)
- [Existing-project tutorial](../advanced/application-mapping/tutorial.md#continue-from-an-existing-selenium-project)
