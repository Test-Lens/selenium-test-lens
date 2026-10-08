---
title: AI-assisted integration
description: Build a complete repository-aware prompt for integrating published Test Lens capabilities safely.
---

# AI-assisted integration

Use the AI Integration Builder to prepare one complete prompt for a coding agent. The prompt tells the agent to inspect the real project first, preserve its WebDriver and runner contracts, select only published Test Lens APIs, implement the capabilities you choose, validate the result, and report what it actually ran.

!!! tip "Start new integrations from latest"
    This page follows the current [`latest` documentation](https://test-lens.github.io/selenium-test-lens/latest/), which is the recommended starting point for a new integration. Older Test Lens versions and their documentation remain available and may still work correctly. They can produce a less complete or less effective integration because later releases may include fixes, improved APIs and lifecycle adapters, newer diagnostics and observability, or corrected integration patterns.

The builder's selectable surface is the published 0.5.0 runtime consumer boundary:

| Choice | Public artifact/API |
|---|---|
| Core, native observation, `UiLocator`, HUD, reports/evidence, upload, BiDi network, redaction, auth, state/resources and waits | `selenium-test-lens`; public `TestLens` and the documented supporting types |
| JUnit 5 lifecycle | `selenium-test-lens-junit5`; `TestLensExtension` |
| TestNG lifecycle | `selenium-test-lens-testng`; `TestLensTestNgListener`, `@TestLensTestNg` and `TestLensTestNgContext` |
| Allure evidence attachment | `selenium-test-lens-allure`; `AllureTestLens` |
| React/SPA helpers | `selenium-test-lens-react`; `ReactSupport` and the documented helper types |

Test Engineering Studio, compatibility analysis, migration tooling, and Selector Lab are not runtime-builder choices. Studio has its own Maven entry point; CI vendors, Grid, and cloud providers remain discovered constraints rather than Test Lens runtime feature adapters.

## AI Integration Builder

The builder runs entirely in this page. It does not read or modify your repository and sends nothing to a server. Presets only change the selected capabilities; every result is the same full-detail integration prompt.

The page's build metadata supplies one concrete target release. Coordinates and copied API links are both pinned to that numeric release, even when you opened the page through `latest` or a local/development preview. A development preview labels its stable integration target explicitly. If the metadata is missing, unsafe, or disagrees with a numeric documentation URL, the builder disables copying instead of guessing.

<section class="tl-ai-builder" data-ai-integration-builder aria-labelledby="ai-builder-title">
  <h3 id="ai-builder-title">Build your integration prompt</h3>
  <p class="tl-ai-builder__target" data-target-status role="status" aria-live="polite"></p>
  <ol class="tl-ai-builder__steps" aria-label="Builder steps">
    <li>Choose integration style</li>
    <li>Choose capabilities</li>
    <li>Review the full prompt</li>
    <li>Copy the prompt</li>
  </ol>

  <div>
    <strong>Presets</strong>
    <p class="tl-ai-builder__note">A preset is a starting selection, not a detail level. Project-specific features remain explicit.</p>
    <div class="tl-ai-builder__presets" role="group" aria-label="Capability presets">
      <button type="button" data-preset="minimal" aria-pressed="false">Minimal integration</button>
      <button type="button" data-preset="existing" aria-pressed="false">Existing Selenium project</button>
      <button type="button" data-preset="recommended" aria-pressed="false">Recommended observability</button>
      <button type="button" data-preset="maximum" aria-pressed="false">Maximum diagnostics</button>
    </div>
  </div>

  <fieldset>
    <legend>1. Choose integration style</legend>
    <div class="tl-ai-builder__options">
      <label class="tl-ai-builder__option">
        <input type="radio" name="integration-style" value="auto" checked>
        <strong>Inspect and choose safely</strong>
        <small>Let the agent choose manual attachment, JUnit 5, or TestNG after it traces the existing ownership model.</small>
      </label>
      <label class="tl-ai-builder__option">
        <input type="radio" name="integration-style" value="manual">
        <strong>Existing/custom WebDriver lifecycle</strong>
        <small>The project keeps driver creation and cleanup; Lens finalizes before the existing quit.</small>
      </label>
      <label class="tl-ai-builder__option">
        <input type="radio" name="integration-style" value="junit5">
        <strong>JUnit 5 adapter</strong>
        <small>Use the published extension only when its per-invocation driver ownership fits the project.</small>
      </label>
      <label class="tl-ai-builder__option">
        <input type="radio" name="integration-style" value="testng">
        <strong>TestNG adapter</strong>
        <small>Use the explicit listener/annotation contract with the correct PER_METHOD or PER_CLASS scope.</small>
      </label>
    </div>
  </fieldset>

  <div class="tl-ai-builder__core">
    <strong>Core lifecycle is always included.</strong>
    The generated prompt always covers attach/start/finalize, PASSED/FAILED/SKIPPED, original-failure preservation, invocation isolation, retry separation, and finalization before driver cleanup. It cannot be switched off.
  </div>

  <fieldset>
    <legend>2. Choose Test Lens capabilities</legend>
    <div class="tl-ai-builder__options">
      <label class="tl-ai-builder__option">
        <input type="checkbox" data-feature="nativeObservation">
        <strong>Native Selenium observation</strong>
        <small>Observe existing Selenium calls exactly once through <code>observeDriver()</code> or <code>observe(...)</code>.</small>
      </label>
      <label class="tl-ai-builder__option">
        <input type="checkbox" data-feature="uiLocator">
        <strong>Lens-native interactions</strong>
        <small>Use <code>UiLocator</code> deliberately for Lens waits, assertions, actionability and bounded recovery.</small>
      </label>
      <label class="tl-ai-builder__option">
        <input type="checkbox" data-feature="hud">
        <strong>HUD and live diagnostics</strong>
        <small>Use the real MINIMAL, COMPACT, STANDARD or DEBUG preset and DEFAULT/FAST observability contract.</small>
      </label>
      <label class="tl-ai-builder__option">
        <input type="checkbox" data-feature="reportsEvidence">
        <strong>Reports and evidence</strong>
        <small>Trace, JSON/HTML reports, screenshots and failed-session evidence bundles. Test Lens does not record video.</small>
      </label>
      <label class="tl-ai-builder__option">
        <input type="checkbox" data-feature="reportUpload">
        <strong>Explicit report upload</strong>
        <small>Synchronous HTTP upload after finalization; never automatic and independent of CI.</small>
      </label>
      <label class="tl-ai-builder__option">
        <input type="checkbox" data-feature="allure">
        <strong>Allure attachments</strong>
        <small>Attach finalized Lens evidence to an active Allure context without replacing Allure.</small>
      </label>
      <label class="tl-ai-builder__option">
        <input type="checkbox" data-feature="bidiNetwork">
        <strong>WebDriver BiDi network diagnostics</strong>
        <small>Passive request/response/redirect/fetch-error evidence; requires the full Selenium/session/provider preflight.</small>
      </label>
      <label class="tl-ai-builder__option">
        <input type="checkbox" data-feature="visualRedaction">
        <strong>Visual redaction</strong>
        <small>Mask screenshot pixels with <code>VisualRedactionOptions</code> while keeping safe defaults.</small>
      </label>
      <label class="tl-ai-builder__option">
        <input type="checkbox" data-feature="managedAuth">
        <strong>Managed Auth State</strong>
        <small>Same-origin restore/validate/recreate with explicit handling for sensitive persisted state.</small>
      </label>
      <label class="tl-ai-builder__option">
        <input type="checkbox" data-feature="managedTestState">
        <strong>Managed Test State &amp; Resources</strong>
        <small>Invocation state, intentional suite sharing, and exactly-once LIFO resource cleanup.</small>
      </label>
      <label class="tl-ai-builder__option">
        <input type="checkbox" data-feature="reactSpa">
        <strong>React &amp; SPA resilience</strong>
        <small>Add the optional published module only for a verified rerender, React Select, or DOM-readiness need.</small>
      </label>
      <label class="tl-ai-builder__option">
        <input type="checkbox" data-feature="applicationWaits">
        <strong>Application-aware waits</strong>
        <small>Use document and XHR/fetch-idle waits with their exact, bounded semantics.</small>
      </label>
    </div>
    <p id="reports-dependency-note" class="tl-ai-builder__note">Reports and evidence is selected and locked while Report Upload or Allure requires finalized artifacts. If you selected reports explicitly before adding that parent option, reports remains selected when the parent is removed.</p>
  </fieldset>

  <fieldset class="tl-ai-builder__context">
    <legend>Optional early compatibility hint</legend>
    <label for="ai-selenium-version">
      Selenium Java version, if known
      <input id="ai-selenium-version" data-selenium-version type="text" inputmode="decimal" autocomplete="off" placeholder="For example: 4.39.0" aria-describedby="ai-selenium-help ai-selenium-warning">
    </label>
    <p id="ai-selenium-help" class="tl-ai-builder__note">This only provides an early warning. The generated prompt always requires the agent to verify the effective resolved dependency from the repository.</p>
    <p id="ai-selenium-warning" class="tl-ai-builder__warning" data-selenium-warning role="status" aria-live="polite"></p>
  </fieldset>

  <div>
    <strong>Selected capabilities</strong>
    <p class="tl-ai-builder__selection-mode" data-selection-mode>Custom selection</p>
    <p class="tl-ai-builder__summary" data-selection-summary aria-live="polite"></p>
  </div>

  <div>
    <label for="ai-generated-prompt"><strong>3. Review the generated integration prompt</strong></label>
    <textarea id="ai-generated-prompt" data-generated-prompt readonly spellcheck="false" aria-describedby="ai-prompt-note"></textarea>
    <p id="ai-prompt-note" class="tl-ai-builder__note">The prompt always includes repository discovery, Selenium resolution, lifecycle invariants, CI preservation, real validation and a truthful final report.</p>
  </div>

  <div class="tl-ai-builder__copy">
    <button type="button" data-copy-prompt>Copy integration prompt</button>
    <span data-copy-status role="status" aria-live="polite"></span>
  </div>
</section>

<script src="../javascripts/ai-integration-builder.js"></script>

## How it works

The builder only generates text. It cannot inspect dependencies, edit code, run tests, contact a CI system, or enable browser capabilities. Paste the prompt into a coding agent that can work in the target repository. The agent is then instructed to discover the project facts that users should not have to enter manually: build system, Java, resolved Selenium, runner, WebDriver ownership, local/remote execution, Grid or cloud provider, retries, parallelism, reporters and CI.

Presets are intentionally conservative. **Maximum diagnostics** selects the general diagnostic capabilities, including BiDi preflight, but does not assume that a project has Allure, an upload endpoint, managed authentication, managed state, or a React-specific problem. Those remain explicit choices.

## Why the prompt is detailed

Test Lens integration crosses WebDriver lifetime, runner callbacks, recovery and runner retries, parallel state, diagnostics, evidence, reporting and security. A short dependency snippet can compile while still finalizing too late, closing a driver twice, losing the original failure, sharing state across parallel invocations, or claiming unsupported network behavior. The builder therefore produces one complete instruction rather than “basic” and “advanced” variants.

## Compatibility checks

Every generated prompt requires the effective Selenium Java dependency on the relevant test-runtime classpath, not merely the first version literal in a build file. The agent must inspect Maven parents, properties, dependency management and BOMs or Gradle catalogs, platforms and constraints, then confirm the resolved graph and detect mixed Selenium module versions where the build permits it.

The optional version field is only a user-entered early hint. It is never described as dependency-resolution evidence. Stable releases, prerelease/SNAPSHOT values, invalid input and missing input are assessed separately. A prerelease with numeric core `4.39.0` is not treated as equivalent evidence to stable `4.39.0`, but it is not declared incompatible solely because it has a qualifier.

For Test Lens WebDriver BiDi/network integration, Selenium Java **4.39.0 is the minimum supported integration baseline** in the current release. A lower version blocks Lens BiDi configuration until a safe minimal upgrade is made. Version 4.39.0 or newer only clears the dependency-version gate: browser, driver, session capabilities, `webSocketUrl`/BiDi connection, Grid and cloud-provider transport still require runtime verification. This is a Test Lens support boundary, not a claim that earlier Selenium versions have no BiDi functionality.

## Integration invariants

- The existing project owns its driver unless it deliberately adopts the JUnit 5 or TestNG adapter's documented ownership model.
- Lens finalization and failure evidence happen before the owning cleanup calls `driver.quit()` exactly once.
- The original test failure remains authoritative; diagnostic and cleanup failures do not replace it.
- State is isolated per physical invocation. Retry attempts, parameter rows and parallel tests do not share a mutable Lens instance.
- Runner retry, Lens recovery retry and polling are separate concepts and keep their existing policies.
- Central text redaction and screenshot masking defaults stay enabled; secrets are never embedded in source or prompts.
- Integration changes remain minimal and avoid unrelated dependency updates or framework refactors.

## What the agent should not do

- Do not rewrite the test framework, replace the driver factory, mass-migrate Page Objects or add another `quit()` without a proven need.
- Do not enable Test Lens BiDi below its supported Selenium baseline or treat a compatible version as proof of a working session/provider.
- Do not change runner retry count, parallelism, reporting or CI architecture merely to add Test Lens.
- Do not remove an existing reporter or claim that Test Lens replaces Allure.
- Do not describe native observation as Smart Click/recovery, `waitForNetworkIdle` as browser-wide idle, attached video as Lens recording, or caller-supplied API previews as automatic capture.
- Do not claim interception, network mocking, body capture, CDP fallback or automatic report upload.
- Do not use Selector Intelligence, Selector Lab, compatibility tooling, migration tooling or another internal/unpublished module as a consumer dependency.
- Do not say validation passed unless the command was actually executed.

## Version-matched integration guides

- [Getting Started](getting-started.md)
- [Existing WebDriver integration](framework-integration.md)
- [JUnit 5](integrations/junit5.md)
- [TestNG](integrations/testng.md)
- [Allure](integrations/allure.md)
- [HUD and live diagnostics](observability/visual-diagnostics.md)
- [Reports](observability/reports.md)
- [Report upload](observability/report-upload.md)
- [Failure bundles](observability/failure-bundles.md)
- [Network diagnostics](advanced/network.md)
- [Sensitive-data redaction](security/redaction.md)
- [Visual redaction](security/visual-redaction.md)
- [React & SPA resilience](features/react-spa.md)
- [Managed Auth State](advanced/auth-state.md)
- [Managed Test State & Resources](features/managed-test-state.md)
