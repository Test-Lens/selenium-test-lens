# Test Lens 0.5.1 regression audit and prepared 0.5.2

Status: prepared, unpublished. This report separates reproduced product defects from unverified concerns and environment limitations. It supplements the [BiDi audit](selenium-bidi-audit.md) and [BiDi release validation](selenium-bidi-release-0.5.2.md).

This is the completed local audit milestone. Subsequent production validation holds publication for a Chrome 158 BiDi redirect-integrity failure reproduced without Lens or Grid on hosted Linux. The current [production release evidence](production-release-0.5.2.md) supersedes this milestone's readiness verdict without changing its historical test results.

## A. Baseline and isolation

- Actual release tag: `v0.5.1`, commit `e6e2a19f4873b9b1274dfdb17498ca8cd6e42cc3`.
- Original worktree: `D:/Java Projects/JsTestTools`, branch `feat/studio-testng-playground`, HEAD `94189e167a123c5aacfae4d3631634a98c016717`.
- That HEAD differs from the tag in 23 documentation files and no production Java files. Tests for the affected source paths therefore exercise the actual 0.5.1 implementation before applying fixes.
- Actual Maven Central 0.5.1 JARs were downloaded for all 15 published JAR modules. Separate public-API probes reproduced cleanup interruption/error loss, zero measured trace duration, executable report links and terminal HUD overwrite in the published binaries/resources. Other regressions were reproduced against tag-identical sources; they are not claimed as separate published-JAR executions.
- Release work is isolated in `private/bidi-release-0.5.2`, branch `release/bidi-0.5.2`. Commits are replayed onto `94189e1` in `private/release-052-clean`, without copying dirty source directories or compiled classes.
- The original HEAD, porcelain status, staged binary diff and SHA-256 of all 256 initially modified/untracked files are checked against `target/bidi-hardening-original/snapshot.json`. No reset, clean, automatic stash or broad staging was used in the original worktree.

## B. Confirmed defects

Paths below are relative to the corresponding module's `src/main`; regression test class names are unique in the repository. Synthetic critical errors do not exhaust JVM memory.

| ID / priority | Code and failure mechanism | Reproducer and user impact | Minimal fix and regression |
| --- | --- | --- | --- |
| BIDI-01 / P0 | Selenium network adapter compiled against 4.39 size descriptors and numeric listener IDs. Selenium 4.41 changes request `()Ljava/lang/Long;` to `()J`, response headers/body `()J` to `()Ljava/lang/Long;`; 4.46 changes listener IDs from numeric to string. | Legacy bytecode reproducer fails at the changed boundary; a listener could stop recording while assertions still trusted capture. | Cached public-API compatibility boundary, nullable response sizes, owned subscriptions, explicit failure invalidation and same-JAR tests. Full details and signatures: [BiDi audit](selenium-bidi-audit.md). |
| ACT-01 / P0 | `selenium/actions/SmartClickActions.java:386`, `SmartInputActions.java:96`, `selenium/locator/UiLocator.java:997`: message/cause-chain classification treated an ambiguous outer transport failure as proof an action had been rejected. | Generic errors wrapping interception/stale signals caused two or three native dispatches. Repeating submission/input can change application data. Browser fixture executes the real side effect before throwing the ambiguous error. | Retry dispatched actions only on directly classified rejection exceptions; retain read-only recovery. `SmartClickFallbackCascadeTest`, `RuntimeInputRegressionTest`, `UiLocatorAmbiguousActionRegressionTest`, `RuntimeOverlayBrowserAuditIT` assert exact dispatch counts. |
| ACT-02 / P1 | `selenium/locator/UiLocator.java:624,634`: checkbox confirmation could fail stale after a successful click and restart the entire action. | Baseline regression observed two clicks, potentially reverting the desired checkbox state. | Record completed click and fail confirmation without redispatch. `UiLocatorAmbiguousActionRegressionTest`. |
| CANCEL-01 / P1 | `selenium/OverlayWait.java:124`, action/helper entry points, `react/ReactSafeExecutor.java:65,123`: cancellation did not prevent another condition/lookup/action; retry sleep restored interruption and continued. | Pre-interrupted fixtures executed Selenium; cancellation during React recovery allowed another attempt. | Preserve flag and propagate interruption before another operation; propagate interrupted retry delay. `OverlayWaitCancellationRegressionTest`, `RuntimeInputRegressionTest`, `SmartClickFallbackCascadeTest`, `ReactSafeExecutorCancellationAuditTest` (three cancellation boundaries). |
| OVERLAY-01 / P2 | `selenium/core/BlockingOverlayHelper.java:64,104,115`, `PopupDetector.java:259`: three unconditional 500 ms sleeps after dismissal. | Even immediately removed controls incurred the fixed delay; no actual disappearance was observed. | Conditional visibility/staleness polling within the existing 500 ms cap. No new retry or timeout API. Boolean still means dismissal was invoked, not guaranteed disappearance. `LegacyOverlayRecoveryRegressionTest` and immediate/delayed real-DOM browser cases. |
| INPUT-01 / P2 | `selenium/actions/TypingActions.java:60`: OFF mode passed null to `sendKeys`, unlike ON mode. | Baseline null-input regression errored; behavior depended on overlay mode. | Clear field and skip null input consistently. `RuntimeInputRegressionTest`. |
| LIFE-01 / P1 | `selenium/TestLens.java:353`: unexpected observer/evidence error before normal cleanup cached the error without mandatory resource release. | Injected `LinkageError` left cleanup count zero and session STARTED. | Emergency finalization attempts cleanup/network stop once, terminalizes an active session FAILED, retains primary/suppressed errors and replays the same error. `LifecycleFailureAuditTest`. |
| LIFE-02 / P1 | `selenium/TestLens.java:492,535`: cleanup `Error` was downgraded to an ordinary wrapper and swallowed when a primary failure already existed. | Published 0.5.1 public probe confirmed critical cleanup failure could return normally from `finishFailed`. | Retain primary failure and rethrow critical cleanup error after attempting all callbacks; repeated finalization replays it without repeating cleanup. `LifecycleFailureAuditTest` covers AssertionError, LinkageError and synthetic OOM for pass/fail finalization. |
| LIFE-03 / P2 | `selenium/ScenarioResourceManager.java:78-88`: catching cleanup `InterruptedException` lost cancellation. | Published JAR probe showed interruption flag false after cleanup. | Attempt remaining cleanup then restore flag; preserve failure. `LifecycleFailureAuditTest`. |
| META-01 / P1 | Both `selenium/LocatorObservationMetadata.java` and `selenium/locator/LocatorObservationMetadata.java` caught all `Throwable` from custom locator display/remote parameters. | Source regressions showed VM/linkage/ThreadDeath errors silently converted to opaque metadata, allowing continued work. | Rethrow `VirtualMachineError`, `ThreadDeath`, `LinkageError`; retain optional diagnostic AssertionError/runtime fallback. `LocatorObservationFatalAuditTest`, `LocatorObservationMetadataFatalRegressionTest` exercise both paths, including public `UiLocatorDescription.of`. |
| TRACE-01 / P2 | `core/trace/TraceLogSink.java:29,149`: measured operation metadata was not transferred into top-level event duration. | Published JAR probe measured waits of 65/1101 ms but exported 0/0. | Prefer existing monotonic `durationNanos`, then `durationMs`, on terminal measured operations. No invented minimum. `MeasuredOperationDurationAuditTest` and published/patched probe compare JSON and both HTML exporters. |
| REPORT-01 / P1 | `core/trace/export/TraceHtmlExporter.java:514` and `core/logging/export/HtmlLogExporter.java`: escaping text did not reject executable artifact URL schemes. | Published JAR generated active `javascript:` href; failure archive path was another affected sink. No malicious payload was executed. | Validate artifact-link scheme, keep rejected values escaped as text. `ReportLinkSecurityAuditTest` covers both exporters/archive and safe links. |
| HUD-01 / P2 | `overlay/resources/uitestlens/runtime/hud-panel.js`: late RUNNING update could overwrite PASSED for the same operation/category. | Node harness against the actual published resource reproduced terminal status loss. | Preserve terminal state against nonterminal updates; genuine later FAILED still accepted. `HudPanelJsTest`. |

### Test-first evidence

Before-fix behavioral failures are retained under the isolated worktree's `target`: `runtime-smartclick-before.log`, `lifecycle-before.log`, `lifecycle-cleanup-error-before.log`, `react-cancellation-before.log`, `locator-react-cancellation-before.log`, `locator-package-fatal-before.log`, `trace-hud-baseline.log`, `trace-hud-second-baseline.log`, `trace-hud-runtime-baseline.log`, and the UiLocator regression baseline recorded in `runtime-actions-audit.md`. Compilation/command-authoring failures are not counted as product reproducers.

Published-binary evidence is in `lifecycle-published-before.log`, `trace-hud-published-051.log`, `trace-hud-published-runtime-051.log`; `api-release-audit.published.json` records actual published public declarations. Focused regressions then pass, including adapter lifecycle, HUD/highlights and real browser dispatch-count tests. The final aggregate validation below supersedes intermediate milestone counts.

## C. Reviewed without a confirmed new defect

- The suspicious private `OverlayWait.sleepQuiet` loop is unused by the audited public path. The public cancellation defect is fixed; an unused method is not reported as an active spin-loop regression.
- Read/assertion recovery, disabled/actionability checks, React rerenders, JavaScript fallback ordering and highlight phase guards retain their existing contracts. No blanket retry, new fallback or broad refactor was added. Ambiguous navigation/transport completion is deliberately not considered safe evidence to repeat an action.
- Existing JUnit/TestNG suites cover parallel invocation ownership, retry attempt identity, skip/setup/teardown failure, null driver, repeated finalization and failure precedence. Adapter production code remains unchanged. These fixtures do not certify every external runner timeout or early driver shutdown scenario.
- Already terminal core snapshots remain immutable. If a caller explicitly finishes the core session before facade cleanup, or driver quit fails afterwards, the runner can fail while the previously terminal trace retains its former status. Existing tests encode this contract; redesign is outside this patch.
- Central `RedactionPolicy.sensitiveKey` masks a custom `X-Company-Secret` in JSON, HTML and failure ZIP; ordinary Content-Type remains visible. Network request/response headers use that same policy. No custom-header bypass was confirmed.
- Unicode, markup, missing screenshots, bounded large events and ZIP containment have regression coverage (`ReportDataBoundaryAuditTest`). No path traversal was reproduced; arbitrary large-report stress beyond the configured event limit remains unverified.
- Published consumer dependency tree with an explicit Selenium BOM selects a coherent 4.39.0 graph. It does not prove consumer convergence without the BOM. Prepared parent imports the BOM; explicit versions can still override it.
- TestNG 7.9.0 requests SLF4J 1.7.36 while Allure 2.35.5 requests 2.0.17; the fixture selects 1.7.36. This is a real version mediation, but no binary/runtime failure was reproduced, so no speculative dependency upgrade is included. JUnit/Platform versions are consistent in that fixture. Jackson is absent from it; optional tooling has a separate Jackson dependency and is not claimed as audited by this fixture.
- A separate final clean-reactor `mvn -B dependency:tree -Dverbose` also passes. Batched `javap -c -p` over the six resolved Allure JARs found only SLF4J calls available in 1.7 (no fluent 2.0 API calls). This supports retaining the baseline; it does not certify arbitrary logging providers supplied by consumers.
- BrowserStack, Firefox version matrix, actual JVM exhaustion and hosted Linux CI remain outside local certification. No secrets or paid resources were used.

## D. Final verification

All final source checks ran from the clean replay at `c1600e64` (the same source tree as release branch `80e50f1c`). The final documentation commit is replayed afterwards and checked without changing compiled production sources.

| Area | Result | Evidence |
| --- | --- | --- |
| Clean reactor, Java 17 | PASS: 1845 tests; 1806 passed, 0 failures/errors, 39 skipped | `target/final-clean-reactor.log`, `final-clean-reactor-summary.json`; 5 min 38 s |
| Same-JAR Selenium matrix, Java 21 | PASS: 1242 tests, 0 failures/errors/skips | Clean checkout `target/selenium-bidi-compatibility/86fb233cde264c2680985ad6036c8c41/results.json`; summary `target/release-052-evidence.json` in release worktree |
| Local Chrome 157 | PASS on all nine Selenium rows; headless runtime smoke 5/5 and headed smoke 5/5 | Matrix results plus `final-browser-headless.log`, `final-browser-headed.log`; Failsafe selected only `RuntimeOverlayBrowserAuditIT`, unit suites already covered by reactor |
| Grid 4.53.0 / RemoteWebDriver | PASS on all nine Selenium rows | Real HTTP response, lazy initialization, restart and owned-listener lifecycle; matrix results |
| Network report correctness | PASS on all nine versions, local and Grid | Server event/body-byte journal compared to model/JSON/HTML/failure ZIP; 252 preserved report artifacts |
| Public API | PASS | 791 types / 5700 callables; all 15 actual 0.5.1 JAR surfaces identical. Official manifest PASS; stable/advanced projection 267 types / 2089 callables, delta zero |
| Release packaging | PASS | 16 published coordinates, 15 binary/source/Javadoc sets (45 JARs); `final-release-packaging-validation.log`; no upload/signing |
| MkDocs strict | PASS | Development, prepared-release and archived 0.1.0 builds; links PASS; 436 signatures in 104 blocks |
| Workflow validation | PASS locally | actionlint 1.7.12 on CI/docs/release-check workflows; final patch-policy gate and documentation checks |
| Hosted CI | NOT RUN | Configured GitHub authentication is invalid and the branch is local; no push is authorized. No hosted run ID exists |
| BrowserStack | NOT RUN | No safe provider credentials/configuration available; no paid session created |

The 39 reactor skips are existing documentation-only examples, opt-in diagnostic/performance gates and unavailable Windows assumptions (including symbolic links and environment-specific uploader fixtures). They are not counted as PASS. There are **zero** skips in the final binary/browser matrix and in either selected runtime browser smoke. Historical pre-fix failures are intentional reproductions, not hidden final-suite failures. An initial clean command could not delete its own redirected root `target` log on Windows; moving that log outside the cleaned tree allowed the complete successful run above.

| Selenium | Binary | Chrome | Grid | Report data (Chrome / Grid) | Tests / failures / skipped |
| --- | --- | --- | --- | --- | --- |
| 4.39.0 | PASS | PASS | PASS | PASS / PASS | 138 / 0 / 0 |
| 4.40.0 | PASS | PASS | PASS | PASS / PASS | 138 / 0 / 0 |
| 4.41.0 | PASS | PASS | PASS | PASS / PASS | 138 / 0 / 0 |
| 4.45.0 | PASS | PASS | PASS | PASS / PASS | 138 / 0 / 0 |
| 4.46.0 | PASS | PASS | PASS | PASS / PASS | 138 / 0 / 0 |
| 4.47.0 | PASS | PASS | PASS | PASS / PASS | 138 / 0 / 0 |
| 4.51.0 | PASS | PASS | PASS | PASS / PASS | 138 / 0 / 0 |
| 4.52.0 | PASS | PASS | PASS | PASS / PASS | 138 / 0 / 0 |
| 4.53.0 | PASS | PASS | PASS | PASS / PASS | 138 / 0 / 0 |

Final frozen and staged main JAR SHA-256: `EF4240F340359F5383F6B6A4EED374A52A81AA8A3BC974CA733D0A8E03C26AF6`. Core: `6A3D2D4B085B289E639C5D767D4DAEA50A09BE915BC57A9B097C8A73F9414220`; overlay: `1ED81803C687F13B7E75F7C075754D57EC9E275A3EB907EF567AB0495BE4BE6B`. The old BiDi-only `CB3280...` artifact is a superseded milestone, not the final combined candidate.

Reproduction uses the repository's official commands:

```powershell
mvn -B clean test
./scripts/check-public-api.ps1
./scripts/check-public-api-since.ps1
./scripts/test-selenium-bidi-compatibility.ps1 -BrowserTests -GridUrl http://127.0.0.1:4444
mvn -B -Prelease-artifacts -DskipTests package
./scripts/validate-release-packaging.ps1 -Version 0.5.2
./scripts/check-patch-release.ps1 -ExpectedVersion 0.5.2
python -m mkdocs build --strict
git diff --check
```

The binary matrix verifies one frozen main/core/overlay JAR set, actual runtime Selenium version and SHA-256 in every row. Its report tests compare controlled HTTP server journals and transmitted bytes with the internal model, network JSON, trace JSON, rendered HTML and failure ZIP. They exercise 200, POST JSON, redirects, errors, repeated URLs, bursts, empty/unavailable sizes and redaction; file existence alone is insufficient.

## E. Git and release scope

BiDi commits: `e10cb2b1`, `3a2df0df`, `a4d24058`, `35690ba1`, `5dfb1e1b`. Separate runtime commits: `c0496789` (reports/HUD), `36c61081` (lifecycle), `00bf23f1` (React cancellation), `9a034be2` (actions/overlays), `80e50f1c` (both metadata encoders). Each can be reviewed/cherry-picked independently in order. Final documentation commit is discoverable via `git log -- docs/maintainers/selenium-051-regression-audit.md`.

Changed production modules are core, overlay, Selenium and React. Tests add browser/runner/export coverage; build/CI/docs changes prepare only the patch lane. No new Studio/Playground functionality or original dirty hunks were copied. The public API catalog must remain identical to the actual 0.5.1 binary surface; no blanket catalog regeneration is accepted.

Maven Central and GitHub release/tag checks found 0.5.2 unused during preparation. This is a time-bound availability check, not a reservation; recheck before publishing. No push, release tag, GitHub Release or Maven publication was performed.

## F. Release readiness

**READY WITH DOCUMENTED LIMITATIONS**.

Confirmed P0/P1 defects and small P2 fixes are isolated, regression-tested and retain the published API. No local functional blocker remains. The patch keeps Java 17 / Selenium 4.39.0 baselines and certifies the listed BiDi scenarios only on the nine Selenium versions through 4.53.0. Existing BiDi limits remain: the baseline health command may add up to 30 seconds, pre-dispatch malformed JSON has no common public passive failure hook, and 4.39–4.45 can retain server event interest until the shared connection owner closes it.

Hosted Linux CI and BrowserStack remain NOT RUN. After repairing authentication and separately authorizing making the branch available remotely, dispatch `gh workflow run ci.yml --repo Test-Lens/selenium-test-lens --ref release/bidi-0.5.2` and record the run ID. BrowserStack requires a safe test account and an actual captured-response/lifecycle run. These are explicit certification limits, not local PASS results. Firefox matrix, external runner timeout/early-quit combinations and actual memory exhaustion remain unverified; follow-up work must not broaden the patch silently. Release authorization, version-availability recheck and publication remain separate actions.
