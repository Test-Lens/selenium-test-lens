# Recording Test Lens 0.4.0 documentation media

The public clips are reproducible outputs of the versioned `docs/demo/hud/` fixture. That fixture loads the canonical runtime HUD/highlight/scroll assets copied by `docs-hooks/copy-hud-demo-runtime.py`; its checkout events are intentionally synthetic, local, and network-free. The capture workflow is documentation evidence, not a substitute for the real-browser runtime gates named below.

## Reproduce the media

Start the documentation site:

```powershell
python -m mkdocs serve -a 127.0.0.1:8765
```

In another PowerShell, compile the browser fixture if needed and run the opt-in capture test:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -Pbrowser-it -pl selenium-test-lens-browser-tests -am -DskipTests install
mvn -Pbrowser-it -pl selenium-test-lens-browser-tests '-Dbrowser=firefox' '-Dheaded=true' '-DtestLens.captureDocsMedia=true' '-Dit.test=DocumentationMediaCaptureIT' verify
python scripts\build-doc-media.py
```

`DocumentationMediaCaptureIT` is disabled unless `testLens.captureDocsMedia=true`. PNG frames stay under the module `target/`; `build-doc-media.py` writes only the five named public WebP files and target-only contact sheets.

## Shot list

All clips use Firefox, a headed browser, a requested 1024×640 window, the local MkDocs URL, and no credentials or external site. The captured WebDriver viewport is 1262×683 on the certified Windows display scale. DEFAULT means normal observability; DEBUG is selected only where strategy detail matters.

| Target asset | Purpose and exact demo | Mode | Expected HUD and highlight | Target duration | Public use |
| --- | --- | --- | --- | --- | --- |
| `hud-action-assertion-lifecycle.webp` | `DocumentationMediaCaptureIT`, `?scenario=lifecycle`; represents `RealBrowserContractsIT#hudUsesSemanticOperationRowsAndPresetFiltering` | DEFAULT / COMPACT | WAITING, ACTION/RUNNING→PASSED, ASSERTION/RUNNING→PASSED, SYSTEM/PASSED; yellow ACTION, blue WAITING, green SUCCESS | 10.8 s | README, What's New, HUD guide |
| `hud-highlight-lifecycle.webp` | capture IT, `?scenario=highlights`; represents `RealBrowserContractsIT#automaticHighlightStatesReceiveFullDurationsAndIgnoreStaleTransitions` | DEFAULT / COMPACT | HIGHLIGHT diagnostics while labels show ACTION, WAITING, RETRY, SUCCESS, FAILURE; terminal states remain visible for their own interval | 7.6 s | HUD guide |
| `hud-default-vs-fast.webp` | capture IT, `?scenario=observability`; runtime contract is also covered by `NativeSeleniumObservationIT#fastObservationKeepsBusinessActionWithoutCreatingPresentation` | DEFAULT followed by FAST | DEFAULT shows the semantic action and highlight; FAST interval has no live HUD/highlight; final frame explains retained structured summary | 7.2 s | configuration |
| `native-selenium-observation.webp` | capture IT, `?scenario=native`; runtime contract is covered by `NativeSeleniumObservationIT#nativeSeleniumRemainsExactlyOnceAcrossDriverElementAndDerivedContexts` | DEFAULT / COMPACT | observed `sendKeys` and click update one ACTION row each; input is masked; click ends SUCCESS/executed once | 6.4 s | Getting Started |
| `smart-click-fallback.webp` | capture IT, `?scenario=smart-click`; represents `RealBrowserContractsIT#smartClickFallsBackToJavascriptWithoutRemovingARealCoveringOverlay` | DEFAULT with DEBUG details | one ACTION row; NATIVE, ACTIONS, POINT, JS diagnostic details; one terminal SUCCESS; overlay stays until JS dispatch | 7.6 s | element actions |

Do not show IDE or terminal chrome, browser history, profiles, real accounts, production sites, E.ON/internal pages, credentials, or private data. Keep the fixture's light page theme and dark HUD, preserve the full panel and target, and regenerate all five clips after a semantic renderer or demo timing change.

## Visual acceptance

Inspect the generated contact sheets in `target/docs-media-contact-sheets/` and representative source frames before committing. Confirm that the HUD and labels are not clipped, text remains readable at docs width, each expected phase is visible, terminal success/failure receives a complete interval, FAST actually removes live presentation, the native input is masked, the Smart Click cover is not removed before JS dispatch, and no local path, credential, IDE, or terminal appears.

PER_CLASS remains text-only: a browser clip adds little beyond the lifecycle table, while the important session/report/driver ownership boundaries are temporal runner behavior rather than page visuals.
