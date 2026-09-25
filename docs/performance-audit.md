# Runtime performance audit

The runtime audit is an opt-in real-browser harness. It is intentionally excluded from ordinary CI because wall-clock budgets are unreliable on shared runners. It records UTF-8 CSV and JSON rather than deciding pass/fail from elapsed time.

## Reproduce the runtime audit

From PowerShell:

```powershell
./scripts/run-runtime-performance-audit.ps1 `
  -Browser both `
  -Warmups 2 `
  -Repetitions 5 `
  -Operations 100
```

The script first compiles the reactor, then runs the measured browser process separately. It resolves paths from `$PSScriptRoot`, records the source SHA, and writes one directory per browser under `target/performance-audit/`. Dependencies and browser drivers should be prepared before interpreting the measured interval.

The script runs three deliberately separate workloads:

- `RuntimePerformanceAuditIT`: stage 1 micro-workload for prepared HUD entry updates;
- `RuntimeWorkloadPerformanceIT`: equivalent public click, clear/fill, assertion, controlled wait, Smart Click recovery, and failure-evidence workloads in STANDARD and DEBUG;
- `RuntimeTestNgLifecyclePerformanceIT`: the real TestNG adapter matrix for `PER_METHOD`/`PER_CLASS` and STANDARD/DEBUG.

The four stage 1 labels are not new product presets:

- `RAW_SELENIUM` is a lower bound: one direct JavaScript command per event and no Test Lens diagnostics.
- `LENS_LOW_DIAGNOSTICS` uses the normal Test Lens log path with the visual HUD disabled. Trace logging still exists outside this focused renderer measurement.
- `LENS_STANDARD` and `LENS_DEBUG` use the actual corresponding `HudPreset` and the same 100 user-authored semantic events.

This workload isolates HUD dispatch/render cost. It does **not** claim that a raw JavaScript assignment is functionally equivalent to Test Lens actions, Smart Click recovery, evidence, waits, or finalization. Those mechanisms need separate scenario measurements. Likewise, HUD DEBUG is unrelated to IntelliJ/JDWP debugging, browser DevTools, breakpoints, or slow motion.

## Stage 1 — HUD entry update cost

Environment: Windows 11 10.0 amd64, Microsoft OpenJDK 21.0.10, Maven 3.9.13, Selenium 4.39.0, headless Chrome 152.0.7977.83. Two warmups preceded five measured repetitions of 100 events. Times below are medians per event; ranges are min–max across repetitions.

| Profile | Before | After | Client calls per 100 events, before → after |
| --- | ---: | ---: | ---: |
| RAW_SELENIUM | 8.245 ms (7.496–8.783) | 8.382 ms (7.645–8.459) | 100 → 100 |
| LENS_LOW_DIAGNOSTICS | 4.100 ms (3.875–4.758) | 0.034 ms (0.023–0.058) | 201 → 1 |
| LENS_STANDARD | 46.486 ms (45.617–46.674) | 16.889 ms (15.342–18.154) | 706 → 306 |
| LENS_DEBUG | 45.100 ms (44.105–50.635) | 16.349 ms (15.238–17.390) | 706 → 306 |

For this controlled stream the STANDARD and DEBUG HUD-entry update path was reduced by about 64% in the original run and by 65–66% in the final interleaved confirmation. This percentage applies only to dispatching already prepared semantic HUD entries. It is not a reduction for a whole suite, all of Test Lens, or the DEBUG preset as a whole. Both profiles retain all 100 semantic rows. The measured difference between STANDARD and DEBUG in this micro-workload is expected to be small because it manually supplies the same USER entries; the real-action workload above measures how many technical events each preset actually produces.

The confirmed cause was not full-history serialization: the browser renderer already appends or operation-correlates one row and bounds the DOM at 250 rows. The cost came from five repeated JavaScript initialization calls plus one alert probe for every visible event. The runtime now uses one warm self-validating HUD dispatch and reinstalls after document navigation only. It deliberately retains the alert probe: attempting JavaScript while a modal prompt is open can invoke WebDriver's unhandled-prompt policy and dismiss the application's alert. With the panel disabled, the HUD sink now performs no browser probe.

The Firefox post-change contract used the same repetitions and retained every row: STANDARD median 12.999 ms/event and DEBUG 12.816 ms/event, with 306 decorated client calls per 100 events. A Firefox baseline from the old implementation was not captured in this worktree, so no Firefox before/after percentage is claimed.

## Interpretation and limits

Every extra WebDriver command is especially expensive on a remote/Grid topology. Stage 1's `totalClientCalls` is explicitly a count of decorated Java client method calls; it is not a count of HTTP requests or wire round trips. Stage 2 separately instruments `RemoteWebDriver.execute(CommandPayload)` and reports `executorCommands` and `executeScript`. The two metrics must not be substituted for each other. Browser BiDi/WebSocket events remain outside both counts.

`RAW_SELENIUM` and `LENS_LOW_DIAGNOSTICS` are diagnostic lower bounds, not functionally equivalent user-action benchmarks: they perform different work. Only STANDARD versus DEBUG rows from `workload-operations.csv` execute the same public Test Lens operations against the same reset fixture and action policies.

## Stage 2 output and interpretation

`workload-operations.csv` records consumer-visible duration, trace-event delta, browser subtree mutations, logical commands accepted by `RemoteWebDriver.execute(CommandPayload)`, `executeScript` commands, HUD batch/event/payload-byte counts, outcome, business click count, and cumulative trace JSON size for every public operation. `workload-commands.csv` attributes command count and inclusive command duration to the measured public-operation interval without retaining arguments. These executor counts are not a claim about physical HTTP attempts. `workload-lifecycle.csv` separates direct attach, start-session, finish/export/evidence, new-session, and quit observations. `testng-lifecycle.csv` records the real adapter's suite wall time, driver creation time, action time, residual adapter setup/finalization/quit time, and create/quit/session/report counts.

The same `RuntimeWorkloadPerformanceIT` also writes `native-observation-operations.csv` for an equivalent three-command native workload (`find+click`, `find+clear`, `find+sendKeys`) in `RAW`, `OBSERVED_STANDARD`, and `OBSERVED_DEBUG` profiles. It records consumer-visible action time, total time including Lens finalization, logical executor commands, `executeScript`, semantic events, HUD batches/events/bytes, and the business click count. RAW is a lower bound without diagnostics; observed rows retain native Selenium dispatch and do not use Smart Click. This opt-in comparison measures observation overhead without creating a second benchmark framework or treating decorated calls as physical HTTP retry counts.

### Native observation local sample

Source `48ada87+native-observation-working-tree`, reactor test classes, headless Chrome 152.0.7977.83 / ChromeDriver 152.0.7977.82 and Firefox 156.0.1 / geckodriver 0.37.1 on Windows. Two warmups preceded five interleaved measured repetitions. `Action median` covers the three equivalent native operations; `total` also includes attach/start (for observed profiles) and synchronous Lens finalization. Command counts cover the measured profile through finalization and the one business-counter read. They are logical calls accepted by Selenium's command executor, not guaranteed physical HTTP attempts.

| Browser | Profile | Action median (min-max) | Total median | Executor commands | `executeScript` | Lens events | HUD batches/events | Mean HUD bytes | Business clicks |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Chrome | RAW | 126.419 ms (117.641-137.784) | 133.212 ms | 7 | 1 | 0 | 0 / 0 | 0 | 1 |
| Chrome | OBSERVED_STANDARD | 408.844 ms (337.967-457.022) | 495.508 ms | 40 | 28 | 16 | 6 / 6 | 3,646 | 1 |
| Chrome | OBSERVED_DEBUG | 488.969 ms (468.159-585.712) | 571.033 ms | 52 | 34 | 16 | 12 / 15 | 8,333 | 1 |
| Firefox | RAW | 94.374 ms (87.793-108.467) | 100.185 ms | 7 | 1 | 0 | 0 / 0 | 0 | 1 |
| Firefox | OBSERVED_STANDARD | 347.071 ms (297.809-374.742) | 413.127 ms | 40 | 28 | 16 | 6 / 6 | 3,646 | 1 |
| Firefox | OBSERVED_DEBUG | 421.941 ms (408.747-499.329) | 497.608 ms | 52 | 34 | 16 | 12 / 15 | 8,334 | 1 |

The raw profile is not diagnostically equivalent; it is the native lower bound. Observation keeps exactly one business click in every repetition. DEBUG retains the same 16 trace events as STANDARD but presents additional technical locator/read diagnostics, explaining its extra six HUD batches and six JavaScript commands in this workload. The remaining observed overhead includes synchronous semantic HUD delivery and automatic ACTION/SUCCESS highlights; highlight presentation timers do not block the operation. No Smart Click, actionability wait, retry, or extra element lookup is part of these observed native actions.

The TestNG residual is intentionally labelled as a combined value. The public adapter does not expose internal attach/start/finish callbacks, and the audit does not add product API merely to manufacture precision. Direct lifecycle rows provide the separately measured attach/start/finish values. Likewise, HUD transport plus browser runtime remains one WebDriver command duration where Selenium does not expose a safe split.

The successful public workload and the controlled failure/evidence workload are separate rows. Smart Click recovery asserts one business click and keeps the covering element in place. Presentation timing is asynchronous and is not added to action latency.

### 2026-09-24/25 local Stage 2 sample

Source `c0f259087b2fe4f1f97f127b92b393bfdb432ede`, reactor test classes, headless Chrome 152.0.7977.83 / ChromeDriver 152.0.7977.82 and Firefox 156.0.1 / geckodriver 0.37.1 on Windows. One warmup preceded three interleaved measured repetitions. The six-operation median is the sum of click, clear, fill, assertion, controlled wait, and covered Smart Click for one repetition.

| Browser | Preset | Six-operation median | Executor commands | `executeScript` | HUD mutation adds / updates |
| --- | --- | ---: | ---: | ---: | ---: |
| Chrome | STANDARD | 3.932 s | 148 | 85 | 55 / 238 |
| Chrome | DEBUG | 4.352 s | 200 | 111 | 131 / 578 |
| Firefox | STANDARD | 2.995 s | 148 | 85 | 55 / 238 |
| Firefox | DEBUG | 3.607 s | 200 | 111 | 131 / 578 |

Thus DEBUG added 52 logical executor commands (+35.1%) and 26 JavaScript commands (+30.6%) for this exact workload. Its measured wall-time overhead versus STANDARD was 10.7% in Chrome and 20.4% in Firefox. Trace event counts for the successful sessions were effectively equal (49); the difference was presentation visibility: DEBUG sent technical events that STANDARD retained diagnostically but did not render. This identifies browser transport and HUD DOM work as the dominant measured difference, not larger exported traces. The executor count does not prove an equal number of physical HTTP attempts.

The real TestNG adapter used two invocations per cell. Chrome `PER_METHOD` created/quit 2/2 drivers and took 7.88 s STANDARD and 7.84 s DEBUG; `PER_CLASS` created/quit 1/1, produced two independent Lens sessions/reports, and took 4.92 s STANDARD and 5.65 s DEBUG. Firefox measured 14.13/10.11 s for `PER_METHOD` and 6.00/6.76 s for `PER_CLASS`; the three-sample action workload, rather than this single lifecycle observation, is the reliable STANDARD/DEBUG comparison. Driver reuse removes new-session/quit cost but does not remove per-invocation Lens reports.

Controlled failure finalization with screenshot/evidence took 533 ms STANDARD / 453 ms DEBUG in Chrome and 617 ms / 408 ms in Firefox in this sample. These single observations establish the order of magnitude, not a preset speed ranking. A 40-second JFR of the Chrome workload contained 29 execution samples, 2,857 allocation samples and 3,939 thread parks; it supports an I/O/wait-heavy workload but is still too sparse for a method-level CPU hotspot claim. A headed Chrome Performance trace, Grid and BrowserStack were not run and must not be inferred from local numbers.

Presentation duration remains asynchronous and is not included as action execution time. This change does not shorten highlight durations, discard DEBUG events, reduce the 250-row HUD bound, alter trace retention, or change finish/evidence behavior. A missing HUD runtime after navigation takes the cold reinstall path; later events return to one HUD dispatch plus the required non-mutating alert check.

## Stage 3 — operation-scoped semantic HUD transport

Stage 3 keeps the same semantic events and synchronous delivery contract while combining entries that are already ready at a safe operation boundary. A public `RUNNING` row is still dispatched before the operation. Technical diagnostics emitted synchronously inside that operation are retained in DEBUG and sent with the terminal result. `WARNING`, `RETRYING`, `FAILED`, and user-authored HUD messages force an immediate flush; a terminal result never waits for another test action. There is no background WebDriver thread or time-window scheduler.

The alert probe remains mandatory. One ready batch uses one alert probe and one self-validating JavaScript dispatch. An open alert/confirm/prompt defers only HUD presentation into a bounded 250-entry/256-KiB UTF-8 queue; trace/report retention is unchanged. Once a later safe HUD delivery observes that the prompt is gone, it drains one bounded snapshot. A missing runtime returns `false` before applying the batch and permits one cold reinjection. An exception or lost response is ambiguous and is not replayed blindly.

The browser renderer applies every event in order, preserves operation correlation, Unicode, custom icons, safe `textContent`, and the 250-row DOM limit, then performs layout normalization and auto-scroll once per batch rather than once per entry. The `hudMutationAddedNodes` and `hudMutationUpdates` columns count `MutationObserver` subtree mutations, not semantic rows or transport requests.

### Local before/after result

Environment: Windows, Microsoft OpenJDK 21.0.10, Maven 3.9.13, Selenium 4.39.0, headless Chrome 152.0.7977.83 / ChromeDriver 152.0.7977.82 and Firefox 156.0.1 / geckodriver 0.37.1. Both variants used two warmups and five interleaved measured repetitions of the same click, clear, fill, assertion, controlled wait, and covered Smart Click workload. Baseline is `ba8d973` plus measurement-only command timing; optimized is the working tree based on `ba8d973`. Raw artifacts are under `target/performance-audit/hud-batching-baseline` and `target/performance-audit/hud-batching-final`.

| Browser | Preset | Six operations, before → after | Executor commands | Alert probes | `executeScript` | HUD deliveries/batches | Events delivered | Optimized payload bytes | DOM added/update mutations |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Chrome | STANDARD | 3.017 s → 2.989 s (-0.9%) | 148 → 138 | 19 → 14 | 85 → 80 | 19 → 14 | 19 → 19 | 10,483 | 55 / 238 → 55 / 238 |
| Chrome | DEBUG | 3.604 s → 3.085 s (-14.4%) | 200 → 150 | 45 → 20 | 111 → 86 | 45 → 20 | 45 → 45 | 24,452 | 131 / 578 → 131 / 578 |
| Firefox | STANDARD | 1.548 s → 1.535 s (-0.8%) | 148 → 138 | 19 → 14 | 85 → 80 | 19 → 14 | 19 → 19 | 10,479 | 55 / 238 → 55 / 238 |
| Firefox | DEBUG | 1.867 s → 1.612 s (-13.7%) | 200 → 150 | 45 → 20 | 111 → 86 | 45 → 20 | 45 → 45 | 24,451 | 131 / 578 → 131 / 578 |

The baseline transport sent one visible event per dispatch, so baseline deliveries equal alert probes. The enhanced payload-byte counter was added after that preserved baseline; baseline byte values are therefore unavailable and are not reconstructed from trace JSON. `executorCommands` counts calls accepted by `RemoteWebDriver.execute(CommandPayload)`, not decorated Java methods, physical HTTP retries, or BiDi frames. Command elapsed time includes transport plus browser execution because Selenium does not expose a reliable split.

The command reduction is deterministic: STANDARD removes 10 executor commands (6.8%) and DEBUG removes 50 (25.0%) per six-operation run. STANDARD wall-time differences are smaller than normal run-to-run spread and are therefore inconclusive; DEBUG recovered 14.4% on Chrome and 13.7% on Firefox in this local workload. Native click interception, browser work, and application delay remain unchanged. DEBUG retains all 45 visible events and the same DOM mutation totals; it now uses 20 batches rather than 45 individual deliveries. DOM state is applied synchronously before the WebDriver script returns, but browser paint latency was not measured and no paint-time claim is made.

The separate 300-entry USER-message burst intentionally remains synchronous rather than waiting for a batch window: both presets retained the newest 250 rows and reported 50 UI evictions. Median per-entry time was 14.628 ms STANDARD / 15.347 ms DEBUG in Chrome and 11.752 ms / 12.115 ms in Firefox (two warmups, five measured repetitions). Each run made 906 decorated client calls because a public USER message is an immediate flush; these are not protocol-command counts. The raw burst files are under `selenium-test-lens-browser-tests/target/performance-audit/hud-batching-final-burst300`.

No BrowserStack credentials or Grid endpoint were available for this audit. BrowserStack and WAN performance are therefore **NOT RUN**, not inferred from localhost. To measure a remote consumer, use the same source SHA/artifact, expose a reachable fixture, retain the same operation count, and record provider new-session/quit separately from Test Lens event dispatch.

JFR/browser traces are useful for CPU and allocation attribution but change the measured system. A separate 20-event, zero-warmup JFR using the JDK `profile` settings produced a four-second recording. It contained only four execution samples—too few for a CPU attribution claim—and showed the test spending its sampled time in framework startup/waiting/HTTP I/O rather than a demonstrated Java CPU hotspot. This result is supporting context, not the source of the reported speedup; the unprofiled repeated wall-time and command-count runs are the primary evidence. The installed Chrome 152 has no matching Selenium 4.39 CDP module in this repository, so a Chrome DevTools performance trace was **NOT RUN**; Selenium was not upgraded for the audit.
