# Test Lens 0.5.2 production release

Release preparation is in progress under the conditional browser policy below; fresh hosted verification is PENDING. No tag or immutable artifact is claimed until its external verification is recorded below. The earlier [regression audit](selenium-051-regression-audit.md) and [BiDi milestone](selenium-bidi-release-0.5.2.md) retain their historical local results.

## Source and documentation coverage

The prepared source is `release/bidi-0.5.2`, originally `c2d4fece`; `cdcc9344` identifies the earlier redirect-correction milestone, not the final candidate. The final source SHA and fresh hosted results for the conditional Chrome 157 policy are PENDING. Actual baseline is `v0.5.1` (`e6e2a19`). Seven intervening baseline commits concern documentation/branding and the withdrawal of Studio recommendations; they are not new runtime features. The only changes in the Studio module are its parent-version bump. No uncommitted Studio/Playground work is included.

Each entry below is described in the 0.5.2 changelog and release notes. No public API/test-source migration is required. Browser capability setup, consumer-owned Selenium alignment and optional capture activation remain required.

| Change | Commit | Regression test | Changelog | Documentation |
| --- | --- | --- | --- | --- |
| Primitive/boxed sizes and numeric/string listener IDs | `e10cb2b1` | `SeleniumBiDiCompatibilityTest`, `BinaryArtifactTest`, legacy bytecode consumer | Fixed | [Network](../advanced/network.md), [compatibility](../selenium-compatibility.md) |
| Owned listeners, partial cleanup, lazy remote initialization, capture failure invalidation | `e10cb2b1` | network source/diagnostics regressions; `BrowserBinaryTest` | Fixed | Network, compatibility |
| Preserve real redirect-hop request metadata when `beforeRequestSent` is absent; deduplicate by request ID and hop; retain observation timestamp and provenance | `2d971211` | `SeleniumBiDiNetworkCaptureSourceTest`: redirect projection, deduplication, bounded storage and trace provenance; focused source regressions 82 PASS locally | Fixed | [Network](../advanced/network.md), [release notes](release-notes-0.5.2.md) |
| Optional raw-wire probes default OFF; actual HTTP server-journal assertions retained | `cdcc9344` | Journal-based browser/report assertions: final local matrix 1359 PASS, zero failures/skips | Tested compatibility | Network, release notes |
| Overlapping restart generation | `e10cb2b1` | overlapping start source-ownership regression | Fixed | Network, BiDi audit |
| Same-JAR matrix and report server-journal/body-byte checks | `3a2df0df`, `5dfb1e1b` | `BinaryArtifactTest`, `NetworkReportCorrectnessTest` | Compatibility | Compatibility, regression audit |
| Release/CI/API gate correction and BOM alignment | `a4d24058` | prepared-policy, release tooling and API projection fixtures | Compatibility | [Release validation](selenium-bidi-release-0.5.2.md) |
| Potentially delivered action cannot be blindly repeated; checkbox post-click confirmation | `9a034be2` | `SmartClickFallbackCascadeTest`, `RuntimeInputRegressionTest`, `UiLocatorAmbiguousActionRegressionTest`, `RuntimeOverlayBrowserAuditIT` | Fixed runtime regressions, Migration | [Actions](../elements/actions.md) |
| Overlay cancellation/conditional dismissal, consistent null input | `9a034be2` | `OverlayWaitCancellationRegressionTest`, `LegacyOverlayRecoveryRegressionTest`, `RuntimeInputRegressionTest`, browser smoke | Fixed runtime regressions | Actions, [waiting](../elements/waiting.md) |
| React cancellation before another lookup/action | `00bf23f1` | `ReactSafeExecutorCancellationAuditTest` | Fixed runtime regressions | [React/SPA](../features/react-spa.md) |
| Critical finalization failures, cleanup and interrupted flag | `36c61081` | `LifecycleFailureAuditTest`, existing JUnit/TestNG lifecycle suites | Fixed runtime regressions | [TestLens lifecycle](../reference/test-lens.md) |
| Fatal errors preserved by both locator metadata encoders | `80e50f1c` | `LocatorObservationFatalAuditTest`, `LocatorObservationMetadataFatalRegressionTest` | Fixed runtime regressions | TestLens lifecycle, regression audit |
| Measured durations and terminal HUD preservation | `c0496789` | `MeasuredOperationDurationAuditTest`, `HudPanelJsTest` | Fixed runtime regressions | [Trace](../observability/trace.md), [visual diagnostics](../observability/visual-diagnostics.md) |
| Safe HTML artifact/archive links and report boundaries | `c0496789` | `ReportLinkSecurityAuditTest`, `ReportDataBoundaryAuditTest` | Fixed runtime regressions | [Reports](../observability/reports.md) |
| Version-specific setup, upgrade and limitations | `35690ba1`, `c2d4fece` | strict builds, signature/link/version-boundary gates | Migration, Compatibility | [What's new](../whats-new-0.5.2.md), README and integration guides |

These fixes change erroneous behavior: an ambiguous failed action now fails rather than redispatching; interrupted work stops; critical errors propagate; measured durations and terminal HUD status become accurate. Existing timeout/fallback defaults remain. Legacy overlay dismissal retains its 500 ms cap and boolean meaning (dismissal invoked, not guaranteed disappearance). JavaScript dispatch is not declared equivalent to native input.

## The 39 local skipped tests

Classification uses every `<skipped>` testcase in the clean 1845-test reactor XML, not only suite names. The source conditions were inspected.

| Group | Count | Reason / environment | Release-gate decision |
| --- | --- | --- | --- |
| Documentation-only example methods | 25 | Explicitly disabled examples require a real driver, authenticated application or caller-supplied video; not executable generic fixtures | Intentional. Compile examples; use dedicated deterministic Chrome/Grid integration gates instead. No setup failure or regression established |
| Opt-in diagnostic/performance harnesses | 9 | Explicit properties absent: `compatibility.compare.performance`, `compatibility.performance`, `compat.static.performance`, `compat.static.selfAudit`, `testlens.selector.policy.performance`, `testlens.selector.engine.performance`, `selector.audit.perf`, `selector.history.perf`, `selector.tooling.perf` | Intentional offline/scale diagnostics, not correctness gates; no external service required. Not counted as PASS |
| Windows symbolic-link assumptions | 5 | The platform cannot create fixture symbolic links | Expected local environmental skips; must execute on capable hosted Linux. Not silently accepted if Linux setup fails |

The five platform-dependent testcases are:

- `TrustedRepairApplierTest#rejectsDeclarationRangesWithZeroOrMultipleMatchingSelectorExpressions`
- `ApplicationModelStoreTest#oversizedFileIsRejectedAsStoreExceptionAtPersistenceBoundary`
- `MigrationAdditionalGitFixturesTest#detectsSparseCheckoutWhenSupported`
- `JavaLocatorScannerTest#isolatesMalformedFilesAndReportsKotlinAndIncompleteClasspath`
- `ReportUploaderTest#bearerCustomHeadersAndResponseDiagnosticsAreBoundedAndRedacted`

The nine opt-in methods are `CompatibilityComparisonPerformanceTest#measuresHeadlessAndObservabilityComparisonPipeline`, `CompatibilityManifestPerformanceTest#offlineBuildSerializeReadMatrix`, `StaticCompatibilityHarnessTest#offlineScaleHarness`, `StaticCompatibilityHarnessTest#selfAuditRepository`, `PolicyFeedbackPerformanceHarnessTest#measuresDraftAndPatternPreviewScalingWithoutCiThresholds`, `SelectorEnginePerformanceHarnessTest#measuresOfflineScalingWithoutCiThresholds`, `SelectorAuditPerformanceHarnessTest#measuresAuditScaling`, `SelectorHistoryPerformanceHarnessTest#measuresSimilarityHistoryAndPolicyPreviewScaling`, and `SelectorToolingPerformanceHarnessTest#recordsOfflineSmallMediumAndLargeScanMetrics`.

The 25 example methods are enumerated in the checked local `target/release-052-skips.json` and are all the skipped methods in `selenium-test-lens-examples`; their annotation reasons identify driver/auth/video prerequisites. Hosted and local results must be reported separately. The 1242-test same-JAR matrix and each selected 5-test browser smoke have zero skips.

## Publication process and evidence

The existing `central-release` Maven profile creates sources/Javadoc, signs with GPG and stages through Sonatype's Central plugin. `autoPublish=false` / `waitUntil=validated` requires explicit publication of the validated deployment. There is no Central publishing GitHub workflow; Release Check validates packaging without signing. This release follows the existing safe ordering: merge verified source, stage and publish Central, verify artifacts, then create the immutable tag which triggers documentation publication.

Signing preflight is complete. The signing key was verified against the public 0.5.1 signature fingerprint `7C21B14451B2C876E87796C2723272178ED7A876`. Interactive local signing and verification passed, and Maven parent-POM signing and verification passed. No password or private key was exported. These checks establish signing readiness, not a Central deployment or publication.

Baseline synchronization was separated from the patch: [PR #2](https://github.com/Test-Lens/selenium-test-lens/pull/2) merged as `2935dbede2e819725554a34d1896449532b7f578` after [CI run 44515897545](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44515897545) passed. [Release PR #3](https://github.com/Test-Lens/selenium-test-lens/pull/3) exists. Local release work remains isolated and preserves the original dirty work. The existing local Maven `central-release` profile remains the staging path, with `autoPublish=false`; no Actions publication workflow has been introduced.

Release version 0.5.2 was absent from local/remote tags, GitHub Releases and Central at initial preflight. Availability is checked again immediately before immutable actions. PR, hosted run IDs, merge SHA, Central deployment/public artifacts, tag, documentation URLs and post-release consumer evidence are recorded here as each completes.

### Local signing validation

After the maintainer entered the passphrase through local GPG pinentry, a new detached proof signature verified successfully. On the clean `cd00bef8` checkout the existing Maven profile then completed signed verification in 23.630 seconds. **All 61 signatures across the 16 publishable coordinates verified against the expected fingerprint**, covering the parent POM, 15 module POMs, main JARs, sources and Javadoc. The local inventory records each file's SHA-256 and signer in `target/gpg-release-preflight/all-published-artifact-signatures.json`. This is local signing/packaging evidence, not a Central deployment or final release certification.

The signing command used the existing `central-release` profile and configured local `central` server credentials; their values were not logged. The explicit nonsecret overrides were:

```powershell
mvn -B -Pcentral-release -DskipTests `
  '-Dgpg.keyname=7C21B14451B2C876E87796C2723272178ED7A876' `
  '-Dgpg.executable=C:/Program Files/GnuPG/bin/gpg.exe' `
  '-Dgpg.useagent=true' verify
```

The profile's batch/loopback arguments require an already primed `gpg-agent` cache; a separate `gpg --pinentry-mode ask --armor --detach-sign` proof provides the local pinentry step when that cache expires. No passphrase property, settings entry, environment variable, private-key export or key change was introduced. The GitHub runner has no access to the local key; the existing Release Check remains an unsigned validation workflow. Publication would use the established local, manually validated Central deployment process only after the release blocker is resolved.

## Current status

**PREPARED, UNPUBLISHED; CONDITIONAL VERIFICATION PENDING.** The maintainer superseded the absolute Chrome 158 hold with permission for a controlled Chrome 157 release gate, provided unchanged tests prove 157 and the independently reproduced external 158 failure has no available correction. This permission does not establish those results: fresh hosted evidence and the final assessment remain PENDING. No Central publication, immutable tag or GitHub Release has been created. BrowserStack and the public-artifact post-release smoke remain NOT RUN.

### Conditional Chrome 157 gate and visible Chrome 158 canary

Required CI browser checks pin **Chrome and ChromeDriver 157.0.8103.78** as a matching pair. The same frozen-JAR Selenium matrix remains **4.39.0, 4.40.0, 4.41.0, 4.45.0, 4.46.0, 4.47.0, 4.51.0, 4.52.0 and 4.53.0**, including unchanged exact server-journal and report-data assertions. Required standalone Selenium-only checks must each complete **50 distinct redirect stimuli**, both with direct ChromeDriver and through Grid 4.53.0. Failed stimuli are not retried or relabeled as skipped. The remaining required hosted gates must also pass on the final source.

A separate **nonblocking Chrome/ChromeDriver 158.0.8127.36 canary** runs both direct and Grid paths. Its actual outcomes, including failing assertions, remain visible in summaries, JUnit XML and raw-event/peer-journal artifacts. A nonblocking outcome changes the release decision policy; it does not turn a failure into a PASS. A Chrome 157 PASS does not certify Chrome 158 network reliability or imply full Chrome 158 support.

The official Chrome for Testing metadata dated **2026-11-19** still identifies **158.0.8127.36** as stable; no newer stable correction was available in that check. Earlier standalone Linux failures below establish that neither Lens nor Grid is necessary, and inspection of raw decoding found no input-map mutation. Attribution to a specific vendor component remains unproven. The final no-available-fix assessment must accompany the fresh hosted evidence; pinning 157 alone does not resolve the external failure.

Upstream research identified official Chromium BiDi commit [089ef525, “Match redirect ExtraInfo events to the correct hop”](https://github.com/GoogleChromeLabs/chromium-bidi/commit/089ef52586b3dcac9390ce6c938008c742ad9449), dated November 5, describing late CDP ExtraInfo being assigned to the wrong redirect hop. Stable Chrome 158.0.8127.36 at revision **1727472** lacks the correction at position **1736133**. Direct inspection of the shipped [158 source](https://raw.githubusercontent.com/chromium/chromium/158.0.8127.36/third_party/chromium-bidi/src/bidiMapper/modules/network/NetworkRequest.ts) and [159 Beta source](https://raw.githubusercontent.com/chromium/chromium/159.0.8154.13/third_party/chromium-bidi/src/bidiMapper/modules/network/NetworkRequest.ts) confirms that the latter includes the previous-hop/late-ExtraInfo routing correction. Chrome **159.0.8154.13 is Beta**, not stable; effectiveness against this reproducer remains UNVERIFIED. No verified stable correction for Chrome 158 was found. This is a concrete component-level candidate, not proof that this exact change resolves the observed failure; paired raw WebSocket/CDP evidence would be needed to assign the precise failing transition. The unchanged assertions already exclude a mere assumption about callback ordering: eventual HTTP status and terminal completion are required irrespective of arrival order.

| Final evidence required | Current state | Completion evidence to record |
| --- | --- | --- |
| Final candidate and unchanged test/assertion scope | PENDING | Source SHA, CI configuration SHA and probe/report assertion comparison |
| Required Chrome 157 browser pair and nine-version same-JAR matrix | PENDING | Fresh hosted run URL, exact browser/driver versions, frozen JAR SHA-256, per-version test/failure/error/skip totals |
| Required standalone Chrome 157 direct path | PENDING | Hosted run URL, 50 completed distinct stimuli, JUnit totals and peer/raw artifact references |
| Required standalone Chrome 157 Grid path | PENDING | Hosted run URL, matching browser/driver and Grid versions, 50 completed distinct stimuli, JUnit totals and peer/raw artifacts |
| Separate Chrome 158 direct/Grid canaries | PENDING | Each actual outcome, executed stimuli, JUnit totals, visible summary and artifact references, including any FAIL |
| External failure and no available correction | Prior independent failures retained; final assessment PENDING | Current upstream/version check, examined correction options and conclusion tied to the final validation date |
| Other required hosted CI, release packaging and documentation | PENDING for final candidate | Run URLs, source SHA, exact results and classified skips |
| Immutable publication and public-artifact consumer smoke | NOT RUN | Central deployment/public artifacts, tag, GitHub Release, documentation URLs and clean public consumer evidence after authorization |

The signing/packaging evidence above remains valid for its recorded source and is not repeated as a new signing result. Existing passing and failing rows below are historical evidence, not substitutes for these pending final checks.

### Historical finalization checks and Chrome 158 redirect failure

Commit `2d971211` retains real redirect request metadata with its observation phase; `cdcc9344` keeps optional raw-wire probes OFF by default. The focused source regressions passed 82 tests. Independent reviews found no actionable defects in either change. The new local nine-version same-JAR matrix passed **1359 tests, zero failures/skips**, including Chrome 158, Grid and server-journal report comparisons. Lens SHA-256: `972981F9C86FA796706768A36CF34D518ADDF8D46FDE8B55DB5832C6F79104FB`.

| Check | Observed result | Evidence |
| --- | --- | --- |
| Fresh checkout, full reactor and release-profile packaging | PASS: 1811 tests, 39 classified skips, zero failures; sources/Javadoc and publication whitelist checked without signing | Clean `cd00bef8` checkout; local `production-final-verify-unsigned.log` and `production-packaging-check.log` |
| Hosted Linux reactor | PASS: 1816 tests, 34 intentional skips, zero failures; all five Windows-only skipped tests executed successfully | [CI 44526160581](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44526160581), `reactor-test-results` artifact; the complete workflow is not a PASS because its matrix failed |
| Hosted same-JAR matrix | 1359 PASS on one complete run; an independent repeat failed the Grid redirect journal comparison | [44525749243](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44525749243), [44526160581](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44526160581) |
| Hosted complete manual CI, including headed Chrome and Firefox | PASS at `cd00bef8`, retained as evidence of that run, not a waiver for the intermittent failure | [44526149198](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44526149198) |
| Hosted release packaging/clean-room gate | PASS at `cdcc9344`, signing intentionally disabled | [Release Check 44525892997](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44525892997) |
| Public API | PASS: 791 types / 5700 callables; stable/advanced baseline delta zero | `check-public-api.ps1`, `check-public-api-since.ps1` |
| Hosted strict documentation | PASS at `ad8605ac` | [Documentation 44527264850](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44527264850) |

The failing Grid scenario on Chrome **158.0.8127.36**, Grid **4.53.0**, and Java **21** received a raw `network.responseStarted` and `network.responseCompleted` carrying **200 /case/redirect**, then `responseStarted` for **200 /case/final**, but no completed final response and no 302 response in any observed phase. The controlled HTTP peer journal contains **302 /case/redirect → 200 /case/final**. This was reproduced with client 4.39.0 in [diagnostic CI 44527256815](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44527256815); a normal run without probes failed with client 4.45.0. The bounded raw observer used Lens's existing connection and did not map through Lens's response model. Status remained STARTED with no callback failure or dropped events, so connection health alone does not establish complete or accurate traffic.

Changing Lens's URL preference cannot restore the missing 302 response or the response count. Fabricating it from the expected server behavior is not an acceptable fix. The exact browser/Grid component responsible remains under investigation; this evidence alone does not assign the defect exclusively to Chrome. A standalone public-Selenium-only reproducer is supplied in `consumer-tests/selenium-bidi/protocol-reproducer`, with no Lens dependency and no retry of failed stimuli. It tests 50 distinct redirects and writes bounded, sanitized raw events and peer journals. Local Windows runs are not evidence that the hosted Linux defect is fixed.

The standalone reproducer subsequently established that **neither Lens nor Grid is necessary for the failure**:

| Standalone Selenium-only run | Environment | Result |
| --- | --- | --- |
| [44530948312](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44530948312), `9819d4ca` | Linux, client 4.53.0, Grid 4.53.0, Chrome/ChromeDriver 158.0.8127.36 | FAIL at distinct stimulus 20: peer 302 `/redirect/20` then 200 `/final/20`; raw completed 200 `/redirect/20`, no completed final response |
| [44531701988](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44531701988), `1c755c44` | Same hosted Linux versions, Grid | PASS for 50 distinct stimuli; this passing repeat does not waive the failure |
| Same paired run, direct `ChromeDriver`, no Grid | Linux, client 4.53.0, Chrome/ChromeDriver 158.0.8127.36 | FAIL at stimulus 41 with the same incorrect 200/redirect and missing 302/final completion |
| Local Windows probes | Grid 4.53.0, Chrome 158, clients 4.53.0 and 4.45.0 | 50 stimuli PASS per run; not a hosted-Linux certification |

Both hosted failures set `outcome=PROTOCOL_MISMATCH`, `evidenceOverflow=false`, with one failing JUnit test and zero skipped tests. The tests stop at the first mismatch; the 50 stimuli are different requests, not retries of a failed action. Separate Grid and local Surefire artifacts and raw-event/peer-journal JSON are retained. The earlier diagnostic launch [44530051630](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44530051630) failed before executing this standalone test because a PowerShell Maven property needed quoting; `743a8da4` corrected it. That launch is NOT RUN for the standalone scenario, not a protocol failure or PASS.

The remaining boundary is the Linux Chrome/ChromeDriver BiDi path. Pinpointing the vendor component or supplying an upstream correction remains outstanding; the observed data cannot be repaired truthfully from the Selenium event fields alone. These observations originally caused an absolute release hold. The current conditional policy above replaces that decision rule while retaining every failing and passing result and the exact server-journal assertions.

Release authorization now depends on completing the conditional Chrome 157 policy and all required hosted gates on the final source, with the external Chrome 158 limitation and no-available-fix assessment explicit. Any later correction or newer component must still pass the unchanged standalone and report assertions before Chrome 158 is certified; compatibility is not inferred from a version number or an individual passing repeat.

Separately, [PR #4](https://github.com/Test-Lens/selenium-test-lens/pull/4) fixed only an existing Studio browser fixture's readiness race. Its complete hosted checks passed before merge `5a47e7b0`; the release branch merges that approved baseline. There are no Studio production changes in the 0.5.2 diff, and the only Studio module change relative to current main remains its parent-version bump. The original working tree's HEAD, index, status and all 256 recorded file hashes remain unchanged.

Public documentation checks after that merge confirmed identical SHA-256 values for all seven existing version index pages (0.1.0, 0.2.0, 0.3.0, 0.3.1, 0.4.0, 0.5.0, 0.5.1). `versions.json` still assigns `latest` to 0.5.1. The 0.5.2 main JAR remains absent from public Central (HTTP 404); no release tag or immutable publication has been created by this work.

## Complete documentation-example skip inventory

All 25 are intentionally disabled illustrative methods, not hosted browser gate failures:

- `ActionabilityExampleTest#checkElementActionabilityBeforeClicking` — Documentation-only example; requires a real WebDriver.
- `AuthSessionStateExampleTest#authSessionStateUsage` — Documentation-only example; requires a real WebDriver and authenticated app.
- `BrowserExecutionConfigExampleTest#manualFactoryResolvesExecutionIntentBeforeCreatingTheDriver` — Documentation-only example; requires a real Chrome installation.
- `BusinessAssertionsExampleTest#businessAssertionsUsage` — Documentation-only example; requires a real WebDriver.
- `BusinessStepDslExampleTest#businessStepDslUsage` — Documentation-only example; requires a real WebDriver.
- `HtmlTraceReportExampleTest#logOnlyHtmlReportUsage` — Documentation-only example; requires a real WebDriver.
- `HudThemeExampleTest#blackAndColorsHudThemeUsage` — Documentation-only example; requires a real WebDriver.
- `HudThemeExampleTest#hudThemeUsage` — Documentation-only example; requires a real WebDriver.
- `HudThemeExampleTest#customHudThemeUsage` — Documentation-only example; requires a real WebDriver.
- `NativeSeleniumObservationExampleTest#existingPageObjectsCanKeepUsingNativeSelenium` — Documentation-only example; requires a real WebDriver.
- `NetworkDiagnosticsExampleTest#networkDiagnosticsUsage` — Documentation-only example; requires a real browser/network-capable WebDriver.
- `NetworkWaitForResponseExampleTest#waitForResponseUsage` — Documentation-only example; requires a real browser/network-capable WebDriver.
- `OverlayPolicyExampleTest#configureKnownBlockingOverlays` — Documentation-only example; requires a real WebDriver.
- `PlaywrightStyleLocatorsExampleTest#playwrightStyleLocatorsUsage` — Documentation-only example; requires a real WebDriver.
- `ReactActionabilityExampleTest#checkReactReadinessBeforeClicking` — Documentation-only example; requires a real WebDriver.
- `ReactUsageExampleTest#reactSafeUsage` — Documentation-only example; requires a real WebDriver.
- `RetryableAssertionsExampleTest#retryableAssertionsUsage` — Documentation-only example; requires a real WebDriver.
- `RetryableLocatorExampleTest#retryableLocatorUsage` — Documentation-only example; requires a real WebDriver.
- `ScreenshotEvidenceExampleTest#screenshotEvidenceUsage` — Documentation-only example; requires a real WebDriver.
- `ScreenshotEvidenceExampleTest#screenshotOnFailedStepUsage` — Documentation-only example; requires a real WebDriver.
- `SeleniumUsageExampleTest#seleniumOverlayUsage` — Documentation-only example; requires a real WebDriver.
- `SeleniumUsageExampleTest#fastObservabilityKeepsTestSemantics` — Documentation-only example; requires a real WebDriver.
- `TraceEventMappingExampleTest#traceEventMappingUsage` — Documentation-only example; requires a real WebDriver.
- `TraceEvidenceModelExampleTest#traceEvidenceModelUsage` — Documentation-only example; requires a real WebDriver.
- `VideoEvidenceExampleTest#videoEvidenceUsage` — Documentation-only example; requires external video artifact.
