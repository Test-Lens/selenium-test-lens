# AI test workflow orchestration

Test Lens 0.5.0 tooling connects a verified application model and existing Page Object API to a provider-neutral, auditable test-engineering workflow. The deterministic layers decide what source exists, which elements it represents, what context is relevant, whether proposed Java compiles, and what the targeted test actually reported. An external agent reasons only over that bounded evidence.

!!! info "Provider-neutral tooling"
    The workflow is opt-in tooling. No OpenAI, Anthropic, Gemini, or other provider SDK is included, and Test Lens does not contact an AI service unless an explicitly selected external executor does so.

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

`AgentExecutor` receives an `AgentCommand` containing a run ID, a role, bounded artifact envelopes, and instructions, then returns a structured result payload. `ScriptedAgentExecutor` keeps CI deterministic; passing that fixture proves the deterministic gates and state transitions, not the quality of a real AI model. `ExternalAgentRunner` is the tooling-side implementation for an explicitly configured subprocess; neither implementation adds a provider SDK or a network dependency to Test Lens runtime.

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

### Bounded external process

`AgentProfile` is trusted-host configuration, not agent input. It pins an absolute executable, an argument list, one role, timeout, input/output/diagnostic byte limits, an environment allowlist, and either standard-output or file output transport. Arguments are passed directly to `ProcessBuilder`; requirements and context are written to standard input and never interpolated into a shell command.

For every call, `ExternalAgentRunner`:

1. checks that command and profile roles match;
2. serializes `TEST_LENS_EXTERNAL_AGENT_V1` and validates redaction, canaries, references, and the input bound;
3. creates an ephemeral staging directory below an absolute, non-symlink root and applies owner-only POSIX permissions where supported;
4. optionally writes only allowlisted logical source excerpts whose content fingerprints match;
5. clears the child environment and copies only allowed, non-secret variable names;
6. bounds process time, standard output, standard error, result size, and cleanup;
7. validates the result against the role-specific JSON Schema and strict decoder;
8. applies the output security gate before creating an `ArtifactEnvelope`.

The failure codes are intentionally distinct: `AGENT_NOT_AVAILABLE`, `AGENT_TIMEOUT`, `AGENT_PROCESS_FAILED`, `AGENT_OUTPUT_INVALID`, and `AGENT_CONTEXT_REJECTED`. The runner does not silently fall back to `ScriptedAgentExecutor`.

Malformed JSON, additional or missing fields, a wrong schema version, an unexpected result type, truncated output, or an output security violation are `AGENT_OUTPUT_INVALID`. The supported result type is selected by role, for example `TEST_PLAN` for `TEST_ARCHITECT`, `TEST_IMPLEMENTATION_PROPOSAL` or a capability result for `TEST_IMPLEMENTER`, and `CODE_REVIEW_RESULT` for `CODE_REVIEWER`. Free-form prose is not a successful result.

### Local Codex profile

`CodexCliDetector` locates a directly executable native `codex` binary without invoking a shell. `CodexCliProfiles.readOnly(...)` supplies the verified non-interactive invocation:

```text
codex exec --ephemeral --ignore-user-config --ignore-rules
  --sandbox read-only --color never --skip-git-repo-check
  -C <isolated-staging-directory>
  --output-schema <generated-schema.json>
  --output-last-message <bounded-result.json> -
```

The profile deliberately runs in a dedicated staging directory rather than the consumer project. Its default environment allowlist contains only `CODEX_HOME` plus the minimum system-root and temporary-directory locations; it does not forward `HOME`, `USERPROFILE`, shell variables, or secret-looking names. Provider authentication remains owned by the installed CLI and is never serialized into the command or workflow report.

This is process-boundary hardening, not a portable operating-system read sandbox. `ProcessBuilder` cannot prevent a configured executable from naming and reading an arbitrary host path. The executable and its provider sandbox are therefore trusted host configuration; use an OS/container sandbox when untrusted executables require enforceable filesystem read isolation. Lens itself stages only fingerprint-checked excerpts and exposes no API for arbitrary file reads.

This is the first concrete external profile, not a Codex-specific workflow engine. Other tools can implement the same strict subprocess contract.

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

Every `ArtifactEnvelope` belongs to one run, has a digest, and can name parent artifacts. The envelope snapshots supported payloads before calculating that digest: byte arrays are copied on input and output, while the immutable workflow contract records are retained. Unknown objects, arrays, collections, and maps are rejected instead of relying on a mutable object's `toString()`. The public constructor and `create(...)` factory enforce the same rule. This deliberately narrows the otherwise generic type parameter to the payload types used by the workflow; adding another payload type requires an explicit immutability decision.

The reducer rejects duplicate artifact IDs, unknown parents, and cross-run artifacts. `TestEngineeringRun` records state transitions and metrics for agent calls, compilations, executions, corrections, reruns, and repairs.

`AgentWorkflowCoordinator` connects this reducer to an injected `AgentExecutor`, `TargetedJavaCompiler`, targeted-test boundary, failure classifier, and stabilizer. It requests architect, implementer, and reviewer results through the same executor abstraction, enforces `PAGE_OBJECTS_ONLY`, retries compile correction only up to `maxImplementationAttempts`, never executes after a policy or compile failure, and routes failed execution through classification before an optional `PROPOSE_ONLY` repair.

## Page Objects only is enforced

The implementer instruction is backed by `GeneratedTestPolicyValidator`. The proposal contains a complete Java 17 compilation unit, not a diff. The validator checks the source-size bound before parsing, parses the complete source into an AST, and rejects an unsuccessful parse as `SOURCE_PARSE_FAILED`; an unparseable proposal is never treated as a successful partial analysis. Its default policy rejects generated Java containing:

- `By.*` declarations or calls;
- `driver.findElement` or `findElements`;
- `Thread.sleep`, `java.lang.Thread.sleep`, matching method references, and unqualified `sleep(...)` only when imported statically from `java.lang.Thread`;
- direct `JavascriptExecutor` or `executeScript` use;
- retry annotations (short or qualified) and retry-like `for`, enhanced `for`, `while`, or `do/while` loops;
- a catch of `AssertionError`, `Error`, or `Throwable` that has any path which can return, break, continue, or complete normally instead of throwing;
- changes outside request-approved paths or in protected production/build paths.

`catch (Exception)` does not catch `AssertionError` and is not rejected by this rule. A catch may add diagnostics and then throw on every path; ordinary data iteration, Page Object calls, `assertThrows`, and project wait APIs remain allowed. Comments, string and character literals, and text blocks are AST literals rather than executable calls, so explanatory text does not become a violation. The analysis is syntax-aware but intentionally does not resolve arbitrary custom type hierarchies or prove business correctness. It is a deterministic generated-source policy, not a replacement Java compiler, a security sandbox, or evidence that a generated test asserts the right product behavior.

The coordinator applies this gate to every full implementation attempt, including corrected implementations, before compilation. Studio also validates generated and restored reviewed implementations before replay. A policy rejection records a failed `POLICY_VALIDATION` step with zero compile and execution attempts. Selector repair is a separate, narrowly targeted contract: `TrustedRepairApplier` validates the indexed declaration, fingerprints, allowed path and exact replacement, then the repair verifier compiles the final persisted source. It is not passed through the generated-test path policy because a legitimate Page Object repair can target `src/main/java`.

If the required Page Object operation is absent, the result is `PageObjectCapabilityMissing`. A `PageObjectExtensionProposal` may describe a mechanical method using an already-correlated declaration. If a new locator is required, the request is delegated to Selector Intelligence; the agent does not paste a guessed CSS or XPath into the test.

## Compile and run only the target

`TargetedJavaCompiler` compiles proposed in-memory source with the JDK compiler. Each source unit has a relative path and expected current-content fingerprint. A mismatch returns `SOURCE_PRECONDITION_FAILED`; it never compiles against a silently changed source. Diagnostics are bounded and the compile step must pass before execution.

`CompiledTargetedTestExecutor` is the injected host boundary used by `AgentWorkflowCoordinator`. It receives the run ID, test class, runner selectors, positive timeout, and the exact immutable `CompiledOutput` produced by the successful compile step. This prevents a targeted runner from accidentally executing a stale installed class with the same name. The host implementation owns JUnit or TestNG invocation and collection of Test Lens trace, assertion, runtime, screenshot-reference, and selector-diagnostic evidence. It should run the requested test or scenario, not the complete reactor by default. `TargetedTestExecutor.ExecutionResult` remains the bounded result projection.

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

The resulting `RepairProposal` is always `PROPOSE_ONLY`. It names the source file and exact declaration range, file and declaration fingerprints, old strategy/value, replacement candidate and live evidence, classification and drift references, risks, affected methods/tests, and verification plan.

`TrustedRepairApplier` is the explicit trusted-host operation for this selector-only path. It accepts only `EXACT` or `STRONG` correlation and a live-analyzed, verified-in-scope, same-target, unique replacement with accepted stability evidence. Before mutation it verifies the allowed relative source root, rejects symlink traversal, matches the indexed source-file fingerprint, declaration identity/fingerprint/range, old strategy/value fingerprint, and exactly one old `By` expression in that range. It then delegates an atomic, fingerprint-guarded write to `ControlledSourceApplier`. Statuses such as `TRUST_REQUIRED`, `CORRELATION_BLOCKED`, `SOURCE_PRECONDITION_FAILED`, `DECLARATION_PRECONDITION_FAILED`, and `OLD_SELECTOR_MISMATCH` leave source unchanged.

The browser certification copies a hand-written Page Object fixture to a temporary workspace. The old page and `By.id("old-login-button")` first pass and correlate exactly; the changed page removes that ID and exposes `data-testid="login-submit"`, so the same observed Selenium test fails with `NoSuchElementException` and a failed Lens trace. A new mapper observation obtains the replacement through live Selector Intelligence, the proposal remains `PROPOSE_ONLY`, trusted apply changes only the indexed `By` expression, compilation succeeds, and the same test passes. The contract is exercised in both Chrome and Firefox without patching repository source.

## Workflow artifacts and security gate

`WorkflowArtifactStore` accepts only the known artifact names for request, context, plan, implementation, execution, classification, repair, review, metrics, agent receipts, and JSON/text workflow reports. Writes are rooted under a validated run ID, atomic where supported, content-addressed, and bounded by explicit run retention.

Before storage or provider transport, `AgentArtifactSecurityGate` requires enabled central `RedactionPolicy`, rejects text that would still change under that policy, and checks caller-provided secret canaries. References to auth/storage state, cookies, page source, console or raw runner output, screenshots, and videos are rejected by default. The gate does not inspect image pixels; image and video evidence therefore remain outside agent context unless a separate trusted process establishes a safe projection.

Do not include passwords, cookie values, bearer/JWT tokens, authorization headers, CSRF or client secrets, local/session storage, environment values, system properties, saved auth-state files, or raw stack/log bundles in workflow artifacts. Use a dedicated non-production account, keep reusable auth state outside the repository with restricted permissions and retention, validate the application origin, and fail closed if authentication state is uncertain.

## Workflow report

`WorkflowReport` is a schema-versioned summary of one run in deterministic JSON and a compact text projection. It records the requirement, final workflow state, ordered steps, evidence references, limitations, and separate metrics for context bytes, agent input/output, external-agent duration, deterministic duration, compile/execution duration, total duration, attempts, changed source files, and repair proposals. It is a summary, not a container for prompts, credentials, raw Maven logs, or model chain-of-thought.

Typical successful steps are `CONTEXT_PREPARED`, `PLAN_READY`, `IMPLEMENTATION_READY`, `POLICY_VALIDATION`, `COMPILE_PASS`, `EXECUTION_PASS`, and `REVIEW`. A selector failure instead records `EXECUTION_FAIL`, `FAILURE_CLASSIFIED`, and, if evidence is sufficient, `REPAIR_PROPOSED` with status `PROPOSE_ONLY`. Reporting a proposal never implies that trusted apply occurred.

## Opt-in Codex dogfooding

The real-agent integrations are opt-in and require an authenticated local Codex CLI. Run the tooling-only contract from a reviewed checkout:

```powershell
mvn -pl selenium-test-lens-application-tooling -am `
  -Dtest=RealCodexWorkflowIT `
  -Dsurefire.failIfNoSpecifiedTests=false `
  -Dtestlens.codex.dogfood=true test
```

`RealCodexWorkflowIT` dispatches architect, implementer, and reviewer roles through separate read-only Codex profiles, validates the implementation policy, compiles the proposed test, executes the exact generated bytecode through `CompiledTargetedTestExecutor`, and requires final review approval.

The browser proof is separate:

```powershell
mvn -Pbrowser-it -pl selenium-test-lens-browser-tests -am `
  -Dit.test=RealCodexExistingPageObjectWorkflowIT `
  -Dtestlens.codex.dogfood=true -Dbrowser=chrome -Dheaded=false verify
```

It sends the bounded invalid-password requirement to real architect, implementer, and reviewer processes. The generated bytecode calls only the supplied `RuntimeLoginPage` API, is executed through a Test Lens-observed Chrome driver against a local fixture, leaves Lens trace events, reaches `SUCCESS`, and emits a JSON `WorkflowReport`. Use `-am` from the reactor root, or first install the current development artifacts; running the browser module against an older locally installed application-tooling JAR can produce class/linkage failures that are not agent failures.

The runner records role, byte counts, duration, and status; it does not persist chain-of-thought. A skipped opt-in test or unavailable CLI is `NOT RUN`, not evidence of real-agent execution. The scripted workflow remains the CI contract and is not presented as a substitute for either real dogfood run.

See [Existing Page Object correlation](../advanced/application-mapping/existing-page-objects.md) and the [guided tutorial](../advanced/application-mapping/tutorial.md#continue-from-an-existing-selenium-project).
