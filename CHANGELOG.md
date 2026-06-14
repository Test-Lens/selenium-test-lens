# Changelog

All notable changes to UI Test Lens will be documented in this file.

## [0.1.0-SNAPSHOT] - Unreleased

### Added
- Multi-module Maven structure.
- `ui-test-lens-core` with the logging/event model, log sinks, log exporters, `BrowserScriptExecutor`, and `JsResources`.
- `ui-test-lens-overlay` with browser runtime JavaScript resources and overlay bridge classes.
- `ui-test-lens-selenium` with Selenium facade/actions, waits, guards, popup/blocking overlay helpers, target resolver, and Selenium factories/adapters.
- `ui-test-lens-react` with React-safe helpers and the `ReactSupport` entrypoint.
- `ui-test-lens-examples` with compile-checked examples.
- Runtime JavaScript resources for API overlay, Wait HUD, Highlight, Type hint, Scroll arrow, HUD panel, and Assertion badges.

### Changed
- Migrated project naming from the historical helper codebase to UI Test Lens.
- Moved Java packages to `io.github.mmaciekk111.uitestlens`.
- Changed project version to `0.1.0-SNAPSHOT` to reflect pre-1.0 API status.
- Split core, overlay, selenium, react, examples, and all-in-one compatibility responsibilities.
- Introduced `window.__uiTestLens` as the primary browser runtime namespace while preserving legacy aliases.

### Removed
- Removed direct dependencies on legacy utility classes.
- Removed RestAssured from the main artifact path.
- Removed Selenium dependency from the overlay module.
- Removed React dependency from the Selenium module.

### Notes
- Project is still pre-1.0.
- Public APIs may still change between 0.x releases.
- Maven Central publication is not configured yet.
- Legacy browser runtime aliases are still maintained for compatibility.
