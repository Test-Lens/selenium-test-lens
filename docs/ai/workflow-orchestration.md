# AI test workflow orchestration

Test Lens 0.5.0 development tooling connects a verified application model and existing Page Object API to a provider-neutral, auditable test-engineering workflow. The deterministic layers decide what source exists, which elements it represents, what context is relevant, whether proposed Java compiles, and what the targeted test actually reported. An external agent reasons only over that bounded evidence.

!!! warning "Unpublished development API"
    The workflow classes are source-only in `selenium-test-lens-application-tooling`. They are not part of the published 0.4.0 artifacts. No OpenAI, Anthropic, Gemini, or other provider SDK is included, and Test Lens does not contact an AI service.

## Prepare source-aware context

The source-aware `ContextSlicer` overload accepts the `ApplicationModel`, `ExistingProjectIndex`, and evidence-bearing `PageObjectCorrelation`. It projects only relevant:

- pages, states, elements, and transition closure;
- Page Object classes and API methods;
- selector declaration references and value fingerprints, not raw locator values;
- JUnit 5 or TestNG tests that call the selected methods;
- inclusion and exclusion reasons.

Separate budgets limit application pages and elements, Page Object classes, methods, declarations, existing tests, strings, scope decisions, and serialized characters. If any bound is reached, `AgentContextPack.completeness()` becomes `PARTIAL` and records the reason. A caller must not treat a partial pack as proof that no other source or coverage exists.

Each included item explains why it is present, for example `CORRELATED_TO_INCLUDED_PAGE`, `USES_CORRELATED_ELEMENT`, or `CALLS_RELEVANT_PAGE_OBJECT_METHOD`. Excluded scope decisions explain limits or `OUTSIDE_TASK_SUBGRAPH`. This makes context size and relevance reviewable instead of hiding truncation.

Source excerpts are not included by default. If host tooling needs one, it should read only the recorded logical path and bounded source range, verify a content fingerprint, apply central redaction, and record why the excerpt is necessary. Never attach a whole source tree as a convenience.

## Provider-neutral execution boundary

`AgentExecutor` receives an `AgentCommand` containing a run ID, a role, bounded artifact envelopes, and instructions, then returns a structured result payload. Production provider integration belongs outside these modules. Tests use `ScriptedAgentExecutor`; there is no network dependency.

Available roles include:

| Role | Responsibility |
|---|---|
| `TEST_ARCHITECT` | Produce a `TestPlan` from requirement, application graph, correlated APIs, and relevant existing coverage. |
| `SCENARIO_DESIGNER` | Refine justified scenarios without inventing unknown product behavior. |
| `TEST_IMPLEMENTER` | Produce a test proposal using existing Page Object APIs only by default. |
| `TEST_VERIFIER` | Interpret bounded compile, execution, and Test Lens evidence. |
| `STABILIZER` | Classify failure and propose the smallest evidence-backed change. |
| `CODE_REVIEWER` | Review policy, isolation, lifecycle, and maintainability. |
| `UNIT_TEST_AGENT` | Add focused tests for a production method, parser/correlation rule, or workflow branch without taking ownership of production code. |

An orchestrator should send each role only its inputs. A stabilizer does not need every Page Object, and a unit-test agent does not need authentication state or browser evidence.

## Auditable state machine

`TestEngineeringRequest` wraps the requirement immediately with accepted paths, framework and test engine, target module/class/scenario, scope, and execution policy. `TestEngineeringWorkflow` is a pure reducer over `TestEngineeringRun`; it does not invoke an agent or mutate source itself.

```text
CREATED
  -> CONTEXT_PREPARED
  -> PLAN_REQUESTED
  -> PLAN_READY
  -> IMPLEMENTATION_REQUESTED
  -> IMPLEMENTATION_READY
  -> compile
       FAIL -> COMPILE_FAILED -> bounded correction
       PASS -> EXECUTION_READY
  -> targeted run
       PASS -> REVIEW_REQUESTED -> SUCCESS or REJECTED
       FAIL -> EXECUTION_FAILED -> FAILURE_CLASSIFIED
            -> REPAIR_PROPOSED -> AWAITING_TRUSTED_APPLY
            -> RERUN_REQUIRED -> IMPLEMENTATION_READY
```

`BLOCKED`, `FAILED`, `REJECTED`, and `NEEDS_HUMAN_REVIEW` are terminal. `WorkflowPolicy` independently bounds implementation corrections, reruns, repair proposals, artifacts, and audit entries. Reaching a correction, rerun, or repair bound requests human review; there is no infinite self-healing loop.

Every `ArtifactEnvelope` belongs to one run, has a digest, and can name parent artifacts. The reducer rejects duplicate artifact IDs, unknown parents, and cross-run artifacts. `TestEngineeringRun` records state transitions and metrics for agent calls, compilations, executions, corrections, reruns, and repairs.

## Page Objects only is enforced

The implementer instruction is backed by `GeneratedTestPolicyValidator`. Its default policy rejects Java test patches containing:

- `By.*` declarations or calls;
- `driver.findElement` or `findElements`;
- `Thread.sleep`;
- direct `JavascriptExecutor` or `executeScript` use;
- retry annotations and retry-like loops;
- changes outside request-approved paths or in protected production/build paths.

The scanner masks comments, string literals, character literals, and text blocks before token checks, so an explanatory string does not become a false selector violation. The validator is intentionally a bounded test-patch policy, not a replacement Java compiler or security sandbox.

If the required Page Object operation is absent, the result is `PageObjectCapabilityMissing`. A `PageObjectExtensionProposal` may describe a mechanical method using an already-correlated declaration. If a new locator is required, the request is delegated to Selector Intelligence; the agent does not paste a guessed CSS or XPath into the test.

## Compile and run only the target

`TargetedJavaCompiler` compiles proposed in-memory source with the JDK compiler. Each source unit has a relative path and expected current-content fingerprint. A mismatch returns `SOURCE_PRECONDITION_FAILED`; it never compiles against a silently changed source. Diagnostics are bounded and the compile step must pass before execution.

`TargetedTestExecutor` is an injected host boundary. It receives the run ID, test class, runner selectors, and a positive timeout, and returns a bounded result. The host implementation owns JUnit or TestNG invocation and collection of Test Lens trace, assertion, runtime, screenshot-reference, and selector-diagnostic evidence. It should run the requested test or scenario, not the complete reactor by default.

## Classify before repair

`EvidenceFailureClassifier` is conservative. It distinguishes compilation, authentication, product mismatch, selector instability, synchronization, Page Object capability, and unknown evidence; the contract also supports test logic, assertion expectation, test data, environment, and page-model drift categories. Counter-evidence remains visible.

A product mismatch must not be "fixed" by weakening the test. Synchronization evidence should lead to the correct condition, not a blanket sleep or timeout increase. Selector repair is allowed only after a correlated selector failure has been classified as `SELECTOR_INSTABILITY`.

`SelectorRepairPlanner` then requires:

1. one unambiguous source declaration correlation;
2. a replacement produced by existing Selector Intelligence;
3. live candidate analysis provenance;
4. same-target evidence;
5. uniqueness and verified selector quality;
6. source impact for affected methods and tests.

The resulting `RepairProposal` is always `PROPOSE_ONLY`. It names the declaration, old and new candidate IDs, classification and drift references, evidence, risks, affected tests, and verification plan. `ControlledSourceApplier` is a separate trusted-host operation requiring an allowed path, explicit approval, and matching source fingerprint. Browser-side consent never applies a source patch.

## Workflow artifacts and security gate

`WorkflowArtifactStore` accepts only the known JSON artifact names for request, context, plan, implementation, execution, classification, repair, review, and metrics. Writes are rooted under a validated run ID, atomic where supported, content-addressed, and bounded by explicit run retention.

Before storage or provider transport, `AgentArtifactSecurityGate` requires enabled central `RedactionPolicy`, rejects text that would still change under that policy, and checks caller-provided secret canaries. References to auth/storage state, cookies, page source, console or raw runner output, screenshots, and videos are rejected by default. The gate does not inspect image pixels; image and video evidence therefore remain outside agent context unless a separate trusted process establishes a safe projection.

Do not include passwords, cookie values, bearer/JWT tokens, authorization headers, CSRF or client secrets, local/session storage, environment values, system properties, saved auth-state files, or raw stack/log bundles in workflow artifacts. Use a dedicated non-production account, keep reusable auth state outside the repository with restricted permissions and retention, validate the application origin, and fail closed if authentication state is uncertain.

See [Existing Page Object correlation](../advanced/application-mapping/existing-page-objects.md) and the [guided tutorial](../advanced/application-mapping/tutorial.md#continue-from-an-existing-selenium-project).
