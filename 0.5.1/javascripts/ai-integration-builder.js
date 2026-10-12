(function (global) {
  'use strict';

  var VERSION_TOKEN = '{{TEST_LENS_TARGET_RELEASE}}';
  var PUBLIC_DOCS_ORIGIN = 'https://test-lens.github.io';
  var BIDI_MINIMUM = [4, 39, 0];

  var FEATURE_ORDER = [
    'nativeObservation',
    'uiLocator',
    'hud',
    'reportsEvidence',
    'reportUpload',
    'allure',
    'bidiNetwork',
    'visualRedaction',
    'managedAuth',
    'managedTestState',
    'reactSpa',
    'applicationWaits'
  ];

  var FEATURE_LABELS = {
    nativeObservation: 'Native Selenium observation',
    uiLocator: 'Lens-native UiLocator interactions',
    hud: 'HUD and live diagnostics',
    reportsEvidence: 'Reports and failure evidence',
    reportUpload: 'Explicit report upload',
    allure: 'Allure attachments',
    bidiNetwork: 'WebDriver BiDi network diagnostics',
    visualRedaction: 'Visual redaction',
    managedAuth: 'Managed Auth State',
    managedTestState: 'Managed Test State & Resources',
    reactSpa: 'React & SPA resilience',
    applicationWaits: 'Application-aware waits'
  };

  var PRESETS = {
    minimal: [],
    existing: ['nativeObservation', 'reportsEvidence', 'visualRedaction'],
    recommended: ['nativeObservation', 'uiLocator', 'hud', 'reportsEvidence', 'visualRedaction', 'applicationWaits'],
    maximum: ['nativeObservation', 'uiLocator', 'hud', 'reportsEvidence', 'bidiNetwork', 'visualRedaction', 'applicationWaits']
  };

  var MODULES = {
    BASE: [
      '# Test Lens integration task',
      '',
      'Integrate Test Lens ' + VERSION_TOKEN + ' into this existing Selenium Java test project. Work from the repository as it is; inspect it before editing and preserve its architecture unless a change is technically required for the selected capabilities.',
      '',
      'Use the pinned numeric Test Lens documentation linked below and only public, published APIs. For a manual/custom lifecycle the main coordinate is io.github.test-lens:selenium-test-lens:' + VERSION_TOKEN + '. When an official JUnit 5 or TestNG adapter is selected, use its published adapter coordinate and its transitive main runtime instead of adding internal reactor modules. Keep Selenium explicit and require Java 17 or newer.',
      '',
      'Do not add selenium-test-lens-core or selenium-test-lens-overlay directly unless the existing project deliberately consumes their documented low-level API. Do not use internal source modules, unpublished artifacts, roadmap functionality, or APIs inferred only from implementation classes. Do not perform unrelated dependency upgrades or refactors.'
    ],
    PROJECT_DISCOVERY: [
      '## Mandatory project discovery before changes',
      '',
      '1. Inspect repository status, build files, parent/BOM/version catalogs, test source layout, and existing documentation. Preserve unrelated local changes.',
      '2. Determine the build system and wrapper/approved commands, Java source/runtime level, test framework and its actual version.',
      '3. Determine the effective/resolved Selenium Java version on the test-runtime classpath of every module being integrated. Do not trust the first literal found in a POM or Gradle file. Inspect Maven properties, parent POMs, dependencyManagement and imported BOMs, or Gradle version catalogs, platforms and dependency constraints, including transitive resolution.',
      '4. Confirm resolution with the project build tool where possible: use Maven dependency:tree or dependency:list, Gradle dependencies/dependencyInsight, or the project equivalent. Record the exact command and resolved org.seleniumhq.selenium modules, and flag mixed Selenium module versions instead of reducing them to one misleading number.',
      '5. Trace WebDriver creation, options/capabilities, ownership, storage, and cleanup. Identify local versus RemoteWebDriver execution, Grid/cloud provider, browser matrix, retry implementation, parallel execution and driver scope.',
      '6. Inventory existing reporters, screenshots/artifacts, redaction/security conventions, CI jobs, commands, retry, parallelism and artifact collection. Treat CI as project context, not as a Test Lens feature.',
      '7. Identify the smallest integration seam and the affected tests before editing. Do not mechanically migrate every Page Object.'
    ],
    SELENIUM_COMPATIBILITY: [
      '## Selenium compatibility preflight',
      '',
      'Keep Selenium as an explicit consumer-owned dependency. Test Lens must not silently become the project\'s Selenium version authority. Preserve the resolved version unless a selected capability has a proven compatibility requirement or compilation/testing demonstrates that a minimal adjustment is necessary.',
      '',
      'Report the effective Selenium Java version, where it is controlled, and whether multiple versions resolve. Avoid unrelated browser, driver, framework or dependency upgrades.'
    ],
    LIFECYCLE_INVARIANTS: [
      '## Non-negotiable lifecycle invariants',
      '',
      '- Create exactly one TestLens state for each physical test invocation and its driver. Keep it invocation-safe; never use a shared static driver or Lens for parallel tests.',
      '- Map terminal outcomes accurately to finishPassed(), finishFailed(originalFailure), or finishSkipped(reason). Preserve the original test throwable and attach cleanup/diagnostic failures without replacing it.',
      '- Finalize Test Lens, its reports, redaction and failure bundle while the browser is still alive, before the owner calls driver.quit(). Never add a second driver owner or a second quit().',
      '- Do not run finalization twice. Respect the adapter/facade idempotency contract when application code may already have finalized.',
      '- Keep runner retries separate from Lens recovery retries and assertion/wait polling. Do not change retry counts, retry eligibility, parallelism or invocation identity.',
      '- Preserve central text-redaction and screenshot visual-redaction defaults. Never hard-code secrets, tokens, cookies, storage values or report credentials.'
    ],
    CI_PRESERVATION: [
      '## Preserve CI and project execution',
      '',
      'Detect the existing CI but do not redesign it merely to integrate Test Lens. Preserve jobs, commands, matrices, retries, parallelism, reporters and existing artifacts. Never hard-code secrets. If Test Lens reports/evidence are selected and CI already collects artifacts, make only the smallest necessary extension to that existing artifact mechanism; CI vendor configuration is not itself a Test Lens integration.'
    ],
    AUTO_LIFECYCLE: [
      '## Lifecycle model: inspect and choose',
      '',
      'Choose exactly one supported ownership model after inspection: manual attachment to the existing consumer-owned driver, TestLensExtension for a matching JUnit 5 per-invocation factory, or TestLensTestNgListener for a matching TestNG ownership model. Do not invent a generic framework adapter. Prefer manual attachment when an official adapter would conflict with the project\'s established driver ownership or cleanup.'
    ],
    MANUAL_LIFECYCLE: [
      '## Lifecycle model: existing/custom WebDriver',
      '',
      'Keep the project responsible for WebDriver creation and driver.quit(). Attach with public TestLens.attach(driver[, options]), start one session for the invocation, map PASSED/FAILED/SKIPPED explicitly, preserve and rethrow the original failure, and finalize before the project\'s existing cleanup. Do not move factory ownership into Test Lens and do not add another quit path.'
    ],
    JUNIT5: [
      '## Lifecycle model: JUnit 5 adapter',
      '',
      'Use the published io.github.test-lens:selenium-test-lens-junit5:' + VERSION_TOKEN + ' artifact only if TestLensExtension matches the project architecture. Register it once per test class with a real driver factory and use its WebDriver/TestLens parameter injection. The extension owns the driver returned by that factory, creates isolated state per parameterized/repeated/nested/parallel invocation, maps aborted tests to SKIPPED, finalizes first and calls quit exactly once. Remove or avoid a competing @AfterEach quit for that driver; do not combine lifecycle adapters for one invocation.'
    ],
    TESTNG: [
      '## Lifecycle model: TestNG adapter',
      '',
      'Use the published io.github.test-lens:selenium-test-lens-testng:' + VERSION_TOKEN + ' artifact only if TestLensTestNgListener and @TestLensTestNg fit the project. Both explicit registrations are required; there is no automatic service registration. Preserve the project\'s retry analyzer, DataProvider behavior, parallel schedule and driver scope. Default PER_METHOD owns one driver per physical invocation. Use opt-in PER_CLASS only when the existing class-scoped ownership is compatible: every DataProvider row and retry still needs a fresh Lens session/report while the class driver remains suite/context/instance-owned. Do not add competing setup/teardown ownership or a second quit. With PER_CLASS, recreate every observed facade and Page Object binding for the current Lens invocation; never retain an observed facade from a finalized invocation merely because the raw driver is shared.'
    ],
    OBSERVE_DRIVER: [
      '## Native Selenium observation',
      '',
      'Use public TestLens.observeDriver() or observe(element[, label]) where existing Page Objects should retain ordinary Selenium semantics. Pass the returned observed facade into the code that must be instrumented; old references that continue using the raw driver are not automatically observed.',
      '',
      'The observed facade delegates native calls exactly once and preserves Selenium results/exceptions. It adds correlated diagnostics but does not add Smart Click, UiLocator actionability, Lens recovery retries, stale re-resolution, extra lookup or JavaScript fallback. Do not claim otherwise and do not replace provider code that requires a concrete driver class without checking compatibility.'
    ],
    UI_LOCATOR: [
      '## Lens-native interactions',
      '',
      'Use public TestLens locator factories and UiLocator only in new or deliberately modernized seams that benefit from Lens-native waits, assertions, re-resolution and bounded recovery. Preserve locator intent and Page Object boundaries. UiLocator.click() uses the documented bounded NATIVE -> ACTIONS -> POINT -> optional JS cascade under one deadline; do not reproduce it with ad-hoc retries or mass-convert Page Objects. Informational boundary: native Selenium observation is a separate option for areas requiring unchanged Selenium semantics; do not add it unless it was selected or already exists.'
    ],
    HUD: [
      '## HUD and live diagnostics',
      '',
      'Configure public HudOptions/HighlightOptions through TestLensOptions only when useful. Valid HUD presets are MINIMAL, COMPACT (default), STANDARD and DEBUG. ObservabilityMode.DEFAULT and FAST are execution-independent presentation/retention modes; FAST is not Selenium headless mode and does not change interaction semantics, waits or retry policy. Keep the HUD passive, preserve pointer-safe highlighting and retain visual-redaction defaults. Do not invent a DEFAULT HUD preset or new mode names.'
    ],
    REPORTS_AND_EVIDENCE: [
      '## Reports and evidence',
      '',
      'Use the finalized TestLensFinalizationResult and existing public trace/report/evidence APIs. Test Lens can produce structured trace, JSON and standalone HTML reports, explicit screenshots, and a best-effort failed-session evidence bundle/ZIP. Check nullable result paths and diagnosticFailures instead of assuming every collector succeeded. Capture failure evidence before driver cleanup and never let best-effort diagnostics replace the original failure.',
      '',
      'Do not describe Test Lens as a video recorder. It can attach a file or URL produced by Grid, a cloud provider, CI or another recorder; visual redaction does not modify video.'
    ],
    REPORT_UPLOAD: [
      '## Explicit report upload',
      '',
      'Use public ReportUploader with one already-finalized TestLensFinalizationResult. Upload is explicit, synchronous and independent of CI; configuring an endpoint or finalizing never sends anything automatically. First locate the endpoint and credentials in the project\'s existing controlled configuration. Never guess an address or secret. If required configuration is absent, report report-upload as blocked and do not claim that capability is integrated. Preserve local evidence on upload failure, respect payload/timeout/retry bounds, do not follow redirects, and choose explicitly whether requireSuccess() should affect the test/build.'
    ],
    ALLURE: [
      '## Allure coexistence',
      '',
      'Use the published io.github.test-lens:selenium-test-lens-allure:' + VERSION_TOKEN + ' artifact only when the project already has an active supported Allure context. Keep the existing Allure runner adapter, annotations, lifecycle and allure-results directory. Call AllureTestLens.attach(finalizedResult[, options]) once with the same finalized result while the Allure executable is active. It attaches finalized evidence; it does not replace Allure, generate the Allure report, own WebDriver, upload over HTTP or map every trace event to an Allure step. Preserve the original test outcome if attachment fails.'
    ],
    BIDI_NETWORK: [
      '## WebDriver BiDi network diagnostics and mandatory compatibility gate',
      '',
      'Test Lens BiDi/network diagnostics require Selenium Java 4.39.0 or newer as the supported integration baseline. First resolve the effective Selenium version from the dependency graph.',
      '',
      '- If the resolved stable version is below 4.39.0, print this clear warning using the real resolved version: "Test Lens BiDi/network diagnostics require Selenium Java 4.39.0 or newer. This project currently resolves Selenium X.Y.Z, so Test Lens BiDi-based functionality cannot be enabled with the current dependency set." Do not configure apparently working Lens BiDi. Identify the minimal controlling dependency/property change, assess framework/browser/Grid/cloud impact, and avoid unrelated upgrades. If that one upgrade is safe and required, perform it minimally and run full validation; otherwise integrate core Lens without BiDi and report the limitation.',
      '- If the resolved value is a prerelease or SNAPSHOT, do not treat its numeric core as proof that the supported stable baseline is met. Identify the exact artifact, qualifier and resolved modules, then verify compatibility explicitly. Do not declare it automatically incompatible solely because it is prerelease.',
      '- If the resolved stable version is 4.39.0 or newer, do not treat the version check as sufficient. Verify the actual browser, driver, session construction, enableBiDi()/webSocketUrl capability, local versus RemoteWebDriver path, Grid/cloud provider support and successful BiDi transport at runtime.',
      '',
      'Use public NetworkDiagnostics with BIDI or AUTO only after those checks. Capture is passive request/response/redirect/fetch-error evidence with waits/assertions over captured state. It is not interception, blocking, mocking, replay, request/response body capture, CDP, performance-log fallback or a general BiDi wrapper. Preserve redaction and header masking. Check startup/status results; unsupported or failed capture must not be reported as zero failures.'
    ],
    VISUAL_REDACTION: [
      '## Visual redaction',
      '',
      'Configure public VisualRedactionOptions through TestLensOptions for screenshot masking. Preserve the default automatic SOLID masking of password inputs and fail-closed STRICT behavior unless the project has a reviewed reason to add explicit masks or use BEST_EFFORT. Validate required mask targets. Text redaction is separate, and video remains outside pixel masking; do not weaken central security/redaction defaults.'
    ],
    AUTH_STATE: [
      '## Managed Auth State',
      '',
      'Use public lens.authState() / AuthStateManager and AuthStateRequest only for a deliberate same-origin test authentication workflow. Preserve tri-state validation, bounded one-login recreation, origin checks, expiry handling, locking and atomic persistence. Treat persisted cookies/storage as live credentials: use ignored access-controlled paths, short-lived test accounts and no unrestricted CI artifact. Do not claim automatic cross-origin SSO federation or include auth-state files in normal reports/bundles.'
    ],
    TEST_STATE_RESOURCES: [
      '## Managed Test State & Resources',
      '',
      'Use public scenarioState() for one physical invocation, suiteState() only for intentional thread-safe sharing in one logical run, and resources() for exactly-once LIFO cleanup tied to Lens finalization. Keep DataProvider rows, parameterized cases and retry attempts isolated. Do not use these APIs as disk, cross-process or distributed state, and do not serialize state/resource values into diagnostics. Preserve cleanup ordering and original failures.'
    ],
    REACT_SPA: [
      '## React & SPA resilience',
      '',
      'Add the published io.github.test-lens:selenium-test-lens-react:' + VERSION_TOKEN + ' artifact only when verified rerender windows, React Select conventions or DOM-readiness conventions require it. Do not add it merely because the application uses React. Informational boundary: UiLocator is a separate capability and is not added by this selection; if it already exists or was selected, prefer its ordinary click for normal interaction. ReactSupport.smartClick has a separate legacy/specialized contract. Keep ReactSafeExecutor retry settings separate from UiLocator and runner retries, and do not claim component-tree introspection or universal framework compatibility.'
    ],
    APPLICATION_WAITS: [
      '## Application-aware waits',
      '',
      'Use public page waits only where their exact semantics match the application. waitForPageReady requires document.readyState=complete; waitForInteractiveOrComplete accepts interactive or complete. waitForNetworkIdle is a bounded in-page heuristic that tracks only XHR/fetch started after its tracker is installed and requires a complete zero-active idle window. It is not browser-wide network idle and does not see earlier requests, images, CSS, scripts, WebSocket, EventSource or beacon. Informational boundary: BiDi diagnostics are a separate option for passive browser-level evidence; do not configure BiDi, its dependencies or browser capabilities unless it was selected or already exists. Keep polling distinct from Lens recovery and runner retries.'
    ],
    VALIDATION: [
      '## Required validation',
      '',
      'Run the project\'s real compile and focused tests first, then its normal test command and any relevant browser/integration suite that the environment permits. Exercise PASSED, FAILED and SKIPPED/aborted lifecycle paths where supported; verify finalization precedes cleanup, exactly one quit occurs, original failures survive, reports/evidence paths are real, and parallel/retry invocations remain isolated.',
      '',
      'For every selected capability, add or update focused contract coverage without weakening existing assertions. If BiDi is selected, validate the effective dependency version plus a real compatible session/provider path; do not replace runtime validation with a version comparison. Never use arbitrary sleeps to make tests pass. Do not claim a command, browser, provider or workflow passed unless it was actually executed.'
    ],
    FINAL_REPORT: [
      '## Final report',
      '',
      'Report: repository/base state; effective Java/build/test framework and Selenium resolution evidence; driver ownership/lifecycle before and after; selected Test Lens artifacts/APIs; exact files changed; compatibility decisions and any minimal Selenium upgrade; retry/parallel/CI preservation; security/redaction handling; exact validation commands with PASS/FAIL/NOT RUN and reasons; remaining limitations; and final Git status. Explicitly distinguish Lens recovery from runner retry and list anything not validated.'
    ]
  };

  var FEATURE_MODULES = {
    nativeObservation: 'OBSERVE_DRIVER',
    uiLocator: 'UI_LOCATOR',
    hud: 'HUD',
    reportsEvidence: 'REPORTS_AND_EVIDENCE',
    reportUpload: 'REPORT_UPLOAD',
    allure: 'ALLURE',
    bidiNetwork: 'BIDI_NETWORK',
    visualRedaction: 'VISUAL_REDACTION',
    managedAuth: 'AUTH_STATE',
    managedTestState: 'TEST_STATE_RESOURCES',
    reactSpa: 'REACT_SPA',
    applicationWaits: 'APPLICATION_WAITS'
  };

  var STYLE_MODULES = {
    auto: 'AUTO_LIFECYCLE',
    manual: 'MANUAL_LIFECYCLE',
    junit5: 'JUNIT5',
    testng: 'TESTNG'
  };

  var STYLE_LABELS = {
    auto: 'Inspect and choose safely',
    manual: 'Existing/custom WebDriver lifecycle',
    junit5: 'JUnit 5 adapter',
    testng: 'TestNG adapter'
  };

  var DOC_LINKS = {
    base: [
      ['Getting Started', 'getting-started/'],
      ['Existing WebDriver integration', 'framework-integration/']
    ],
    junit5: [['JUnit 5', 'integrations/junit5/']],
    testng: [['TestNG', 'integrations/testng/']],
    hud: [['HUD and live diagnostics', 'observability/visual-diagnostics/']],
    reportsEvidence: [
      ['Reports', 'observability/reports/'],
      ['Failure bundles', 'observability/failure-bundles/']
    ],
    reportUpload: [['Report upload', 'observability/report-upload/']],
    allure: [['Allure', 'integrations/allure/']],
    bidiNetwork: [['Network diagnostics', 'advanced/network/']],
    visualRedaction: [
      ['Sensitive-data redaction', 'security/redaction/'],
      ['Visual redaction', 'security/visual-redaction/']
    ],
    managedAuth: [['Managed Auth State', 'advanced/auth-state/']],
    managedTestState: [['Managed Test State & Resources', 'features/managed-test-state/']],
    reactSpa: [['React and SPA resilience', 'features/react-spa/']],
    applicationWaits: [['Element and application waits', 'elements/waiting/']]
  };

  function resolveCapabilities(capabilities) {
    var selected = new Set((capabilities || []).filter(function (name) { return FEATURE_ORDER.indexOf(name) >= 0; }));
    var reasons = {};
    if (!selected.has('reportsEvidence')) {
      var parents = [];
      if (selected.has('reportUpload')) parents.push(FEATURE_LABELS.reportUpload);
      if (selected.has('allure')) parents.push(FEATURE_LABELS.allure);
      if (parents.length) reasons.reportsEvidence = 'required by ' + parents.join(' and ');
    }
    var explicit = FEATURE_ORDER.filter(function (name) { return selected.has(name); });
    var required = FEATURE_ORDER.filter(function (name) { return Object.prototype.hasOwnProperty.call(reasons, name); });
    var effectiveSet = new Set(explicit.concat(required));
    return {
      explicit: explicit,
      required: required,
      effective: FEATURE_ORDER.filter(function (name) { return effectiveSet.has(name); }),
      reasons: reasons
    };
  }

  function normalizedCapabilities(capabilities) {
    return resolveCapabilities(capabilities).effective;
  }

  function parseVersion(value) {
    var match = /^\s*(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z][0-9A-Za-z.-]*))?(?:\+([0-9A-Za-z][0-9A-Za-z.-]*))?\s*$/.exec(value || '');
    if (!match) return null;
    return {
      numbers: [Number(match[1]), Number(match[2]), Number(match[3])],
      qualifier: match[4] || '',
      buildMetadata: match[5] || '',
      stability: match[4] ? 'prerelease' : 'stable'
    };
  }

  function compareVersion(left, right) {
    for (var index = 0; index < 3; index += 1) {
      if (left[index] !== right[index]) return left[index] < right[index] ? -1 : 1;
    }
    return 0;
  }

  function seleniumAssessment(value, bidiSelected) {
    var trimmed = String(value || '').trim();
    if (!trimmed) return bidiSelected
      ? { kind: 'unknown', trusted: false, message: 'Optional hint not provided. The agent must resolve Selenium from the repository before enabling BiDi.' }
      : { kind: 'inactive', trusted: false, message: 'BiDi is not selected. The agent must still resolve the effective Selenium version.' };
    var parsed = parseVersion(trimmed);
    if (!parsed) return { kind: 'invalid', trusted: false, message: 'The entered value is not a supported Selenium version format. Enter a value such as 4.39.0, 4.39.0-rc1 or 4.39.0-SNAPSHOT. The agent will still verify the resolved dependency.' };
    if (!bidiSelected) return { kind: 'hint-only', trusted: true, parsed: parsed, message: 'Version hint recorded. BiDi is not selected, and the agent must still resolve the effective Selenium version.' };
    if (parsed.stability === 'prerelease') {
      var numericRelation = compareVersion(parsed.numbers, BIDI_MINIMUM);
      return {
        kind: 'prerelease-review',
        trusted: true,
        parsed: parsed,
        message: numericRelation < 0
          ? 'The prerelease version entered is numerically below the supported Test Lens BiDi baseline and also requires separate compatibility verification.'
          : 'The version entered is a prerelease or SNAPSHOT. It does not by itself demonstrate the supported stable Test Lens BiDi baseline; verify the exact resolved build and runtime compatibility.'
      };
    }
    if (compareVersion(parsed.numbers, BIDI_MINIMUM) < 0) {
      return {
        kind: 'blocked',
        trusted: true,
        parsed: parsed,
        message: 'The version entered is below the supported Test Lens BiDi baseline. Test Lens BiDi/network diagnostics require stable Selenium Java 4.39.0 or newer. The agent must verify the project\'s resolved dependency before deciding whether BiDi can be enabled.'
      };
    }
    return { kind: 'compatible-version', trusted: true, parsed: parsed, message: 'The stable version entered meets the numeric Test Lens BiDi baseline. This does not confirm working BiDi: browser, session, transport and provider compatibility still require runtime verification.' };
  }

  function normalizePublicRoot(value) {
    try {
      var url = new URL(value);
      if (url.protocol !== 'https:' || url.origin !== PUBLIC_DOCS_ORIGIN || !/\/selenium-test-lens\/$/.test(url.pathname)) return null;
      return url.href;
    } catch (ignored) {
      return null;
    }
  }

  function resolveDocumentationTarget(metadata, pageUrl) {
    var targetRelease = String(metadata && metadata.targetRelease || '').trim();
    var publicDocsRoot = normalizePublicRoot(metadata && metadata.publicDocsRoot);
    var sourceKind = String(metadata && metadata.sourceKind || '').trim();
    var sourceRevision = String(metadata && metadata.sourceRevision || '').trim();
    var errors = [];
    if (!/^\d+\.\d+\.\d+$/.test(targetRelease)) errors.push('Target release metadata must be a concrete MAJOR.MINOR.PATCH version.');
    if (!publicDocsRoot) errors.push('Public documentation root metadata is missing or unsafe.');
    if (sourceKind !== 'development' && sourceKind !== 'release') errors.push('Documentation source kind metadata is missing or invalid.');
    try {
      var page = new URL(pageUrl);
      var numericPath = /\/selenium-test-lens\/(\d+\.\d+\.\d+)(?:\/|$)/.exec(page.pathname);
      if (numericPath && numericPath[1] !== targetRelease) errors.push('Numeric documentation path and target release metadata disagree.');
    } catch (ignored) {
      errors.push('Builder page URL is invalid.');
    }
    return {
      valid: errors.length === 0,
      errors: errors,
      targetRelease: targetRelease,
      publicDocsRoot: publicDocsRoot,
      docsBase: errors.length || !publicDocsRoot ? null : publicDocsRoot + targetRelease + '/',
      sourceKind: sourceKind,
      sourceRevision: sourceRevision || 'unavailable'
    };
  }

  function documentationSection(style, capabilities, docsBase) {
    var links = DOC_LINKS.base.slice();
    if (style === 'junit5') links = links.concat(DOC_LINKS.junit5);
    if (style === 'testng') links = links.concat(DOC_LINKS.testng);
    capabilities.forEach(function (name) { if (DOC_LINKS[name]) links = links.concat(DOC_LINKS[name]); });
    var seen = new Set();
    var lines = ['## Version-matched public documentation', ''];
    links.forEach(function (item) {
      var url = docsBase + item[1];
      if (!seen.has(url)) {
        seen.add(url);
        lines.push('- ' + item[0] + ': ' + url);
      }
    });
    return lines;
  }

  function renderModule(name, targetRelease) {
    return MODULES[name].join('\n').split(VERSION_TOKEN).join(targetRelease);
  }

  function scopeSection(style, resolution, targetRelease, sourceKind) {
    var explicit = resolution.explicit.map(function (name) { return FEATURE_LABELS[name]; });
    var required = resolution.required.map(function (name) { return FEATURE_LABELS[name] + ' (' + resolution.reasons[name] + ')'; });
    var notSelected = FEATURE_ORDER.filter(function (name) { return resolution.effective.indexOf(name) < 0; }).map(function (name) { return FEATURE_LABELS[name]; });
    return [
      '## Requested integration scope',
      '',
      '- Target release: Test Lens ' + targetRelease + '.',
      '- Builder source: ' + (sourceKind === 'development' ? 'development preview explicitly targeting the stable ' + targetRelease + ' consumer contract' : 'versioned release documentation for ' + targetRelease) + '.',
      '- Lifecycle model: ' + STYLE_LABELS[style] + '.',
      '- Explicitly selected capabilities: ' + (explicit.length ? explicit.join('; ') : 'none; core lifecycle only') + '.',
      '- Capabilities included only as required dependencies: ' + (required.length ? required.join('; ') : 'none') + '.',
      '- Additional capabilities not requested: ' + (notSelected.length ? notSelected.join('; ') : 'none') + '.',
      '',
      'Implement only the lifecycle model, explicit selections and required dependencies listed above. Mentions of other Test Lens capabilities are informational boundaries, not authorization to configure them. An unchecked option does not disable built-in safe defaults such as finalization or redaction. Preserve any existing project integration unless a selected change requires a reviewed adjustment; do not remove it merely because it was not selected again.'
    ];
  }

  function compositionSection(style, resolution) {
    var upload = resolution.effective.indexOf('reportUpload') >= 0;
    var allure = resolution.effective.indexOf('allure') >= 0;
    if (!upload && !allure) return [];
    var lines = ['## Finalization, publication and cleanup composition', ''];
    if (style === 'manual' || style === 'auto') {
      lines.push('For a manual lifecycle, obtain exactly one terminal TestLensFinalizationResult, then use that same result for' + (upload && allure ? ' Allure attachment and explicit HTTP upload' : upload ? ' explicit HTTP upload' : ' Allure attachment') + ', and only then allow the existing owner to quit the driver. Preserve the original throwable across publication failures.');
      if (style === 'auto') lines.push('If an official adapter is chosen after inspection, follow its documented adapter-specific publication path instead of copying the manual teardown sequence into its callback.');
    } else {
      lines.push('The selected adapter normally owns terminal mapping, finalization and its single driver cleanup. Do not add a second finish or quit around the adapter. When publication is required, use the documented supported path: deliberately finalize once while the test/Allure context is still active and publish the returned result before the test returns; the adapter then reuses that terminal result and performs its normal cleanup. For centralized coverage of every outcome, use a project-owned ordered ' + (style === 'junit5' ? 'JUnit 5 extension' : 'TestNG listener') + ' around the documented uploader/Allure APIs only if the project can prove ordering; do not invent a Test Lens callback.');
    }
    if (upload) lines.push('Report upload remains incomplete if no endpoint/authentication configuration can be found and verified. Report that concrete blocker; never guess configuration or claim success.');
    if (allure) lines.push('Attach while an active Allure executable context exists. If that context cannot be established for the chosen lifecycle, report Allure attachment as blocked instead of moving attachment after runner teardown.');
    return lines;
  }

  function buildPrompt(options) {
    var style = STYLE_MODULES[options && options.style] ? options.style : 'auto';
    var targetRelease = String(options && options.targetRelease || '').trim();
    var publicDocsRoot = normalizePublicRoot(options && options.publicDocsRoot);
    var sourceKind = options && options.sourceKind === 'development' ? 'development' : 'release';
    if (!/^\d+\.\d+\.\d+$/.test(targetRelease) || !publicDocsRoot) throw new Error('Trusted target-release metadata is required before a prompt can be generated.');
    var resolution = resolveCapabilities(options && options.capabilities);
    var capabilities = resolution.effective;
    var assessment = seleniumAssessment(options && options.seleniumVersion, capabilities.indexOf('bidiNetwork') >= 0);
    var sections = [];
    ['BASE', 'PROJECT_DISCOVERY', 'SELENIUM_COMPATIBILITY', 'LIFECYCLE_INVARIANTS', 'CI_PRESERVATION'].forEach(function (name) {
      sections.push(renderModule(name, targetRelease));
    });
    sections.push(scopeSection(style, resolution, targetRelease, sourceKind).join('\n'));
    sections.push(renderModule(STYLE_MODULES[style], targetRelease));
    capabilities.forEach(function (name) { sections.push(renderModule(FEATURE_MODULES[name], targetRelease)); });
    if (compositionSection(style, resolution).length) sections.push(compositionSection(style, resolution).join('\n'));
    if (capabilities.indexOf('bidiNetwork') >= 0 && options && String(options.seleniumVersion || '').trim()) {
      var hintLines = ['## Builder Selenium hint', ''];
      if (assessment.trusted) hintLines.push('User-entered hint: ' + String(options.seleniumVersion).trim() + '.');
      else hintLines.push('The optional version field contained an invalid value; it has not been copied into this prompt as technical evidence.');
      hintLines.push(assessment.message, 'This is only a user-entered hint, not dependency-resolution evidence. Verify the effective test-runtime classpath before editing.');
      sections.push(hintLines.join('\n'));
    }
    sections.push(documentationSection(style, capabilities, publicDocsRoot + targetRelease + '/').join('\n'));
    sections.push(renderModule('VALIDATION', targetRelease));
    sections.push(renderModule('FINAL_REPORT', targetRelease));
    return sections.join('\n\n').replace(/\n{3,}/g, '\n\n').trim() + '\n';
  }

  function init(document, location, navigator) {
    var root = document.querySelector('[data-ai-integration-builder]');
    if (!root) return;
    if (root.dataset.aiBuilderInitialized === 'true') return;
    root.dataset.aiBuilderInitialized = 'true';
    var prompt = root.querySelector('[data-generated-prompt]');
    var warning = root.querySelector('[data-selenium-warning]');
    var summary = root.querySelector('[data-selection-summary]');
    var selectionMode = root.querySelector('[data-selection-mode]');
    var targetStatus = root.querySelector('[data-target-status]');
    var copyStatus = root.querySelector('[data-copy-status]');
    var versionInput = root.querySelector('[data-selenium-version]');
    var copyButton = root.querySelector('[data-copy-prompt]');
    var featureInputs = Array.from(root.querySelectorAll('[data-feature]'));
    var styleInputs = Array.from(root.querySelectorAll('input[name="integration-style"]'));
    var metadataValue = function (name) {
      var element = document.querySelector('meta[name="' + name + '"]');
      return element ? element.getAttribute('content') : '';
    };
    var target = resolveDocumentationTarget({
      targetRelease: metadataValue('test-lens-integration-target'),
      publicDocsRoot: metadataValue('test-lens-docs-public-root'),
      sourceKind: metadataValue('test-lens-docs-source-kind'),
      sourceRevision: metadataValue('test-lens-docs-source-revision')
    }, location.href);
    var explicitCapabilities = new Set(featureInputs.filter(function (input) { return input.checked; }).map(function (input) { return input.dataset.feature; }));

    if (!target.valid) {
      targetStatus.textContent = 'Prompt unavailable: ' + target.errors.join(' ');
      targetStatus.dataset.state = 'invalid';
      prompt.value = 'A trusted target release could not be established. Do not use this builder output.\n';
      copyButton.disabled = true;
      warning.dataset.state = 'invalid';
      warning.textContent = 'Release metadata validation failed; no version or documentation URL was guessed.';
      return;
    }
    targetStatus.textContent = (target.sourceKind === 'development' ? 'Development preview' : 'Versioned documentation') + ' — integration target Test Lens ' + target.targetRelease + '; API links are pinned to /' + target.targetRelease + '/. Source revision: ' + target.sourceRevision + '.';
    targetStatus.dataset.state = target.sourceKind;

    function style() {
      var selected = styleInputs.find(function (input) { return input.checked; });
      return selected ? selected.value : 'auto';
    }

    function sameSelection(left, right) {
      return left.length === right.length && left.every(function (name, index) { return name === right[index]; });
    }

    function matchingPreset(explicit) {
      return Object.keys(PRESETS).find(function (name) {
        var preset = FEATURE_ORDER.filter(function (feature) { return PRESETS[name].indexOf(feature) >= 0; });
        return sameSelection(explicit, preset);
      }) || null;
    }

    function syncDependencies(resolution) {
      var reports = featureInputs.find(function (input) { return input.dataset.feature === 'reportsEvidence'; });
      featureInputs.forEach(function (input) {
        input.checked = resolution.effective.indexOf(input.dataset.feature) >= 0;
      });
      var dependencyActive = resolution.explicit.indexOf('reportUpload') >= 0 || resolution.explicit.indexOf('allure') >= 0;
      reports.disabled = dependencyActive;
      reports.closest('label').classList.toggle('is-required', dependencyActive);
      reports.setAttribute('aria-describedby', dependencyActive ? 'reports-dependency-note' : '');
    }

    function syncPreset(resolution) {
      var preset = matchingPreset(resolution.explicit);
      root.querySelectorAll('[data-preset]').forEach(function (button) {
        var active = button.dataset.preset === preset;
        button.setAttribute('aria-pressed', String(active));
        button.classList.toggle('is-active', active);
      });
      selectionMode.textContent = preset ? 'Preset: ' + root.querySelector('[data-preset="' + preset + '"]').textContent : 'Custom selection';
    }

    function update() {
      var resolution = resolveCapabilities(Array.from(explicitCapabilities));
      syncDependencies(resolution);
      syncPreset(resolution);
      var assessment = seleniumAssessment(versionInput.value, resolution.effective.indexOf('bidiNetwork') >= 0);
      warning.dataset.state = assessment.kind;
      warning.textContent = assessment.message;
      var explicitSummary = resolution.explicit.length ? resolution.explicit.map(function (name) { return FEATURE_LABELS[name]; }).join(' · ') : 'Core lifecycle only';
      var requiredSummary = resolution.required.length ? '; required dependency: ' + resolution.required.map(function (name) { return FEATURE_LABELS[name] + ' — ' + resolution.reasons[name]; }).join(' · ') : '';
      summary.textContent = 'Explicit: ' + explicitSummary + requiredSummary;
      prompt.value = buildPrompt({
        style: style(),
        capabilities: resolution.explicit,
        seleniumVersion: versionInput.value,
        targetRelease: target.targetRelease,
        publicDocsRoot: target.publicDocsRoot,
        sourceKind: target.sourceKind
      });
      copyStatus.textContent = '';
    }

    function applyPreset(name) {
      explicitCapabilities = new Set(PRESETS[name] || []);
      update();
    }

    function copyPrompt() {
      var value = prompt.value;
      function success() { copyStatus.textContent = 'Integration prompt copied.'; copyButton.focus(); }
      function fallback() {
        prompt.focus();
        prompt.select();
        var copied = false;
        try { copied = document.execCommand('copy'); } catch (ignored) { copied = false; }
        copyStatus.textContent = copied ? 'Integration prompt copied.' : 'Select the prompt and copy it manually.';
        copyButton.focus();
      }
      if (navigator.clipboard && navigator.clipboard.writeText) navigator.clipboard.writeText(value).then(success, fallback);
      else fallback();
    }

    root.querySelectorAll('[data-preset]').forEach(function (button) {
      button.addEventListener('click', function () { applyPreset(button.dataset.preset); });
    });
    styleInputs.forEach(function (input) { input.addEventListener('change', update); });
    featureInputs.forEach(function (input) {
      input.addEventListener('change', function () {
        if (input.checked) explicitCapabilities.add(input.dataset.feature);
        else explicitCapabilities.delete(input.dataset.feature);
        update();
      });
    });
    versionInput.addEventListener('input', update);
    copyButton.addEventListener('click', copyPrompt);
    update();
  }

  var api = {
    VERSION_TOKEN: VERSION_TOKEN,
    BIDI_MINIMUM: BIDI_MINIMUM.slice(),
    FEATURE_ORDER: FEATURE_ORDER.slice(),
    FEATURE_LABELS: Object.assign({}, FEATURE_LABELS),
    PRESETS: JSON.parse(JSON.stringify(PRESETS)),
    MODULES: MODULES,
    STYLE_LABELS: STYLE_LABELS,
    resolveCapabilities: resolveCapabilities,
    normalizedCapabilities: normalizedCapabilities,
    parseVersion: parseVersion,
    seleniumAssessment: seleniumAssessment,
    resolveDocumentationTarget: resolveDocumentationTarget,
    buildPrompt: buildPrompt,
    init: init
  };
  global.TestLensAiIntegrationBuilder = api;
  if (global.document) {
    var initializeCurrentPage = function () { init(global.document, global.location, global.navigator); };
    if (global.document$ && typeof global.document$.subscribe === 'function') global.document$.subscribe(initializeCurrentPage);
    if (global.document.readyState === 'loading') global.document.addEventListener('DOMContentLoaded', initializeCurrentPage);
    else initializeCurrentPage();
  }
})(typeof window === 'undefined' ? globalThis : window);
