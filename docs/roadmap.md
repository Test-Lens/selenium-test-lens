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

1. Keep the polished single-file per-test and suite HTML reports focused on static/offline diagnostics rather than turning them into a full interactive viewer.
2. Keep JSON reports and ZIP bundles stable enough for CI/API integrations while the pre-1.0 API is finalized.
3. Map more action/assertion/network metadata into trace attributes where useful.
4. Add better anchors between failure summaries and timeline events.

## Network diagnostics

1. Add an optional real browser network capture provider.
2. Evaluate guarded support for Selenium performance logs and WebDriver BiDi.
3. Keep mocking/interception as a separate future feature, not part of passive diagnostics.

## Packaging and release readiness

1. Decide final public groupId/artifact naming.
2. Revisit Maven Wrapper once the local environment can generate it reliably.
3. Add a compact API migration note for any final pre-1.0 renames.

## Maven Central release checklist

The `central-release` Maven profile is configured for release-time source jars, Javadoc jars, GPG signing and Central Portal deployment. It is inactive by default and does not include credentials. See [release verification](release.md) for the current CI and manual release-check commands.

Before publishing:

1. Choose and tag a non-SNAPSHOT release version.
2. Configure a GPG key locally or in CI.
3. Configure Central Portal credentials in Maven `settings.xml` or CI secrets for server id `central`, or override `central.publishing.serverId`.
4. Run a verification build such as `mvn -Pcentral-release -DskipTests verify` in the release environment.
5. Deploy only when the staged release contents have been reviewed.

Do not store Central Portal tokens, signing keys or passphrases in this repository.

## Possible future modules

1. RestAssured adapter/module, if API-test diagnostics become part of the product scope.
2. Provider-specific video artifact helpers for CI/Selenium Grid/Selenoid/BrowserStack/Sauce, without making the core library provider-dependent.
3. Additional locator helpers if their matching rules stay clear and maintainable.
