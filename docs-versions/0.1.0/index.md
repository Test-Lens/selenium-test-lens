# Selenium Test Lens 0.1.0

**Selenium Test Lens 0.1.0 — stable**

This is the documentation snapshot for the latest release currently available from Maven Central. Test Lens 0.1.0 attaches to an existing Selenium `WebDriver` and adds observable locator operations, a browser HUD, structured trace events, reports, manual network diagnostics, and optional React helpers.

The pages in this version describe only code present in tag [`v0.1.0`](https://github.com/Test-Lens/selenium-test-lens/tree/v0.1.0). [Looking for unreleased features? See the development documentation.](https://test-lens.github.io/selenium-test-lens/dev/)

## What 0.1.0 includes

- `TestLens.attach(driver)` and an explicit session lifecycle.
- Selenium `By` locators plus the original test-id, text, label, placeholder, and role-oriented factories.
- Element actions, reads, indexed collection views, waits, and element assertions.
- Browser-side HUD/highlight diagnostics and structured trace events.
- JSON and HTML session/suite reports and report ZIP export.
- A manual network event collector with waits and assertions.
- Optional React-oriented actionability and action helpers.

Start with [installation](installation.md) and [getting started](getting-started.md), then use the task-oriented guides in this version. The [published Javadoc](https://javadoc.io/doc/io.github.test-lens/selenium-test-lens/0.1.0/) is the exhaustive signature reference, not a replacement for these guides.
