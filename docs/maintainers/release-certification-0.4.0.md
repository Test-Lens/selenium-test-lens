# Test Lens 0.4.0 release certification

This maintainer evidence records the S12B3 platform and real-browser run made on 2026-09-29. It is a release-candidate checkpoint, not a substitute for the final S12B4 certification. A missing row is never inferred from another browser or mode.

## Run identity

| Field | Value |
| --- | --- |
| Source commit | `53b7c7c8a2aceaeed38a4a34bd5ac50759c61f5e` plus the reviewed S12B3 test/evidence changes |
| OS | Windows 11, amd64 |
| Java | Microsoft OpenJDK 21.0.10 LTS |
| Maven | 3.9.13 |
| Chrome | 152.0.7977.83 |
| ChromeDriver | 152.0.7977.82 |
| Firefox / GeckoDriver | NOT_RUN — Firefox is not installed on this certification host |

Selenium Manager resolved the ChromeDriver shown above. Chrome 152 also emitted the expected warning that Selenium 4.39 has no version-matched CDP module; the explicit WebDriver BiDi tests did not depend on that CDP module and passed.

## Browser matrix

| Browser | Mode | Observability | Scope | Result |
| --- | --- | --- | --- | --- |
| Chrome | headless (`--headless=new`) | DEFAULT | full `browser-it` suite | PASS — 113 tests, 0 failures/errors, 6 declared skips |
| Chrome | headed | DEFAULT plus focused FAST contract | compatibility capture, execution config, native observation, TestNG lifecycle, Selector Live and Selector Lab | PASS — 15/15 |
| Chrome | headless | FAST | real native action/evidence contract in `NativeSeleniumObservationIT` | PASS |
| Chrome | headless | BiDi BIDI/AUTO | explicit `NetworkBiDiBrowserIT` | PASS — 5/5 |
| Firefox | headless | DEFAULT | full suite | NOT_RUN — browser unavailable |
| Firefox | headed | DEFAULT | targeted suite | NOT_RUN — browser unavailable |
| Firefox | headless | FAST | focused contract | NOT_RUN — browser unavailable |
| Firefox | headless | BiDi BIDI/AUTO | explicit network suite | NOT_RUN — browser unavailable |

The full Chrome run covered Smart Click and fallback contracts, native Selenium observation, highlight/HUD/source-navigation state, Selector Live, Selector Lab, compatibility capture, and headless execution metadata. The headed run independently captured a manifest with `headed=true`; headed behavior was not inferred from the headless pass.

## TestNG lifecycle

`ListenerTestNgBrowserIT` passed on real Chrome in both runs. PER_METHOD created an invocation-owned browser/session. Sequential PER_CLASS kept one browser owner across three logical invocations, created independent Lens sessions/reports, enabled BiDi, and closed the browser at final teardown. Retry, DataProvider, parallel and factory conflict guards remain covered by the non-browser adapter suite rather than an unnecessary browser Cartesian matrix.

Firefox lifecycle certification is NOT_RUN because Firefox is absent from this host.

## Wrapper-main and Windows filesystem

The local Maven wrapper-main fixture packages a harmless test class at exactly `org.apache.maven.wrapper.MavenWrapperMain`, binds the JAR and properties SHA-256 values, and launches it through the production trusted `java.exe -classpath ...` argument array. Literal shell-looking input remains a literal argument. The fixture uses no shell, network capability, download, or public wrapper artifact. It certifies Test Lens integration, not upstream Maven Wrapper internals. Gradle layout, fixed-main selection, digests and stale-plan rejection retain existing coverage; a separate local Gradle process fixture was not added.

On the local NTFS filesystem, disposable migration tests exercised sibling temp creation, `FileChannel.force(true)`, atomic replacement where supported, target digest verification, repository `FileChannel` locking, UTF-8 BOM, CRLF and supplementary Unicode preservation, exact rollback to original bytes, and DOS read-only rejection before plan creation. The injected atomic-move fallback remains covered deterministically; `ATOMIC_MOVE` failure was not naturally reproducible on this filesystem. ACL/owner preservation is not claimed.

The symlink containment fixture was skipped because this Windows process lacks symlink creation privilege (`SKIP_WINDOWS_SYMLINK_PRIVILEGE`). Production `NOFOLLOW_LINKS` and containment checks remain enabled.

## Providers and declared skips

- Local Grid: NOT_RUN_OPTIONAL_ENVIRONMENT. Docker 29.7.2 is available, but no Selenium Grid/browser image is present; S12B3 did not download or install one.
- BrowserStack: BROWSERSTACK_NOT_RUN_OPTIONAL_PROVIDER. No authorized username/access-key references are present; BrowserStack is not a runtime dependency or a release requirement.
- Firefox: NOT_RUN_RC_ENVIRONMENT_MISSING_BROWSER. The final release candidate still requires the Firefox rows on a suitable machine.
- Chrome full-suite skips: `HudStudioDocumentationLayoutIT` needs an explicit docs URL; the five other skips are opt-in runtime/TestNG/workload/Selector Live performance tests. No functional browser contract was silently skipped.
- Direct `.cmd`, `.bat`, and `.ps1` migration verification plans continue to safe-stop. Native executables and fixed Java wrapper-main adapters are the supported shell-free alternatives.

## Commands

```powershell
mvn -Pbrowser-it -pl selenium-test-lens-browser-tests -am verify -Dbrowser=chrome -Dheaded=false
mvn -Pbrowser-it -pl selenium-test-lens-browser-tests -am verify -Dbrowser=chrome -Dheaded=true "-Dit.test=HeadlessExecutionConfigIT,NativeSeleniumObservationIT,CompatibilityCaptureIT,ListenerTestNgBrowserIT,SelectorLiveCandidateIT,SelectorLabBrowserIT"
```

The initial sandboxed headed invocation could not update Maven status files outside its writable root and is environment noise, not a product failure; the authorized identical command above passed.
