---
title: Test Engineering Studio
description: Use the development Test Engineering Studio to scan a Selenium project, map its application, review generated tests, run them, and approve evidence-backed repairs.
---

# Test Engineering Studio

!!! warning "Development tooling"
    Test Engineering Studio is source-only work for the next Test Lens release. It is not included in Maven 0.4.0 and is not a remotely hosted service.

Test Engineering Studio is a local, persistent interface over the deterministic Application Mapper, existing-Page-Object index, `ContextSlicer`, S12 agent workflow, targeted compiler and trusted repair boundary. It is separate from the runtime HUD.

Start with the [Maven getting-started guide](test-engineering-studio-getting-started.md) for the canonical 0.5.0 development goal, project/local configuration split, preflight and repository policy.

![Project overview populated by the browser fixture](../assets/media/test-engineering-studio/project-overview.png)

## Alternative source-checkout launcher

The canonical consumer entry point is the Maven goal in [Getting started](test-engineering-studio-getting-started.md). Contributors can instead use the Studio module's `exec-maven-plugin`. Build the development reactor once, then start the loopback host for one explicit project root:

```powershell
mvn -pl selenium-test-lens-test-engineering-studio -am -DskipTests install
mvn -f selenium-test-lens-test-engineering-studio/pom.xml exec:java -Dexec.args="--project D:\work\my-selenium-tests"
```

Use `--no-open` when the host must print its loopback URL without opening the default browser:

```powershell
mvn -f selenium-test-lens-test-engineering-studio/pom.xml exec:java -Dexec.args="--project D:\work\my-selenium-tests --no-open"
```

Stop the Maven process to stop the Studio host. The public bootstrap loads project-provided browser/agent providers from the test classpath when launched through the plugin; otherwise it uses the bounded local browser provider and Codex CLI provider. No browser or agent process starts until explicit preflight/action code requires it.

## Configuration and autodiscovery

Studio resolves the project root without following a project-root symlink and then reads optional `.test-lens/project.json`. The effective precedence is:

1. explicit `ProjectDiscovery.Request` overrides supplied by an embedding host;
2. `.test-lens/project.json`;
3. Maven/Gradle layout autodetection;
4. conservative defaults.

The contributor command-line launcher exposes only `--project` and `--no-open`; it does not expose every programmatic override as a flag. A minimal project file is:

```json
{
  "schemaVersion": 1,
  "applicationName": "Checkout tests",
  "sourceRoots": ["src/main/java", "src/test/java"],
  "classpathEntries": ["target/classes", "target/test-classes"],
  "workspaceDirectory": ".test-lens",
  "startUrl": "http://127.0.0.1:8080/login",
  "browser": {
    "name": "CHROME",
    "headless": true
  }
}
```

All paths must remain under the selected project root and may not traverse symbolic links. `startUrl` must be absolute HTTP(S), without user information or secret-like query parameters. Secret-like field names and credential-shaped configuration values are rejected. Do not put passwords, tokens, cookies or auth-state content in this file.

Maven projects are detected from `pom.xml`, with conventional `src/main/java`, `src/test/java`, `target/classes` and `target/test-classes` paths retained only when present. Gradle layout is detected for diagnostics, but the current descriptor reports Gradle projects as unsupported rather than pretending the complete workflow is ready. The Overview page shows the resolved source, roots, browser mode, workspace label and host operation status.

## Project setup

Open one explicit project root, then use the setup actions in order:

1. **Scan project** indexes Page Objects, components, tests, locator declarations and bounded usage edges.
2. **Map application** observes the caller-owned browser. `CURRENT_PAGE` performs one observation; repeated `GUIDED` observations retain the mapper session and aggregate visited pages and states.
3. **Correlate Page Objects** relates application elements to existing source declarations using S11 evidence. Studio displays `EXACT`, `STRONG`, `PROBABLE`, `AMBIGUOUS`, `NO_MATCH` and `CONFLICT`; it does not recalculate those decisions.
4. **Problems** centralizes selector review, ambiguous or missing correlation, source-index limitations and partial mapping.

Counts are measured facts, not a fabricated coverage percentage. A partial mapper result remains `PARTIAL`, with limitations visible in the evidence panel.

## Create and review a test

Enter a short observable requirement. Studio builds a redacted, bounded `AgentContextPack` from the current `ApplicationModel`, `PageObjectIndex`, correlation and usage graph. It records included and excluded scope rather than sending the whole repository.

![A structured plan generated from the bounded fixture context](../assets/media/test-engineering-studio/requirement-plan.png)

The reviewable workflow deliberately separates three actions:

- **Generate plan** calls the Architect and stores a structured `TestPlan`.
- **Generate test** calls the Implementer and shows the exact `TestImplementationProposal` plus deterministic policy results.
- **Run** replays those reviewed artifacts into the existing S12 coordinator, then performs policy validation, compilation, targeted execution and review.

Viewing a plan or proposed patch never compiles or starts a browser test. Page-Objects-only policy still rejects raw `By`, direct `findElement`, sleeps and JavaScript workarounds before execution.

## Run and diagnose

The run view presents compilation and execution status, bounded diagnostics, Test Lens trace references, duration and workflow attempts. Raw diagnostics remain behind progressive disclosure. Agent failures retain their structured code, including `AGENT_TIMEOUT`, `AGENT_OUTPUT_INVALID` and `AGENT_CONTEXT_REJECTED`.

When execution fails, the diagnosis projection shows the existing `FailureClassification`, evidence, counter-evidence and affected source scope. Studio does not reinterpret a product defect as a selector problem.

## Review a repair

![A real Selector Intelligence proposal awaiting explicit approval](../assets/media/test-engineering-studio/diagnosis-repair.png)

Repairs preserve the trusted-host boundary:

```text
failure evidence
-> FailureClassification
-> Selector Intelligence
-> RepairProposal (PROPOSE_ONLY)
-> human Approve or Reject
-> TrustedRepairApplier
-> verification run
```

Opening or rejecting a proposal never changes source. **Approve and apply** sends only the proposal identifier. The local host resolves the stored proposal and rechecks the allowed source root, path and symlink safety, project and declaration fingerprints, and old selector. A stale proposal returns `SOURCE_PRECONDITION_FAILED`; there is no force-apply endpoint.

## Workspace and artifacts

Bounded projections are written below the selected project root:

```text
.test-lens/
  project/
    project-status.json
    application-model.json
    page-object-index.json
    usage-graph.json
    correlations.json
    problems.json
  ai/
    workflow-history.json
  repairs/
    history.json
```

The UI reads projections instead of transferring full models or source trees on every request. Correlations and problems are paged with bounded offsets and limits. Refreshing the browser performs read-only `GET` requests; it does not rerun agents, tests, mapping or repair application.

Each workflow also has a versioned snapshot under `<resolved-workspace>/ai/sessions/<runId>.json` (by default `.test-lens/ai/sessions/<runId>.json`). Studio lists the bounded workflow summaries and loads one selected detail by `runId`; selecting a workflow is read-only.

On host restart, snapshots are validated before they are exposed. A successfully restored workflow is marked `resumed`. Work that was in `PLAN_GENERATING` or `IMPLEMENTATION_GENERATING` becomes `AGENT_EXECUTION_INTERRUPTED`; work that was executing or verifying becomes `EXECUTION_INTERRUPTED`. Studio never assumes an interrupted process completed.

Freshness compares the persisted source fingerprint with the currently indexed project:

- `FRESH` workflows expose only actions valid for their restored state;
- `STALE` means the project source changed and the old reviewed artifacts cannot be executed or applied as current evidence;
- `MISSING` means there is no current source fingerprint;
- `INVALID` identifies a snapshot that could not be safely restored.

The UI renders `availableActions` supplied by the backend rather than reconstructing the workflow state machine. Unsupported regeneration labels remain informational until the local action allowlist has a matching endpoint; the browser never posts an invented action.

The launcher passes the resolved in-root `workspaceDirectory` to `StudioWorkspaceStore`; `.test-lens` is only the default. Persistence rejects a workspace outside the project root or one reached through a symbolic link.

### Moving a project

Stop Studio before moving the directory. Move the complete project, including the configured workspace directory (`.test-lens` by default), then launch again with the new `--project` path. The default project ID derives from application name and detected build system rather than the absolute root, so a normal move can retain identity. Source fingerprints still decide whether artifacts are fresh; changed source roots or files can correctly make a restored workflow stale. Absolute paths outside the new root, symlinked workspaces and copied snapshots without their reviewed source are rejected or must be regenerated.

## Local transport security

The tooling host binds an ephemeral port on `127.0.0.1` only. It uses a random 256-bit session token, exact Host and same-origin checks, bounded JSON bodies, CSP, an action allowlist and one in-process operation lock. It exposes no arbitrary file-write or command endpoint. Backend strings and source diffs are rendered as text, not HTML.

Requirements and context use the existing `RedactionPolicy`. Cookies, authorization headers, browser storage, auth-state files and agent prompts are not Studio projections.

## Run the development fixture

From the repository root, run the complete Chrome fixture:

```powershell
mvn -pl selenium-test-lens-test-engineering-studio test
```

The fixture starts the loopback Studio host and a local login application, scans hand-written Page Objects, maps and correlates the live page, generates and reviews a scripted provider-neutral plan/test, compiles and executes it in Chrome, creates a real Selector Intelligence repair after selector drift, verifies that viewing the proposal does not mutate source, then applies it only after the browser clicks **Approve and apply**. A Firefox test selector exists, but this page does not treat it or the scripted agent as certification of the complete external-provider repair workflow.

Screenshots are written to `selenium-test-lens-test-engineering-studio/target/studio-screenshots/`.

## Troubleshooting

- `AGENT_NOT_AVAILABLE`: configure a real external runner profile or use the scripted executor only for offline fixture tests.
- `OPERATION_IN_PROGRESS`: wait for the current mapping, agent, compile, run or apply operation.
- `SOURCE_PRECONDITION_FAILED`: source changed after proposal creation; regenerate correlation and repair evidence.
- `AGENT_EXECUTION_INTERRUPTED` / `EXECUTION_INTERRUPTED`: the previous host stopped during work. Use only the backend-provided next action; Studio does not resume a subprocess in place.
- `STALE`, `MISSING` or `INVALID` workflow: inspect the workflow banner and limitations. Refresh/reindex the project and regenerate the indicated reviewed artifact rather than forcing an old run.
- `PARTIAL`: open Limitations. Do not interpret a bounded or unsupported region as complete mapping.
- Empty correlations: scan and map before correlation, then verify source roots and page identity.

See [AI workflow orchestration](workflow-orchestration.md) for S11/S12 contracts and [existing Page Object correlation](../advanced/application-mapping/existing-page-objects.md) for evidence semantics.

For the packaging boundary between source-only development modules and a future consumer release, see [0.5.0 publication candidates](0.5.0-publication-candidates.md).
