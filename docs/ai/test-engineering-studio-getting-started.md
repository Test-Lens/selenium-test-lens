---
title: Test Engineering Studio getting started
description: Launch the 0.5.0 development Test Engineering Studio from Maven, configure a project safely, and run its reviewed local workflow.
---

# Test Engineering Studio getting started

!!! warning "0.5.0 development / release-candidate preparation"
    The Maven goal on this page targets `0.5.0-SNAPSHOT` built from the current source checkout. The latest published release is 0.4.0, which does not contain Test Engineering Studio or this Maven plugin. This page is not a 0.5.0 release announcement.

Test Engineering Studio is a local UI for application mapping and the reviewed AI test-engineering workflow. It uses the current Maven project's source roots and test classpath, starts a loopback-only host, and performs no scan, browser action, agent call, source edit, or repair until the corresponding UI action is selected.

## Prerequisites

- JDK 17 or newer, including `javac`;
- Maven and a Maven Selenium test project that reaches `test-compile`;
- Chrome for the currently verified local end-to-end Studio path;
- a running application URL when mapping or executing a generated test;
- Codex CLI with the required `exec` capabilities for real agent stages, or a project-provided `AgentExecutorProvider` on the test classpath.

The source contains a bounded local Firefox provider, but the 0.5.0 external-consumer repair workflow is not claimed as Firefox-certified here. The repository's end-to-end repair fixture uses a deterministic scripted agent; it does not certify a real external provider through repair and verification.

## 1. Install the development snapshot

Until 0.5.0 is actually published, install the current reactor into your local Maven repository from the Test Lens checkout:

```powershell
mvn -DskipTests install
```

Then change to the root of the Maven Selenium project you want Studio to inspect. Run the fully qualified goal so the invocation does not depend on local plugin-prefix metadata:

```powershell
mvn io.github.test-lens:test-lens-test-engineering-maven-plugin:0.5.0-SNAPSHOT:studio
```

The goal runs through `test-compile`, resolves the test classpath, opens Studio in the default browser, prints its `127.0.0.1` URL, and waits until the Maven process is stopped. To print the URL without opening a browser:

```powershell
mvn -DtestLens.studio.openBrowser=false io.github.test-lens:test-lens-test-engineering-maven-plugin:0.5.0-SNAPSHOT:studio
```

Stopping Maven stops the Studio host and Studio-owned browser session. It does not repeat or approve a workflow.

## 2. Split project and machine-local configuration

Create `.test-lens/project.json` for reviewable, machine-independent project settings:

```json
{
  "schemaVersion": 1,
  "applicationName": "Checkout tests",
  "sourceRoots": ["src/main/java", "src/test/java"],
  "workspaceDirectory": ".test-lens",
  "startUrl": "http://127.0.0.1:8080/login",
  "agentProfiles": {
    "TEST_ARCHITECT": "high",
    "TEST_IMPLEMENTER": "standard",
    "TEST_VERIFIER": "standard",
    "STABILIZER": "high"
  }
}
```

The Maven goal supplies the actual compile/test roots and resolved test classpath with higher precedence than file autodiscovery. Keep application identity, relative source/workspace paths, the non-secret start URL, and logical agent profile names in `project.json`.

Create `.test-lens/local.json` only for settings tied to one workstation:

```json
{
  "schemaVersion": 1,
  "browser": {
    "name": "CHROME",
    "headless": true
  },
  "agents": {
    "profiles": {
      "TEST_ARCHITECT": "high"
    }
  },
  "codexExecutable": "C:\\tools\\codex\\codex.exe"
}
```

`codexExecutable` must be an absolute path. Local profile entries override matching project profile entries. Unknown fields, symbolic-link configuration files, secret-shaped field names, bearer/JWT-shaped values, and secret-like URL query parameters fail closed.

Do not store credentials in either file. The caller prepares authentication in the application or its normal test bootstrap; Studio does not persist passwords, cookies, tokens, authorization headers, or browser storage as configuration.

## 3. Commit only the reviewable configuration

Commit `.test-lens/project.json` when it contains no private environment details. Do not commit:

- `.test-lens/local.json`;
- generated project, workflow, context, repair, model, history, or Page Object output;
- auth state, agent staging files, screenshots, traces, or other execution evidence.

For the default workspace, a consumer repository can use this policy in its own `.gitignore`:

```gitignore
/.test-lens/*
!/.test-lens/project.json
```

If `workspaceDirectory` points elsewhere inside the project, ignore that generated directory separately. Review any artifact explicitly before moving it into version-controlled documentation or fixtures.

## 4. Check preflight before running work

Open **Overview** after launch. Studio reports browser, agent, and JDK-compiler capabilities separately.

- The default browser provider checks the configured Chrome or Firefox through Selenium Manager only during explicit preflight/open operations. `AVAILABLE` means the local driver path was resolved; it does not prove the application is reachable or authenticated.
- A project can provide a `BrowserSessionProvider` through `ServiceLoader` on the test classpath. Studio never silently reuses an unrelated WebDriver.
- The default agent provider discovers Codex CLI or uses the absolute local override. Preflight checks `--version` and the required `codex exec` flags; it does not send a prompt.
- A project can provide an `AgentExecutorProvider` through `ServiceLoader`. No OpenAI, Anthropic, Gemini, or other provider SDK is added to Test Lens runtime.
- A full JDK compiler is required for generated-source validation.

Treat any non-available capability as a blocker for the corresponding action. Project scanning and inspection of already persisted projections do not establish browser or agent readiness.

### Custom browser and agent providers

The built-ins are selected by `browser.provider: "LOCAL"` and `agents.provider: "CODEX"` (their defaults). To load project-specific providers from the Maven test classpath, set each required provider to either `"SERVICE"` or its equivalent `"CUSTOM"` selector:

```json
{
  "schemaVersion": 1,
  "browser": {
    "name": "CHROME",
    "headless": true,
    "provider": "SERVICE",
    "profile": "grid-smoke"
  },
  "agents": {
    "provider": "SERVICE",
    "profiles": {
      "TEST_ARCHITECT": "architecture-review",
      "TEST_IMPLEMENTER": "implementation"
    }
  }
}
```

Register exactly one implementation for each selected SPI using Java `ServiceLoader` files on the test classpath:

```text
src/test/resources/META-INF/services/io.github.testlens.studio.browser.BrowserSessionProvider
src/test/resources/META-INF/services/io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider
```

The browser SPI implements synchronous `preflight(BrowserRequest)` and `open(BrowserRequest)`. Honor the request's `MAPPING`/`TEST_EXECUTION` purpose, browser, headless flag, logical profile and lifecycle ownership; a `BrowserSession` quits its driver only for `STUDIO_OWNED`. The agent SPI implements availability/preflight and returns an `AgentExecutor` for the requested role and safe logical profile ID. It must return structured Test Lens agent artifacts rather than free-form prose.

Provider selection fails closed when the test classpath is absent, no implementation is found, or more than one implementation is registered. Studio closes the provider class loader when its launch handle closes. A custom provider is trusted project code; its network, credentials and process policy are the provider owner's responsibility and must not be placed in Studio JSON or projections.

### Timeouts and attempts

The built-in Codex CLI preflight has a five-second bound. Its agent execution timeout is three minutes, and the current public Studio bootstrap gives targeted compilation/test execution the same three-minute bound. The workflow permits at most two bounded attempts; it does not loop until a generated test passes. These launcher defaults are not configurable through `project.json` or `local.json` in this release-candidate source.

The bundled external-runner `AgentProfile` contract accepts timeouts only in the supported range of one second through 30 minutes. Do not respond to `AGENT_TIMEOUT` or a targeted test timeout by blindly increasing all waits or retry counts. Reduce irrelevant context, verify the external process and application readiness, and fix the classified synchronization or test issue.

## 5. Run the reviewed workflow

Use the UI in this order:

1. **Scan project** to index existing Page Objects, tests and locator declarations.
2. **Map application** after the application is running and its authentication/session bootstrap is ready.
3. **Correlate Page Objects** and review partial/ambiguous evidence.
4. Enter a bounded requirement, then separately **Generate plan** and **Generate test**.
5. Review the structured plan, proposed source and deterministic policy result before **Run**.
6. On failure, inspect classification and evidence. A failure is not automatically a selector defect.
7. If a selector repair is proposed, compare the old/new selector, same-target and stability evidence, affected source and verification plan.
8. Choose **Approve and apply** or **Reject** explicitly. Approval is scoped to the stored proposal; it is not a standing permission.
9. Run verification after an applied repair and inspect the new result.

Opening a proposal cannot edit source. The trusted local host rechecks allowed roots, source and declaration fingerprints, the old selector, replacement evidence, and symlink/path boundaries immediately before apply. Stale evidence fails with `SOURCE_PRECONDITION_FAILED`; there is no force-apply or silent-healing path. Studio does not commit, push, publish, or contact Git remotes.

## Security boundary

Studio binds an ephemeral `127.0.0.1` port and uses a random session token, exact Host/origin validation, CSP, bounded request bodies, explicit action allowlists, and a single mutating operation at a time. The browser UI receives bounded projections rather than arbitrary repository files. Agent context is redacted and sliced; normal runtime does not initialize Studio, scan the DOM, or start an agent.

The local process is still trusted tooling with access to the selected project and explicitly started browser/agent processes. Review configuration, generated source, repair evidence and diffs as code. Do not expose the loopback URL/session token or workspace artifacts in logs shared outside the machine.

## Upgrading an existing 0.4.x project

The Studio plugin is tooling for the 0.5.0 line. Adding or invoking it does not require changing the behavior of a 0.4.x Test Lens runtime integration: existing `TestLens.attach(...)`, actions, waits, retries, HUD, reports and runner lifecycle remain unchanged. Do not replace a stable `0.4.x` runtime dependency with `0.5.0-SNAPSHOT` merely to try Studio in a release project. Use an isolated branch/worktree and a locally installed development snapshot until the 0.5.0 artifacts are actually released.

Continue with the [Studio workflow reference](test-engineering-studio.md), [application-mapping tutorial](../advanced/application-mapping/tutorial.md), and [AI workflow contracts](workflow-orchestration.md).

## Troubleshooting

- **The fully qualified Maven goal cannot be resolved:** confirm that the complete `0.5.0-SNAPSHOT` reactor was installed in the same local Maven repository used by the consumer project. The goal is not in the 0.4.0 release.
- **`test-compile` or dependency resolution fails before Studio opens:** fix the consumer project's normal Maven test compilation/classpath first. The plugin intentionally requires test dependency resolution and does not construct a substitute classpath.
- **Browser preflight is `NOT_AVAILABLE`:** verify the configured browser is installed and Selenium Manager can resolve it. `CONFIGURATION_INVALID` or `UNSUPPORTED` requires correcting the selected provider/request rather than repeatedly clicking Map.
- **Agent preflight is `NOT_AVAILABLE`, `VERSION_UNSUPPORTED`, or `PROFILE_INVALID`:** verify the absolute executable, required `codex exec` flags, or safe logical profile. Preflight never falls back silently from a selected custom provider.
- **A service provider reports zero or multiple implementations:** keep exactly one service registration for the selected SPI on the resolved test classpath.
- **`AGENT_TIMEOUT`:** the external agent exceeded its bounded execution time and is terminated. Inspect its bounded diagnostics and context size; no scripted provider is substituted.
- **Targeted execution times out:** inspect application startup, test synchronization and the selected test. Studio closes the Studio-owned browser and reports the timeout; it does not convert it into a pass.
- **`OPERATION_IN_PROGRESS`:** wait for or stop the current bounded operation. Studio deliberately serializes mutating actions.
- **`SOURCE_PRECONDITION_FAILED`:** source or selector evidence changed after proposal creation. Re-scan, re-map/correlate, and create a fresh proposal; do not force an old repair.
- **A restored workflow is stale or interrupted:** follow only its backend-provided `availableActions`. Studio never assumes a pre-restart browser, agent or test process is still running.
