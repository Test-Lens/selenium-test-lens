# Test Lens 0.5.2 production release

This document records the release-gating policy and pre-publication audit milestones. Source metadata is prepared in publication form; authoritative completion receipts for the immutable release will be recorded only after complete validation and publication. No new external publication or tag is claimed; the Windows qualification has passed, while final-source release validation remains PENDING. The earlier [regression audit](selenium-051-regression-audit.md) and [BiDi milestone](selenium-bidi-release-0.5.2.md) retain their historical local results.

## Source and documentation coverage

The prepared source is `release/bidi-0.5.2`, originally `c2d4fece`; `cdcc9344` identifies the earlier redirect-correction milestone, not the final candidate. The Linux stock-157 gate has been rejected, as has the official-mapper configuration introduced in CI commit `537f8cab`. The separate Windows stock-157 configuration passed the complete matrix at `d4700cc46c5d0916d344d190d09ae20a5a9ba8e6`; the final integrated source SHA and its required CI results remain PENDING. The official-159 hosted gate ran at `bb7ba9fc`; independent native/CDP evidence followed at `12959089bb5c3410e1960d6ef82422dcb9d94f83`, and the Chrome-156 fallback failed its full report gate at `7caf2e49b953582e91347e7469f42276ea95bb75`. Windows and Linux qualifications are separate; the final source must repeat all required checks with one shared frozen artifact. Actual baseline is `v0.5.1` (`e6e2a19`). Seven intervening baseline commits concern documentation/branding and the withdrawal of Studio recommendations; they are not new runtime features. The only changes in the Studio module are its parent-version bump. No uncommitted Studio/Playground work is included.

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

**WINDOWS STOCK 157 CONFIGURATION VERIFIED; FINAL-SOURCE RELEASE GATE PENDING.** Under the authorized condition-C exception, the exact Windows Chrome/ChromeDriver 157.0.8103.78 configuration is the browser gate. It passed all unchanged nine-version matrix/report checks and both standalone 50-redirect probes. Required CI now builds once on Linux, uploads the frozen artifacts, then runs the blocking Windows browser matrix against those same JARs. Linux stock 157/158 network integrity remains uncertified; stock-158 canaries retain visible actual outcomes. No custom or official mapper override is used by this gate. No new external publication receipt or tag is claimed; publication-form metadata is source preparation. BrowserStack remains NOT RUN.

### Rejected Linux stable Chrome 158 with official mapper 159

CI commit `537f8cab` introduces stable **Chrome/ChromeDriver 158.0.8127.36** with official npm **`chromium-bidi@159.0.8154-29`**. Hosted [run 44685534676](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44685534676) at `bb7ba9fc` rejected this configuration. The bundle contains the [upstream redirect-hop correction](https://github.com/GoogleChromeLabs/chromium-bidi/commit/089ef52586b3dcac9390ce6c938008c742ad9449) absent from npm `158.0.8127-72`. No newer stable Chrome was available. This uses a mapper bundle, not Chrome 159 Beta; the Beta browser is not used or recommended.

The [CI composite for the rejected configuration](https://github.com/Test-Lens/selenium-test-lens/blob/537f8cab/.github/actions/setup-bidi-browser/action.yml) downloads the [official tarball](https://registry.npmjs.org/chromium-bidi/-/chromium-bidi-159.0.8154-29.tgz), verifies its pinned SHA-512 before extraction, then verifies this entry separately:

- Entry: `package/out/Default/gen/src/mapperTab.js`.
- Mapper SHA-256: `223556758eb4543c8e6fb206a9205192d2122942fc939148dfa1a30d7c48e4b5`.
- Chrome and ChromeDriver: matching `158.0.8127.36`.
- Public ChromeDriver service argument: `--bidi-mapper-path=<absolute verified mapper path>`.

The flag is exposed in ChromeDriver help and consumed by its [session setup](https://chromium.googlesource.com/chromium/src/+/refs/tags/158.0.8127.36/chrome/test/chromedriver/session_commands.cc); [server option declarations](https://chromium.googlesource.com/chromium/src/+/refs/tags/158.0.8127.36/chrome/test/chromedriver/server/chromedriver_server.cc) provide the other source boundary. For example, after verifying the official bundle, the rejected experimental service was launched with:

```text
chromedriver --port=9515 --bidi-mapper-path=/absolute/verified/mapperTab.js
```

This is a **ChromeDriver service flag**, not a Chrome browser argument, session capability or Lens API. CI wraps the actual ChromeDriver service executable so both direct and Grid-launched services receive the override, records launch/provenance artifacts and keeps stock canaries separate. There is no production Lens injection, custom mapper patch, manual BiDi initialization or `getHandle()` workaround. Public API, test sources' HTTP expectations, response counts, terminal-completion requirements and report assertions are unchanged. An official cross-version bundle is not a blanket compatibility guarantee; only the exact configuration can be certified after complete tests.

A Windows direct ChromeDriver service trial on port 9515 completed **50 stimuli PASS** with the official override. It was not a true Grid run and is not hosted-Linux evidence. Subsequent Linux verification failed. Any replacement must pass the unchanged frozen-JAR matrix on **4.39.0, 4.40.0, 4.41.0, 4.45.0, 4.46.0, 4.47.0, 4.51.0, 4.52.0 and 4.53.0**, exact server-journal/report checks, and **50 standalone redirects each through direct ChromeDriver and real Grid 4.53.0**. API, full reactor, packaging and strict documentation gates must pass on the final source. No failed stimulus is retried or relabeled as skipped.

Stock Chrome/ChromeDriver **158.0.8127.36 direct/Grid canaries** remain separate and nonblocking, preserving actual PASS/FAIL/NOT RUN outcomes, summaries, XML and raw/peer artifacts. Passing mapper experiments would not certify unpatched stock 158. Transient stock-157 or stock-158 passes do not erase independently observed failures.

Independent [native JSON/CDP run 44688547436](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44688547436) at `12959089bb5c3410e1960d6ef82422dcb9d94f83` eliminates Selenium and Lens parsing from the failure path. Stimulus 1 completes; at stimulus 2, CDP records final-hop request ExtraInfo **before** its `requestWillBeSent`, followed by response ExtraInfo 200, `responseReceived` 200 and `loadingFinished`. BiDi emits only the redirect hop (correct 302), with no final-hop events within the declared deadline. The server journal independently contains exactly one 302 redirect and one 200 final response. Mapper 159 routes the early ExtraInfo to the previous hop, overwrites its stored ExtraInfo, then creates a final hop that waits for an event already consumed. This is direct trace and source evidence, not certification of an experimental patch.

### Rejected Linux stock Chrome 156 fallback

[Run 44689160257](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44689160257) at `7caf2e49b953582e91347e7469f42276ea95bb75` passed all **9 binary** and **9 basic-browser** matrix rows; the latter cover local and remote execution, totaling **18 basic-browser cases**. The complete matrix recorded **1368 tests: 1350 PASS, 18 FAIL, 0 SKIP**. Standalone direct ChromeDriver and real Grid each passed **50 distinct redirects**. However, **all 18 report scenarios failed**: for an uncompressed 11-byte server response, BiDi reported `bodySize=144`. Redirect-only success does not waive report correctness. Stock Chrome 156 is therefore rejected as a complete release configuration.

### Bounded mapper experiment; not a release candidate

A local experiment against the official npm 159 modules reproduced early ExtraInfo routing with upstream network tests and additional regressions. The original modules passed 124 of 131 tests; a narrow FIFO derivative passed 130 of 131. The remaining test, `FIFO boundary: early first ExtraInfo does not belong to a no-ExtraInfo predecessor`, still observes the wrong hop when `redirectHasExtraInfo=false`. The failing assertion remains enabled. Correcting already-emitted data would require a broader change to event/interception semantics; the derivative is **NOT PRODUCTION READY** and is not used by release gates or Lens production code.

This experiment used esbuild-transpiled upstream TypeScript tests with the actual npm modules, not Chromium's official GN/Ninja build. Its isolated commands, patch, baseline/fixed logs and hashes are retained locally in `target/mapper-fifo-experiment/README.md` and `candidate/provenance.json`. The derivative mapper SHA-256 is `d247d662f23e19da07c162664ddd9b97a9ced8db7b0600b1638ffc3253ff1695`; it has no browser certification. These results do not establish an available release fix.

### Verified Windows stock 157 configuration and final-source gate

[Run 44690264321](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44690264321) at `55c90dcf` first passed all nine binary, local-browser and local-report rows and the standalone 50-redirect direct probe on Windows. Its Grid scenarios could not connect after a process started in a previous workflow step; they are not protocol evidence or PASS.

[Run 44691010963](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44691010963) at `d4700cc46c5d0916d344d190d09ae20a5a9ba8e6` kept Grid startup, all unchanged probes and cleanup within the same owned-process lifetime. With matching stock Chrome/ChromeDriver **157.0.8103.78 on Windows**, all **nine Selenium rows passed: 152 tests each, 1368 PASS, 0 FAIL, 0 ERROR, 0 SKIP**. This includes direct/local and Grid browser scenarios and report/server-journal comparisons across the existing nine Selenium runtimes. The independent standalone direct and real-Grid probes each completed **50 distinct redirects PASS**. No status, response-count, body-size or completion assertion was relaxed; no failed stimulus was retried. The frozen Lens JAR SHA-256 is `15252599CC308060461AEA7128281999618C7D47796910EF7FB40235689364B4`.

CI commit `fb155a29` prepares the required cross-OS contract; its final hosted completion is not claimed here. The required final CI contract separates Linux binary verification from Windows browser verification: Linux builds the Lens/core/overlay/legacy artifacts once, runs all nine binary rows and uploads that frozen run. The blocking Windows nine-version browser/report matrix consumes the same artifact using `-ReuseFrozenRun` with `-BrowserTests`; it must not substitute a Windows rebuild. Both standalone direct/Grid 50-redirect probes remain required. Other Linux Chrome integration, consumer and headed checks pin stock 157; their narrower checks do not certify the failing Linux network scenario. Stock 158 canaries remain separate and visible. The final integrated source SHA and complete required hosted results are **PENDING**; the successful diagnostic run is configuration evidence, not a release publication receipt.

| Required evidence | Current state | Completion record |
| --- | --- | --- |
| Chrome 158 / official mapper 159 | REJECTED | Hosted run 44685534676 at `bb7ba9fc`; independent native/CDP run 44688547436 at `12959089bb5c3410e1960d6ef82422dcb9d94f83`, failing stimulus 2 |
| Chrome 156 nine-version binary and basic-browser matrix | 9 binary PASS; 9 basic-browser rows / 18 local+remote cases PASS | Run 44689160257 at `7caf2e49b953582e91347e7469f42276ea95bb75` |
| Chrome 156 standalone direct and real Grid | 50 redirects PASS each | Same run; narrower than the complete report gate |
| Chrome 156 report matrix | 18 FAIL | Expected body size 11; reported 144; no assertions relaxed |
| Qualified browser/transport configuration | Windows stock Chrome/ChromeDriver 157.0.8103.78 PASS | Run 44691010963 at `d4700cc46c5d0916d344d190d09ae20a5a9ba8e6`; 1368 matrix tests and 50 redirects each direct/Grid |
| Final source and shared-artifact CI | PENDING | Linux builds once; nine binary rows; Windows blocking browser/report matrix reuses exactly the uploaded frozen JARs |
| Reactor, packaging, API and strict documentation | Final-source verification required | Historical passes retain their recorded scope |
| Immutable publication and public consumer smoke | No new publication/tag or receipt | Source publication form and historical signing checks do not establish external publication |

The existing 61-signature/16-coordinate signing evidence remains historical evidence for its recorded source; no new signing result is asserted. The following preflights and failures retain their original scope.

### Rejected Linux stock Chrome 157 gate

In [run 44683766883](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44683766883) at `f195`, the unchanged standalone stock-157 Linux direct probe failed at **stimulus 43**: the peer journal contained 302/redirect followed by 200/final, while raw BiDi supplied 200/redirect and omitted the required 302 redirect completion and 200 final completion. All nine Lens matrix rows passed in that run and Grid completed 50 stimuli; exact matrix totals await the retained reports and are not inferred here. Those passes do not waive the standalone failure. This run rejects the Linux stock-157 strategy despite the earlier passing preflight below; it does not negate the separately verified Windows configuration.

Preflight [44682004928](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44682004928) at `375c9b08` verified the exact 157 browser/driver pair, all nine Chrome/Grid/report rows and both 50-stimulus standalone paths. The workflow itself failed because `stopAndAssertionObserveAConsistentValidSnapshot` incorrectly demanded PASS for every concurrent stop/assertion interleaving. Production intentionally releases the lock around health I/O and rejects a changed generation. Test-only commit `f9b7dddb` replaces the scheduling race with two controlled cases: a generation change must produce the exact rejection with zero PASS logs, while a fresh assertion of the valid stopped snapshot passes; ordered active/stop/stopped assertions pass. No production code or HTTP expectation changed. All **83** focused tests passed, including **46** lifecycle tests against the exact preflight frozen JAR with Selenium 4.40.0. Independent review found no actionable issue. The corrected test is part of the required final matrix, not a waiver for the failed preflight.

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
| Same paired run, direct `ChromeDriver`, no Grid | Linux, client 4.53.0, Chrome/ChromeDriver 158.0.8127.36 | FAIL at stimulus 41 with the same incorrect 200/redirect and missing 302 redirect completion and 200 final completion |
| Local Windows probes | Grid 4.53.0, Chrome 158, clients 4.53.0 and 4.45.0 | 50 stimuli PASS per run; not a hosted-Linux certification |

Both hosted failures set `outcome=PROTOCOL_MISMATCH`, `evidenceOverflow=false`, with one failing JUnit test and zero skipped tests. The tests stop at the first mismatch; the 50 stimuli are different requests, not retries of a failed action. Separate Grid and local Surefire artifacts and raw-event/peer-journal JSON are retained. The earlier diagnostic launch [44530051630](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44530051630) failed before executing this standalone test because a PowerShell Maven property needed quoting; `743a8da4` corrected it. That launch is NOT RUN for the standalone scenario, not a protocol failure or PASS.

These historical observations identified the Linux Chrome/ChromeDriver BiDi path without pinpointing a vendor component. The later independent CDP trace above narrows the official-159 failure to early ExtraInfo routing; a correction passing the complete release gate remains outstanding. Missing responses cannot be repaired truthfully from the Selenium event fields alone. These observations originally caused an absolute release hold. The attempted Linux stock-157 exception subsequently failed as well. The official-mapper configuration above has since failed hosted verification; every earlier passing and failing result retains its scope.

Release authorization now requires complete unchanged final-source CI using the qualified Windows stock-157 configuration and the same Linux-built frozen JAR, plus every other required gate, with all rejected Linux configurations explicit. Any later correction or newer component must still pass the unchanged standalone and report assertions before Chrome 158 is certified; compatibility is not inferred from a version number or an individual passing repeat.

Separately, [PR #4](https://github.com/Test-Lens/selenium-test-lens/pull/4) fixed only an existing Studio browser fixture's readiness race. Its complete hosted checks passed before merge `5a47e7b0`; the release branch merges that approved baseline. There are no Studio production changes in the 0.5.2 diff, and the only Studio module change relative to current main remains its parent-version bump. The original working tree's HEAD, index, status and all 256 recorded file hashes remain unchanged.

Public documentation checks after that merge confirmed identical SHA-256 values for all seven existing version index pages (0.1.0, 0.2.0, 0.3.0, 0.3.1, 0.4.0, 0.5.0, 0.5.1). At that preflight checkpoint, `versions.json` assigned `latest` to 0.5.1 and the 0.5.2 main JAR was absent from public Central (HTTP 404); no release tag or immutable publication had been created. These are historical checkpoint facts, not the final publication receipt.

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
