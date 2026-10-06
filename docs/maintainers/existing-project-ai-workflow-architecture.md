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

S12 adds two certifications without changing that ownership boundary: a real-browser, hand-written-Page-Object repair flow and a provider-neutral subprocess runner. The runner transports contracts; it does not take ownership of context selection, selector choice, validation, execution, or source mutation.

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
- strict external-process schemas, size/time/environment bounds, and workflow reports.

This layer is canonical and conservative. `UNKNOWN`, `PARTIAL`, `AMBIGUOUS`, and `CONFLICT` are valid results. It does not need an LLM SDK or network access.

### AI reasoning layer

The reasoning layer may:

- turn a requirement and confirmed graph slice into a test plan;
- choose justified scenarios and existing Page Object methods;
- propose test source that satisfies the Page-Objects-only policy;
- interpret bounded evidence and suggest the smallest repair;
- review the proposed changes.

It may not claim new application capabilities, invent locators, silently apply source changes, weaken assertions to obtain a pass, or treat every failure as flaky Selenium.

The external process receives immutable role instructions alongside the bounded input and expected result schema. Prompt instructions are defense in depth: the deterministic generated-test validator, compiler, targeted runner, repair planner, and reviewer remain authoritative after the agent returns.

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

There is no additional runtime work when these tooling APIs are unused: no scanner initialization, browser command, model allocation, source parse, agent process, or background WebDriver thread.

The subprocess implementation, protocol codec, Codex CLI profile, coordinator, reports, and trusted repair apply remain in `selenium-test-lens-application-tooling`. They do not enter core, overlay, application-model, mapper runtime, or the published 0.4.0 artifacts.

## Security decision

Authentication state is not AI context. The workflow fails closed unless central `RedactionPolicy` is enabled and the outgoing text is already redacted. Secret canaries and forbidden references are checked before persistence or provider transport. Auth/storage state, cookie values, bearer/JWT tokens, authorization data, environment and system properties, raw runner logs, page source, console output, screenshots, and videos remain outside the default pack.

Saved login state is a replayable credential, not redacted diagnostic evidence. It must stay outside the repository under trusted-host access and retention controls. Agents receive semantic preconditions such as `VALID_USER`, never the underlying password or token.

The subprocess boundary clears the inherited environment and restores only explicitly allowlisted, non-secret names. Inputs use standard input, output uses a bounded stream or isolated result file, source excerpts require logical-path and content-fingerprint checks, and both directions pass redaction/canary validation. Provider CLI authentication is discovered by the CLI outside serialized context; it is not copied into workflow artifacts.

The configured executable remains a trusted-host boundary. A JDK `ProcessBuilder` can restrict arguments, current directory, inherited environment, and exposed excerpts, but it cannot portably enforce a filesystem read allowlist against the child process. Deployments that execute an untrusted binary must add an OS/container sandbox; documentation must not describe staging alone as protection from arbitrary host-path reads.

## Source mutation decision

Agent output is a proposal. Generated test source is restricted to request-approved paths and validated before compile. Page Object repair remains `PROPOSE_ONLY`; applying it requires a separate trusted-host call.

`TrustedRepairApplier` accepts only an exact or strong correlation and Selector Intelligence replacement evidence that is live, verified, same-target, unique, and stable. It verifies the allowed logical source root, source-file and declaration fingerprints, exact source range, old strategy/value fingerprint, one old expression, and absence of symlink traversal before an atomic guarded write. A browser session or agent response cannot set the explicit trusted-apply flag.

The certification fixture establishes a baseline exact correlation for a hand-written locator, changes the browser application so the old ID genuinely stops resolving, observes a failed Lens trace, remaps the same model element, obtains the replacement through the existing live Selector Intelligence pipeline, applies the reviewed proposal to a temporary copy, recompiles, and reruns the same test successfully in Chrome and Firefox. It therefore certifies the composed flow, not merely individual model objects.

## External agent decision

`AgentExecutor` remains the sole provider-neutral workflow boundary. `ScriptedAgentExecutor` supports deterministic CI; `ExternalAgentRunner` invokes an explicitly configured subprocess with `ProcessBuilder`, strict `TEST_LENS_EXTERNAL_AGENT_V1` JSON, role-specific JSON Schema, bounded I/O and timeout, isolated staging, and no shell interpolation. Failures are typed and never trigger an implicit scripted fallback.

The first concrete profile targets a locally installed Codex CLI in non-interactive, ephemeral, read-only mode. The profile is an adapter at the tooling boundary, not a vendor SDK and not a `CodexWorkflowEngine`. CI does not require it; the real dogfood integration is opt-in and reports `NOT RUN` when its preconditions are absent.

`AgentWorkflowCoordinator` composes architect, implementer, compile, targeted execution, classification/stabilization, and reviewer steps over the existing reducer. Its `CompiledTargetedTestExecutor` receives the exact immutable output produced by the successful compilation, preventing a same-named stale class from substituting for the agent proposal. `WorkflowReport` exposes deterministic JSON and human-readable summaries with ordered steps, evidence references, byte counts, separate deterministic/external durations, and bounded attempt/repair counters. Reports contain no chain-of-thought.

## Consequences and limitations

- Context packs are smaller and traceable but can be `PARTIAL` when bounds or unresolved Java semantics are encountered.
- The usage projection handles common structural calls and inheritance facts, not every reflection or dynamic-dispatch case.
- Correlation may require a reviewed stable-ID override; names alone are insufficient.
- Remote services, vendor SDKs, autonomous browsing, IDE integration, deep compiler analysis, and silent repair remain out of scope.
- Scripted workflow tests prove orchestration semantics but not a real provider; the opt-in Codex dogfood run is reported separately.

## Codex dogfooding record

Repository development used bounded delegation for the following roles: source analyzer/implementer for the same-parse-pass index, workflow implementer, unit-test agent, browser-test agent, documentation agent, and independent reviewer. Each role received task-local instructions, owned paths, acceptance criteria, and the minimum repository context needed for its assignment. No role received secrets, authentication state, or a provider credential.

This is evidence about the repository-development process, not a stable product claim. Deterministic workflow contracts remain exercised in CI with `ScriptedAgentExecutor`. The source-only S12 tooling also provides a real local Codex subprocess profile. `RealCodexWorkflowIT` certifies structured architect/implementer/reviewer execution and generated-bytecode handoff; `RealCodexExistingPageObjectWorkflowIT` additionally executes the generated code through an existing Page Object and a Test Lens-observed Chrome browser, then writes a JSON `WorkflowReport`. Both invoke the installed CLI rather than a Codex API SDK. A skipped or unavailable opt-in run must be recorded as `NOT RUN`, never inferred from scripted tests.

## Related documentation

- [Application Mapper architecture](application-mapper-architecture.md)
- [Existing Page Object correlation](../advanced/application-mapping/existing-page-objects.md)
- [AI workflow orchestration](../ai/workflow-orchestration.md)
- [Existing-project tutorial](../advanced/application-mapping/tutorial.md#continue-from-an-existing-selenium-project)
