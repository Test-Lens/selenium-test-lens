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

Every extra WebDriver command is especially expensive on a remote/Grid topology. Stage 1's `totalClientCalls` is explicitly a count of decorated Java client method calls; it is not a count of HTTP requests or wire round trips. Stage 2 separately instruments `RemoteWebDriver.execute(CommandPayload)` and reports `wireCommands` and `wireExecuteScript`. The two metrics must not be substituted for each other. Browser BiDi/WebSocket events remain outside both counts.

`RAW_SELENIUM` and `LENS_LOW_DIAGNOSTICS` are diagnostic lower bounds, not functionally equivalent user-action benchmarks: they perform different work. Only STANDARD versus DEBUG rows from `workload-operations.csv` execute the same public Test Lens operations against the same reset fixture and action policies.

## Stage 2 output and interpretation

`workload-operations.csv` records consumer-visible duration, trace-event delta, browser DOM row inserts/updates, actual WebDriver protocol commands, `executeScript` commands, outcome, business click count, and cumulative trace JSON size for every public operation. `workload-lifecycle.csv` separates direct attach, start-session, finish/export/evidence, new-session, and quit observations. `testng-lifecycle.csv` records the real adapter's suite wall time, driver creation time, action time, residual adapter setup/finalization/quit time, and create/quit/session/report counts.

The TestNG residual is intentionally labelled as a combined value. The public adapter does not expose internal attach/start/finish callbacks, and the audit does not add product API merely to manufacture precision. Direct lifecycle rows provide the separately measured attach/start/finish values. Likewise, HUD transport plus browser runtime remains one WebDriver command duration where Selenium does not expose a safe split.

The successful public workload and the controlled failure/evidence workload are separate rows. Smart Click recovery asserts one business click and keeps the covering element in place. Presentation timing is asynchronous and is not added to action latency.

### 2026-09-24/25 local Stage 2 sample

Source `c0f259087b2fe4f1f97f127b92b393bfdb432ede`, reactor test classes, headless Chrome 152.0.7977.83 / ChromeDriver 152.0.7977.82 and Firefox 156.0.1 / geckodriver 0.37.1 on Windows. One warmup preceded three interleaved measured repetitions. The six-operation median is the sum of click, clear, fill, assertion, controlled wait, and covered Smart Click for one repetition.

| Browser | Preset | Six-operation median | Wire commands | `executeScript` | HUD inserts / updates |
| --- | --- | ---: | ---: | ---: | ---: |
| Chrome | STANDARD | 3.932 s | 148 | 85 | 55 / 238 |
| Chrome | DEBUG | 4.352 s | 200 | 111 | 131 / 578 |
| Firefox | STANDARD | 2.995 s | 148 | 85 | 55 / 238 |
| Firefox | DEBUG | 3.607 s | 200 | 111 | 131 / 578 |

Thus DEBUG added 52 protocol commands (+35.1%) and 26 JavaScript commands (+30.6%) for this exact workload. Its measured wall-time overhead versus STANDARD was 10.7% in Chrome and 20.4% in Firefox. Trace event counts for the successful sessions were effectively equal (49); the difference was presentation visibility: DEBUG sent technical events that STANDARD retained diagnostically but did not render. This identifies browser transport and HUD DOM work as the dominant measured difference, not larger exported traces.

The real TestNG adapter used two invocations per cell. Chrome `PER_METHOD` created/quit 2/2 drivers and took 7.88 s STANDARD and 7.84 s DEBUG; `PER_CLASS` created/quit 1/1, produced two independent Lens sessions/reports, and took 4.92 s STANDARD and 5.65 s DEBUG. Firefox measured 14.13/10.11 s for `PER_METHOD` and 6.00/6.76 s for `PER_CLASS`; the three-sample action workload, rather than this single lifecycle observation, is the reliable STANDARD/DEBUG comparison. Driver reuse removes new-session/quit cost but does not remove per-invocation Lens reports.

Controlled failure finalization with screenshot/evidence took 533 ms STANDARD / 453 ms DEBUG in Chrome and 617 ms / 408 ms in Firefox in this sample. These single observations establish the order of magnitude, not a preset speed ranking. A 40-second JFR of the Chrome workload contained 29 execution samples, 2,857 allocation samples and 3,939 thread parks; it supports an I/O/wait-heavy workload but is still too sparse for a method-level CPU hotspot claim. A headed Chrome Performance trace, Grid and BrowserStack were not run and must not be inferred from local numbers.

Presentation duration remains asynchronous and is not included as action execution time. This change does not shorten highlight durations, discard DEBUG events, reduce the 250-row HUD bound, alter trace retention, or change finish/evidence behavior. A missing HUD runtime after navigation takes the cold reinstall path; later events return to one HUD dispatch plus the required non-mutating alert check.

No BrowserStack credentials or Grid endpoint were available for this audit. BrowserStack and WAN performance are therefore **NOT RUN**, not inferred from localhost. To measure a remote consumer, use the same source SHA/artifact, expose a reachable fixture, retain the same operation count, and record provider new-session/quit separately from Test Lens event dispatch.

JFR/browser traces are useful for CPU and allocation attribution but change the measured system. A separate 20-event, zero-warmup JFR using the JDK `profile` settings produced a four-second recording. It contained only four execution samples—too few for a CPU attribution claim—and showed the test spending its sampled time in framework startup/waiting/HTTP I/O rather than a demonstrated Java CPU hotspot. This result is supporting context, not the source of the reported speedup; the unprofiled repeated wall-time and command-count runs are the primary evidence. The installed Chrome 152 has no matching Selenium 4.39 CDP module in this repository, so a Chrome DevTools performance trace was **NOT RUN**; Selenium was not upgraded for the audit.
