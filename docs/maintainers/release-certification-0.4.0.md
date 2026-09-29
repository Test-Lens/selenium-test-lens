# Test Lens 0.4.0 release certification

This maintainer evidence records the S12B3 platform and real-browser run and the completed S12B4 release-form preparation made on 2026-09-29. Every mandatory consumer row was executed independently; no result is inferred from another JDK, build tool, browser, or mode.

## Run identity

| Field | Value |
| --- | --- |
| Source commit | `7fd5827fe9e2006767919ab0c9a2d8ee62d0f555` plus this Firefox certification evidence update |
| OS | Windows 11, amd64 |
| Java | Microsoft OpenJDK 21.0.10 LTS |
| Maven | 3.9.13 |
| Chrome | 152.0.7977.83 |
| ChromeDriver | 152.0.7977.82 |
| Firefox | 156.0.1 |
| GeckoDriver | 0.37.1 |

Selenium Manager resolved both drivers shown above. Chrome 152 also emitted the expected warning that Selenium 4.39 has no version-matched CDP module; the explicit WebDriver BiDi tests did not depend on that CDP module and passed.

## Browser matrix

| Browser | Mode | Observability | Scope | Result |
| --- | --- | --- | --- | --- |
| Chrome | headless (`--headless=new`) | DEFAULT | full `browser-it` suite | PASS — 113 tests, 0 failures/errors, 6 declared skips |
| Chrome | headed | DEFAULT plus focused FAST contract | compatibility capture, execution config, native observation, TestNG lifecycle, Selector Live and Selector Lab | PASS — 15/15 |
| Chrome | headless | FAST | real native action/evidence contract in `NativeSeleniumObservationIT` | PASS |
| Chrome | headless | BiDi BIDI/AUTO | explicit `NetworkBiDiBrowserIT` | PASS — 5/5 |
| Firefox | headless (`-headless`) | DEFAULT | full `browser-it` suite | PASS — 113 tests, 0 failures/errors, 11 declared skips |
| Firefox | headed | DEFAULT | compatibility capture, execution config, native observation, TestNG lifecycle, Selector Live and Selector Lab | PASS — 15/15 |
| Firefox | headless | FAST | real native action/presentation-suppression contract in `NativeSeleniumObservationIT` | PASS — 1/1 |
| Firefox | headless | BiDi BIDI/AUTO | explicit `NetworkBiDiBrowserIT` | PASS — 5/5 |

The full Chrome and Firefox runs covered Smart Click and fallback contracts, native Selenium observation, highlight/HUD/source-navigation state, Selector Live, Selector Lab, compatibility capture, and headless execution metadata. Each headed run independently captured a manifest with `headed=true`; headed behavior was not inferred from a headless pass. The fresh Firefox captures recorded Firefox 156.0.1, GeckoDriver 0.37.1, Windows, DEFAULT observability, requested/effective headless state, viewport/screen evidence and evidence availability. The headed Firefox capabilities independently reported `moz:headless=false`.

## TestNG lifecycle

`ListenerTestNgBrowserIT` passed on real Chrome and Firefox. On Firefox, the explicit focused run passed 2/2 with no skips: PER_METHOD created an invocation-owned browser/session and exact report artifacts; sequential PER_CLASS kept one BiDi-enabled browser owner across three logical invocations, created three independent Lens sessions/reports and network captures, and closed the browser at final teardown. Retry, DataProvider, parallel and factory conflict guards remain covered by the non-browser adapter suite rather than an unnecessary browser Cartesian matrix.

## Wrapper-main and Windows filesystem

The local Maven wrapper-main fixture packages a harmless test class at exactly `org.apache.maven.wrapper.MavenWrapperMain`, binds the JAR and properties SHA-256 values, and launches it through the production trusted `java.exe -classpath ...` argument array. Literal shell-looking input remains a literal argument. The fixture uses no shell, network capability, download, or public wrapper artifact. It certifies Test Lens integration, not upstream Maven Wrapper internals. Gradle layout, fixed-main selection, digests and stale-plan rejection retain existing coverage; a separate local Gradle process fixture was not added.

On the local NTFS filesystem, disposable migration tests exercised sibling temp creation, `FileChannel.force(true)`, atomic replacement where supported, target digest verification, repository `FileChannel` locking, UTF-8 BOM, CRLF and supplementary Unicode preservation, exact rollback to original bytes, and DOS read-only rejection before plan creation. The injected atomic-move fallback remains covered deterministically; `ATOMIC_MOVE` failure was not naturally reproducible on this filesystem. ACL/owner preservation is not claimed.

The symlink containment fixture was skipped because this Windows process lacks symlink creation privilege (`SKIP_WINDOWS_SYMLINK_PRIVILEGE`). Production `NOFOLLOW_LINKS` and containment checks remain enabled.

## Providers and declared skips

- Local Grid: NOT_RUN_OPTIONAL_ENVIRONMENT. Docker 29.7.2 is available, but no Selenium Grid/browser image is present; S12B3 did not download or install one.
- BrowserStack: BROWSERSTACK_NOT_RUN_OPTIONAL_PROVIDER. No authorized username/access-key references are present; BrowserStack is not a runtime dependency or a release requirement.
- Chrome full-suite skips: `HudStudioDocumentationLayoutIT` needs an explicit docs URL; the five other skips are opt-in runtime/TestNG/workload/Selector Live performance tests. No functional browser contract was silently skipped.
- Firefox full-suite skips: five `BrowserProfileCleanupIT` cases are deliberately Chrome-specific, `HudStudioDocumentationLayoutIT` needs an explicit docs URL, and five runtime/TestNG/workload/Selector Live performance tests are opt-in. No functional Firefox contract was silently skipped.
- Direct `.cmd`, `.bat`, and `.ps1` migration verification plans continue to safe-stop. Native executables and fixed Java wrapper-main adapters are the supported shell-free alternatives.

## Commands

```powershell
mvn -Pbrowser-it -pl selenium-test-lens-browser-tests -am verify -Dbrowser=chrome -Dheaded=false
mvn -Pbrowser-it -pl selenium-test-lens-browser-tests -am verify -Dbrowser=chrome -Dheaded=true "-Dit.test=HeadlessExecutionConfigIT,NativeSeleniumObservationIT,CompatibilityCaptureIT,ListenerTestNgBrowserIT,SelectorLiveCandidateIT,SelectorLabBrowserIT"
mvn -Pbrowser-it -pl selenium-test-lens-browser-tests -am verify -Dbrowser=firefox -Dheaded=false
mvn -Pbrowser-it -pl selenium-test-lens-browser-tests -am verify -Dbrowser=firefox -Dheaded=true "-Dit.test=HeadlessExecutionConfigIT,NativeSeleniumObservationIT,CompatibilityCaptureIT,ListenerTestNgBrowserIT,SelectorLiveCandidateIT,SelectorLabBrowserIT"
mvn -Pbrowser-it -pl selenium-test-lens-browser-tests -am verify -Dbrowser=firefox -Dheaded=false "-Dit.test=NativeSeleniumObservationIT#fastObservationKeepsBusinessActionWithoutCreatingPresentation"
mvn -Pbrowser-it -pl selenium-test-lens-browser-tests -am verify -Dbrowser=firefox -Dheaded=false "-Dit.test=NetworkBiDiBrowserIT,ListenerTestNgBrowserIT"
```

The initial sandboxed headed invocation could not update Maven status files outside its writable root and is environment noise, not a product failure; the authorized identical command above passed.

## S12B4 release-form preparation

The prepared workspace is based on `0c13c1b81b2dbdb2730a8ff5d7ac11c833e36c70`. It resolves all 16 normal reactor projects and all 17 projects with `browser-it` to `0.4.0`. No runtime or browser source changed from the S12B3-certified commit. The final `chore(release): prepare 0.4.0` commit follows review of this accumulated release-form change set.

| Gate | Result |
| --- | --- |
| `mvn clean verify -Dheaded=false` | PASS, 16-project release-form reactor |
| Central profile, deploy disabled and GPG skipped | PASS, 16 projects; source and Javadoc JAR generation validated |
| Publication staging | PASS, exactly 8 publishable coordinates and 9 excluded projects |
| Java 17 Maven clean-room consumer | PASS with Maven 3.9.13 on Microsoft OpenJDK 17.0.20.1 against locally staged `0.4.0` artifacts |
| Java 17 Gradle clean-room consumer | PASS with Gradle 8.10.2 on Microsoft OpenJDK 17.0.20.1 against locally staged `0.4.0` artifacts |
| Java 21 Maven clean-room consumer | PASS against locally staged `0.4.0` artifacts |
| Java 21 Gradle clean-room consumer | PASS with Gradle 8.10.2 on Microsoft OpenJDK 21.0.10 against locally staged `0.4.0` artifacts |
| Public API | PASS: 283 types, 2183 callables; baseline 276/2132; delta 7/51; `@since 0.4.0` PASS |
| Release-form Chrome smoke | PASS: Chrome 152.0.7977.83 / ChromeDriver 152.0.7977.82, headless compatibility capture 1/1 |
| Release-form Firefox smoke | PASS: Firefox 156.0.1 / GeckoDriver 0.37.1, headless compatibility capture 1/1 |
| Versioned documentation simulation | PASS: `0.4.0`, `latest -> 0.4.0`, historical versions retained, development source kept distinct |

The clean-room Maven consumer used only the local staged repository for Test Lens `0.4.0`; third-party dependencies came from the configured dependency repositories. No Test Lens snapshot or workspace reactor classpath was accepted as a substitute. The release-form browser smokes supplement, rather than rewrite, the full S12B3 evidence above.

The Central profile was exercised with `-Dgpg.skip=true`; signing was not attempted and no signed-artifact claim is made. The project does not claim byte-for-byte reproducible archives for 0.4.0 because no `project.build.outputTimestamp` contract is configured.

### Documented limitations

- Direct `.cmd`, `.bat`, and `.ps1` migration verification plans safe-stop; fixed Java wrapper-main and trusted native executables are the supported alternatives.
- Compatibility and selector tooling enforce documented aggregate/input/output bounds; unlimited scale is not claimed.
- Byte-for-byte reproducible archives are not claimed for 0.4.0.

### Optional NOT_RUN

- BrowserStack and local Grid remain optional environment/provider validations.
- Windows symlink creation was skipped for missing process privilege.
- The atomic-move fallback is deterministically injected in tests but was not naturally reproduced on the certification NTFS volume.
- Archive signing was not run during local preparation; it remains an explicitly authorized release operation.
- Workflow YAML was not re-linted locally because neither `actionlint` nor Python/PyYAML is installed; workflows were unchanged from S12B3 and their repository contract checks remained in the docs simulation.
