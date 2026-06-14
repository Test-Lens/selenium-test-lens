# Roadmap

This roadmap tracks follow-up work after the 0.1 feature consolidation.

## Near-term polish

1. Stabilize public API names before a 0.1 release.
2. Review helper visibility and hide implementation helpers where appropriate.
3. Keep README and the current docs set as the source of truth.
4. Clean up examples so documentation-only examples are clearly separated from executable tests.

## Visual overlay

1. Extend the common HUD theme system to Wait HUD and assertion badges.
2. Add a lightweight visual smoke page for HUD theme inspection.
3. Document exact visual debugging methods once the pre-1.0 API is frozen.

## Trace and evidence

1. Improve HTML trace report UX without turning it into a full interactive viewer.
2. Map more action/assertion/network metadata into trace attributes where useful.
3. Add better anchors between failure summaries and timeline events.

## Network diagnostics

1. Add an optional real browser network capture provider.
2. Evaluate guarded support for Selenium performance logs and WebDriver BiDi.
3. Keep mocking/interception as a separate future feature, not part of passive diagnostics.

## Packaging and release readiness

1. Decide final public groupId/artifact naming.
2. Prepare Maven Central publishing configuration when the API is stable enough.
3. Revisit Maven Wrapper once the local environment can generate it reliably.
4. Add a compact API migration note for any final pre-1.0 renames.

## Possible future modules

1. RestAssured adapter/module, if API-test diagnostics become part of the product scope.
2. Provider-specific video artifact helpers for CI/Selenium Grid/Selenoid/BrowserStack/Sauce, without making the core library provider-dependent.
3. Additional locator helpers if their matching rules stay clear and maintainable.
