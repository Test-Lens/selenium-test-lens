# Runtime performance audit

The runtime audit is an opt-in real-browser harness. It is intentionally excluded from ordinary CI because wall-clock budgets are unreliable on shared runners. It records UTF-8 CSV and JSON rather than deciding pass/fail from elapsed time.

## Reproduce the focused HUD measurement

From PowerShell:

```powershell
./scripts/run-runtime-performance-audit.ps1 `
  -Browser both `
  -Warmups 2 `
  -Repetitions 5 `
  -Operations 100
```

The script first compiles the reactor, then runs the measured browser process separately. It resolves paths from `$PSScriptRoot`, records the source SHA, and writes one directory per browser under `target/performance-audit/`. Dependencies and browser drivers should be prepared before interpreting the measured interval.

The four harness labels are not new product presets:

- `RAW_SELENIUM` is a lower bound: one direct JavaScript command per event and no Test Lens diagnostics.
- `LENS_LOW_DIAGNOSTICS` uses the normal Test Lens log path with the visual HUD disabled. Trace logging still exists outside this focused renderer measurement.
- `LENS_STANDARD` and `LENS_DEBUG` use the actual corresponding `HudPreset` and the same 100 user-authored semantic events.

This workload isolates HUD dispatch/render cost. It does **not** claim that a raw JavaScript assignment is functionally equivalent to Test Lens actions, Smart Click recovery, evidence, waits, or finalization. Those mechanisms need separate scenario measurements. Likewise, HUD DEBUG is unrelated to IntelliJ/JDWP debugging, browser DevTools, breakpoints, or slow motion.

## Audited 2026-09-24 result

Environment: Windows 11 10.0 amd64, Microsoft OpenJDK 21.0.10, Maven 3.9.13, Selenium 4.39.0, headless Chrome 152.0.7977.83. Two warmups preceded five measured repetitions of 100 events. Times below are medians per event; ranges are min–max across repetitions.

| Profile | Before | After | Client calls per 100 events, before → after |
| --- | ---: | ---: | ---: |
| RAW_SELENIUM | 8.245 ms (7.496–8.783) | 8.382 ms (7.645–8.459) | 100 → 100 |
| LENS_LOW_DIAGNOSTICS | 4.100 ms (3.875–4.758) | 0.034 ms (0.023–0.058) | 201 → 1 |
| LENS_STANDARD | 46.486 ms (45.617–46.674) | 16.889 ms (15.342–18.154) | 706 → 306 |
| LENS_DEBUG | 45.100 ms (44.105–50.635) | 16.349 ms (15.238–17.390) | 706 → 306 |

For this controlled stream the STANDARD reduction is 63.7% and DEBUG reduction is 63.7%. Both retain all 100 semantic rows. The measured difference between STANDARD and DEBUG is within run-to-run variation because user-authored USER events are visible in both presets; DEBUG overhead in a real suite mainly depends on how many additional technical events that suite emits.

The confirmed cause was not full-history serialization: the browser renderer already appends or operation-correlates one row and bounds the DOM at 250 rows. The cost came from five repeated JavaScript initialization calls plus one alert probe for every visible event. The runtime now uses one warm self-validating HUD dispatch and reinstalls after document navigation only. It deliberately retains the alert probe: attempting JavaScript while a modal prompt is open can invoke WebDriver's unhandled-prompt policy and dismiss the application's alert. With the panel disabled, the HUD sink now performs no browser probe.

The Firefox post-change contract used the same repetitions and retained every row: STANDARD median 12.999 ms/event and DEBUG 12.816 ms/event, with 306 decorated client calls per 100 events. A Firefox baseline from the old implementation was not captured in this worktree, so no Firefox before/after percentage is claimed.

## Interpretation and limits

Every extra WebDriver command is especially expensive on a remote/Grid topology because it adds a transport round trip. The harness counts decorated client calls; it does not claim they are raw HTTP retry counts. Browser BiDi/WebSocket events are outside this count.

Presentation duration remains asynchronous and is not included as action execution time. This change does not shorten highlight durations, discard DEBUG events, reduce the 250-row HUD bound, alter trace retention, or change finish/evidence behavior. A missing HUD runtime after navigation takes the cold reinstall path; later events return to one HUD dispatch plus the required non-mutating alert check.

No BrowserStack credentials or Grid endpoint were available for this audit. BrowserStack and WAN performance are therefore **NOT RUN**, not inferred from localhost. To measure a remote consumer, use the same source SHA/artifact, expose a reachable fixture, retain the same operation count, and record provider new-session/quit separately from Test Lens event dispatch.

JFR/browser traces are useful for CPU and allocation attribution but change the measured system. A separate 20-event, zero-warmup JFR using the JDK `profile` settings produced a four-second recording. It contained only four execution samples—too few for a CPU attribution claim—and showed the test spending its sampled time in framework startup/waiting/HTTP I/O rather than a demonstrated Java CPU hotspot. This result is supporting context, not the source of the reported speedup; the unprofiled repeated wall-time and command-count runs are the primary evidence. The installed Chrome 152 has no matching Selenium 4.39 CDP module in this repository, so a Chrome DevTools performance trace was **NOT RUN**; Selenium was not upgraded for the audit.
