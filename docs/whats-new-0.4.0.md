# What's new in 0.4.0

Test Lens for Selenium 0.4.0 expands runtime diagnostics while keeping Selenium and the consumer-owned `WebDriver` in control. Java 17 remains the minimum runtime, Selenium stays an explicit consumer dependency, and the published Maven group remains `io.github.test-lens`.

## Runtime and runner integration

- Native Selenium observation can add correlated HUD, trace, report, source-navigation, and redacted evidence around ordinary `WebDriver` and selected `WebElement` operations without substituting Smart Click or repeating the native action.
- Smart Click, highlighting, HUD status, source navigation, full-page evidence, trace retention, and failure reporting have tighter bounded lifecycle and ordering contracts.
- Headed/headless intent is explicit before adapted driver creation. `ObservabilityMode.FAST` reduces live presentation and successful-test retention without changing browser interaction semantics or failure evidence.
- The TestNG adapter supports sequential `PER_CLASS` driver ownership while retaining a fresh Test Lens invocation for every logical test. Retry, DataProvider, factory, parallel, and shared-driver conflicts remain guarded.

## Offline analysis tooling

The 0.4.0 source tree includes bounded offline Selector Intelligence, Selector Audit, Selector Lab, Compatibility Analyzer, and Migration Assistant tooling. These modules are internal and are not published to Maven Central. They do not add dependencies or startup work to the normal runtime artifacts.

The Migration Assistant provides read-only Git preflight, evidence-bound proposals, explicit trusted run orchestration, guarded exact-byte Apply transactions, ownership-checked rollback and recovery, post-Apply verification, and sanitized local reports. It does not provide a generic shell, automatic Git commit, push, or implicit source mutation.

## Security and bounds

- Imported HTML, browser content, source prose, and reports cannot authorize project-code execution or source mutation.
- Compatibility and selector tooling use explicit input/output budgets and fail structurally at limits rather than silently truncating complete evidence.
- Direct `.cmd`, `.bat`, and `.ps1` Migration verification plans safe-stop. Trusted native executables and fixed Java wrapper-main adapters are the supported shell-free alternatives.
- Exact source backups, patches, diffs, command output, and migration evidence remain local-sensitive artifacts and are not embedded in shareable summaries by default.

## Compatibility and certification

Release-candidate browser coverage includes current local Chrome and Firefox in headed/headless, DEFAULT/FAST, WebDriver BiDi, compatibility capture, and representative TestNG lifecycle paths. BrowserStack and external Grid runs are optional provider validation and are not claimed for this release.

The eight Central coordinates are the parent POM plus `selenium-test-lens-core`, `selenium-test-lens-overlay`, `selenium-test-lens`, `selenium-test-lens-junit5`, `selenium-test-lens-testng`, `selenium-test-lens-allure`, and `selenium-test-lens-react`. Selector, compatibility, migration, example, and browser-test modules remain nonpublished.

See the [changelog](https://github.com/Test-Lens/selenium-test-lens/blob/main/CHANGELOG.md) for the complete factual list and [consumer compatibility](consumer-compatibility.md) for the release-artifact validation boundary.
