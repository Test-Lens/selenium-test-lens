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

The tooling contract permits only `RepairProposal.ApplicationPolicy.PROPOSE_ONLY`. A proposal records what changed, why, source path and exact declaration range, file and declaration fingerprints, old and new selector evidence, same-target and stability evidence, and affected methods/tests. It is not silent self-healing and is not automatically applied.

`TrustedRepairApplier` is a separate host operation. It blocks `PROBABLE` and `AMBIGUOUS` correlations, non-live or wrong-target candidates, non-unique candidates, stale files/declarations, old-selector mismatch, paths outside allowed source roots, and symlink traversal. A successful apply replaces exactly one supported `By` expression inside the indexed declaration; it does not reformat or regenerate the class.

The repair certification uses a real browser failure rather than a mocked status. A hand-written Page Object first passes against the old fixture, then fails after the old element ID is removed. Test Lens captures the failed Selenium call, the changed page is mapped through live Selector Intelligence, and trusted apply patches a copied temporary project before compile and rerun. Chrome and Firefox exercise the same contract.

## Security boundary

The model is safer to share than raw DOM because the scanner deliberately excludes form values, password and textarea contents, cookies, authorization data, session identifiers, remote element IDs, and raw page source. Text and URL projections use the caller's central [`RedactionPolicy`](../../security/redaction.md), and `ContextSlicer` applies redaction again before writing agent context.

This is not a guarantee that arbitrary personal data can be inferred and removed. Avoid secrets in test labels, accessible names, URLs, custom attributes, application names, requirements, conventions, and user overrides. Review model and context artifacts before sending them outside the trusted environment. Screenshot and video boundaries remain separate from text redaction.

Authentication is caller-owned. Restore or perform login before mapping; do not store credentials, tokens, cookies, or reusable authentication state in `ApplicationModel`.

Before an authenticated test workflow can provide context to an agent, treat saved auth state as a replayable credential and apply these hard gates:

- use a dedicated non-production account and validate the expected origin;
- keep browser auth/storage-state files outside the repository with restricted access and bounded retention;
- keep passwords, cookie values, authorization headers, JWTs, CSRF/client secrets, local/session storage, environment values, and system properties out of requests and artifacts;
- require an enabled central `RedactionPolicy`, apply it before persistence or transport, and fail if secret canaries remain;
- exclude raw Surefire output, stack traces, console logs, page source, screenshots, and video from agent context by default;
- fail closed when authentication provenance or redaction status cannot be established.

`AgentArtifactSecurityGate` implements the textual/canary/reference checks for the workflow boundary. It does not inspect screenshot pixels and cannot turn arbitrary image evidence into a safe agent artifact.

`ExternalAgentRunner` applies this gate both before process start and after result collection. It uses an isolated temporary working directory, bounded standard streams and result file, a timeout, an absolute executable, argument-list invocation, and an environment cleared to an explicit non-secret allowlist. Optional source excerpts use allowlisted logical paths and verified content fingerprints. The staging directory is deleted after success or failure; cleanup failures are not logged because staged content may be sensitive.

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

### Existing source correlation is AMBIGUOUS

Inspect every candidate declaration and its page/context evidence. Do not choose by field or method name alone. Narrow the configured source roots or add a reviewed `CorrelationOverrides` entry that references stable class/page or source/application element IDs. If neither resolves identity, keep the result ambiguous.

### Source-aware context is PARTIAL

Review both application and source limitations. The source slice has independent bounds for Page Object classes, methods, declarations, existing tests, and serialized characters. An incomplete source index also makes the context partial. Increase only the limiting dimension after reviewing data exposure; do not remove `CONTEXT_BUDGET_REACHED` or source limitations from the artifact.

### A generated test is rejected before compile

Read `GeneratedTestPolicyValidator.ValidationResult`. The default policy rejects raw locators, direct driver lookup, sleeps, direct JavaScript, retry workarounds, protected or unapproved paths, and oversized source. Fix the proposal through the Page Object API. If the API is insufficient, record `PageObjectCapabilityMissing` instead of weakening policy.

### Compilation reports SOURCE_PRECONDITION_FAILED

The source changed after the proposal was prepared. Discard the stale proposal, re-read and re-index the affected source, create a new content fingerprint, and regenerate the bounded context. Do not overwrite the concurrent change.

### A selector repair is not produced

Confirm that failure classification is `SELECTOR_INSTABILITY`, correlation resolves to one source declaration, and the replacement is live-analyzed, same-target, unique, and `VERIFIED`. Missing any one of these is a reason to stop or request human review, not to invent a locator.

### Trusted repair is not applied

Inspect `TrustedRepairApplier.ApplyResult.status()` rather than retrying with weaker checks. `TRUST_REQUIRED` means the host did not explicitly authorize mutation. `CORRELATION_BLOCKED` preserves uncertain identity. `SOURCE_PRECONDITION_FAILED`, `DECLARATION_PRECONDITION_FAILED`, and `OLD_SELECTOR_MISMATCH` mean the proposal is stale or no longer describes the indexed source. Re-index and rebuild the proposal; do not patch by textual search outside the recorded declaration.

### External agent is unavailable or times out

Distinguish `AGENT_NOT_AVAILABLE` from `AGENT_TIMEOUT` and `AGENT_PROCESS_FAILED`. Verify the absolute executable, local provider authentication, role-specific profile, timeout, and the deliberately small environment allowlist. Do not substitute `ScriptedAgentExecutor` silently in a production run.

### External agent output is rejected

`AGENT_OUTPUT_INVALID` means output was empty, oversized, malformed, used the wrong schema/version/result type, contained extra or missing fields, or failed output redaction/canary validation. Ask the agent to return exactly the supplied JSON Schema; do not scrape prose or recover a partial/truncated result with regular expressions.

`AGENT_CONTEXT_REJECTED` is an input-side failure: the role may not match the profile, input may exceed the bound, a source excerpt fingerprint/path may be invalid, or redaction/canary/reference checks may have failed. Review the bounded projection instead of increasing all limits.

### Codex dogfood test is skipped

`RealCodexWorkflowIT` is opt-in and requires `-Dtestlens.codex.dogfood=true` plus a detected, locally authenticated native Codex CLI. A skipped test is `NOT RUN`; the scripted executor tests do not prove real external-agent execution. The CLI works in isolated read-only staging, so agent-side edits to the consumer checkout are neither expected nor permitted.

Use `RealCodexExistingPageObjectWorkflowIT` for the actual browser proof. Run from the reactor root with `-am` so browser tests use the current `application-tooling` classes. A `NoClassDefFoundError`, `NoSuchMethodError`, or similar linkage failure can mean the module was launched against a stale locally installed development JAR; rebuild/install the current reactor artifacts and rerun before diagnosing the provider or browser.

### Workflow stops at NEEDS_HUMAN_REVIEW

Inspect workflow metrics and audit entries. The configured correction, rerun, or repair bound was reached, or the caller explicitly requested review. Start a new run only after addressing the underlying uncertainty; do not increase every bound to hide a repeated failure.
